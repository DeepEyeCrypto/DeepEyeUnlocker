using System;
using System.Collections.Generic;
using Xunit;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.HIL;

namespace DeepEyeUnlocker.Tests.Protocols.HIL
{
    public class SimulationValidatorTests
    {
        [Fact]
        public void ExactMatch_ReturnsMatch()
        {
            // Arrange
            var validator = new SimulationValidator();
            var options = new ValidationOptions();
            
            var scenarioA = new ProtocolScenario
            {
                Steps = new List<ScenarioStep>
                {
                    new ScenarioStep { Label = "S1", DataHex = "AABB", DelayMs = 10 }
                }
            };
            var scenarioB = new ProtocolScenario
            {
                Steps = new List<ScenarioStep>
                {
                    new ScenarioStep { Label = "S1", DataHex = "AABB", DelayMs = 10 }
                }
            };

            // Act
            var result = validator.ValidateAgainstGolden(scenarioA, scenarioB, options);

            // Assert
            Assert.True(result.IsMatch);
            Assert.Equal(1.0, result.SimilarityScore);
            Assert.Empty(result.Differences);
        }

        [Fact]
        public void DataMismatch_IdentifiesDifference()
        {
            // Arrange
            var validator = new SimulationValidator();
            var options = new ValidationOptions();
            
            var scenarioA = new ProtocolScenario
            {
                Steps = new List<ScenarioStep> { new ScenarioStep { Label = "S1", DataHex = "AABB" } }
            };
            var scenarioB = new ProtocolScenario
            {
                Steps = new List<ScenarioStep> { new ScenarioStep { Label = "S1", DataHex = "CCDD" } }
            };

            // Act
            var result = validator.ValidateAgainstGolden(scenarioA, scenarioB, options);

            // Assert
            Assert.False(result.IsMatch);
            Assert.Single(result.Differences);
            Assert.Equal("DataMismatch", result.Differences[0].DifferenceType);
            Assert.Equal(0.0, result.SimilarityScore);
        }

        [Fact]
        public void TimingDrift_WithinTolerance_Matches()
        {
            // Arrange
            var validator = new SimulationValidator();
            var options = new ValidationOptions { TimingTolerance = 0.2 }; // 20%
            
            var scenarioA = new ProtocolScenario
            {
                Steps = new List<ScenarioStep> { new ScenarioStep { Label = "S1", DataHex = "AABB", DelayMs = 110 } }
            };
            var scenarioB = new ProtocolScenario
            {
                Steps = new List<ScenarioStep> { new ScenarioStep { Label = "S1", DataHex = "AABB", DelayMs = 100 } }
            };

            // Act
            var result = validator.ValidateAgainstGolden(scenarioA, scenarioB, options);

            // Assert
            Assert.True(result.IsMatch);
        }

        [Fact]
        public void TimingDrift_BeyondTolerance_Fails()
        {
            // Arrange
            var validator = new SimulationValidator();
            var options = new ValidationOptions { TimingTolerance = 0.1 }; // 10%
            
            var scenarioA = new ProtocolScenario
            {
                Steps = new List<ScenarioStep> { new ScenarioStep { Label = "S1", DataHex = "AABB", DelayMs = 150 } }
            };
            var scenarioB = new ProtocolScenario
            {
                Steps = new List<ScenarioStep> { new ScenarioStep { Label = "S1", DataHex = "AABB", DelayMs = 100 } }
            };

            // Act
            var result = validator.ValidateAgainstGolden(scenarioA, scenarioB, options);

            // Assert
            Assert.False(result.IsMatch);
            Assert.Equal("TimingDrift", result.Differences[0].DifferenceType);
        }
    }
}
