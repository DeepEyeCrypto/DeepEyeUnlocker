using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Scenarios
{
    public abstract class OperationScenario
    {
        public string Name { get; protected set; } = "Generic Operation";
        public string Description { get; protected set; } = string.Empty;

        public abstract Task<ScenarioResult> RunAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct);

        protected void Report(IProgress<ProgressUpdate> progress, int percent, string message)
        {
            progress?.Report(ProgressUpdate.Info(percent, message));
        }
    }

    public class ScenarioResult
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public Dictionary<string, string> Metadata { get; set; } = new();

        public static ScenarioResult Fail(string msg) => new ScenarioResult { Success = false, Message = msg };
        public static ScenarioResult Ok(string msg) => new ScenarioResult { Success = true, Message = msg };
    }
}
