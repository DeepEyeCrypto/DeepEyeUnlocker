using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core
{
    public class PartitionInfo
    {
        public string Name { get; set; } = "";
        public long Size { get; set; }
        public long StartAddress { get; set; }
        public string Flags { get; set; } = "";
    }
}
