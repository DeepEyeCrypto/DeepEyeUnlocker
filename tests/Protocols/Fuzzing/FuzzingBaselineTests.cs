using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;
using DeepEyeUnlocker.Core.Diagnostics;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Simulation;
using DeepEyeUnlocker.Protocols.Qualcomm;
using DeepEyeUnlocker.Tests.Mocks;

namespace DeepEyeUnlocker.Tests.Protocols.Fuzzing
{
    [Trait("Category", "Fuzzing")]
    public class FuzzingBaselineTests
    {
        private readonly string _scenarioDir;
        private readonly ITestOutputHelper _output;

        public FuzzingBaselineTests(ITestOutputHelper output)
        {
            _output = output;
            _scenarioDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "scenarios");
        }

        [Fact]
        public async Task Sahara_BaselineCoverage_IsTracked()
        {
            // Arrange
            ProtocolCoverage.Reset();
            ProtocolCoverage.Enable();
            
            var scenarioPath = Path.Combine(_scenarioDir, "sahara", "hello_success.json");
            var scenario = ScenarioLoader.Load(scenarioPath);
            
            using var usb = new ScenarioUsbDevice(scenario);
            var protocol = new SaharaProtocol(usb);

            // Act
            bool result = await protocol.ProcessHelloAsync();
            usb.Dispose();

            // Assert
            Assert.True(result);
            Assert.True(ProtocolCoverage.WasHit("Sahara_ProcessHello_Success"), "Should have hit Sahara_ProcessHello_Success");
            
            var results = ProtocolCoverage.GetResults();
            _output.WriteLine("Coverage Results:");
            foreach (var kvp in results)
            {
                _output.WriteLine($"  {kvp.Key}: {kvp.Value}");
            }
            
            Assert.True(ProtocolCoverage.UniquePathCount > 0);
        }

        [Fact]
        public async Task Firehose_BaselineCoverage_IsTracked()
        {
            // Arrange
            ProtocolCoverage.Reset();
            ProtocolCoverage.Enable();
            
            var scenarioPath = Path.Combine(_scenarioDir, "firehose", "open_success.json");
            var scenario = ScenarioLoader.Load(scenarioPath);
            
            using var usb = new ScenarioUsbDevice(scenario);
            var protocol = new FirehoseProtocol(usb);

            // Act
            bool result = await protocol.ConfigureAsync();
            usb.Dispose();

            // Assert
            Assert.True(result);
            Assert.True(ProtocolCoverage.WasHit("Firehose_Config_Success"), "Should have hit Firehose_Config_Success");
            Assert.True(ProtocolCoverage.WasHit("Firehose_ReceiveResponse_Data"), "Should have hit Firehose_ReceiveResponse_Data");
            
            var results = ProtocolCoverage.GetResults();
            _output.WriteLine("Coverage Results:");
            foreach (var kvp in results)
            {
                _output.WriteLine($"  {kvp.Key}: {kvp.Value}");
            }
        }
    }
}
