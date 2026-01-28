# DeepEyeUnlocker - Technical Walkthrough

Welcome to the **DeepEyeUnlocker** codebase. This guide explains how the project is organized and how the core "unlock" logic works across different chipsets.

## 📁 System Architecture

The project follows a modular "Engine -> Protocol -> Operation" architecture:

1. **Core (`src/Core`)**:
    * `DeviceManager.cs`: The heartbeat of the app. It uses `LibUsbDotNet` to scan the USB bus and identify devices in "Unlocking Modes" (EDL, BROM, Download).
    * `OperationFactory.cs`: Dynamically decides whether to use the Qualcomm, MTK, or Samsung engine based on the connected device.

2. **Protocols (`src/Protocols`)**:
    * **Qualcomm**: Uses `Sahara` for the initial handshake and `Firehose` (XML-based) for partition reading/writing.
    * **MediaTek**: Uses a `Preloader` handshake to gain low-level access.
    * **Samsung**: Uses the `ODIN/Loke` protocol for firmware flashing.

3. **Operations (`src/Operations`)**:
    * User-facing features like `FRP Bypass`, `Factory Reset`, and `Pattern Clear` are implemented as independent operation classes.
    * Each operation reports real-time progress to the UI.

## 🛠 Adding a New Device

To add support for a new device:

1. Add the device's **VID/PID** to `DeviceManager.cs` if it uses a non-standard mode.
2. Update the **Device Matrix** in `docs/SUPPORTED_DEVICES.md`.
3. If it's a Qualcomm device, ensure the correct **Firehose Programmer (.mbn)** is available.

## 🎨 UI & Localization

* The UI is built with **WinForms .NET 6.0**.
* **Themes**: `BrandColors.cs` and `DarkTheme.cs` handle the premium dark aesthetic.
* **Languages**: `LocalizationManager.cs` supports English and Hindi. Toggle languages at runtime via the header dropdown.

## 🚀 CI/CD Pipeline

* **Build**: Automated build/test on every PR via GitHub Actions.
* **Release**: Push a tag (e.g., `v1.0.0`) to automatically generate and upload a portable Windows binary to GitHub Releases.

---
*For more details, check `docs/ARCHITECTURE.md`.*
