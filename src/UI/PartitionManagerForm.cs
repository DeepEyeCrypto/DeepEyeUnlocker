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

            btnRead = new Button { Text = "Read Selected", Location = new Point(20, 380), Size = new Size(150, 40) };
            btnErase = new Button { Text = "Erase Selected", Location = new Point(180, 380), Size = new Size(150, 40), ForeColor = Color.Red };

            this.Controls.AddRange(new Control[] { grid, btnRead, btnErase });
            
            this.Load += async (s, e) => await LoadPartitionsAsync();
        }

        private async Task LoadPartitionsAsync()
        {
            var partitions = await _protocol.GetPartitionTableAsync();
            foreach (var p in partitions)
            {
                grid.Rows.Add(p.Name, p.Size, p.StartAddress);
            }
        }
    }
}
