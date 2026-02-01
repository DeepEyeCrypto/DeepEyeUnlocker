# TestSprite: Protocol Scenario Requests

Use these requests to generate new simulations for the Protocol Simulation Engine.

## Auto-Generation Requests

- **Request 1: Sahara Mid-Handshake Restart**
  - "Generate a Sahara scenario where the device sends a HELLO packet, the host responds, but then the device sends another HELLO packet (simulating a crash/restart) before the host can send a command."

- **Request 2: Firehose Unsupported Command**
  - "Generate a Firehose scenario where the device returns an error response with `value=\"NAK\"` and `reason=\"Unsupported Command\"` after the host sends a `configure` XML block."

- **Request 3: MTK Timeout mid-sequence**
  - "Generate an MTK scenario where the host sends `0xA0`, device responds `0x5F`, then host sends `0x0A` but the device remains silent (timeout)."

---

## Instructions for TestSprite

1. Read `docs/protocol_simulation_overview.md` for architecture.
2. Read `docs/protocol_scenarios.schema.json` for the data format.
3. Create a new JSON file in `scenarios/<protocol>/`.
4. Add a test case in `tests/Protocols/ScenarioReplayTests.cs`.
5. Run `dotnet test` to verify.
