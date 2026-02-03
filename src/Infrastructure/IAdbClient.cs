using System;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Infrastructure
{
    public interface IAdbClient
    {
        Task<string> ExecuteShellAsync(string command, CancellationToken ct = default);
        Task<System.IO.Stream> OpenShellStreamAsync(string command, CancellationToken ct = default);
        Task<System.IO.Stream> OpenShellWritableStreamAsync(string command, CancellationToken ct = default);
        Task<bool> PushFileAsync(string localPath, string remotePath, CancellationToken ct = default);
        Task<bool> PullFileAsync(string remotePath, string localPath, CancellationToken ct = default);
        Task<bool> InstallPackageAsync(string apkPath, CancellationToken ct = default);
        Task<string> RunAdbCommandAsync(string args, CancellationToken ct = default);
        Task RebootAsync(CancellationToken ct = default);
        bool IsConnected();
        Task<bool> HasRootAsync(CancellationToken ct = default);
        string? TargetSerial { get; set; }
    }
}
