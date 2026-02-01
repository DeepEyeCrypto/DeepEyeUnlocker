using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.AI
{
    public class MockLlmClient : ILlmClient
    {
        public bool SupportsStructuredOutput => true;

        public async Task<LlmResponse> AnalyzeAsync(string prompt)
        {
            await Task.Delay(1000); // Simulate network
            return new LlmResponse
            {
                Confidence = 0.95,
                Analysis = new LlmAnalysis
                {
                    ProtocolType = "Qualcomm Sahara",
                    Summary = "Identified as standard Sahara Hello handshake.",
                    Steps = new List<InferredStep>
                    {
                        new InferredStep { StepIndex = 0, Direction = "DeviceToHost", Description = "Sahara Hello" },
                        new InferredStep { StepIndex = 1, Direction = "HostToDevice", Description = "Sahara Hello Response" }
                    },
                    StateMachine = new InferredStateMachine
                    {
                        InitialState = "Hello",
                        Transitions = new List<InferredTransition>
                        {
                            new InferredTransition { From = "Idle", Trigger = "DeviceConnected", To = "Hello" }
                        }
                    }
                }
            };
        }
    }
}
