using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using DeepEyeUnlocker.Core.Diagnostics;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Simulation;
using DeepEyeUnlocker.Tests.Mocks;

namespace DeepEyeUnlocker.Tests.Protocols.Fuzzing
{
    public class FuzzReport
    {
        public DateTime GeneratedAt { get; set; } = DateTime.Now;
        public int TotalIterations { get; set; }
        public int CrashCount { get; set; }
        public int UniquePathsHit { get; set; }
        public List<string> Paths { get; set; } = new();
        public List<FuzzResult> Crashes { get; set; } = new();
    }

    public static class FuzzReportGenerator
    {
        public static string Generate(int totalIterations, List<FuzzResult> crashes, string outputPath)
        {
            var report = new FuzzReport
            {
                TotalIterations = totalIterations,
                CrashCount = crashes.Count,
                UniquePathsHit = ProtocolCoverage.UniquePathCount,
                Paths = ProtocolCoverage.GetResults().Keys.OrderBy(k => k).ToList(),
                Crashes = crashes
            };

            string json = JsonSerializer.Serialize(report, new JsonSerializerOptions { WriteIndented = true });
            
            Directory.CreateDirectory(Path.GetDirectoryName(outputPath)!);
            File.WriteAllText(outputPath, json);
            
            return json;
        }

        public static void SaveCrashScenario(ProtocolScenario baseScenario, int seed, string outputPath)
        {
            var mutator = new ProtocolMutator(seed);
            var mutatedScenario = new ProtocolScenario
            {
                Name = baseScenario.Name + "_fuzzed_seed_" + seed,
                Protocol = baseScenario.Protocol,
                Description = "Auto-generated regression scenario from fuzz seed " + seed,
                Steps = baseScenario.Steps.Select(s => {
                    var newStep = new ScenarioStep
                    {
                        Direction = s.Direction,
                        Label = s.Label,
                        DataHex = s.DataHex,
                        DelayMs = s.DelayMs
                    };
                    if (s.Direction == StepDirection.DeviceToHost)
                    {
                        var originalData = s.GetData();
                        var fuzzedData = mutator.Mutate(originalData);
                        newStep.DataHex = Convert.ToHexString(fuzzedData);
                    }
                    return newStep;
                }).ToList()
            };

            string json = JsonSerializer.Serialize(mutatedScenario, new JsonSerializerOptions { WriteIndented = true });
            Directory.CreateDirectory(Path.GetDirectoryName(outputPath)!);
            File.WriteAllText(outputPath, json);
        }
    }
}
