using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.FRP;

namespace DeepEyeUnlocker.UI.Panels
{
    public class FRPPanel : UserControl
    {
        private IProtocolEngine? _engine;
        private FRPManager? _manager;
        private DeviceContext? _device;

        private ComboBox _cmbModel = null!;
        private Button _btnExecute = null!;
        private RichTextBox _logView = null!;
        private ProgressBar _progressBar = null!;
        private CheckBox _chkOwnership = null!;
        private CheckBox _chkDataBackup = null!;
        private Label _lblStatus = null!;

        public FRPPanel()
        {
            InitializeComponent();
            PopulateProfiles();
        }

        private void PopulateProfiles()
        {
            _cmbModel.Items.Clear();
            var profiles = FRPProfiles.GetStandardProfiles();
            foreach (var p in profiles)
            {
                _cmbModel.Items.Add(p.Model);
            }
            if (_cmbModel.Items.Count > 0) _cmbModel.SelectedIndex = 0;
        }

        public void SetDevice(DeviceContext? device, IProtocolEngine? engine)
        {
            _device = device;
            _engine = engine;
            if (_engine != null)
            {
                _manager = new FRPManager(_engine);
                _lblStatus.Text = $"Device Ready: {_engine.Name}";
                _btnExecute.Enabled = true;
            }
            else
            {
                _lblStatus.Text = "Waiting for EDL/BROM device...";
                _btnExecute.Enabled = false;
            }
        }

        private void InitializeComponent()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = Color.FromArgb(18, 18, 18);
            this.ForeColor = Color.White;

            var mainLayout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                ColumnCount = 1,
                RowCount = 3,
                Padding = new Padding(15)
            };
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 180)); // Config
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));  // Logs
            mainLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 60));  // Project Controls

            // 1. Config Section
            var configGroup = new GroupBox { Text = "FRP Reset Configuration", Dock = DockStyle.Fill, ForeColor = Color.Cyan, Font = new Font("Segoe UI", 9, FontStyle.Bold) };
            var configFlow = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(10) };
            
            _cmbModel = new ComboBox { Width = 250, DropDownStyle = ComboBoxStyle.DropDownList, BackColor = Color.FromArgb(30, 30, 30), ForeColor = Color.White };

            _chkOwnership = new CheckBox { Text = "I confirm I am the legal owner of this device", AutoSize = true, ForeColor = Color.Gold };
            _chkDataBackup = new CheckBox { Text = "Perform emergency partition backup before erase", AutoSize = true, Checked = true };

            configFlow.Controls.Add(new Label { Text = "Select Target Profile:", AutoSize = true });
            configFlow.Controls.Add(_cmbModel);
            configFlow.Controls.Add(_chkOwnership);
            configFlow.Controls.Add(_chkDataBackup);
            configGroup.Controls.Add(configFlow);

            // 2. Log View
            _logView = new RichTextBox
            {
                Dock = DockStyle.Fill,
                BackColor = Color.Black,
                ForeColor = Color.Lime,
                ReadOnly = true,
                Font = new Font("Consolas", 9),
                BorderStyle = BorderStyle.None
            };

            // 3. Footer
            var footerLayout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 3 };
            _lblStatus = new Label { Text = "Ready", AutoSize = true, Anchor = AnchorStyles.Left };
            _progressBar = new ProgressBar { Width = 300, Height = 25, Visible = false };
            _btnExecute = new Button 
            { 
                Text = "⚡ EXECUTE FRP RESET", 
                Width = 200, 
                Height = 40, 
                FlatStyle = FlatStyle.Flat, 
                BackColor = Color.DarkRed, 
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 10, FontStyle.Bold)
            };
            _btnExecute.Click += async (s, e) => await RunFRPResetAsync();

            footerLayout.Controls.Add(_lblStatus, 0, 0);
            footerLayout.Controls.Add(_progressBar, 1, 0);
            footerLayout.Controls.Add(_btnExecute, 2, 0);

            mainLayout.Controls.Add(configGroup, 0, 0);
            mainLayout.Controls.Add(_logView, 0, 1);
            mainLayout.Controls.Add(footerLayout, 0, 2);

            this.Controls.Add(mainLayout);
        }

        private async Task RunFRPResetAsync()
        {
            if (!_chkOwnership.Checked)
            {
                MessageBox.Show("Ownership confirmation required for security bypass.", "Security Guard", MessageBoxButtons.OK, MessageBoxIcon.Stop);
                return;
            }

            if (_manager == null || _engine == null) return;

            var selectedProfileName = _cmbModel.SelectedItem?.ToString() ?? "Generic";
            var profiles = FRPProfiles.GetStandardProfiles();
            var profile = profiles.FirstOrDefault(p => p.Model == selectedProfileName) ?? profiles[0];

            var plan = new FRPResetPlan
            {
                ModelName = profile.Model,
                TargetPartitions = profile.Partitions,
                RequiresAuthBypass = profile.Model.Contains("Xiaomi")
            };

            try
            {
                _btnExecute.Enabled = false;
                _progressBar.Visible = true;
                _logView.AppendText($"[{DateTime.Now:T}] [PLAN] Initializing FRP Bypass for {plan.ModelName}...\n");

                var progress = new Progress<ProgressUpdate>(u => 
                {
                    this.Invoke(new Action(() => 
                    {
                        _progressBar.Value = u.Percentage;
                        _lblStatus.Text = u.Status;
                        _logView.AppendText($"[{DateTime.Now:T}] [{u.Level}] {u.Status}\n");
                        _logView.SelectionStart = _logView.Text.Length;
                        _logView.ScrollToCaret();
                    }));
                });

                bool success = await _manager.ExecuteFRPResetAsync(plan, progress, CancellationToken.None);

                if (success)
                {
                    MessageBox.Show("FRP Protection cleared. You may now reboot into the setup wizard.", "Sentinel Pro - Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                else
                {
                    MessageBox.Show("FRP Bypass failed. Check logs for details.", "Sentinel Pro - Failed", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
            finally
            {
                _btnExecute.Enabled = true;
                _progressBar.Visible = false;
            }
        }
    }
}
