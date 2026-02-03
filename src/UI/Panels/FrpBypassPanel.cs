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
    /// FRP Bypass Control Panel
    /// </summary>
    public class FrpBypassPanel : Panel
    {
        private FrpBypassManager? _manager;
        private DeviceContext? _currentDevice;
        private FrpStatus? _currentFrpInfo;
        private Protocols.IProtocol? _currentProtocol;
        
        // UI Components
        private Label _titleLabel = null!;
        private Label _deviceLabel = null!;
        
        private GroupBox _statusGroup = null!;
        private Label _statusLabel = null!;
        private Label _accountLabel = null!;
        private Label _methodLabel = null!;
        private Button _detectButton = null!;
        
        private GroupBox _bypassGroup = null!;
        private RadioButton _autoMethodRadio = null!;
        private RadioButton _eraseMethodRadio = null!;
        private RadioButton _overwriteMethodRadio = null!;
        private RadioButton _adbMethodRadio = null!;
        private Button _bypassButton = null!;
        
        private ProgressBar _progressBar = null!;
        private RichTextBox _logBox = null!;
        
        // Warning label
        private Label _warningLabel = null!;

        public FrpBypassPanel()
        {
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device, FirehoseManager? firehose, Protocols.IProtocol? protocol = null)
        {
            _currentDevice = device;
            _currentProtocol = protocol;
            _manager = new FrpBypassManager(firehose, protocol);
            _currentFrpInfo = null;
            UpdateUI();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Padding = new Padding(15);
            this.Size = new Size(480, 580);

            int y = 15;

            // Title
            _titleLabel = new Label
            {
                Text = "⚡ Sentinel Pro: FRP Bypass",
                Font = new Font("Segoe UI", 14, FontStyle.Bold),
                ForeColor = Color.FromArgb(0, 150, 136),
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_titleLabel);
            y += 35;

            // Device info
            _deviceLabel = new Label
            {
                Text = "No device connected",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_deviceLabel);
            y += 30;

            // Warning Label
            _warningLabel = new Label
            {
                Text = "⚠️ FRP bypass should only be used for legitimate purposes such as\n" +
                       "   recovering your own device. Misuse may be illegal.",
                Font = new Font("Segoe UI", 8.5f),
                ForeColor = Color.FromArgb(255, 193, 7),
                Location = new Point(15, y),
                Size = new Size(450, 35)
            };
            this.Controls.Add(_warningLabel);
            y += 45;

            // Status Group
            _statusGroup = new GroupBox
            {
                Text = "FRP Status",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                Size = new Size(450, 105)
            };
            this.Controls.Add(_statusGroup);

            int sy = 22;
            _statusGroup.Controls.Add(CreateLabel("Status:", 15, sy));
            _statusLabel = CreateValueLabel("Unknown", 100, sy);
            _statusGroup.Controls.Add(_statusLabel);
            sy += 22;

            _statusGroup.Controls.Add(CreateLabel("Account:", 15, sy));
            _accountLabel = CreateValueLabel("--", 100, sy);
            _statusGroup.Controls.Add(_accountLabel);
            sy += 22;

            _statusGroup.Controls.Add(CreateLabel("Detection:", 15, sy));
            _methodLabel = CreateValueLabel("--", 100, sy);
            _statusGroup.Controls.Add(_methodLabel);

            _detectButton = new Button
            {
                Text = "🔍 Detect",
                Font = new Font("Segoe UI", 9),
                Location = new Point(350, 30),
                Size = new Size(85, 55),
                BackColor = Color.FromArgb(0, 123, 255),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _detectButton.FlatAppearance.BorderSize = 0;
            _detectButton.Click += OnDetectClicked;
            _statusGroup.Controls.Add(_detectButton);

            y += 120;

            // Bypass Group
            _bypassGroup = new GroupBox
            {
                Text = "Bypass Method",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                Size = new Size(450, 140),
                Enabled = false
            };
            this.Controls.Add(_bypassGroup);

            int by = 25;
            _autoMethodRadio = CreateRadioButton("🤖 Auto (best method for this device)", 15, by, true);
            _bypassGroup.Controls.Add(_autoMethodRadio);
            by += 25;

            _eraseMethodRadio = CreateRadioButton("🗑️ Partition Erase (EDL/BROM)", 15, by);
            _bypassGroup.Controls.Add(_eraseMethodRadio);
            by += 25;

            _overwriteMethodRadio = CreateRadioButton("✏️ Partition Overwrite (safer)", 15, by);
            _bypassGroup.Controls.Add(_overwriteMethodRadio);
            by += 25;

            _adbMethodRadio = CreateRadioButton("📱 ADB Method (requires USB debug)", 15, by);
            _bypassGroup.Controls.Add(_adbMethodRadio);

            _bypassButton = new Button
            {
                Text = "🔓 Bypass FRP",
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Location = new Point(340, 40),
                Size = new Size(100, 70),
                BackColor = Color.FromArgb(220, 53, 69),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _bypassButton.FlatAppearance.BorderSize = 0;
            _bypassButton.Click += OnBypassClicked;
            _bypassGroup.Controls.Add(_bypassButton);

            y += 155;

            // Progress Bar
            _progressBar = new ProgressBar
            {
                Location = new Point(15, y),
                Size = new Size(450, 10),
                Style = ProgressBarStyle.Continuous,
                Visible = false
            };
            this.Controls.Add(_progressBar);
            y += 20;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(15, y),
                Size = new Size(450, 150),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            LogMessage("FRP Bypass panel ready.");
            LogMessage("Use responsibly and only for legitimate purposes.");
        }

        private RadioButton CreateRadioButton(string text, int x, int y, bool isChecked = false)
        {
            return new RadioButton
            {
                Text = text,
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.White,
                Location = new Point(x, y),
                AutoSize = true,
                Checked = isChecked
            };
        }

        private Label CreateLabel(string text, int x, int y) => new Label
        {
            Text = text,
            Font = new Font("Segoe UI", 9),
            ForeColor = Color.FromArgb(150, 150, 160),
            Location = new Point(x, y),
            AutoSize = true
        };

        private Label CreateValueLabel(string text, int x, int y) => new Label
        {
            Text = text,
            Font = new Font("Segoe UI", 9, FontStyle.Bold),
            ForeColor = Color.White,
            Location = new Point(x, y),
            AutoSize = true
        };

        private void UpdateUI()
        {
            if (_currentDevice == null)
            {
                _deviceLabel.Text = "No device connected";
                _detectButton.Enabled = false;
                _bypassGroup.Enabled = false;
                return;
            }

            _deviceLabel.Text = $"{_currentDevice.Brand} {_currentDevice.Model} ({_currentDevice.Mode})";
            _detectButton.Enabled = true;
            
            // Update method availability based on device mode
            _eraseMethodRadio.Enabled = _currentDevice.Mode == ConnectionMode.EDL || _currentDevice.Mode == ConnectionMode.BROM || _currentDevice.Mode == ConnectionMode.Preloader;
            _overwriteMethodRadio.Enabled = _currentDevice.Mode == ConnectionMode.EDL || _currentDevice.Mode == ConnectionMode.BROM;
            _adbMethodRadio.Enabled = _currentDevice.Mode == ConnectionMode.ADB;

            if (_currentFrpInfo != null)
            {
                UpdateStatusDisplay();
            }
        }

        private void UpdateStatusDisplay()
        {
            if (_currentFrpInfo == null) return;

            _statusLabel.Text = GetStatusText(_currentFrpInfo.Status);
            _statusLabel.ForeColor = GetStatusColor(_currentFrpInfo.Status);
            
            _accountLabel.Text = _currentFrpInfo.AccountHint ?? 
                                (_currentFrpInfo.IsGoogleAccountBound ? "Account linked" : "No account");
            
            _methodLabel.Text = _currentFrpInfo.DetectionMethod.ToString();

            _bypassGroup.Enabled = _currentFrpInfo.Status == FrpLockStatus.Locked;
        }

        private async void OnDetectClicked(object? sender, EventArgs e)
        {
            if (_manager == null || _currentDevice == null) return;

            _detectButton.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Style = ProgressBarStyle.Marquee;

            LogMessage("\n--- Detecting FRP Status ---", Color.Cyan);

            try
            {
                var progress = new Progress<ProgressUpdate>(update => LogMessage(update.Message));

                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(30));
                _currentFrpInfo = await _manager.DetectFrpStatusAsync(_currentDevice, progress, cts.Token);

                UpdateStatusDisplay();

                LogMessage($"Status: {_currentFrpInfo.Status}", GetStatusColor(_currentFrpInfo.Status));
                
                if (_currentFrpInfo.Status == FrpLockStatus.Locked)
                {
                    LogMessage("FRP is locked. You can attempt bypass.", Color.FromArgb(255, 193, 7));
                }
                else if (_currentFrpInfo.Status == FrpLockStatus.Unlocked)
                {
                    LogMessage("FRP is not locked. No bypass needed.", Color.FromArgb(40, 167, 69));
                }
            }
            catch (Exception ex)
            {
                LogMessage($"Detection error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _progressBar.Visible = false;
                _detectButton.Enabled = true;
            }
        }

        private async void OnBypassClicked(object? sender, EventArgs e)
        {
            if (_manager == null || _currentDevice == null) return;

            // Confirmation dialog
            var result = MessageBox.Show(
                "⚠️ WARNING: FRP Bypass\n\n" +
                "This will attempt to remove Factory Reset Protection.\n" +
                "Only proceed if:\n" +
                "• This is YOUR device\n" +
                "• You have legal authorization\n" +
                "• You understand the risks\n\n" +
                "Unauthorized access to devices is illegal.\n\n" +
                "Continue?",
                "Confirm FRP Bypass",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result != DialogResult.Yes) return;

            // Get selected method
            FrpBypassMethod? method = null;
            if (!_autoMethodRadio.Checked)
            {
                if (_eraseMethodRadio.Checked) method = FrpBypassMethod.PartitionErase;
                else if (_overwriteMethodRadio.Checked) method = FrpBypassMethod.PartitionOverwrite;
                else if (_adbMethodRadio.Checked) method = FrpBypassMethod.AdbBypass;
            }

            _bypassGroup.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Style = ProgressBarStyle.Marquee;

            LogMessage($"\n--- Starting Sentinel Pro FRP Bypass ({method?.ToString() ?? "Auto"}) ---", Color.FromArgb(0, 150, 136));

            try
            {
                var progress = new Progress<ProgressUpdate>(update =>
                {
                    LogMessage(update.Message);
                });

                using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(5));
                var bypassResult = await _manager.BypassFrpAsync(_currentDevice, method, progress, cts.Token);

                if (bypassResult.Success)
                {
                    LogMessage($"✅ SUCCESS: {bypassResult.Message}", Color.FromArgb(40, 167, 69));
                    LogMessage($"Method used: {bypassResult.MethodUsed}");
                    
                    if (bypassResult.RequiresReboot)
                    {
                        LogMessage("⚡ Reboot required to apply changes", Color.FromArgb(255, 193, 7));
                    }
                    
                    if (!string.IsNullOrEmpty(bypassResult.AdditionalSteps))
                    {
                        LogMessage($"📋 Next: {bypassResult.AdditionalSteps}");
                    }

                    _statusLabel.Text = "✅ Bypassed";
                    _statusLabel.ForeColor = Color.FromArgb(40, 167, 69);
                }
                else
                {
                    LogMessage($"❌ FAILED: {bypassResult.Message}", Color.FromArgb(220, 53, 69));
                    
                    if (!string.IsNullOrEmpty(bypassResult.AdditionalSteps))
                    {
                        LogMessage($"💡 Try: {bypassResult.AdditionalSteps}", Color.FromArgb(255, 193, 7));
                    }
                }
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _progressBar.Visible = false;
                _bypassGroup.Enabled = true;
            }
        }

        private static string GetStatusText(FrpLockStatus status) => status switch
        {
            FrpLockStatus.Locked => "🔒 LOCKED",
            FrpLockStatus.Unlocked => "🔓 Unlocked",
            FrpLockStatus.PartiallyCleared => "⚠️ Partially Cleared",
            FrpLockStatus.Error => "❌ Error",
            _ => "❓ Unknown"
        };

        private static Color GetStatusColor(FrpLockStatus status) => status switch
        {
            FrpLockStatus.Locked => Color.FromArgb(220, 53, 69),
            FrpLockStatus.Unlocked => Color.FromArgb(40, 167, 69),
            FrpLockStatus.PartiallyCleared => Color.FromArgb(255, 193, 7),
            _ => Color.Gray
        };

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
