using System;
using System.Drawing;
using System.Windows.Forms;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;
using DeepEyeUnlocker.UI.Themes;
using DeepEyeUnlocker.Protocols;

namespace DeepEyeUnlocker.UI.Panels
{
    public class BootloaderUnlockPanel : UserControl
    {
        private DeviceContext? _device;
        private CancellationTokenSource? _cts;

        // UI Components
        private Panel wizardCard = null!;
        private Label lblPhaseTitle = null!;
        private Label lblPhaseDesc = null!;
        private ProgressBar prgStatus = null!;
        private Button btnStartUnlock = null!;
        private CheckBox chkAgree1 = null!;
        private CheckBox chkAgree2 = null!;
        private RichTextBox rtbLog = null!;

        public BootloaderUnlockPanel()
        {
            InitializeComponent();
        }

        public void SetDevice(DeviceContext? device)
        {
            _device = device;
            UpdateAssistantState();
        }

        private void InitializeComponent()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = BrandColors.Primary;
            this.Padding = new Padding(25);

            // 1. Wizard Card (Centered Content)
            wizardCard = new Panel
            {
                BackColor = Color.FromArgb(40, 40, 45),
                Dock = DockStyle.Fill,
                Padding = new Padding(30)
            };

            Label title = new Label
            {
                Text = "🛡️ BOOTLOADER UNLOCK ASSISTANT",
                Font = new Font("Segoe UI", 16, FontStyle.Bold),
                ForeColor = Color.White,
                AutoSize = true,
                Location = new Point(30, 30)
            };

            Label desc = new Label
            {
                Text = "This wizard will guide you through the process of unlocking your device's bootloader.\nUnlocking allows for custom kernels, root, and custom firmware.",
                Font = new Font("Segoe UI", 10),
                ForeColor = BrandColors.TextSecondary,
                Size = new Size(600, 60),
                Location = new Point(33, 70)
            };

            // Warning Box
            Panel warningBox = new Panel
            {
                BackColor = Color.FromArgb(60, 40, 40),
                Size = new Size(700, 100),
                Location = new Point(33, 140),
                Padding = new Padding(15)
            };
            Label lblWarning = new Label
            {
                Text = "⚠️ CRITICAL WARNING: This operation will TRIGGER A FACTORY RESET. All photos, videos, and personal data will be PERMANENTLY DELETED. Warranty will also be void.",
                Dock = DockStyle.Fill,
                ForeColor = Color.FromArgb(255, 120, 120),
                Font = new Font("Segoe UI Semibold", 10)
            };
            warningBox.Controls.Add(lblWarning);

            chkAgree1 = new CheckBox
            {
                Text = "I have backed up all my data.",
                Location = new Point(33, 260),
                AutoSize = true,
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 9)
            };
            chkAgree2 = new CheckBox
            {
                Text = "I understand that this may PERMANENTLY BRICK my device if interrupted.",
                Location = new Point(33, 290),
                AutoSize = true,
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 9)
            };
            chkAgree1.CheckedChanged += (s, e) => ValidateCheckboxes();
            chkAgree2.CheckedChanged += (s, e) => ValidateCheckboxes();

            btnStartUnlock = new Button
            {
                Text = "⚡ INITIATE UNLOCK PROCESS",
                Size = new Size(300, 55),
                Location = new Point(33, 340),
                BackColor = Color.FromArgb(60, 60, 65),
                ForeColor = Color.White,
                Enabled = false,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 11, FontStyle.Bold)
            };
            btnStartUnlock.Click += async (s, e) => await StartUnlockSequenceAsync();

            lblPhaseTitle = new Label
            {
                Text = "Ready to Begin",
                Location = new Point(33, 420),
                AutoSize = true,
                Font = new Font("Segoe UI", 12, FontStyle.Bold),
                ForeColor = BrandColors.Accent
            };

            lblPhaseDesc = new Label
            {
                Text = "Connect a supported device to start.",
                Location = new Point(33, 445),
                Size = new Size(600, 40),
                ForeColor = BrandColors.TextSecondary
            };

            prgStatus = new ProgressBar
            {
                Location = new Point(33, 490),
                Size = new Size(700, 12),
                Style = ProgressBarStyle.Continuous,
                BackColor = Color.FromArgb(45, 45, 50)
            };

            rtbLog = new RichTextBox
            {
                Location = new Point(33, 520),
                Size = new Size(700, 150),
                BackColor = Color.FromArgb(25, 25, 30),
                ForeColor = Color.Lime,
                BorderStyle = BorderStyle.None,
                ReadOnly = true,
                Font = new Font("Consolas", 9)
            };

            wizardCard.Controls.AddRange(new Control[] { 
                title, desc, warningBox, chkAgree1, chkAgree2, 
                btnStartUnlock, lblPhaseTitle, lblPhaseDesc, prgStatus, rtbLog 
            });

            this.Controls.Add(wizardCard);
        }

        private void ValidateCheckboxes()
        {
            if (_device == null) return;
            
            bool canStart = chkAgree1.Checked && chkAgree2.Checked;
            btnStartUnlock.Enabled = canStart;
            btnStartUnlock.BackColor = canStart ? Color.FromArgb(183, 28, 28) : Color.FromArgb(60, 60, 65);
        }

        private async Task StartUnlockSequenceAsync()
        {
            if (_device == null) return;

            var confirm = MessageBox.Show("ARE YOU SURE? This is your last chance to cancel before partition wipe begins.", 
                "FINAL CONFIRMATION", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
            
            if (confirm != DialogResult.Yes) return;

            btnStartUnlock.Enabled = false;
            chkAgree1.Enabled = false;
            chkAgree2.Enabled = false;
            _cts = new CancellationTokenSource();

            try
            {
                var op = new BootloaderOperation(null!);
                var progress = new Progress<ProgressUpdate>(p => {
                    this.Invoke(new Action(() => {
                        prgStatus.Value = p.Percentage;
                        lblPhaseTitle.Text = p.Status;
                        Log(p.Status);
                    }));
                });

                bool success = await op.ExecuteAsync(_device, progress, _cts.Token);

                if (success)
                {
                    lblPhaseTitle.Text = "✅ UNLOCK COMPLETE";
                    lblPhaseDesc.Text = "Your device will now reboot. First boot may take up to 10 minutes.";
                    MessageBox.Show("Bootloader Unlock Successful!", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
            }
            catch (Exception ex)
            {
                Log($"ERROR: {ex.Message}");
                MessageBox.Show($"Unlock failed: {ex.Message}", "Critical Failure", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                btnStartUnlock.Enabled = true;
                chkAgree1.Enabled = true;
                chkAgree2.Enabled = true;
                _cts = null;
            }
        }

        private void UpdateAssistantState()
        {
            if (_device == null)
            {
                lblPhaseTitle.Text = "Device Disconnected";
                lblPhaseDesc.Text = "Waiting for connection (ADB, Fastboot, or EDL)...";
                btnStartUnlock.Enabled = false;
            }
            else
            {
                lblPhaseTitle.Text = $"Ready to Unlock: {_device.Brand} {_device.Model}";
                lblPhaseDesc.Text = $"Detected Strategy: {GetRecommendedStrategy(_device)}";
                ValidateCheckboxes();
            }
        }

        private string GetRecommendedStrategy(DeviceContext device)
        {
            if (device.Mode == ConnectionMode.EDL) return "EDL 9008 Auth Bypass (Recommended)";
            if (device.Mode == ConnectionMode.BROM) return "BROM Exploit + Flash DevInfo";
            if (device.Mode == ConnectionMode.Fastboot) return "Fastboot OEM Unlock (Token)";
            return "Standard OEM Unlock";
        }

        private void Log(string msg)
        {
            rtbLog.AppendText($"[{DateTime.Now:HH:mm:ss}] {msg}\n");
            rtbLog.ScrollToCaret();
        }
    }
}
