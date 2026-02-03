using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Engines
{
    public class PortableEngine : Protocols.IProtocol, IDisposable
    {
        private IntPtr _transportHandle;
        private IntPtr _engineHandle;
        private readonly DeviceContext _context;

        public string Name => "DeepEye Portable Engine";
        public DeviceContext Context => _context;

        public PortableEngine(DeviceContext context)
        {
            _context = context;
            _transportHandle = PortableEngineNative.DeepEye_CreateTransport();
            _engineHandle = PortableEngineNative.DeepEye_CreateEngine(_transportHandle);
        }

        public Task<bool> ConnectAsync(CancellationToken ct)
        {
            // On Windows, 'fd' might be mocked or we might need a different Open method
            // for raw USB access. For now, we use a placeholder FD.
            bool success = PortableEngineNative.DeepEye_TransportOpen(_transportHandle, 0);
            return Task.FromResult(success);
        }

        public Task<bool> DisconnectAsync()
        {
            PortableEngineNative.DeepEye_TransportClose(_transportHandle);
            return Task.FromResult(true);
        }

        public Task<bool> ReadPartitionToStreamAsync(string partitionName, Stream output, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            // Simplified: The C++ core currently writes to path. 
            // In a full implementation, we'd use a callback or temporary file.
            string tempPath = Path.GetTempFileName();
            bool success = PortableEngineNative.DeepEye_EngineDumpPartition(_engineHandle, partitionName, tempPath);
            
            if (success)
            {
                using (var fs = File.OpenRead(tempPath))
                {
                    fs.CopyTo(output);
                }
                File.Delete(tempPath);
            }
            
            return Task.FromResult(success);
        }

        public Task<bool> WritePartitionFromStreamAsync(string partitionName, Stream input, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            string tempPath = Path.GetTempFileName();
            using (var fs = File.Create(tempPath))
            {
                input.CopyTo(fs);
            }

            bool success = PortableEngineNative.DeepEye_EngineFlashPartition(_engineHandle, partitionName, tempPath);
            File.Delete(tempPath);
            
            return Task.FromResult(success);
        }

        public Task<bool> ErasePartitionAsync(string partitionName, IProgress<ProgressUpdate>? progress, CancellationToken ct)
        {
            bool success = PortableEngineNative.DeepEye_EngineErasePartition(_engineHandle, partitionName);
            return Task.FromResult(success); 
        }

        public Task<IEnumerable<PartitionInfo>> GetPartitionTableAsync()
        {
            var collection = new List<PartitionInfo>();
            var sb = new System.Text.StringBuilder(8192);
            int len = PortableEngineNative.DeepEye_EngineGetPartitions(_engineHandle, sb, 8192);

            if (len > 0)
            {
                var lines = sb.ToString().Split(new[] { '\n' }, StringSplitOptions.RemoveEmptyEntries);
                foreach (var line in lines)
                {
                    var parts = line.Split('|');
                    if (parts.Length == 2)
                    {
                        collection.Add(new PartitionInfo 
                        { 
                            Name = parts[0], 
                            SizeInBytes = ulong.Parse(parts[1]) 
                        });
                    }
                }
            }
            
            return Task.FromResult<IEnumerable<PartitionInfo>>(collection);
        }

        public Task<bool> RebootAsync(string mode = "system")
        {
            return Task.FromResult(true);
        }

        public async Task<byte[]> ReadPartitionAsync(string partitionName)
        {
            using (var ms = new MemoryStream())
            {
                if (await ReadPartitionToStreamAsync(partitionName, ms, null!, CancellationToken.None))
                {
                    return ms.ToArray();
                }
            }
            return Array.Empty<byte>();
        }
        
        public async Task<bool> WritePartitionAsync(string partitionName, byte[] data)
        {
            using (var ms = new MemoryStream(data))
            {
                return await WritePartitionFromStreamAsync(partitionName, ms, null!, CancellationToken.None);
            }
        }

        public void Dispose()
        {
            if (_engineHandle != IntPtr.Zero)
            {
                PortableEngineNative.DeepEye_DestroyEngine(_engineHandle);
                _engineHandle = IntPtr.Zero;
            }
            if (_transportHandle != IntPtr.Zero)
            {
                PortableEngineNative.DeepEye_DestroyTransport(_transportHandle);
                _transportHandle = IntPtr.Zero;
            }
        }
    }
}
