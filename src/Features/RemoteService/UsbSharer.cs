using LibUsbDotNet;
using LibUsbDotNet.Main;
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Features.RemoteService
{
    public class UsbSharer : IDisposable
    {
        private TcpListener? _server;
        private bool _isRunning;
        private UsbDevice? _usbDevice;
        private UsbEndpointReader? _reader;
        private UsbEndpointWriter? _writer;

        // Events for UI Updates
        public event Action<string>? LogEvent;
        public event Action<bool>? StatusChanged;

        /// <summary>
        /// Starts sharing a specific USB device over TCP.
        /// </summary>
        /// <param name="localPort">Port to listen on (default 33000)</param>
        /// <param name="vid">Vendor ID of the device to share</param>
        /// <param name="pid">Product ID of the device to share</param>
        /// <returns>Session Code for the remote technician</returns>
        public async Task<string?> StartSharingAsync(int vid, int pid, int localPort = 33000)
        {
            try
            {
                LogEvent?.Invoke($"[Remote] Scanning for USB Device (VID: {vid:X4} PID: {pid:X4})...");

                await Task.Yield(); // Fix CS1998: Make async method real

                // 1. Find USB Device
                var finder = new UsbDeviceFinder(vid, pid);
                _usbDevice = UsbDevice.OpenUsbDevice(finder);

                if (_usbDevice == null)
                {
                    LogEvent?.Invoke("[Error] Device Not Found! Please connect the device via USB.");
                    return null;
                }

                // 2. Open Interface & Endpoints
                IUsbDevice? wholeUsbDevice = _usbDevice as IUsbDevice;
                if (!ReferenceEquals(wholeUsbDevice, null))
                {
                    // Select config #1
                    wholeUsbDevice.SetConfiguration(1);
                    wholeUsbDevice.ClaimInterface(0);
                }

                // Open Reader (IN) and Writer (OUT) - assuming Bulk endpoints for now
                // In a real scenario, we'd iterate interfaces to find correct endpoints.
                _reader = _usbDevice.OpenEndpointReader(ReadEndpointID.Ep01);
                _writer = _usbDevice.OpenEndpointWriter(WriteEndpointID.Ep01);

                LogEvent?.Invoke($"[Remote] Attached to: {_usbDevice.Info.ProductString}");

                // 3. Start TCP Server
                _server = new TcpListener(IPAddress.Any, localPort);
                _server.Start();
                _isRunning = true;
                StatusChanged?.Invoke(true);

                string sessionCode = $"{GetLocalIPAddress()}:{localPort}"; // Simple IP:Port for now
                LogEvent?.Invoke($"[Remote] Server Started! Technican Code: {sessionCode}");

                // 4. Listen for Connections (Background)
                _ = Task.Run(() => ListenForClientAsync());

                return sessionCode;
            }
            catch (Exception ex)
            {
                LogEvent?.Invoke($"[Error] Failed to start sharing: {ex.Message}");
                StopSharing();
                return null;
            }
        }

        private async Task ListenForClientAsync()
        {
            if (_server == null) return;

            while (_isRunning)
            {
                try
                {
                    LogEvent?.Invoke("[Remote] Waiting for technician connection...");
                    using (var client = await _server.AcceptTcpClientAsync())
                    using (var stream = client.GetStream())
                    {
                        LogEvent?.Invoke($"[Remote] Technician Connected from {client.Client.RemoteEndPoint}!");

                        if (_reader != null && _writer != null)
                        {
                            // Start bi-directional forwarding
                            var usbToNet = ForwardUsbToNetwork(_reader, stream);
                            var netToUsb = ForwardNetworkToUsb(stream, _writer);

                            await Task.WhenAny(usbToNet, netToUsb);
                        }
                    }
                }
                catch (Exception ex)
                {
                    if (_isRunning) LogEvent?.Invoke($"[Remote] Connection Error: {ex.Message}");
                }
            }
        }

        private async Task ForwardUsbToNetwork(UsbEndpointReader reader, NetworkStream stream)
        {
            byte[] buffer = new byte[4096];
            while (_isRunning && stream.CanWrite)
            {
                int bytesRead;
                var ec = reader.Read(buffer, 1000, out bytesRead);
                
                if (bytesRead > 0)
                {
                    await stream.WriteAsync(buffer, 0, bytesRead);
                }
                else if (ec != ErrorCode.None && ec != ErrorCode.IoTimedOut)
                {
                    LogEvent?.Invoke($"[USB Read Error] {ec}");
                    break;
                }
                await Task.Yield(); // Ensure loop is async
            }
        }

        private async Task ForwardNetworkToUsb(NetworkStream stream, UsbEndpointWriter writer)
        {
            byte[] buffer = new byte[4096];
            while (_isRunning && stream.CanRead)
            {
                int bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length);
                if (bytesRead == 0) break; // Disconnected

                int bytesWritten;
                var ec = writer.Write(buffer, 1000, out bytesWritten);
                
                if (ec != ErrorCode.None)
                {
                    LogEvent?.Invoke($"[USB Write Error] {ec}");
                    break;
                }
            }
        }

        public void StopSharing()
        {
            _isRunning = false;
            _server?.Stop();
            
            if (_usbDevice != null && _usbDevice.IsOpen)
            {
                IUsbDevice? wholeUsbDevice = _usbDevice as IUsbDevice;
                if (!ReferenceEquals(wholeUsbDevice, null))
                {
                    wholeUsbDevice.ReleaseInterface(0);
                }
                _usbDevice.Close();
            }

            StatusChanged?.Invoke(false);
            LogEvent?.Invoke("[Remote] Session Stopped.");
        }

        private string GetLocalIPAddress()
        {
            try
            {
                var host = Dns.GetHostEntry(Dns.GetHostName());
                foreach (var ip in host.AddressList)
                {
                    if (ip.AddressFamily == AddressFamily.InterNetwork)
                    {
                        return ip.ToString();
                    }
                }
            }
            catch {}
            return "127.0.0.1";
        }

        public void Dispose()
        {
            StopSharing();
        }
    }
}
