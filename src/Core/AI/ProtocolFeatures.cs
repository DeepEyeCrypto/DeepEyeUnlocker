using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.AI
{
    public class ProtocolFeatures
    {
        public List<PacketCluster> Clusters { get; set; } = new();
        public Dictionary<int, byte[]> CommonPrefixes { get; set; } = new();
        public TimingAnalysis Timing { get; set; } = new();
        public EntropyProfile Entropy { get; set; } = new();
    }

    public class PacketCluster
    {
        public int Size { get; set; }
        public int Count { get; set; }
        public double AverageDelay { get; set; }
    }

    public class TimingAnalysis
    {
        public double MinInterPacketDelayMs { get; set; }
        public double MaxInterPacketDelayMs { get; set; }
        public double AverageInterPacketDelayMs { get; set; }
    }

    public class EntropyProfile
    {
        public double AverageEntropy { get; set; } // 0.0 to 1.0
        public bool IsLikelyEncrypted { get; set; }
    }
}
