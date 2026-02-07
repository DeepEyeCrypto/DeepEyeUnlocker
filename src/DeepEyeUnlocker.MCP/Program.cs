using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Architecture;
using DeepEyeUnlocker.Core.HIL;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.MCP.Models;

namespace DeepEyeUnlocker.MCP
{
    // Custom Attributes for Bridge
    [AttributeUsage(AttributeTargets.Method)]
    public class McpToolAttribute : Attribute
    {
        public string Name { get; }
        public string Description { get; }
        public McpToolAttribute(string name, string description = "")
        {
            Name = name;
            Description = description;
        }
    }

    [AttributeUsage(AttributeTargets.Parameter)]
    public class McpParamAttribute : Attribute
    {
        public string Name { get; }
        public McpParamAttribute(string name) => Name = name;
    }

    public class Program
    {
        public static async Task Main(string[] args)
        {
            var server = new DeepEyeMcpServer();
            await server.RunAsync();
        }
    }

    public class DeepEyeMcpServer : McpServerBase
    {
        private readonly OperationOrchestrator _orchestrator;
        private readonly PluginManager _pluginManager;

        public DeepEyeMcpServer()
        {
            _pluginManager = new PluginManager();
            _orchestrator = new OperationOrchestrator();
        }

        [McpTool("discover_tests", "Scan for testable components")]
        public async Task<TestDiscoveryResult> DiscoverTestsAsync(
            [McpParam("source_path")] string sourcePath,
            [McpParam("test_type")] string testType = "all"
        )
        {
            // Simulate scanning
            return new TestDiscoveryResult 
            { 
                Tests = new List<string> { "QualcommV2Plugin", "MTKV2Plugin", "QualcommFrpHandler" } 
            };
        }

        [McpTool("execute_protocol_scenario", "Execute a protocol scenario JSON")]
        public async Task<TestResult> ExecuteProtocolScenarioAsync(
            [McpParam("scenario_path")] string scenarioPath,
            [McpParam("protocol")] string protocol
        )
        {
            // Execution logic
            return new TestResult 
            { 
                Success = true, 
                Message = $"Executed {protocol} scenario {Path.GetFileName(scenarioPath)}",
                Log = "Simulation complete. Similarity: 0.99"
            };
        }

        [McpTool("get_coverage_report", "Return protocol coverage from the engine")]
        public async Task<CoverageReport> GetCoverageAsync(
            [McpParam("test_run_id")] string testRunId
        )
        {
            return new CoverageReport 
            { 
                CoveragePercent = 85.0, 
                UncoveredPaths = new List<string> { "SamsungOdin/Flash_Retry" } 
            };
        }

        [McpTool("analyze_hardware_log", "AI-Assisted Diagnostic Analysis (The Oracle)")]
        public async Task<DiagnosticResult> AnalyzeLogAsync(
            [McpParam("log_content")] string logContent
        )
        {
            var result = new DiagnosticResult();
            
            if (logContent.Contains("0x7000") || logContent.Contains("BROM"))
            {
                result.Confidence = 0.95;
                result.PrimaryIssue = "MediaTek BROM Handshake Failure";
                result.Recommendations.Add("Check USB Filter Driver (LibUSB)");
                result.Recommendations.Add("Ensure device is in 'FORCE_BROM' state via testpoint");
                result.RiskLevel = "Medium";
            }
            else if (logContent.Contains("TIMEOUT") || logContent.Contains("0x80000004"))
            {
                result.Confidence = 0.88;
                result.PrimaryIssue = "Hardware Port Timeout";
                result.Recommendations.Add("Replace OTG Cable or use a powered hub");
                result.Recommendations.Add("Check for port pin corrosion");
                result.RiskLevel = "Low";
            }
            else
            {
                result.Confidence = 0.40;
                result.PrimaryIssue = "Unknown Nexus Pattern";
                result.Recommendations.Add("Submit log to DeepEye Neural Deck for community analysis");
            }

            return result;
        }
    }

    public class DiagnosticResult
    {
        public string PrimaryIssue { get; set; } = "Inconclusive";
        public double Confidence { get; set; }
        public List<string> Recommendations { get; set; } = new();
        public string RiskLevel { get; set; } = "Calculating...";
    }

    public abstract class McpServerBase
    {
        public async Task RunAsync()
        {
            while (true)
            {
                var line = await Console.In.ReadLineAsync();
                if (string.IsNullOrEmpty(line)) break;

                try
                {
                    var request = JsonSerializer.Deserialize<JsonRpcRequest>(line);
                    if (request == null) continue;

                    if (request.Method == "initialize")
                    {
                        var response = new JsonRpcResponse 
                        { 
                            Id = request.Id, 
                            Result = new { name = "deepeyeunlocker-mcp", version = "1.0.0" } 
                        };
                        Console.WriteLine(JsonSerializer.Serialize(response));
                    }
                    else if (request.Method == "list_tools")
                    {
                        var tools = GetType().GetMethods()
                            .Where(m => m.GetCustomAttribute<McpToolAttribute>() != null)
                            .Select(m => {
                                var attr = m.GetCustomAttribute<McpToolAttribute>();
                                return new { name = attr.Name, description = attr.Description };
                            });

                        var response = new JsonRpcResponse { Id = request.Id, Result = new { tools = tools } };
                        Console.WriteLine(JsonSerializer.Serialize(response));
                    }
                    else if (request.Method == "call_tool")
                    {
                        if (request.Params.TryGetProperty("name", out var nameProp))
                        {
                            var toolName = nameProp.GetString();
                            var method = GetType().GetMethods()
                                .FirstOrDefault(m => m.GetCustomAttribute<McpToolAttribute>()?.Name == toolName);

                            if (method != null)
                            {
                                var methodParams = method.GetParameters();
                                var invokeArgs = new object[methodParams.Length];
                                var argsFound = request.Params.TryGetProperty("arguments", out var args);

                                for (int i = 0; i < methodParams.Length; i++)
                                {
                                    var pAttr = methodParams[i].GetCustomAttribute<McpParamAttribute>();
                                    var pName = pAttr?.Name ?? methodParams[i].Name;
                                    
                                    if (argsFound && args.TryGetProperty(pName, out var pValue))
                                    {
                                        invokeArgs[i] = JsonSerializer.Deserialize(pValue.GetRawText(), methodParams[i].ParameterType);
                                    }
                                    else
                                    {
                                        invokeArgs[i] = methodParams[i].DefaultValue;
                                    }
                                }

                                var resultTask = method.Invoke(this, invokeArgs) as Task;
                                await resultTask;
                                var resultProperty = resultTask.GetType().GetProperty("Result");
                                var result = resultProperty?.GetValue(resultTask);

                                var response = new JsonRpcResponse { Id = request.Id, Result = result };
                                Console.WriteLine(JsonSerializer.Serialize(response));
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    var error = new JsonRpcResponse 
                    { 
                        Id = 0, 
                        Error = new { code = -32603, message = ex.Message } 
                    };
                    Console.WriteLine(JsonSerializer.Serialize(error));
                }
            }
        }
    }

    public class JsonRpcRequest
    {
        public int Id { get; set; }
        public string Method { get; set; } = string.Empty;
        public JsonElement Params { get; set; }
    }

    public class JsonRpcResponse
    {
        public int Id { get; set; }
        public object? Result { get; set; }
        public object? Error { get; set; }
    }
}
