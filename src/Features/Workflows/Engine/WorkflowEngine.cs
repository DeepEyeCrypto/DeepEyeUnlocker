using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Workflows.Models;

namespace DeepEyeUnlocker.Features.Workflows.Engine
{
    public class WorkflowEngine
    {
        public async Task<bool> ExecuteWorkflowAsync(DeviceContext device, DeviceWorkflow workflow, IProgress<ProgressUpdate> progress, CancellationToken ct)
        {
            Logger.Info($"[WORKFLOW] Starting: {workflow.Name} on {device.Serial}");
            
            int currentStep = 0;
            int totalSteps = workflow.Steps.Count;

            foreach (var step in workflow.Steps)
            {
                if (ct.IsCancellationRequested)
                {
                    Logger.Warn("[WORKFLOW] Operation cancelled by user.");
                    return false;
                }

                currentStep++;
                int percent = (int)((float)currentStep / totalSteps * 100);
                progress.Report(ProgressUpdate.Info(percent, $"Step {currentStep}/{totalSteps}: {step.Action} {step.Target}"));

                bool success = await ExecuteStepAsync(device, step, ct);
                
                if (!success)
                {
                    Logger.Error($"[WORKFLOW] Step failed: {step.Action} {step.Target}");
                    if (step.StopOnError) return false;
                }
            }

            Logger.Success($"[WORKFLOW] '{workflow.Name}' finished successfully.");
            return true;
        }

        private async Task<bool> ExecuteStepAsync(DeviceContext device, WorkflowStep step, CancellationToken ct)
        {
            switch (step.Action)
            {
                case WorkflowAction.Backup:
                    Logger.Info($"[WORKFLOW] Backing up {step.Target}...");
                    await Task.Delay(1000, ct); // Simulation
                    return true;

                case WorkflowAction.Delay:
                    int ms = int.Parse(step.Parameter);
                    await Task.Delay(ms, ct);
                    return true;

                case WorkflowAction.Reboot:
                    Logger.Info("[WORKFLOW] Rebooting device...");
                    return true;

                default:
                    Logger.Warn($"[WORKFLOW] Action '{step.Action}' not yet implemented in engine.");
                    return false;
            }
        }
    }
}
