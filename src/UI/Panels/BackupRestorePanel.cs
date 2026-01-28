using System;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;
using DeepEyeUnlocker.Protocols.Qualcomm;

namespace DeepEyeUnlocker.UI.Panels
{
    /// <summary>
    /// Backup and Restore Control Panel
    /// </summary>
    public class BackupRestorePanel : Panel
    {
        private BackupRestoreManager? _manager;
        private DeviceContext? _currentDevice;
        private PartitionTable? _partitionTable;
        
        // UI Components
        private Label _titleLabel = null!;
        private Label _deviceLabel = null!;
        private Label _partitionCountLabel = null!;
        
        private GroupBox _backupGroup = null!;
        private RadioButton _fullBackupRadio = null!;
        private RadioButton _criticalBackupRadio = null!;
        private RadioButton _customBackupRadio = null!;
        private CheckedListBox _partitionListBox = null!;
        private Button _backupButton = null!;
        
        private GroupBox _restoreGroup = null!;
        private TextBox _restorePathTextBox = null!;
        private Button _browseRestoreButton = null!;
        private Button _restoreButton = null!;
        private Label _restoreInfoLabel = null!;
        
        private ProgressBar _progressBar = null!;
        private RichTextBox _logBox = null!;

        public BackupRestorePanel()
        {
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device, FirehoseManager? firehose)
        {
            _currentDevice = device;
            if (firehose != null)
            {
                _manager = new BackupRestoreManager(firehose);
            }
            UpdateUI();
        }

        public void SetPartitionTable(PartitionTable? table)
        {
            _partitionTable = table;
            PopulatePartitions();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Padding = new Padding(15);
            this.Size = new Size(500, 600);

            int y = 15;

            // Title
            _titleLabel = new Label
            {
                Text = "💾 Backup & Restore",
                Font = new Font("Segoe UI", 14, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_titleLabel);
            y += 35;

            // Device info
            _deviceLabel = CreateValueLabel("No device connected", 15, y);
            this.Controls.Add(_deviceLabel);
            y += 22;

            _partitionCountLabel = CreateLabel("Partitions: --", 15, y);
            this.Controls.Add(_partitionCountLabel);
            y += 30;

            // Backup Group
            _backupGroup = new GroupBox
            {
                Text = "Create Backup",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                Size = new Size(470, 200)
            };
            this.Controls.Add(_backupGroup);

            int by = 25;
            _fullBackupRadio = CreateRadioButton("Full Backup (all partitions)", 15, by, true);
            _backupGroup.Controls.Add(_fullBackupRadio);
            by += 25;

            _criticalBackupRadio = CreateRadioButton("Critical Only (boot, recovery, modem...)", 15, by);
            _backupGroup.Controls.Add(_criticalBackupRadio);
            by += 25;

            _customBackupRadio = CreateRadioButton("Custom (select partitions)", 15, by);
            _customBackupRadio.CheckedChanged += (s, e) => _partitionListBox.Enabled = _customBackupRadio.Checked;
            _backupGroup.Controls.Add(_customBackupRadio);
            by += 28;

            _partitionListBox = new CheckedListBox
            {
                Location = new Point(15, by),
                Size = new Size(340, 80),
                BackColor = Color.FromArgb(40, 40, 45),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                CheckOnClick = true,
                Enabled = false
            };
            _backupGroup.Controls.Add(_partitionListBox);

            _backupButton = new Button
            {
                Text = "📦 Start Backup",
                Font = new Font("Segoe UI", 10, FontStyle.Bold),
                Location = new Point(365, by + 20),
                Size = new Size(95, 45),
                BackColor = Color.FromArgb(40, 167, 69),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _backupButton.FlatAppearance.BorderSize = 0;
            _backupButton.Click += OnBackupClicked;
            _backupGroup.Controls.Add(_backupButton);

            y += 215;

            // Restore Group
            _restoreGroup = new GroupBox
            {
                Text = "Restore Backup",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                Size = new Size(470, 100)
            };
            this.Controls.Add(_restoreGroup);

            _restorePathTextBox = new TextBox
            {
                Location = new Point(15, 30),
                Size = new Size(340, 25),
                BackColor = Color.FromArgb(40, 40, 45),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.FixedSingle,
                PlaceholderText = "Select backup file or folder..."
            };
            _restoreGroup.Controls.Add(_restorePathTextBox);

            _browseRestoreButton = new Button
            {
                Text = "📂",
                Location = new Point(360, 28),
                Size = new Size(40, 27),
                BackColor = Color.FromArgb(50, 50, 60),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _browseRestoreButton.Click += OnBrowseRestoreClicked;
            _restoreGroup.Controls.Add(_browseRestoreButton);

            _restoreButton = new Button
            {
                Text = "🔄",
                Location = new Point(410, 28),
                Size = new Size(50, 27),
                BackColor = Color.FromArgb(0, 123, 255),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _restoreButton.Click += OnRestoreClicked;
            _restoreGroup.Controls.Add(_restoreButton);

            _restoreInfoLabel = CreateLabel("", 15, 63);
            _restoreGroup.Controls.Add(_restoreInfoLabel);

            y += 115;

            // Progress Bar
            _progressBar = new ProgressBar
            {
                Location = new Point(15, y),
                Size = new Size(470, 10),
                Style = ProgressBarStyle.Continuous,
                Visible = false
            };
            this.Controls.Add(_progressBar);
            y += 20;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(15, y),
                Size = new Size(470, 130),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            LogMessage("Backup & Restore panel ready.");
        }

        private RadioButton CreateRadioButton(string text, int x, int y, bool isChecked = false)
        {
            return new RadioButton
            {
                Text = text,
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.White,
                Location = new Point(x, y),
                AutoSize = true,
                Checked = isChecked
            };
        }

        private Label CreateLabel(string text, int x, int y) => new Label
        {
            Text = text,
            Font = new Font("Segoe UI", 9),
            ForeColor = Color.FromArgb(150, 150, 160),
            Location = new Point(x, y),
            AutoSize = true
        };

        private Label CreateValueLabel(string text, int x, int y) => new Label
        {
            Text = text,
            Font = new Font("Segoe UI", 9, FontStyle.Bold),
            ForeColor = Color.White,
            Location = new Point(x, y),
            AutoSize = true
        };

        private void UpdateUI()
        {
            if (_currentDevice == null)
            {
                _deviceLabel.Text = "No device connected";
                _backupGroup.Enabled = false;
                return;
            }

            _deviceLabel.Text = $"{_currentDevice.Brand} {_currentDevice.Model} ({_currentDevice.Mode})";
            _backupGroup.Enabled = _manager != null;
        }

        private void PopulatePartitions()
        {
            _partitionListBox.Items.Clear();

            if (_partitionTable?.Partitions == null)
            {
                _partitionCountLabel.Text = "Partitions: Unable to read";
                return;
            }

            _partitionCountLabel.Text = $"Partitions: {_partitionTable.Partitions.Count}";

            foreach (var p in _partitionTable.Partitions.OrderBy(x => x.Name))
            {
                _partitionListBox.Items.Add($"{p.Name} ({p.SizeFormatted})", false);
            }
        }

        private async void OnBackupClicked(object? sender, EventArgs e)
        {
            if (_manager == null || _currentDevice == null) return;

            using var dialog = new FolderBrowserDialog
            {
                Description = "Select backup destination folder"
            };

            if (dialog.ShowDialog() != DialogResult.OK) return;

            var type = _fullBackupRadio.Checked ? BackupType.Full :
                       _criticalBackupRadio.Checked ? BackupType.Critical : BackupType.Custom;

            _backupGroup.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Value = 0;

            LogMessage($"\n--- Starting {type} Backup ---", Color.Cyan);

            try
            {
                var progress = new Progress<ProgressUpdate>(update =>
                {
                    LogMessage(update.Message);
                    _progressBar.Value = Math.Min(update.ProgressPercent, 100);
                });

                using var cts = new CancellationTokenSource(TimeSpan.FromHours(2));
                var result = await _manager.CreateBackupAsync(
                    _currentDevice,
                    dialog.SelectedPath,
                    type,
                    BackupFormat.Compressed,
                    progress,
                    cts.Token);

                if (result.Success)
                {
                    LogMessage($"✅ Backup complete: {result.PartitionsBackedUp} partitions", Color.FromArgb(40, 167, 69));
                    LogMessage($"Output: {result.OutputPath}");
                    LogMessage($"Size: {result.TotalBytes:N0} bytes, Duration: {result.Duration.TotalMinutes:F1} min");
                }
                else
                {
                    LogMessage($"❌ Backup failed: {result.Error}", Color.FromArgb(220, 53, 69));
                }
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _progressBar.Visible = false;
                _backupGroup.Enabled = true;
            }
        }

        private void OnBrowseRestoreClicked(object? sender, EventArgs e)
        {
            using var dialog = new OpenFileDialog
            {
                Title = "Select backup file",
                Filter = "Backup Files (*.zip;*.deb)|*.zip;*.deb|All Files (*.*)|*.*"
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                _restorePathTextBox.Text = dialog.FileName;
                
                // Load and show manifest info
                var manifest = _manager?.LoadManifest(dialog.FileName);
                if (manifest != null)
                {
                    _restoreInfoLabel.Text = $"{manifest.DeviceBrand} {manifest.DeviceModel} - {manifest.Partitions.Count} partitions";
                }
            }
        }

        private async void OnRestoreClicked(object? sender, EventArgs e)
        {
            if (_manager == null || string.IsNullOrEmpty(_restorePathTextBox.Text)) return;

            var result = MessageBox.Show(
                "⚠️ WARNING: Restoring will overwrite partition data.\n\n" +
                "This is a potentially DANGEROUS operation.\n" +
                "Make sure the backup matches this device model.\n\n" +
                "Continue?",
                "Confirm Restore",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result != DialogResult.Yes) return;

            _restoreGroup.Enabled = false;
            _progressBar.Visible = true;

            LogMessage("\n--- Starting Restore ---", Color.Cyan);

            try
            {
                var progress = new Progress<ProgressUpdate>(update =>
                {
                    LogMessage(update.Message);
                    _progressBar.Value = Math.Min(update.ProgressPercent, 100);
                });

                using var cts = new CancellationTokenSource(TimeSpan.FromHours(2));
                var restoreResult = await _manager.RestoreBackupAsync(
                    _restorePathTextBox.Text,
                    null, // All partitions
                    progress,
                    cts.Token);

                if (restoreResult.Success)
                {
                    LogMessage($"✅ Restore complete: {restoreResult.PartitionsRestored} partitions", Color.FromArgb(40, 167, 69));
                }
                else
                {
                    LogMessage($"❌ Restore failed: {restoreResult.Error}", Color.FromArgb(220, 53, 69));
                }
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _progressBar.Visible = false;
                _restoreGroup.Enabled = true;
            }
        }

        private void LogMessage(string message, Color? color = null)
        {
            if (InvokeRequired)
            {
                Invoke(() => LogMessage(message, color));
                return;
            }

            _logBox.SelectionStart = _logBox.TextLength;
            _logBox.SelectionColor = color ?? Color.FromArgb(180, 180, 180);
            _logBox.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
            _logBox.ScrollToCaret();
        }
    }
}
