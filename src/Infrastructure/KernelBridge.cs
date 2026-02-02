using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Infrastructure
{
    /// <summary>
    /// Commands supported by the DeepEye Kernel Module (deepeye_kernel.ko)
    /// </summary>
    public enum KernelCommand : uint
    {
        DEEPEYE_HIDE_ROOT = 0x4001,
        DEEPEYE_UNHIDE_ROOT = 0x4002,
        DEEPEYE_PATCH_BOOT = 0x4003,
        DEEPEYE_CHECK_SAFETY = 0x4004
    }

    /// <summary>
    /// Direct P/Invoke bridge for when the tool runs natively on the device.
    /// </summary>
    internal static class NativeKernelMethods
    {
        private const string KernelLib = "libdeepeye_kernel.so";

        [DllImport(KernelLib, CallingConvention = CallingConvention.Cdecl)]
        public static extern int deepeye_init();

        [DllImport(KernelLib, CallingConvention = CallingConvention.Cdecl)]
        public static extern int deepeye_load_module(string module_path);

        [DllImport(KernelLib, CallingConvention = CallingConvention.Cdecl)]
        public static extern int deepeye_hide_root(int pid);
    }

    /// <summary>
    /// Bridge between the C# tool and Android Kernel space via IOCTL simulation.
    /// </summary>
    public class KernelBridge : IDisposable
    {
        private readonly IAdbClient _adbClient;

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Ansi)]
        public struct HideRootArgs
        {
            public int Pid;
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
            public string PackageName;
            public int Flags;
        }

        public KernelBridge(IAdbClient adbClient)
        {
            _adbClient = adbClient ?? throw new ArgumentNullException(nameof(adbClient));
        }

        /// <summary>
        /// Sends an IOCTL command to the /dev/deepeye device node.
        /// This is abstracted through an ADB proxy binary on the device.
        /// </summary>
        public bool SendIoctl(KernelCommand cmd, object? args = null)
        {
            string cmdName = cmd.ToString();
            Console.WriteLine($"[KernelBridge] Executing {cmdName}...");

            string extraArgs = "";
            if (args is HideRootArgs hideArgs)
            {
                extraArgs = $"--pid {hideArgs.Pid} --pkg {hideArgs.PackageName}";
            }

            // High-level abstraction: deepeye_cli is our native proxy on Android
            var fullCmd = $"deepeye_cli --ioctl {(uint)cmd} {extraArgs}";
            var result = _adbClient.ExecuteShellAsync(fullCmd).GetAwaiter().GetResult();

            return result.Contains("IOCTL_SUCCESS");
        }

        public bool HideRoot(int pid, string packageName)
        {
            var args = new HideRootArgs 
            { 
                Pid = pid, 
                PackageName = packageName, 
                Flags = 1 // Stealth Mode 
            };
            return SendIoctl(KernelCommand.DEEPEYE_HIDE_ROOT, args);
        }

        public bool PatchBootImage(string inputPath, string outputPath)
        {
            Console.WriteLine($"[KernelBridge] Patching boot image via native module...");
            var result = _adbClient.ExecuteShellAsync($"deepeye_native --patch {inputPath} {outputPath}").GetAwaiter().GetResult();
            return result.Contains("Patched image saved");
        }

        public void Dispose()
        {
            // Resource cleanup if necessary
        }
    }
}
