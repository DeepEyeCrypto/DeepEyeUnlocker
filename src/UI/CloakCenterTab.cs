using System;
using System.Drawing;
using System.Windows.Forms;
using System.Collections.Generic;
using DeepEyeUnlocker.Cloak.Root;
using DeepEyeUnlocker.Cloak.Dev;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.UI.Themes;

namespace DeepEyeUnlocker.UI
{
    public class CloakCenterTab : TabPage
    {
        private readonly IAdbClient _adb;
        private readonly RootCloakManager _rootCloak;
        private readonly DevModeCloakManager _devCloak;

        private GroupBox rootGroup = null!;
        private GroupBox devGroup = null!;
        private Label lblRootStatus = null!;
        private Label lblDevStatus = null!;
        private Button btnScan = null!;
        private Button btnOptimizeRoot = null!;
        private Button btnStealthDev = null!;
        private Button btnRestoreDev = null!;

        public CloakCenterTab(IAdbClient adb)
        {
            _adb = adb;
            _rootCloak = new RootCloakManager(adb);
            _devCloak = new DevModeCloakManager(adb);
            
            this.Text = "Cloak Center";
            this.BackColor = BrandColors.Primary;
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            rootGroup = new GroupBox { Text = "Root Cloak (Shamiko/Magisk)", ForeColor = Color.White, Location = new Point(20, 20), Size = new Size(400, 200) };
            devGroup = new GroupBox { Text = "Dev Mode Cloak", ForeColor = Color.White, Location = new Point(440, 20), Size = new Size(400, 200) };

            lblRootStatus = new Label { Text = "Status: Unknown", Location = new Point(20, 30), AutoSize = true, ForeColor = BrandColors.Text };
            btnOptimizeRoot = new Button { Text = "Optimize Root Hiding", Location = new Point(20, 140), Size = new Size(180, 40), Enabled = false };
            btnOptimizeRoot.Click += async (s, e) => await OptimizeRootAsync();

            lblDevStatus = new Label { Text = "Status: Unknown", Location = new Point(20, 30), AutoSize = true, ForeColor = BrandColors.Text };
            btnStealthDev = new Button { Text = "Enable Stealth Mode", Location = new Point(20, 140), Size = new Size(180, 40), Enabled = false };
            btnRestoreDev = new Button { Text = "Restore Normal Settings", Location = new Point(210, 140), Size = new Size(160, 40), Enabled = false };
            
            btnStealthDev.Click += async (s, e) => await ExecuteDevActionAsync(true);
            btnRestoreDev.Click += async (s, e) => await ExecuteDevActionAsync(false);

            btnScan = new Button { Text = "Scan Device Stealth State", Location = new Point(20, 250), Size = new Size(200, 45), BackColor = BrandColors.Accent, ForeColor = Color.White };
            btnScan.Click += async (s, e) => await ScanDeviceAsync();

            rootGroup.Controls.AddRange(new Control[] { lblRootStatus, btnOptimizeRoot });
            devGroup.Controls.AddRange(new Control[] { lblDevStatus, btnStealthDev, btnRestoreDev });

            this.Controls.AddRange(new Control[] { rootGroup, devGroup, btnScan });
        }

        private async Task ScanDeviceAsync()
        {
            if (!_adb.IsConnected())
            {
                MessageBox.Show("Please connect a device via ADB first.", "No Device", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            btnScan.Enabled = false;
            try
            {
                var rootStatus = await _rootCloak.InspectAsync();
                var devStatus = await _devCloak.InspectAsync();

                lblRootStatus.Text = $"Magisk: {(rootStatus.IsMagiskInstalled ? "Detected" : "None")}\n" +
                                     $"Zygisk: {(rootStatus.ZygiskActive ? "Active" : "Disabled")}\n" +
                                     $"Shamiko: {(rootStatus.ShamikoActive ? "Installed" : "Missing")}\n" +
                                     $"Integrity: {rootStatus.PlayIntegrity}";
                
                btnOptimizeRoot.Enabled = rootStatus.IsRooted;

                lblDevStatus.Text = $"Dev Options: {(devStatus.DeveloperOptionsEnabled ? "Visible" : "Hidden")}\n" +
                                    $"ADB Debugging: {(devStatus.UsbDebuggingEnabled ? "ON" : "OFF")}\n" +
                                    $"Ghost Mode: {(devStatus.IsStealth ? "ACTIVE" : "Inactive")}";

                btnStealthDev.Enabled = true;
                btnRestoreDev.Enabled = true;

                NotificationHelper.ShowNotification("Scan Complete", "Device stealth state updated.");
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Scan failed: {ex.Message}");
            }
            finally
            {
                btnScan.Enabled = true;
            }
        }

        private async Task OptimizeRootAsync()
        {
            // Placeholder for app selection - in real app we'd show a dialog
            var targets = new List<string> { "com.google.android.apps.walletnfcrel", "com.google.android.gms" };
            bool success = await _rootCloak.OptimizeForBankingAsync(targets);
            if (success)
            {
                MessageBox.Show("Root hiding optimized. Some changes may require a REBOOT.", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
        }

        private async Task ExecuteDevActionAsync(bool stealth)
        {
            if (stealth)
            {
                var res = MessageBox.Show("Enabling Stealth Mode will disable USB Debugging. This will DISCONNECT DeepEyeUnlocker. Continue?", "Warning", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (res != DialogResult.Yes) return;
                await _devCloak.ApplyStealthProfileAsync();
            }
            else
            {
                await _devCloak.RestoreOriginalDevSettingsAsync();
            }
            await ScanDeviceAsync();
        }
    }
}
