using System;
using System.Drawing;
using System.Windows.Forms;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Cloud;
using DeepEyeUnlocker.UI.Themes;

namespace DeepEyeUnlocker.UI
{
    public class ResourceCenterTab : TabPage
    {
        private readonly FirmwareRepository _repo;
        private readonly DownloadManager _downloader;
        private TextBox _searchBox = null!;
        private ListView _resultsList = null!;
        private Button _btnSearch = null!;
        private ProgressBar _downloadProgress = null!;
        private Label _lblStatus = null!;

        public ResourceCenterTab()
        {
            _repo = new FirmwareRepository();
            _downloader = new DownloadManager();
            _downloader.OnProgress += (p) => _downloadProgress.Invoke((Action)(() => _downloadProgress.Value = p));
            
            this.Text = "Resource Center";
            this.BackColor = BrandColors.Primary;
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            var searchPanel = new Panel { Dock = DockStyle.Top, Height = 60, Padding = new Padding(10) };
            _searchBox = new TextBox { Width = 300, Location = new Point(20, 18), Font = new Font("Segoe UI", 10) };
            _btnSearch = new Button { Text = "Search Cloud", Location = new Point(330, 15), Size = new Size(120, 32), BackColor = BrandColors.Accent, ForeColor = Color.White };
            _btnSearch.Click += async (s, e) => await PerformSearchAsync();

            searchPanel.Controls.AddRange(new Control[] { _searchBox, _btnSearch });

            _resultsList = new ListView
            {
                Dock = DockStyle.Fill,
                View = View.Details,
                FullRowSelect = true,
                BackColor = BrandColors.Secondary,
                ForeColor = BrandColors.Text,
                Font = new Font("Segoe UI", 9)
            };
            _resultsList.Columns.Add("Model", 200);
            _resultsList.Columns.Add("Type", 100);
            _resultsList.Columns.Add("FileName", 200);
            _resultsList.Columns.Add("Size", 100);

            var contextMenu = new ContextMenuStrip();
            var downloadItem = new ToolStripMenuItem("Download to Local Repository");
            downloadItem.Click += async (s, e) => await DownloadSelectedAsync();
            contextMenu.Items.Add(downloadItem);
            _resultsList.ContextMenuStrip = contextMenu;

            var bottomPanel = new Panel { Dock = DockStyle.Bottom, Height = 60, Padding = new Padding(10) };
            _downloadProgress = new ProgressBar { Dock = DockStyle.Top, Height = 10 };
            _lblStatus = new Label { Text = "Right-click an item to download.", Dock = DockStyle.Fill, ForeColor = Color.Gray, TextAlign = ContentAlignment.MiddleCenter };
            bottomPanel.Controls.AddRange(new Control[] { _lblStatus, _downloadProgress });

            this.Controls.Add(_resultsList);
            this.Controls.Add(bottomPanel);
            this.Controls.Add(searchPanel);
        }

        private async Task PerformSearchAsync()
        {
            _btnSearch.Enabled = false;
            _resultsList.Items.Clear();
            _lblStatus.Text = "Searching DeepEye Cloud...";

            try
            {
                var results = await _repo.SearchAsync(_searchBox.Text);
                foreach (var entry in results)
                {
                    var item = new ListViewItem(new[] { entry.Model, entry.Type, entry.FileName, $"{(entry.Size / 1024.0):F1} KB" });
                    item.Tag = entry;
                    _resultsList.Items.Add(item);
                }
                _lblStatus.Text = $"Found {results.Count} results in cloud.";
            }
            finally
            {
                _btnSearch.Enabled = true;
            }
        }

        private async Task DownloadSelectedAsync()
        {
            if (_resultsList.SelectedItems.Count == 0) return;
            var entry = (FirmwareEntry)_resultsList.SelectedItems[0].Tag;

            string destDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "assets", entry.Type.ToLower() + "s");
            if (!Directory.Exists(destDir)) Directory.CreateDirectory(destDir);
            
            string destPath = Path.Combine(destDir, entry.FileName);
            
            _lblStatus.Text = $"Downloading {entry.FileName}...";
            _downloadProgress.Value = 0;

            bool success = await _downloader.DownloadFileAsync(entry.FileUrl, destPath, entry.Checksum);
            if (success)
            {
                _lblStatus.Text = "Download complete and verified.";
                MessageBox.Show($"{entry.FileName} is now available in your local repository.", "Cloud Sync Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            else
            {
                _lblStatus.Text = "Download failed.";
                MessageBox.Show("Cloud download failed. Please check your connection.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }
    }
}
