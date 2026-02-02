using System;
using System.Drawing;
using System.Windows.Forms;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Infrastructure;

namespace DeepEyeUnlocker.Features.Modifications
{
    public class ExpertPanel : UserControl
    {
        private readonly IAdbClient _adb;
        private readonly PartitionRestorer _restorer;
        
        private Panel _gatePanel = null!;
        private Panel _contentPanel = null!;
        private CheckBox _chkUnderstand = null!;
        private Button _btnUnlock = null!;
        
        private Label _statusLabel = null!;
        private ProgressBar _progressBar = null!;
        private Button _btnRestore = null!;
        private Button _btnKernelCheck = null!;
        private Button _btnSpoof = null!;
        private Button _btnCalibrate = null!;
        
        private KernelBridge _kernelBridge = null!;

        public ExpertPanel(IAdbClient adb)
        {
            _adb = adb;
            _restorer = new PartitionRestorer(adb);
            _kernelBridge = new KernelBridge(adb);
            InitializeComponents();
        }

        private void InitializeComponents()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = Color.FromArgb(20, 20, 25);

            // Gate Panel
            _gatePanel = new Panel { Dock = DockStyle.Fill, BackColor = Color.FromArgb(40, 30, 30) };
            
            var lblWarning = new Label
            {
                Text = "🛑 EXPERT MODE: CRITICAL WARNING",
                Font = new Font("Segoe UI", 16, FontStyle.Bold),
                ForeColor = Color.Red,
                Location = new Point(50, 50),
                AutoSize = true
            };
            
            var lblDesc = new Label
            {
                Text = "The features in this section can PERMANENTLY BRICK your device.\n" +
                       "Write operations, calibration fixes, and low-level modifications\n" +
                       "should only be performed by experienced technicians with a valid backup.",
                Font = new Font("Segoe UI", 10),
                ForeColor = Color.White,
                Location = new Point(50, 100),
                Size = new Size(500, 80)
            };

            _chkUnderstand = new CheckBox
            {
                Text = "I acknowledge that I have a full backup and understand the risks of bricking my device.",
                ForeColor = Color.Yellow,
                Location = new Point(50, 200),
                Size = new Size(500, 30)
            };

            _btnUnlock = new Button
            {
                Text = "🔓 Enter Expert Mode",
                Location = new Point(50, 250),
                Size = new Size(200, 45),
                BackColor = Color.FromArgb(60, 60, 60),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _btnUnlock.Click += (s, e) => {
                if (_chkUnderstand.Checked)
                {
                    _gatePanel.Visible = false;
                    _contentPanel.Visible = true;
                }
                else
                {
                    MessageBox.Show("Please acknowledge the risks first.", "Safety Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            };

            _gatePanel.Controls.AddRange(new Control[] { lblWarning, lblDesc, _chkUnderstand, _btnUnlock });
            this.Controls.Add(_gatePanel);

            // Content Panel
            _contentPanel = new Panel { Dock = DockStyle.Fill, Visible = false };
            
            var lblHeader = new Label
            {
                Text = "🛠️ Expert Modification Center",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(20, 20),
                AutoSize = true
            };

            // Section: Safety & Recovery
            var lblSafety = new Label { Text = "1. Safety & Recovery", ForeColor = Color.Gray, Font = new Font("Segoe UI", 10, FontStyle.Bold), Location = new Point(20, 70), AutoSize = true };
            _btnRestore = new Button { Text = "♻️ Restore Backup (.debk)", Location = new Point(20, 95), Size = new Size(220, 40), BackColor = Color.FromArgb(0, 122, 204), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            _btnRestore.Click += OnRestoreClicked;

            // Section: Kernel Bridge (v4.0)
            var lblKernel = new Label { Text = "2. DeepEye Kernel Bridge (v4.0)", ForeColor = Color.Gray, Font = new Font("Segoe UI", 10, FontStyle.Bold), Location = new Point(20, 155), AutoSize = true };
            _btnKernelCheck = new Button { Text = "🔍 Check LKM Integrity", Location = new Point(20, 180), Size = new Size(220, 40), BackColor = Color.FromArgb(45, 45, 50), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            _btnKernelCheck.Click += OnKernelCheckClicked;

            var btnHideRoot = new Button { Text = "🛡️ Cloak Module (Stealth)", Location = new Point(250, 180), Size = new Size(220, 40), BackColor = Color.FromArgb(45, 45, 50), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            btnHideRoot.Click += async (s, e) => {
                if (await _kernelBridge.ExecuteKernelCommand(KernelCommand.HideRoot, 0))
                    MessageBox.Show("Stealth Cloaking Successful. Module is now hidden from lsmod.", "Kernel Success");
            };

            // Section: Advanced Tweaks
            var lblTweaks = new Label { Text = "3. Identity & Spoofing", ForeColor = Color.Gray, Font = new Font("Segoe UI", 10, FontStyle.Bold), Location = new Point(20, 240), AutoSize = true };
            _btnSpoof = new Button { Text = "🎭 Fingerprint Spoofer", Location = new Point(20, 265), Size = new Size(220, 40), BackColor = Color.FromArgb(45, 45, 50), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            _btnSpoof.Click += OnSpoofClicked;

            _btnCalibrate = new Button { Text = "📡 Calibration Repair (IMEI)", Location = new Point(250, 265), Size = new Size(220, 40), BackColor = Color.FromArgb(45, 45, 50), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            _btnCalibrate.Click += OnCalibrateClicked;

            _statusLabel = new Label
            {
                Text = "Status: Expert Mode Active",
                ForeColor = Color.Gray,
                Location = new Point(20, 330),
                AutoSize = true
            };

            _progressBar = new ProgressBar
            {
                Location = new Point(20, 355),
                Size = new Size(520, 15),
                Visible = false
            };

            _contentPanel.Controls.AddRange(new Control[] { 
                lblHeader, lblSafety, _btnRestore, 
                lblKernel, _btnKernelCheck, btnHideRoot,
                lblTweaks, _btnSpoof, _btnCalibrate,
                _statusLabel, _progressBar 
            });
            this.Controls.Add(_contentPanel);
        }

        private async void OnKernelCheckClicked(object? sender, EventArgs e)
        {
            _statusLabel.Text = "Running Kernel Integrity Audit...";
            bool intact = await _kernelBridge.VerifyKernelModule();
            _statusLabel.Text = intact ? "Status: Kernel Module ACTIVE & VERIFIED" : "Status: Kernel Module NOT FOUND (Upload required)";
            _statusLabel.ForeColor = intact ? Color.LimeGreen : Color.Orange;
        }

        private async void OnSpoofClicked(object? sender, EventArgs e)
        {
            var spoofer = new Features.Modifications.Magisk.FingerprintSpoofer();
            var target = Features.Modifications.Magisk.FingerprintSpoofer.CertifiedDatabase[0]; // Pixel 7 Pro
            
            _statusLabel.Text = $"Generating Magisk Spoofer for {target.Model}...";
            spoofer.ExportModuleZip(target, Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "temp_mod"));
            
            await Task.Delay(1000);
            MessageBox.Show($"Generated Magisk module for {target.Model}.\nYou can find it in the temp_mod folder.", "Spoof Module Ready");
        }

        private void OnCalibrateClicked(object? sender, EventArgs e)
        {
            MessageBox.Show("This will launch the Calibration Fixer interface.\nRequires /persist partition backup.", "Advanced Tool");
        }

        private async void OnRestoreClicked(object? sender, EventArgs e)
        {
            using (var ofd = new OpenFileDialog { Filter = "DeepEye Manifest|manifest.json" })
            {
                if (ofd.ShowDialog() == DialogResult.OK)
                {
                    var confirm = MessageBox.Show(
                        "WARNING: This will WRITTE data to your device partitions. Are you absolutely sure?",
                        "Confirm Restore",
                        MessageBoxButtons.YesNo,
                        MessageBoxIcon.Warning);

                    if (confirm != DialogResult.Yes) return;

                    _btnRestore.Enabled = false;
                    _progressBar.Visible = true;

                    try
                    {
                        var progress = new Progress<ProgressUpdate>(u => {
                            this.Invoke(new Action(() => {
                                _progressBar.Value = u.Percentage;
                                _statusLabel.Text = u.Status;
                            }));
                        });

                        await _restorer.RestoreAsync(ofd.FileName, progress, CancellationToken.None);
                        MessageBox.Show("Restore successful!", "Done", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    }
                    catch (Exception ex)
                    {
                        Logger.Error(ex, "Restore failed");
                        MessageBox.Show($"Restore Failed: {ex.Message}", "Critical Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    }
                    finally
                    {
                        _btnRestore.Enabled = true;
                        _progressBar.Visible = false;
                        _statusLabel.Text = "Standby";
                    }
                }
            }
        }

        public void SetDevice(DeviceContext? device)
        {
            // Expert mode is selective
            _btnRestore.Enabled = device != null && device.Mode == ConnectionMode.ADB;
        }
    }
}
