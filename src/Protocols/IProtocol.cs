using System.Threading.Tasks;

namespace DeepEyeUnlocker.Protocols
{
    public interface IProtocol
    {
        string Name { get; }
        Task<bool> ConnectAsync();
        Task DisconnectAsync();
        Task<byte[]> ReadPartitionAsync(string partitionName);
        Task<bool> WritePartitionAsync(string partitionName, byte[] data);
        Task<bool> ErasePartitionAsync(string partitionName);
        Task<System.Collections.Generic.List<Core.PartitionInfo>> GetPartitionTableAsync();
    }
}
