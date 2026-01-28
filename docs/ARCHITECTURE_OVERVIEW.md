# Architecture Overview - DeepEyeUnlocker

## 1. System Layers

DeepEyeUnlocker follows a strict layered architecture to decouple UI from hardware-specific protocols.

### 1.1 UI Layer (`src/UI`)

- **MainForm**: The central hub that hosts independent operational panels.
- **Panels**: Self-contained units (`FlashCenterPanel`, `CloakCenterPanel`, etc.) that interact with the Core via `DeviceContext`.
- **Themes**: Visual branding (Teal & Gold) and custom WinForms controls.

### 1.2 Operations Layer (`src/Operations`)

- **Operation Base**: Abstract class providing `ExecuteAsync` and progress reporting.
- **Managers**: Specialized logic for complex flows (e.g., `FlashManager` for parsing manifests, `DeviceInfoManager` for diagnostics).
- **Automation Targets**: These classes are prime candidates for Unit Testing.

### 1.3 Core Domain (`src/Core`)

- **Models**: DTOs for `DeviceContext`, `ProgressUpdate`, and `FirmwareManifest`.
- **Interfaces**: Abstractions for ADB, Fastboot, and USB communication (essential for Moq).

### 1.4 Protocol Engines (`src/Protocols`)

- **Engines**: Low-level communication logic.
  - `QualcommEngine`: Manages Sahara and Firehose XML protocols.
  - `MtkEngine`: Manages BROM handshakes and DA payloads.
  - `SamsungEngine`: Manages Odin/Loke PIT and chunked transfers.
  - `AdbEngine`: High-level wrapper for CLI-based Android interactions.

### 1.5 Infrastructure (`src/Infrastructure`)

- **USB Stack**: Uses `LibUsbDotNet` for device handle management.
- **Logging**: Unified logging that broadcasts to both File and UI Console.

## 2. Key Interfaces for Mocking

To test without physical phones, TestSprite should mock:

1. `DeepEyeUnlocker.Core.IAdbClient`
2. `DeepEyeUnlocker.Core.IUsbDevice`
3. `DeepEyeUnlocker.Infrastructure.IDriverManager`

## 3. Data Flow

`USB Event` -> `DeviceManager` -> `DeviceContext` -> `MainForm` -> `Panel.SetDevice()` -> `Operation.Execute()`
