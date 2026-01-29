using System;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.DeviceHealth;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.UI.Panels
{
    public class DeviceHealthPanel : UserControl
    {
        private readonly IAdbClient _adb;
        private readonly DeviceHealthScanner _scanner;
        private DeviceContext? _device;
        
        private TableLayoutPanel _mainLayout = null!;
        private DataGridView _healthDataGrid = null!;
        private Button _btnScan = null!;
        private Button _btnExport = null!;
        private Label _lblStatus = null!;
        private ProgressBar _progressBar = null!;
        private DeviceHealthReport? _lastReport;

        public DeviceHealthPanel(IAdbClient adb)
        {
            _adb = adb ?? throw new ArgumentNullException(nameof(adb));
            _scanner = new DeviceHealthScanner(_adb);
            InitializeComponent();
        }

        public void SetDevice(DeviceContext? device)
        {
            _device = device;
            _btnScan.Enabled = _device != null && _device.Mode == ConnectionMode.ADB;
        }

        private void InitializeComponent()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = Color.FromArgb(18, 18, 18);
            this.ForeColor = Color.White;

            _mainLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                ColumnCount = 1,
                RowCount = 3,
                Padding = new Padding(10)
            };
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 50));
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));

            // Header Section
            var headerLayout = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.LeftToRight };
            _btnScan = CreateStyledButton("Start Deep Audit", Color.FromArgb(0, 150, 136));
            _btnScan.Click += async (s, e) => await StartScanAsync();
            
            _btnExport = CreateStyledButton("Export Report", Color.FromArgb(33, 150, 243));
            _btnExport.Enabled = false;
            _btnExport.Click += (s, e) => ExportReport();

            headerLayout.Controls.Add(_btnScan);
            headerLayout.Controls.Add(_btnExport);
            
            // Grid Section
            _healthDataGrid = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.FromArgb(24, 24, 24),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                ColumnHeadersVisible = true,
                RowHeadersVisible = false,
                AllowUserToAddRows = false,
                ReadOnly = true,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect
            };
            _healthDataGrid.Columns.Add("Property", "Property");
            _healthDataGrid.Columns.Add("Value", "Value");
            _healthDataGrid.Columns.Add("Status", "Status");

            // Footer Section
            var footerLayout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2 };
            _lblStatus = new Label { Text = "Ready to scan...", AutoSize = true, Anchor = AnchorStyles.Left };
            _progressBar = new ProgressBar { Dock = DockStyle.Fill, Height = 10, Visible = false };
            
            footerLayout.Controls.Add(_lblStatus, 0, 0);
            footerLayout.Controls.Add(_progressBar, 1, 0);

            _mainLayout.Controls.Add(headerLayout, 0, 0);
            _mainLayout.Controls.Add(_healthDataGrid, 0, 1);
            _mainLayout.Controls.Add(footerLayout, 0, 2);

            this.Controls.Add(_mainLayout);
        }

        private Button CreateStyledButton(string text, Color backColor)
        {
            return new Button
            {
                Text = text,
                Width = 150,
                Height = 35,
                FlatStyle = FlatStyle.Flat,
                BackColor = backColor,
                ForeColor = Color.White,
                Cursor = Cursors.Hand
            };
        }


        private async Task StartScanAsync()
        {
            if (!_adb.IsConnected())
            {
                MessageBox.Show("Please connect an ADB device first.", "No Device", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            try
            {
                _btnScan.Enabled = false;
                _lblStatus.Text = "Scanning device hardware and security...";
                _progressBar.Visible = true;
                _progressBar.Style = ProgressBarStyle.Marquee;
                _healthDataGrid.Rows.Clear();

                _lastReport = await _scanner.ScanAsync();
                
                PopulateGrid(_lastReport);

                _lblStatus.Text = $"Audit complete. Rooted: {_lastReport.IsRooted}";
                _btnExport.Enabled = true;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Health audit failed");
                _lblStatus.Text = "Audit failed: " + ex.Message;
            }
            finally
            {
                _btnScan.Enabled = true;
                _progressBar.Visible = false;
            }
        }

        private void PopulateGrid(DeviceHealthReport r)
        {
            AddRow("Serial Number", r.SerialNumber, "OK");
            AddRow("Android Version", r.AndroidVersion, "OK");
            AddRow("Security Patch", r.SecurityPatchLevel, GetPatchStatus(r.SecurityPatchLevel));
            AddRow("Battery Level", $"{r.BatteryLevel}%", r.BatteryLevel < 20 ? "LOW" : "OK");
            AddRow("Battery Health", $"{r.BatteryHealth}%", r.BatteryHealth < 80 ? "POOR" : "EXCELLENT");
            AddRow("Root Status", r.IsRooted ? "YES" : "NO", r.IsRooted ? "⚠️ RISKY" : "✅ SAFE");
            AddRow("Bootloader", r.IsBootloaderUnlocked ? "UNLOCKED" : "LOCKED", r.IsBootloaderUnlocked ? "⚠️ EXPOSED" : "✅ SECURE");
            AddRow("SELinux", r.IsSelinuxEnforcing ? "Enforcing" : "Permissive", r.IsSelinuxEnforcing ? "✅ PROTECTED" : "❌ EXPOSED");
            AddRow("Dev Options", r.IsDevOptionsEnabled ? "ENABLED" : "DISABLED", "OK");
        }

        private void AddRow(string prop, string val, string status)
        {
            int idx = _healthDataGrid.Rows.Add(prop, val, status);
            if (status.Contains("RISKY") || status.Contains("EXPOSED") || status.Contains("LOW") || status.Contains("❌"))
            {
                _healthDataGrid.Rows[idx].Cells[2].Style.ForeColor = Color.Salmon;
            }
            else if (status.Contains("SAFE") || status.Contains("SECURE") || status.Contains("PROTECTED") || status.Contains("✅"))
            {
                _healthDataGrid.Rows[idx].Cells[2].Style.ForeColor = Color.LightGreen;
            }
        }

        private string GetPatchStatus(string patch)
        {
            if (string.IsNullOrEmpty(patch)) return "Unknown";
            if (DateTime.TryParse(patch, out DateTime patchDate))
            {
                var diff = DateTime.Now - patchDate;
                if (diff.TotalDays > 365) return "❌ CRITICALLY OUTDATED";
                if (diff.TotalDays > 180) return "⚠️ OUTDATED";
            }
            return "✅ SECURE";
        }

        private void ExportReport()
        {
            if (_lastReport == null) return;

            using (var sfd = new SaveFileDialog())
            {
                sfd.Filter = "Markdown Report (*.md)|*.md|JSON Data (*.json)|*.json";
                sfd.FileName = $"HealthReport_{_lastReport.SerialNumber}_{DateTime.Now:yyyyMMdd}";

                if (sfd.ShowDialog() == DialogResult.OK)
                {
                    var format = Path.GetExtension(sfd.FileName).ToLower() == ".json" ? ExportFormat.Json : ExportFormat.Markdown;
                    Task.Run(async () => 
                    {
                        await ReportExporter.ExportAsync(_lastReport, sfd.FileName, format);
                        MessageBox.Show("Report exported successfully!", "Export Complete", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    });
                }
            }
        }
    }
}
