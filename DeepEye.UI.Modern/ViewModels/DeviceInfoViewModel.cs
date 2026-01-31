using CommunityToolkit.Mvvm.ComponentModel;
using DeepEyeUnlocker.Core.Models;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class DeviceInfoViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        public override string Title => "DEVICE INFORMATION";

        [ObservableProperty] private string _deviceName = "No Device Connected";
        [ObservableProperty] private string _chipsetInfo = "Select a device to see hardware details";
        [ObservableProperty] private string _connectionStatus = "DISCONNECTED";
        [ObservableProperty] private string _soC = "--";
        [ObservableProperty] private string _brand = "--";
        [ObservableProperty] private string _serial = "--";

        public DeviceInfoViewModel(DeviceContext? device)
        {
            _device = device;
            if (_device != null)
            {
                DeviceName = $"{_device.Brand} {_device.Model}".Trim();
                ChipsetInfo = $"{_device.Chipset} ({_device.Mode})";
                ConnectionStatus = $"CONNECTED VIA {_device.Mode}";
                SoC = _device.SoC ?? "Unknown";
                Brand = _device.Brand ?? "Unknown";
                Serial = _device.Serial ?? "N/A";
            }
        }
    }
}
