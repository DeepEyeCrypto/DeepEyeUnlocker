using System;
using System.IO;
using System.Collections.Generic;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Core
{
    public class JobRecord
    {
        public DateTime Timestamp { get; set; }
        public string DeviceName { get; set; } = "";
        public string Operation { get; set; } = "";
        public string Status { get; set; } = "";
        public string LogSnippet { get; set; } = "";
    }

    public static class HistoryManager
    {
        private static readonly string HistoryPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "history.json");

        public static void SaveJob(JobRecord record)
        {
            var history = LoadHistory();
            history.Insert(0, record);
            if (history.Count > 500) history.RemoveAt(500); // Circular buffer

            string json = JsonConvert.SerializeObject(history, Formatting.Indented);
            File.WriteAllText(HistoryPath, json);
        }

        public static List<JobRecord> LoadHistory()
        {
            if (!File.Exists(HistoryPath)) return new List<JobRecord>();
            string json = File.ReadAllText(HistoryPath);
            return JsonConvert.DeserializeObject<List<JobRecord>>(json) ?? new List<JobRecord>();
        }
    }
}
