using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Drivers;

namespace DeepEyeUnlocker.UI.Panels
{
    public class DriverProPanel : UserControl
    {
        private readonly DriverCenterEngine _engine;
        private DeviceContext? _device;
        private TableLayoutPanel _mainLayout = null!;
        private DataGridView _driverGrid = null!;
        private RichTextBox _logBox = null!;
        private Button _btnScan = null!;
        private Button _btnRepair = null!;
        private ProgressBar _progressBar = null!;
        private Label _lblStatus = null!;
        private Label _lblSummary = null!;

        public DriverProPanel()
        {
            _engine = new DriverCenterEngine();
            InitializeComponent();
        }

        public void SetDevice(DeviceContext? device)
        {
            _device = device;
            _lblStatus.Text = _device != null ? $"Context available: {_device.Mode}" : "No device context selected.";
        }

        private void InitializeComponent()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = Color.FromArgb(20, 20, 25); // Sentinel Dark
            this.ForeColor = Color.White;

            _mainLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                ColumnCount = 1,
                RowCount = 5,
                Padding = new Padding(20)
            };
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));  // Title
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 50));  // Toolbar
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 60));   // Grid
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 40));   // Log
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 40));  // Status Bar

            // 1. Title
            var titleLabel = new Label
            {
                Text = "🛡️ Sentinel Pro: Driver Integrity Audit",
                Font = new Font("Segoe UI", 16, FontStyle.Bold),
                ForeColor = Color.FromArgb(0, 150, 136), // Emerald
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 10)
            };

            // 2. Toolbar
            var toolbar = new FlowLayoutPanel { Dock = DockStyle.Fill, Margin = new Padding(0) };
            _btnScan = CreateStyledButton("🔍 Full Stack Audit", Color.FromArgb(0, 150, 136));
            _btnScan.Click += async (s, e) => await PerformScanAsync();
            
            _btnRepair = CreateStyledButton("🛠️ Fix Integrity Issues", Color.FromArgb(255, 87, 34));
            _btnRepair.Enabled = false;
            _btnRepair.Click += async (s, e) => await PerformRepairAsync();

            toolbar.Controls.Add(_btnScan);
            toolbar.Controls.Add(_btnRepair);

            // 3. Grid
            _driverGrid = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.FromArgb(25, 25, 30),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                GridColor = Color.FromArgb(40, 40, 45),
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                AllowUserToAddRows = false,
                ReadOnly = true,
                RowHeadersVisible = false,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect,
                EnableHeadersVisualStyles = false
            };
            _driverGrid.ColumnHeadersDefaultCellStyle = new DataGridViewCellStyle { BackColor = Color.FromArgb(35, 35, 40), ForeColor = Color.Gray, Font = new Font("Segoe UI", 9, FontStyle.Bold) };
            _driverGrid.Columns.Add("Icon", "");
            _driverGrid.Columns[0].Width = 30;
            _driverGrid.Columns.Add("Name", "Component Name");
            _driverGrid.Columns.Add("HWID", "Hardware/Filter ID");
            _driverGrid.Columns.Add("Status", "Integrity State");
            _driverGrid.Columns.Add("Provider", "Audit Notes");

            // 4. Log Box
            _logBox = new RichTextBox
            {
                Dock = DockStyle.Fill,
                BackColor = Color.FromArgb(15, 15, 20),
                ForeColor = Color.FromArgb(150, 150, 160),
                BorderStyle = BorderStyle.None,
                ReadOnly = true,
                Font = new Font("Consolas", 9),
                Margin = new Padding(0, 10, 0, 0)
            };

            // 5. Status Bar
            var statusBar = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, RowCount = 1 };
            _lblStatus = new Label { Text = "Ready for audit.", AutoSize = true, ForeColor = Color.Gray, Anchor = AnchorStyles.Left };
            _progressBar = new ProgressBar { Dock = DockStyle.Fill, Visible = false, Style = ProgressBarStyle.Continuous, Height = 10, Margin = new Padding(10, 5, 0, 5) };
            statusBar.Controls.Add(_lblStatus, 0, 0);
            statusBar.Controls.Add(_progressBar, 1, 0);

            _mainLayout.Controls.Add(titleLabel, 0, 0);
            _mainLayout.Controls.Add(toolbar, 0, 1);
            _mainLayout.Controls.Add(_driverGrid, 0, 2);
            _mainLayout.Controls.Add(_logBox, 0, 3);
            _mainLayout.Controls.Add(statusBar, 0, 4);

            this.Controls.Add(_mainLayout);
        }

        private Button CreateStyledButton(string text, Color backColor)
        {
            var btn = new Button
            {
                Text = text,
                Width = 180,
                Height = 35,
                FlatStyle = FlatStyle.Flat,
                BackColor = backColor,
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                Cursor = Cursors.Hand,
                Margin = new Padding(0, 0, 10, 0)
            };
            btn.FlatAppearance.BorderSize = 0;
            return btn;
        }

        private void Log(string message, Color? color = null)
        {
            _logBox.SelectionStart = _logBox.TextLength;
            _logBox.SelectionColor = color ?? Color.Gray;
            _logBox.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
            _logBox.ScrollToCaret();
        }
        }

        private async Task PerformScanAsync()
        {
            _btnScan.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Value = 10;
            _lblStatus.Text = "Auditing Windows Driver Store and Registry Filters...";
            _lblStatus.ForeColor = Color.Gray;
            
            _logBox.Clear();
            Log("Initiating Sentinel Pro Audit Sequence...", Color.FromArgb(0, 150, 136));
            Log("Querying PnP enumeration for connected chipset targets...");
            
            var drivers = await _engine.ScanDriversAsync();
            
            _progressBar.Value = 60;
            _driverGrid.Rows.Clear();
            
            foreach (var d in drivers)
            {
                string icon = d.Status == DriverStatus.Healthy ? "✅" : (d.Status == DriverStatus.Conflict ? "⚠️" : "⚪");
                int idx = _driverGrid.Rows.Add(icon, d.Name, d.HardwareId, d.Status.ToString(), d.Provider);
                var row = _driverGrid.Rows[idx];

                if (d.Status == DriverStatus.Conflict)
                {
                    row.DefaultCellStyle.ForeColor = Color.Salmon;
                    Log($"CRITICAL: {d.Name} - conflict detected in registry filter.", Color.Salmon);
                }
                else if (d.Status == DriverStatus.Healthy)
                {
                    row.DefaultCellStyle.ForeColor = Color.LightGreen;
                }
            }

            _progressBar.Value = 100;
            int conflictCount = drivers.Count(d => d.Status == DriverStatus.Conflict);
            
            if (conflictCount > 0)
            {
                _lblStatus.Text = $"⚠️ Sentinel identified {conflictCount} critical issues pre-flight.";
                _lblStatus.ForeColor = Color.Salmon;
                _btnRepair.Enabled = true;
                _btnRepair.BackColor = Color.FromArgb(211, 47, 47);
                Log("Action Required: Resolve listed conflicts to ensure protocol stability.", Color.Orange);
            }
            else
            {
                _lblStatus.Text = "✅ No critical conflicts identified in active stack.";
                _lblStatus.ForeColor = Color.LightGreen;
                _btnRepair.Enabled = false;
                _btnRepair.BackColor = Color.FromArgb(45, 45, 50);
                Log("System state validated for BROM/EDL operations.", Color.LightGreen);
            }
            
            _btnScan.Enabled = true;
            await Task.Delay(1000);
            _progressBar.Visible = false;
        }

        private async Task PerformRepairAsync()
        {
            var res = MessageBox.Show("Sentinel Pro will now attempt to purge conflicting registry filters and legacy INF files. Connected devices may flicker. Proceed?", 
                "Driver Integrity Repair", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
            
            if (res != DialogResult.Yes) return;

            _btnRepair.Enabled = false;
            _btnScan.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Value = 20;

            _lblStatus.Text = "Executing deep-system driver cleanup...";
            _lblStatus.ForeColor = Color.Orange;
            
            Log("Initiating professional repair sequence...", Color.Orange);
            Log("Purging known registry filter conflicts...");
            
            _progressBar.Value = 40;
            bool success = await _engine.RepairDriverPresetAsync("Conflict Purge");
            
            _progressBar.Value = 80;
            if (success)
            {
                Log("REPAIR SUCCESSFUL: Driver stack has been sanitized.", Color.LightGreen);
                _progressBar.Value = 100;
                
                MessageBox.Show("Driver stack successfully cleaned. Please re-plug your device for changes to take effect.", 
                    "Repair Complete", MessageBoxButtons.OK, MessageBoxIcon.Information);
                
                await PerformScanAsync();
            }
            else
            {
                Log("ERROR: Repair sequence failed. Access denied or system busy.", Color.Salmon);
                _lblStatus.Text = "Error during automated repair.";
                _btnRepair.Enabled = true;
                _btnScan.Enabled = true;
            }

            await Task.Delay(1000);
            _progressBar.Visible = false;
        }
    }
}
