using System;
using System.Drawing;
using System.Windows.Forms;
using System.Collections.Generic;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Features.Analytics.Models;
using DeepEyeUnlocker.Features.Analytics.Services;

namespace DeepEyeUnlocker.Features.Analytics.UI
{
    public class AnalyticsPanel : UserControl
    {
        private readonly CveScanner _cveScanner = new();
        private readonly FleetAnalytics _fleetAnalytics = new();
        
        private DataGridView _dgvCve = null!;
        private Label _lblRiskScore = null!;
        private Label _lblSummary = null!;
        private Button _btnScan = null!;
        
        private DeviceHealthReport? _currentHealth;

        public AnalyticsPanel()
        {
            InitializeComponents();
        }

        private void InitializeComponents()
        {
            this.Dock = DockStyle.Fill;
            this.BackColor = Color.FromArgb(20, 20, 25);
            this.Padding = new Padding(20);

            var lblHeader = new Label
            {
                Text = "📊 Analytics & Vulnerability Intelligence",
                Font = new Font("Segoe UI", 18, FontStyle.Bold),
                ForeColor = Color.White,
                Location = new Point(20, 20),
                AutoSize = true
            };

            _lblRiskScore = new Label
            {
                Text = "Risk Score: --",
                Font = new Font("Segoe UI", 12, FontStyle.Bold),
                ForeColor = Color.Gray,
                Location = new Point(20, 70),
                AutoSize = true
            };

            _dgvCve = new DataGridView
            {
                Location = new Point(20, 110),
                Size = new Size(840, 300),
                BackgroundColor = Color.FromArgb(30, 30, 35),
                ForeColor = Color.White,
                BorderStyle = BorderStyle.None,
                AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill,
                ReadOnly = true,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect,
                AllowUserToAddRows = false
            };
            _dgvCve.DefaultCellStyle.BackColor = Color.FromArgb(30, 30, 35);
            _dgvCve.DefaultCellStyle.ForeColor = Color.White;
            _dgvCve.ColumnHeadersDefaultCellStyle.BackColor = Color.FromArgb(45, 45, 50);
            _dgvCve.ColumnHeadersDefaultCellStyle.ForeColor = Color.White;
            _dgvCve.EnableHeadersVisualStyles = false;

            _dgvCve.Columns.Add("CVE", "Vulnerability ID");
            _dgvCve.Columns.Add("Severity", "Severity");
            _dgvCve.Columns.Add("Component", "Component");
            _dgvCve.Columns.Add("Description", "Description");

            _btnScan = new Button
            {
                Text = "🔍 Scan for CVEs",
                Location = new Point(710, 60),
                Size = new Size(150, 40),
                BackColor = Color.FromArgb(0, 122, 204),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10, FontStyle.Bold)
            };
            _btnScan.Click += (s, e) => RunCveScan();

            var btnExport = new Button
            {
                Text = "📄 Export Report",
                Location = new Point(550, 60),
                Size = new Size(150, 40),
                BackColor = Color.FromArgb(60, 60, 65),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10, FontStyle.Bold)
            };
            btnExport.Click += OnExportClicked;
            this.Controls.Add(btnExport);

            _lblSummary = new Label
            {
                Text = "Fleet Summary: Waiting for data...",
                Font = new Font("Segoe UI", 10),
                ForeColor = Color.FromArgb(200, 200, 200),
                Location = new Point(20, 430),
                Size = new Size(840, 100)
            };

            this.Controls.AddRange(new Control[] { lblHeader, _lblRiskScore, _dgvCve, _btnScan, _lblSummary });
        }

        public void SetDeviceHealth(DeviceHealthReport? report)
        {
            _currentHealth = report;
            _btnScan.Enabled = _currentHealth != null;
            if (_currentHealth == null)
            {
                _lblRiskScore.Text = "Risk Score: Select a device";
                _dgvCve.Rows.Clear();
            }
        }

        private void RunCveScan()
        {
            if (_currentHealth == null) return;

            var report = _cveScanner.Scan(_currentHealth);
            _dgvCve.Rows.Clear();
            
            foreach (var v in report.Vulnerabilities)
            {
                int rowIdx = _dgvCve.Rows.Add(v.Id, v.Severity, v.AffectedComponent, v.Description);
                if (v.Severity == "Critical") _dgvCve.Rows[rowIdx].DefaultCellStyle.ForeColor = Color.Red;
                else if (v.Severity == "High") _dgvCve.Rows[rowIdx].DefaultCellStyle.ForeColor = Color.OrangeRed;
            }

            _lblRiskScore.Text = $"Risk Score: {report.RiskScore}/10";
            _lblRiskScore.ForeColor = report.RiskScore > 7 ? Color.Red : (report.RiskScore > 4 ? Color.Orange : Color.Green);
        }

        private void OnExportClicked(object? sender, EventArgs e)
        {
            if (_currentHealth == null) return;

            using (var sfd = new SaveFileDialog { Filter = "Text Report|*.txt", FileName = $"DeepEye_Security_{_currentHealth.SerialNumber}.txt" })
            {
                if (sfd.ShowDialog() == DialogResult.OK)
                {
                    var cveReport = _cveScanner.Scan(_currentHealth);
                    _ = ReportGenerator.ExportPdfReportAsync(_currentHealth, cveReport, sfd.FileName);
                    MessageBox.Show("Security Report Exported.", "Success");
                }
            }
        }
        public void UpdateFleetSummary(IEnumerable<DeviceHealthReport> healthList, IEnumerable<CveReport> cveList)
        {
            var summary = _fleetAnalytics.GenerateSummary(healthList, cveList);
            _lblSummary.Text = $"Managed Devices: {summary.ManagedDevicesCount}\n" +
                               $"Health Alerts: {summary.HealthAlertsCount}\n" +
                               $"Critical Vulnerabilities: {summary.CriticalCvesCount}\n" +
                               $"Avg Fleet Risk: {summary.AverageRiskScore:F1}/10\n" +
                               $"Top Threats: {string.Join(", ", summary.CommonVulnerabilities)}";
        }
    }
}
