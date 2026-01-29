using System;
using System.Drawing;
using System.Windows.Forms;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;
using DeepEyeUnlocker.UI.Themes;
using DeepEyeUnlocker.Infrastructure.Logging;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Protocols;

namespace DeepEyeUnlocker.UI.Panels
{
    public class FlashCenterPanel : UserControl
    {
        private readonly FlashManager _flashManager = new();
        private DeviceContext? _device;
        private IProtocol? _protocol;
        private FirmwareManifest? _currentManifest;
        private CancellationTokenSource? _cts;

        // UI Components
        private Panel headerCard = null!;
        private Panel actionCard = null!;
        private DataGridView partitionGrid = null!;
        private Button btnLoadFirmware = null!;
        private Button btnFlash = null!;
        private Button btnStop = null!;
        private CheckBox chkSafeMode = null!;
        private ProgressBar prgOverall = null!;
        private Label lblFirmwareInfo = null!;
        private Label lblStatus = null!;

        public FlashCenterPanel()
        {
            InitializeComponent();
        }

        public void SetDevice(DeviceContext? device, IProtocol? protocol)
        {
            _device = device;
            _protocol = protocol;
            UpdateStatus();
        }

        private void InitializeComponent()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = BrandColors.Primary;
            this.Padding = new Padding(20);

            // 1. Header Card (Load Firmware)
            headerCard = new Panel
            {
                Dock = DockStyle.Top,
                Height = 120,
                BackColor = Color.FromArgb(40, 40, 45),
                Padding = new Padding(20)
            };

            btnLoadFirmware = new Button
            {
                Text = "📁 LOAD FIRMWARE PACKAGE",
                Location = new Point(20, 20),
                Size = new Size(220, 45),
                BackColor = BrandColors.Accent,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10, FontStyle.Bold)
            };
            btnLoadFirmware.Click += (s, e) => LoadFirmware();

            lblFirmwareInfo = new Label
            {
                Text = "No firmware loaded. Select a folder containing rawprogram.xml or scatter.txt",
                Location = new Point(250, 32),
                AutoSize = true,
                ForeColor = BrandColors.TextSecondary,
                Font = new Font("Segoe UI", 9)
            };

            chkSafeMode = new CheckBox
            {
                Text = "Safeguard Critical Partitions (EFS, Persist, NVRAM)",
                Location = new Point(20, 80),
                AutoSize = true,
                Checked = true,
                ForeColor = Color.Gold,
                Font = new Font("Segoe UI", 9, FontStyle.Italic)
            };
            chkSafeMode.CheckedChanged += (s, e) => ApplySafeMode();

            headerCard.Controls.AddRange(new Control[] { btnLoadFirmware, lblFirmwareInfo, chkSafeMode });

            // 2. Action Card (Bottom)
            actionCard = new Panel
            {
                Dock = DockStyle.Bottom,
                Height = 100,
                BackColor = Color.FromArgb(40, 40, 45),
                Padding = new Padding(20)
            };

            btnFlash = new Button
            {
                Text = "⚡ START FLASH",
                Location = new Point(20, 20),
                Size = new Size(180, 50),
                BackColor = Color.FromArgb(0, 150, 136),
                ForeColor = Color.White,
                Enabled = false,
                Font = new Font("Segoe UI Semibold", 11)
            };
            btnFlash.Click += async (s, e) => await StartFlashAsync();

            btnStop = new Button
            {
                Text = "STOP",
                Location = new Point(210, 20),
                Size = new Size(100, 50),
                BackColor = Color.FromArgb(183, 28, 28),
                ForeColor = Color.White,
                Visible = false
            };
            btnStop.Click += (s, e) => _cts?.Cancel();

            prgOverall = new ProgressBar
            {
                Location = new Point(330, 35),
                Size = new Size(400, 20),
                Style = ProgressBarStyle.Continuous
            };

            lblStatus = new Label
            {
                Text = "Ready",
                Location = new Point(330, 60),
                AutoSize = true,
                ForeColor = BrandColors.TextSecondary
            };

            actionCard.Controls.AddRange(new Control[] { btnFlash, btnStop, prgOverall, lblStatus });

            // 3. Partition Grid
            partitionGrid = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.FromArgb(30, 30, 35),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                AllowUserToAddRows = false,
                RowHeadersVisible = false,
                ColumnHeadersHeight = 35,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect
            };
            partitionGrid.ColumnHeadersDefaultCellStyle.BackColor = Color.FromArgb(45, 45, 50);
            partitionGrid.ColumnHeadersDefaultCellStyle.ForeColor = Color.White;
            partitionGrid.EnableHeadersVisualStyles = false;
            partitionGrid.DefaultCellStyle.BackColor = Color.FromArgb(35, 35, 40);
            partitionGrid.DefaultCellStyle.SelectionBackColor = BrandColors.Accent;

            this.Controls.Add(partitionGrid);
            this.Controls.Add(headerCard);
            this.Controls.Add(actionCard);
        }

        private void LoadFirmware()
        {
            using var fdb = new FolderBrowserDialog();
            if (fdb.ShowDialog() == DialogResult.OK)
            {
                Task.Run(async () =>
                {
                    _currentManifest = await _flashManager.ParseFirmwareAsync(fdb.SelectedPath);
                    this.Invoke(new Action(() => PopulateGrid()));
                });
            }
        }

        private void PopulateGrid()
        {
            if (_currentManifest == null) return;

            lblFirmwareInfo.Text = $"Package: {_currentManifest.FirmwareName} ({_currentManifest.Type})";
            lblFirmwareInfo.ForeColor = Color.Cyan;

            partitionGrid.Columns.Clear();
            partitionGrid.Columns.Add(new DataGridViewCheckBoxColumn { Name = "Selected", DataPropertyName = "IsSelected", Width = 60, HeaderText = "Flash" });
            partitionGrid.Columns.Add("Name", "Partition");
            partitionGrid.Columns.Add("FileName", "File");
            partitionGrid.Columns.Add("Size", "Size");
            partitionGrid.Columns.Add("Status", "Type");

            foreach (var p in _currentManifest.Partitions)
            {
                int rowIndex = partitionGrid.Rows.Add(p.IsSelected, p.PartitionName, p.FileName, FormatSize(p.Size), p.IsCritical ? "CRITICAL" : "Normal");
                if (p.IsCritical)
                {
                    partitionGrid.Rows[rowIndex].Cells[4].Style.ForeColor = Color.Orange;
                }
            }

            ApplySafeMode();
            btnFlash.Enabled = true;
        }

        private void ApplySafeMode()
        {
            if (_currentManifest == null) return;

            bool safeguard = chkSafeMode.Checked;
            foreach (DataGridViewRow row in partitionGrid.Rows)
            {
                string type = row.Cells["Status"].Value?.ToString() ?? "";
                if (type == "CRITICAL" && safeguard)
                {
                    row.Cells["Selected"].Value = false;
                    row.DefaultCellStyle.ForeColor = Color.Gray;
                }
                else
                {
                    row.DefaultCellStyle.ForeColor = Color.White;
                }
            }
        }

        private async Task StartFlashAsync()
        {
            if (_device == null || _currentManifest == null)
            {
                MessageBox.Show("Please connect a device and load firmware first.", "Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            btnFlash.Enabled = false;
            btnStop.Visible = true;
            _cts = new CancellationTokenSource();

            try
            {
                lblStatus.Text = "Verifying Package...";
                prgOverall.Value = 0;

                // Sync UI Selection back to Manifest
                foreach (DataGridViewRow row in partitionGrid.Rows)
                {
                    var partitionName = row.Cells["Name"].Value.ToString();
                    var isSelected = (bool)row.Cells["Selected"].Value;
                    var p = _currentManifest.Partitions.FirstOrDefault(x => x.PartitionName == partitionName);
                    if (p != null) p.IsSelected = isSelected;
                }

                if (_protocol == null)
                {
                    lblStatus.Text = "Protocol Engine Not Initialized";
                    return;
                }

                var operation = new FlashOperation(_currentManifest.BaseDirectory, _protocol)
                {
                    IsSafeMode = chkSafeMode.Checked
                };

                var progress = new Progress<ProgressUpdate>(u => {
                    this.Invoke(new Action(() => {
                        prgOverall.Value = Math.Max(0, Math.Min(100, u.Percentage));
                        lblStatus.Text = u.Message;
                    }));
                });

                LogMessage("--- Starting EPIC B Secure Flash ---", Color.Lime);
                bool success = await operation.ExecuteAsync(_device, progress, _cts.Token);

                if (success)
                {
                    lblStatus.Text = "Flash Completed Successfully";
                    MessageBox.Show("Flash Successful!\n\nDevice is rebooting.", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                else
                {
                    lblStatus.Text = "Flash Failed";
                }
            }
            catch (Exception ex)
            {
                lblStatus.Text = $"Error: {ex.Message}";
            }
            finally
            {
                btnFlash.Enabled = true;
                btnStop.Visible = false;
                _cts = null;
            }
        }

        private void UpdateStatus()
        {
            if (_device == null)
            {
                lblStatus.Text = "Status: Device Disconnected";
                lblStatus.ForeColor = Color.Gray;
            }
            else
            {
                lblStatus.Text = $"Status: {_device.Brand} {_device.Model} ({_device.Mode}) Connected";
                lblStatus.ForeColor = Color.Lime;
            }
        }

        private void LogMessage(string message, Color color)
        {
            // Internal logging for progress feedback
            Logger.Info(message);
        }

        private string FormatSize(long bytes)
        {
            string[] Suffix = { "B", "KB", "MB", "GB", "TB" };
            int i;
            double dblSByte = bytes;
            for (i = 0; i < Suffix.Length && bytes >= 1024; i++, bytes /= 1024)
            {
                dblSByte = bytes / 1024.0;
            }
            return $"{dblSByte:0.##} {Suffix[i]}";
        }
    }
}
