using System;
using System.Collections.Generic;
using System.Linq;

namespace DeepEyeUnlocker.Protocols.SPD
{
    public static class SpdPacketizer
    {
        public static byte[] Wrap(ushort command, byte[]? data = null)
        {
            var payload = new List<byte>();
            
            // Header: Command(2) + Length(2)
            payload.Add((byte)((command >> 8) & 0xFF));
            payload.Add((byte)(command & 0xFF));
            
            ushort len = (ushort)(data?.Length ?? 0);
            payload.Add((byte)((len >> 8) & 0xFF));
            payload.Add((byte)(len & 0xFF));
            
            if (data != null) payload.AddRange(data);
            
            // Add CRC (SPD usually uses a standard CRC-16 over the payload)
            // Note: In some versions, CRC is omitted or different. We'll implement a basic one.
            // ushort crc = CalculateCrc(payload.ToArray());
            // payload.Add((byte)((crc >> 8) & 0xFF));
            // payload.Add((byte)(crc & 0xFF));

            // Escape payload for HDLC
            var escaped = new List<byte>();
            escaped.Add(SpdConstants.HDLC_FLAG);
            
            foreach (var b in payload)
            {
                if (b == SpdConstants.HDLC_FLAG || b == SpdConstants.HDLC_ESCAPE)
                {
                    escaped.Add(SpdConstants.HDLC_ESCAPE);
                    escaped.Add((byte)(b ^ SpdConstants.HDLC_ESCAPE_MASK));
                }
                else
                {
                    escaped.Add(b);
                }
            }
            
            escaped.Add(SpdConstants.HDLC_FLAG);
            return escaped.ToArray();
        }

        public static byte[]? Unwrap(byte[] framedData, out ushort command)
        {
            command = 0;
            if (framedData == null || framedData.Length < 6) return null;

            // Find first and last flag
            int start = Array.IndexOf(framedData, SpdConstants.HDLC_FLAG);
            int end = Array.LastIndexOf(framedData, SpdConstants.HDLC_FLAG);
            
            if (start == -1 || end <= start) return null;

            var unescaped = new List<byte>();
            for (int i = start + 1; i < end; i++)
            {
                if (framedData[i] == SpdConstants.HDLC_ESCAPE)
                {
                    i++;
                    unescaped.Add((byte)(framedData[i] ^ SpdConstants.HDLC_ESCAPE_MASK));
                }
                else
                {
                    unescaped.Add(framedData[i]);
                }
            }

            var result = unescaped.ToArray();
            if (result.Length < 4) return null;

            command = (ushort)((result[0] << 8) | result[1]);
            ushort dataLen = (ushort)((result[2] << 8) | result[3]);

            if (result.Length < 4 + dataLen) return null;

            return result.Skip(4).Take(dataLen).ToArray();
        }
    }
}
