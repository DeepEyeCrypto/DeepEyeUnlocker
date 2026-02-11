using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.Models
{
    public class FrpServiceContext
    {
        public DeviceProfile Profile { get; set; } = new();
        public string ConnectionId { get; set; } = string.Empty;
        public string Protocol { get; set; } = string.Empty;
        public object? ActiveConnection { get; set; }
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
