using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Features.PartitionBackup;
using DeepEyeUnlocker.Features.PartitionBackup.Engine;
using DeepEyeUnlocker.Features.PartitionBackup.Models;

namespace DeepEyeUnlocker.UI.Panels
{
    public class PartitionBackupPanel : UserControl
    {
        private IProtocolEngine? _engine;
        private IAdbClient? _adb;
        private BackupOrchestrator? _orchestrator;
        private BackupEngine? _adbBackupEngine;
        private PartitionMetadataCollector? _metadataCollector;
        private DeviceContext? _device;
        private List<PartitionInfo> _partitionInfos = new();
        
        private CheckedListBox _chkPartitions = null!;
        private Button _btnBackup = null!;
        private Button _btnSelectAll = null!;
        private ProgressBar _progressBar = null!;
        private Label _lblStatus = null!;
        private TextBox _txtOutputPath = null!;
        private Button _btnBrowse = null!;
        private CheckBox _chkEncrypt = null!;

        public PartitionBackupPanel()
        {
            InitializeComponent();
        }

        public void SetDevice(DeviceContext? device, IProtocolEngine? engine, IAdbClient? adb = null)
        {
            _device = device;
            _engine = engine;
            _adb = adb;

            if (_adb != null)
            {
                _adb.TargetSerial = device?.Serial;
                _adbBackupEngine = new BackupEngine(_adb);
                _metadataCollector = new PartitionMetadataCollector(_adb);
            }

            if (_engine != null)
            {
                _orchestrator = new BackupOrchestrator(_engine);
                _ = LoadPartitionsAsync();
            }
            else if (_adb != null && _device != null && _device.Mode == ConnectionMode.ADB)
            {
                _ = LoadPartitionsAdbAsync();
            }
            else
            {
                _chkPartitions.Items.Clear();
                _btnBackup.Enabled = false;
            }
        }

        private async Task LoadPartitionsAdbAsync()
        {
            if (_metadataCollector == null) return;
            try
            {
                _lblStatus.Text = "Scanning partitions via ADB (Root suggested)...";
                _partitionInfos = await _metadataCollector.GetPartitionsViaAdbAsync();
                
                this.Invoke(new Action(() => {
                    _chkPartitions.Items.Clear();
                    foreach (var p in _partitionInfos)
                        _chkPartitions.Items.Add(p.Name);
                    
                    _btnBackup.Enabled = _chkPartitions.Items.Count > 0;
                    _lblStatus.Text = $"ADB Discovery: {_chkPartitions.Items.Count} partitions found.";
                }));
            }
            catch (Exception ex)
            {
                _lblStatus.Text = "ADB Scan Failed: " + ex.Message;
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
                RowCount = 4,
                Padding = new Padding(10)
            };
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 50));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 100));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));

            // Header
            var headerPanel = new FlowLayoutPanel { Dock = DockStyle.Fill };
            _lblStatus = new Label { Text = "Select partitions to backup", AutoSize = true, Font = new Font("Segoe UI", 10, FontStyle.Bold) };
            headerPanel.Controls.Add(_lblStatus);

            // List
            _chkPartitions = new CheckedListBox
            {
                Dock = DockStyle.Fill,
                BackColor = Color.FromArgb(24, 24, 24),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                CheckOnClick = true
            };

            // Options
            var optionsPanel = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 3, RowCount = 2 };
            _txtOutputPath = new TextBox { Dock = DockStyle.Fill, BackColor = Color.FromArgb(32, 32, 32), ForeColor = Color.White };
            _txtOutputPath.Text = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "DeepEyeBackups");
            
            _btnBrowse = new Button { Text = "Browse...", FlatStyle = FlatStyle.Flat };
            _btnBrowse.Click += (s, e) => BrowseOutput();

            _chkEncrypt = new CheckBox { Text = "Encrypt Backup (AES-256)", AutoSize = true };
            _btnSelectAll = new Button { Text = "Select All", FlatStyle = FlatStyle.Flat };
            _btnSelectAll.Click += (s, e) => { for (int i = 0; i < _chkPartitions.Items.Count; i++) _chkPartitions.SetItemChecked(i, true); };

            optionsPanel.Controls.Add(new Label { Text = "Output Path:", AutoSize = true }, 0, 0);
            optionsPanel.Controls.Add(_txtOutputPath, 1, 0);
            optionsPanel.Controls.Add(_btnBrowse, 2, 0);
            optionsPanel.Controls.Add(_btnSelectAll, 0, 1);
            optionsPanel.Controls.Add(_chkEncrypt, 1, 1);

            // Footer
            var footerPanel = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.RightToLeft };
            _btnBackup = new Button
            {
                Text = "Start Backup",
                Width = 150,
                Height = 40,
                FlatStyle = FlatStyle.Flat,
                BackColor = Color.FromArgb(76, 175, 80),
                Enabled = false
            };
            _btnBackup.Click += async (s, e) => await StartBackupAsync();
            
            _progressBar = new ProgressBar { Width = 300, Height = 20, Visible = false };
            footerPanel.Controls.Add(_btnBackup);
            footerPanel.Controls.Add(_progressBar);

            mainLayout.Controls.Add(headerPanel, 0, 0);
            mainLayout.Controls.Add(_chkPartitions, 0, 1);
            mainLayout.Controls.Add(optionsPanel, 0, 2);
            mainLayout.Controls.Add(footerPanel, 0, 3);

            this.Controls.Add(mainLayout);
        }

        private async Task LoadPartitionsAsync()
        {
            if (_engine == null) return;

            try
            {
                _lblStatus.Text = "Reading partition table...";
                var table = await _engine.GetPartitionTableAsync();
                
                this.Invoke(new Action(() => 
                {
                    _chkPartitions.Items.Clear();
                    foreach (var part in table)
                    {
                        _chkPartitions.Items.Add(part.Name);
                    }
                    _btnBackup.Enabled = _chkPartitions.Items.Count > 0;
                    _lblStatus.Text = $"Found {_chkPartitions.Items.Count} partitions.";
                }));
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to load partitions");
                _lblStatus.Text = "Error: " + ex.Message;
            }
        }

        private void BrowseOutput()
        {
            using (var fbd = new FolderBrowserDialog())
            {
                if (fbd.ShowDialog() == DialogResult.OK)
                {
                    _txtOutputPath.Text = fbd.SelectedPath;
                }
            }
        }

        private async Task StartBackupAsync()
        {
            if (_device == null) return;

            var selected = _chkPartitions.CheckedItems.Cast<string>().ToList();
            if (selected.Count == 0)
            {
                MessageBox.Show("Please select at least one partition.", "Selection Required", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            string outDir = Path.Combine(_txtOutputPath.Text, $"{_device.Model}_{DateTime.Now:yyyyMMdd_HHmm}");

            try
            {
                _btnBackup.Enabled = false;
                _progressBar.Visible = true;
                _progressBar.Value = 0;

                var progress = new Progress<ProgressUpdate>(u => 
                {
                    this.Invoke(new Action(() => 
                    {
                        _progressBar.Value = u.Percentage;
                        _lblStatus.Text = u.Status;
                    }));
                });

                bool success = false;

                if (_engine != null && _orchestrator != null)
                {
                    var oldJob = new DeepEyeUnlocker.Core.Models.PartitionBackupJob
                    {
                        DeviceSerial = _device.Serial,
                        Partitions = selected,
                        OutputDirectory = outDir,
                        Encrypt = _chkEncrypt.Checked
                    };
                    success = await _orchestrator.RunBackupAsync(oldJob, progress, CancellationToken.None);
                }
                else if (_adb != null && _adbBackupEngine != null)
                {
                    var newJob = new BackupJob
                    {
                        DeviceSerial = _device.Serial,
                        TargetPartitions = _partitionInfos.Where(p => selected.Contains(p.Name)).ToList(),
                        DestinationPath = outDir,
                        Encrypt = _chkEncrypt.Checked,
                        Compress = true
                    };
                    await _adbBackupEngine.ExecuteAsync(newJob, progress, CancellationToken.None);
                    success = newJob.Status == DeepEyeUnlocker.Features.PartitionBackup.Models.BackupStatus.Completed;
                }

                if (success)
                {
                    MessageBox.Show($"Backup completed successfully!\nLocation: {outDir}", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    _lblStatus.Text = "Backup finished.";
                }
                else
                {
                    MessageBox.Show($"Backup failed. Check logs.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    _lblStatus.Text = "Backup failed.";
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Backup execution failed");
                MessageBox.Show($"Critical error: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                _btnBackup.Enabled = true;
                _progressBar.Visible = false;
            }
        }
    }
}
