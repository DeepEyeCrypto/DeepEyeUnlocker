# Contributing to DeepEyeUnlocker Protocols

We use a **Protocol Simulation Engine** to test device communication without hardware. All protocol changes must be accompanied by matching simulation scenarios.

## Adding a New Protocol Scenario

Follow these 5 steps to add or update a protocol verification case:

### Step 1: Create the Scenario JSON

Add a new JSON file under `scenarios/<protocol>/` (e.g., `scenarios/sahara/my_new_case.json`).

**Example Template:**

```json
{
  "name": "sahara_my_case",
  "protocol": "sahara",
  "description": "Short description of what this tests.",
  "steps": [
    { "direction": "device_to_host", "label": "in", "data_hex": "..." },
    { "direction": "host_to_device", "label": "out", "data_hex": "..." }
  ],
  "expectations": { "max_duration_ms": 1000 }
}
```

### Step 2: Validate the Schema

Run the schema validation test to ensure your JSON is correctly formatted:

```bash
dotnet test --filter "ScenarioSchemaValidationTests"
```

All scenarios must pass this test before they are accepted.

### Step 3: Add the Replay Test

Add a new `[Fact]` to `tests/Protocols/ScenarioReplayTests.cs` referencing your JSON:

```csharp
[Fact]
public async Task My_New_Protocol_Scenario()
{
    var scenarioPath = Path.Combine(_scenarioDir, "sahara", "my_new_case.json");
    var scenario = ScenarioLoader.Load(scenarioPath);
    using (var usb = new ScenarioUsbDevice(scenario))
    {
        var protocol = new SaharaProtocol(usb);
        // Execute your protocol logic here
        bool success = await protocol.DoSomethingAsync();
        Assert.True(success);
    }
}
```

### Step 4: Run Locally

Verify the simulation passes on your machine:

```bash
dotnet test --filter "ScenarioReplayTests"
```

### Step 5: Submit PR

Ensure your PR includes:

- The `scenarios/*.json` fixture.
- The updated `ScenarioReplayTests.cs`.
- Any protocol engine changes.

---

## Adding FRP Scenarios

If you are adding a scenario to test FRP (Factory Reset Protection) policy behaviors:

1. **Category**: Use the `scenarios/frp/` directory.
2. **Schema**: Ensure you use the `frp_context` field to describe the simulated device state.
3. **Tests**: Add your case to `tests/Protocols/FrpPolicyIntegrationTests.cs` using the `[InlineData]` pattern.
4. **Verification**: Run `dotnet test --filter "Category=FrpPolicy"`.

---

## Roadmap & TODO

See [SCENARIOS_TODO.md](SCENARIOS_TODO.md) for a list of prioritized protocol behaviors that need simulation coverage.
