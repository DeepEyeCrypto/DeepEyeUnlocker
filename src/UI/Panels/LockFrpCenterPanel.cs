using System;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.UI.Panels
{
    /// <summary>
    /// Lock & FRP Center - Unified diagnostics and recovery panel
    /// </summary>
    public class LockFrpCenterPanel : Panel
    {
        private LockFrpDiagnosticsManager? _diagnostics;
        private DeviceContext? _currentDevice;
        private LockFrpDiagnostics? _lastScan;
        private bool _expertModeEnabled;
        private bool _disclaimerAccepted;

        // UI Components
        private Label _titleLabel = null!;
        private Label _deviceInfoLabel = null!;

        // FRP Status Section
        private GroupBox _frpGroup = null!;
        private Label _frpStatusLabel = null!;
        private Label _frpAccountLabel = null!;
        private Label _frpOemLabel = null!;

        // Lock Status Section
        private GroupBox _lockGroup = null!;
        private Label _lockStatusLabel = null!;
        private Label _lockSecurityLabel = null!;
        private Label _lockRecoveryLabel = null!;

        // Actions Section
        private GroupBox _actionsGroup = null!;
        private Button _scanButton = null!;
        private Button _ownerRecoveryButton = null!;
        private Button _oemSupportButton = null!;
        private Button _factoryResetButton = null!;

        // Expert Mode
        private CheckBox _expertModeToggle = null!;
        private Panel _expertPanel = null!;
        private Button _backupFrpButton = null!;
        private Button _exportReportButton = null!;

        // Log
        private RichTextBox _logBox = null!;

        private IProtocol? _currentProtocol;

        public LockFrpCenterPanel()
        {
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device, IProtocol? protocol)
        {
            _currentDevice = device;
            _currentProtocol = protocol;
            _diagnostics = new LockFrpDiagnosticsManager(protocol);
            UpdateDeviceInfo();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Size = new Size(520, 680);
            this.Padding = new Padding(15);

            int y = 15;

            // Title
            _titleLabel = new Label
            {
                Text = "🔐 Lock & FRP Center",
                Font = new Font("Segoe UI", 16, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_titleLabel);
            y += 40;

            // Subtitle warning
            var subtitleLabel = new Label
            {
                Text = "For device owners and authorized technicians only",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(255, 193, 7),
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(subtitleLabel);
            y += 25;

            // Device info
            _deviceInfoLabel = new Label
            {
                Text = "No device connected",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_deviceInfoLabel);
            y += 30;

            // FRP Status Group
            _frpGroup = CreateGroup("FRP Status", 15, y, 490, 90);
            this.Controls.Add(_frpGroup);

            _frpStatusLabel = CreateStatusLabel("Status: ⚪ Unknown", 15, 25, _frpGroup);
            _frpAccountLabel = CreateInfoLabel("Account: --", 15, 47, _frpGroup);
            _frpOemLabel = CreateInfoLabel("OEM Lock: --", 15, 67, _frpGroup);

            y += 105;

            // Lock Status Group
            _lockGroup = CreateGroup("Screen Lock Status", 15, y, 490, 90);
            this.Controls.Add(_lockGroup);

            _lockStatusLabel = CreateStatusLabel("Status: ⚪ Unknown", 15, 25, _lockGroup);
            _lockSecurityLabel = CreateInfoLabel("Security: --", 15, 47, _lockGroup);
            _lockRecoveryLabel = CreateInfoLabel("Recovery: --", 15, 67, _lockGroup);

            y += 105;

            // Actions Group
            _actionsGroup = CreateGroup("Recovery Options", 15, y, 490, 115);
            this.Controls.Add(_actionsGroup);

            _scanButton = CreateActionButton("🔍 Scan Status", 15, 28, 145, 35, Color.FromArgb(0, 123, 255));
            _scanButton.Click += OnScanClicked;
            _actionsGroup.Controls.Add(_scanButton);

            _ownerRecoveryButton = CreateActionButton("👤 Owner Help", 170, 28, 145, 35, Color.FromArgb(40, 167, 69));
            _ownerRecoveryButton.Click += OnOwnerRecoveryClicked;
            _actionsGroup.Controls.Add(_ownerRecoveryButton);

            _oemSupportButton = CreateActionButton("🏢 OEM Support", 325, 28, 150, 35, Color.FromArgb(108, 117, 125));
            _oemSupportButton.Click += OnOemSupportClicked;
            _actionsGroup.Controls.Add(_oemSupportButton);

            _factoryResetButton = CreateActionButton("🗑️ Factory Reset", 15, 70, 220, 35, Color.FromArgb(220, 53, 69));
            _factoryResetButton.Click += OnFactoryResetClicked;
            _actionsGroup.Controls.Add(_factoryResetButton);

            y += 130;

            // Expert Mode Toggle
            _expertModeToggle = new CheckBox
            {
                Text = "⚙️ Enable Expert Tools (authorized technicians only)",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                AutoSize = true
            };
            _expertModeToggle.CheckedChanged += OnExpertModeToggled;
            this.Controls.Add(_expertModeToggle);
            y += 28;

            // Expert Panel (hidden by default)
            _expertPanel = new Panel
            {
                Location = new Point(15, y),
                Size = new Size(490, 45),
                BackColor = Color.FromArgb(40, 40, 50),
                Visible = false
            };
            this.Controls.Add(_expertPanel);

            _backupFrpButton = CreateActionButton("💾 Backup FRP Partition", 10, 8, 180, 30, Color.FromArgb(108, 117, 125));
            _backupFrpButton.Click += OnBackupFrpClicked;
            _expertPanel.Controls.Add(_backupFrpButton);

            _exportReportButton = CreateActionButton("📋 Export Report", 200, 8, 140, 30, Color.FromArgb(108, 117, 125));
            _exportReportButton.Click += OnExportReportClicked;
            _expertPanel.Controls.Add(_exportReportButton);

            y += 55;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(15, y),
                Size = new Size(490, 120),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            LogMessage("Lock & FRP Center ready.");
            LogMessage("⚠️ Use only for devices you own or are authorized to service.");
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

        private Label CreateStatusLabel(string text, int x, int y, Control parent)
        {
            var label = new Label
            {
                Text = text,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(x, y),
                AutoSize = true
            };
            parent.Controls.Add(label);
            return label;
        }

        private Label CreateInfoLabel(string text, int x, int y, Control parent)
        {
            var label = new Label
            {
                Text = text,
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(180, 180, 180),
                Location = new Point(x, y),
                AutoSize = true
            };
            parent.Controls.Add(label);
            return label;
        }

        private Button CreateActionButton(string text, int x, int y, int w, int h, Color color)
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

        private void UpdateDeviceInfo()
        {
            if (_currentDevice == null)
            {
                _deviceInfoLabel.Text = "No device connected";
                _scanButton.Enabled = false;
                return;
            }

            _deviceInfoLabel.Text = $"📱 {_currentDevice.Brand} {_currentDevice.Model} ({_currentDevice.Mode})";
            _scanButton.Enabled = true;
        }

        private async void OnScanClicked(object? sender, EventArgs e)
        {
            if (_diagnostics == null || _currentDevice == null) return;

            if (!EnsureDisclaimerAccepted()) return;

            _scanButton.Enabled = false;
            LogMessage("\n--- Scanning Lock & FRP Status ---", Color.Cyan);

            try
            {
                var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(30));

                _lastScan = await _diagnostics.FullDiagnosticsAsync(_currentDevice, progress, cts.Token);

                UpdateFrpDisplay(_lastScan.FrpStatus);
                UpdateLockDisplay(_lastScan.LockStatus);

                LogMessage($"Scan complete: {_lastScan.Summary}", Color.FromArgb(40, 167, 69));
            }
            catch (Exception ex)
            {
                LogMessage($"Scan error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _scanButton.Enabled = true;
            }
        }

        private void UpdateFrpDisplay(FrpStatus status)
        {
            var (icon, color) = status.Status switch
            {
                FrpLockStatus.Locked => ("🔴", Color.FromArgb(220, 53, 69)),
                FrpLockStatus.Unlocked => ("🟢", Color.FromArgb(40, 167, 69)),
                FrpLockStatus.PartiallyCleared => ("🟡", Color.FromArgb(255, 193, 7)),
                _ => ("⚪", Color.Gray)
            };

            _frpStatusLabel.Text = $"Status: {icon} {status.Status}";
            _frpStatusLabel.ForeColor = color;

            _frpAccountLabel.Text = $"Account: {status.AccountHint ?? "(not detected)"}";
            _frpOemLabel.Text = status.OemInfo?.HasOemAccountLock == true
                ? $"OEM Lock: {status.OemInfo.OemAccountType} may be active"
                : "OEM Lock: None detected";
        }

        private void UpdateLockDisplay(ScreenLockStatus status)
        {
            var lockIcon = status.IsLockEnabled == true ? "🔒" : "🔓";
            _lockStatusLabel.Text = $"Status: {lockIcon} {(status.IsLockEnabled == true ? "LOCKED" : "Unknown")}";
            _lockSecurityLabel.Text = $"Security: {status.SecurityLevel}";
            _lockRecoveryLabel.Text = status.CanRecoverDataWithoutCredential
                ? "Recovery: Data may be recoverable"
                : "Recovery: Factory reset required (data loss)";
        }

        private void OnOwnerRecoveryClicked(object? sender, EventArgs e)
        {
            var url = "https://accounts.google.com/signin/recovery";
            LogMessage($"Opening Google Account Recovery: {url}");
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(url) { UseShellExecute = true });
        }

        private void OnOemSupportClicked(object? sender, EventArgs e)
        {
            var url = _lastScan?.FrpStatus.OemInfo?.OfficialUnlockUrl ?? "https://support.google.com/android";
            LogMessage($"Opening OEM support: {url}");
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(url) { UseShellExecute = true });
        }

        private void OnFactoryResetClicked(object? sender, EventArgs e)
        {
            var result = MessageBox.Show(
                "⚠️ FACTORY RESET WARNING\n\n" +
                "This will PERMANENTLY ERASE:\n" +
                "• All photos, videos, and files\n" +
                "• All apps and app data\n" +
                "• All accounts and settings\n\n" +
                "This action CANNOT be undone.\n\n" +
                "If FRP is active, you will still need to verify\n" +
                "the Google account after reset.\n\n" +
                "Proceed with factory reset?",
                "Confirm Factory Reset",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result == DialogResult.Yes)
            {
                LogMessage("Factory reset initiated by user", Color.FromArgb(220, 53, 69));
                
                if (_currentDevice == null || _currentProtocol == null)
                {
                    LogMessage("Error: Connect device in low-level mode (EDL/BROM) first.", Color.Red);
                    return;
                }

                Task.Run(async () =>
                {
                    var formatOp = new FormatOperation(_currentProtocol);
                    var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Status));
                    bool success = await formatOp.ExecuteAsync(_currentDevice, progress, CancellationToken.None);
                    
                    if (success)
                    {
                        LogMessage("SUCCESS: Device formatted and rebooted.", Color.Lime);
                    }
                    else
                    {
                        LogMessage("FAILURE: Wipe operation failed.", Color.Red);
                    }
                });
            }
        }

        private void OnExpertModeToggled(object? sender, EventArgs e)
        {
            if (_expertModeToggle.Checked && !_expertModeEnabled)
            {
                var result = MessageBox.Show(
                    "Expert Tools are for authorized technicians only.\n\n" +
                    "These tools allow partition-level access which can:\n" +
                    "• Brick the device if misused\n" +
                    "• Result in data loss\n\n" +
                    "Confirm you are authorized to use these tools?",
                    "Enable Expert Mode",
                    MessageBoxButtons.YesNo,
                    MessageBoxIcon.Warning);

                if (result == DialogResult.Yes)
                {
                    _expertModeEnabled = true;
                    _expertPanel.Visible = true;
                    LogMessage("Expert mode enabled", Color.FromArgb(255, 193, 7));
                }
                else
                {
                    _expertModeToggle.Checked = false;
                }
            }
            else
            {
                _expertPanel.Visible = _expertModeToggle.Checked;
            }
        }

        private async void OnBackupFrpClicked(object? sender, EventArgs e)
        {
            LogMessage("FRP partition backup - feature requires EDL mode");
        }

        private async void OnExportReportClicked(object? sender, EventArgs e)
        {
            if (_lastScan == null || _diagnostics == null)
            {
                LogMessage("Run a scan first before exporting report");
                return;
            }

            using var dialog = new SaveFileDialog
            {
                Filter = "JSON Report (*.json)|*.json",
                FileName = $"lockfrp_report_{DateTime.Now:yyyyMMdd_HHmmss}.json"
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                await _diagnostics.ExportReportAsync(_lastScan, dialog.FileName);
                LogMessage($"Report exported: {dialog.FileName}", Color.FromArgb(40, 167, 69));
            }
        }

        private bool EnsureDisclaimerAccepted()
        {
            if (_disclaimerAccepted) return true;

            var result = MessageBox.Show(
                "🔐 LOCK & FRP CENTER DISCLAIMER\n\n" +
                "This section is designed for:\n" +
                "• Device OWNERS who forgot credentials\n" +
                "• Authorized service technicians\n\n" +
                "❌ NOT SUPPORTED:\n" +
                "• Unlocking stolen devices\n" +
                "• Bypassing security without authorization\n\n" +
                "By clicking 'Accept', you confirm you are the device\n" +
                "owner or have explicit authorization.\n\n" +
                "Unauthorized device access is a criminal offense.",
                "Terms of Use",
                MessageBoxButtons.OKCancel,
                MessageBoxIcon.Information);

            _disclaimerAccepted = result == DialogResult.OK;
            return _disclaimerAccepted;
        }

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
