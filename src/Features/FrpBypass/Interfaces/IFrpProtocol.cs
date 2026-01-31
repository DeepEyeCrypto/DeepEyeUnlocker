using System.Threading.Tasks;

namespace DeepEyeUnlocker.Features.FrpBypass.Interfaces
{
    /// <summary>
    /// Unified interface for Factory Reset Protection bypass protocols across different chipsets and modes.
    /// </summary>
    public interface IFrpProtocol
    {
        string ProtocolName { get; }
        
        // Fastboot Methods
        Task<bool> Fastboot_Oem_Unlock();
        Task<bool> Fastboot_Format_Partition(string partitionName);

        // Qualcomm EDL Methods
        Task<bool> EDL_Flash_Partition(string partitionName, byte[] data);
        Task<bool> EDL_Erase_Partition(string partitionName);

        // MediaTek BROM Methods
        Task<bool> MTK_Brom_Execute(uint address, byte[] payload);
        Task<bool> MTK_Brom_Write_Reg32(uint address, uint value);

        // Samsung Odin Methods
        Task<bool> Samsung_Odin_Send_Command(string cmd);
        Task<bool> Samsung_Odin_Flash_File(string target, byte[] data);
        
        // ADB General
        Task<bool> ADB_Bypass_FRP();
        
        // General
        Task<bool> Execute_Raw_Command(byte[] command);
    }
}
