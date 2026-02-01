using System;
using System.Collections.Generic;
using System.Linq;
using DeepEyeUnlocker.Infrastructure.HIL;

namespace DeepEyeUnlocker.Core.AI
{
    public class PcapFeatureExtractor
    {
        public ProtocolFeatures ExtractFeatures(IEnumerable<UsbPacket> packets)
        {
            var packetList = packets.ToList();
            if (!packetList.Any()) return new ProtocolFeatures();

            var features = new ProtocolFeatures
            {
                Timing = AnalyzeTiming(packetList),
                Entropy = AnalyzeEntropy(packetList),
                Clusters = ClusterBySize(packetList)
            };

            // Identify common headers (first 4 bytes frequently repeated)
            features.CommonPrefixes = packetList
                .Where(p => p.Data.Length >= 4)
                .GroupBy(p => BitConverter.ToInt32(p.Data, 0))
                .Where(g => g.Count() > 2)
                .ToDictionary(g => g.Key, g => g.First().Data.Take(4).ToArray());

            return features;
        }

        private TimingAnalysis AnalyzeTiming(List<UsbPacket> packets)
        {
            var deltas = new List<double>();
            for (int i = 1; i < packets.Count; i++)
            {
                deltas.Add((packets[i].TimestampUs - packets[i-1].TimestampUs) / 1000.0); // Us to Ms
            }

            if (!deltas.Any()) return new TimingAnalysis();

            return new TimingAnalysis
            {
                MinInterPacketDelayMs = deltas.Min(),
                MaxInterPacketDelayMs = deltas.Max(),
                AverageInterPacketDelayMs = deltas.Average()
            };
        }

        private EntropyProfile AnalyzeEntropy(List<UsbPacket> packets)
        {
            var allData = packets.SelectMany(p => p.Data).ToArray();
            if (allData.Length == 0) return new EntropyProfile();

            double entropy = CalculateShannonEntropy(allData);
            return new EntropyProfile
            {
                AverageEntropy = entropy,
                IsLikelyEncrypted = entropy > 7.5 // 0-8 bits scale
            };
        }

        private double CalculateShannonEntropy(byte[] data)
        {
            var counts = data.GroupBy(b => b).ToDictionary(g => g.Key, g => g.Count());
            double entropy = 0;
            foreach (var count in counts.Values)
            {
                double p = (double)count / data.Length;
                entropy -= p * Math.Log2(p);
            }
            return entropy;
        }

        private List<PacketCluster> ClusterBySize(List<UsbPacket> packets)
        {
            return packets.GroupBy(p => p.Data.Length)
                .Select(g => new PacketCluster 
                { 
                    Size = g.Key, 
                    Count = g.Count(),
                    AverageDelay = 0 // Complexity omitted for simplicity
                })
                .OrderByDescending(c => c.Count)
                .ToList();
        }
    }
}
