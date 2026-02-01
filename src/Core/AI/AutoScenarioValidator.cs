using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.HIL;

namespace DeepEyeUnlocker.Core.AI
{
    public class AutoScenarioValidator
    {
        private readonly SimulationValidator _validator = new();

        public Task<AiValidationResult> ValidateAsync(ProtocolScenario actual, ProtocolScenario expected)
        {
            var result = _validator.ValidateAgainstGolden(actual, expected, new ValidationOptions());
            
            return Task.FromResult(new AiValidationResult
            {
                Success = result.IsMatch,
                Similarity = result.SimilarityScore,
                ErrorLog = string.Join("\n", result.Differences.Select(d => $"{d.DifferenceType} at step {d.StepIndex}"))
            });
        }
    }

    public class AiValidationResult
    {
        public bool Success { get; set; }
        public double Similarity { get; set; }
        public string ErrorLog { get; set; } = string.Empty;
    }

    public class SelfHealingLoop
    {
        private readonly ILlmClient _llm;
        private readonly ScenarioSynthesizer _synthesizer;

        public SelfHealingLoop(ILlmClient llm, ScenarioSynthesizer synthesizer)
        {
            _llm = llm;
            _synthesizer = synthesizer;
        }

        public async Task<ProtocolScenario> RefineAsync(ProtocolScenario initial, AiValidationResult failure)
        {
            string prompt = $"The scenario failed validation with score {failure.Similarity:P}.\nErrors:\n{failure.ErrorLog}\nPlease correct the structure.";
            var response = await _llm.AnalyzeAsync(prompt);
            
            if (response.Analysis != null)
                return _synthesizer.GenerateScenario(response.Analysis, new List<Infrastructure.HIL.UsbPacket>()); // Simplified

            return initial;
        }
    }
}
