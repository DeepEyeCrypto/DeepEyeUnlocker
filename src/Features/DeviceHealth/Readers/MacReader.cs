using System;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Features.DeviceHealth.Interfaces;

namespace DeepEyeUnlocker.Features.DeviceHealth.Readers
{
    public class MacReader : IDeviceHealthReader
    {
        private readonly IAdbClient _adb;
        public string Name => "MAC Address Reader";

        public MacReader(IAdbClient adb)
        {
            _adb = adb;
        }

        public async Task ReadAsync(DeviceHealthReport report, CancellationToken ct = default)
        {
            // WiFi MAC
            var wifiMac = await _adb.ExecuteShellAsync("cat /sys/class/net/wlan0/address 2>/dev/null", ct);
            if (string.IsNullOrEmpty(wifiMac) || wifiMac.Contains("No such file"))
            {
                wifiMac = await _adb.ExecuteShellAsync("getprop ro.boot.mac", ct);
            }
            report.MacAddress = wifiMac.Trim().ToUpper();

            // BT MAC
            var btMac = await _adb.ExecuteShellAsync("getprop ro.boot.btmacaddr", ct);
            if (string.IsNullOrEmpty(btMac))
            {
                btMac = await _adb.ExecuteShellAsync("settings get secure bluetooth_address", ct);
            }
            report.BluetoothAddress = btMac.Trim().ToUpper();

            ValidateMac(report, report.MacAddress, "WiFi");
            ValidateMac(report, report.BluetoothAddress, "Bluetooth");
        }

        private void ValidateMac(DeviceHealthReport report, string mac, string type)
        {
            if (string.IsNullOrEmpty(mac) || mac == "00:00:00:00:00:00" || mac == "02:00:00:00:00:00")
            {
                report.AuditFindings.Add($"[{type}] MAC address is hidden or unavailable (02:00:00... is common on Android 6+).");
                return;
            }

            // Simple format check (HH:HH:HH:HH:HH:HH)
            if (mac.Split(':').Length != 6)
            {
                report.AuditFindings.Add($"[{type}] Invalid MAC format: {mac}");
            }
        }
    }
}
