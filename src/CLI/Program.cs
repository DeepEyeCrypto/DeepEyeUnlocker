using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using CommandLine;
using DeepEyeUnlocker.Core.HIL;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Simulation;
using DeepEyeUnlocker.Infrastructure.HIL;
using DeepEyeUnlocker.Operations.HIL;
using DeepEyeUnlocker.Features.ModelDiscovery.Database;
using DeepEyeUnlocker.Features.ModelDiscovery.Services;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.CLI
{
    [Verb("capture", HelpText = "Capture USB traffic from a device.")]
    class CaptureOptions
    {
        [Option('v', "vid", Required = true, HelpText = "Vendor ID (hex).")]
        public string Vid { get; set; } = string.Empty;

        [Option('p', "pid", Required = true, HelpText = "Product ID (hex).")]
        public string Pid { get; set; } = string.Empty;

        [Option('o', "output", Required = true, HelpText = "Output Pcap file path.")]
        public string Output { get; set; } = string.Empty;
    }

    [Verb("convert", HelpText = "Convert Pcap capture to Scenario JSON.")]
    class ConvertOptions
    {
        [Option('i', "input", Required = true, HelpText = "Input Pcap file.")]
        public string Input { get; set; } = string.Empty;

        [Option('o', "output", Required = true, HelpText = "Output Scenario JSON.")]
        public string Output { get; set; } = string.Empty;

        [Option('p', "protocol", Required = true, HelpText = "Protocol (sahara, firehose, mtk).")]
        public string Protocol { get; set; } = string.Empty;
    }

    [Verb("validate", HelpText = "Validate a scenario against a golden reference.")]
    class ValidateOptions
    {
        [Option('i', "input", Required = true, HelpText = "Input Scenario JSON.")]
        public string InputPath { get; set; } = string.Empty;

        [Option('g', "golden", Required = true, HelpText = "Golden Scenario JSON.")]
        public string GoldenPath { get; set; } = string.Empty;

        [Option('r', "report", Required = false, HelpText = "Output HTML report path.")]
        public string ReportPath { get; set; } = string.Empty;
    }

    [Verb("register", HelpText = "Register a device and its protocol scenarios in the golden registry.")]
    class RegisterOptions
    {
        [Option('d', "id", Required = true, HelpText = "Unique Device ID.")]
        public string DeviceId { get; set; } = string.Empty;

        [Option('m', "model", Required = true, HelpText = "Device Model.")]
        public string Model { get; set; } = string.Empty;

        [Option('p', "protocol", Required = true, HelpText = "Protocol Name.")]
        public string Protocol { get; set; } = string.Empty;

        [Option('s', "scenario", Required = true, HelpText = "Path to Scenario JSON.")]
        public string ScenarioPath { get; set; } = string.Empty;
    }

    [Verb("list", HelpText = "List all registered golden devices.")]
    class ListOptions { }

    [Verb("analyze", HelpText = "Auto-analyze a USB pcap and generate a scenario.")]
    public class AnalyzeOptions
    {
        [Option('i', "input", Required = true, HelpText = "Input pcap file or directory.")]
        public string InputPath { get; set; } = string.Empty;

        [Option('o', "output", HelpText = "Output directory for generated scenarios.")]
        public string? OutputPath { get; set; }

        [Option("mock", Default = false, HelpText = "Use mock LLM for analysis.")]
        public bool UseMock { get; set; }
    }

    [Verb("models", HelpText = "Access the Global Supported Models Database.")]
    class ModelsOptions
    {
        [Value(0, MetaName = "action", Required = true, HelpText = "Action: brands, list, export")]
        public string Action { get; set; } = string.Empty;

        [Option('b', "brand", HelpText = "Filter by brand.")]
        public string? Brand { get; set; }

        [Option('s', "search", HelpText = "Search term.")]
        public string? Search { get; set; }

        [Option('l', "limit", Default = 50, HelpText = "Limit results.")]
        public int Limit { get; set; }

        [Option('o', "output", HelpText = "Output file path.")]
        public string? Output { get; set; }
    }

    class Program
    {
        static async Task<int> Main(string[] args)
        {
            return await Parser.Default.ParseArguments<CaptureOptions, ConvertOptions, ValidateOptions, RegisterOptions, ListOptions, AnalyzeOptions, ModelsOptions>(args)
                .MapResult(
                    (CaptureOptions opts) => RunCapture(opts),
                    (ConvertOptions opts) => RunConvert(opts),
                    (ValidateOptions opts) => RunValidate(opts),
                    (RegisterOptions opts) => RunRegister(opts),
                    (ListOptions opts) => RunList(opts),
                    (AnalyzeOptions opts) => RunAnalyze(opts),
                    (ModelsOptions opts) => RunModels(opts),
                    errs => Task.FromResult(1));
        }

        static Task<int> RunRegister(RegisterOptions opts)
        {
            var registry = new GoldenDeviceRegistry(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "hil_registry"));
            var info = registry.GetDevice(opts.DeviceId) ?? new GoldenDeviceInfo { DeviceId = opts.DeviceId, Model = opts.Model };
            
            info.Scenarios[opts.Protocol] = opts.ScenarioPath;
            registry.RegisterDevice(info);
            
            Console.WriteLine($"Registered {opts.Protocol} scenario for {opts.DeviceId} ({opts.Model})");
            return Task.FromResult(0);
        }

        static Task<int> RunList(ListOptions opts)
        {
            var registry = new GoldenDeviceRegistry(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "hil_registry"));
            var devices = registry.ListDevices();
            
            Console.WriteLine("--- Golden Device Registry ---");
            foreach (var d in devices)
            {
                Console.WriteLine($"{d.DeviceId} | {d.Model} | Protocols: {string.Join(", ", d.Scenarios.Keys)}");
            }
            return Task.FromResult(0);
        }

        static async Task<int> RunCapture(CaptureOptions opts)
        {
            Console.WriteLine($"Starting capture for VID:0x{opts.Vid} PID:0x{opts.Pid}...");
            var engine = new UsbCaptureEngine();
            int vid = Convert.ToInt32(opts.Vid, 16);
            int pid = Convert.ToInt32(opts.Pid, 16);
            
            await engine.StartCaptureAsync(vid, pid, opts.Output);
            Console.WriteLine("Capture started. Press any key to stop.");
            Console.ReadKey();
            await engine.StopCaptureAsync();
            Console.WriteLine($"Capture saved to {opts.Output}");
            return 0;
        }

        static async Task<int> RunConvert(ConvertOptions opts)
        {
            Console.WriteLine($"Converting {opts.Input} to {opts.Output}...");
            var engine = new UsbCaptureEngine();
            var converter = new PcapToScenarioConverter();
            
            var packets = await engine.ParseCaptureAsync(opts.Input);
            var scenario = converter.Convert(packets, opts.Protocol, new ConversionOptions());
            
            var json = JsonConvert.SerializeObject(scenario, Formatting.Indented);
            File.WriteAllText(opts.Output, json);
            
            Console.WriteLine("Conversion complete.");
            return 0;
        }

        static Task<int> RunValidate(ValidateOptions opts)
        {
            Console.WriteLine($"Validating {opts.InputPath} against {opts.GoldenPath}...");
            var actual = JsonConvert.DeserializeObject<ProtocolScenario>(File.ReadAllText(opts.InputPath));
            var golden = JsonConvert.DeserializeObject<ProtocolScenario>(File.ReadAllText(opts.GoldenPath));
            
            if (actual == null || golden == null)
            {
                Console.Error.WriteLine("Error loading scenarios.");
                return Task.FromResult(1);
            }

            var validator = new SimulationValidator();
            var result = validator.ValidateAgainstGolden(actual, golden, new ValidationOptions());
            
            Console.WriteLine($"Result: {(result.IsMatch ? "PASS" : "FAIL")}");
            Console.WriteLine($"Similarity: {result.SimilarityScore:P2}");
            
            if (!string.IsNullOrEmpty(opts.ReportPath))
            {
                var html = HilHtmlReporter.GenerateReport(result, opts.InputPath, actual.Protocol);
                File.WriteAllText(opts.ReportPath, html);
                Console.WriteLine($"HTML report saved to {opts.ReportPath}");
            }

            return Task.FromResult(result.IsMatch ? 0 : 1);
        }
        static async Task<int> RunAnalyze(AnalyzeOptions opts)
        {
            Console.WriteLine($"AI-Powered Analysis starting for {opts.InputPath}...");
            
            var captureEngine = new UsbCaptureEngine();
            var packets = await captureEngine.ParseCaptureAsync(opts.InputPath);
            
            var extractor = new DeepEyeUnlocker.Core.AI.PcapFeatureExtractor();
            var features = extractor.ExtractFeatures(packets);
            
            DeepEyeUnlocker.Core.AI.ILlmClient client = opts.UseMock 
                ? new DeepEyeUnlocker.Core.AI.MockLlmClient() 
                : throw new NotImplementedException("Real AI client requires API key configuration.");

            var promptBuilder = new DeepEyeUnlocker.Core.AI.ProtocolAnalysisPrompt();
            var prompt = promptBuilder.BuildPrompt(new List<UsbPacket>(packets), features);
            
            var response = await client.AnalyzeAsync(prompt);
            
            if (response.Analysis != null)
            {
                var synthesizer = new DeepEyeUnlocker.Core.AI.ScenarioSynthesizer();
                var scenario = synthesizer.GenerateScenario(response.Analysis, new List<UsbPacket>(packets));
                
                var json = JsonConvert.SerializeObject(scenario, Formatting.Indented);
                File.WriteAllText(opts.OutputPath ?? "output.json", json);
                
                Console.WriteLine($"Analysis complete. Scenario saved to {opts.OutputPath ?? "output.json"}");
                Console.WriteLine($"Protocol: {response.Analysis.ProtocolType} (Confidence: {response.Confidence:P2})");
                return 0;
            }

            Console.Error.WriteLine("AI Analysis failed to produce a valid protocol model.");
            return 1;
        }

        static async Task<int> RunModels(ModelsOptions opts)
        {
            using var db = new DiscoveryDbContext();
            var service = new DiscoveryService(db);

            switch (opts.Action.ToLower())
            {
                case "brands":
                    var brands = await service.GetBrandsAsync();
                    Console.WriteLine("--- Supported Brands ---");
                    foreach (var b in brands) Console.WriteLine(b);
                    break;

                case "list":
                    var models = await service.GetModelsAsync(opts.Brand, opts.Search, opts.Limit);
                    Console.WriteLine($"--- Models (Limit {opts.Limit}) ---");
                    Console.WriteLine($"{"Brand",-15} | {"Model Name",-30} | {"Model #",-15} | {"Tool"}");
                    Console.WriteLine(new string('-', 80));
                    foreach (var m in models)
                    {
                        Console.WriteLine($"{m.Brand,-15} | {m.MarketingName,-30} | {m.ModelNumber,-15} | {m.Tool}");
                    }
                    break;

                case "export":
                    if (string.IsNullOrEmpty(opts.Output))
                    {
                        Console.Error.WriteLine("Error: Output path required for export.");
                        return 1;
                    }
                    await service.ExportToCsvAsync(opts.Output, opts.Brand);
                    Console.WriteLine($"Exported DB to {opts.Output}");
                    break;

                default:
                    Console.Error.WriteLine($"Unknown action: {opts.Action}");
                    return 1;
            }

            return 0;
        }
    }
}
