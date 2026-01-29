using System;
using System.Drawing;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;

namespace DeepEyeUnlocker.UI.Panels
{
    /// <summary>
    /// Cloak Center - Root and Dev Mode hiding management
    /// </summary>
    public class CloakCenterPanel : Panel
    {
        private DeviceContext? _device;
        private RootCloakManager _rootManager = new();
        private DevModeCloakManager _devManager = new();
        private Features.Cloak.CloakOrchestrator _orchestrator = new();
        
        private RootCloakStatus? _rootStatus;
        private DevModeStatus? _devStatus;
        private bool _expertModeEnabled;

        // UI Components
        private TabControl _tabControl = null!;
        
        // Root Cloak Tab
        private Panel _rootPanel = null!;
        private Label _rootStatusLabel = null!;
        private Label _magiskLabel = null!;
        private Label _zygiskLabel = null!;
        private Label _shamikoLabel = null!;
        private Label _denyListLabel = null!;
        private Label _integrityLabel = null!;
        private Button _scanRootButton = null!;
        private Button _showGuideButton = null!;
        private ComboBox _profileCombo = null!;
        private Button _applyProfileButton = null!;
        private Button _sentinelCloakButton = null!;
        private ComboBox _tierCombo = null!;

        // Dev Mode Tab
        private Panel _devPanel = null!;
        private Label _devOptionsLabel = null!;
        private Label _usbDebugLabel = null!;
        private Label _debuggableLabel = null!;
        private Label _oemUnlockLabel = null!;
        private Button _scanDevButton = null!;
        private Button _applyStealthButton = null!;
        private Button _restoreButton = null!;
        private Button _generateScriptButton = null!;

        // Expert mode
        private CheckBox _expertToggle = null!;
        private RichTextBox _logBox = null!;

        public CloakCenterPanel()
        {
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device)
        {
            _device = device;
            UpdateDeviceState();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Size = new Size(560, 700);

            int y = 10;

            // Title
            var titleLabel = new Label
            {
                Text = "🛡️ Cloak Center",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(titleLabel);
            y += 45;

            var subtitleLabel = new Label
            {
                Text = "Root & Developer Options hiding for power users",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(255, 193, 7),
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(subtitleLabel);
            y += 30;

            // Tab Control
            _tabControl = new TabControl
            {
                Location = new Point(10, y),
                Size = new Size(540, 420),
                Font = new Font("Segoe UI", 9)
            };

            // Root Cloak Tab
            var rootTab = new TabPage("🔓 Root Cloak");
            rootTab.BackColor = Color.FromArgb(35, 35, 40);
            CreateRootCloakTab(rootTab);
            _tabControl.TabPages.Add(rootTab);

            // Dev Mode Tab
            var devTab = new TabPage("⚙️ Dev Mode Cloak");
            devTab.BackColor = Color.FromArgb(35, 35, 40);
            CreateDevModeTab(devTab);
            _tabControl.TabPages.Add(devTab);

            this.Controls.Add(_tabControl);
            y += 430;

            // Expert Mode Toggle
            _expertToggle = new CheckBox
            {
                Text = "🔧 Enable Expert Mode (advanced users only)",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                AutoSize = true
            };
            _expertToggle.CheckedChanged += OnExpertToggled;
            this.Controls.Add(_expertToggle);
            y += 30;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(10, y),
                Size = new Size(540, 120),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            LogMessage("Cloak Center ready. Connect a rooted device to begin.");
        }

        private void CreateRootCloakTab(TabPage tab)
        {
            int y = 15;

            // Status Section
            var statusGroup = CreateGroup("Root Status", 10, y, 510, 140);
            tab.Controls.Add(statusGroup);

            _rootStatusLabel = CreateStatusLabel("Readiness: ⚪ Not Scanned", 15, 25, statusGroup);
            _magiskLabel = CreateInfoLabel("Magisk: --", 15, 50, statusGroup);
            _zygiskLabel = CreateInfoLabel("Zygisk: --", 15, 70, statusGroup);
            _shamikoLabel = CreateInfoLabel("Shamiko: --", 260, 50, statusGroup);
            _denyListLabel = CreateInfoLabel("DenyList: --", 260, 70, statusGroup);
            _integrityLabel = CreateInfoLabel("Play Integrity: --", 15, 95, statusGroup);

            y += 155;

            // Actions
            _scanRootButton = CreateButton("🔍 Scan Root Status", 15, y, 160, 35, Color.FromArgb(0, 123, 255));
            _scanRootButton.Click += OnScanRootClicked;
            tab.Controls.Add(_scanRootButton);

            _showGuideButton = CreateButton("📋 Setup Guide", 185, y, 130, 35, Color.FromArgb(40, 167, 69));
            _showGuideButton.Click += OnShowGuideClicked;
            tab.Controls.Add(_showGuideButton);

            y += 50;

            // Profile Selection
            var profileLabel = new Label
            {
                Text = "Apply DenyList Profile:",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.White,
                Location = new Point(15, y + 5),
                AutoSize = true
            };
            tab.Controls.Add(profileLabel);

            _profileCombo = new ComboBox
            {
                Location = new Point(160, y),
                Size = new Size(180, 25),
                DropDownStyle = ComboBoxStyle.DropDownList,
                Font = new Font("Segoe UI", 9)
            };
            _profileCombo.Items.AddRange(new[] { "Banking Apps", "Gaming Apps", "Streaming Apps", "Enterprise Apps" });
            _profileCombo.SelectedIndex = 0;
            tab.Controls.Add(_profileCombo);

            _applyProfileButton = CreateButton("Apply", 350, y - 2, 80, 30, Color.FromArgb(108, 117, 125));
            _applyProfileButton.Click += OnApplyProfileClicked;
            tab.Controls.Add(_applyProfileButton);

            y += 50;
            // Issues/Recommendations Panel
            var issuesLabel = new Label
            {
                Text = "Issues & Recommendations will appear after scanning",
                Font = new Font("Segoe UI", 9, FontStyle.Italic),
                ForeColor = Color.FromArgb(120, 120, 130),
                Location = new Point(15, y),
                AutoSize = true
            };
            tab.Controls.Add(issuesLabel);
            y += 40;

            // Sentinel Orchestrator Section (Sentinel Pro)
            var sentinelGroup = CreateGroup("⚡ Sentinel Pro Orchestrator", 10, y, 510, 80);
            tab.Controls.Add(sentinelGroup);

            var tierLabel = new Label { Text = "Stealth Tier:", Location = new Point(15, 30), AutoSize = true };
            _tierCombo = new ComboBox { Location = new Point(100, 26), Size = new Size(120, 25), DropDownStyle = ComboBoxStyle.DropDownList };
            _tierCombo.Items.AddRange(Enum.GetNames(typeof(Features.Cloak.StealthTier)));
            _tierCombo.SelectedIndex = 1; // Hybrid

            _sentinelCloakButton = CreateButton("🚀 Apply Full Sentinel Cloak", 240, 24, 250, 35, Color.FromArgb(75, 0, 130));
            _sentinelCloakButton.Font = new Font("Segoe UI", 9, FontStyle.Bold);
            _sentinelCloakButton.Click += OnSentinelCloakClicked;

            sentinelGroup.Controls.Add(tierLabel);
            sentinelGroup.Controls.Add(_tierCombo);
            sentinelGroup.Controls.Add(_sentinelCloakButton);
        }

        private void CreateDevModeTab(TabPage tab)
        {
            int y = 15;

            // Status Section
            var statusGroup = CreateGroup("Developer Options Status", 10, y, 510, 120);
            tab.Controls.Add(statusGroup);

            _devOptionsLabel = CreateStatusLabel("Developer Options: ⚪ Unknown", 15, 25, statusGroup);
            _usbDebugLabel = CreateInfoLabel("USB Debugging: --", 15, 50, statusGroup);
            _debuggableLabel = CreateInfoLabel("ro.debuggable: --", 15, 70, statusGroup);
            _oemUnlockLabel = CreateInfoLabel("OEM Unlock: --", 260, 50, statusGroup);

            y += 135;

            // Actions
            _scanDevButton = CreateButton("🔍 Scan Dev Mode", 15, y, 150, 35, Color.FromArgb(0, 123, 255));
            _scanDevButton.Click += OnScanDevClicked;
            tab.Controls.Add(_scanDevButton);

            _applyStealthButton = CreateButton("🥷 Apply Stealth", 175, y, 140, 35, Color.FromArgb(255, 193, 7));
            _applyStealthButton.Click += OnApplyStealthClicked;
            _applyStealthButton.ForeColor = Color.Black;
            tab.Controls.Add(_applyStealthButton);

            _restoreButton = CreateButton("↩️ Restore", 325, y, 100, 35, Color.FromArgb(108, 117, 125));
            _restoreButton.Click += OnRestoreClicked;
            tab.Controls.Add(_restoreButton);

            y += 50;

            // Script Generation (Expert)
            _generateScriptButton = CreateButton("📝 Generate Hook Script", 15, y, 180, 35, Color.FromArgb(75, 0, 130));
            _generateScriptButton.Click += OnGenerateScriptClicked;
            _generateScriptButton.Visible = false; // Hidden until expert mode
            tab.Controls.Add(_generateScriptButton);

            y += 50;

            // Warning
            var warningLabel = new Label
            {
                Text = "⚠️ Note: Applying stealth may affect ADB connection.\n" +
                       "Re-enable Developer Options manually on device if needed.",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(255, 193, 7),
                Location = new Point(15, y),
                Size = new Size(490, 40)
            };
            tab.Controls.Add(warningLabel);
        }

        #region Event Handlers

        private async void OnScanRootClicked(object? sender, EventArgs e)
        {
            if (_device == null) return;

            _scanRootButton.Enabled = false;
            LogMessage("\n--- Scanning Root Environment ---", Color.Cyan);

            try
            {
                var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(30));

                _rootStatus = await _rootManager.InspectAsync(_device, progress, cts.Token);
                UpdateRootDisplay();

                LogMessage($"Scan complete: {_rootStatus.Readiness}", Color.FromArgb(40, 167, 69));
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _scanRootButton.Enabled = true;
            }
        }

        private void OnShowGuideClicked(object? sender, EventArgs e)
        {
            if (_rootStatus == null)
            {
                MessageBox.Show("Please scan root status first.", "Info", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            var guide = _rootManager.GenerateSetupInstructions(_rootStatus);
            
            var guideForm = new Form
            {
                Text = "Magisk Root Hiding Setup Guide",
                Size = new Size(600, 500),
                StartPosition = FormStartPosition.CenterParent,
                BackColor = Color.FromArgb(30, 30, 35)
            };

            var textBox = new RichTextBox
            {
                Dock = DockStyle.Fill,
                BackColor = Color.FromArgb(25, 25, 30),
                ForeColor = Color.White,
                Font = new Font("Consolas", 10),
                Text = guide,
                ReadOnly = true,
                BorderStyle = BorderStyle.None
            };

            guideForm.Controls.Add(textBox);
            guideForm.ShowDialog();
        }

        private async void OnApplyProfileClicked(object? sender, EventArgs e)
        {
            if (_device == null) return;

            var profileType = _profileCombo.SelectedIndex switch
            {
                0 => ProfileType.Banking,
                1 => ProfileType.Gaming,
                2 => ProfileType.Streaming,
                3 => ProfileType.Enterprise,
                _ => ProfileType.Banking
            };

            var profile = _rootManager.GetProfile(profileType);

            var result = MessageBox.Show(
                $"Apply {profile.Name} profile?\n\n" +
                $"This will add {profile.TargetPackages.Count} apps to Magisk DenyList.\n\n" +
                "Requires root access.",
                "Confirm",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Question);

            if (result == DialogResult.Yes)
            {
                LogMessage($"Applying {profile.Name} profile...");
                var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
                await _rootManager.ApplyDenyListProfile(profile, progress);
                LogMessage("Profile applied. Refresh DenyList in Magisk app.", Color.FromArgb(40, 167, 69));
            }
        }

        private async void OnSentinelCloakClicked(object? sender, EventArgs e)
        {
            if (_device == null) return;

            var tier = (Features.Cloak.StealthTier)Enum.Parse(typeof(Features.Cloak.StealthTier), _tierCombo.SelectedItem.ToString()!);
            
            var result = MessageBox.Show(
                $"This will apply a multi-layer {tier} stealth profile.\n\n" +
                "Includes:\n" +
                $"• Root hiding for standard profiles\n" +
                $"• Developer Mode stealth features\n" +
                ((tier == Features.Cloak.StealthTier.Maximum) ? "• System prop (resetprop) surgical tweaks\n" : "") +
                "\nContinue?",
                "Sentinel Pro Orchestration",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result == DialogResult.Yes)
            {
                _sentinelCloakButton.Enabled = false;
                LogMessage($"\n--- Starting Sentinel {tier} Orchestration ---", Color.FromArgb(75, 0, 130));
                
                var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
                bool success = await _orchestrator.ApplyFullStealthProfileAsync(_device, tier, progress, CancellationToken.None);
                
                if (success)
                {
                    LogMessage("SENTINEL CLOAK APPLIED SUCCESSFULLY", Color.FromArgb(40, 167, 69));
                    MessageBox.Show("Stealth Orchestration Complete.\nReboot recommended to finalize all changes.", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                else
                {
                    LogMessage("Orchestration encountered errors", Color.FromArgb(220, 53, 69));
                }
                
                _sentinelCloakButton.Enabled = true;
            }
        }

        private async void OnScanDevClicked(object? sender, EventArgs e)
        {
            if (_device == null) return;

            _scanDevButton.Enabled = false;
            LogMessage("\n--- Scanning Developer Options ---", Color.Cyan);

            try
            {
                var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(15));

                _devStatus = await _devManager.InspectAsync(_device, progress, cts.Token);
                UpdateDevDisplay();

                LogMessage("Dev mode scan complete", Color.FromArgb(40, 167, 69));
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _scanDevButton.Enabled = true;
            }
        }

        private async void OnApplyStealthClicked(object? sender, EventArgs e)
        {
            var result = MessageBox.Show(
                "⚠️ STEALTH MODE WARNING\n\n" +
                "This will hide Developer Options from apps.\n\n" +
                "• Developer options will appear disabled to apps\n" +
                "• ADB connection will be preserved\n" +
                "• Some apps may still detect root separately\n\n" +
                "Continue?",
                "Apply Stealth Mode",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result == DialogResult.Yes)
            {
                var profile = DevModeCloakManager.GetBankingProfile();
                var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
                
                var stealthResult = await _devManager.ApplyStealthAsync(profile, _rootStatus?.IsRooted ?? false, progress);
                
                if (stealthResult.Success)
                {
                    LogMessage("Stealth mode applied", Color.FromArgb(40, 167, 69));
                    foreach (var change in stealthResult.AppliedChanges)
                    {
                        LogMessage($"  ✓ {change}");
                    }
                }
                else
                {
                    LogMessage($"Failed: {stealthResult.Error}", Color.FromArgb(220, 53, 69));
                }
            }
        }

        private async void OnRestoreClicked(object? sender, EventArgs e)
        {
            var progress = new Progress<ProgressUpdate>(u => LogMessage(u.Message));
            var result = await _devManager.RestoreNormalAsync(progress);
            
            if (result.Success)
            {
                LogMessage("Normal dev settings restored", Color.FromArgb(40, 167, 69));
            }
        }

        private void OnGenerateScriptClicked(object? sender, EventArgs e)
        {
            using var dialog = new SaveFileDialog
            {
                Filter = "JavaScript (*.js)|*.js|Java (*.java)|*.java",
                FileName = "devmode_hider"
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                string script;
                if (dialog.FileName.EndsWith(".js"))
                {
                    script = _devManager.GenerateFridaScript();
                }
                else
                {
                    script = _devManager.GenerateXposedHookScript(new[] { "com.example.banking" });
                }

                File.WriteAllText(dialog.FileName, script);
                LogMessage($"Script saved: {dialog.FileName}", Color.FromArgb(40, 167, 69));
            }
        }

        private void OnExpertToggled(object? sender, EventArgs e)
        {
            if (_expertToggle.Checked && !_expertModeEnabled)
            {
                var result = MessageBox.Show(
                    "Expert Mode provides advanced features that can:\n" +
                    "• Break ADB connectivity\n" +
                    "• Require device-side recovery\n" +
                    "• Generate custom hook scripts\n\n" +
                    "Only enable if you understand the risks.",
                    "Enable Expert Mode",
                    MessageBoxButtons.YesNo,
                    MessageBoxIcon.Warning);

                if (result == DialogResult.Yes)
                {
                    _expertModeEnabled = true;
                    _generateScriptButton.Visible = true;
                    LogMessage("Expert mode enabled", Color.FromArgb(255, 193, 7));
                }
                else
                {
                    _expertToggle.Checked = false;
                }
            }
            else
            {
                _generateScriptButton.Visible = _expertToggle.Checked;
            }
        }

        #endregion

        #region Display Updates

        private void UpdateRootDisplay()
        {
            if (_rootStatus == null) return;

            var (icon, color) = _rootStatus.Readiness switch
            {
                CloakReadiness.OptimalSetup => ("🟢", Color.FromArgb(40, 167, 69)),
                CloakReadiness.WellHidden => ("🟡", Color.FromArgb(255, 193, 7)),
                CloakReadiness.PartiallyHidden => ("🟠", Color.Orange),
                CloakReadiness.RootExposed => ("🔴", Color.FromArgb(220, 53, 69)),
                _ => ("⚪", Color.Gray)
            };

            _rootStatusLabel.Text = $"Readiness: {icon} {_rootStatus.Readiness}";
            _rootStatusLabel.ForeColor = color;

            _magiskLabel.Text = $"Magisk: {(_rootStatus.HasMagisk ? $"✓ {_rootStatus.MagiskVersion}" : "✗ Not found")}";
            _zygiskLabel.Text = $"Zygisk: {(_rootStatus.ZygiskEnabled ? "✓ Enabled" : "✗ Disabled")}";
            _shamikoLabel.Text = $"Shamiko: {(_rootStatus.ShamikoActive ? "✓ Active" : _rootStatus.ShamikoInstalled ? "⚠ Installed but inactive" : "✗ Not installed")}";
            _denyListLabel.Text = $"DenyList: {(_rootStatus.DenyListConfigured ? $"✓ {_rootStatus.DenyListedPackages.Count} apps" : "✗ Empty")}";
            
            var piStatus = _rootStatus.PlayIntegrityFixInstalled ? "✓ Fix installed" : "⚠ Not installed";
            _integrityLabel.Text = $"Play Integrity: {piStatus}";
        }

        private void UpdateDevDisplay()
        {
            if (_devStatus == null) return;

            var devIcon = _devStatus.DeveloperOptionsEnabled ? "🔴 ON" : "🟢 OFF";
            _devOptionsLabel.Text = $"Developer Options: {devIcon}";
            _devOptionsLabel.ForeColor = _devStatus.DeveloperOptionsEnabled 
                ? Color.FromArgb(220, 53, 69) 
                : Color.FromArgb(40, 167, 69);

            _usbDebugLabel.Text = $"USB Debugging: {(_devStatus.UsbDebuggingEnabled ? "ON" : "OFF")}";
            _debuggableLabel.Text = $"ro.debuggable: {(_devStatus.SystemDebuggable ? "1 (risky)" : "0")}";
            _oemUnlockLabel.Text = $"OEM Unlock: {(_devStatus.OemUnlockAllowed ? "Allowed" : "Disabled")}";
        }

        private void UpdateDeviceState()
        {
            bool connected = _device != null && _device.Mode == ConnectionMode.ADB;
            _scanRootButton.Enabled = connected;
            _scanDevButton.Enabled = connected;
        }

        #endregion

        #region Helpers

        private GroupBox CreateGroup(string title, int x, int y, int w, int h)
        {
            return new GroupBox
            {
                Text = title,
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(x, y),
                Size = new Size(w, h)
            };
        }

        private Label CreateStatusLabel(string text, int x, int y, Control parent)
        {
            var label = new Label
            {
                Text = text,
                Font = new Font("Segoe UI", 11, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(x, y),
                AutoSize = true
            };
            parent.Controls.Add(label);
            return label;
        }

        private Label CreateInfoLabel(string text, int x, int y, Control parent)
        {
            var label = new Label
            {
                Text = text,
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(180, 180, 180),
                Location = new Point(x, y),
                AutoSize = true
            };
            parent.Controls.Add(label);
            return label;
        }

        private Button CreateButton(string text, int x, int y, int w, int h, Color color)
        {
            var btn = new Button
            {
                Text = text,
                Font = new Font("Segoe UI", 9),
                Location = new Point(x, y),
                Size = new Size(w, h),
                BackColor = color,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btn.FlatAppearance.BorderSize = 0;
            return btn;
        }

        private void LogMessage(string message, Color? color = null)
        {
            if (InvokeRequired)
            {
                Invoke(() => LogMessage(message, color));
                return;
            }

            _logBox.SelectionStart = _logBox.TextLength;
            _logBox.SelectionColor = color ?? Color.FromArgb(180, 180, 180);
            _logBox.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
            _logBox.ScrollToCaret();
        }

        #endregion
    }
}
