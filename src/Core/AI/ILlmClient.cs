using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.AI
{
    public interface ILlmClient
    {
        Task<LlmResponse> AnalyzeAsync(string prompt);
        bool SupportsStructuredOutput { get; }
    }

    public class LlmResponse
    {
        public string RawContent { get; set; } = string.Empty;
        public LlmAnalysis? Analysis { get; set; }
        public double Confidence { get; set; }
    }

    public class LlmAnalysis
    {
        public string ProtocolType { get; set; } = "Unknown";
        public string Summary { get; set; } = string.Empty;
        public List<InferredStep> Steps { get; set; } = new();
        public InferredStateMachine StateMachine { get; set; } = new();
    }

    public class InferredStep
    {
        public int StepIndex { get; set; }
        public string Direction { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public InferredStructure? Structure { get; set; }
    }

    public class InferredStructure
    {
        public int HeaderSize { get; set; }
        public int CommandIdOffset { get; set; }
        public int LengthOffset { get; set; }
        public string PayloadEncoding { get; set; } = "Hex";
    }

    public class InferredStateMachine
    {
        public string InitialState { get; set; } = "Idle";
        public string FinalState { get; set; } = "Finished";
        public List<InferredTransition> Transitions { get; set; } = new();
    }

    public class InferredTransition
    {
        public string From { get; set; } = string.Empty;
        public string Trigger { get; set; } = string.Empty;
        public string To { get; set; } = string.Empty;
    }
}
