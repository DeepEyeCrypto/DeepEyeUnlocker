using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.PartitionRestore;

namespace DeepEyeUnlocker.UI.Panels
{
    public class RestorePanel : UserControl
    {
        private IProtocolEngine? _engine;
        private RestoreManager? _manager;
        private DeviceContext? _device;

        private TableLayoutPanel _mainLayout = null!;
        private DataGridView _partitionGrid = null!;
        private RichTextBox _logBox = null!;
        private Button _btnRestore = null!;
        private Button _btnRefresh = null!;
        private Label _lblStatus = null!;
        private ProgressBar _progressBar = null!;
        private string _selectedImagePath = string.Empty;

        public RestorePanel()
        {
            InitializeComponent();
        }

        public void SetDevice(DeviceContext? device, IProtocolEngine? engine)
        {
            _device = device;
            _engine = engine;
            if (_engine != null)
            {
                _manager = new RestoreManager(_engine);
                _ = RefreshPartitionListAsync();
            }
            else
            {
                _partitionGrid.Rows.Clear();
                _btnRestore.Enabled = false;
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
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));  // Title
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 50));  // Legend/Status
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 60));   // Grid
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 40));   // Log
            _mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 50));  // Footer

            // 1. Title
            var titleLabel = new Label
            {
                Text = "⚡ Sentinel Pro: Partition Restore Center",
                Font = new Font("Segoe UI", 16, FontStyle.Bold),
                ForeColor = Color.FromArgb(0, 150, 136),
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 10)
            };

            // 2. Legend & Info
            var legendFlow = new FlowLayoutPanel { Dock = DockStyle.Fill, Margin = new Padding(0) };
            legendFlow.Controls.Add(CreateLegendItem("Safe Zone", Color.LightGreen));
            legendFlow.Controls.Add(CreateLegendItem("System", Color.Gold));
            legendFlow.Controls.Add(CreateLegendItem("Calibration (HIGH RISK)", Color.Salmon));
            
            _lblStatus = new Label { Text = "Connect device in Service Mode (EDL/BROM) to begin.", AutoSize = true, ForeColor = Color.Gray, Anchor = AnchorStyles.Left, Margin = new Padding(10, 0, 0, 0) };
            legendFlow.Controls.Add(_lblStatus);

            // 3. Grid
            _partitionGrid = new DataGridView
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
            _partitionGrid.ColumnHeadersDefaultCellStyle = new DataGridViewCellStyle { BackColor = Color.FromArgb(35, 35, 40), ForeColor = Color.Gray, Font = new Font("Segoe UI", 9, FontStyle.Bold) };
            _partitionGrid.Columns.Add("Name", "Partition Name");
            _partitionGrid.Columns.Add("Size", "Capacity");
            _partitionGrid.Columns.Add("Risk", "Stability Risk");
            _partitionGrid.Columns.Add("Target", "Restoration Target");

            // 4. Log Box
            _logBox = new RichTextBox
            {
                Dock = DockStyle.Fill,
                BackColor = Color.FromArgb(15, 15, 20),
                ForeColor = Color.FromArgb(150, 150, 160),
                BorderStyle = BorderStyle.None,
                ReadOnly = true,
                Font = new Font("Consolas", 8),
                Margin = new Padding(0, 10, 0, 0)
            };

            // 5. Footer
            var footer = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 3 };
            _btnRefresh = CreateStyledButton("🔄 Refresh GPT", Color.FromArgb(50, 50, 55));
            _btnRefresh.Click += async (s, e) => await RefreshPartitionListAsync();
            
            _progressBar = new ProgressBar { Dock = DockStyle.Fill, Visible = false, Style = ProgressBarStyle.Continuous, Height = 10, Margin = new Padding(10, 15, 10, 15) };
            
            _btnRestore = CreateStyledButton("⚡ Flash Selected", Color.FromArgb(183, 28, 28));
            _btnRestore.Enabled = false;
            _btnRestore.Click += async (s, e) => await StartRestoreFlowAsync();

            footer.Controls.Add(_btnRefresh, 0, 0);
            footer.Controls.Add(_progressBar, 1, 0);
            footer.Controls.Add(_btnRestore, 2, 0);

            _mainLayout.Controls.Add(titleLabel, 0, 0);
            _mainLayout.Controls.Add(legendFlow, 0, 1);
            _mainLayout.Controls.Add(_partitionGrid, 0, 2);
            _mainLayout.Controls.Add(_logBox, 0, 3);
            _mainLayout.Controls.Add(footer, 0, 4);

            this.Controls.Add(_mainLayout);
        }

        private Button CreateStyledButton(string text, Color backColor)
        {
            var btn = new Button
            {
                Text = text,
                Width = 160,
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

        private void Log(string message, Color? color = null)
        {
            _logBox.SelectionStart = _logBox.TextLength;
            _logBox.SelectionColor = color ?? Color.Gray;
            _logBox.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
            _logBox.ScrollToCaret();
        }

        private Control CreateLegendItem(string text, Color color)
        {
            var p = new Panel { AutoSize = true, Padding = new Padding(0, 0, 15, 0) };
            var box = new Panel { Size = new Size(12, 12), BackColor = color, Location = new Point(0, 4) };
            var lbl = new Label { Text = text, AutoSize = true, Location = new Point(16, 2), Font = new Font("Segoe UI", 8), ForeColor = Color.Gray };
            p.Controls.Add(box);
            p.Controls.Add(lbl);
            return p;
        }

        private async Task RefreshPartitionListAsync()
        {
            if (_engine == null) return;
            try
            {
                _lblStatus.Text = "Reading GUID Partition Table via native core...";
                _logBox.Clear();
                Log("Querying GPT structure...", Color.Cyan);
                
                var parts = await _engine.GetPartitionTableAsync();
                
                this.Invoke(new Action(() => 
                {
                    _partitionGrid.Rows.Clear();
                    foreach (var p in parts)
                    {
                        string risk = p.IsHighRisk ? "HIGH (Calibration)" : (p.IsCritical ? "CRITICAL (System)" : "STANDARD");
                        int idx = _partitionGrid.Rows.Add(p.Name, p.SizeFormatted, risk, "UNSET");
                        var row = _partitionGrid.Rows[idx];

                        if (p.IsHighRisk) row.DefaultCellStyle.ForeColor = Color.Salmon;
                        else if (p.IsCritical) row.DefaultCellStyle.ForeColor = Color.Gold;
                        else row.DefaultCellStyle.ForeColor = Color.LightGreen;
                    }
                    _lblStatus.Text = $"{parts.Count()} partitions identified via {_engine.Name}.";
                    _btnRestore.Enabled = true;
                    Log($"GPT Enumeration Complete. {parts.Count()} segments decoded.", Color.LightGreen);
                }));
            }
            catch (Exception ex)
            {
                _lblStatus.Text = "GPT Fault: " + ex.Message;
                Log($"ERROR: Failed to parse partition table - {ex.Message}", Color.Salmon);
            }
        }

        private void ApplyRiskColoring(DataGridViewRow row, PartitionInfo part)
        {
            if (part.IsHighRisk) row.DefaultCellStyle.ForeColor = Color.Salmon;
            else if (part.IsCritical) row.DefaultCellStyle.ForeColor = Color.Gold;
            else row.DefaultCellStyle.ForeColor = Color.LightGreen;
        }

        private async Task StartRestoreFlowAsync()
        {
            if (_partitionGrid.SelectedRows.Count == 0 || _manager == null) return;
            
            string partName = _partitionGrid.SelectedRows[0].Cells[0].Value.ToString()!;
            string risk = _partitionGrid.SelectedRows[0].Cells[2].Value.ToString()!;

            using (var ofd = new OpenFileDialog())
            {
                ofd.Filter = "Partition Image (*.img;*.bin)|*.img;*.bin|All Files (*.*)|*.*";
                ofd.Title = $"Select Image for [{partName}] Restoration";
                
                if (ofd.ShowDialog() == DialogResult.OK)
                {
                    if (risk.Contains("HIGH"))
                    {
                        var res = MessageBox.Show($"⚠️ CALIBRATION LOSS WARNING\n\n[{partName}] contains unique HWID signatures (IMEI, MAC, SN).\nFlash only a 1:1 backup from THIS specific device.\n\nProceed with FLASH?", "Sentinel Pro - Security Alert", MessageBoxButtons.YesNo, MessageBoxIcon.Error);
                        if (res != DialogResult.Yes) return;
                    }

                    _btnRestore.Enabled = false;
                    _btnRefresh.Enabled = false;
                    _progressBar.Visible = true;
                    
                    Log($"Loading restore candidate: {Path.GetFileName(ofd.FileName)}", Color.Cyan);
                    _partitionGrid.SelectedRows[0].Cells[3].Value = "WAITING...";
                    _partitionGrid.SelectedRows[0].Cells[3].Style.ForeColor = Color.Cyan;

                    var job = new RestoreJob { PartitionName = partName, ImagePath = ofd.FileName };
                    var progress = new Progress<ProgressUpdate>(u => this.Invoke(new Action(() => {
                        _progressBar.Value = u.Percentage;
                        if (!string.IsNullOrEmpty(u.Status)) Log(u.Status);
                    })));
                    
                    try
                    {
                        bool success = await _manager.RestorePartitionAsync(job, progress, CancellationToken.None);
                        
                        if (success)
                        {
                            Log($"RESTORE SUCCESS: {partName} has been synchronized.", Color.LightGreen);
                            _partitionGrid.SelectedRows[0].Cells[3].Value = "SYNCED";
                            _partitionGrid.SelectedRows[0].Cells[3].Style.ForeColor = Color.LightGreen;
                            MessageBox.Show($"Partition {partName} restored successfully.", "Restore Complete", MessageBoxButtons.OK, MessageBoxIcon.Information);
                        }
                        else
                        {
                            Log($"RESTORE FAILED: Protocol error during sync of {partName}.", Color.Salmon);
                            _partitionGrid.SelectedRows[0].Cells[3].Value = "FAULT";
                            _partitionGrid.SelectedRows[0].Cells[3].Style.ForeColor = Color.Salmon;
                            MessageBox.Show("Restoration failed. Verify protocol connection.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        }
                    }
                    catch (Exception ex)
                    {
                        Log($"FATAL: {ex.Message}", Color.Salmon);
                    }
                    finally
                    {
                        _btnRestore.Enabled = true;
                        _btnRefresh.Enabled = true;
                        _progressBar.Visible = false;
                    }
                }
            }
        }

        private string FormatSize(ulong bytes) => (bytes / 1024.0 / 1024.0).ToString("N2") + " MB";
    }
}
