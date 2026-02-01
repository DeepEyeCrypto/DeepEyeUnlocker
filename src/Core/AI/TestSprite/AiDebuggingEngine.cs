using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.AI.TestSprite
{
    public class AiDebuggingEngine
    {
        private readonly ILlmClient _llm;

        public AiDebuggingEngine(ILlmClient llm)
        {
            _llm = llm;
        }

        public async Task<string> AnalyzeFailureAsync(string testName, string error, string stackTrace, string contextCode)
        {
            string prompt = $@"
Test failed: {testName}
Error: {error}

Stack trace:
{stackTrace}

Relevant code:
{contextCode}

Analyze:
1. Root cause category (protocol bug, race condition, logic error, config issue)
2. Specific line or condition causing failure
3. Suggested fix with code snippet
4. Prevention recommendation (test to add, validation to strengthen)

Return structured analysis.
";
            var response = await _llm.AnalyzeAsync(prompt);
            return response.RawContent;
        }
    }
}
