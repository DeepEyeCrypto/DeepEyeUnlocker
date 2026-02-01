# FRP Policy Testing Overview

This document defines the scope for simulating **FRP (Factory Reset Protection)** policy enforcement within the Protocol Simulation Engine.

## Objective

To verify that the DeepEyeUnlocker engine correctly identifies, respects, and reports device security policies when encountering FRP-enforced states, without requiring real hardware or exploits.

## Supported Operations (Simulation Scope)

### 1. Qualcomm Firehose (EDL)

- **Identify**: Read basic device parameters (Brand, Model, FRP Status).
- **Partition Read**: Simulating successful reads from non-protected partitions (e.g., `userdata`) vs. blocked reads from protected partitions.
- **Partition Write/Erase**: Simulating failure when writing to `config`, `persist`, or `frp` partitions.
- **Error Handling**: Mapping XML `NAK` responses to internal `AccessDenied` or `RequiresAuth` states.

### 2. MediaTek (BROM/DA)

- **DA Handshake**: Detecting if a device requires SLA (Secure Lock Authentication) or DAA (Download Agent Authentication).
- **Partition Enumeration**: Identifying partitions marked as "Protected" in the partition table.
- **Read/Write Blockage**: Simulating DA status codes for security violations (e.g., `0xC0020007`).

## Expected Behaviors

| Operation | FRP Unlocked | FRP Locked |
| ----------- | -------------- | ------------ |
| Device Info Read | FULL | BASIC ONLY |
| Read `userdata` | ALLOW | ALLOW (usually) |
| Read `config`/`frp` | ALLOW | DENY / NAK |
| Write `config`/`frp` | ALLOW | DENY / NAK |
| Erase `userdata` | ALLOW | ALLOW |
| Erase `persist` | ALLOW | DENY / NAK |

## Policy Violation Error Codes (Simulated)

### Firehose NAK Reasons

- `Protected Partition`: Specific error string in XML response.
- `Requires Authentication`: Indicating a higher security state.

### MTK DA Status Codes

- `SLA_REQUIRED` (`0xC0020007`): Attempting sensitive operation without signature.
- `DA_AUTH_FAILURE`: Failed DAA handshake.

## Test Scenarios to Implement

1. **Firehose: Locked Read Info Only**: Verify only basic metadata is retrieved.
2. **Firehose: Write Denied**: Verify `NAK` on `config` partition write.
3. **Firehose: Partial Access**: Success on `userdata`, fail on `persist`.
4. **MTK: Partition Protected**: Verify DA reports "Protected" status for `protect_f`.
5. **MTK: Policy After Wipe**: Ensure wipe doesn't accidentally clear FRP flags unless authenticated.
6. **MTK: SLA Required**: Verify correct handling of authentication-locked operations.

---

## How to Extend FRP Policy Tests

Adding a new FRP-related behavior check is a 3-step process:

1. **Add Scenario JSON**: Drop a new JSON file in `scenarios/frp/`. Use the `frp_context` and `expectations` fields defined in the schema.
2. **Add Test Case**: Add an `[InlineData]` entry in `FrpPolicyIntegrationTests.cs` referencing your new scenario and the expected `EngineStatus`.
3. **Verify**: Run `dotnet test --filter "Category=FrpPolicy"` to validate your scenario against the protocol engine.

The Schema Validation guardrail in CI will automatically verify your JSON structure.
