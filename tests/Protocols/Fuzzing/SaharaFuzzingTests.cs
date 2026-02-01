using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;
using DeepEyeUnlocker.Core.Diagnostics;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Simulation;
using DeepEyeUnlocker.Tests.Mocks;

namespace DeepEyeUnlocker.Tests.Protocols.Fuzzing
{
    [Trait("Category", "Fuzzing")]
    public class SaharaFuzzingTests
    {
        private readonly string _scenarioDir;
        private readonly ITestOutputHelper _output;

        public SaharaFuzzingTests(ITestOutputHelper output)
        {
            _output = output;
            _scenarioDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "scenarios");
        }

        [Fact]
        public async Task Sahara_FuzzHandshake_StructuredCampaign()
        {
            // Arrange
            var scenarioPath = Path.Combine(_scenarioDir, "sahara", "hello_success.json");
            var scenario = ScenarioLoader.Load(scenarioPath);
            var harness = new ProtocolFuzzHarness();
            int iterations = 100;
            
            ProtocolCoverage.Reset();
            ProtocolCoverage.Enable();
            int baselineCount = ProtocolCoverage.UniquePathCount;

            // Act
            var crashes = await harness.FuzzSaharaAsync(scenario, iterations);

            // Assert & Report
            string reportPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "fuzz_reports", "sahara_fuzz_report.json");
            FuzzReportGenerator.Generate(iterations, crashes, reportPath);
            
            _output.WriteLine($"Fuzzing Campaign Complete. Report saved to: {reportPath}");
            _output.WriteLine($"New paths discovered: {ProtocolCoverage.UniquePathCount - baselineCount}");

            foreach (var crash in crashes)
            {
                string crashPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "fuzz_findings", $"sahara_crash_seed_{crash.Seed}.json");
                FuzzReportGenerator.SaveCrashScenario(scenario, crash.Seed, crashPath);
                _output.WriteLine($"CRASH FOUND: Seed {crash.Seed}. Regression scenario saved to {crashPath}");
            }

            Assert.Empty(crashes);
        }
    }
}
