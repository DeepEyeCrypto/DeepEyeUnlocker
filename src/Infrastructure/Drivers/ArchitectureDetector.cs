using System;
using System.Runtime.InteropServices;

namespace DeepEyeUnlocker.Infrastructure.Drivers
{
    public enum SystemArchitecture
    {
        Windows32,
        Windows64_Native,
        Windows64_Wow64
    }

    public class ArchitectureDetector
    {
        [DllImport("kernel32.dll", SetLastError = true, CallingConvention = CallingConvention.Winapi)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool IsWow64Process([In] IntPtr process, [Out] out bool wow64Process);

        public SystemArchitecture Detect()
        {
            if (!Environment.Is64BitOperatingSystem)
                return SystemArchitecture.Windows32;

            if (IsWow64())
                return SystemArchitecture.Windows64_Wow64;

            return SystemArchitecture.Windows64_Native;
        }

        private bool IsWow64()
        {
            if (IsWow64Process(System.Diagnostics.Process.GetCurrentProcess().Handle, out bool isWow64))
            {
                return isWow64;
            }
            return false;
        }

        public string GetDriverSourcePath(string basePath)
        {
            var arch = Detect();
            return arch switch
            {
                SystemArchitecture.Windows32 => System.IO.Path.Combine(basePath, "x86"),
                _ => System.IO.Path.Combine(basePath, "x64") // Both Wow64 and Native64 need x64 drivers
            };
        }
    }
}
