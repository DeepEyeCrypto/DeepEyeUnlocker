# DeepEyeUnlocker v3.1.1 (AI Edition) - Release Notes

## [3.1.1] - 2026-02-01

This release introduces groundbreaking AI capabilities to DeepEyeUnlocker, transforming it into the world's first autonomous mobile protocol servicing tool.

### Added: ProtocolAutoAnalyzer (PAA)

- **Autonomous Protocol Discovery**: AI-powered engine that learns unknown USB protocols from raw captures.
- **PcapFeatureExtractor**: Statistical analysis (entropy, timing, clustering) to guide LLM inference.
- **ScenarioSynthesizer**: Automatic generation of high-fidelity Scenario DSL from LLM analysis.
- **Self-Healing Loop**: Autonomous error correction using the HIL Bridge simulation.
- **CLI `analyze`**: New command to turn pcap captures into working protocol plugins.

### Added: TestSprite AI Integration

- **MCP Server Bridge**: Standardized interface (Model Context Protocol) for AI agent interaction.
- **Autonomous Test Generation**: Natural language prompts to generate protocol, operation, and security tests.
- **Self-Healing Tests**: AI correction logic for Scenario DSL evolution and API refactors.
- **AiDebuggingEngine**: Failure analyst that identifies protocol root causes and suggests code fixes.
- **CI/CD Automation**: Automated TestSprite execution in GitHub Actions on every PR.

### Changed

- **CLI Standardization**: Uniform naming (`InputPath`, `OutputPath`) across all commands.
- **Stability**: Fixed various race conditions in the `OperationOrchestrator` revealed by AI stress testing.

### Security

- **Data Sanitization**: Enhanced PAA redaction to ensure sensitive device identifiers are never sent to LLM providers.
- **Safety Interlock**: Integrated new PAA-generated scenarios into the safety pre-flight check system.

---
*DeepEyeUnlocker Community - Power to the People.*
