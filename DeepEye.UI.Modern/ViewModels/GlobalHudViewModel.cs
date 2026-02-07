using CommunityToolkit.Mvvm.ComponentModel;
using System;
using System.Collections.ObjectModel;
using System.Windows.Threading;

namespace DeepEye.UI.Modern.ViewModels
{
    public partial class GlobalHudViewModel : ObservableObject
    {
        public class HudEvent
        {
            public string Timestamp { get; set; } = DateTime.Now.ToString("HH:mm:ss");
            public string Message { get; set; }
            public string Type { get; set; } // INFO, SECURITY, NEXUS
        }

        public ObservableCollection<HudEvent> ActivityLog { get; } = new();
        private readonly DispatcherTimer _simulationTimer;
        private readonly Random _random = new();

        private readonly string[] _locations = { "BERLIN", "TOKYO", "NEW YORK", "DUBAI", "SEOUL", "MUMBAI" };
        private readonly string[] _actions = { "Device Secured", "Qualcomm v2 Bypassed", "Neural Report Generated", "Expert Workflow Published", "UniSoc Tiger Authenticated" };

        public GlobalHudViewModel()
        {
            _simulationTimer = new DispatcherTimer();
            _simulationTimer.Interval = TimeSpan.FromSeconds(5);
            _simulationTimer.Tick += (s, e) => GenerateEvent();
            _simulationTimer.Start();
            
            // Initial Seed
            ActivityLog.Add(new HudEvent { Message = "[!] DEEPEYE NEXUS ONLINE - VERSION 5.0.0", Type = "NEXUS" });
        }

        private void GenerateEvent()
        {
            string loc = _locations[_random.Next(_locations.Length)];
            string act = _actions[_random.Next(_actions.Length)];
            
            ActivityLog.Insert(0, new HudEvent 
            { 
                Message = $"INCOMING_{loc}: {act}",
                Type = "INFO"
            });

            if (ActivityLog.Count > 10) ActivityLog.RemoveAt(10);
        }
    }
}
