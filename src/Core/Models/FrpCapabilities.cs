using System.Collections.Generic;

namespace DeepEyeUnlocker.Core.Models
{
    public class FrpCapabilities
    {
        /// <summary>
        /// List of protocols that can be used to service FRP on this device (e.g., "EDL", "FASTBOOT", "ODIN")
        /// </summary>
        public List<string> SupportedProtocols { get; set; } = new();

        /// <summary>
        /// The partition name where FRP data is stored (e.g., "frp", "persistent", "config")
        /// </summary>
        public string FrpPartitionName { get; set; } = "frp";

        /// <summary>
        /// The type of FRP implementation
        /// </summary>
        public FrpType Type { get; set; } = FrpType.GoogleStandard;

        /// <summary>
        /// Whether an authenticated DA/Loader is required to touch this partition
        /// </summary>
        public bool RequiresAuthAgent { get; set; }

        /// <summary>
        /// Identifier for the official OEM removal method (e.g. "FASTBOOT_ERASE_PERSISTENT")
        /// </summary>
        public string OfficialServiceMethod { get; set; } = string.Empty;

        /// <summary>
        /// Risk level associated with clearing FRP on this device
        /// </summary>
        public RiskLevel RiskLevel { get; set; } = RiskLevel.Low;

        /// <summary>
        /// Supported by Android Enterprise official wipe commands
        /// </summary>
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
