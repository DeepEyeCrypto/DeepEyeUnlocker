using System;
using System.Drawing;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;

namespace DeepEyeUnlocker.UI.Panels
{
    /// <summary>
    /// Device Information Center - Comprehensive device diagnostics
    /// </summary>
    public class DeviceInfoPanel : Panel
    {
        private DeviceContext? _device;
        private DeviceInfoManager _infoManager = new();
        private DeviceInfo? _currentInfo;

        // UI Components
        private Label _titleLabel = null!;
        private Label _deviceLabel = null!;

        // Info Cards
        private Panel _deviceCard = null!;
        private Panel _softwareCard = null!;
        private Panel _hardwareCard = null!;
        private Panel _storageCard = null!;
        private Panel _batteryCard = null!;
        private Panel _securityCard = null!;

        // Buttons
        private Button _scanButton = null!;
        private Button _exportTextButton = null!;
        private Button _exportJsonButton = null!;
        private Button _copyButton = null!;

        // Log
        private RichTextBox _logBox = null!;

        public DeviceInfoPanel()
        {
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device)
        {
            _device = device;
            UpdateDeviceLabel();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Size = new Size(580, 720);
            this.AutoScroll = true;

            int y = 10;

            // Title
            _titleLabel = new Label
            {
                Text = "📱 Device Information Center",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_titleLabel);
            y += 45;

            // Device Label
            _deviceLabel = new Label
            {
                Text = "No device connected",
                Font = new Font("Segoe UI", 10),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_deviceLabel);
            y += 30;

            // Buttons Row
            _scanButton = CreateButton("🔍 Scan Device", 15, y, 140, 35, Color.FromArgb(0, 123, 255));
            _scanButton.Click += OnScanClicked;
            this.Controls.Add(_scanButton);

            _exportTextButton = CreateButton("📄 Export Text", 165, y, 120, 35, Color.FromArgb(108, 117, 125));
            _exportTextButton.Click += OnExportTextClicked;
            this.Controls.Add(_exportTextButton);

            _exportJsonButton = CreateButton("📋 Export JSON", 295, y, 120, 35, Color.FromArgb(108, 117, 125));
            _exportJsonButton.Click += OnExportJsonClicked;
            this.Controls.Add(_exportJsonButton);

            _copyButton = CreateButton("📝 Copy", 425, y, 80, 35, Color.FromArgb(108, 117, 125));
            _copyButton.Click += OnCopyClicked;
            this.Controls.Add(_copyButton);

            y += 50;

            // Info Cards (2 column layout)
            int cardWidth = 265;
            int cardHeight = 120;
            int gap = 10;

            // Row 1
            _deviceCard = CreateInfoCard("📱 Device", 15, y, cardWidth, cardHeight);
            this.Controls.Add(_deviceCard);

            _softwareCard = CreateInfoCard("🔧 Software", 15 + cardWidth + gap, y, cardWidth, cardHeight);
            this.Controls.Add(_softwareCard);

            y += cardHeight + gap;

            // Row 2
            _hardwareCard = CreateInfoCard("⚙️ Hardware", 15, y, cardWidth, cardHeight);
            this.Controls.Add(_hardwareCard);

            _storageCard = CreateInfoCard("💾 Storage", 15 + cardWidth + gap, y, cardWidth, cardHeight);
            this.Controls.Add(_storageCard);

            y += cardHeight + gap;

            // Row 3
            _batteryCard = CreateInfoCard("🔋 Battery", 15, y, cardWidth, cardHeight);
            this.Controls.Add(_batteryCard);

            _securityCard = CreateInfoCard("🔐 Security", 15 + cardWidth + gap, y, cardWidth, cardHeight);
            this.Controls.Add(_securityCard);

            y += cardHeight + 15;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(15, y),
                Size = new Size(540, 100),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            LogMessage("Device Info Center ready. Connect a device and click Scan.");
        }

        private Panel CreateInfoCard(string title, int x, int y, int width, int height)
        {
            var card = new Panel
            {
                Location = new Point(x, y),
                Size = new Size(width, height),
                BackColor = Color.FromArgb(40, 40, 45),
                BorderStyle = BorderStyle.None
            };

            var titleLabel = new Label
            {
                Text = title,
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                ForeColor = Color.FromArgb(100, 200, 255),
                Location = new Point(10, 8),
                AutoSize = true
            };
            card.Controls.Add(titleLabel);

            var contentLabel = new Label
            {
                Name = "content",
                Text = "--",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(200, 200, 200),
                Location = new Point(10, 32),
                Size = new Size(width - 20, height - 40)
            };
            card.Controls.Add(contentLabel);

            // Rounded corners effect
            card.Paint += (s, e) =>
            {
                var rect = new Rectangle(0, 0, card.Width - 1, card.Height - 1);
                using var pen = new Pen(Color.FromArgb(60, 60, 70), 1);
                e.Graphics.DrawRectangle(pen, rect);
            };

            return card;
        }

        private Button CreateButton(string text, int x, int y, int w, int h, Color color)
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

        private void UpdateDeviceLabel()
        {
            if (_device == null)
            {
                _deviceLabel.Text = "No device connected";
                _scanButton.Enabled = false;
            }
            else
            {
                _deviceLabel.Text = $"Connected: {_device.Brand} {_device.Model} ({_device.Mode})";
                _scanButton.Enabled = true;
            }
        }

        private async void OnScanClicked(object? sender, EventArgs e)
        {
            if (_device == null) return;

            _scanButton.Enabled = false;
            LogMessage("\n--- Collecting Device Information ---", Color.Cyan);

            try
            {
                var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(60));

                _currentInfo = await _infoManager.GetFullInfoAsync(_device, progress, cts.Token);
                UpdateCards();

                LogMessage("Scan complete!", Color.FromArgb(40, 167, 69));
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _scanButton.Enabled = true;
            }
        }

        private void UpdateCards()
        {
            if (_currentInfo == null) return;

            // Device Card
            SetCardContent(_deviceCard, 
                $"Brand: {_currentInfo.Brand}\n" +
                $"Model: {_currentInfo.Model}\n" +
                $"Device: {_currentInfo.Device}\n" +
                $"Serial: {MaskSerial(_currentInfo.Serial)}");

            // Software Card
            SetCardContent(_softwareCard,
                $"Android: {_currentInfo.AndroidVersion} (SDK {_currentInfo.SdkVersion})\n" +
                $"Build: {_currentInfo.BuildId}\n" +
                $"Security: {_currentInfo.SecurityPatch}");

            // Hardware Card
            if (_currentInfo.Hardware != null)
            {
                var hw = _currentInfo.Hardware;
                SetCardContent(_hardwareCard,
                    $"Chipset: {hw.Chipset}\n" +
                    $"Platform: {hw.Platform}\n" +
                    $"RAM: {hw.TotalRamMB} MB\n" +
                    $"Screen: {hw.ScreenResolution}");
            }

            // Storage Card
            if (_currentInfo.Storage != null)
            {
                var st = _currentInfo.Storage;
                var totalGB = st.InternalTotalKB / 1024.0 / 1024.0;
                var usedGB = st.InternalUsedKB / 1024.0 / 1024.0;
                var usedPct = totalGB > 0 ? (usedGB / totalGB * 100) : 0;

                SetCardContent(_storageCard,
                    $"Total: {totalGB:F1} GB\n" +
                    $"Used: {usedGB:F1} GB ({usedPct:F0}%)\n" +
                    $"Free: {(st.InternalAvailableKB / 1024.0 / 1024.0):F1} GB");
            }

            // Battery Card
            if (_currentInfo.Battery != null)
            {
                var bat = _currentInfo.Battery;
                var tempColor = bat.Temperature > 40 ? "⚠️ " : "";
                SetCardContent(_batteryCard,
                    $"Level: {bat.Level}%\n" +
                    $"Health: {bat.Health}\n" +
                    $"{tempColor}Temp: {bat.Temperature:F1}°C\n" +
                    $"Status: {bat.Status}");
            }

            // Security Card
            if (_currentInfo.Security != null)
            {
                var sec = _currentInfo.Security;
                var blIcon = sec.BootloaderLocked ? "🔒" : "🔓";
                var rootIcon = sec.SuBinaryFound ? "⚠️ ROOT" : "✓ Stock";
                SetCardContent(_securityCard,
                    $"Bootloader: {blIcon} {(sec.BootloaderLocked ? "Locked" : "Unlocked")}\n" +
                    $"SELinux: {sec.SelinuxMode}\n" +
                    $"Encryption: {sec.EncryptionState}\n" +
                    $"Status: {rootIcon}");
            }
        }

        private void SetCardContent(Panel card, string content)
        {
            foreach (Control c in card.Controls)
            {
                if (c.Name == "content" && c is Label label)
                {
                    label.Text = content;
                    break;
                }
            }
        }

        private string MaskSerial(string? serial)
        {
            if (string.IsNullOrEmpty(serial) || serial.Length < 6)
                return serial ?? "--";
            return serial[..3] + "***" + serial[^3..];
        }

        private async void OnExportTextClicked(object? sender, EventArgs e)
        {
            if (_currentInfo == null)
            {
                MessageBox.Show("Please scan a device first.", "No Data", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            using var dialog = new SaveFileDialog
            {
                Filter = "Text File (*.txt)|*.txt",
                FileName = $"device_info_{_currentInfo.Model}_{DateTime.Now:yyyyMMdd_HHmmss}.txt"
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                var report = _infoManager.GenerateReport(_currentInfo);
                await File.WriteAllTextAsync(dialog.FileName, report);
                LogMessage($"Report saved: {dialog.FileName}", Color.FromArgb(40, 167, 69));
            }
        }

        private async void OnExportJsonClicked(object? sender, EventArgs e)
        {
            if (_currentInfo == null)
            {
                MessageBox.Show("Please scan a device first.", "No Data", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            using var dialog = new SaveFileDialog
            {
                Filter = "JSON File (*.json)|*.json",
                FileName = $"device_info_{_currentInfo.Model}_{DateTime.Now:yyyyMMdd_HHmmss}.json"
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                await _infoManager.ExportJsonAsync(_currentInfo, dialog.FileName);
                LogMessage($"JSON exported: {dialog.FileName}", Color.FromArgb(40, 167, 69));
            }
        }

        private void OnCopyClicked(object? sender, EventArgs e)
        {
            if (_currentInfo == null)
            {
                MessageBox.Show("Please scan a device first.", "No Data", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            var report = _infoManager.GenerateReport(_currentInfo);
            Clipboard.SetText(report);
            LogMessage("Report copied to clipboard!", Color.FromArgb(40, 167, 69));
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
