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
        private System.Collections.Generic.List<LibUsbDotNet.Main.UsbRegistry> _usbDevices;
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

        private ComboBox langSelector = null!;

        private ToolTip _toolTip;

        public MainForm()
        {
            _deviceManager = new DeviceManager();
            _usbDevices = new System.Collections.Generic.List<LibUsbDotNet.Main.UsbRegistry>();
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
            this._cloakPanel = new CloakCenterPanel();

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
            
            mainTabs.TabPages.AddRange(new TabPage[] { operationsTab, deviceInfoTab, adbToolsTab, lockFrpTab, cloakTab, driverTab, resourceTab });

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
            
            foreach (var dev in _usbDevices)
            {
                var discovery = ProtocolDiscoveryService.Discover(dev);
                string displayMode = discovery.Chipset == "Unknown" ? "Unknown / MTP" : $"{discovery.Chipset} {discovery.Mode}";
                deviceSelector.Items.Add($"{dev.FullName} [{displayMode}]");
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

            var selectedRegistry = _usbDevices[deviceSelector.SelectedIndex];
            var discovery = ProtocolDiscoveryService.Discover(selectedRegistry);
            
            logConsole.Items.Add($"[{DateTime.Now:HH:mm:ss}] {LocalizationManager.GetString("OperationStarted")} {operationName}");
            progressBar.Value = 0;

            try 
            {
                using var usbDevice = selectedRegistry.Device;
                if (usbDevice == null) throw new Exception("Could not open USB device.");

                if (discovery.Confidence < 0.5f)
                {
                    statusLabel.Text = "Probing protocol...";
                    var activeDiscovery = await ProtocolDiscoveryService.HandshakeDiscoveryAsync(usbDevice);
                    if (activeDiscovery.Chipset != "Unknown")
                    {
                        discovery = activeDiscovery;
                    }
                }

                string chipset = discovery.Chipset.ToLower();
                string mode = $"{discovery.Chipset} {discovery.Mode}";

                var engine = OperationFactory.CreateEngine(chipset, usbDevice);
                if (engine == null) throw new Exception($"No engine available for {chipset}");

                statusLabel.Text = $"Connecting to {mode}...";
                if (await engine.ConnectAsync())
                {
                    // Map UI operation name to Operation class
                    Operation op = operationName switch
                    {
                        "FRP Bypass" => new Operations.FrpBypassOperation(),
                        "Format" => new Operations.FormatOperation(),
                        "Flash" => new Operations.FlashOperation("path_to_firmware.zip"),
                        "Device Info" => new Operations.DeviceInfoOperation(),
                        "Pattern Clear" => new Operations.PatternClearOperation(),
                        "Backup" => new Operations.BackupOperation(engine),
                        "Bootloader" => new Operations.BootloaderOperation(),
                        _ => throw new NotSupportedException($"Operation {operationName} not implemented.")
                    };

                    op.OnProgress += (progress, status) => {
                        this.Invoke(new Action(() => {
                            progressBar.Value = progress;
                            statusLabel.Text = status;
                        }));
                    };

                    bool success = await op.ExecuteAsync(new Device { Mode = mode, Chipset = chipset });
                    
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
                _deviceInfoPanel.SetDevice(null);
                _adbToolsPanel.SetDevice(null);
                _lockFrpPanel.SetDevice(null, null);
                _cloakPanel.SetDevice(null);
                return;
            }

            var selectedRegistry = _usbDevices[deviceSelector.SelectedIndex];
            var discovery = ProtocolDiscoveryService.Discover(selectedRegistry);
            
            ConnectionMode mode = discovery.Mode switch
            {
                "ADB" => ConnectionMode.ADB,
                "Fastboot" => ConnectionMode.Fastboot,
                "EDL" => ConnectionMode.EDL,
                "BROM" => ConnectionMode.BROM,
                "Preloader" => ConnectionMode.Preloader,
                "Download" => ConnectionMode.DownloadMode,
                "Recovery" => ConnectionMode.Recovery,
                _ => ConnectionMode.None
            };

            var context = new DeviceContext
            {
                Serial = selectedRegistry.SymbolicName ?? "Unknown",
                Brand = selectedRegistry.Vid.ToString("X4"),
                Model = selectedRegistry.Pid.ToString("X4"),
                Chipset = discovery.Chipset,
                Mode = mode
            };

            _deviceInfoPanel.SetDevice(context);
            _adbToolsPanel.SetDevice(context);
            _lockFrpPanel.SetDevice(context, null);
            _cloakPanel.SetDevice(context);
        }
    }
}
