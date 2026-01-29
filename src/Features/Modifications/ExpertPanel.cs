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

        public ExpertPanel(IAdbClient adb)
        {
            _adb = adb;
            _restorer = new PartitionRestorer(adb);
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

            _btnRestore = new Button
            {
                Text = "♻️ Safety Restore (from .debk)",
                Location = new Point(20, 80),
                Size = new Size(220, 50),
                BackColor = Color.FromArgb(0, 122, 204),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10, FontStyle.Bold)
            };
            _btnRestore.Click += OnRestoreClicked;

            _statusLabel = new Label
            {
                Text = "Standby",
                ForeColor = Color.Gray,
                Location = new Point(20, 140),
                AutoSize = true
            };

            _progressBar = new ProgressBar
            {
                Location = new Point(20, 165),
                Size = new Size(520, 20),
                Visible = false
            };

            _contentPanel.Controls.AddRange(new Control[] { lblHeader, _btnRestore, _statusLabel, _progressBar });
            this.Controls.Add(_contentPanel);
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
