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

        private DataGridView _partitionGrid = null!;
        private Button _btnRestore = null!;
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
            this.BackColor = Color.FromArgb(18, 18, 18);
            this.ForeColor = Color.White;

            var mainLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                ColumnCount = 1,
                RowCount = 3,
                Padding = new Padding(10)
            };
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 40));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));

            // 1. Header
            _lblStatus = new Label { Text = "Connect a device in EDL/BROM mode to begin restoration", AutoSize = true, Font = new Font("Segoe UI", 10, FontStyle.Italic) };
            mainLayout.Controls.Add(_lblStatus, 0, 0);

            // 2. Grid
            _partitionGrid = new DataGridView
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
            _partitionGrid.Columns.Add("Name", "Partition Name");
            _partitionGrid.Columns.Add("Size", "Capacity");
            _partitionGrid.Columns.Add("Risk", "Safety Zone");
            
            mainLayout.Controls.Add(_partitionGrid, 0, 1);

            // 3. Footer
            var footerFlow = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.RightToLeft };
            _btnRestore = new Button 
            { 
                Text = "♻️ Restore Partition", 
                Width = 180, 
                Height = 40, 
                Enabled = false, 
                FlatStyle = FlatStyle.Flat,
                BackColor = Color.FromArgb(33, 150, 243)
            };
            _btnRestore.Click += async (s, e) => await StartRestoreFlowAsync();
            
            _progressBar = new ProgressBar { Width = 300, Height = 20, Visible = false };
            
            footerFlow.Controls.Add(_btnRestore);
            footerFlow.Controls.Add(_progressBar);
            mainLayout.Controls.Add(footerFlow, 0, 2);

            this.Controls.Add(mainLayout);
        }

        private async Task RefreshPartitionListAsync()
        {
            if (_engine == null) return;
            try
            {
                _lblStatus.Text = "Retrieving GPT structure...";
                var parts = await _engine.GetPartitionTableAsync();
                this.Invoke(new Action(() => 
                {
                    _partitionGrid.Rows.Clear();
                    foreach (var p in parts)
                    {
                        string risk = p.IsHighRisk ? "CALIBRATION" : (p.IsCritical ? "CRITICAL" : "STANDARD");
                        int idx = _partitionGrid.Rows.Add(p.Name, p.SizeFormatted, risk);
                        ApplyRiskColoring(_partitionGrid.Rows[idx], p);
                    }
                    _lblStatus.Text = $"{parts.Count()} partitions detected.";
                    _btnRestore.Enabled = true;
                }));
            }
            catch (Exception ex)
            {
                _lblStatus.Text = "Error: " + ex.Message;
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
                ofd.Title = $"Select image for {partName}";
                
                if (ofd.ShowDialog() == DialogResult.OK)
                {
                    if (risk == "CALIBRATION")
                    {
                        var res = MessageBox.Show($"⚠️ TOTAL DATA LOSS WARNING\n\n{partName} contains unique device identifiers (IMEI, MAC, SN).\nRestoring an incorrect image will permanently brick network functionality.\n\nAre you ABSOLUTELY sure you want to proceed?", "Sentinel Pro - High Risk", MessageBoxButtons.YesNo, MessageBoxIcon.Error);
                        if (res != DialogResult.Yes) return;
                    }

                    _btnRestore.Enabled = false;
                    _progressBar.Visible = true;
                    
                    var job = new RestoreJob { PartitionName = partName, ImagePath = ofd.FileName };
                    var progress = new Progress<ProgressUpdate>(u => this.Invoke(new Action(() => _progressBar.Value = u.Percentage)));
                    
                    bool success = await _manager.RestorePartitionAsync(job, progress, CancellationToken.None);
                    
                    _btnRestore.Enabled = true;
                    _progressBar.Visible = false;

                    if (success) MessageBox.Show("Restoration complete.", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    else MessageBox.Show("Restoration failed. Check logs.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
        }

        private string FormatSize(ulong bytes) => (bytes / 1024.0 / 1024.0).ToString("N2") + " MB";
    }
}
