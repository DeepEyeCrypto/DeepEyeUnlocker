using System;
using DeepEyeUnlocker.Infrastructure.Logging;
using DeepEyeUnlocker.Infrastructure.Logging.Sinks;

namespace DeepEye.UI.Modern.Infrastructure
{
    public class UILogSink : ILogSink
    {
        private readonly Action<string> _onLog;

        public UILogSink(Action<string> onLog)
        {
            _onLog = onLog;
        }

        public void Write(LogEntry entry)
        {
            string time = DateTime.Now.ToString("HH:mm:ss");
            string levelPrefix = entry.Level switch
            {
                LogLevel.Error => "[ERROR] ",
                LogLevel.Warn => "[WARN ] ",
                LogLevel.Info => "",
                _ => $"[{entry.Level.ToString().ToUpper()}] "
            };

            string logLine = $"[{time}] {levelPrefix}{entry.Message}";
            _onLog?.Invoke(logLine);
        }
    }
}
