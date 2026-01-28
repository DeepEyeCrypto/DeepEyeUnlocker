using System;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Qualcomm;
using LibUsbDotNet;

namespace DeepEyeUnlocker.UI.Panels
{
    /// <summary>
    /// Firehose Control Panel for flash operations after EDL entry
    /// </summary>
    public class FirehoseControlPanel : Panel
    {
        private FirehoseManager? _manager;
        private DeviceContext? _currentDevice;
        private UsbDevice? _usbDevice;
        
        // UI Components
        private Label _titleLabel = null!;
        private Label _sessionStatusLabel = null!;
        private Label _programmerLabel = null!;
        private ComboBox _programmerComboBox = null!;
        private Button _initButton = null!;
        private Button _browseProgrammerButton = null!;
        
        private GroupBox _operationsGroup = null!;
        private ComboBox _partitionComboBox = null!;
        private Button _readButton = null!;
        private Button _writeButton = null!;
        private Button _eraseButton = null!;
        
        private ProgressBar _progressBar = null!;
        private RichTextBox _logBox = null!;

        // Common partition names
        private static readonly string[] CommonPartitions = new[]
        {
            "boot", "recovery", "system", "vendor", "userdata",
            "persist", "modem", "bluetooth", "dsp", "aboot",
            "sbl1", "rpm", "tz", "hyp", "keymaster",
            "cmnlib", "cmnlib64", "devcfg", "frp", "misc"
        };

        public FirehoseControlPanel()
        {
            InitializeComponents();
        }

        /// <summary>
        /// Set the device context and USB device for Firehose operations
        /// </summary>
        public void SetDevice(DeviceContext? device, UsbDevice? usbDevice)
        {
            _currentDevice = device;
            _usbDevice = usbDevice;
            UpdateUI();
        }

        /// <summary>
        /// Get the active Firehose manager
        /// </summary>
        public FirehoseManager? GetManager() => _manager;

        private void InitializeComponents()
        {
            this.BackColor = Color.FromArgb(30, 30, 35);
            this.Padding = new Padding(15);
            this.Size = new Size(450, 550);

            int y = 15;

            // Title
            _titleLabel = new Label
            {
                Text = "🔥 Firehose Flash Manager",
                Font = new Font("Segoe UI", 14, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(15, y),
                AutoSize = true
            };
            this.Controls.Add(_titleLabel);
            y += 40;

            // Session Status
            this.Controls.Add(CreateLabel("Session Status:", 15, y));
            _sessionStatusLabel = CreateValueLabel("Not Initialized", 130, y);
            _sessionStatusLabel.ForeColor = Color.Gray;
            this.Controls.Add(_sessionStatusLabel);
            y += 28;

            // Programmer Selection
            _programmerLabel = CreateLabel("Programmer:", 15, y);
            this.Controls.Add(_programmerLabel);
            y += 22;

            _programmerComboBox = new ComboBox
            {
                Location = new Point(15, y),
                Size = new Size(280, 28),
                DropDownStyle = ComboBoxStyle.DropDownList,
                BackColor = Color.FromArgb(45, 45, 50),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _programmerComboBox.Items.Add("(Auto-detect based on SoC)");
            _programmerComboBox.SelectedIndex = 0;
            this.Controls.Add(_programmerComboBox);

            _browseProgrammerButton = new Button
            {
                Text = "📂",
                Location = new Point(305, y - 2),
                Size = new Size(40, 28),
                BackColor = Color.FromArgb(50, 50, 60),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _browseProgrammerButton.FlatAppearance.BorderSize = 1;
            _browseProgrammerButton.Click += OnBrowseProgrammer;
            this.Controls.Add(_browseProgrammerButton);

            _initButton = new Button
            {
                Text = "⚡ Initialize",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                Location = new Point(355, y - 2),
                Size = new Size(80, 28),
                BackColor = Color.FromArgb(0, 123, 255),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _initButton.FlatAppearance.BorderSize = 0;
            _initButton.Click += OnInitializeClicked;
            this.Controls.Add(_initButton);
            y += 45;

            // Operations Group
            _operationsGroup = new GroupBox
            {
                Text = "Partition Operations",
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, y),
                Size = new Size(420, 130),
                Enabled = false
            };
            this.Controls.Add(_operationsGroup);

            var opY = 25;
            
            var partitionLabel = new Label
            {
                Text = "Partition:",
                Font = new Font("Segoe UI", 9),
                ForeColor = Color.FromArgb(150, 150, 160),
                Location = new Point(15, opY),
                AutoSize = true
            };
            _operationsGroup.Controls.Add(partitionLabel);

            _partitionComboBox = new ComboBox
            {
                Location = new Point(85, opY - 3),
                Size = new Size(150, 25),
                DropDownStyle = ComboBoxStyle.DropDown,
                BackColor = Color.FromArgb(45, 45, 50),
                ForeColor = Color.White
            };
            foreach (var p in CommonPartitions)
                _partitionComboBox.Items.Add(p);
            _partitionComboBox.SelectedIndex = 0;
            _operationsGroup.Controls.Add(_partitionComboBox);
            opY += 40;

            // Operation Buttons
            _readButton = CreateOperationButton("📖 Read", 15, opY, Color.FromArgb(40, 167, 69));
            _readButton.Click += OnReadClicked;
            _operationsGroup.Controls.Add(_readButton);

            _writeButton = CreateOperationButton("✏️ Write", 140, opY, Color.FromArgb(255, 193, 7));
            _writeButton.Click += OnWriteClicked;
            _operationsGroup.Controls.Add(_writeButton);

            _eraseButton = CreateOperationButton("🗑️ Erase", 265, opY, Color.FromArgb(220, 53, 69));
            _eraseButton.Click += OnEraseClicked;
            _operationsGroup.Controls.Add(_eraseButton);

            y += 145;

            // Progress Bar
            _progressBar = new ProgressBar
            {
                Location = new Point(15, y),
                Size = new Size(420, 10),
                Style = ProgressBarStyle.Continuous,
                Visible = false
            };
            this.Controls.Add(_progressBar);
            y += 20;

            // Log Box
            _logBox = new RichTextBox
            {
                Location = new Point(15, y),
                Size = new Size(420, 200),
                BackColor = Color.FromArgb(20, 20, 25),
                ForeColor = Color.FromArgb(180, 180, 180),
                Font = new Font("Consolas", 8.5f),
                BorderStyle = BorderStyle.None,
                ReadOnly = true
            };
            this.Controls.Add(_logBox);

            LogMessage("Firehose Control Panel initialized.");
            LogMessage("Connect a device in EDL mode to begin.");
        }

        private Button CreateOperationButton(string text, int x, int y, Color bgColor)
        {
            var btn = new Button
            {
                Text = text,
                Font = new Font("Segoe UI", 9),
                Location = new Point(x, y),
                Size = new Size(110, 35),
                BackColor = bgColor,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btn.FlatAppearance.BorderSize = 0;
            return btn;
        }

        private Label CreateLabel(string text, int x, int y) => new Label
        {
            Text = text,
            Font = new Font("Segoe UI", 9),
            ForeColor = Color.FromArgb(150, 150, 160),
            Location = new Point(x, y),
            AutoSize = true
        };

        private Label CreateValueLabel(string text, int x, int y) => new Label
        {
            Text = text,
            Font = new Font("Segoe UI", 9, FontStyle.Bold),
            ForeColor = Color.White,
            Location = new Point(x, y),
            AutoSize = true
        };

        private void UpdateUI()
        {
            if (_currentDevice == null || _currentDevice.Mode != ConnectionMode.EDL)
            {
                _initButton.Enabled = false;
                _operationsGroup.Enabled = false;
                _sessionStatusLabel.Text = "No EDL device";
                _sessionStatusLabel.ForeColor = Color.Gray;
                return;
            }

            _initButton.Enabled = true;
            
            // Populate available programmers
            PopulateProgrammers();
            
            if (_manager != null && _manager.IsReady)
            {
                _sessionStatusLabel.Text = "✅ Ready";
                _sessionStatusLabel.ForeColor = Color.FromArgb(40, 167, 69);
                _operationsGroup.Enabled = true;
            }
            else
            {
                _sessionStatusLabel.Text = "Not Initialized";
                _sessionStatusLabel.ForeColor = Color.FromArgb(255, 193, 7);
                _operationsGroup.Enabled = false;
            }
        }

        private void PopulateProgrammers()
        {
            _programmerComboBox.Items.Clear();
            _programmerComboBox.Items.Add("(Auto-detect based on SoC)");

            if (_currentDevice != null)
            {
                var tempManager = new FirehoseManager();
                var available = tempManager.GetAvailableProgrammers(_currentDevice);
                foreach (var p in available)
                {
                    _programmerComboBox.Items.Add(p.FileName);
                }
            }

            _programmerComboBox.SelectedIndex = 0;
        }

        private void OnBrowseProgrammer(object? sender, EventArgs e)
        {
            using var dialog = new OpenFileDialog
            {
                Title = "Select Firehose Programmer",
                Filter = "Programmer Files (*.elf;*.mbn)|*.elf;*.mbn|All Files (*.*)|*.*",
                InitialDirectory = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "programmers")
            };

            if (dialog.ShowDialog() == DialogResult.OK)
            {
                _programmerComboBox.Items.Add(dialog.FileName);
                _programmerComboBox.SelectedIndex = _programmerComboBox.Items.Count - 1;
                LogMessage($"Selected programmer: {Path.GetFileName(dialog.FileName)}");
            }
        }

        private async void OnInitializeClicked(object? sender, EventArgs e)
        {
            if (_currentDevice == null || _usbDevice == null)
            {
                LogMessage("No EDL device connected!", Color.Red);
                return;
            }

            _initButton.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Style = ProgressBarStyle.Marquee;

            LogMessage("\n--- Initializing Firehose Session ---", Color.Cyan);

            try
            {
                _manager = new FirehoseManager();

                string? programmerPath = null;
                if (_programmerComboBox.SelectedIndex > 0)
                {
                    var selected = _programmerComboBox.SelectedItem?.ToString();
                    if (!string.IsNullOrEmpty(selected) && File.Exists(selected))
                    {
                        programmerPath = selected;
                    }
                }

                var progress = new Progress<ProgressUpdate>(update =>
                {
                    LogMessage(update.Message);
                    if (_progressBar.Style != ProgressBarStyle.Marquee)
                    {
                        _progressBar.Value = Math.Min(update.ProgressPercent, 100);
                    }
                });

                using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(2));
                var result = await _manager.InitializeSessionAsync(
                    _usbDevice,
                    programmerPath,
                    _currentDevice,
                    progress,
                    cts.Token);

                if (result.Success)
                {
                    LogMessage($"✅ Session ready! Duration: {result.Duration.TotalSeconds:F1}s", Color.FromArgb(40, 167, 69));
                    if (result.LoadedProgrammer != null)
                    {
                        LogMessage($"Programmer: {result.LoadedProgrammer.FileName}");
                        _programmerLabel.Text = $"Programmer: {result.LoadedProgrammer.FileName}";
                    }
                    _sessionStatusLabel.Text = "✅ Ready";
                    _sessionStatusLabel.ForeColor = Color.FromArgb(40, 167, 69);
                    _operationsGroup.Enabled = true;
                }
                else
                {
                    LogMessage($"❌ {result.Message}", Color.FromArgb(220, 53, 69));
                    _sessionStatusLabel.Text = "Failed";
                    _sessionStatusLabel.ForeColor = Color.FromArgb(220, 53, 69);
                }
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _progressBar.Visible = false;
                _initButton.Enabled = true;
            }
        }

        private async void OnReadClicked(object? sender, EventArgs e)
        {
            if (_manager == null || !_manager.IsReady) return;

            var partition = _partitionComboBox.Text;
            if (string.IsNullOrEmpty(partition)) return;

            using var dialog = new SaveFileDialog
            {
                Title = $"Save {partition} partition",
                FileName = $"{partition}.img",
                Filter = "Image Files (*.img)|*.img|Binary Files (*.bin)|*.bin|All Files (*.*)|*.*"
            };

            if (dialog.ShowDialog() != DialogResult.OK) return;

            await ExecuteWithProgress(async (progress, ct) =>
            {
                LogMessage($"Reading partition: {partition}...", Color.Cyan);
                var data = await _manager.ReadPartitionAsync(partition, progress, ct);
                
                if (data.Length > 0)
                {
                    await File.WriteAllBytesAsync(dialog.FileName, data, ct);
                    LogMessage($"✅ Saved {data.Length} bytes to {Path.GetFileName(dialog.FileName)}", Color.FromArgb(40, 167, 69));
                    return true;
                }
                else
                {
                    LogMessage("❌ No data read from partition", Color.FromArgb(220, 53, 69));
                    return false;
                }
            });
        }

        private async void OnWriteClicked(object? sender, EventArgs e)
        {
            if (_manager == null || !_manager.IsReady) return;

            var partition = _partitionComboBox.Text;
            if (string.IsNullOrEmpty(partition)) return;

            using var dialog = new OpenFileDialog
            {
                Title = $"Select image for {partition}",
                Filter = "Image Files (*.img)|*.img|Binary Files (*.bin)|*.bin|All Files (*.*)|*.*"
            };

            if (dialog.ShowDialog() != DialogResult.OK) return;

            // Confirmation
            var result = MessageBox.Show(
                $"⚠️ WARNING: You are about to write to partition '{partition}'.\n\n" +
                $"File: {Path.GetFileName(dialog.FileName)}\n" +
                $"Size: {new FileInfo(dialog.FileName).Length:N0} bytes\n\n" +
                "This operation is potentially DANGEROUS and may brick your device.\n\n" +
                "Are you sure you want to proceed?",
                "Confirm Partition Write",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result != DialogResult.Yes) return;

            await ExecuteWithProgress(async (progress, ct) =>
            {
                LogMessage($"Writing to partition: {partition}...", Color.Cyan);
                var data = await File.ReadAllBytesAsync(dialog.FileName, ct);
                
                var success = await _manager.WritePartitionAsync(partition, data, progress, ct);
                
                if (success)
                {
                    LogMessage($"✅ Successfully wrote {data.Length} bytes to {partition}", Color.FromArgb(40, 167, 69));
                }
                else
                {
                    LogMessage($"❌ Write failed for {partition}", Color.FromArgb(220, 53, 69));
                }
                
                return success;
            });
        }

        private async void OnEraseClicked(object? sender, EventArgs e)
        {
            if (_manager == null || !_manager.IsReady) return;

            var partition = _partitionComboBox.Text;
            if (string.IsNullOrEmpty(partition)) return;

            // Confirmation
            var result = MessageBox.Show(
                $"⚠️ WARNING: You are about to ERASE partition '{partition}'.\n\n" +
                "This operation is IRREVERSIBLE and may brick your device.\n" +
                "Make sure you have a backup before proceeding.\n\n" +
                "Are you absolutely sure?",
                "Confirm Partition Erase",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (result != DialogResult.Yes) return;

            await ExecuteWithProgress(async (progress, ct) =>
            {
                LogMessage($"Erasing partition: {partition}...", Color.Cyan);
                
                var success = await _manager.ErasePartitionAsync(partition, progress, ct);
                
                if (success)
                {
                    LogMessage($"✅ Successfully erased {partition}", Color.FromArgb(40, 167, 69));
                }
                else
                {
                    LogMessage($"❌ Erase failed for {partition}", Color.FromArgb(220, 53, 69));
                }
                
                return success;
            });
        }

        private async Task ExecuteWithProgress(Func<IProgress<ProgressUpdate>, CancellationToken, Task<bool>> action)
        {
            _operationsGroup.Enabled = false;
            _progressBar.Visible = true;
            _progressBar.Style = ProgressBarStyle.Continuous;
            _progressBar.Value = 0;

            try
            {
                var progress = new Progress<ProgressUpdate>(update =>
                {
                    _progressBar.Value = Math.Min(update.ProgressPercent, 100);
                });

                using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(10));
                await action(progress, cts.Token);
            }
            catch (Exception ex)
            {
                LogMessage($"Error: {ex.Message}", Color.FromArgb(220, 53, 69));
            }
            finally
            {
                _progressBar.Visible = false;
                _operationsGroup.Enabled = true;
            }
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
    }
}
