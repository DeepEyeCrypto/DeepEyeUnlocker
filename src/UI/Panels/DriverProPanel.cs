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
        private DataGridView _driverGrid = null!;
        private Button _btnScan = null!;
        private Button _btnRepair = null!;
        private Label _lblStatus = null!;

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
            this.BackColor = Color.FromArgb(18, 18, 18);
            this.ForeColor = Color.White;

            var mainLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                ColumnCount = 1,
                RowCount = 3,
                Padding = new Padding(15)
            };
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));

            // 1. Toolbar
            var toolbar = new FlowLayoutPanel { Dock = DockStyle.Fill };
            _btnScan = new Button { Text = "🔍 Scan Driver Stack", Width = 160, Height = 35, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(0, 150, 136) };
            _btnScan.Click += async (s, e) => await PerformScanAsync();
            
            _btnRepair = new Button { Text = "🛠 Fix Conflicts", Width = 140, Height = 35, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(255, 87, 34), Enabled = false };
            _btnRepair.Click += async (s, e) => await PerformRepairAsync();

            toolbar.Controls.Add(_btnScan);
            toolbar.Controls.Add(_btnRepair);

            // 2. Grid
            _driverGrid = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.FromArgb(24, 24, 24),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                AllowUserToAddRows = false,
                ReadOnly = true,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect
            };
            _driverGrid.Columns.Add("Name", "Driver Name");
            _driverGrid.Columns.Add("HWID", "Hardware ID");
            _driverGrid.Columns.Add("Status", "Status");
            _driverGrid.Columns.Add("Provider", "Provider");

            // 3. Status
            _lblStatus = new Label { Text = "Scan needed to verify driver integrity.", AutoSize = true, Anchor = AnchorStyles.Left };

            mainLayout.Controls.Add(toolbar, 0, 0);
            mainLayout.Controls.Add(_driverGrid, 0, 1);
            mainLayout.Controls.Add(_lblStatus, 0, 2);

            this.Controls.Add(mainLayout);
        }

        private async Task PerformScanAsync()
        {
            _btnScan.Enabled = false;
            _lblStatus.Text = "Analyzing PnP device stack...";
            
            var drivers = await _engine.ScanDriversAsync();
            
            _driverGrid.Rows.Clear();
            foreach (var d in drivers)
            {
                int idx = _driverGrid.Rows.Add(d.Name, d.HardwareId, d.Status.ToString(), d.Provider);
                if (d.Status == DriverStatus.Conflict || d.Status == DriverStatus.Missing)
                {
                    _driverGrid.Rows[idx].DefaultCellStyle.ForeColor = Color.Salmon;
                }
            }

            _lblStatus.Text = $"Scan complete. Found {drivers.Count} drivers.";
            _btnRepair.Enabled = drivers.Any(d => d.Status != DriverStatus.Healthy);
            _btnScan.Enabled = true;
        }

        private async Task PerformRepairAsync()
        {
            var res = MessageBox.Show("Repairing drivers may temporarily disconnect connected devices. Proceed with Driver Stack Cleanup?", "Sentinel Pro - Driver Repair", MessageBoxButtons.YesNo, MessageBoxIcon.Information);
            if (res != DialogResult.Yes) return;

            _btnRepair.Enabled = false;
            _lblStatus.Text = "Applying driver repair presets...";
            
            bool success = await _engine.RepairDriverPresetAsync("Generic Repair");
            
            if (success)
            {
                MessageBox.Show("Driver stack successfully repaired. Please re-plug your devices.", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                await PerformScanAsync();
            }
        }
    }
}
