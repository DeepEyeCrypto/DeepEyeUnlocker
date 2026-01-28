# DeepEyeUnlocker - Testing Guide

## 🛠 Overview

Testing DeepEyeUnlocker involves validating complex protocol handshakes (Sahara, Firehose, Odin) and partition management logic without requiring physical Android devices in the CI/CD pipeline.

## 🧪 Testing Strategy

We use **xUnit** and **Moq** for our testing suite.

### 1. Mocking External Interfaces

Since most operations interact with USB hardware, you MUST use mocked versions of our core device interfaces:

- `IAdbClient`: For simulating ADB shell commands and file transfers.
- `IUsbDevice`: For simulating low-level USB reads/writes.
- `IDeviceManager`: For simulating device detection events.

### 2. Logic-Only Modules (High Priority)

The following modules contain the "Brains" of the app and should have 80%+ coverage:

- **FlashManager**: Logic for parsing rawprogram.xml and scatter files.
- **DeviceInfoManager**: Logic for interpreting Android build properties and security strings.
- **BootloaderOperation**: Decision logic for selecting the correct unlock strategy based on Brand/Chipset.
- **LocalizationManager**: Ensuring correct string retrieval for English/Hindi.

### 3. Hardware-Dependent Modules (Low Priority / Mocked)

- **Engines (Qualcomm/MTK/Samsung)**: Focus on testing that the correct protocol packets are generated.
- **DriverInstaller**: Verify that INF file paths are calculated correctly based on OS version.

## 🚀 How to Run Tests

From the project root, execute:

```powershell
dotnet test tests/DeepEyeUnlocker.Tests.csproj
```

## 📝 TestSprite Instructions

When generating tests, TestSprite should:

1. Refer to `docs/PRD_DeepEyeUnlocker.md` for business rules.
2. Refer to `docs/ARCHITECTURE_OVERVIEW.md` for module mapping.
3. Automatically generate mocks for any class in the `DeepEyeUnlocker.Protocols` namespace.
