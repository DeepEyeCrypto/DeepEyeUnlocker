# TestSprite: FRP Policy Scenario Generation

Use these prompts with TestSprite to generate new edge-case scenarios for the FRP Policy suite.

## Prompt 1: Firehose Partial Blockage

"Generate a Firehose scenario where the device allows a full read of the 'userdata' partition but returns a NAK with reason 'Protected' when the host attempts to write to the 'persist' or 'config' partitions. The scenario should include the correctly formatted XML responses."

## Prompt 2: MTK SLA Required

"Generate an MTK scenario where the BROM handshake succeeds, the DA (Download Agent) is loaded, but any attempt to read partitions results in a status code 0xC0020007 (SLA_REQUIRED) from the device. Use the MTK binary protocol markers."

## Prompt 3: FRP Wiped vs OEM Lock

"Generate a Firehose scenario representing a device where the 'frp' partition is clean (all zeros/ACK on read), but the 'config' partition is still locked, preventing firmware flashes. The engine should report 'LockedByOEM' policy."

### Validation Rule

All generated scenarios must pass the `ScenarioSchemaValidationTests` and be placed in `scenarios/frp/`.
