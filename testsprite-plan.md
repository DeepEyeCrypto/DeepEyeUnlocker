# TestSprite Test Plan: DeepEyeUnlocker

## Test Categories

### 1. Unit Tests (P0)

- **Target**: `DeepEyeUnlocker.Core`
- **Scope**: Protocol packet builders, address validators, VersionManager logic.
- **Tool**: xUnit

### 2. Integration / API Tests (P0)

- **Target**: `Protocols` and `Operations` modules.
- **Scope**: Simulated handshake flows for MTK/Qualcomm, OperationFactory logic.
- **Tool**: xUnit + Moq

### 3. UI / E2E Tests (P1)

- **Target**: `DeepEye.UI.Modern`
- **Scope**: Navigation flow, Device detection visual state, Expert mode toggle gating.
- **Tool**: Playwright for .NET / WinAppDriver

## Test Scenarios

### Feature: MTK Exploitation

- **Scenario 1**: Successful handshake with simulated device. (P0)
- **Scenario 2**: Handshake timeout handling. (P0)

### Feature: Qualcomm Sahara

- **Scenario 1**: Hello packet validation. (P0)
- **Scenario 2**: Invalid programmer data handling. (P1)

### Feature: UI Discovery

- **Scenario 1**: UI shows "Waiting for device" when no USB connected. (P1)
- **Scenario 2**: UI updates to "Connected" when device detected via `DeviceManager`. (P1)

## Out-of-Scope (Current Phase)

- Physical hardware testing (requires real USB connection).
- Full firmware flash simulation (requires massive binary blobs).
