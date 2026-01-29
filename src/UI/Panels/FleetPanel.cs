using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Features.Fleet;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.UI.Panels
{
    public class FleetPanel : UserControl
    {
        private readonly FleetManager _manager;
        private DataGridView _deviceList = null!;
        private Button _btnRefresh = null!;
        private Button _btnBatchAdb = null!;
        private Label _lblFleetStats = null!;

        public FleetPanel(IAdbClient adb)
        {
            _manager = new FleetManager(adb);
            InitializeComponent();
            _deviceList.SelectionChanged += OnSelectionChanged;
        }

        public FleetManager FleetManager => _manager;

        private void OnSelectionChanged(object? sender, EventArgs e)
        {
            if (_deviceList.SelectedRows.Count == 1)
            {
                var serial = _deviceList.SelectedRows[0].Cells[0].Value.ToString();
                var devices = _manager.GetDevices();
                var selectedFleetDevice = devices.FirstOrDefault(d => d.Context.Serial == serial);
                if (selectedFleetDevice != null)
                {
                    _manager.SelectedDevice = selectedFleetDevice.Context;
                }
            }
            else
            {
                _manager.SelectedDevice = null;
            }
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
            _btnRefresh = new Button { Text = "🚢 Refresh Fleet", Width = 140, Height = 35, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(33, 150, 243) };
            _btnRefresh.Click += async (s, e) => await RefreshFleetAsync();
            
            _btnBatchAdb = new Button { Text = "⚡ Batch ADB Shell", Width = 150, Height = 35, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(76, 175, 80) };
            _btnBatchAdb.Click += async (s, e) => await ShowBatchAdbDialogAsync();

            toolbar.Controls.Add(_btnRefresh);
            toolbar.Controls.Add(_btnBatchAdb);

            // 2. Grid
            _deviceList = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.FromArgb(24, 24, 24),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect,
                AllowUserToAddRows = false,
                ReadOnly = true
            };
            _deviceList.Columns.Add("Serial", "Serial Number");
            _deviceList.Columns.Add("Alias", "Alias (Workstation)");
            _deviceList.Columns.Add("Model", "Device Model");
            _deviceList.Columns.Add("Status", "Current Status");

            // 3. Stats
            _lblFleetStats = new Label { Text = "Ships in Fleet: 0", AutoSize = true, Anchor = AnchorStyles.Left, Font = new Font("Segoe UI", 9, FontStyle.Bold) };

            mainLayout.Controls.Add(toolbar, 0, 0);
            mainLayout.Controls.Add(_deviceList, 0, 1);
            mainLayout.Controls.Add(_lblFleetStats, 0, 2);

            this.Controls.Add(mainLayout);
        }

        private async Task RefreshFleetAsync()
        {
            _btnRefresh.Enabled = false;
            await _manager.RefreshDevicesAsync();
            
            _deviceList.Rows.Clear();
            var devices = _manager.GetDevices();
            foreach (var dev in devices)
            {
                _deviceList.Rows.Add(dev.Context.Serial, dev.Alias, dev.Context.Model, dev.LastStatus);
            }

            _lblFleetStats.Text = $"Ships in Fleet: {devices.Count()}";
            _btnRefresh.Enabled = true;
        }

        private async Task ShowBatchAdbDialogAsync()
        {
            var selectedRows = _deviceList.SelectedRows;
            if (selectedRows.Count == 0)
            {
                MessageBox.Show("Please select one or more devices to command.", "Fleet Order Required", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            string command = Microsoft.VisualBasic.Interaction.InputBox("Enter ADB Shell Command to run on selected fleet:", "Batch Command Hub", "reboot recovery");
            if (string.IsNullOrEmpty(command)) return;

            var serials = selectedRows.Cast<DataGridViewRow>().Select(r => r.Cells[0].Value.ToString()!).ToList();
            var result = await _manager.BatchExecuteShellAsync(serials, command);

            MessageBox.Show($"Fleet Command Dispatched.\nSuccess: {result.SuccessCount}\nFailed: {result.FailCount}", "Batch Result", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
    }
}
