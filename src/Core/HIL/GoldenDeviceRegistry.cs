using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;

namespace DeepEyeUnlocker.Core.HIL
{
    public class GoldenDeviceRegistry
    {
        private readonly string _baseDir;
        private readonly string _registryFile;
        private GoldenRegistryRoot _registry = new();

        public GoldenDeviceRegistry(string baseDir)
        {
            _baseDir = baseDir;
            _registryFile = Path.Combine(_baseDir, "registry.json");
            Load();
        }

        public void RegisterDevice(GoldenDeviceInfo device)
        {
            var existing = _registry.Devices.FirstOrDefault(d => d.DeviceId == device.DeviceId);
            if (existing != null)
            {
                _registry.Devices.Remove(existing);
            }
            
            device.RegisteredAt = DateTime.Now;
            _registry.Devices.Add(device);
            Save();
        }

        public GoldenDeviceInfo? GetDevice(string deviceId)
        {
            return _registry.Devices.FirstOrDefault(d => d.DeviceId == deviceId);
        }

        public IEnumerable<GoldenDeviceInfo> ListDevices() => _registry.Devices;

        private void Load()
        {
            if (!File.Exists(_registryFile))
            {
                _registry = new GoldenRegistryRoot();
                return;
            }

            try
            {
                var json = File.ReadAllText(_registryFile);
                _registry = JsonConvert.DeserializeObject<GoldenRegistryRoot>(json) ?? new GoldenRegistryRoot();
            }
            catch
            {
                _registry = new GoldenRegistryRoot();
            }
        }

        private void Save()
        {
            if (!Directory.Exists(_baseDir))
            {
                Directory.CreateDirectory(_baseDir);
            }

            var json = JsonConvert.SerializeObject(_registry, Formatting.Indented);
            File.WriteAllText(_registryFile, json);
        }
    }
}
