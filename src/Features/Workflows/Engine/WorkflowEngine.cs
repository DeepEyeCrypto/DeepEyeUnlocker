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
            try 
            {
                switch (step.Action)
                {
                    case WorkflowAction.Backup:
                        // This would ideally use a real protocol if active
                        Logger.Info($"[WORKFLOW] Step {step.Action}: Target={step.Target}");
                        await Task.Delay(1000, ct); 
                        return true;

                    case WorkflowAction.ShellCommand:
                        Logger.Info($"[WORKFLOW] Executing Shell: {step.Target} {step.Parameter}...");
                        var adb = new Operations.AdbToolsManager();
                        string result = await adb.ExecuteCommandAsync($"{step.Target} {step.Parameter}");
                        Logger.Info($"[ADB] {result.Trim()}");
                        return !result.Contains("error", StringComparison.OrdinalIgnoreCase);

                    case WorkflowAction.Delay:
                        if (int.TryParse(step.Parameter, out int ms))
                        {
                            Logger.Info($"[WORKFLOW] Delaying for {ms}ms...");
                            await Task.Delay(ms, ct);
                            return true;
                        }
                        return false;

                    case WorkflowAction.Reboot:
                        Logger.Info("[WORKFLOW] Sending Reboot signal...");
                        var rebootAdb = new Operations.AdbToolsManager();
                        var mode = Enum.TryParse<Operations.RebootMode>(step.Parameter, true, out var m) ? m : Operations.RebootMode.System;
                        await rebootAdb.RebootDevice(mode); 
                        return true;

                    case WorkflowAction.Erase:
                        Logger.Info($"[WORKFLOW] Erasing partition: {step.Target}...");
                        await Task.Delay(1500, ct); // Simulation for now
                        return true;

                    default:
                        Logger.Warn($"[WORKFLOW] Action '{step.Action}' not yet fully implemented in engine.");
                        return false;
                }
            }
            catch (Exception ex)
            {
                Logger.Error($"[WORKFLOW] Step Exception: {ex.Message}");
                return false;
            }
        }
    }
}
