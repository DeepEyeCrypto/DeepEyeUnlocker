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
        private TableLayoutPanel _mainLayout = null!;
        private DataGridView _deviceList = null!;
        private RichTextBox _fleetConsole = null!;
        private Button _btnRefresh = null!;
        private Button _btnBatchAdb = null!;
        private Button _btnBatchInstall = null!;
        private Button _btnBatchReboot = null!;
        private Label _lblFleetStats = null!;
        private ProgressBar _batchProgress = null!;

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
            this.BackColor = Color.FromArgb(20, 20, 25);
            this.ForeColor = Color.White;

            _mainLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                ColumnCount = 1,
                RowCount = 5,
                Padding = new Padding(20)
            };
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));  // Title & Stats
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 50));  // Toolbar
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 60));   // Device Grid
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 40));   // Fleet Console
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 10));  // Progress

            // 1. Header
            var header = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2 };
            var titleLabel = new Label
            {
                Text = "🚢 Sentinel Pro: Fleet HQ",
                Font = new Font("Segoe UI", 16, FontStyle.Bold),
                ForeColor = Color.FromArgb(0, 150, 136),
                AutoSize = true
            };
            _lblFleetStats = new Label { Text = "Ships in Fleet: 0", ForeColor = Color.Gray, Anchor = AnchorStyles.Right, AutoSize = true, Font = new Font("Segoe UI", 10, FontStyle.Bold) };
            header.Controls.Add(titleLabel, 0, 0);
            header.Controls.Add(_lblFleetStats, 1, 0);

            // 2. Toolbar
            var toolbar = new FlowLayoutPanel { Dock = DockStyle.Fill, Margin = new Padding(0) };
            _btnRefresh = CreateStyledButton("🔄 Refresh Fleet", Color.FromArgb(50, 50, 55));
            _btnRefresh.Click += async (s, e) => await RefreshFleetAsync();
            
            _btnBatchAdb = CreateStyledButton("⚡ Batch Shell", Color.FromArgb(0, 150, 136));
            _btnBatchAdb.Click += async (s, e) => await ShowBatchAdbDialogAsync();

            _btnBatchInstall = CreateStyledButton("📦 Batch Install", Color.FromArgb(255, 160, 0));
            _btnBatchInstall.Click += async (s, e) => await ShowBatchInstallDialogAsync();

            _btnBatchReboot = CreateStyledButton("🔄 Batch Reboot", Color.FromArgb(211, 47, 47));
            _btnBatchReboot.Click += ShowBatchRebootMenu;

            toolbar.Controls.AddRange(new Control[] { _btnRefresh, _btnBatchAdb, _btnBatchInstall, _btnBatchReboot });

            // 3. Grid
            _deviceList = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.FromArgb(25, 25, 30),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                GridColor = Color.FromArgb(40, 40, 45),
                SelectionMode = DataGridViewSelectionMode.FullRowSelect,
                AllowUserToAddRows = false,
                ReadOnly = true,
                RowHeadersVisible = false,
                EnableHeadersVisualStyles = false,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill
            };
            _deviceList.ColumnHeadersDefaultCellStyle = new DataGridViewCellStyle { BackColor = Color.FromArgb(35, 35, 40), ForeColor = Color.Gray, Font = new Font("Segoe UI", 9, FontStyle.Bold) };
            _deviceList.Columns.Add("Serial", "Vessel ID (Serial)");
            _deviceList.Columns.Add("Alias", "Bench Alias");
            _deviceList.Columns.Add("Model", "Hardware Profile");
            _deviceList.Columns.Add("Status", "Comm Status");

            // 4. Console
            _fleetConsole = new RichTextBox
            {
                Dock = DockStyle.Fill,
                BackColor = Color.FromArgb(15, 15, 20),
                ForeColor = Color.FromArgb(150, 150, 160),
                BorderStyle = BorderStyle.None,
                ReadOnly = true,
                Font = new Font("Consolas", 8),
                Margin = new Padding(0, 10, 0, 0)
            };

            // 5. Progress
            _batchProgress = new ProgressBar { Dock = DockStyle.Fill, Visible = false, Style = ProgressBarStyle.Marquee, Height = 10 };

            _mainLayout.Controls.Add(header, 0, 0);
            _mainLayout.Controls.Add(toolbar, 0, 1);
            _mainLayout.Controls.Add(_deviceList, 0, 2);
            _mainLayout.Controls.Add(_fleetConsole, 0, 3);
            _mainLayout.Controls.Add(_batchProgress, 0, 4);

            this.Controls.Add(_mainLayout);
            LogFleet("Fleet Command Center Initialized. Ready for dispatch.", Color.FromArgb(0, 150, 136));
        }

        private Button CreateStyledButton(string text, Color backColor)
        {
            var btn = new Button
            {
                Text = text,
                Width = 140,
                Height = 35,
                FlatStyle = FlatStyle.Flat,
                BackColor = backColor,
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                Cursor = Cursors.Hand
            };
            btn.FlatAppearance.BorderSize = 0;
            return btn;
        }

        private void LogFleet(string message, Color? color = null)
        {
            _fleetConsole.SelectionStart = _fleetConsole.TextLength;
            _fleetConsole.SelectionColor = color ?? Color.Gray;
            _fleetConsole.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
            _fleetConsole.ScrollToCaret();
        }
        }

        private async Task RefreshFleetAsync()
        {
            _btnRefresh.Enabled = false;
            LogFleet("Scanning USB interfaces for ADB vessels...", Color.Cyan);
            
            await _manager.RefreshDevicesAsync();
            
            _deviceList.Rows.Clear();
            var devices = _manager.GetDevices();
            foreach (var dev in devices)
            {
                int idx = _deviceList.Rows.Add(dev.Context.Serial, dev.Alias, dev.Context.Model, dev.LastStatus);
                _deviceList.Rows[idx].DefaultCellStyle.ForeColor = Color.LightGreen;
            }

            _lblFleetStats.Text = $"Ships in Fleet: {devices.Count()}";
            _btnRefresh.Enabled = true;
            LogFleet($"Reconnaissance complete. {devices.Count()} vessels online.", Color.LightGreen);
        }

        private async Task ShowBatchAdbDialogAsync()
        {
            var selectedRows = _deviceList.SelectedRows;
            if (selectedRows.Count == 0)
            {
                MessageBox.Show("Select vessels for command dispatch.", "Fleet HQ", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            string command = Microsoft.VisualBasic.Interaction.InputBox("Enter ADB Shell Command:", "Fleet Command Hub", "getprop ro.product.model");
            if (string.IsNullOrEmpty(command)) return;

            LogFleet($"Dispatching command to {selectedRows.Count} vessels: [{command}]", Color.Gold);
            _batchProgress.Visible = true;

            var serials = selectedRows.Cast<DataGridViewRow>().Select(r => r.Cells[0].Value.ToString()!).ToList();
            var result = await _manager.BatchExecuteShellAsync(serials, command);
            
            _batchProgress.Visible = false;
            ShowBatchResult(result);
            LogFleet($"Batch execution finalized. {result.SuccessCount} successful, {result.FailCount} failed.", result.FailCount > 0 ? Color.Salmon : Color.LightGreen);
        }

        private async Task ShowBatchInstallDialogAsync()
        {
            var selectedRows = _deviceList.SelectedRows;
            if (selectedRows.Count == 0) return;

            using (var ofd = new OpenFileDialog { Filter = "APK Files|*.apk" })
            {
                if (ofd.ShowDialog() == DialogResult.OK)
                {
                    LogFleet($"Dispatching APK deployment: {Path.GetFileName(ofd.FileName)} to {selectedRows.Count} vessels.", Color.Gold);
                    _batchProgress.Visible = true;

                    var serials = selectedRows.Cast<DataGridViewRow>().Select(r => r.Cells[0].Value.ToString()!).ToList();
                    var result = await _manager.BatchInstallApkAsync(serials, ofd.FileName);
                    
                    _batchProgress.Visible = false;
                    ShowBatchResult(result);
                    LogFleet($"Deployment finalized. Success: {result.SuccessCount}, Fail: {result.FailCount}", result.FailCount > 0 ? Color.Salmon : Color.LightGreen);
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

            LogFleet($"Dispatching batch REBOOT ({mode}) to {selectedRows.Count} vessels...", Color.Salmon);
            _batchProgress.Visible = true;

            var serials = selectedRows.Cast<DataGridViewRow>().Select(r => r.Cells[0].Value.ToString()!).ToList();
            var result = await _manager.BatchRebootAsync(serials, mode);
            
            _batchProgress.Visible = false;
            ShowBatchResult(result);
            LogFleet($"Batch reboot sequence complete.", Color.LightGreen);
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
