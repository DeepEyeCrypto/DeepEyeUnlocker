using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using DeepEyeUnlocker.Infrastructure.HIL;

namespace DeepEyeUnlocker.Core.AI
{
    public class ProtocolAnalysisPrompt
    {
        public string BuildPrompt(List<UsbPacket> packets, ProtocolFeatures features)
        {
            var sb = new StringBuilder();
            sb.AppendLine("Analyze these USB packets from a mobile device in bootloader mode.");
            sb.AppendLine();
            sb.AppendLine("### PACKET DUMP (Sample):");
            foreach (var p in packets.Take(20))
            {
                string hex = BitConverter.ToString(p.Data.Take(32).ToArray()).Replace("-", "");
                sb.AppendLine($"[{p.TimestampUs}us] {p.Direction} -> {hex} (Len: {p.Data.Length})");
            }

            sb.AppendLine();
            sb.AppendLine("### STATISTICAL FEATURES:");
            sb.AppendLine($"- Entropy Score: {features.Entropy.AverageEntropy:F2} (Likely Encrypted: {features.Entropy.IsLikelyEncrypted})");
            sb.AppendLine($"- Timing: Avg Delay {features.Timing.AverageInterPacketDelayMs:F2}ms");
            sb.AppendLine($"- Common Headers: {string.Join(", ", features.CommonPrefixes.Values.Select(b => BitConverter.ToString(b).Replace("-", "")))}");
            
            sb.AppendLine();
            sb.AppendLine("### TASK:");
            sb.AppendLine("1. Identify the protocol type (Sahara, Firehose, Odin, Preloader, Unknown).");
            sb.AppendLine("2. Segment packets into logical steps.");
            sb.AppendLine("3. Identify packet structure (Header size, Command ID offset, Length offset).");
            sb.AppendLine("4. Map the state machine transitions.");
            
            sb.AppendLine();
            sb.AppendLine("### OUTPUT FORMAT (STRICT JSON):");
            sb.AppendLine("{ \"protocol_type\": \"...\", \"confidence\": 0.0, \"steps\": [...], \"state_machine\": {} }");

            return sb.ToString();
        }
    }
}
