using System;
using DeepEyeUnlocker.Core.Models;
using Xunit;

namespace DeepEyeUnlocker.Tests.Core.Models
{
    public class ModelValidationTests
    {
        [Fact]
        public void DeviceHealthReport_ShouldInitializeWithDefaults()
        {
            var report = new DeviceHealthReport();
            
            Assert.NotNull(report.SerialNumber);
            Assert.NotNull(report.AuditFindings);
            Assert.Equal(0, report.BatteryLevel);
            Assert.False(report.IsRooted);
            Assert.Equal("1.3.0", report.ToolVersion);
        }

        [Fact]
        public void PartitionBackupJob_ShouldGenerateUniqueId()
        {
            var job1 = new PartitionBackupJob();
            var job2 = new PartitionBackupJob();
            
            Assert.NotNull(job1.JobId);
            Assert.NotEqual(job1.JobId, job2.JobId);
            Assert.Equal(BackupStatus.Pending, job1.Status);
        }

        [Fact]
        public void PartitionInfo_ShouldFormatSizeCorrectlly()
        {
            var info = new PartitionInfo { SizeInBytes = 2 * 1024 * 1024 * 1024L }; // 2 GB
            
            Assert.Equal("2 GB", info.SizeFormatted);
        }

        [Fact]
        public void FrpStatus_ShouldDefaultToUnknown()
        {
            var status = new FrpStatus();
            
            Assert.Equal(FrpLockStatus.Unknown, status.Status);
            Assert.NotNull(status.RecommendedActions);
        }
    }
}
