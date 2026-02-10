using DeepEye.UI.Modern.Infrastructure;
using System.Windows.Input;
using System.Windows.Media;

namespace DeepEye.UI.Modern.ViewModels
{
    public class RemoteServerViewModel : ViewModelBase
    {
        private string _sessionCode;
        private string _connectionStatus = "DISCONNECTED";
        private Brush _statusColor = Brushes.Gray;
        private string _remoteDeviceModel = "---";
        private string _remoteVidPid = "0000:0000";
        private string _latencyMs = "0ms";
        private bool _isConnected;

        public string SessionCode
        {
            get => _sessionCode;
            set => SetProperty(ref _sessionCode, value);
        }

        public string ConnectionStatus
        {
            get => _connectionStatus;
            set => SetProperty(ref _connectionStatus, value);
        }

        public Brush StatusColor
        {
            get => _statusColor;
            set => SetProperty(ref _statusColor, value);
        }

        public string RemoteDeviceModel
        {
            get => _remoteDeviceModel;
            set => SetProperty(ref _remoteDeviceModel, value);
        }

        public string RemoteVidPid
        {
            get => _remoteVidPid;
            set => SetProperty(ref _remoteVidPid, value);
        }

        public string LatencyMs
        {
            get => _latencyMs;
            set => SetProperty(ref _latencyMs, value);
        }

        public bool IsConnected
        {
            get => _isConnected;
            set => SetProperty(ref _isConnected, value);
        }

        public ICommand ConnectCommand { get; }
        public ICommand MountDriverCommand { get; }

        public RemoteServerViewModel()
        {
            ConnectCommand = new RelayCommand(ExecuteConnect);
            MountDriverCommand = new RelayCommand(ExecuteMount);
        }

        private void ExecuteConnect()
        {
            if (string.IsNullOrWhiteSpace(SessionCode))
            {
                ConnectionStatus = "INVALID CODE";
                StatusColor = Brushes.Red;
                return;
            }

            ConnectionStatus = "CONNECTING...";
            StatusColor = Brushes.Orange;

            // TODO: Call UsbClient.Connect(SessionCode)
            // Simulating success for UI testing
            IsConnected = true;
            ConnectionStatus = "CONNECTED SECURELY";
            StatusColor = Brushes.LimeGreen;
            RemoteDeviceModel = "Samsung Galaxy S24 Ultra";
            RemoteVidPid = "04E8:6860";
            LatencyMs = "45ms";
        }

        private void ExecuteMount()
        {
            // TODO: Install Virtual Driver
        }
    }
}
