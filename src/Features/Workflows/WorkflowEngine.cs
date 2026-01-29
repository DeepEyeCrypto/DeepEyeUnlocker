using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Workflows.Interfaces;

namespace DeepEyeUnlocker.Features.Workflows
{
    public class WorkflowEngine
    {
        private readonly List<IWorkflowStep> _steps = new();

        public void AddStep(IWorkflowStep step) => _steps.Add(step);

        public async Task<bool> RunAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Logger.Info($"Starting automated workflow for {device.Serial} ({_steps.Count} steps)");
            
            int finished = 0;
            foreach (var step in _steps)
            {
                if (ct.IsCancellationRequested) return false;

                progress.Report(ProgressUpdate.Info((int)((float)finished / _steps.Count * 100), $"Step {finished+1}/{_steps.Count}: {step.Name}"));
                
                try
                {
                    bool success = await step.ExecuteAsync(device, progress, ct);
                    if (!success)
                    {
                        Logger.Error($"Workflow failed at step: {step.Name}");
                        return false;
                    }
                }
                catch (Exception ex)
                {
                    Logger.Error(ex, $"Critical error in workflow step: {step.Name}");
                    return false;
                }
                
                finished++;
            }

            progress.Report(ProgressUpdate.Info(100, "Workflow Completed Successfully."));
            return true;
        }
    }
}
