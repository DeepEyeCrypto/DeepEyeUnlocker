using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Models
{
    public class FrpServiceContext
    {
        public DeviceProfile Profile { get; set; }
        
        /// <summary>
        /// ID of the active protocol connection (e.g. "USB_123" or "EDL_HANDLE_5")
        /// </summary>
        public string ConnectionId { get; set; } 
        
        /// <summary>
        /// Active Protocol Detected (e.g. "EDL", "FASTBOOT")
        /// </summary>
        public string Protocol { get; set; }

        public OwnershipStatus Ownership { get; set; } = OwnershipStatus.Unknown;
        public string UserReason { get; set; } = "Refurbish";
    }

    public enum OwnershipStatus
    {
        /// <summary>
        /// Ownership not yet verified. Default state.
        /// </summary>
        Unknown,
        
        /// <summary>
        /// Device belongs to a verified enterprise fleet (MDM).
        /// </summary>
        VerifiedEnterpriseOwner,
        
        /// <summary>
        /// User has provided proof of purchase or identity.
        /// </summary>
        VerifiedIndividual,
        
        /// <summary>
        /// Verification failed or refused. Service blocked.
        /// </summary>
        Unverified
    }
}
