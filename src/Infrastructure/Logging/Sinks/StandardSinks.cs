using System;
using System.IO;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Infrastructure.Logging.Sinks
{
    public class FileLogSink : ILogSink
    {
        private readonly string _logPath;
        private readonly object _lock = new object();

        public FileLogSink(string appName)
        {
            _logPath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                appName, "logs", $"{DateTime.Now:yyyy-MM-dd}.log");
            
            var directory = Path.GetDirectoryName(_logPath);
            if (directory != null) Directory.CreateDirectory(directory);
        }

        public void Write(LogEntry entry)
        {
            lock (_lock)
            {
                try
                {
                    File.AppendAllText(_logPath, entry.ToString() + Environment.NewLine);
                }
                catch { /* Fail silently */ }
            }
        }
    }

    public class ConsoleLogSink : ILogSink
    {
        public void Write(LogEntry entry)
        {
            var originalColor = Console.ForegroundColor;
            Console.ForegroundColor = GetColor(entry.Level);
            Console.WriteLine(entry.ToString());
            Console.ForegroundColor = originalColor;
        }

        private ConsoleColor GetColor(LogLevel level) => level switch
        {
            LogLevel.Trace => ConsoleColor.Gray,
            LogLevel.Debug => ConsoleColor.DarkGray,
            LogLevel.Info => ConsoleColor.White,
            LogLevel.Warn => ConsoleColor.Yellow,
            LogLevel.Error => ConsoleColor.Red,
            LogLevel.Critical => ConsoleColor.DarkRed,
            _ => ConsoleColor.White
        };
    }
}
