using System;

namespace DeepEyeUnlocker.Core.Models
{
    public enum LogLevel
    {
        Trace,
        Debug,
        Info,
        Warn,
        Error,
        Critical
    }

    public class ProgressUpdate
    {
        public int Percentage { get; set; }
        public string Status { get; set; } = string.Empty;
        public string Message => Status; // Alias for Status
        public LogLevel Level { get; set; } = LogLevel.Info;
        public string Category { get; set; } = "General";
        public DateTime Timestamp { get; } = DateTime.Now;

        public static ProgressUpdate Info(int percentage, string status) 
            => new ProgressUpdate { Percentage = percentage, Status = status, Level = LogLevel.Info };

        public static ProgressUpdate Warning(int percentage, string status) 
            => new ProgressUpdate { Percentage = percentage, Status = status, Level = LogLevel.Warn };

        public static ProgressUpdate Error(int percentage, string status) 
            => new ProgressUpdate { Percentage = percentage, Status = status, Level = LogLevel.Error };
    }
}
