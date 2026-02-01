using System;
using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    public enum StepDirection
    {
        HostToDevice,
        DeviceToHost
    }

    public class ScenarioStep
    {
        public StepDirection Direction { get; set; }
        public string Label { get; set; } = string.Empty;
        public string DataHex { get; set; } = string.Empty;
        public int DelayMs { get; set; }
        
        public byte[] GetData() => Convert.FromHexString(DataHex);
    }

    public class FrpContext
    {
        public string Status { get; set; } = string.Empty;
        public List<string> ProtectedPartitions { get; set; } = new();
        public bool AuthRequired { get; set; }
    }

    public class ScenarioExpectations
    {
        public string? FinalState { get; set; }
        public int MaxDurationMs { get; set; } = 5000;
        public string? EngineResult { get; set; }
        public string? ErrorCode { get; set; }
        public string? ProtectedOperation { get; set; }
    }

    public class ProtocolScenario
    {
        public string Name { get; set; } = string.Empty;
        public string Protocol { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public List<ScenarioStep> Steps { get; set; } = new();
        public FrpContext? FrpContext { get; set; }
        public ScenarioExpectations Expectations { get; set; } = new();
    }
}
