using System.Collections.Generic;

namespace DeepEyeUnlocker.Features.Workflows.Models
{
    public enum WorkflowAction
    {
        Backup,
        Erase,
        Flash,
        Reboot,
        ShellCommand,
        Delay
    }

    public class WorkflowStep
    {
        public WorkflowAction Action { get; set; }
        public string Target { get; set; } = string.Empty; // Partition name or command
        public string Parameter { get; set; } = string.Empty; // Hex payload or filename
        public bool StopOnError { get; set; } = true;
    }

    public class DeviceWorkflow
    {
        public string Name { get; set; } = "Custom Workflow";
        public List<WorkflowStep> Steps { get; set; } = new();
    }
}
