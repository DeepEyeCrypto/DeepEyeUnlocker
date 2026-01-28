using System;
using System.IO;
using System.Collections.Generic;
using Microsoft.Data.Sqlite;
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
        private static readonly string DbPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "history.db");
        private static readonly string ConnectionString = $"Data Source={DbPath}";

        static HistoryManager()
        {
            InitializeDatabase();
            MigrateFromJson();
        }

        private static void InitializeDatabase()
        {
            using var connection = new SqliteConnection(ConnectionString);
            connection.Open();

            var command = connection.CreateCommand();
            command.CommandText = @"
                CREATE TABLE IF NOT EXISTS Jobs (
                    Id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Timestamp TEXT,
                    DeviceName TEXT,
                    Operation TEXT,
                    Status TEXT,
                    LogSnippet TEXT
                )";
            command.ExecuteNonQuery();
        }

        public static void SaveJob(JobRecord record)
        {
            using var connection = new SqliteConnection(ConnectionString);
            connection.Open();

            var command = connection.CreateCommand();
            command.CommandText = @"
                INSERT INTO Jobs (Timestamp, DeviceName, Operation, Status, LogSnippet)
                VALUES ($ts, $device, $op, $status, $log)";
            
            command.Parameters.AddWithValue("$ts", record.Timestamp.ToString("yyyy-MM-dd HH:mm:ss"));
            command.Parameters.AddWithValue("$device", record.DeviceName);
            command.Parameters.AddWithValue("$op", record.Operation);
            command.Parameters.AddWithValue("$status", record.Status);
            command.Parameters.AddWithValue("$log", record.LogSnippet);

            command.ExecuteNonQuery();
        }

        public static List<JobRecord> LoadHistory()
        {
            var results = new List<JobRecord>();
            using var connection = new SqliteConnection(ConnectionString);
            connection.Open();

            var command = connection.CreateCommand();
            command.CommandText = "SELECT Timestamp, DeviceName, Operation, Status, LogSnippet FROM Jobs ORDER BY Timestamp DESC LIMIT 500";

            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                results.Add(new JobRecord
                {
                    Timestamp = DateTime.Parse(reader.GetString(0)),
                    DeviceName = reader.GetString(1),
                    Operation = reader.GetString(2),
                    Status = reader.GetString(3),
                    LogSnippet = reader.IsDBNull(4) ? "" : reader.GetString(4)
                });
            }

            return results;
        }

        private static void MigrateFromJson()
        {
            string oldPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "history.json");
            if (File.Exists(oldPath))
            {
                try
                {
                    string json = File.ReadAllText(oldPath);
                    var legacyData = JsonConvert.DeserializeObject<List<JobRecord>>(json);
                    if (legacyData != null)
                    {
                        foreach (var job in legacyData) SaveJob(job);
                    }
                    File.Delete(oldPath); // Clean up
                }
                catch { /* Ignore migration errors */ }
            }
        }
    }
}
