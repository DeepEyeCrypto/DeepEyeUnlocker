    public class BootloaderOperation : Operation
    {
        private static readonly Logger Logger = LogManager.GetCurrentClassLogger();

        public BootloaderOperation()
        {
            Name = "Advanced Bootloader Unlock";
        }

        public override async Task<bool> ExecuteAsync(Device device)
        {
            ReportProgress(5, "Initialing Security Check...");

            // Logic for manual confirmation handled in UI or via progress status
            ReportProgress(10, "⚠️ WARNING: Data will be wiped!");
            await Task.Delay(1000);

            try
            {
                BootloaderUnlockMethod method = device.Chipset?.ToLower() switch
                {
                    "qualcomm" => new QualcommBootloaderUnlock(),
                    "mtk" => new MTKBootloaderUnlock(),
                    "samsung" => new SamsungBootloaderUnlock(),
                    _ => new GenericOemUnlock()
                };

                ReportProgress(30, $"Executing {method.GetType().Name}...");
                bool result = await method.ExecuteAsync(device);

                if (result)
                {
                    ReportProgress(100, "✅ Bootloader Unlocked Successfully.");
                    return true;
                }
                
                return false;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Bootloader unlock orchestration failed.");
                return false;
            }
        }
    }
}
