using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.Architecture
{
    public class OperationOrchestrator
    {
        public event EventHandler<OperationProgressEventArgs>? ProgressUpdated;
        public event EventHandler<OperationResult>? OperationCompleted;

        public async Task<OperationResult> RunOperationAsync(
            DeviceContext ctx, 
            IOperationHandler handler, 
            Dictionary<string, object> parameters,
            CancellationToken ct = default)
        {
            var result = new OperationResult();
            
            try
            {
                // 1. Safety Pre-flight
                if (!await SafetyInterlock.PreFlightCheckAsync(ctx, handler))
                {
                    result.Success = false;
                    result.Message = "Safety check failed. Operation aborted.";
                    return result;
                }

                // 2. Execute Handler
                NotifyProgress(handler.OperationName, 0, "Starting operation...");
                result = await handler.ExecuteAsync(ctx, parameters);
                
                NotifyProgress(handler.OperationName, 100, "Operation complete.");
            }
            catch (Exception ex)
            {
                result.Success = false;
                result.Message = $"Internal Error: {ex.Message}";
                result.Logs.Add(ex.ToString());
            }

            OperationCompleted?.Invoke(this, result);
            return result;
        }

        private void NotifyProgress(string opName, int percent, string status)
        {
            ProgressUpdated?.Invoke(this, new OperationProgressEventArgs 
            { 
                OperationName = opName, 
                Percentage = percent, 
                Status = status 
            });
        }
    }

    public class OperationProgressEventArgs : EventArgs
    {
        public string OperationName { get; set; } = string.Empty;
        public int Percentage { get; set; }
        public string Status { get; set; } = string.Empty;
    }
}
