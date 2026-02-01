using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.HIL;

namespace DeepEyeUnlocker.Operations.HIL
{
    public class ConversionOptions
    {
        public bool RedactUserData { get; set; } = true;
        public int TimingToleranceMs { get; set; } = 10;
        public string DeviceName { get; set; } = "CapturedDevice";
    }

    public class PcapToScenarioConverter
    {
        public ProtocolScenario Convert(IEnumerable<UsbPacket> packets, string protocol, ConversionOptions options)
        {
            var scenario = new ProtocolScenario
            {
                Name = $"{options.DeviceName}_{protocol}_{DateTime.Now:yyyyMMdd}",
                Protocol = protocol,
                Description = $"Auto-captured {protocol} scenario from real hardware.",
                Steps = new List<ScenarioStep>()
            };

            long lastTimestamp = 0;
            foreach (var packet in packets)
            {
                string label = IdentifyLabel(packet, protocol);
                var step = new ScenarioStep
                {
                    Direction = packet.Direction == UsbDirection.HostToDevice ? StepDirection.HostToDevice : StepDirection.DeviceToHost,
                    Label = label,
                    DataHex = System.Convert.ToHexString(packet.Data)
                };

                if (lastTimestamp != 0)
                {
                    long delayUs = packet.TimestampUs - lastTimestamp;
                    step.DelayMs = (int)(delayUs / 1000);
                }

                if (options.RedactUserData)
                {
                    step.DataHex = SanitizeData(step.DataHex, protocol);
                }

                scenario.Steps.Add(step);
                lastTimestamp = packet.TimestampUs;
            }

            return scenario;
        }

        private string IdentifyLabel(UsbPacket packet, string protocol)
        {
            if (packet.Label != null) return packet.Label;

            string protoLower = protocol.ToLower();
            if (protoLower == "sahara") return IdentifySaharaLabel(packet);
            if (protoLower == "firehose") return IdentifyFirehoseLabel(packet);

            return $"Step_{Guid.NewGuid().ToString().Substring(0, 4)}";
        }

        private string IdentifySaharaLabel(UsbPacket packet)
        {
            if (packet.Data.Length < 4) return "Sahara_Fragment";
            uint command = BitConverter.ToUInt32(packet.Data, 0);

            return command switch
            {
                0x01 => "Sahara_Hello",
                0x02 => "Sahara_HelloResponse",
                0x03 => "Sahara_ReadData",
                0x04 => "Sahara_ReadDataResponse",
                0x05 => "Sahara_EndTransfer",
                0x06 => "Sahara_Done",
                0x07 => "Sahara_DoneResponse",
                0x08 => "Sahara_Reset",
                0x09 => "Sahara_ResetResponse",
                _ => $"Sahara_Cmd_0x{command:X2}"
            };
        }

        private string IdentifyFirehoseLabel(UsbPacket packet)
        {
            try
            {
                string text = Encoding.UTF8.GetString(packet.Data).Trim();
                if (text.StartsWith("<?xml"))
                {
                    if (text.Contains("<configure")) return "Firehose_Config";
                    if (text.Contains("<read")) return "Firehose_ReadCmd";
                    if (text.Contains("<program")) return "Firehose_WriteCmd";
                    if (text.Contains("<erase")) return "Firehose_EraseCmd";
                    if (text.Contains("<getstorageinfo")) return "Firehose_StorageInfoCmd";
                    if (text.Contains("ACK")) return "Firehose_Ack";
                    if (text.Contains("NAK")) return "Firehose_Nak";
                    return "Firehose_Xml";
                }
                return packet.Data.Length == 512 || packet.Data.Length == 1024 || packet.Data.Length == 4096 
                    ? "Firehose_DataPayload" 
                    : "Firehose_RawData";
            }
            catch { return "Firehose_Binary"; }
        }

        private string SanitizeData(string hex, string protocol)
        {
            // Simple redaction: if it looks like a large chunk of non-zero data in Firehose write or Sahara upload, zero it out.
            // In a real implementation, we'd check partition names in Firehose XML.
            if (hex.Length > 1024) 
            {
                return new string('0', hex.Length);
            }
            return hex;
        }
    }
}
