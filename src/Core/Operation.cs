using System;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core
{
    public abstract class Operation
    {
        public string Name { get; protected set; }
        public int Progress { get; protected set; }
        public string Status { get; protected set; } = "Idle";

        public abstract Task<bool> ExecuteAsync(Device device);

        protected void ReportProgress(int progress, string status)
        {
            Progress = progress;
            Status = status;
            ProgressChanged?.Invoke(this, new ProgressChangedEventArgs(progress, status));
        }

        public event EventHandler<ProgressChangedEventArgs>? ProgressChanged;
    }

    public class ProgressChangedEventArgs : EventArgs
    {
        public int Progress { get; }
        public string Status { get; }

        public ProgressChangedEventArgs(int progress, string status)
        {
            Progress = progress;
            Status = status;
        }
    }
}
