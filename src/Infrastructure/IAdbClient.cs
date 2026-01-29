using System;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Infrastructure
{
    public interface IAdbClient
    {
        Task<string> ExecuteShellAsync(string command);
        Task<bool> PushFileAsync(string localPath, string remotePath);
        Task<bool> PullFileAsync(string remotePath, string localPath);
        Task<bool> InstallPackageAsync(string apkPath);
        Task RebootAsync();
        bool IsConnected();
        string? TargetSerial { get; set; }
    }
}
