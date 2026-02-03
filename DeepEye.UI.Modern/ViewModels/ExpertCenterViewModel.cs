using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Workflows.Engine;
using DeepEyeUnlocker.Features.Workflows.Models;
using System;
using System.Collections.ObjectModel;
using System.Threading;
using System.Threading.Tasks;

using DeepEyeUnlocker.Core;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class ExpertCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        private readonly WorkflowEngine _workflowEngine;
        private CancellationTokenSource? _cts;

        public override string Title => "EXPERT WORKFLOW ENGINE";

        [ObservableProperty] private bool _isExecuting = false;
        [ObservableProperty] private string _statusMessage = "Locked - Expert Mode Required";
        [ObservableProperty] private double _progressValue = 0;

        public ObservableCollection<WorkflowStep> WorkflowSteps { get; } = new();
        public ObservableCollection<DeviceWorkflow> Presets { get; } = new();
        public WorkflowAction[] AvailableActions => (WorkflowAction[])Enum.GetValues(typeof(WorkflowAction));

        public ExpertCenterViewModel(DeviceContext? device)
        {
            _device = device;
            _workflowEngine = new WorkflowEngine();
            LoadPresets();
            
            if (_device != null)
            {
                StatusMessage = "Ready - Proceed with caution";
            }
        }

        private void LoadPresets()
        {
            Presets.Clear();
            
            var frpClean = new DeviceWorkflow { Name = "Safe FRP Wipe (Qualcomm)" };
            frpClean.Steps.Add(new WorkflowStep { Action = WorkflowAction.Backup, Target = "frp" });
            frpClean.Steps.Add(new WorkflowStep { Action = WorkflowAction.Erase, Target = "frp" });
            frpClean.Steps.Add(new WorkflowStep { Action = WorkflowAction.Reboot });
            Presets.Add(frpClean);

            var fullAudit = new DeviceWorkflow { Name = "Full Health & Audit" };
            fullAudit.Steps.Add(new WorkflowStep { Action = WorkflowAction.ShellCommand, Target = "getprop", Parameter = "ro.build.fingerprint" });
            fullAudit.Steps.Add(new WorkflowStep { Action = WorkflowAction.Delay, Parameter = "1000" });
            Presets.Add(fullAudit);
        }

        [RelayCommand]
        private void SelectPreset(DeviceWorkflow? workflow)
        {
            if (workflow == null) return;
            WorkflowSteps.Clear();
            foreach (var step in workflow.Steps) 
            {
                // Deep copy the steps to allow editing without modifying the preset
                WorkflowSteps.Add(new WorkflowStep { 
                    Action = step.Action, 
                    Target = step.Target, 
                    Parameter = step.Parameter, 
                    StopOnError = step.StopOnError 
                });
            }
            StatusMessage = $"Preset Loaded: {workflow.Name}";
        }

        [RelayCommand]
        private async Task ExecuteWorkflow()
        {
            if (_device == null || WorkflowSteps.Count == 0) return;

            IsExecuting = true;
            ProgressValue = 0;
            _cts = new CancellationTokenSource();

            var workflow = new DeviceWorkflow { Name = "Custom Active Stream", Steps = new(WorkflowSteps) };
            
            var progress = new Progress<ProgressUpdate>(p => {
                ProgressValue = p.Percentage;
                StatusMessage = p.Message;
            });

            try 
            {
                bool success = await _workflowEngine.ExecuteWorkflowAsync(_device, workflow, progress, _cts.Token);
                StatusMessage = success ? "Workflow Completed Successfully." : "Workflow Failed or Aborted.";
            }
            catch (Exception ex)
            {
                StatusMessage = $"Critical Engine Error: {ex.Message}";
                Logger.Error(ex, "Expert Workflow Engine crashed.");
            }

            IsExecuting = false;
        }

        [RelayCommand]
        private void StopWorkflow()
        {
            _cts?.Cancel();
            StatusMessage = "Cancellation requested...";
        }

        [RelayCommand]
        private void AddStep(WorkflowAction action)
        {
            WorkflowSteps.Add(new WorkflowStep { Action = action, Parameter = action == WorkflowAction.Delay ? "500" : "" });
        }

        [RelayCommand]
        private void RemoveStep(WorkflowStep? step)
        {
            if (step != null) WorkflowSteps.Remove(step);
        }

        [RelayCommand]
        private void MoveStepUp(WorkflowStep? step)
        {
            if (step == null) return;
            int idx = WorkflowSteps.IndexOf(step);
            if (idx > 0)
            {
                WorkflowSteps.RemoveAt(idx);
                WorkflowSteps.Insert(idx - 1, step);
            }
        }

        [RelayCommand]
        private void MoveStepDown(WorkflowStep? step)
        {
            if (step == null) return;
            int idx = WorkflowSteps.IndexOf(step);
            if (idx < WorkflowSteps.Count - 1)
            {
                WorkflowSteps.RemoveAt(idx);
                WorkflowSteps.Insert(idx + 1, step);
            }
        }

        [RelayCommand]
        private void SaveWorkflow(string name)
        {
            if (string.IsNullOrWhiteSpace(name)) name = $"Custom_{DateTime.Now:HHmm}";
            var newWorkflow = new DeviceWorkflow { Name = name, Steps = new(WorkflowSteps) };
            Presets.Add(newWorkflow);
            StatusMessage = $"Workflow saved as preset: {name}";
        }
    }
}
