using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xunit;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Simulation;
using DeepEyeUnlocker.Core.HIL;
using DeepEyeUnlocker.Tests.Mocks;

namespace DeepEyeUnlocker.Tests.Protocols.HIL
{
    [Trait("Category", "HIL")]
    public class HardwareFaultInjectorTests
    {
        [Fact]
        public void Safety_BlocksUnapprovedDevice()
        {
            // Arrange
            var scenario = new ProtocolScenario();
            using var usb = new ScenarioUsbDevice(scenario);
            var injector = new HardwareFaultInjector(usb, "PROD_DEVICE_123");

            // Act & Assert
            Assert.Throws<InvalidOperationException>(() => 
                injector.SetFault(FaultType.PacketDrop, 0));
        }

        [Fact]
        public void Safety_AllowsApprovedDevice()
        {
            // Arrange
            string deviceId = "TEST_DEVICE_456";
            FaultInjectionSafety.ApproveDevice(deviceId);
            var scenario = new ProtocolScenario();
            using var usb = new ScenarioUsbDevice(scenario);
            var injector = new HardwareFaultInjector(usb, deviceId);

            // Act
            injector.SetFault(FaultType.PacketDrop, 0); // Should not throw
            
            // Cleanup
            FaultInjectionSafety.ResetEmergencyStop();
        }

        [Fact]
        public void PacketDrop_ReturnsEmptyData()
        {
            // Arrange
            string deviceId = "TEST_DEVICE_789";
            FaultInjectionSafety.ApproveDevice(deviceId);
            
            var scenario = new ProtocolScenario {
                Steps = new List<ScenarioStep> {
                    new ScenarioStep { Label = "Step0", DataHex = "AABB", Direction = StepDirection.DeviceToHost }
                }
            };
            using var usb = new ScenarioUsbDevice(scenario);
            var injector = new HardwareFaultInjector(usb, deviceId);
            
            injector.SetFault(FaultType.PacketDrop, 0);

            // Act
            var reader = usb.OpenEndpointReader(LibUsbDotNet.Main.ReadEndpointID.Ep01);
            byte[] buffer = new byte[10];
            reader.Read(buffer, 1000, out int bytesRead);

            // Assert
            Assert.Equal(0, bytesRead);
        }

        [Fact]
        public void PacketCorruption_FlipsFirstByte()
        {
            // Arrange
            string deviceId = "TEST_DEVICE_ABC";
            FaultInjectionSafety.ApproveDevice(deviceId);
            
            var scenario = new ProtocolScenario {
                Steps = new List<ScenarioStep> {
                    new ScenarioStep { Label = "Step0", DataHex = "AABB", Direction = StepDirection.DeviceToHost }
                }
            };
            using var usb = new ScenarioUsbDevice(scenario);
            var injector = new HardwareFaultInjector(usb, deviceId);
            
            injector.SetFault(FaultType.PacketCorruption, 0);

            // Act
            var reader = usb.OpenEndpointReader(LibUsbDotNet.Main.ReadEndpointID.Ep01);
            byte[] buffer = new byte[2];
            reader.Read(buffer, 1000, out int bytesRead);

            // Assert
            Assert.Equal(2, bytesRead);
            Assert.Equal(0xAA ^ 0xFF, buffer[0]);
            Assert.Equal(0xBB, buffer[1]);
        }
    }
}
