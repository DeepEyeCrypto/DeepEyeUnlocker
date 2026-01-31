using System;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core;
using DeepEye.UI.Modern.ViewModels;

namespace DeepEye.UI.Modern.Infrastructure
{
    public class UIProgressReporter : IProgress<ProgressUpdate>
    {
        public void Report(ProgressUpdate value)
        {
            if (MainViewModel.Instance == null) return;

            App.Current.Dispatcher.Invoke(() =>
            {
                MainViewModel.Instance.ProgressValue = value.Percentage;
                MainViewModel.Instance.StatusText = value.Status;
                
                if (value.Level == LogLevel.Error)
                {
                    Logger.Error(value.Status, value.Category);
                }
                else
                {
                    Logger.Info(value.Status, value.Category);
                }
            });
        }
    }
}
