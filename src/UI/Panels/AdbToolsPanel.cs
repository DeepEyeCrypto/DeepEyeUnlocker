using System;
using System.Collections.Generic;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;

namespace DeepEyeUnlocker.UI.Panels
{
    /// <summary>
    /// ADB Tools Center - Common ADB utilities in one place
    /// </summary>
    public class AdbToolsPanel : Panel
    {
        private DeviceContext? _device;
        private AdbToolsManager _adbManager = new();

        // UI Components
        private Label _titleLabel = null!;
        private Label _statusLabel = null!;

        // Reboot Section
        private GroupBox _rebootGroup = null!;
        private Button _rebootSystemButton = null!;
        private Button _rebootRecoveryButton = null!;
        private Button _rebootBootloaderButton = null!;
        private Button _rebootDownloadButton = null!;
        private Button _shutdownButton = null!;

        // App Section
        private GroupBox _appGroup = null!;
        private ComboBox _packageFilterCombo = null!;
        private Button _listPackagesButton = null!;
        private ListBox _packageListBox = null!;
        private Button _uninstallButton = null!;
        private Button _disableButton = null!;
        private Button _clearDataButton = null!;

        // File Transfer Section
        private GroupBox _fileGroup = null!;
        private TextBox _remotePathBox = null!;
        private Button _pushButton = null!;
        private Button _pullButton = null!;
        private Button _screenshotButton = null!;

        // Input Section
        private GroupBox _inputGroup = null!;
        private Button _homeButton = null!;
        private Button _backButton = null!;
        private Button _menuButton = null!;
        private TextBox _inputTextBox = null!;
        private Button _sendTextButton = null!;

        // Log
        private RichTextBox _logBox = null!;

        public AdbToolsPanel()
        {
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device)
        {
            _device = device;
            UpdateStatus();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Size = new Size(580, 720);

            int y = 10;

            // Title
            _titleLabel = new Label
            {
                Text = "🔧 ADB Tools Center",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_titleLabel);
            y += 45;

            // Status
            _statusLabel = new Label
            {
                Text = "⚪ No device connected",
                Font = new Font("Segoe UI", 10),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_statusLabel);
            y += 30;

            // Reboot Section
            _rebootGroup = CreateGroup("🔄 Reboot Options", 15, y, 260, 140);
            this.Controls.Add(_rebootGroup);

            int btnY = 25;
            _rebootSystemButton = CreateSmallButton("System", 10, btnY, 75, 30, Color.FromArgb(40, 167, 69));
            _rebootSystemButton.Click += async (s, e) => await RebootTo(RebootMode.System);
            _rebootGroup.Controls.Add(_rebootSystemButton);

            _rebootRecoveryButton = CreateSmallButton("Recovery", 90, btnY, 75, 30, Color.FromArgb(255, 193, 7));
            _rebootRecoveryButton.ForeColor = Color.Black;
            _rebootRecoveryButton.Click += async (s, e) => await RebootTo(RebootMode.Recovery);
            _rebootGroup.Controls.Add(_rebootRecoveryButton);

            _rebootBootloaderButton = CreateSmallButton("Fastboot", 170, btnY, 75, 30, Color.FromArgb(0, 123, 255));
            _rebootBootloaderButton.Click += async (s, e) => await RebootTo(RebootMode.Bootloader);
            _rebootGroup.Controls.Add(_rebootBootloaderButton);

            btnY += 40;
            _rebootDownloadButton = CreateSmallButton("Download", 10, btnY, 85, 30, Color.FromArgb(108, 117, 125));
            _rebootDownloadButton.Click += async (s, e) => await RebootTo(RebootMode.Download);
            _rebootGroup.Controls.Add(_rebootDownloadButton);

            _shutdownButton = CreateSmallButton("Shutdown", 100, btnY, 85, 30, Color.FromArgb(220, 53, 69));
            _shutdownButton.Click += async (s, e) => await Shutdown();
            _rebootGroup.Controls.Add(_shutdownButton);

            // Input Section (right of reboot)
            _inputGroup = CreateGroup("📱 Device Input", 285, y, 280, 140);
            this.Controls.Add(_inputGroup);

            _homeButton = CreateSmallButton("🏠", 10, 25, 50, 35, Color.FromArgb(60, 60, 70));
            _homeButton.Click += async (s, e) => await SendKey(AdbToolsManager.KeyCodes.Home);
            _inputGroup.Controls.Add(_homeButton);

            _backButton = CreateSmallButton("◀", 65, 25, 50, 35, Color.FromArgb(60, 60, 70));
            _backButton.Click += async (s, e) => await SendKey(AdbToolsManager.KeyCodes.Back);
            _inputGroup.Controls.Add(_backButton);

            _menuButton = CreateSmallButton("≡", 120, 25, 50, 35, Color.FromArgb(60, 60, 70));
            _menuButton.Click += async (s, e) => await SendKey(AdbToolsManager.KeyCodes.Menu);
            _inputGroup.Controls.Add(_menuButton);

            _screenshotButton = CreateSmallButton("📷 Screenshot", 175, 25, 95, 35, Color.FromArgb(0, 123, 255));
            _screenshotButton.Click += OnScreenshotClicked;
            _inputGroup.Controls.Add(_screenshotButton);

            _inputTextBox = new TextBox
            {
                Location = new Point(10, 70),
                Size = new Size(175, 25),
                Font = new Font("Segoe UI", 9),
                PlaceholderText = "Text to send..."
            };
            _inputGroup.Controls.Add(_inputTextBox);

            _sendTextButton = CreateSmallButton("Send", 190, 68, 80, 28, Color.FromArgb(40, 167, 69));
            _sendTextButton.Click += OnSendTextClicked;
            _inputGroup.Controls.Add(_sendTextButton);

            y += 155;

            // App Management Section
            _appGroup = CreateGroup("📦 App Management", 15, y, 550, 180);
            this.Controls.Add(_appGroup);

            var filterLabel = new Label
            {
                Text = "Filter:",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.White,
                Location = new Point(10, 28),
                AutoSize = true
            };
            _appGroup.Controls.Add(filterLabel);

            _packageFilterCombo = new ComboBox
            {
                Location = new Point(55, 25),
                Size = new Size(120, 25),
                DropDownStyle = ComboBoxStyle.DropDownList,
                Font = new Font("Segoe UI", 9)
            };
            _packageFilterCombo.Items.AddRange(new[] { "All", "Third-Party", "System", "Disabled" });
            _packageFilterCombo.SelectedIndex = 1;
            _appGroup.Controls.Add(_packageFilterCombo);

            _listPackagesButton = CreateSmallButton("List", 180, 23, 60, 28, Color.FromArgb(0, 123, 255));
            _listPackagesButton.Click += OnListPackagesClicked;
            _appGroup.Controls.Add(_listPackagesButton);

            _packageListBox = new ListBox
            {
                Location = new Point(10, 55),
                Size = new Size(350, 110),
                Font = new Font("Consolas", 9),
                BackColor = Color.FromArgb(25, 25, 30),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None
            };
            _appGroup.Controls.Add(_packageListBox);

            _uninstallButton = CreateSmallButton("Uninstall", 370, 55, 80, 30, Color.FromArgb(220, 53, 69));
            _uninstallButton.Click += OnUninstallClicked;
            _appGroup.Controls.Add(_uninstallButton);

            _disableButton = CreateSmallButton("Disable", 370, 90, 80, 30, Color.FromArgb(255, 193, 7));
            _disableButton.ForeColor = Color.Black;
            _disableButton.Click += OnDisableClicked;
            _appGroup.Controls.Add(_disableButton);

            _clearDataButton = CreateSmallButton("Clear Data", 370, 125, 80, 30, Color.FromArgb(108, 117, 125));
            _clearDataButton.Click += OnClearDataClicked;
            _appGroup.Controls.Add(_clearDataButton);

            y += 195;

            // File Transfer Section
            _fileGroup = CreateGroup("📂 File Transfer", 15, y, 550, 85);
            this.Controls.Add(_fileGroup);

            var pathLabel = new Label
            {
                Text = "Remote Path:",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.White,
                Location = new Point(10, 28),
                AutoSize = true
            };
            _fileGroup.Controls.Add(pathLabel);

            _remotePathBox = new TextBox
            {
                Location = new Point(95, 25),
                Size = new Size(250, 25),
                Font = new Font("Segoe UI", 9),
                Text = "/sdcard/"
            };
            _fileGroup.Controls.Add(_remotePathBox);

            _pushButton = CreateSmallButton("⬆ Push", 355, 23, 90, 30, Color.FromArgb(0, 123, 255));
            _pushButton.Click += OnPushClicked;
            _fileGroup.Controls.Add(_pushButton);

            _pullButton = CreateSmallButton("⬇ Pull", 450, 23, 90, 30, Color.FromArgb(40, 167, 69));
            _pullButton.Click += OnPullClicked;
            _fileGroup.Controls.Add(_pullButton);

            y += 100;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(15, y),
                Size = new Size(550, 100),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            LogMessage("ADB Tools ready. Connect a device via ADB.");
        }

        private GroupBox CreateGroup(string title, int x, int y, int w, int h)
        {
            return new GroupBox
            {
                Text = title,
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(x, y),
                Size = new Size(w, h)
            };
        }

        private Button CreateSmallButton(string text, int x, int y, int w, int h, Color color)
        {
            var btn = new Button
            {
                Text = text,
                Font = new Font("Segoe UI", 9),
                Location = new Point(x, y),
                Size = new Size(w, h),
                BackColor = color,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btn.FlatAppearance.BorderSize = 0;
            return btn;
        }

        private void UpdateStatus()
        {
            if (_device == null || _device.Mode != ConnectionMode.ADB)
            {
                _statusLabel.Text = "⚪ No ADB device connected";
                _statusLabel.ForeColor = Color.FromArgb(150, 150, 160);
                SetControlsEnabled(false);
            }
            else
            {
                _statusLabel.Text = $"🟢 {_device.Brand} {_device.Model} (ADB)";
                _statusLabel.ForeColor = Color.FromArgb(40, 167, 69);
                SetControlsEnabled(true);
            }
        }

        private void SetControlsEnabled(bool enabled)
        {
            _rebootSystemButton.Enabled = enabled;
            _rebootRecoveryButton.Enabled = enabled;
            _rebootBootloaderButton.Enabled = enabled;
            _rebootDownloadButton.Enabled = enabled;
            _shutdownButton.Enabled = enabled;
            _homeButton.Enabled = enabled;
            _backButton.Enabled = enabled;
            _menuButton.Enabled = enabled;
            _screenshotButton.Enabled = enabled;
            _sendTextButton.Enabled = enabled;
            _listPackagesButton.Enabled = enabled;
            _pushButton.Enabled = enabled;
            _pullButton.Enabled = enabled;
        }

        #region Event Handlers

        private async Task RebootTo(RebootMode mode)
        {
            LogMessage($"Rebooting to {mode}...");
            var result = await _adbManager.RebootDevice(mode);
            LogMessage(result ? $"Reboot to {mode} initiated" : "Reboot failed", 
                result ? Color.FromArgb(40, 167, 69) : Color.FromArgb(220, 53, 69));
        }

        private async Task Shutdown()
        {
            var confirm = MessageBox.Show("Shutdown the device?", "Confirm", 
                MessageBoxButtons.YesNo, MessageBoxIcon.Question);
            if (confirm == DialogResult.Yes)
            {
                LogMessage("Shutting down device...");
                await _adbManager.ShutdownDevice();
                LogMessage("Shutdown initiated", Color.FromArgb(255, 193, 7));
            }
        }

        private async Task SendKey(int keyCode)
        {
            await _adbManager.InputKeyEvent(keyCode);
            LogMessage($"Sent key event: {keyCode}");
        }

        private async void OnScreenshotClicked(object? sender, EventArgs e)
        {
            LogMessage("Capturing screenshot...");
            var bytes = await _adbManager.CaptureScreenshot();
            
            if (bytes != null && bytes.Length > 0)
            {
                using var dialog = new SaveFileDialog
                {
                    Filter = "PNG Image (*.png)|*.png",
                    FileName = $"screenshot_{DateTime.Now:yyyyMMdd_HHmmss}.png"
                };

                if (dialog.ShowDialog() == DialogResult.OK)
                {
                    await System.IO.File.WriteAllBytesAsync(dialog.FileName, bytes);
                    LogMessage($"Screenshot saved: {dialog.FileName}", Color.FromArgb(40, 167, 69));
                }
            }
            else
            {
                LogMessage("Screenshot failed", Color.FromArgb(220, 53, 69));
            }
        }

        private async void OnSendTextClicked(object? sender, EventArgs e)
        {
            var text = _inputTextBox.Text;
            if (string.IsNullOrEmpty(text)) return;

            await _adbManager.InputText(text);
            LogMessage($"Sent text: {text}");
            _inputTextBox.Clear();
        }

        private async void OnListPackagesClicked(object? sender, EventArgs e)
        {
            LogMessage("Listing packages...");
            _packageListBox.Items.Clear();

            var filter = _packageFilterCombo.SelectedIndex switch
            {
                0 => PackageFilter.All,
                1 => PackageFilter.ThirdParty,
                2 => PackageFilter.System,
                3 => PackageFilter.Disabled,
                _ => PackageFilter.All
            };

            var packages = await _adbManager.ListPackages(filter);
            foreach (var pkg in packages)
            {
                _packageListBox.Items.Add(pkg.PackageName);
            }

            LogMessage($"Found {packages.Count} packages");
        }

        private async void OnUninstallClicked(object? sender, EventArgs e)
        {
            if (_packageListBox.SelectedItem == null) return;
            var pkg = _packageListBox.SelectedItem.ToString()!;

            var confirm = MessageBox.Show($"Uninstall {pkg}?", "Confirm Uninstall",
                MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
            
            if (confirm == DialogResult.Yes)
            {
                LogMessage($"Uninstalling {pkg}...");
                var result = await _adbManager.UninstallPackage(pkg);
                LogMessage(result ? "Uninstalled successfully" : "Uninstall failed",
                    result ? Color.FromArgb(40, 167, 69) : Color.FromArgb(220, 53, 69));
            }
        }

        private async void OnDisableClicked(object? sender, EventArgs e)
        {
            if (_packageListBox.SelectedItem == null) return;
            var pkg = _packageListBox.SelectedItem.ToString()!;

            LogMessage($"Disabling {pkg}...");
            var result = await _adbManager.DisableApp(pkg);
            LogMessage(result ? "App disabled" : "Disable failed",
                result ? Color.FromArgb(40, 167, 69) : Color.FromArgb(220, 53, 69));
        }

        private async void OnClearDataClicked(object? sender, EventArgs e)
        {
            if (_packageListBox.SelectedItem == null) return;
            var pkg = _packageListBox.SelectedItem.ToString()!;

            var confirm = MessageBox.Show($"Clear all data for {pkg}?\nThis cannot be undone.", 
                "Confirm Clear Data", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
            
            if (confirm == DialogResult.Yes)
            {
                LogMessage($"Clearing data for {pkg}...");
                var result = await _adbManager.ClearAppData(pkg);
                LogMessage(result ? "Data cleared" : "Clear failed",
                    result ? Color.FromArgb(40, 167, 69) : Color.FromArgb(220, 53, 69));
            }
        }

        private async void OnPushClicked(object? sender, EventArgs e)
        {
            using var dialog = new OpenFileDialog
            {
                Title = "Select file to push"
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                var remotePath = _remotePathBox.Text + System.IO.Path.GetFileName(dialog.FileName);
                LogMessage($"Pushing to {remotePath}...");
                var result = await _adbManager.PushFile(dialog.FileName, remotePath);
                LogMessage(result ? "File pushed successfully" : "Push failed",
                    result ? Color.FromArgb(40, 167, 69) : Color.FromArgb(220, 53, 69));
            }
        }

        private async void OnPullClicked(object? sender, EventArgs e)
        {
            var remotePath = _remotePathBox.Text;
            if (string.IsNullOrEmpty(remotePath)) return;

            using var dialog = new SaveFileDialog
            {
                FileName = System.IO.Path.GetFileName(remotePath)
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                LogMessage($"Pulling {remotePath}...");
                var result = await _adbManager.PullFile(remotePath, dialog.FileName);
                LogMessage(result ? "File pulled successfully" : "Pull failed",
                    result ? Color.FromArgb(40, 167, 69) : Color.FromArgb(220, 53, 69));
            }
        }

        #endregion

        private void LogMessage(string message, Color? color = null)
        {
            if (InvokeRequired)
            {
                Invoke(() => LogMessage(message, color));
                return;
            }

            _logBox.SelectionStart = _logBox.TextLength;
            _logBox.SelectionColor = color ?? Color.FromArgb(180, 180, 180);
            _logBox.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
            _logBox.ScrollToCaret();
        }
    }
}
