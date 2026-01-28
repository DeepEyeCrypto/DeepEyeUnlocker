using System;
using System.IO;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Helpers
{
    public static class FileHelper
    {
        public static async Task<byte[]> ReadAllBytesAsync(string path)
        {
            return await File.ReadAllBytesAsync(path);
        }

        public static async Task WriteAllBytesAsync(string path, byte[] data)
        {
            await File.WriteAllBytesAsync(path, data);
        }

        public static string GetReadableFileSize(long bytes)
        {
            string[] units = { "B", "KB", "MB", "GB", "TB" };
            double size = bytes;
            int unitIndex = 0;
            while (size >= 1024 && unitIndex < units.Length - 1)
            {
                size /= 1024;
                unitIndex++;
            }
            return $"{size:F2} {units[unitIndex]}";
        }
    }
}
