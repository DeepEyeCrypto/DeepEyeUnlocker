# TestSprite Project Overview: DeepEyeUnlocker

## Stack

- **Languages**: C# (.NET 8.0), JavaScript (Node.js)
- **Frontend**: WPF (WPF-UI / Modern UI)
- **Backend/Core**: .NET Library (`DeepEyeUnlocker.Core`), Node.js Express (`backend/`)
- **Infrastructure**: WiX Toolset (MSI), GitHub Actions (CI/CD)

## Entrypoints

- **UI Application**: `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj` (Target: `DeepEyeUnlocker.exe`)
- **Backend**: `backend/server.js` (if applicable, Node.js entrypoint)
- **Tests**: `dotnet test DeepEyeUnlocker.sln`

## Features

- **MTK Protocol**: MediaTek BootROM/Preloader interaction and exploit engine.
- **Qualcomm Protocol**: Sahara/Firehose protocol for EDL mode operations.
- **Samsung Protocol**: Odin/Loke protocol for flashing and unlocking.
- **Operations**: FRP Bypass, Network Unlock, Partition Backup, Flash Center.
- **Diagnostics**: Real-time USB device detection and health audit.
- **Telemetry**: Sentry-based crash reporting bridge (recently added).

## Risk Areas

- **Firmware Flashing**: High risk of device bricking if handshake fails.
- **Partition Backup**: Memory-intensive; potential for `OutOfMemoryException` on large partitions.
- **Expert Mode**: Bypassing security checks; needs strict validation.
- **Driver Interaction**: dependency on `LibUsbDotNet` handles; potential for deadlocks or resource leaks.
