using System;
using System.IO;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Engines;

namespace DeepEyeUnlocker.Protocols
{
    public interface IProtocol : IProtocolEngine
    {
        // Legacy support
        Task<byte[]> ReadPartitionAsync(string partitionName);
        Task<bool> WritePartitionAsync(string partitionName, byte[] data);
    }
}
