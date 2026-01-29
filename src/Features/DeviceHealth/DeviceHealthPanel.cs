using System;
using System.Drawing;
using System.Windows.Forms;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.DeviceHealth
{
    public class DeviceHealthPanel : Panel
    {
        private readonly IAdbClient _adb;
        private readonly DeviceHealthScanner _scanner;
        
        public event Action<DeviceHealthReport>? ReportScanned;

        private Label _headerLabel = null!;
        private Label _statusLabel = null!;
        private FlowLayoutPanel _findingsPanel = null!;
        private Button _scanButton = null!;
        private RichTextBox _reportDisplay = null!;

        public DeviceHealthPanel(IAdbClient adb)
        {
            _adb = adb;
            _scanner = new DeviceHealthScanner(adb);
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device)
        {
            _adb.TargetSerial = device?.Serial;
            _scanButton.Enabled = device != null && device.Mode == ConnectionMode.ADB;
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Size = new Size(560, 700);

            _headerLabel = new Label
            {
                Text = "🏥 Device Health Center",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, 15),
                AutoSize = true
            };
            this.Controls.Add(_headerLabel);

            var subLabel = new Label
            {
                Text = "Advanced Security Audit & Hardware Diagnostics",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(20, 50),
                AutoSize = true
            };
            this.Controls.Add(subLabel);

            _scanButton = new Button
            {
                Text = "🔍 Start Deep Audit",
                Location = new Point(20, 80),
                Size = new Size(180, 40),
                BackColor = Color.FromArgb(0, 122, 204),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10, FontStyle.Bold)
            };
            _scanButton.FlatAppearance.BorderSize = 0;
            _scanButton.Click += OnScanClicked;
            this.Controls.Add(_scanButton);

            _statusLabel = new Label
            {
                Text = "Ready to scan",
                Font = new Font("Segoe UI", 9, FontStyle.Italic),
                ForeColor = Color.Gray,
                Location = new Point(210, 92),
                AutoSize = true
            };
            this.Controls.Add(_statusLabel);

            _reportDisplay = new RichTextBox
            {
                Location = new Point(20, 140),
                Size = new Size(520, 300),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(0, 255, 127),
                Font = new Font("Consolas", 9),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_reportDisplay);

            var findingsLabel = new Label
            {
                Text = "⚠️ Security & Health Findings",
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                ForeColor = Color.FromArgb(255, 193, 7),
                Location = new Point(20, 455),
                AutoSize = true
            };
            this.Controls.Add(findingsLabel);

            _findingsPanel = new FlowLayoutPanel
            {
                Location = new Point(20, 480),
                Size = new Size(520, 200),
                AutoScroll = true,
                BackColor = Color.FromArgb(35, 35, 40)
            };
            this.Controls.Add(_findingsPanel);
        }

        private async void OnScanClicked(object? sender, EventArgs e)
        {
            _scanButton.Enabled = false;
            _statusLabel.Text = "Scanning... (this may take 10-20s)";
            _statusLabel.ForeColor = Color.Cyan;
            _reportDisplay.Clear();
            _findingsPanel.Controls.Clear();

            try
            {
                var report = await _scanner.ScanAsync();
                DisplayReport(report);
                ReportScanned?.Invoke(report);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Audit Failed: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                _statusLabel.Text = "Scan failed";
                _statusLabel.ForeColor = Color.Red;
            }
            finally
            {
                _scanButton.Enabled = true;
            }
        }

        private void DisplayReport(DeviceHealthReport report)
        {
            _statusLabel.Text = "Scan complete";
            _statusLabel.ForeColor = Color.Lime;

            _reportDisplay.AppendText($"[DEEPEYE AUDIT REPORT - {report.ScanTimestamp:yyyy-MM-dd HH:mm:ss}]\n");
            _reportDisplay.AppendText(new string('=', 45) + "\n");
            _reportDisplay.AppendText($"SN:         {report.SerialNumber}\n");
            _reportDisplay.AppendText($"ANDROID:    {report.AndroidVersion} (Patch: {report.SecurityPatchLevel})\n");
            _reportDisplay.AppendText($"KERNEL:     {report.KernelVersion}\n");
            _reportDisplay.AppendText($"ROOT:       {(report.IsRooted ? "YES (" + report.RootMethod + ")" : "NO")}\n");
            _reportDisplay.AppendText($"BOOTLOADER: {(report.IsBootloaderUnlocked ? "UNLOCKED" : "LOCKED")}\n");
            _reportDisplay.AppendText($"BATTERY:    {report.BatteryLevel}% ({report.BatteryTemperature}°C, Health: {report.BatteryHealth}%)\n");
            _reportDisplay.AppendText($"VERIFY BOOT:{report.VerifiedBootState}\n");
            _reportDisplay.AppendText(new string('=', 45) + "\n");

            foreach (var finding in report.AuditFindings)
            {
                var label = new Label
                {
                    Text = "• " + finding,
                    ForeColor = finding.Contains("Critical") || finding.Contains("Failed") ? Color.Red : Color.White,
                    Font = new Font("Segoe UI", 9),
                    Size = new Size(480, 30),
                    TextAlign = ContentAlignment.MiddleLeft
                };
                _findingsPanel.Controls.Add(label);
            }
        }
    }
}
