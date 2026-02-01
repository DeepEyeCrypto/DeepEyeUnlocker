using System.IO;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using Newtonsoft.Json.Serialization;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Tests.Mocks
{
    public static class ScenarioLoader
    {
        private static readonly JsonSerializerSettings Settings = new()
        {
            ContractResolver = new DefaultContractResolver { NamingStrategy = new SnakeCaseNamingStrategy() },
            Converters = { new StringEnumConverter(new SnakeCaseNamingStrategy()) }
        };

        public static ProtocolScenario Load(string filePath)
        {
            var json = File.ReadAllText(filePath);
            return JsonConvert.DeserializeObject<ProtocolScenario>(json, Settings) 
                   ?? throw new InvalidDataException($"Failed to load scenario from {filePath}");
        }
    }
}
