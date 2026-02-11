using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Models
{
    public class FrpServiceContext
    {
        public DeviceProfile Profile { get; set; }
        public string ConnectionId { get; set; } 
        public string Protocol { get; set; }
        public object ActiveConnection { get; set; }
        public OwnershipStatus Ownership { get; set; } = OwnershipStatus.Unknown;
        public string UserReason { get; set; } = "Refurbish";
    }

    public enum OwnershipStatus
    {
        Unknown,
        VerifiedEnterpriseOwner,
        VerifiedIndividual,
        Unverified
    }
}
