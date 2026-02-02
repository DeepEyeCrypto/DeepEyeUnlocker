using System;
using System.Drawing;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Features.ModelDiscovery.Database;
using DeepEyeUnlocker.Features.ModelDiscovery.Services;

namespace DeepEyeUnlocker.Features.ModelDiscovery.UI
{
    public class DiscoveryPanel : UserControl
    {
        private readonly CrawlerManager _crawler;
        private readonly DiscoveryService _service;
        private readonly DiscoveryDbContext _db;
        
        private ListBox _lstBrands = null!;
        private DataGridView _grid = null!;
        private Button _btnDiscover = null!;
        private Button _btnExport = null!;
        private TextBox _txtSearch = null!;
        private Label _lblStats = null!;
        private string? _selectedBrand = null;

        public DiscoveryPanel()
        {
            _db = new DiscoveryDbContext();
            _crawler = new CrawlerManager(_db);
            _service = new DiscoveryService(_db);
            
            _crawler.RegisterExtractor(new ChimeraExtractor());
            _crawler.RegisterExtractor(new UnlockToolExtractor());
            _crawler.RegisterExtractor(new HydraExtractor());
            _crawler.RegisterExtractor(new MiracleExtractor());

            InitializeComponent();
            _ = LoadBrandsAsync();
        }

        private void InitializeComponent()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = Color.FromArgb(20, 20, 25);

            var mainLayout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 1, RowCount = 3 };
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 40));

            // Header: Actions
            var header = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(10) };
            _txtSearch = new TextBox { Width = 220, PlaceholderText = "Search models..." };
            _txtSearch.TextChanged += async (s, e) => await RefreshGridAsync();

            _btnDiscover = new Button { Text = "🌐 Discover Models", Width = 150, BackColor = Color.FromArgb(40, 40, 45), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            _btnDiscover.Click += OnDiscoverClicked;

            _btnExport = new Button { Text = "📥 Export CSV", Width = 120, BackColor = Color.FromArgb(40, 40, 45), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            _btnExport.Click += OnExportClicked;

            header.Controls.Add(new Label { Text = "Global Device DB", ForeColor = Color.White, Font = new Font("Segoe UI", 12, FontStyle.Bold), AutoSize = true, Margin = new Padding(0, 5, 20, 0) });
            header.Controls.Add(_txtSearch);
            header.Controls.Add(_btnDiscover);
            header.Controls.Add(_btnExport);

            // Body: Master-Detail
            var bodySplit = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, RowCount = 1 };
            bodySplit.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 180));
            bodySplit.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));

            _lstBrands = new ListBox { Dock = DockStyle.Fill, BackColor = Color.FromArgb(25, 25, 30), ForeColor = Color.White, BorderStyle = BorderStyle.None, Font = new Font("Segoe UI", 10) };
            _lstBrands.SelectedIndexChanged += OnBrandSelected;

            _grid = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.FromArgb(30, 30, 35),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                AllowUserToAddRows = false,
                ReadOnly = true,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect,
                EnableHeadersVisualStyles = false
            };
            _grid.ColumnHeadersDefaultCellStyle.BackColor = Color.FromArgb(45, 45, 50);
            _grid.ColumnHeadersDefaultCellStyle.ForeColor = Color.White;
            _grid.Columns.Add("Tool", "Tool");
            _grid.Columns.Add("Model", "Marketing Name");
            _grid.Columns.Add("ModelNumber", "Model #");
            _grid.Columns.Add("Ops", "Operations");

            bodySplit.Controls.Add(_lstBrands, 0, 0);
            bodySplit.Controls.Add(_grid, 1, 0);

            // Footer
            _lblStats = new Label { Text = "Ready", ForeColor = Color.Gray, AutoSize = true, Padding = new Padding(10, 5, 0, 0) };

            mainLayout.Controls.Add(header, 0, 0);
            mainLayout.Controls.Add(bodySplit, 0, 1);
            mainLayout.Controls.Add(_lblStats, 0, 2);

            this.Controls.Add(mainLayout);
        }

        private async Task LoadBrandsAsync()
        {
            var brands = await _service.GetBrandsAsync();
            this.Invoke(new Action(() => {
                _lstBrands.Items.Clear();
                _lstBrands.Items.Add("[All Brands]");
                foreach (var b in brands) _lstBrands.Items.Add(b);
                _lstBrands.SelectedIndex = 0;
            }));
        }

        private async void OnBrandSelected(object? sender, EventArgs e)
        {
            _selectedBrand = _lstBrands.SelectedItem?.ToString();
            if (_selectedBrand == "[All Brands]") _selectedBrand = null;
            await RefreshGridAsync();
        }

        private async Task RefreshGridAsync()
        {
            var models = await _service.GetModelsAsync(_selectedBrand, _txtSearch.Text);
            
            _grid.Rows.Clear();
            foreach (var m in models)
            {
                _grid.Rows.Add(m.Tool, m.MarketingName, m.ModelNumber, m.OperationsJson);
            }
            _lblStats.Text = $"Browsing {_grid.Rows.Count} records " + (_selectedBrand != null ? $"for {_selectedBrand}" : "");
        }

        private async void OnDiscoverClicked(object? sender, EventArgs e)
        {
            _btnDiscover.Enabled = false;
            _lblStats.Text = "Crawling tool providers... Please wait.";
            
            await _crawler.RunDiscoveryAsync(msg => {
                this.Invoke(new Action(() => _lblStats.Text = msg));
            });

            await LoadBrandsAsync();
            _btnDiscover.Enabled = true;
            _lblStats.Text = "Discovery Complete.";
        }

        private async void OnExportClicked(object? sender, EventArgs e)
        {
            using var sfd = new SaveFileDialog { Filter = "CSV Files|*.csv", FileName = "deep_eye_supported_models.csv" };
            if (sfd.ShowDialog() == DialogResult.OK)
            {
                await _service.ExportToCsvAsync(sfd.FileName, _selectedBrand);
                MessageBox.Show($"Exported successfully to {sfd.FileName}", "Export Done");
            }
        }
    }
}
