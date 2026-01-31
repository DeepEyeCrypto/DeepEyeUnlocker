using System;

namespace DeepEyeUnlocker.Features.DeviceHealth.Models
{
    public enum HealthStatus
    {
        Unknown,
        Healthy,
        Warning,
        Critical
    }

    public class HealthMetric
    {
        public string Label { get; set; } = string.Empty;
        public string Value { get; set; } = string.Empty;
        public HealthStatus Status { get; set; } = HealthStatus.Unknown;
        public string Details { get; set; } = string.Empty;
    }

    public class HardwareHealthReport
    {
        public string DeviceId { get; set; } = string.Empty;
        public DateTime Timestamp { get; set; } = DateTime.Now;
        public System.Collections.Generic.List<HealthMetric> Metrics { get; set; } = new();
    }
}
