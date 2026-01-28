using System;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Drivers;

namespace DeepEyeUnlocker.UI.Panels
{
    /// <summary>
    /// EDL Control Panel for managing EDL reboot and status
    /// </summary>
    public class EdlControlPanel : Panel
    {
        private readonly IEdlManager _edlManager;
        private readonly DriverManager _driverManager;
        
        private DeviceContext? _currentDevice;
        
        // UI Components
        private Label _titleLabel = null!;
        private Label _statusLabel = null!;
        private Label _capabilityLabel = null!;
        private Label _driverStatusLabel = null!;
        private Button _rebootButton = null!;
        private Button _checkDriverButton = null!;
        private Button _installDriverButton = null!;
        private ProgressBar _progressBar = null!;
        private RichTextBox _logBox = null!;

        public EdlControlPanel() : this(new EdlManager(), new DriverManager()) { }

        public EdlControlPanel(IEdlManager edlManager, DriverManager driverManager)
        {
            _edlManager = edlManager;
            _driverManager = driverManager;
            InitializeComponents();
        }

        /// <summary>
        /// Update the panel with a new device context
        /// </summary>
        public void SetDevice(DeviceContext? device)
        {
            _currentDevice = device;
            UpdateUI();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Padding = new Padding(15);
            this.Size = new Size(400, 450);

            int y = 15;

            // Title
            _titleLabel = new Label
            {
                Text = "⚡ EDL Manager",
                Font = new Font("Segoe UI", 14, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_titleLabel);
            y += 40;

            // Status Row
            this.Controls.Add(CreateLabel("Device Status:", 15, y));
            _statusLabel = CreateValueLabel("No device selected", 130, y);
            this.Controls.Add(_statusLabel);
            y += 28;

            // Capability Row
            this.Controls.Add(CreateLabel("EDL Capability:", 15, y));
            _capabilityLabel = CreateValueLabel("Unknown", 130, y);
            this.Controls.Add(_capabilityLabel);
            y += 28;

            // Driver Status Row
            this.Controls.Add(CreateLabel("Driver Status:", 15, y));
            _driverStatusLabel = CreateValueLabel("Checking...", 130, y);
            this.Controls.Add(_driverStatusLabel);
            y += 40;

            // Reboot Button
            _rebootButton = new Button
            {
                Text = "🔄 Reboot to EDL",
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                BackColor = Color.FromArgb(220, 53, 69),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Location = new Point(15, y),
                Size = new Size(170, 40),
                Enabled = false
            };
            _rebootButton.FlatAppearance.BorderSize = 0;
            _rebootButton.Click += OnRebootClicked;
            this.Controls.Add(_rebootButton);

            // Check Driver Button
            _checkDriverButton = new Button
            {
                Text = "🔍 Check Drivers",
                Font = new Font("Segoe UI", 9),
                BackColor = Color.FromArgb(50, 50, 60),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Location = new Point(195, y),
                Size = new Size(110, 40)
            };
            _checkDriverButton.FlatAppearance.BorderSize = 1;
            _checkDriverButton.FlatAppearance.BorderColor = Color.FromArgb(70, 70, 80);
            _checkDriverButton.Click += OnCheckDriversClicked;
            this.Controls.Add(_checkDriverButton);

            // Install Driver Button
            _installDriverButton = new Button
            {
                Text = "📥 Install",
                Font = new Font("Segoe UI", 9),
                BackColor = Color.FromArgb(50, 50, 60),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Location = new Point(315, y),
                Size = new Size(70, 40),
                Enabled = false
            };
            _installDriverButton.FlatAppearance.BorderSize = 1;
            _installDriverButton.FlatAppearance.BorderColor = Color.FromArgb(70, 70, 80);
            _installDriverButton.Click += OnInstallDriverClicked;
            this.Controls.Add(_installDriverButton);
            y += 55;

            // Progress Bar
            _progressBar = new ProgressBar
            {
                Location = new Point(15, y),
                Size = new Size(370, 8),
                Style = ProgressBarStyle.Continuous,
                Visible = false
            };
            this.Controls.Add(_progressBar);
            y += 20;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(15, y),
                Size = new Size(370, 180),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            // Initial driver check
            _ = CheckDriverStatusAsync();
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
                _statusLabel.Text = "No device selected";
                _statusLabel.ForeColor = Color.Gray;
                _capabilityLabel.Text = "N/A";
                _capabilityLabel.ForeColor = Color.Gray;
                _rebootButton.Enabled = false;
                return;
            }

            // Update status
            _statusLabel.Text = $"{_currentDevice.Brand} - {GetModeLabel(_currentDevice.Mode)}";
            _statusLabel.ForeColor = _currentDevice.Mode == ConnectionMode.EDL 
                ? Color.FromArgb(40, 167, 69) 
                : Color.White;

            // Update capability
            var capability = _edlManager.GetCapabilityFor(_currentDevice);
            _capabilityLabel.Text = GetCapabilityLabel(capability);
            _capabilityLabel.ForeColor = GetCapabilityColor(capability);

            // Enable reboot button based on capability
            _rebootButton.Enabled = capability != EdlCapability.HARDWARE_ONLY && 
                                    _currentDevice.Mode != ConnectionMode.EDL &&
                                    IsQualcommDevice(_currentDevice);

            // Log device info
            LogMessage($"Device: {_currentDevice.Brand} {_currentDevice.Model}");
            LogMessage($"Chipset: {_currentDevice.Chipset}, Mode: {_currentDevice.Mode}");
            LogMessage($"EDL Capability: {capability}");

            // Check profile
            var profile = _edlManager.GetProfileFor(_currentDevice);
            if (profile != null && !string.IsNullOrEmpty(profile.Notes))
            {
                LogMessage($"Note: {profile.Notes}", Color.FromArgb(255, 193, 7));
            }
        }

        private async void OnRebootClicked(object? sender, EventArgs e)
        {
            if (_currentDevice == null) return;

            // Show confirmation
            var result = MessageBox.Show(
                "You are about to reboot this device into EDL mode.\n\n" +
                "⚠️ The device will become unresponsive until a Firehose programmer is loaded.\n" +
                "⚠️ Use the original USB cable directly connected to PC.\n\n" +
                "Do you want to proceed?",
                "Confirm EDL Reboot",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result != DialogResult.Yes) return;

            _rebootButton.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Style = ProgressBarStyle.Marquee;

            LogMessage("\n--- Starting EDL Reboot ---", Color.Cyan);

            try
            {
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(30));
                var edlResult = await _edlManager.RebootToEdlAsync(_currentDevice, cts.Token);

                if (edlResult.Success)
                {
                    LogMessage($"✅ SUCCESS: Device entered EDL via {edlResult.MethodUsed}", Color.FromArgb(40, 167, 69));
                    LogMessage($"Time elapsed: {edlResult.ElapsedTime.TotalSeconds:F1}s");
                    
                    _statusLabel.Text = "EDL Mode (9008)";
                    _statusLabel.ForeColor = Color.FromArgb(40, 167, 69);

                    MessageBox.Show(
                        "Device successfully entered EDL mode!\n\n" +
                        "You can now perform flash operations.",
                        "EDL Reboot Successful",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Information);
                }
                else
                {
                    LogMessage($"❌ FAILED: {edlResult.FailureReason}", Color.FromArgb(220, 53, 69));
                    
                    if (!string.IsNullOrEmpty(edlResult.Log))
                    {
                        LogMessage("\nDetailed log:", Color.Gray);
                        foreach (var line in edlResult.Log.Split('\n'))
                        {
                            if (!string.IsNullOrWhiteSpace(line))
                                LogMessage($"  {line}", Color.FromArgb(120, 120, 130));
                        }
                    }

                    // Show test-point hint if applicable
                    var capability = _edlManager.GetCapabilityFor(_currentDevice);
                    if (capability == EdlCapability.HARDWARE_ONLY || capability == EdlCapability.SOFTWARE_RESTRICTED)
                    {
                        var testPoint = _edlManager.GetTestPointInfo(_currentDevice);
                        if (testPoint != null)
                        {
                            LogMessage($"\n🔩 Test-point available: {testPoint.Description}", Color.FromArgb(255, 193, 7));
                        }
                    }
                }
            }
            catch (OperationCanceledException)
            {
                LogMessage("Operation timed out", Color.FromArgb(255, 193, 7));
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _progressBar.Visible = false;
                _rebootButton.Enabled = true;
            }
        }

        private async void OnCheckDriversClicked(object? sender, EventArgs e)
        {
            await CheckDriverStatusAsync();
        }

        private async void OnInstallDriverClicked(object? sender, EventArgs e)
        {
            LogMessage("\nInstalling Qualcomm driver...", Color.Cyan);
            
            var progress = new Progress<string>(msg => LogMessage(msg));
            var result = await _driverManager.InstallDriverAsync("qualcomm", progress);
            
            if (result)
            {
                LogMessage("Driver installation completed!", Color.FromArgb(40, 167, 69));
                await CheckDriverStatusAsync();
            }
            else
            {
                LogMessage("Driver installation failed. Try manual installation.", Color.FromArgb(220, 53, 69));
            }
        }

        private async Task CheckDriverStatusAsync()
        {
            _driverStatusLabel.Text = "Checking...";
            _driverStatusLabel.ForeColor = Color.Gray;

            try
            {
                var driverInfo = await _driverManager.CheckQualcommDriverAsync();

                if (driverInfo.Status == DriverStatus.Installed)
                {
                    _driverStatusLabel.Text = "✅ Installed";
                    _driverStatusLabel.ForeColor = Color.FromArgb(40, 167, 69);
                    _installDriverButton.Enabled = false;
                    LogMessage($"Qualcomm driver: Installed ({driverInfo.Version})", Color.FromArgb(40, 167, 69));
                }
                else if (driverInfo.Status == DriverStatus.NotInstalled)
                {
                    _driverStatusLabel.Text = "❌ Not Installed";
                    _driverStatusLabel.ForeColor = Color.FromArgb(220, 53, 69);
                    _installDriverButton.Enabled = true;
                    LogMessage("Qualcomm driver: Not installed", Color.FromArgb(255, 193, 7));
                }
                else
                {
                    _driverStatusLabel.Text = "⚠️ Unknown";
                    _driverStatusLabel.ForeColor = Color.FromArgb(255, 193, 7);
                    _installDriverButton.Enabled = true;
                }
            }
            catch (Exception ex)
            {
                _driverStatusLabel.Text = "Error";
                LogMessage($"Driver check error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
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

        private static bool IsQualcommDevice(DeviceContext device)
        {
            var chipset = device.Chipset?.ToLower() ?? "";
            var soc = device.SoC?.ToLower() ?? "";
            
            return chipset.Contains("qualcomm") ||
                   chipset.Contains("snapdragon") ||
                   soc.StartsWith("sm") ||
                   soc.StartsWith("sdm") ||
                   soc.StartsWith("msm");
        }

        private static string GetModeLabel(ConnectionMode mode) => mode switch
        {
            ConnectionMode.ADB => "ADB",
            ConnectionMode.Fastboot => "Fastboot",
            ConnectionMode.EDL => "EDL (9008)",
            ConnectionMode.BROM => "BROM",
            ConnectionMode.DownloadMode => "Download",
            _ => "Unknown"
        };

        private static string GetCapabilityLabel(EdlCapability capability) => capability switch
        {
            EdlCapability.SOFTWARE_DIRECT_SUPPORTED => "✅ Supported",
            EdlCapability.SOFTWARE_RESTRICTED => "⚠️ Restricted",
            EdlCapability.HARDWARE_ONLY => "🔩 Test-Point Only",
            EdlCapability.UNKNOWN => "❓ Unknown",
            _ => "N/A"
        };

        private static Color GetCapabilityColor(EdlCapability capability) => capability switch
        {
            EdlCapability.SOFTWARE_DIRECT_SUPPORTED => Color.FromArgb(40, 167, 69),
            EdlCapability.SOFTWARE_RESTRICTED => Color.FromArgb(255, 193, 7),
            EdlCapability.HARDWARE_ONLY => Color.FromArgb(220, 53, 69),
            _ => Color.Gray
        };
    }
}
