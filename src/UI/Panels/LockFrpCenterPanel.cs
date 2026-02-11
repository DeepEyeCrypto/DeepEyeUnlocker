using System;
using System.Collections.Generic;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Operations;
using DeepEyeUnlocker.Protocols;
using DeepEyeUnlocker.Core.Services.Repositories;

namespace DeepEyeUnlocker.UI.Panels
{
    public class LockFrpCenterPanel : Panel
    {
        private LockFrpDiagnosticsManager? _diagnostics;
        private DeviceContext? _currentDevice;
        private LockFrpDiagnostics? _lastScan;
        private bool _expertModeEnabled;
        private bool _disclaimerAccepted;

        private Label _titleLabel = null!;
        private Label _deviceInfoLabel = null!;

        private GroupBox _frpGroup = null!;
        private Label _frpStatusLabel = null!;
        private Label _frpAccountLabel = null!;
        private Label _frpOemLabel = null!;

        private GroupBox _lockGroup = null!;
        private Label _lockStatusLabel = null!;
        private Label _lockSecurityLabel = null!;
        private Label _lockRecoveryLabel = null!;

        private GroupBox _actionsGroup = null!;
        private Button _scanButton = null!;
        private Button _ownerRecoveryButton = null!;
        private Button _oemSupportButton = null!;
        private Button _factoryResetButton = null!;
        private Button _bypassFrpButton = null!;
        private Button _diagramButton = null!; // Parity Stage 3

        private CheckBox _expertModeToggle = null!;
        private Panel _expertPanel = null!;
        private Button _backupFrpButton = null!;
        private Button _exportReportButton = null!;

        private RichTextBox _logBox = null!;
        private IProtocol? _currentProtocol;
        private readonly DiagramRepository _diagramRepo = new();

        public LockFrpCenterPanel()
        {
            InitializeComponents();
        }

        public void SetDevice(DeviceContext? device, IProtocol? protocol)
        {
            _currentDevice = device;
            _currentProtocol = protocol;
            _diagnostics = new LockFrpDiagnosticsManager(protocol);
            UpdateDeviceInfo();
            UpdateDiagramAvailability();
        }

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Size = new Size(520, 680);
            this.Padding = new Padding(15);

            int y = 15;
            _titleLabel = new Label { Text = "🔐 Lock & FRP Center", Font = new Font("Segoe UI", 16, FontStyle.Bold), ForeColor = Color.White, Location = new Point(15, y), AutoSize = true };
            this.Controls.Add(_titleLabel);
            y += 40;

            _deviceInfoLabel = new Label { Text = "No device connected", Font = new Font("Segoe UI", 9, FontStyle.Bold), ForeColor = Color.White, Location = new Point(15, y), AutoSize = true };
            this.Controls.Add(_deviceInfoLabel);
            y += 35;

            _frpGroup = CreateGroup("FRP Status", 15, y, 490, 90);
            this.Controls.Add(_frpGroup);
            _frpStatusLabel = CreateStatusLabel("Status: ⚪ Unknown", 15, 25, _frpGroup);
            _frpAccountLabel = CreateInfoLabel("Account: --", 15, 47, _frpGroup);
            _frpOemLabel = CreateInfoLabel("OEM Lock: --", 15, 67, _frpGroup);
            y += 105;

            _lockGroup = CreateGroup("Screen Lock Status", 15, y, 490, 90);
            this.Controls.Add(_lockGroup);
            _lockStatusLabel = CreateStatusLabel("Status: ⚪ Unknown", 15, 25, _lockGroup);
            _lockSecurityLabel = CreateInfoLabel("Security: --", 15, 47, _lockGroup);
            _lockRecoveryLabel = CreateInfoLabel("Recovery: --", 15, 67, _lockGroup);
            y += 105;

            _actionsGroup = CreateGroup("Recovery Options", 15, y, 490, 115);
            this.Controls.Add(_actionsGroup);

            _scanButton = CreateActionButton("🔍 Scan", 15, 28, 145, 35, Color.FromArgb(0, 123, 255));
            _scanButton.Click += OnScanClicked;
            _actionsGroup.Controls.Add(_scanButton);

            _factoryResetButton = CreateActionButton("🗑️ Reset", 170, 28, 145, 35, Color.FromArgb(220, 53, 69));
            _factoryResetButton.Click += OnFactoryResetClicked;
            _actionsGroup.Controls.Add(_factoryResetButton);

            _oemSupportButton = CreateActionButton("🏢 Support", 325, 28, 150, 35, Color.FromArgb(108, 117, 125));
            _oemSupportButton.Click += OnOemSupportClicked;
            _actionsGroup.Controls.Add(_oemSupportButton);

            _bypassFrpButton = CreateActionButton("⚡ Bypass FRP (Auto)", 15, 70, 220, 35, Color.FromArgb(75, 0, 130));
            _bypassFrpButton.Click += OnBypassFrpClicked;
            _actionsGroup.Controls.Add(_bypassFrpButton);

            _diagramButton = CreateActionButton("🖼️ View Diagram", 245, 70, 230, 35, Color.FromArgb(40, 40, 50));
            _diagramButton.Click += OnViewDiagramClicked;
            _diagramButton.Enabled = false;
            _actionsGroup.Controls.Add(_diagramButton);

            y += 130;
            _expertModeToggle = new CheckBox { Text = "⚙️ Expert Tools", Font = new Font("Segoe UI", 9), ForeColor = Color.FromArgb(150, 150, 160), Location = new Point(15, y), AutoSize = true };
            _expertModeToggle.CheckedChanged += OnExpertModeToggled;
            this.Controls.Add(_expertModeToggle);
            y += 28;

            _expertPanel = new Panel { Location = new Point(15, y), Size = new Size(490, 45), BackColor = Color.FromArgb(40, 40, 50), Visible = false };
            this.Controls.Add(_expertPanel);
            _backupFrpButton = CreateActionButton("💾 Backup FRP", 10, 8, 180, 30, Color.FromArgb(108, 117, 125));
            _backupFrpButton.Click += OnBackupFrpClicked;
            _expertPanel.Controls.Add(_backupFrpButton);

            y += 55;
            _logBox = new RichTextBox { Location = new Point(15, y), Size = new Size(490, 150), BackColor = Color.FromArgb(20, 20, 25), ForeColor = Color.FromArgb(180, 180, 180), Font = new Font("Consolas", 8.5f), BorderStyle = BorderStyle.None, ReadOnly = true };
            this.Controls.Add(_logBox);
        }

        private void UpdateDiagramAvailability()
        {
            if (_currentDevice == null) { _diagramButton.Enabled = false; return; }
            // Mock: Realme devices often need TP diagram in Sentinel Pro
            _diagramButton.Enabled = _currentDevice.Brand.Equals("Realme", StringComparison.OrdinalIgnoreCase);
        }

        private void OnViewDiagramClicked(object? sender, EventArgs e)
        {
            string diagId = "realme_c75_tp_v1";
            var meta = _diagramRepo.GetDiagram(diagId);
            if (meta != null)
            {
                var viewer = new DiagramViewerForm(meta, _diagramRepo.GetFullImagePath(diagId));
                viewer.ShowDialog(this);
            }
            else LogMessage("No diagram found for this model.");
        }

        // Helper methods... (Omitted implementation details for brevity, but they'd be standard)
        private GroupBox CreateGroup(string title, int x, int y, int w, int h) => new GroupBox { Text = title, Font = new Font("Segoe UI", 9, FontStyle.Bold), ForeColor = Color.FromArgb(150, 150, 160), Location = new Point(x, y), Size = new Size(w, h) };
        private Label CreateStatusLabel(string text, int x, int y, Control p) { var l = new Label { Text = text, Font = new Font("Segoe UI", 10, FontStyle.Bold), ForeColor = Color.White, Location = new Point(x, y), AutoSize = true }; p.Controls.Add(l); return l; }
        private Label CreateInfoLabel(string text, int x, int y, Control p) { var l = new Label { Text = text, Font = new Font("Segoe UI", 9), ForeColor = Color.FromArgb(180, 180, 180), Location = new Point(x, y), AutoSize = true }; p.Controls.Add(l); return l; }
        private Button CreateActionButton(string text, int x, int y, int w, int h, Color c) { var b = new Button { Text = text, Font = new Font("Segoe UI", 9), Location = new Point(x, y), Size = new Size(w, h), BackColor = c, ForeColor = Color.White, FlatStyle = FlatStyle.Flat }; b.FlatAppearance.BorderSize = 0; return b; }
        
        private void UpdateDeviceInfo() { if (_currentDevice != null) _deviceInfoLabel.Text = $"📱 {_currentDevice.Brand} {_currentDevice.Model} ({_currentDevice.Mode})"; }
        private void LogMessage(string msg, Color? c = null) { _logBox.AppendText($"[{DateTime.Now:HH:mm:ss}] {msg}\n"); }
        
        // Mocking missing methods for compilation
        private void OnScanClicked(object? sender, EventArgs e) => LogMessage("Scanning...");
        private void OnFactoryResetClicked(object? sender, EventArgs e) => LogMessage("Reset requested.");
        private void OnOemSupportClicked(object? sender, EventArgs e) => LogMessage("Support opened.");
        private void OnBypassFrpClicked(object? sender, EventArgs e) => LogMessage("Bypass initiated.");
        private void OnBackupFrpClicked(object? sender, EventArgs e) => LogMessage("Backup started.");
        private void OnExpertModeToggled(object? sender, EventArgs e) { _expertPanel.Visible = _expertModeToggle.Checked; }
    }
}
