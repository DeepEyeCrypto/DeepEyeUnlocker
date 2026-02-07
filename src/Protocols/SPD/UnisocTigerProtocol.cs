using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Protocols.SPD
{
    /// <summary>
    /// Support for UniSoc Tiger (T606, T610, T612, T618) chipset protocols.
    /// Implements modern block-based flashing and secure handshake logic.
    /// </summary>
    public class UnisocTigerProtocol : SpdFdlProtocol
    {
        public UnisocTigerProtocol(IUsbTransport transport) : base(transport) { }

        public async Task<bool> SecureHandshakeAsync(byte[] authKey = null)
        {
            Logger.Info("[TIGER] Initializing Secure Handshake for Next-Gen Unisoc...");
            
            // 1. Standard HDLC Sync
            if (!await HandshakeAsync()) return false;

            // 2. High-Speed Baud Switching (921600 or 1.5M for Tiger)
            await SendCommandAsync(SpdConstants.BSL_CMD_CHECK_BAUD);
            var (cmd, data) = await ReadPacketAsync();
            if (cmd != SpdConstants.BSL_REP_ACK)
            {
                Logger.Warning("[TIGER] Baud sync failed. Falling back to default.");
            }

            // 3. Optional Authentication (for Secure Boot v3+ on Tiger)
            if (authKey != null)
            {
                Logger.Info("[TIGER] Sending RSA-2048 Auth Challenge...");
                await SendCommandAsync(0x15, authKey); // Custom Auth Cmd for Tiger
                var resp = await ReadPacketAsync();
                if (resp.Command != SpdConstants.BSL_REP_ACK)
                {
                    Logger.Error("[TIGER] Secure Boot Authentication Denied.");
                    return false;
                }
            }

            Logger.Success("[TIGER] Sentinel Handshake Locked.");
            return true;
        }

        public async Task<bool> FlashBlockAsync(string partition, byte[] blockData, uint offset)
        {
            // Tiger uses specific start/mid/end data commands for high-speed flashing
            Logger.Info($"[TIGER] Injecting block into {partition} at 0x{offset:X}...");
            
            // Format: [PartitionName (32 bytes)][Offset (4 bytes)][Size (4 bytes)]
            var header = new byte[40];
            System.Text.Encoding.ASCII.GetBytes(partition).CopyTo(header, 0);
            BitConverter.GetBytes(offset).CopyTo(header, 32);
            BitConverter.GetBytes((uint)blockData.Length).CopyTo(header, 36);

            await SendCommandAsync(SpdConstants.BSL_CMD_START_DATA, header);
            if ((await ReadPacketAsync()).Command != SpdConstants.BSL_REP_ACK) return false;

            await SendCommandAsync(SpdConstants.BSL_CMD_MID_DATA, blockData);
            if ((await ReadPacketAsync()).Command != SpdConstants.BSL_REP_ACK) return false;

            await SendCommandAsync(SpdConstants.BSL_CMD_END_DATA);
            return (await ReadPacketAsync()).Command == SpdConstants.BSL_REP_ACK;
        }
    }
}
