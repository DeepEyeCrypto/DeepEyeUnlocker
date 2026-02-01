using System;
using System.Windows;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Core.Services;

namespace DeepEye.UI.Modern.Infrastructure
{
    public static class GlobalExceptionHandler
    {
        public static void Initialize()
        {
            // Catch WPF UI Thread exceptions
            Application.Current.DispatcherUnhandledException += (s, e) =>
            {
                HandleException(e.Exception, "WPF Dispatcher");
                e.Handled = true; // Prevents immediate crash
            };

            // Catch non-UI thread exceptions
            AppDomain.CurrentDomain.UnhandledException += (s, e) =>
            {
                HandleException(e.ExceptionObject as Exception, "AppDomain");
            };
        }

        private static void HandleException(Exception? ex, string source)
        {
            if (ex == null) return;

            Logger.Error(ex, $"Unhandled Exception from {source}");

            try
            {
                var diagnostic = new DiagnosticService();
                string reportPath = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "CRASH_REPORT.txt");
                
                var sb = new System.Text.StringBuilder();
                sb.AppendLine("!!! DEEPEYEUNLOCKER CRASH DETECTED !!!");
                sb.AppendLine($"Source: {source}");
                sb.AppendLine($"Error: {ex.Message}");
                sb.AppendLine("---------------------------------------");
                sb.AppendLine(ex.ToString());
                sb.AppendLine();
                sb.AppendLine(diagnostic.GenerateSystemReport());

                System.IO.File.WriteAllText(reportPath, sb.ToString());

                // Optional: Push to Telemetry if enabled
                var telemetry = new TelemetryService(null, false); // Placeholder config
                _ = telemetry.SendCrashReportAsync(ex.Message, sb.ToString());

                MessageBox.Show(
                    $"A critical error occurred ({source}).\n\nA crash report has been generated at:\n{reportPath}\n\nPlease include this file when reporting the issue.",
                    "DeepEyeUnlocker - Critical Error",
                    MessageBoxButton.OK,
                    MessageBoxImage.Error);
            }
            catch
            {
                // Last resort if even report generation fails
            }
        }
    }
}
