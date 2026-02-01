using System;
using System.Collections.Generic;
using System.Linq;
using DeepEyeUnlocker.Core.Models;

namespace DeepEyeUnlocker.Core.HIL
{
    public class ValidationOptions
    {
        public double TimingTolerance { get; set; } = 0.2; // 20% jitter
        public bool IgnoreVolatileFields { get; set; } = true;
    }

    public class PacketDiff
    {
        public int StepIndex { get; set; }
        public string Label { get; set; } = string.Empty;
        public string ExpectedHex { get; set; } = string.Empty;
        public string ActualHex { get; set; } = string.Empty;
        public int ExpectedDelay { get; set; }
        public int ActualDelay { get; set; }
        public string DifferenceType { get; set; } = "DataMismatch";
    }

    public class ValidationResult
    {
        public bool IsMatch { get; set; }
        public double SimilarityScore { get; set; }
        public List<PacketDiff> Differences { get; set; } = new();
        public string Recommendation { get; set; } = string.Empty;
    }

    public class SimulationValidator
    {
        public ValidationResult ValidateAgainstGolden(ProtocolScenario actual, ProtocolScenario expected, ValidationOptions options)
        {
            var result = new ValidationResult { IsMatch = true };
            int stepCount = Math.Max(actual.Steps.Count, expected.Steps.Count);
            int matches = 0;

            for (int i = 0; i < stepCount; i++)
            {
                if (i >= actual.Steps.Count || i >= expected.Steps.Count)
                {
                    result.IsMatch = false;
                    result.Differences.Add(new PacketDiff 
                    { 
                        StepIndex = i, 
                        DifferenceType = "StepCountMismatch",
                        Label = i < actual.Steps.Count ? actual.Steps[i].Label : expected.Steps[i].Label
                    });
                    continue;
                }

                var actualStep = actual.Steps[i];
                var expectedStep = expected.Steps[i];

                bool dataMatch = actualStep.DataHex == expectedStep.DataHex;
                bool timingMatch = IsTimingWithinTolerance(actualStep.DelayMs, expectedStep.DelayMs, options.TimingTolerance);

                if (!dataMatch || !timingMatch)
                {
                    result.IsMatch = false;
                    result.Differences.Add(new PacketDiff
                    {
                        StepIndex = i,
                        Label = actualStep.Label,
                        ActualHex = actualStep.DataHex,
                        ExpectedHex = expectedStep.DataHex,
                        ActualDelay = actualStep.DelayMs,
                        ExpectedDelay = expectedStep.DelayMs,
                        DifferenceType = !dataMatch ? "DataMismatch" : "TimingDrift"
                    });
                }
                else
                {
                    matches++;
                }
            }

            result.SimilarityScore = stepCount > 0 ? (double)matches / stepCount : 1.0;
            result.Recommendation = result.IsMatch ? "No action required." : 
                                   result.SimilarityScore > 0.9 ? "Update scenario (minor drift)." : 
                                   "Investigate protocol drift or firmware update.";

            return result;
        }

        private bool IsTimingWithinTolerance(int actual, int expected, double tolerance)
        {
            if (expected == 0) return true; // Ignore jitter on zero-delay steps
            double diff = Math.Abs(actual - expected);
            return diff <= expected * tolerance;
        }
    }
}
