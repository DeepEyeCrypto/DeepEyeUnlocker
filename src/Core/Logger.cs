using System;

namespace DeepEyeUnlocker.Core
{
    public static class Logger
    {
        public static void Info(string msg) => Console.WriteLine($"[INFO] {DateTime.Now:HH:mm:ss} {msg}");
        public static void Error(string msg) => Console.WriteLine($"[ERROR] {DateTime.Now:HH:mm:ss} {msg}");
        public static void Error(Exception ex, string msg) => Console.WriteLine($"[CRITICAL] {msg}: {ex.Message}");
        public static void Success(string msg) => Console.WriteLine($"[SUCCESS] {msg}");
    }
}
