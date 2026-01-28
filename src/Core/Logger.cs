using System;
using System.Collections.Generic;
using System.Linq;
using DeepEyeUnlocker.Infrastructure.Logging;
using DeepEyeUnlocker.Infrastructure.Logging.Sinks;

namespace DeepEyeUnlocker.Core;

public static class Logger
{
    private static readonly List<ILogSink> Sinks = new();
    private static readonly object Lock = new();

    static Logger()
    {
        // Default sinks
        Sinks.Add(new ConsoleLogSink());
        Sinks.Add(new FileLogSink("DeepEyeUnlocker"));
    }

    public static void AddSink(ILogSink sink)
    {
        lock (Lock) Sinks.Add(sink);
    }

    public static void Log(LogLevel level, string msg, string category = "System", Exception? ex = null)
    {
        var entry = new LogEntry 
        { 
            Level = level, 
            Message = msg, 
            Category = category, 
            Exception = ex 
        };

        lock (Lock)
        {
            foreach (var sink in Sinks) sink.Write(entry);
        }
    }

    public static void Info(string msg, string category = "System") => Log(LogLevel.Info, msg, category);
    public static void Success(string msg, string category = "System") => Log(LogLevel.Info, "[SUCCESS] " + msg, category);
    public static void Error(string msg, string category = "System") => Log(LogLevel.Error, msg, category);
    public static void Error(Exception ex, string msg, string category = "System") => Log(LogLevel.Error, msg, category, ex);
    public static void Warn(string msg, string category = "System") => Log(LogLevel.Warn, msg, category);
    public static void Warning(string msg, string category = "System") => Warn(msg, category);
    public static void Debug(string msg, string category = "System") => Log(LogLevel.Debug, msg, category);
    public static void Trace(string msg, string category = "System") => Log(LogLevel.Trace, msg, category);
    public static void Critical(string msg, string category = "System") => Log(LogLevel.Critical, msg, category);
}
