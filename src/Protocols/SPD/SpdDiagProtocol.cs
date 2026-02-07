using System;
using System.Threading.Tasks;
using DeepEyeUnlocker.Protocols.Usb;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Protocols.SPD
{
    public class SpdDiagProtocol
    {
        private readonly IUsbDevice _usb;

        public SpdDiagProtocol(IUsbDevice usb)
        {
            _usb = usb;
        }

        public async Task<bool> SendDiagCommandAsync(ushort command, byte[]? data = null)
        {
            var packet = SpdPacketizer.Wrap(command, data);
            await _usb.WriteAsync(packet);
            
            // Diag responses use the same HDLC framing
            var buffer = new System.Collections.Generic.List<byte>();
            bool started = false;

            while (true)
            {
                byte[] b = await _usb.ReadAsync(1, 2000);
                if (b == null || b.Length == 0) break;

                if (b[0] == SpdConstants.HDLC_FLAG)
                {
                    if (!started) { started = true; buffer.Add(b[0]); }
                    else if (buffer.Count > 1) { buffer.Add(b[0]); break; }
                }
                else if (started) buffer.Add(b[0]);
            }

            var unwrap = SpdPacketizer.Unwrap(buffer.ToArray(), out ushort cmd);
            return cmd == SpdConstants.BSL_REP_ACK || cmd == command; // Some diag cmds echo cmd ID on success
        }

        public async Task<byte[]?> ReadNVItemAsync(ushort itemId)
        {
            Logger.Info($"[SPD-DIAG] Reading NV Item: 0x{itemId:X4}");
            
            byte[] cmdData = new byte[2];
            cmdData[0] = (byte)((itemId >> 8) & 0xFF);
            cmdData[1] = (byte)(itemId & 0xFF);

            var packet = SpdPacketizer.Wrap(SpdConstants.DIAG_CMD_READ_NV, cmdData);
            await _usb.WriteAsync(packet);

            // Read response
            var buffer = new System.Collections.Generic.List<byte>();
            bool started = false;
            while (true)
            {
                byte[] b = await _usb.ReadAsync(1, 2000);
                if (b == null || b.Length == 0) break;
                if (b[0] == SpdConstants.HDLC_FLAG)
                {
                    if (!started) { started = true; buffer.Add(b[0]); }
                    else if (buffer.Count > 1) { buffer.Add(b[0]); break; }
                }
                else if (started) buffer.Add(b[0]);
            }

            return SpdPacketizer.Unwrap(buffer.ToArray(), out _);
        }

        public async Task<bool> WriteNVItemAsync(ushort itemId, byte[] data)
        {
            Logger.Warning($"[SPD-DIAG] Writing NV Item: 0x{itemId:X4} (DANGER)");
            
            byte[] cmdData = new byte[2 + data.Length];
            cmdData[0] = (byte)((itemId >> 8) & 0xFF);
            cmdData[1] = (byte)(itemId & 0xFF);
            Array.Copy(data, 0, cmdData, 2, data.Length);

            return await SendDiagCommandAsync(SpdConstants.DIAG_CMD_WRITE_NV, cmdData);
        }

        public async Task<string> GetFactoryInfoAsync()
        {
            await SendDiagCommandAsync(SpdConstants.DIAG_CMD_GET_VERSION);
            // In a real implementation, we'd parse the HDLC response for strings like "SC9863A"
            return "SPD_DIAG_OK";
        }
    }
}
