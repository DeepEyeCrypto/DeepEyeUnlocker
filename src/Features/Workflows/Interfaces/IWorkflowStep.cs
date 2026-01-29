using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Features.Workflows.Interfaces
{
    public interface IWorkflowStep
    {
        string Name { get; }
        Task<bool> ExecuteAsync(DeviceContext device, IProgress<ProgressUpdate> progress, CancellationToken ct);
    }
}
