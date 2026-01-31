using CommunityToolkit.Mvvm.Input;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using System.Linq;
using System.Threading.Tasks;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class UnlockCenterViewModel : CenterViewModelBase
    {
        private readonly DeviceContext? _device;
        public override string Title => "LOCK & FRP SECURITY";

        public UnlockCenterViewModel(DeviceContext? device)
        {
            _device = device;
        }

        [RelayCommand]
        private async Task EraseFRP()
        {
            if (_device == null) { Logger.Error("No device selected!"); return; }
            
            Logger.Info($"Initiating FRP Erase for {_device.Brand}...");
            
            var ops = OperationFactory.GetAvailableOperations(_device);
            var frpOp = ops.FirstOrDefault(o => o.Name.Contains("FRP", StringComparison.OrdinalIgnoreCase) || o.Name.Contains("Factory Reset Protection", StringComparison.OrdinalIgnoreCase));
            
            if (frpOp != null)
            {
                var progress = new Infrastructure.UIProgressReporter();
                await frpOp.ExecuteAsync(_device, progress, System.Threading.CancellationToken.None);
            }
            else
            {
                Logger.Warning("No matching FRP operation found for this device mode.");
            }
        }

        [RelayCommand]
        private async Task FactoryReset()
        {
            if (_device == null) { Logger.Error("No device selected!"); return; }
            
            var ops = OperationFactory.GetAvailableOperations(_device);
            var formatOp = ops.FirstOrDefault(o => o.Name.Contains("Format", StringComparison.OrdinalIgnoreCase));
            
            if (formatOp != null)
            {
                await formatOp.ExecuteAsync(_device, new Infrastructure.UIProgressReporter(), System.Threading.CancellationToken.None);
            }
            else
            {
                Logger.Info($"[WIRING] Factory Reset (Coming Soon) for {_device.Brand}...");
            }
        }

        [RelayCommand]
        private void UnlockBootloader()
        {
            if (_device == null) { Logger.Error("No device selected!"); return; }
            Logger.Info($"[WIRING] Initiating Bootloader Unlock for {_device.Brand}...");
        }
    }
}
