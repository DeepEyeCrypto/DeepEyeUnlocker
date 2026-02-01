using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xunit;
using DeepEyeUnlocker.Core.AI;
using DeepEyeUnlocker.Infrastructure.HIL;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Tests.AI
{
    public class ProtocolAutoAnalyzerTests
    {
        [Fact]
        [Trait("Category", "AI")]
        public async Task Analyze_RawPackets_GeneratesValidScenario()
        {
            // Arrange
            var packets = new List<UsbPacket>
            {
                new UsbPacket { TimestampUs = 0, Direction = UsbDirection.DeviceToHost, Data = new byte[] { 0x01, 0x01, 0x00, 0x00 } },
                new UsbPacket { TimestampUs = 10000, Direction = UsbDirection.HostToDevice, Data = new byte[] { 0x02, 0x01, 0x00, 0x00 } }
            };

            var extractor = new PcapFeatureExtractor();
            var promptBuilder = new ProtocolAnalysisPrompt();
            var client = new MockLlmClient();
            var synthesizer = new ScenarioSynthesizer();

            // Act
            var features = extractor.ExtractFeatures(packets);
            var prompt = promptBuilder.BuildPrompt(packets, features);
            var response = await client.AnalyzeAsync(prompt);
            
            Assert.NotNull(response.Analysis);
            var scenario = synthesizer.GenerateScenario(response.Analysis, packets);

            // Assert
            Assert.Equal("Qualcomm Sahara", response.Analysis.ProtocolType);
            Assert.Equal(2, scenario.Steps.Count);
            Assert.Contains("Sahara Hello", scenario.Steps[0].Label);
            Assert.Equal(StepDirection.DeviceToHost, scenario.Steps[0].Direction);
        }
    }
}
