# Maintainer Checklist: Protocol & Simulation

This checklist must be used when reviewing pull requests that modify protocol engines (`src/Protocols/`) or simulation fixtures (`scenarios/`).

## 🧪 Simulation Integrity

- [ ] **All Scenarios Pass**: Run `dotnet test --filter "Category=Protocol"` locally.
- [ ] **FRP Policies Respected**: Run `dotnet test --filter "Category=FrpPolicy"` locally.
- [ ] **Schema Validation**: Run `dotnet test --filter "ScenarioSchemaValidationTests"`. No manual JSON edits should bypass the schema.

## 🛡️ FRP & Security Guardrails

- [ ] **Policy Parity**: If a protocol engine response is modified, ensure the corresponding `frp/` scenarios are updated to match real-world enforcement.
- [ ] **Error Codes**: Verify that internal engine result mapping (e.g., `AccessDenied`) correctly reflects hardware-specific status codes (e.g., MTK `0xC0020007`).

## 🚢 CI & Cross-Platform

- [ ] **Matrix Green**: Verification job must pass on Windows, Linux, and macOS.
- [ ] **Artifacts**: Check `.trx` logs if a platform-specific failure occurs.

## 📝 Documentation

- [ ] **Task List**: stage-by-stage progress reflected in `task.md`.
- [ ] **Contributor Guide**: New protocol behaviors are added to `SCENARIOS_TODO.md` if not implemented yet.
