using System;
using System.IO;

namespace DeepEyeUnlocker.Core;

public static class Logger
{
    private static readonly string LogFile = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "DeepEyeUnlocker", "logs", $"{DateTime.Now:yyyy-MM-dd}.log");

    public static void Info(string msg) => Write("INFO", msg);
    public static void Success(string msg) => Write("SUCCESS", msg);
    public static void Error(string msg) => Write("ERROR", msg);
    public static void Error(Exception ex, string msg) => Write("ERROR", $"{msg} | {ex.Message}");
    public static void Warn(string msg) => Write("WARN", msg);
    public static void Warn(Exception ex, string msg) => Write("WARN", $"{msg} | {ex.Message}");
    public static void Warning(string msg) => Warn(msg);
    public static void Debug(string msg) => Write("DEBUG", msg);
    public static void Debug(Exception ex, string msg) => Write("DEBUG", $"{msg} | {ex.Message}");

    private static void Write(string level, string msg)
    {
        var timestamp = DateTime.Now.ToString("HH:mm:ss");
        var logEntry = $"[{timestamp}] [{level}] {msg}";
        
        Console.WriteLine(logEntry);
        
        try
        {
            var logDir = Path.GetDirectoryName(LogFile);
            if (!string.IsNullOrEmpty(logDir) && !Directory.Exists(logDir))
                Directory.CreateDirectory(logDir);
            
            File.AppendAllText(LogFile, logEntry + Environment.NewLine);
        }
        catch { /* Ignore file write errors */ }
    }
}
