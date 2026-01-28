using System;
using System.IO;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Engines
{
    public interface IProtocolEngine
    {
        string Name { get; }
        DeviceContext Context { get; }
        
        Task<bool> ConnectAsync(CancellationToken ct);
        Task<bool> DisconnectAsync();
        
        /// <summary>
        /// Reads a specific partition directly into a stream (Safe for large files).
        /// </summary>
        Task<bool> ReadPartitionToStreamAsync(string partitionName, Stream output, IProgress<ProgressUpdate> progress, CancellationToken ct);
        
        /// <summary>
        /// Writes a stream to a specific partition.
        /// </summary>
        Task<bool> WritePartitionFromStreamAsync(string partitionName, Stream input, IProgress<ProgressUpdate> progress, CancellationToken ct);

        Task<IEnumerable<PartitionInfo>> GetPartitionTableAsync();
        
        Task<bool> RebootAsync(string mode = "system");
    }
}
