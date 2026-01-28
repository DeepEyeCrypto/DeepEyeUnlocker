using System;
using System.Drawing;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Helpers
{
    public static class NotificationHelper
    {
        private static NotifyIcon _notifyIcon;

        public static void Initialize(Form mainForm)
        {
            _notifyIcon = new NotifyIcon
            {
                Icon = SystemIcons.Information, // In production, use the DeepEye icon
                Visible = true,
                Text = "DeepEyeUnlocker"
            };

            _notifyIcon.BalloonTipClicked += (s, e) => {
                mainForm.WindowState = FormWindowState.Normal;
                mainForm.Activate();
            };
        }

        public static void ShowNotification(string title, string message, ToolTipIcon icon = ToolTipIcon.Info)
        {
            if (_notifyIcon != null)
            {
                _notifyIcon.ShowBalloonTip(3000, title, message, icon);
            }
        }

        public static void Dispose()
        {
            if (_notifyIcon != null)
            {
                _notifyIcon.Visible = false;
                _notifyIcon.Dispose();
            }
        }
    }
}
