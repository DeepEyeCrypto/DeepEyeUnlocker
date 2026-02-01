using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.AI.TestSprite.Healers
{
    public class ScenarioSchemaHealer
    {
        private readonly ILlmClient _llm;

        public ScenarioSchemaHealer(ILlmClient llm)
        {
            _llm = llm;
        }

        public async Task<string> HealScenarioAsync(string outdatedJson, string errorMessage, string newSchema)
        {
            string prompt = $@"
The following protocol scenario JSON failed validation against a new schema.
Error: {errorMessage}
New Schema: {newSchema}
Outdated Scenario:
{outdatedJson}

Please correct the JSON to comply with the new schema. Return ONLY the corrected JSON.
";
            var response = await _llm.AnalyzeAsync(prompt);
            return response.RawContent;
        }
    }

    public class HandlerSignatureHealer
    {
        private readonly ILlmClient _llm;

        public HandlerSignatureHealer(ILlmClient llm)
        {
            _llm = llm;
        }

        public async Task<string> HealHandlerTestAsync(string testCode, string oldSignature, string newSignature)
        {
            string prompt = $@"
Update this test to match the new method signature:
{oldSignature} -> {newSignature}

Test code:
{testCode}

Return ONLY the updated test method.
";
            var response = await _llm.AnalyzeAsync(prompt);
            return response.RawContent;
        }
    }
}
