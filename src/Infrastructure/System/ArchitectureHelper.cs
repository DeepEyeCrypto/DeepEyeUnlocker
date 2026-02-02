using System;
using System.Diagnostics;
using System.Runtime.InteropServices;

namespace DeepEyeUnlocker.Infrastructure.Native
{
    public static class ArchitectureHelper
    {
        [DllImport("kernel32.dll", SetLastError = true, CallingConvention = CallingConvention.Winapi)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool IsWow64Process(
            [In] IntPtr hProcess,
            [Out, MarshalAs(UnmanagedType.Bool)] out bool lpSystemInfo);

        /// <summary>
        /// Checks if the current process is running under WOW64 (32-bit on 64-bit OS)
        /// </summary>
        public static bool IsRunningUnderWow64()
        {
            if (IntPtr.Size == 8) return false; // Native 64-bit

            if (IsWow64Process(Process.GetCurrentProcess().Handle, out bool isWow64))
            {
                return isWow64;
            }

            return false;
        }

        /// <summary>
        /// Gets the target driver architecture (x64 or x86)
        /// </summary>
        public static string GetDriverArchitecture()
        {
            if (IntPtr.Size == 8 || IsRunningUnderWow64())
            {
                return "x64";
            }
            return "x86";
        }
    }
}
