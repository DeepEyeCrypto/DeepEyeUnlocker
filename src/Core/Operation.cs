using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core
{
    public abstract class Operation
    {
        public string Name { get; protected set; } = string.Empty;
        public bool IsRunning { get; private set; }

        public abstract Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct);

        protected void Report(IProgress<ProgressUpdate> p, int pct, string status, LogLevel level = LogLevel.Info)
        {
            p?.Report(new ProgressUpdate 
            { 
                Percentage = pct, 
                Status = status, 
                Level = level,
                Category = Name
            });
        }
    }
}
