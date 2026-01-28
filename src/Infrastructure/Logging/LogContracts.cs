using System;

namespace DeepEyeUnlocker.Infrastructure.Logging
{
    public enum LogLevel { Trace, Debug, Info, Warn, Error, Critical }

    public class LogEntry
    {
        public DateTime Timestamp { get; } = DateTime.Now;
        public LogLevel Level { get; set; }
        public string Category { get; set; } = "System";
        public string Message { get; set; } = string.Empty;
        public Exception? Exception { get; set; }

        public override string ToString() => $"[{Timestamp:HH:mm:ss}] [{Level}] [{Category}] {Message}";
    }

    public interface ILogSink
    {
        void Write(LogEntry entry);
    }
}
