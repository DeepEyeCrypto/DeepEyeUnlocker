using System;
using System.Drawing;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Helpers
{
    public static class NotificationHelper
    {
        private static NotifyIcon? _notifyIcon;

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

        public static void ShowSuccess(string msg) => MessageBox.Show(msg ?? "", "✅ Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
        public static void ShowError(string msg) => MessageBox.Show(msg ?? "", "❌ Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
        public static void ShowWarning(string msg) => MessageBox.Show(msg ?? "", "⚠️ Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning);

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
