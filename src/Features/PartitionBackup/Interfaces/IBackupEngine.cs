using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.PartitionBackup.Models;

namespace DeepEyeUnlocker.Features.PartitionBackup.Interfaces
{
    public interface IBackupEngine
    {
        Task<bool> StartBackupAsync(DeviceContext device, string partitionName, Stream outputStream, IProgress<ProgressUpdate> progress, CancellationToken ct);
        Task<bool> VerifyBackupAsync(string filePath, string deviceSerial);
    }
}
