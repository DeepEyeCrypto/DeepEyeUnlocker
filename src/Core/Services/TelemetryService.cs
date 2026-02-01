using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Core.Services
{
    public class TelemetryService
    {
        private static readonly HttpClient Client = new HttpClient();
        private readonly string? _dsn; // Sentry or Custom API Endpoint
        private readonly bool _isEnabled;

        public TelemetryService(string? dsn = null, bool enabled = false)
        {
            _dsn = dsn;
            _isEnabled = enabled;
        }

        public async Task TrackEventAsync(string eventName, object? data = null)
        {
            if (!_isEnabled || string.IsNullOrEmpty(_dsn))
            {
                Logger.Info($"[Telemetry-Disabled] {eventName}");
                return;
            }

            try
            {
                var payload = new
                {
                    Event = eventName,
                    Timestamp = DateTime.UtcNow,
                    AppVersion = VersionManager.FullVersionDisplay,
                    Data = data
                };

                var content = new StringContent(JsonConvert.SerializeObject(payload), Encoding.UTF8, "application/json");
                await Client.PostAsync(_dsn, content);
            }
            catch (Exception ex)
            {
                // Silent failure to not disturb user flow
                Logger.Error(ex, "Telemetry push failed.");
            }
        }

        public async Task SendCrashReportAsync(string crashDetails, string diagnosticReport)
        {
            if (!_isEnabled || string.IsNullOrEmpty(_dsn)) return;

            await TrackEventAsync("AppCrash", new
            {
                Details = crashDetails,
                SystemInfo = diagnosticReport
            });
        }
    }
}
