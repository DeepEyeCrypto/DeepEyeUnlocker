# 🛡️ Testing DeepEyeUnlocker with TestSprite

This project uses **TestSprite** for autonomous quality assurance, providing a continuous feedback loop between testing and bug-fixing.

## 🚀 Running Tests

### 1. Local .NET Tests

Run the core logic validations (requires Windows for full desktop support).

```bash
dotnet test tests/DeepEyeUnlocker.Tests.csproj
```

### 2. Backend API Tests

Run the Node.js API test suite.

```bash
cd backend
npm install
npm test
```

### 3. Triggering a TestSprite Cloud Run

If you have the `testsprite` CLI installed:

```bash
testsprite run --cloud --auto-fix
```

### 4. Protocol Simulation Engine

Run hardware-independent protocol replays on any OS:

```bash
dotnet test tests/DeepEyeUnlocker.Tests.csproj --filter "ScenarioReplayTests"
```

## 📊 Interpreting Reports

- **`testsprite-report-INITIAL.md`**: The first baseline report.
- **GitHub Artifacts**: Download `.trx` files from the "Protocol Verification" workflow for cross-platform simulation results.

## 🛠 Adding New Scenarios

To add a new flow for TestSprite to cover, simply add a description to `testsprite-plan.md` or `testsprite-protocol-scenarios.md`.

TestSprite can:

- **Analyze**: Read `protocol_simulation_overview.md` to understand the architecture.
- **Generate**: Create new JSON scenarios automatically for complex protocol edge cases.
- **Verify**: Run the replayer to confirm the host engine handles the new scenario correctly.

---
*Driven by TestSprite Autonomous Agent.*
