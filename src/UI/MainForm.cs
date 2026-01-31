using System;
using System.Drawing;
using System.Windows.Forms;
using DeepEyeUnlocker.UI.Themes;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Helpers;
using DeepEyeUnlocker.Infrastructure;
using DeepEyeUnlocker.Cloak.Root;
using DeepEyeUnlocker.Cloak.Dev;
using DeepEyeUnlocker.UI.Panels;
using DeepEyeUnlocker.Core.Engines;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Modifications;
using DeepEyeUnlocker.Features.Analytics.UI;
using DeepEyeUnlocker.Features.DeviceHealth;
using DeepEyeUnlocker.Features.PartitionBackup;

namespace DeepEyeUnlocker.UI
{
    public partial class MainForm : Form
    {
        private Panel headerPanel = null!;
        private Panel devicePanel = null!;
        private Panel operationPanel = null!;
        private Panel progressPanel = null!;
        private ListBox logConsole = null!;
        private ProgressBar progressBar = null!;
        private Label statusLabel = null!;
        private Label updateLabel = null!;
        private ComboBox deviceSelector = null!;
        private Button btnRefresh = null!;
        private DeviceManager _deviceManager;
        private System.Collections.Generic.List<DeviceContext> _usbDevices;
        private AdbClient _adbClient;

        private TabControl mainTabs = null!;
        private TabPage operationsTab = null!;
        private TabPage deviceInfoTab = null!;
        private TabPage adbToolsTab = null!;
        private TabPage lockFrpTab = null!;
        private TabPage cloakTab = null!;
        private DriverCenterTab driverTab = null!;
        private ResourceCenterTab resourceTab = null!;
        // New panels
        private DeviceInfoPanel _deviceInfoPanel = null!;
        private AdbToolsPanel _adbToolsPanel = null!;
        private LockFrpCenterPanel _lockFrpPanel = null!;
        private CloakCenterPanel _cloakPanel = null!;
        private FlashCenterPanel _flashPanel = null!;
        private BootloaderUnlockPanel _bootloaderPanel = null!;
        private DeviceHealthPanel _healthPanel = null!;
        private Features.DsuSandbox.UI.RomSandboxPanel _sandboxPanel = null!;
        // private PartitionBackupPanel _backupPanel;
        private FRPPanel _frpPanel = null!;
        private RestorePanel _restorePanel = null!;
        private DriverProPanel _driverProPanel = null!;
        private FleetPanel _fleetPanel = null!;
        private ExpertPanel _expertPanel = null!;
        private AnalyticsPanel _analyticsPanel = null!;

        private ComboBox langSelector = null!;

        private ToolTip _toolTip;

        public MainForm()
        {
            _deviceManager = new DeviceManager();
            _usbDevices = new System.Collections.Generic.List<DeviceContext>();
            _adbClient = new AdbClient();
            _toolTip = new ToolTip();
            InitializeComponent();
            DarkTheme.Apply(this);
            NotificationHelper.Initialize(this);
            RefreshDeviceList();
            _ = CheckForUpdatesAsync();
            this.FormClosing += (s, e) => NotificationHelper.Dispose();
            this.KeyPreview = true;
            this.KeyDown += MainForm_KeyDown;
        }

        private void InitializeComponent()
        {
            this.headerPanel = new Panel();
            this.devicePanel = new Panel();
            this.operationPanel = new Panel();
            this.progressPanel = new Panel();
            this.logConsole = new ListBox();
            this.progressBar = new ProgressBar();
            this.statusLabel = new Label();
            this.deviceSelector = new ComboBox();
            this.btnRefresh = new Button();
            this.langSelector = new ComboBox();
            this.mainTabs = new TabControl();
            this.operationsTab = new TabPage();
            this.deviceInfoTab = new TabPage();
            this.adbToolsTab = new TabPage();
            this.lockFrpTab = new TabPage();
            this.cloakTab = new TabPage();
            this.driverTab = new DriverCenterTab();
            this.resourceTab = new ResourceCenterTab();

            // Initialize new panels
            this._deviceInfoPanel = new DeviceInfoPanel();
            this._adbToolsPanel = new AdbToolsPanel();
            this._lockFrpPanel = new LockFrpCenterPanel();
            this._cloakPanel = new CloakCenterPanel(_adbClient);
            this._flashPanel = new FlashCenterPanel();
            this._bootloaderPanel = new BootloaderUnlockPanel();
            this._healthPanel = new DeviceHealthPanel(_adbClient);
            this._sandboxPanel = new Features.DsuSandbox.UI.RomSandboxPanel(_adbClient);
            // this._backupPanel = new PartitionBackupPanel();
            this._frpPanel = new FRPPanel();
            this._restorePanel = new RestorePanel();
            this._driverProPanel = new DriverProPanel();
            this._fleetPanel = new FleetPanel(_adbClient);
            this._expertPanel = new ExpertPanel(_adbClient);
            this._analyticsPanel = new AnalyticsPanel();

            _fleetPanel.FleetManager.SelectedDeviceChanged += (s, device) => 
            {
                UpdateDeviceOnPanels(device);
            };

            _healthPanel.ReportScanned += (report) =>
            {
                _analyticsPanel.SetDeviceHealth(report);
            };

            this.SuspendLayout();

            // Form Settings
            this.ClientSize = new System.Drawing.Size(900, 700);
            this.Text = LocalizationManager.GetString("AppTitle");
            this.StartPosition = FormStartPosition.CenterScreen;

            // Header Panel
            headerPanel.Dock = DockStyle.Top;
            headerPanel.Height = 70;
            headerPanel.BackColor = BrandColors.Secondary;
            
            Label titleLabel = new Label 
            { 
                Text = LocalizationManager.GetString("HeaderTitle"), 
                Font = new Font("Segoe UI Semibold", 18, FontStyle.Bold),
                Location = new Point(25, 20),
                AutoSize = true
            };
            
            langSelector.Location = new Point(780, 20);
            langSelector.Width = 100;
            langSelector.Items.AddRange(new string[] { "English", "Hindi" });
            langSelector.SelectedIndex = 0;
            langSelector.SelectedIndexChanged += (s, e) => {
                LocalizationManager.CurrentLanguage = langSelector.SelectedIndex == 1 ? LocalizationManager.Language.Hindi : LocalizationManager.Language.English;
                UpdateUILanguage();
            };

            headerPanel.Controls.Add(titleLabel);
            headerPanel.Controls.Add(langSelector);

            // Device Panel
            devicePanel.Dock = DockStyle.Top;
            devicePanel.Height = 80;
            devicePanel.Padding = new Padding(20);
            
            Label lblDevice = new Label { Text = "Target Device:", Location = new Point(20, 30), AutoSize = true };
            deviceSelector.Location = new Point(120, 27);
            deviceSelector.Width = 300;
            deviceSelector.DropDownStyle = ComboBoxStyle.DropDownList;
            
            btnRefresh.Text = "Refresh";
            btnRefresh.Location = new Point(430, 25);
            btnRefresh.Click += (s, e) => { RefreshDeviceList(); };

            deviceSelector.SelectedIndexChanged += (s, e) => UpdateDeviceOnPanels();

            devicePanel.Controls.AddRange(new Control[] { lblDevice, deviceSelector, btnRefresh });

            // Tab Control
            mainTabs.Dock = DockStyle.Fill;
            operationsTab.Text = "Operations";
            operationsTab.BackColor = BrandColors.Primary;
            operationsTab.Padding = new Padding(10);
            
            // Move operationPanel to operationsTab
            operationPanel.Dock = DockStyle.Fill;
            operationPanel.BackColor = BrandColors.Primary;
            AddOperationButtons();
            operationsTab.Controls.Add(operationPanel);

            // Configure new tabs
            deviceInfoTab.Text = "📱 Device Info";
            deviceInfoTab.BackColor = BrandColors.Primary;
            _deviceInfoPanel.Dock = DockStyle.Fill;
            deviceInfoTab.Controls.Add(_deviceInfoPanel);

            adbToolsTab.Text = "🔧 ADB Tools";
            adbToolsTab.BackColor = BrandColors.Primary;
            _adbToolsPanel.Dock = DockStyle.Fill;
            adbToolsTab.Controls.Add(_adbToolsPanel);

            lockFrpTab.Text = "🔐 Lock & FRP";
            lockFrpTab.BackColor = BrandColors.Primary;
            _lockFrpPanel.Dock = DockStyle.Fill;
            lockFrpTab.Controls.Add(_lockFrpPanel);

            cloakTab.Text = "🛡️ Cloak Center";
            cloakTab.BackColor = BrandColors.Primary;
            _cloakPanel.Dock = DockStyle.Fill;
            cloakTab.Controls.Add(_cloakPanel);

            TabPage flashTab = new TabPage("⚡ Flash Center") { BackColor = BrandColors.Primary };
            _flashPanel.Dock = DockStyle.Fill;
            flashTab.Controls.Add(_flashPanel);

            TabPage bootloaderTab = new TabPage("🔓 Unlock Assistant") { BackColor = BrandColors.Primary };
            _bootloaderPanel.Dock = DockStyle.Fill;
            bootloaderTab.Controls.Add(_bootloaderPanel);

            TabPage healthTab = new TabPage("📋 Report Center") { BackColor = BrandColors.Primary };
            _healthPanel.Dock = DockStyle.Fill;
            healthTab.Controls.Add(_healthPanel);

            TabPage sandboxTab = new TabPage("🧪 ROM Sandbox") { BackColor = BrandColors.Primary };
            _sandboxPanel.Dock = DockStyle.Fill;
            sandboxTab.Controls.Add(_sandboxPanel);

            /*
            TabPage backupTab = new TabPage("📦 Backup Center") { BackColor = BrandColors.Primary };
            _backupPanel.Dock = DockStyle.Fill;
            backupTab.Controls.Add(_backupPanel);
            */

            TabPage frpTab = new TabPage("🔓 FRP Engine") { BackColor = BrandColors.Primary };
            _frpPanel.Dock = DockStyle.Fill;
            frpTab.Controls.Add(_frpPanel);

            TabPage restoreTab = new TabPage("♻️ Restore Center") { BackColor = BrandColors.Primary };
            _restorePanel.Dock = DockStyle.Fill;
            restoreTab.Controls.Add(_restorePanel);

            TabPage driverProTab = new TabPage("🏎️ Driver Pro") { BackColor = BrandColors.Primary };
            _driverProPanel.Dock = DockStyle.Fill;
            driverProTab.Controls.Add(_driverProPanel);

            TabPage fleetTab = new TabPage("🚢 Fleet Manager") { BackColor = BrandColors.Primary };
            _fleetPanel.Dock = DockStyle.Fill;
            fleetTab.Controls.Add(_fleetPanel);

            TabPage expertTab = new TabPage("🛑 Expert Mode") { BackColor = BrandColors.Primary };
            _expertPanel.Dock = DockStyle.Fill;
            expertTab.Controls.Add(_expertPanel);

            TabPage analyticsTab = new TabPage("📊 Analytics") { BackColor = BrandColors.Primary };
            _analyticsPanel.Dock = DockStyle.Fill;
            analyticsTab.Controls.Add(_analyticsPanel);
            
            mainTabs.TabPages.AddRange(new TabPage[] { operationsTab, deviceInfoTab, adbToolsTab, lockFrpTab, cloakTab, flashTab, bootloaderTab, healthTab, sandboxTab, frpTab, restoreTab, driverProTab, fleetTab, expertTab, analyticsTab, driverTab, resourceTab });

            // Progress Panel
            progressPanel.Dock = DockStyle.Bottom;
            progressPanel.Height = 250;
            progressPanel.Padding = new Padding(20);

            progressBar.Dock = DockStyle.Top;
            progressBar.Height = 15;
            
            statusLabel.Text = "Ready for operation...";
            statusLabel.Location = new Point(20, 40);
            statusLabel.AutoSize = true;

            updateLabel = new Label
            {
                Text = "",
                ForeColor = BrandColors.Accent,
                Location = new Point(700, 40),
                Cursor = Cursors.Hand,
                AutoSize = true
            };
            
            // Driver Status Flow
            FlowLayoutPanel driverFlow = new FlowLayoutPanel 
            { 
                Location = new Point(450, 15), 
                Size = new Size(300, 30),
                BackColor = Color.Transparent
            };
            
            var driverList = DriverChecker.CheckDrivers();
            foreach (var d in driverList)
            {
                Label dLabel = new Label
                {
                    Text = $"● {d.Name}",
                    ForeColor = d.IsInstalled ? Color.LimeGreen : Color.Gray,
                    AutoSize = true,
                    Font = new Font("Segoe UI", 8, FontStyle.Bold)
                };
                _toolTip.SetToolTip(dLabel, d.IsInstalled ? $"{d.Name} driver is active." : $"{d.Name} driver missing.");
                driverFlow.Controls.Add(dLabel);
            }
            
            headerPanel.Controls.AddRange(new Control[] { titleLabel, langSelector, driverFlow });

            // Final Assembly
            this.Controls.Add(mainTabs);
            this.Controls.Add(devicePanel);
            this.Controls.Add(progressPanel);
            this.Controls.Add(headerPanel);

            this.ResumeLayout(false);
        }

        private void MainForm_KeyDown(object? sender, KeyEventArgs e)
        {
            if (e.Control && e.KeyCode == Keys.R)
            {
                RefreshDeviceList();
                e.Handled = true;
            }
            else if (e.KeyCode == Keys.F1)
            {
                ShowHelp();
            }
        }

        private void ShowHelp()
        {
            MessageBox.Show(
                "DeepEyeUnlocker Shortcuts:\n\n" +
                "Ctrl + R: Refresh Device List\n" +
                "F1: Show Help\n" +
                "Alt + F4: Exit",
                "Context Help",
                MessageBoxButtons.OK,
                MessageBoxIcon.Information
            );
        }

        private void AddOperationButtons()
        {
            int x = 20, y = 20;
            string[] ops = { "Format", "FRP Bypass", "Pattern Clear", "Backup", "Flash", "Bootloader", "Device Info" };
            
            foreach (var op in ops)
            {
                Button btn = new Button
                {
                    Text = op,
                    Size = new Size(150, 45),
                    Location = new Point(x, y),
                    Font = new Font("Segoe UI", 10, FontStyle.Bold)
                };
                
                _toolTip.SetToolTip(btn, $"Run {op} operation on the target device.");
                btn.Click += async (s, e) => await ExecuteOperationAsync(op);
                
                operationPanel.Controls.Add(btn);
                x += 170;
                if (x > 700) { x = 20; y += 60; }
            }
        }

        private void UpdateUILanguage()
        {
            this.Text = LocalizationManager.GetString("AppTitle");
            // Iterating and updating text would be better with a proper layout re-init
            // but for now, we re-trigger the essential labels
            statusLabel.Text = LocalizationManager.GetString("Ready");
            btnRefresh.Text = LocalizationManager.GetString("Refresh");
            
            // Re-scan or refresh list to update log entries
            logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] Language changed to: {LocalizationManager.CurrentLanguage}");
        }

        private void RefreshDeviceList()
        {
            logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] {LocalizationManager.GetString("Scanning")}");
            _usbDevices = _deviceManager.EnumerateDevices();
            deviceSelector.Items.Clear();
            
            foreach (var context in _usbDevices)
            {
                string displayMode = context.Chipset == "Unknown" ? "Unknown / MTP" : $"{context.Chipset} {context.Mode}";
                deviceSelector.Items.Add($"{context.Brand} {context.Model} [{displayMode}] ({context.Serial})");
            }

            if (deviceSelector.Items.Count > 0)
            {
                deviceSelector.SelectedIndex = 0;
                logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] Found {deviceSelector.Items.Count} device(s).");
            }
            else
            {
                logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] {LocalizationManager.GetString("NoDevice")}");
            }
        }

        private async Task ExecuteOperationAsync(string operationName)
        {
            if (deviceSelector.SelectedIndex == -1)
            {
                MessageBox.Show(LocalizationManager.GetString("NoDevice"), "Error", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            var context = _usbDevices[deviceSelector.SelectedIndex];
            
            logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] {LocalizationManager.GetString("OperationStarted")} {operationName}");
            progressBar.Value = 0;

            try 
            {
                using var usbDevice = _deviceManager.OpenDevice(context);
                if (usbDevice == null) throw new Exception("Could not open USB device.");

                var engine = OperationFactory.CreateEngine(context, usbDevice);
                if (engine == null) throw new Exception($"No engine available for {context.Chipset}");

                statusLabel.Text = $"Connecting to {context.Mode}...";
                if (await engine.ConnectAsync(CancellationToken.None))
                {
                    // Map UI operation name to Operation class
                    Operation op = operationName switch
                    {
                        "FRP Bypass" => new Operations.FrpBypassOperation(engine),
                        "Format" => new Operations.FormatOperation(engine),
                        "Flash" => new Operations.FlashOperation(null, engine),
                        "Device Info" => new Operations.DeviceInfoOperation(engine),
                        "Pattern Clear" => new Operations.PatternClearOperation(engine),
                        "Backup" => new Operations.BackupOperation(engine),
                        "Bootloader" => new Operations.BootloaderOperation(engine),
                        "Xiaomi Mi Account Bypass" => new Operations.XiaomiServiceOperation(engine),
                        "Oppo/Realme Advanced FRP" => new Operations.OppoServiceOperation(engine),
                        _ => throw new NotSupportedException($"Operation {operationName} not implemented.")
                    };

                    var progress = new Progress<ProgressUpdate>(u => {
                        this.Invoke(new Action(() => {
                            progressBar.Value = u.Percentage;
                            statusLabel.Text = u.Status;
                            if (!string.IsNullOrEmpty(u.Message))
                            {
                                logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] {u.Message}");
                            }
                        }));
                    });

                    bool success = await op.ExecuteAsync(context, progress, CancellationToken.None);
                    
                    if (success)
                    {
                        statusLabel.Text = $"{operationName} {LocalizationManager.GetString("OperationFinished")}";
                        NotificationHelper.ShowNotification("Success", $"{operationName} completed successfully.");
                    }
                    else
                    {
                        statusLabel.Text = $"{operationName} Failed!";
                        NotificationHelper.ShowNotification("Failed", $"{operationName} failed. Check logs.", ToolTipIcon.Error);
                    }
                    
                    await engine.DisconnectAsync();
                }
            }
            catch (Exception ex)
            {
                Logger.Error(ex, $"Failed to execute {operationName}");
                logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] ERROR: {ex.Message}");
                MessageBox.Show($"Operation failed: {ex.Message}", "Critical Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private async Task CheckForUpdatesAsync()
        {
            var latest = await UpdateManager.CheckForUpdatesAsync("1.0.0");
            if (latest != null)
            {
                updateLabel.Text = $"🚀 Update Available: v{latest.Version}";
                logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] New version available: v{latest.Version}. Visit GitHub to download.");
            }
        }

        private void UpdateDeviceOnPanels()
        {
            if (deviceSelector.SelectedIndex == -1)
            {
                UpdateDeviceOnPanels(null);
                return;
            }

            var context = _usbDevices[deviceSelector.SelectedIndex];
            UpdateDeviceOnPanels(context);
        }

        private void UpdateDeviceOnPanels(DeviceContext? context)
        {
            _deviceInfoPanel.SetDevice(context);
            _adbToolsPanel.SetDevice(context);
            _lockFrpPanel.SetDevice(context, null);
            _cloakPanel.SetDevice(context);
            _flashPanel.SetDevice(context, null);
            _bootloaderPanel.SetDevice(context);
            _healthPanel.SetDevice(context);
            _sandboxPanel.SetDevice(context);
            _driverProPanel.SetDevice(context);
            _expertPanel.SetDevice(context);
            _analyticsPanel.SetDeviceHealth(null); // Will be updated by Health Center
            
            // For backup panel, we need the protocol engine if available
            // _backupPanel.SetDevice(context, null, _adbClient);
            _frpPanel.SetDevice(context, null); 
            _restorePanel.SetDevice(context, null);
        }
    }
}
