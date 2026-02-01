using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using SharpPcap;
using SharpPcap.LibPcap;
using PacketDotNet;

namespace DeepEyeUnlocker.Infrastructure.HIL
{
    public class UsbCaptureEngine : IUsbCaptureEngine
    {
        public Task StartCaptureAsync(int vid, int pid, string outputPcap)
        {
            // Real implementation would find the correct USB interface and start capture
            // For now, we'll focus on the parsing logic which is critical for Stage 2.
            return Task.CompletedTask;
        }

        public Task StopCaptureAsync()
        {
            return Task.CompletedTask;
        }

        public async Task<IEnumerable<UsbPacket>> ParseCaptureAsync(string pcapPath)
        {
            var packets = new List<UsbPacket>();
            
            if (!File.Exists(pcapPath)) return packets;

            await Task.Run(() =>
            {
                using var device = new CaptureFileReaderDevice(pcapPath);
                device.Open();

                PacketCapture e;
                var linkLayer = device.LinkType;
                while (device.GetNextPacket(out e).ToString().Contains("Ok"))
                {
                    var rawPacket = new RawCapture(linkLayer, e.Header.Timeval, e.Data.ToArray());
                    var usbPacket = ParseRawPacket(rawPacket);
                    if (usbPacket != null)
                    {
                        packets.Add(usbPacket);
                    }
                }

                device.Close();
            });

            return packets;
        }

        private UsbPacket? ParseRawPacket(RawCapture raw)
        {
            // Link layers:
            // 189 = LINKTYPE_USB_LINUX
            // 249 = LINKTYPE_USBPCAP
            
            if (raw.LinkLayerType == (LinkLayers)189)
            {
                return ParseLinuxUsb(raw);
            }
            if (raw.LinkLayerType == (LinkLayers)249)
            {
                return ParseUsbPcap(raw);
            }

            return null;
        }

        private UsbPacket? ParseLinuxUsb(RawCapture raw)
        {
            // Linux USB (usbmon) header is typically 64 bytes
            if (raw.Data.Length < 64) return null;

            // Direction: 'S' (Submission/HostToDevice) or 'C' (Completion/DeviceToHost)
            char eventType = (char)raw.Data[0]; 
            byte directionChar = raw.Data[10]; // Bit 7 of endpoint address
            
            var direction = (directionChar & 0x80) != 0 ? UsbDirection.DeviceToHost : UsbDirection.HostToDevice;
            
            // In usbmon, 'S' on Out endpoint is HostToDevice
            // 'C' on In endpoint is DeviceToHost
            
            // Raw data starts after the header
            byte[] payload = raw.Data.Skip(64).ToArray();
            if (payload.Length == 0) return null;

            return new UsbPacket
            {
                TimestampUs = (long)raw.Timeval.Seconds * 1000000 + (long)raw.Timeval.MicroSeconds,
                Data = payload,
                Direction = direction,
                Endpoint = raw.Data[10] & 0x0F
            };
        }

        private UsbPacket? ParseUsbPcap(RawCapture raw)
        {
            // USBPcap header is variable size, usually starts with 27 bytes for the common part
            if (raw.Data.Length < 27) return null;

            ushort headerLen = BitConverter.ToUInt16(raw.Data, 0);
            if (raw.Data.Length < headerLen) return null;

            byte endpointAddr = raw.Data[20];
            var direction = (endpointAddr & 0x80) != 0 ? UsbDirection.DeviceToHost : UsbDirection.HostToDevice;

            byte[] payload = raw.Data.Skip(headerLen).ToArray();
            if (payload.Length == 0) return null;

            return new UsbPacket
            {
                TimestampUs = (long)raw.Timeval.Seconds * 1000000 + (long)raw.Timeval.MicroSeconds,
                Data = payload,
                Direction = direction,
                Endpoint = endpointAddr & 0x0F
            };
        }
    }
}
