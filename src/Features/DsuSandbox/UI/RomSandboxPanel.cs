using System;
using System.Drawing;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DsuSandbox.ImageManagement;
using DeepEyeUnlocker.Features.DsuSandbox.Models;
using DeepEyeUnlocker.Features.DsuSandbox.Orchestration;
using DeepEyeUnlocker.Features.DsuSandbox.Validation;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.UI.Themes;

namespace DeepEyeUnlocker.Features.DsuSandbox.UI
{
    /// <summary>
    /// ROM Sandbox Panel - Safe ROM testing via DSU
    /// </summary>
    public class RomSandboxPanel : Panel
    {
        private readonly IAdbClient _adb;
        private readonly GsiDatabase _gsiDatabase;
        private readonly DeviceCapabilityChecker _capabilityChecker;
        private readonly DsuFlashingOrchestrator _flashOrchestrator;

        private DeviceContext? _currentDevice;
        private DsuCapability? _deviceCapability;
        private DsuImage? _selectedImage;
        private CancellationTokenSource? _cts;
        private bool _isFlashing;

        // UI Controls
        private Label lblTitle = null!;
        private Label lblDeviceStatus = null!;
        private Label lblDsuStatus = null!;
        private GroupBox grpImageSelection = null!;
        private ComboBox cmbImages = null!;
        private Label lblImageInfo = null!;
        private Button btnDownload = null!;
        private Button btnBrowse = null!;
        private GroupBox grpTestMethod = null!;
        private RadioButton rbDsuAdb = null!;
        private RadioButton rbDsuRecovery = null!;
        private RadioButton rbABSlot = null!;
        private GroupBox grpPreflightChecks = null!;
        private Label lblFreeSpace = null!;
        private Label lblBattery = null!;
        private Label lblBootloader = null!;
        private Label lblCompatibility = null!;
        private Button btnFlashTest = null!;
        private Button btnRevert = null!;
        private Button btnClearCache = null!;
        private ProgressBar progressBar = null!;
        private Label lblProgress = null!;
        private RichTextBox txtLog = null!;

        public RomSandboxPanel(IAdbClient adb)
        {
            _adb = adb;
            _gsiDatabase = new GsiDatabase();
            _capabilityChecker = new DeviceCapabilityChecker(adb);
            _flashOrchestrator = new DsuFlashingOrchestrator(adb);

            this.Dock = DockStyle.Fill;
            this.BackColor = BrandColors.Primary;
            this.Padding = new Padding(20);

            InitializeControls();
            _ = LoadDatabaseAsync();
        }

        private void InitializeControls()
        {
            // Title
            lblTitle = new Label
            {
                Text = "🧪 ROM SANDBOX – Safe Testing (Zero Risk)",
                Font = new Font("Segoe UI", 16, FontStyle.Bold),
                ForeColor = Color.White,
                AutoSize = true,
                Location = new Point(20, 10)
            };

            // Device Status
            lblDeviceStatus = new Label
            {
                Text = "Device: Not connected",
                ForeColor = BrandColors.Text,
                Font = new Font("Segoe UI", 10),
                AutoSize = true,
                Location = new Point(20, 45)
            };

            lblDsuStatus = new Label
            {
                Text = "DSU Support: Unknown",
                ForeColor = BrandColors.Text,
                Font = new Font("Segoe UI", 10),
                AutoSize = true,
                Location = new Point(300, 45)
            };

            // Image Selection Group
            grpImageSelection = new GroupBox
            {
                Text = "📦 Select ROM / GSI Image",
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Location = new Point(20, 75),
                Size = new Size(450, 200)
            };

            cmbImages = new ComboBox
            {
                Location = new Point(15, 30),
                Size = new Size(400, 28),
                DropDownStyle = ComboBoxStyle.DropDownList,
                Font = new Font("Segoe UI", 10)
            };
            cmbImages.SelectedIndexChanged += OnImageSelectionChanged;

            lblImageInfo = new Label
            {
                Location = new Point(15, 65),
                Size = new Size(400, 80),
                ForeColor = BrandColors.Text,
                Font = new Font("Segoe UI", 9),
                Text = "Select an image to see details..."
            };

            btnDownload = new Button
            {
                Text = "⬇️ Download",
                Location = new Point(15, 150),
                Size = new Size(120, 35),
                BackColor = BrandColors.Accent,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Enabled = false
            };
            btnDownload.Click += async (s, e) => await DownloadSelectedImageAsync();

            btnBrowse = new Button
            {
                Text = "📁 Browse Local",
                Location = new Point(145, 150),
                Size = new Size(120, 35),
                BackColor = Color.FromArgb(60, 60, 70),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnBrowse.Click += OnBrowseClick;

            grpImageSelection.Controls.AddRange(new Control[] { cmbImages, lblImageInfo, btnDownload, btnBrowse });

            // Test Method Group
            grpTestMethod = new GroupBox
            {
                Text = "⚙️ Test Method",
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Location = new Point(490, 75),
                Size = new Size(280, 120)
            };

            rbDsuAdb = new RadioButton
            {
                Text = "DSU via ADB (Safest)",
                ForeColor = BrandColors.Text,
                Location = new Point(15, 30),
                AutoSize = true,
                Checked = true
            };

            rbDsuRecovery = new RadioButton
            {
                Text = "DSU via Recovery",
                ForeColor = BrandColors.Text,
                Location = new Point(15, 55),
                AutoSize = true
            };

            rbABSlot = new RadioButton
            {
                Text = "A/B Slot (Requires unlock)",
                ForeColor = BrandColors.Text,
                Location = new Point(15, 80),
                AutoSize = true
            };

            grpTestMethod.Controls.AddRange(new Control[] { rbDsuAdb, rbDsuRecovery, rbABSlot });

            // Preflight Checks Group
            grpPreflightChecks = new GroupBox
            {
                Text = "✅ Pre-Test Checks",
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Location = new Point(490, 200),
                Size = new Size(280, 120)
            };

            lblFreeSpace = new Label { Text = "⏳ Free Space: ...", ForeColor = BrandColors.Text, Location = new Point(15, 25), AutoSize = true };
            lblBattery = new Label { Text = "⏳ Battery: ...", ForeColor = BrandColors.Text, Location = new Point(15, 45), AutoSize = true };
            lblBootloader = new Label { Text = "⏳ Bootloader: ...", ForeColor = BrandColors.Text, Location = new Point(15, 65), AutoSize = true };
            lblCompatibility = new Label { Text = "⏳ Compatibility: ...", ForeColor = BrandColors.Text, Location = new Point(15, 85), AutoSize = true };

            grpPreflightChecks.Controls.AddRange(new Control[] { lblFreeSpace, lblBattery, lblBootloader, lblCompatibility });

            // Action Buttons
            btnFlashTest = new Button
            {
                Text = "🚀 FLASH & TEST",
                Location = new Point(20, 330),
                Size = new Size(180, 50),
                BackColor = Color.FromArgb(76, 175, 80),
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 12, FontStyle.Bold),
                FlatStyle = FlatStyle.Flat,
                Enabled = false
            };
            btnFlashTest.Click += async (s, e) => await StartFlashAsync();

            btnRevert = new Button
            {
                Text = "↩️ Revert to Original",
                Location = new Point(210, 330),
                Size = new Size(150, 50),
                BackColor = Color.FromArgb(255, 152, 0),
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                FlatStyle = FlatStyle.Flat,
                Enabled = false
            };
            btnRevert.Click += async (s, e) => await RevertAsync();

            btnClearCache = new Button
            {
                Text = "🗑️ Clear Cache",
                Location = new Point(370, 330),
                Size = new Size(100, 50),
                BackColor = Color.FromArgb(96, 125, 139),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnClearCache.Click += async (s, e) => await ClearCacheAsync();

            // Progress
            progressBar = new ProgressBar
            {
                Location = new Point(20, 390),
                Size = new Size(750, 25),
                Visible = false
            };

            lblProgress = new Label
            {
                Location = new Point(20, 420),
                Size = new Size(750, 25),
                ForeColor = BrandColors.Text,
                Font = new Font("Segoe UI", 10),
                Text = ""
            };

            // Log
            txtLog = new RichTextBox
            {
                Location = new Point(20, 450),
                Size = new Size(750, 150),
                BackColor = Color.FromArgb(30, 30, 35),
                ForeColor = Color.LightGray,
                Font = new Font("Consolas", 9),
                ReadOnly = true,
                BorderStyle = BorderStyle.None
            };

            // Add info label at bottom
            var lblInfo = new Label
            {
                Text = "ⓘ DSU is completely safe: original system untouched, revert with one reboot.",
                ForeColor = Color.FromArgb(100, 180, 255),
                Font = new Font("Segoe UI", 9, FontStyle.Italic),
                Location = new Point(20, 610),
                AutoSize = true
            };

            // Add all controls
            this.Controls.AddRange(new Control[]
            {
                lblTitle, lblDeviceStatus, lblDsuStatus,
                grpImageSelection, grpTestMethod, grpPreflightChecks,
                btnFlashTest, btnRevert, btnClearCache,
                progressBar, lblProgress, txtLog, lblInfo
            });
        }

        private async Task LoadDatabaseAsync()
        {
            await _gsiDatabase.LoadAsync();
            UpdateImageDropdown();
            Log("GSI database loaded with " + _gsiDatabase.Images.Count + " images");
        }

        private void UpdateImageDropdown()
        {
            cmbImages.Items.Clear();
            foreach (var img in _gsiDatabase.Images)
            {
                var status = img.IsDownloaded ? "✓" : "⬇";
                cmbImages.Items.Add($"{status} {img.Name} ({img.SizeFormatted})");
            }
        }

        private void OnImageSelectionChanged(object? sender, EventArgs e)
        {
            if (cmbImages.SelectedIndex < 0) return;

            _selectedImage = _gsiDatabase.Images.ElementAtOrDefault(cmbImages.SelectedIndex);
            if (_selectedImage == null) return;

            lblImageInfo.Text = $"OS: {_selectedImage.OsName} {_selectedImage.Version}\n" +
                               $"Architecture: {_selectedImage.Architecture}\n" +
                               $"Size: {_selectedImage.SizeFormatted}\n" +
                               $"Source: {_selectedImage.Source}\n" +
                               $"{_selectedImage.Notes}";

            btnDownload.Enabled = !_selectedImage.IsDownloaded && !string.IsNullOrEmpty(_selectedImage.DownloadUrl);
            btnFlashTest.Enabled = _selectedImage.IsDownloaded && _deviceCapability?.Level != DsuCapabilityLevel.NotSupported;
        }

        private void OnBrowseClick(object? sender, EventArgs e)
        {
            using var ofd = new OpenFileDialog
            {
                Title = "Select ROM/GSI Image",
                Filter = "System Images (*.img)|*.img|All Files (*.*)|*.*"
            };

            if (ofd.ShowDialog() == DialogResult.OK)
            {
                var name = System.IO.Path.GetFileNameWithoutExtension(ofd.FileName);
                _ = _gsiDatabase.AddCustomImageAsync(name, ofd.FileName);
                UpdateImageDropdown();
                cmbImages.SelectedIndex = cmbImages.Items.Count - 1;
            }
        }

        private async Task DownloadSelectedImageAsync()
        {
            if (_selectedImage == null) return;

            btnDownload.Enabled = false;
            progressBar.Visible = true;
            progressBar.Value = 0;

            try
            {
                Log($"Downloading {_selectedImage.Name}...");
                
                await _gsiDatabase.DownloadImageAsync(
                    _selectedImage.Id,
                    new Progress<int>(p => 
                    {
                        progressBar.Value = p;
                        lblProgress.Text = $"Downloading: {p}%";
                    }),
                    CancellationToken.None);

                Log($"Download complete: {_selectedImage.Name}");
                UpdateImageDropdown();
                cmbImages.SelectedIndex = _gsiDatabase.Images.ToList().FindIndex(i => i.Id == _selectedImage.Id);
            }
            catch (Exception ex)
            {
                Log($"Download failed: {ex.Message}");
                MessageBox.Show($"Download failed: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                progressBar.Visible = false;
                lblProgress.Text = "";
            }
        }

        public async Task UpdateDeviceStatusAsync(DeviceContext device)
        {
            _currentDevice = device;

            lblDeviceStatus.Text = $"Device: {device.Brand} {device.Model}";
            Log($"Device connected: {device.Brand} {device.Model}");

            // Check DSU capability
            lblDsuStatus.Text = "DSU Support: Checking...";
            _deviceCapability = await _capabilityChecker.CheckCapabilityAsync(device);

            var statusIcon = _deviceCapability.Level switch
            {
                DsuCapabilityLevel.Excellent => "🟢",
                DsuCapabilityLevel.FullSupport => "🟢",
                DsuCapabilityLevel.PartialSupport => "🟡",
                _ => "🔴"
            };

            lblDsuStatus.Text = $"DSU Support: {statusIcon} {_deviceCapability.Level}";
            Log($"DSU Capability: {_deviceCapability.Level} - {_deviceCapability.Notes}");

            // Update preflight checks
            await UpdatePreflightChecksAsync();

            // Enable/disable method options
            rbDsuAdb.Enabled = _deviceCapability.SupportsDsuAdb;
            rbDsuRecovery.Enabled = _deviceCapability.SupportsDsuRecovery;
            rbABSlot.Enabled = _deviceCapability.SupportsABSlot && _deviceCapability.BootloaderUnlocked;

            // Enable flash button if ready
            btnFlashTest.Enabled = _selectedImage?.IsDownloaded == true && 
                                   _deviceCapability.Level != DsuCapabilityLevel.NotSupported;
        }

        private async Task UpdatePreflightChecksAsync()
        {
            if (_deviceCapability == null) return;

            // Free space
            var freeGb = _deviceCapability.FreeSpaceBytes / (1024.0 * 1024 * 1024);
            var spaceOk = freeGb >= 5;
            lblFreeSpace.Text = $"{(spaceOk ? "✓" : "⚠")} Free Space: {freeGb:F1} GB";
            lblFreeSpace.ForeColor = spaceOk ? Color.LightGreen : Color.Orange;

            // Battery
            var batteryLevel = 100; // Would need to query device
            try
            {
                var batteryStr = await _adb.ExecuteShellAsync("dumpsys battery | grep level");
                if (int.TryParse(batteryStr?.Replace("level:", "").Trim(), out var level))
                    batteryLevel = level;
            }
            catch { }
            
            var batteryOk = batteryLevel >= 50;
            lblBattery.Text = $"{(batteryOk ? "✓" : "⚠")} Battery: {batteryLevel}%";
            lblBattery.ForeColor = batteryOk ? Color.LightGreen : Color.Orange;

            // Bootloader
            var blOk = _deviceCapability.BootloaderUnlocked;
            lblBootloader.Text = $"{(blOk ? "✓" : "🔒")} Bootloader: {(blOk ? "Unlocked" : "Locked")}";
            lblBootloader.ForeColor = blOk ? Color.LightGreen : Color.Gray;

            // Compatibility
            var compatOk = _deviceCapability.Level != DsuCapabilityLevel.NotSupported;
            lblCompatibility.Text = $"{(compatOk ? "✓" : "✗")} Compatibility: {_deviceCapability.Level}";
            lblCompatibility.ForeColor = compatOk ? Color.LightGreen : Color.Red;
        }

        private async Task StartFlashAsync()
        {
            if (_currentDevice == null || _selectedImage == null || _deviceCapability == null)
            {
                MessageBox.Show("Please connect a device and select an image first.", "Error", 
                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            // Confirm
            var result = MessageBox.Show(
                $"Ready to flash {_selectedImage.Name} using {GetSelectedMethod()}.\n\n" +
                "This will temporarily boot a different ROM. Your original system will NOT be modified.\n\n" +
                "Continue?",
                "Confirm ROM Test",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Question);

            if (result != DialogResult.Yes) return;

            _isFlashing = true;
            _cts = new CancellationTokenSource();
            SetFlashingState(true);

            try
            {
                Log($"Starting DSU flash: {_selectedImage.Name}");

                var healthReport = await _flashOrchestrator.FlashDsuAsync(
                    _currentDevice,
                    _selectedImage,
                    GetSelectedMethod(),
                    new Progress<DsuFlashProgress>(OnFlashProgress),
                    _cts.Token);

                if (healthReport.IsHealthy)
                {
                    Log("✅ DSU boot successful!");
                    btnRevert.Enabled = true;

                    MessageBox.Show(
                        $"ROM booted successfully!\n\n" +
                        $"OS Version: {healthReport.OsVersion}\n" +
                        $"Boot Time: {healthReport.BootTimeSeconds}s\n" +
                        $"Play Services: {(healthReport.PlayServicesPresent ? "Yes" : "No")}\n\n" +
                        "Test the device, then use 'Revert to Original' when done.",
                        "ROM Test Active",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Information);
                }
                else
                {
                    Log("⚠️ Boot completed with issues");
                    foreach (var error in healthReport.BootErrors)
                        Log($"  Error: {error}");
                }
            }
            catch (OperationCanceledException)
            {
                Log("Flash cancelled by user");
            }
            catch (Exception ex)
            {
                Log($"Flash failed: {ex.Message}");
                MessageBox.Show($"Flash failed: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                _isFlashing = false;
                SetFlashingState(false);
            }
        }

        private async Task RevertAsync()
        {
            var result = MessageBox.Show(
                "Revert to original system?\n\nDevice will reboot to your original ROM.",
                "Confirm Revert",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Question);

            if (result != DialogResult.Yes) return;

            try
            {
                Log("Reverting to original system...");
                await _flashOrchestrator.RevertToOriginalAsync();
                Log("Revert initiated - device will boot to original system");
                btnRevert.Enabled = false;
            }
            catch (Exception ex)
            {
                Log($"Revert failed: {ex.Message}");
                MessageBox.Show($"Revert failed: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private async Task ClearCacheAsync()
        {
            var size = _gsiDatabase.GetCacheSizeBytes() / (1024.0 * 1024 * 1024);
            var result = MessageBox.Show(
                $"Clear all cached ROM images? ({size:F2} GB)",
                "Confirm Clear Cache",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Question);

            if (result != DialogResult.Yes) return;

            await _gsiDatabase.ClearCacheAsync();
            UpdateImageDropdown();
            Log("Cache cleared");
        }

        private DsuTestMethod GetSelectedMethod()
        {
            if (rbDsuRecovery.Checked) return DsuTestMethod.DsuRecovery;
            if (rbABSlot.Checked) return DsuTestMethod.ABSlot;
            return DsuTestMethod.DsuAdb;
        }

        private void OnFlashProgress(DsuFlashProgress progress)
        {
            if (InvokeRequired)
            {
                Invoke(new Action(() => OnFlashProgress(progress)));
                return;
            }

            progressBar.Visible = true;
            progressBar.Value = Math.Min(progress.PercentComplete, 100);
            lblProgress.Text = $"Stage {progress.StageNumber}/{progress.TotalStages}: {progress.Message}";

            if (progress.HasError)
            {
                lblProgress.ForeColor = Color.Red;
            }
            else if (progress.IsComplete)
            {
                progressBar.Visible = false;
                lblProgress.ForeColor = Color.LightGreen;
            }
        }

        private void SetFlashingState(bool flashing)
        {
            if (InvokeRequired)
            {
                Invoke(new Action(() => SetFlashingState(flashing)));
                return;
            }

            btnFlashTest.Enabled = !flashing;
            btnDownload.Enabled = !flashing;
            btnBrowse.Enabled = !flashing;
            cmbImages.Enabled = !flashing;
            grpTestMethod.Enabled = !flashing;
            progressBar.Visible = flashing;

            if (flashing)
            {
                btnFlashTest.Text = "⏳ Flashing...";
            }
            else
            {
                btnFlashTest.Text = "🚀 FLASH & TEST";
            }
        }

        private void Log(string message)
        {
            if (InvokeRequired)
            {
                Invoke(new Action(() => Log(message)));
                return;
            }

            var timestamp = DateTime.Now.ToString("HH:mm:ss");
            txtLog.AppendText($"[{timestamp}] {message}\n");
            txtLog.ScrollToCaret();
        }
    }
}
