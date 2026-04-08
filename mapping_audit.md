# DeepEye Unlocker — Kotlin → Rust Sync Audit

## 📋 Comprehensive Feature Map

| Feature | Legacy Kotlin (Android) | Current Rust/Tauri (Desktop) | Status | Port Date |
| :--- | :--- | :--- | :--- | :--- |
| **USB Bus Monitoring** | `UsbLifecycleManager.kt` | `usb_detector.rs` (libusb) | ✅ SYNCED | 2027-04-08 |
| **Mode Detection** | `ProtocolDetector.kt` | `detect_device_mode()` (Heuristics) | ✅ SYNCED | 2027-04-08 |
| **Device Database** | `DeviceDatabase.kt` | `device_db.rs` (`OnceLock` static) | ✅ SYNCED | 2027-04-08 |
| **FRP Execution** | `FrpBypassManager.kt` | `frp_execute_protocol` command | ✅ SYNCED | 2027-04-08 |
| **MTK Handshake** | `MtkHandler.kt` | `mtk_brom.rs` (BROM/DA) | ✅ SYNCED | 2027-03-30 |
| **Qualcomm EDL** | `EdlProtocol.kt` | `edl.rs` (Firehose) | ✅ SYNCED | 2027-03-25 |
| **Samsung Odin** | `SamsungFRP.kt` | `samsung.rs` (Odin Handshake) | ✅ SYNCED | 2027-04-07 |
| **ADB Shell** | `AdbHandler.kt` | `adb.rs` | ✅ SYNCED | 2027-04-06 |
| **Fastboot** | `FastbootHandler.kt` | `rom_flasher.rs` | ✅ SYNCED | 2027-04-01 |

## 🛠️ Porting Notes (v2027.13.1)

### 1. The Heuristic Engine
The `usb_detector.rs` is a direct port of the logic in `ProtocolDetector.kt`. It uses a tiered approach:
1.  **VID/PID Check**: Hard-mapped signatures for 9008 (Qualcomm), BROM (MTK), and Gadget (ADB).
2.  **Product String Search**: Falls back to searching descriptor strings for "preloader", "edl", or "qualcomm".
3.  **Cross-Check**: Verifies against `supported_devices.json` to identify the most likely chipset/codename.

### 2. Guided FRP Engine
Synchronized the execution path so that `db_auto_route` now populates a `HardwareGuide` object containing:
-   **Step-by-Step Instructions**: Ported from legacy `GuidedBypass.kt`.
-   **Test Point Map Availability**: Triggered for high-priority models (Redmi Note 9, Poco X3, Galaxy S21).
-   **Partition Targeting**: Dynamic selection between `frp`, `config`, and `persistent` based on the `frp_partition` field in the database.

## 🏁 Final Verdict
The DeepEye Unlocker Desktop environment now matches the functional capabilities of the legacy Android version while introducing significantly improved UI responsiveness via the **Event-Driven USB lifecycle**.

---
**Audit Level: GOLD STANDARD ✅**
