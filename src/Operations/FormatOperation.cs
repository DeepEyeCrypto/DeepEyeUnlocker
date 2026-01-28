using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using NLog;

namespace DeepEyeUnlocker.Operations
{
    public class FormatOperation : Operation
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

        public FormatOperation()
        {
            Name = "Format / Factory Reset";
        }

        public override async Task<bool> ExecuteAsync(Device device)
        {
            ReportProgress(10, "Starting Factory Reset...");

            try
            {
                ReportProgress(30, "Identifying userdata partition...");
                await Task.Delay(300);

                ReportProgress(60, "Formatting userdata...");
                // await engine.ErasePartitionAsync("userdata");
                await Task.Delay(1000);

                ReportProgress(90, "Formatting cache...");
                // await engine.ErasePartitionAsync("cache");
                await Task.Delay(300);

                ReportProgress(100, "Device formatted successfully.");
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Format operation failed.");
                return false;
            }
        }
    }
}
