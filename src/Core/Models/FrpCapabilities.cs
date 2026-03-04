using System.Collections.Generic;

using Microsoft.EntityFrameworkCore;

namespace DeepEyeUnlocker.Core.Models
{
    [Owned]
    public class FrpCapabilities
    {
        public List<string> SupportedProtocols { get; set; } = new();
        public string FrpPartitionName { get; set; } = "frp";
        public FrpType Type { get; set; } = FrpType.GoogleStandard;
        public bool RequiresAuthAgent { get; set; }
        public string OfficialServiceMethod { get; set; } = string.Empty;
        public RiskLevel RiskLevel { get; set; } = RiskLevel.Low;
        public bool EnterpriseSupport { get; set; }
    }

    public enum FrpType
    {
        GoogleStandard,
        SamsungKnox,
        XiaomiMiCloud,
        HuaweiID,
        AppleiCloud,
        EnterpriseManaged,
        Unknown
    }
}
