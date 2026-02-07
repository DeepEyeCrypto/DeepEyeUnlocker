using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Protocols.Usb;

namespace DeepEyeUnlocker.Protocols.SPD
{
    public class SpdFdlProtocol
    {
        private readonly IUsbDevice _usb;

        public SpdFdlProtocol(IUsbDevice usb)
        {
            _usb = usb;
        }

        public async Task<bool> HandshakeAsync()
        {
            Logger.Info("[SPD] Starting BSL Handshake...");
            
            // 1. Sync baud rate
            byte[] sync = { SpdConstants.HDLC_FLAG };
            await _usb.WriteAsync(sync);
            
            byte[] response = await _usb.ReadAsync(1, 1000);
            if (response == null || response.Length == 0 || response[0] != SpdConstants.HDLC_FLAG)
            {
                Logger.Error("[SPD] Baud rate sync failed. Did not receive 0x7E.");
                return false;
            }

            // 2. Connect
            var connectPacket = SpdPacketizer.Wrap(SpdConstants.BSL_CMD_CONNECT);
            await _usb.WriteAsync(connectPacket);

            var result = await ReadPacketAsync();
            if (result.Command == SpdConstants.BSL_REP_ACK)
            {
                Logger.Success("[SPD] BSL Connection established.");
                return true;
            }

            Logger.Error($"[SPD] Connection failed. Received: 0x{result.Command:X4}");
            return false;
        }

        public async Task<(ushort Command, byte[]? Data)> ReadPacketAsync(int timeoutMs = 2000)
        {
            var buffer = new List<byte>();
            bool started = false;

            // Simple HDLC reader
            while (true)
            {
                byte[] b = await _usb.ReadAsync(1, timeoutMs);
                if (b == null || b.Length == 0) break;

                if (b[0] == SpdConstants.HDLC_FLAG)
                {
                    if (!started)
                    {
                        started = true;
                        buffer.Add(b[0]);
                    }
                    else if (buffer.Count > 1)
                    {
                        buffer.Add(b[0]);
                        break; // End of packet
                    }
                }
                else if (started)
                {
                    buffer.Add(b[0]);
                }
            }

            var packet = SpdPacketizer.Unwrap(buffer.ToArray(), out ushort cmd);
            return (cmd, packet);
        }

        public async Task<bool> SendCommandAsync(ushort command, byte[]? data = null)
        {
            var packet = SpdPacketizer.Wrap(command, data);
            await _usb.WriteAsync(packet);
            
            var resp = await ReadPacketAsync();
            return resp.Command == SpdConstants.BSL_REP_ACK;
        }

        public async Task<bool> LoadLoaderAsync(byte[] loaderData, uint targetAddress)
        {
            Logger.Info($"[SPD] Uploading loader to 0x{targetAddress:X8} ({loaderData.Length} bytes)...");

            // 1. Start data
            byte[] startBuf = new byte[8];
            BitConverter.GetBytes(targetAddress).CopyTo(startBuf, 0);
            BitConverter.GetBytes((uint)loaderData.Length).CopyTo(startBuf, 4);
            if (!await SendCommandAsync(SpdConstants.BSL_CMD_START_DATA, startBuf)) return false;

            // 2. Mid data (Chunks of 1024)
            int offset = 0;
            while (offset < loaderData.Length)
            {
                int chunkSize = Math.Min(1024, loaderData.Length - offset);
                byte[] chunk = new byte[chunkSize];
                Array.Copy(loaderData, offset, chunk, 0, chunkSize);
                
                if (!await SendCommandAsync(SpdConstants.BSL_CMD_MID_DATA, chunk)) return false;
                offset += chunkSize;
            }

            // 3. End data
            if (!await SendCommandAsync(SpdConstants.BSL_CMD_END_DATA)) return false;

            // 4. Exec data
            await _usb.WriteAsync(SpdPacketizer.Wrap(SpdConstants.BSL_CMD_EXEC_DATA));
            
            // "HANDSHAKE FIX": Wait for FDL1 to initialize and re-sync
            // Some devices switch baud rate here, or just need a 0x7E pulse to confirm host awareness
            Logger.Info("[SPD] Waiting for loader execution sync...");
            await Task.Delay(200);

            // Re-sync pulse
            byte[] sync = { SpdConstants.HDLC_FLAG };
            await _usb.WriteAsync(sync);
            
            var resp = await _usb.ReadAsync(1, 1000);
            if (resp != null && resp.Length > 0 && resp[0] == SpdConstants.HDLC_FLAG)
            {
                Logger.Success("[SPD] Loader re-sync successful.");
                return true;
            }

            Logger.Warning("[SPD] Loader re-sync failed, but execution might have succeeded.");
            return true; 
        }

        public async Task<string> ReadDeviceInfoAsync()
        {
            await SendCommandAsync(SpdConstants.BSL_CMD_READ_CHIPID);
            var resp = await ReadPacketAsync();
            if (resp.Command == SpdConstants.BSL_REP_ACK && resp.Data != null)
            {
                uint chipId = BitConverter.ToUInt32(resp.Data, 0);
                return $"SPD_CHIP_0x{chipId:X8}";
            }
            return "SPD_UNKNOWN_DEVICE";
        }
    }
}
