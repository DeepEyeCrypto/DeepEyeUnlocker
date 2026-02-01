using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using Xunit;
using Xunit.Abstractions;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json.Schema;

namespace DeepEyeUnlocker.Tests.Protocols
{
    public class ScenarioSchemaValidationTests
    {
        private readonly ITestOutputHelper _output;
        private readonly string _schemaPath;
        private readonly string _scenariosDir;

        public ScenarioSchemaValidationTests(ITestOutputHelper output)
        {
            _output = output;
            _schemaPath = Path.Combine(Directory.GetCurrentDirectory(), "docs", "protocol_scenarios.schema.json");
            _scenariosDir = Path.Combine(Directory.GetCurrentDirectory(), "scenarios");
        }

        [Fact]
        public void AllScenarios_ShouldConformToSchema()
        {
            // Arrange
            Assert.True(File.Exists(_schemaPath), $"Schema file not found at: {_schemaPath}");
            var schemaJson = File.ReadAllText(_schemaPath);
            var schema = JSchema.Parse(schemaJson);

            var scenarioFiles = Directory.GetFiles(_scenariosDir, "*.json", SearchOption.AllDirectories);
            Assert.NotEmpty(scenarioFiles);

            bool allValid = true;

            // Act & Assert
            foreach (var file in scenarioFiles)
            {
                var relativePath = Path.GetRelativePath(_scenariosDir, file);
                var json = File.ReadAllText(file);
                var jObject = JObject.Parse(json);

                bool isValid = jObject.IsValid(schema, out IList<string> errors);

                if (!isValid)
                {
                    allValid = false;
                    _output.WriteLine($"[FAIL] {relativePath}:");
                    foreach (var error in errors)
                    {
                        _output.WriteLine($"  - {error}");
                    }
                }
                else
                {
                    _output.WriteLine($"[PASS] {relativePath}");
                }
            }

            Assert.True(allValid, "One or more scenario files failed schema validation. See output for details.");
        }
    }
}
