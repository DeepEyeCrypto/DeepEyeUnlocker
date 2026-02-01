# DeepEyeUnlocker v3.1.4 (AI Edition) - Release Notes

## [3.1.4] - 2026-02-01

This release introduces groundbreaking AI capabilities to DeepEyeUnlocker, transforming it into the world's first autonomous mobile protocol servicing tool.

### Added: ProtocolAutoAnalyzer (PAA)

- **Autonomous Protocol Discovery**: AI-powered engine that learns unknown USB protocols from raw captures.
- **PcapFeatureExtractor**: Statistical analysis (entropy, timing, clustering) to guide LLM inference.
- **ScenarioSynthesizer**: Automatic generation of high-fidelity Scenario DSL from LLM analysis.
- **Self-Healing Loop**: Autonomous error correction using the HIL Bridge simulation.
- **CLI `analyze`**: New command to turn pcap captures into working protocol plugins.

# Release Notes - DeepEyeUnlocker v3.1.5 🚀

**Date:** 2026-02-01
**Stability:** Stable

## 🛠️ Infrastructure Updates

- **CI/CD Fallback**: Disabled autonomous TestSprite CLI execution due to package unavailability.
- **Testing**: Switched CI workflow to use `dotnet test` for reliable validation.

## ✨ Improvements

- **Security Check**: Verified no missing binaries in core logic.
- **Workflow**: Ensure build passes without phantom dependencies.

- **Data Sanitization**: Enhanced PAA redaction to ensure sensitive device identifiers are never sent to LLM providers.
- **Safety Interlock**: Integrated new PAA-generated scenarios into the safety pre-flight check system.

---
*DeepEyeUnlocker Community - Power to the People.*
