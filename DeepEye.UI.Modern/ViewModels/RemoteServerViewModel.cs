using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using System.Windows.Media;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class RemoteServerViewModel : CenterViewModelBase
    {
        public override string Title => "REMOTE SERVER";

        [ObservableProperty] private string _sessionCode = "";
        [ObservableProperty] private string _connectionStatus = "DISCONNECTED";
        [ObservableProperty] private Brush _statusColor = Brushes.Gray;
        [ObservableProperty] private string _remoteDeviceModel = "---";
        [ObservableProperty] private string _remoteVidPid = "0000:0000";
        [ObservableProperty] private string _latencyMs = "0ms";
        [ObservableProperty] private bool _isConnected;

        [RelayCommand]
        private void Connect()
        {
            if (string.IsNullOrWhiteSpace(SessionCode))
            {
                ConnectionStatus = "INVALID CODE";
                StatusColor = Brushes.Red;
                return;
            }

            ConnectionStatus = "CONNECTING...";
            StatusColor = Brushes.Orange;

            // Simulating success
            IsConnected = true;
            ConnectionStatus = "CONNECTED SECURELY";
            StatusColor = Brushes.LimeGreen;
            RemoteDeviceModel = "Samsung Galaxy S24 Ultra";
            RemoteVidPid = "04E8:6860";
            LatencyMs = "45ms";
        }

        [RelayCommand]
        private void MountDriver()
        {
            // Install Virtual Driver logic
        }
    }
}
