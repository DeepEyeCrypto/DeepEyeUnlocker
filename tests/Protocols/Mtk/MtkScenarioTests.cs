using Xunit;
using DeepEyeUnlocker.Core.Simulation;
using DeepEyeUnlocker.Protocols.MTK;
using DeepEyeUnlocker.Core.Models;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Tests.Protocols.Mtk
{
    public class MtkScenarioTests
    {
        [Fact]
        public async Task Brom_Handshake_Success()
        {
            // 1. Define Scenario (BROM Handshake Pattern)
            var scenario = new ProtocolScenario
            {
                Name = "MTK_BROM_CONNECT",
                Steps = new List<ScenarioStep>
                {
                    // Preloader Handshake
                    new ScenarioStep { Direction = StepDirection.HostToDevice, Label = "StartCmd", DataHex = "A00A5005" },
                    new ScenarioStep { Direction = StepDirection.DeviceToHost, Label = "Ack", DataHex = "5F000000" },
                    
                    // Get HW Code
                    new ScenarioStep { Direction = StepDirection.HostToDevice, Label = "GetHwCode", DataHex = "50000000" },
                    new ScenarioStep { Direction = StepDirection.DeviceToHost, Label = "HwCodeResp", DataHex = "00008A00" } // HW Code: 0x8A00
                }
            };

            // 2. Setup Mock USB
            var mockDevice = new ScenarioUsbDevice(scenario);
            var engine = new MTKEngine(mockDevice);

            // 3. Execute
            var result = await engine.ConnectAsync();

            // 4. Assert
            Assert.True(result, "BROM Connection failed");
            Assert.True(mockDevice.Result.IsSuccessful, $"Scenario Replay Failed: {mockDevice.Result.ErrorMessage}");
        }

        [Fact]
        public async Task Brom_Watchdog_Timeout_ThrowsError()
        {
            // Scenario: Device sends nothing back
            var scenario = new ProtocolScenario
            {
                Name = "MTK_TIMEOUT",
                Steps = new List<ScenarioStep>
                {
                    new ScenarioStep { Direction = StepDirection.HostToDevice, Label = "StartCmd", DataHex = "A00A5005" },
                    new ScenarioStep { Action = StepAction.Timeout, Label = "TimeoutSim" }
                }
            };

            var mockDevice = new ScenarioUsbDevice(scenario);
            var engine = new MTKEngine(mockDevice);

            var result = await engine.ConnectAsync();
            
            // Should fail gracefully
            Assert.False(result);
        }

        [Fact]
        public async Task Brom_Fuzz_GarbageResponse_ShouldFailGracefully()
        {
            // Scenario: Device sends random garbage instead of handshake ack
            var scenario = new ProtocolScenario
            {
                Name = "MTK_FUZZ_GARBAGE",
                Steps = new List<ScenarioStep>
                {
                    new ScenarioStep { Direction = StepDirection.HostToDevice, Label = "StartCmd", DataHex = "A00A5005" },
                    // Respond with garbage
                    new ScenarioStep { Direction = StepDirection.DeviceToHost, Label = "GarbageAck", DataHex = "DEADBEEF00112233" }
                }
            };

            var mockDevice = new ScenarioUsbDevice(scenario);
            var engine = new MTKEngine(mockDevice);

            var result = await engine.ConnectAsync();

            Assert.False(result, "Engine accepted garbage handshake");
            Assert.True(mockDevice.Result.IsSuccessful, "Scenario replay should succeed (it delivered the garbage)");
        }
    }
}
