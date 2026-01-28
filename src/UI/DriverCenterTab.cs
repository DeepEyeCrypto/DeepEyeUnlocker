using System;
using System.Drawing;
using System.Windows.Forms;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Drivers;
using DeepEyeUnlocker.Drivers.Models;
using DeepEyeUnlocker.Drivers.Detection;
using DeepEyeUnlocker.Drivers.Installer;
using DeepEyeUnlocker.UI.Themes;

namespace DeepEyeUnlocker.UI
{
    public class DriverCenterTab : TabPage
    {
        private readonly UsbDriverManager _manager;
        private DataGridView _deviceGrid = null!;
        private ListView _profileList = null!;
        private Button _btnScan = null!;
        private Button _btnInstallRecommended = null!;
        private Label _lblStatus = null!;

        public DriverCenterTab()
        {
            _manager = new UsbDriverManager(new DeviceSignatureDetector(), new PnputilInstaller());
            this.Text = "Driver Center";
            this.BackColor = BrandColors.Primary;
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            var topPanel = new Panel { Dock = DockStyle.Top, Height = 100, Padding = new Padding(10) };
            _lblStatus = new Label { Text = "No device detected", Location = new Point(20, 20), AutoSize = true, ForeColor = BrandColors.Text, Font = new Font("Segoe UI", 12, FontStyle.Bold) };
            _btnScan = new Button { Text = "Scan Drivers", Location = new Point(20, 50), Size = new Size(150, 40), BackColor = BrandColors.Accent, ForeColor = Color.White };
            _btnScan.Click += async (s, e) => await ScanDevicesAsync();
            
            _btnInstallRecommended = new Button { Text = "Install Recommended", Location = new Point(180, 50), Size = new Size(180, 40), Enabled = false };
            _btnInstallRecommended.Click += async (s, e) => await InstallRecommendedAsync();

            topPanel.Controls.AddRange(new Control[] { _lblStatus, _btnScan, _btnInstallRecommended });

            _deviceGrid = new DataGridView
            {
                Dock = DockStyle.Top,
                Height = 200,
                BackgroundColor = BrandColors.Primary,
                ForeColor = BrandColors.Text,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                AllowUserToAddRows = false,
                ReadOnly = true
            };
            _deviceGrid.Columns.Add("Desc", "Device Description");
            _deviceGrid.Columns.Add("HwId", "Hardware ID");
            _deviceGrid.Columns.Add("Status", "Status");

            _profileList = new ListView
            {
                Dock = DockStyle.Fill,
                View = View.Details,
                FullRowSelect = true,
                BackColor = BrandColors.Secondary,
                ForeColor = BrandColors.Text
            };
            _profileList.Columns.Add("Vendor", 100);
            _profileList.Columns.Add("Name", 250);
            _profileList.Columns.Add("Mode", 100);
            _profileList.Columns.Add("Version", 100);

            var lblProfiles = new Label { Text = "Available Driver Profiles:", Dock = DockStyle.Top, Height = 30, TextAlign = ContentAlignment.BottomLeft, ForeColor = Color.Gray };

            this.Controls.Add(_profileList);
            this.Controls.Add(lblProfiles);
            this.Controls.Add(_deviceGrid);
            this.Controls.Add(topPanel);

            LoadProfiles();
        }

        private void LoadProfiles()
        {
            _profileList.Items.Clear();
            foreach (var p in _manager.GetAllProfiles())
            {
                var item = new ListViewItem(new[] { p.Vendor, p.Name, p.Mode, p.Version });
                item.Tag = p;
                _profileList.Items.Add(item);
            }
        }

        private async Task ScanDevicesAsync()
        {
            _btnScan.Enabled = false;
            _deviceGrid.Rows.Clear();
            _lblStatus.Text = "Scanning USB bus...";

            try
            {
                var devices = await Task.Run(() => _manager.GetActiveDevices());
                ConnectedDevice? bestMatch = null;

                foreach (var dev in devices)
                {
                    string status = dev.IsProblemDevice ? "Driver Missing" : "OK";
                    _deviceGrid.Rows.Add(dev.Description, dev.HardwareId, status);
                    
                    if (dev.IsProblemDevice || bestMatch == null)
                        bestMatch = dev;
                }

                if (bestMatch != null)
                {
                    var rec = _manager.GetRecommendedProfile(bestMatch);
                    if (rec != null)
                    {
                        _lblStatus.Text = $"Found: {bestMatch.Description}. Recommended: {rec.Name}";
                        _btnInstallRecommended.Enabled = true;
                        _btnInstallRecommended.Tag = rec;
                    }
                    else
                    {
                        _lblStatus.Text = $"Detected: {bestMatch.Description} (No profile match)";
                        _btnInstallRecommended.Enabled = false;
                    }
                }
                else
                {
                    _lblStatus.Text = "No Android-related devices detected.";
                }
            }
            finally
            {
                _btnScan.Enabled = true;
            }
        }

        private async Task InstallRecommendedAsync()
        {
            if (_btnInstallRecommended.Tag is DriverProfile profile)
            {
                _btnInstallRecommended.Enabled = false;
                _lblStatus.Text = $"Installing {profile.Name}...";
                
                var result = await _manager.InstallProfileAsync(profile);
                if (result.Success)
                {
                    MessageBox.Show("Driver installed successfully!", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    await ScanDevicesAsync();
                }
                else
                {
                    MessageBox.Show($"Installation failed: {result.ErrorMessage}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
                _btnInstallRecommended.Enabled = true;
            }
        }
    }
}
