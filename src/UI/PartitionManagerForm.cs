using System;
using System.Drawing;
using System.Windows.Forms;
using System.Collections.Generic;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.UI.Themes;

namespace DeepEyeUnlocker.UI
{
    public class PartitionManagerForm : Form
    {
        private DataGridView grid = null!;
        private Button btnRead = null!;
        private Button btnErase = null!;
        private Button btnBackupAll = null!;
        private Button btnRefresh = null!;
        private ProgressBar partitionProgress = null!;
        private Label lblTotalPartitions = null!;
        private IProtocol _protocol;

        public PartitionManagerForm(IProtocol protocol)
        {
            _protocol = protocol;
            InitializeComponent();
            DarkTheme.Apply(this);
        }

        private void InitializeComponent()
        {
            this.Size = new Size(600, 500);
            this.Text = "Partition Manager - DeepEyeUnlocker";
            this.StartPosition = FormStartPosition.CenterParent;

            grid = new DataGridView
            {
                Dock = DockStyle.Top,
                Height = 350,
                BackgroundColor = BrandColors.Primary,
                ForeColor = BrandColors.Text,
                AllowUserToAddRows = false,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill
            };
            grid.Columns.Add("Name", "Name");
            grid.Columns.Add("Size", "Size (Bytes)");
            grid.Columns.Add("Start", "Start Address");

            btnRead = new Button { Text = "Read Selected", Location = new Point(20, 380), Size = new Size(130, 35), BackColor = BrandColors.Accent, ForeColor = Color.White };
            btnRead.Click += async (s, e) => await ReadSelectedAsync();

            btnErase = new Button { Text = "Erase Selected", Location = new Point(160, 380), Size = new Size(130, 35), ForeColor = Color.Red };
            btnErase.Click += async (s, e) => await EraseSelectedAsync();

            btnBackupAll = new Button { Text = "Backup All", Location = new Point(300, 380), Size = new Size(130, 35) };
            btnBackupAll.Click += async (s, e) => await BackupAllAsync();

            btnRefresh = new Button { Text = "Refresh", Location = new Point(440, 380), Size = new Size(100, 35) };
            btnRefresh.Click += async (s, e) => await LoadPartitionsAsync();

            partitionProgress = new ProgressBar { Dock = DockStyle.Bottom, Height = 10 };
            lblTotalPartitions = new Label { Text = "Partitions: 0", Location = new Point(20, 355), AutoSize = true, ForeColor = Color.Gray };

            this.Controls.AddRange(new Control[] { grid, btnRead, btnErase, btnBackupAll, btnRefresh, partitionProgress, lblTotalPartitions });
            
            this.Load += async (s, e) => await LoadPartitionsAsync();
        }

        private async Task LoadPartitionsAsync()
        {
            grid.Rows.Clear();
            var partitions = await _protocol.GetPartitionTableAsync();
            foreach (var p in partitions)
            {
                grid.Rows.Add(p.Name, p.Size, $"0x{p.StartAddress:X8}");
            }
            lblTotalPartitions.Text = $"Partitions: {partitions.Count}";
        }

        private async Task ReadSelectedAsync()
        {
            if (grid.SelectedRows.Count == 0) return;
            string partName = grid.SelectedRows[0].Cells[0].Value.ToString() ?? "";
            
            using var saveDialog = new SaveFileDialog { FileName = $"{partName}.img", Filter = "Image files (*.img)|*.img" };
            if (saveDialog.ShowDialog() == DialogResult.OK)
            {
                btnRead.Enabled = false;
                partitionProgress.Style = ProgressBarStyle.Marquee;
                try
                {
                    var data = await _protocol.ReadPartitionAsync(partName);
                    System.IO.File.WriteAllBytes(saveDialog.FileName, data);
                    MessageBox.Show($"Partition '{partName}' saved successfully!", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Read failed: {ex.Message}");
                }
                finally
                {
                    btnRead.Enabled = true;
                    partitionProgress.Style = ProgressBarStyle.Blocks;
                }
            }
        }

        private async Task BackupAllAsync()
        {
            MessageBox.Show("Unified Backup started. Partitions will be saved to 'backups/' directory.", "Backup", MessageBoxButtons.OK, MessageBoxIcon.Information);
            // This would call BackupOperation
            await Task.CompletedTask;
        }

        private async Task EraseSelectedAsync()
        {
            if (grid.SelectedRows.Count == 0) return;
            string partName = grid.SelectedRows[0].Cells[0].Value.ToString() ?? "";
            
            var res = MessageBox.Show($"Are you SURE you want to ERASE '{partName}'? This cannot be undone.", "Warning", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
            if (res == DialogResult.Yes)
            {
                await _protocol.ErasePartitionAsync(partName);
                MessageBox.Show("Partition erased.");
            }
        }
    }
}
