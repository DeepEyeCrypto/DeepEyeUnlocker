using System;
using System.Runtime.InteropServices;

namespace DeepEyeUnlocker.Core.Engines
{
    public static class PortableEngineNative
    {
        private const string LibName = "deepeye_core";

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr DeepEye_CreateTransport();

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern void DeepEye_DestroyTransport(IntPtr transport);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern bool DeepEye_TransportOpen(IntPtr transport, int fd);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern void DeepEye_TransportClose(IntPtr transport);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr DeepEye_CreateEngine(IntPtr transport);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern void DeepEye_DestroyEngine(IntPtr engine);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern bool DeepEye_EngineIdentify(IntPtr engine);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern bool DeepEye_EngineDumpPartition(IntPtr engine, string name, string outPath);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern bool DeepEye_EngineFlashPartition(IntPtr engine, string name, string inPath);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern bool DeepEye_EngineErasePartition(IntPtr engine, string name);

        [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int DeepEye_EngineGetPartitions(IntPtr engine, System.Text.StringBuilder outBuffer, int bufferSize);
    }
}
