using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class FlashCenterViewModel : CenterViewModelBase
    {
        public override string Title => "FLASH ENGINE";

        [ObservableProperty]
        private string _firmwarePath = "No file selected";

        [RelayCommand]
        private void BrowseFirmware()
        {
            // Open file dialog logic
            FirmwarePath = "C:\\Downloads\\samsung_firmware_v1.zip";
        }

        [RelayCommand]
        private void StartFlash()
        {
            // Start flashing logic
        }
    }
}
