using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure.Data;

namespace DeepEyeUnlocker.Core.Services
{
    public class CloudProfileService
    {
        private readonly DeviceDbContext _context;

        public CloudProfileService(DeviceDbContext context)
        {
            _context = context;
        }

        public async Task<DeviceProfile?> GetProfileAsync(string modelNumber)
        {
            // 1. Check Local DB
            // var local = await _context.Devices.FirstOrDefaultAsync(d => d.ModelNumber == modelNumber);
            // if (local != null) return local;

            // 2. Check Cloud (Simulated)
            return await FetchFromCloudAsync(modelNumber);
        }

        private Task<DeviceProfile?> FetchFromCloudAsync(string modelNumber)
        {
            // Simulate cloud lookup
            if (modelNumber == "SM-S921B") // S24
            {
                return Task.FromResult<DeviceProfile?>(new DeviceProfile
                {
                    ModelNumber = "SM-S921B",
                    MarketingName = "Samsung Galaxy S24 Ultra",
                    Brand = "Samsung",
                    Chipset = new ChipsetInfo { Model = "Snapdragon 8 Gen 3" },
                    ValidationStatus = TestStatus.VerifiedAlpha
                });
            }
            if (modelNumber == "24031PN0DC") // Xiaomi 14 Ultra
            {
                return Task.FromResult<DeviceProfile?>(new DeviceProfile
                {
                    ModelNumber = "24031PN0DC",
                    MarketingName = "Xiaomi 14 Ultra",
                    Brand = "Xiaomi",
                    Chipset = new ChipsetInfo { Model = "Snapdragon 8 Gen 3" },
                    ValidationStatus = TestStatus.VerifiedBeta
                });
            }

            return Task.FromResult<DeviceProfile?>(null);
        }

        public Task<bool> SyncProfilesAsync()
        {
             // Simulate sync
             return Task.FromResult(true);
        }
    }
}
