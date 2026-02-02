using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Infrastructure.Drivers
{
    public class DriverStoreManager
    {
        // SetupAPI P/Invokes
        [DllImport("setupapi.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern bool SetupCopyOEMInf(
            string SourceInfFileName,
            string OEMSourceMediaLocation,
            int OEMSourceMediaType,
            int CopyStyle,
            string DestinationInfFileName,
            int DestinationInfFileNameSize,
            ref int RequiredSize,
            string DestinationInfFileNameComponent);

        private const int SPOST_PATH = 1;
        private const int SP_COPY_NEWER = 0x4;

        public async Task<bool> InstallToDriverStoreAsync(string infPath)
        {
            return await Task.Run(() =>
            {
                if (!File.Exists(infPath))
                    return false;

                string destInf = new string('\0', 260);
                int destInfSize = 260;
                int reqSize = 0;

                // This registers the driver in C:\Windows\INF (as oemXX.inf) 
                // and puts it in the Driver Store.
                bool success = SetupCopyOEMInf(
                    infPath,
                    Path.GetDirectoryName(infPath) ?? string.Empty,
                    SPOST_PATH,
                    SP_COPY_NEWER,
                    destInf,
                    destInfSize,
                    ref reqSize,
                    null!);

                if (!success)
                {
                    int err = Marshal.GetLastWin32Error();
                    // Log error: 0xE000022F = File not found, etc.
                    return false;
                }

                return true;
            });
        }

        public async Task<bool> IsDigitallySignedAsync(string filePath)
        {
            // In a real implementation, would use WinVerifyTrust 
            // For this architecture demo, we check for presence of .cat file
            return await Task.Run(() =>
            {
                string catPath = Path.ChangeExtension(filePath, ".cat");
                return File.Exists(catPath);
            });
        }
    }
}
