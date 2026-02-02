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
        private readonly FleetOrchestrator _manager;
        private DataGridView _deviceList = null!;
        private Button _btnRefresh = null!;
        private Button _btnBatchAdb = null!;
        private Button _btnBatchInstall = null!;
        private Button _btnBatchReboot = null!;
        private Label _lblFleetStats = null!;

        public FleetPanel(IAdbClient adb)
        {
            _manager = new FleetOrchestrator(adb);
            InitializeComponent();
            _deviceList.SelectionChanged += OnSelectionChanged;
        }

        public FleetOrchestrator FleetManager => _manager;

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
            
            _btnBatchAdb = new Button { Text = "⚡ Batch Shell", Width = 130, Height = 35, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(76, 175, 80) };
            _btnBatchAdb.Click += async (s, e) => await ShowBatchAdbDialogAsync();

            _btnBatchInstall = new Button { Text = "📦 Batch Install", Width = 130, Height = 35, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(255, 152, 0) };
            _btnBatchInstall.Click += async (s, e) => await ShowBatchInstallDialogAsync();

            _btnBatchReboot = new Button { Text = "🔄 Batch Reboot", Width = 130, Height = 35, FlatStyle = FlatStyle.Flat, BackColor = Color.FromArgb(156, 39, 176) };
            _btnBatchReboot.Click += ShowBatchRebootMenu;

            toolbar.Controls.AddRange(new Control[] { _btnRefresh, _btnBatchAdb, _btnBatchInstall, _btnBatchReboot });

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
                ReadOnly = true,
                EnableHeadersVisualStyles = false
            };
            _deviceList.ColumnHeadersDefaultCellStyle.BackColor = Color.FromArgb(40, 40, 40);
            _deviceList.ColumnHeadersDefaultCellStyle.ForeColor = Color.White;
            _deviceList.Columns.Add("Serial", "Serial Number");
            _deviceList.Columns.Add("Alias", "Alias");
            _deviceList.Columns.Add("Model", "Model");
            _deviceList.Columns.Add("Status", "Status");

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
            _statusLabel_Update("Scanning USB bus...");
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

        private void _statusLabel_Update(string msg) => _lblFleetStats.Text = $"Fleet: {msg}";

        private async Task ShowBatchAdbDialogAsync()
        {
            var selectedRows = _deviceList.SelectedRows;
            if (selectedRows.Count == 0)
            {
                MessageBox.Show("Select devices for command dispatch.", "Fleet Command", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            string command = Microsoft.VisualBasic.Interaction.InputBox("ADB Shell Command:", "Batch Command Hub", "getprop ro.product.model");
            if (string.IsNullOrEmpty(command)) return;

            var serials = selectedRows.Cast<DataGridViewRow>().Select(r => r.Cells[0].Value.ToString()!).ToList();
            var result = await _manager.BatchExecuteShellAsync(serials, command);
            ShowBatchResult(result);
        }

        private async Task ShowBatchInstallDialogAsync()
        {
            var selectedRows = _deviceList.SelectedRows;
            if (selectedRows.Count == 0) return;

            using (var ofd = new OpenFileDialog { Filter = "APK Files|*.apk" })
            {
                if (ofd.ShowDialog() == DialogResult.OK)
                {
                    var serials = selectedRows.Cast<DataGridViewRow>().Select(r => r.Cells[0].Value.ToString()!).ToList();
                    var result = await _manager.BatchInstallApkAsync(serials, ofd.FileName);
                    ShowBatchResult(result);
                }
            }
        }

        private void ShowBatchRebootMenu(object? sender, EventArgs e)
        {
            var menu = new ContextMenuStrip();
            menu.Items.Add("Normal Reboot", null, async (s, ev) => await ExecuteBatchReboot("normal"));
            menu.Items.Add("Recovery Mode", null, async (s, ev) => await ExecuteBatchReboot("recovery"));
            menu.Items.Add("Bootloader (Fastboot)", null, async (s, ev) => await ExecuteBatchReboot("bootloader"));
            menu.Items.Add("EDL (Emergency Download)", null, async (s, ev) => await ExecuteBatchReboot("edl"));
            
            if (sender is Control c) menu.Show(c, new Point(0, c.Height));
        }

        private async Task ExecuteBatchReboot(string mode)
        {
            var selectedRows = _deviceList.SelectedRows;
            if (selectedRows.Count == 0) return;

            var serials = selectedRows.Cast<DataGridViewRow>().Select(r => r.Cells[0].Value.ToString()!).ToList();
            var result = await _manager.BatchRebootAsync(serials, mode);
            ShowBatchResult(result);
        }

        private void ShowBatchResult(BatchResult res)
        {
            string msg = $"{res.Label} operation complete.\n\nSuccess: {res.SuccessCount}\nFailed: {res.FailCount}";
            if (res.Errors.Any()) msg += "\n\nErrors:\n" + string.Join("\n", res.Errors.Take(5));
            
            MessageBox.Show(msg, "Fleet Operation Result", MessageBoxButtons.OK, 
                res.FailCount > 0 ? MessageBoxIcon.Warning : MessageBoxIcon.Information);
        }
    }
}
