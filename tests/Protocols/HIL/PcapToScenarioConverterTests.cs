using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Xunit;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.HIL;
using DeepEyeUnlocker.Operations.HIL;
using DeepEyeUnlocker.Tests.Mocks;

namespace DeepEyeUnlocker.Tests.Protocols.HIL
{
    public class PcapToScenarioConverterTests
    {
        [Fact]
        public void Sahara_Conversion_CreatesCorrectLabels()
        {
            // Arrange
            var mutator = new PcapToScenarioConverter();
            var options = new ConversionOptions { DeviceName = "TestDevice" };
            
            var packets = new List<UsbPacket>
            {
                new UsbPacket { TimestampUs = 1000, Data = new byte[] { 0x01, 0x00, 0x00, 0x00, 0x30, 0x00, 0x00, 0x00 }, Direction = UsbDirection.DeviceToHost, Endpoint = 1 }, // Hello
                new UsbPacket { TimestampUs = 5000, Data = new byte[] { 0x02, 0x00, 0x00, 0x00, 0x30, 0x00, 0x00, 0x00 }, Direction = UsbDirection.HostToDevice, Endpoint = 1 }  // HelloResponse
            };

            // Act
            var scenario = mutator.Convert(packets, "Sahara", options);

            // Assert
            Assert.Equal(2, scenario.Steps.Count);
            Assert.Equal("Sahara_Hello", scenario.Steps[0].Label);
            Assert.Equal("Sahara_HelloResponse", scenario.Steps[1].Label);
            Assert.Equal(4, scenario.Steps[1].DelayMs); // 5000us - 1000us = 4000us = 4ms
        }

        [Fact]
        public void Firehose_Conversion_CreatesCorrectLabels()
        {
            // Arrange
            var mutator = new PcapToScenarioConverter();
            var options = new ConversionOptions { DeviceName = "TestDevice" };
            
            string configXml = "<?xml version=\"1.0\" ?><data><configure MemoryName=\"emmc\" /></data>";
            string ackXml = "<?xml version=\"1.0\" ?><data><response value=\"ACK\" /></data>";
            
            var packets = new List<UsbPacket>
            {
                new UsbPacket { TimestampUs = 1000, Data = Encoding.UTF8.GetBytes(configXml), Direction = UsbDirection.HostToDevice, Endpoint = 1 },
                new UsbPacket { TimestampUs = 2000, Data = Encoding.UTF8.GetBytes(ackXml), Direction = UsbDirection.DeviceToHost, Endpoint = 1 }
            };

            // Act
            var scenario = mutator.Convert(packets, "Firehose", options);

            // Assert
            Assert.Equal(2, scenario.Steps.Count);
            Assert.Equal("Firehose_Config", scenario.Steps[0].Label);
            Assert.Equal("Firehose_Ack", scenario.Steps[1].Label);
        }

        [Fact]
        public void Redaction_ZerosLargePayloads()
        {
            // Arrange
            var mutator = new PcapToScenarioConverter();
            var options = new ConversionOptions { RedactUserData = true };
            
            byte[] largeData = new byte[2048];
            new Random().NextBytes(largeData);
            
            var packets = new List<UsbPacket>
            {
                new UsbPacket { TimestampUs = 1000, Data = largeData, Direction = UsbDirection.DeviceToHost, Endpoint = 1 }
            };

            // Act
            var scenario = mutator.Convert(packets, "Firehose", options);

            // Assert
            string expectedHex = new string('0', largeData.Length * 2);
            Assert.Equal(expectedHex, scenario.Steps[0].DataHex);
        }
    }
}
