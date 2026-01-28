# DeepEyeUnlocker Test Plan (AI-Targeted)

This document defines the critical testing paths for **TestSprite** and other automated testing engines.

## 🎯 Primary Objectives

1. **Protocol Stability**: Verify handshake reliability across Qualcomm EDL, MTK BROM, and Samsung Download modes.
2. **Detection Accuracy**: Ensure the **AI Discovery Engine** correctly maps Vid/Pid and provides appropriate protocol suggestions.
3. **Stealth Effectiveness**: Validate that **Cloak Center** actions (Root Hide/Dev Hide) correctly modify system properties.
4. **Installer Safety**: Confirm the **Driver Center** correctly utilizes `pnputil` without affecting non-mobile system drivers.

---

## 🧪 Critical Test Cases

### 1. Device Discovery & AI Logic

- **TC_01**: Connect a device with `VID_05C6&PID_9008`. Expected: Matched as "Qualcomm EDL" with 1.0 confidence.
- **TC_02**: Connect a device with a known MTK Preloader PID. Expected: Suggested protocol "MTK_DA".
- **TC_03**: Heuristic check. Connect a device with "MediaTek" in the descriptor. Expected: Confidence 0.7 fallback.

### 2. Cloak Subsystems (Root/Dev)

- **TC_04**: Toggle Zygisk via MagiskController. Verify `magisk --zygisk` output change.
- **TC_05**: Enable "Ghost Mode". Verify `adb_enabled` setting is set to `0`.
- **TC_06**: Mock Shamiko installation. Verify `ShamikoController` detects presence of `/data/adb/modules/zygisk_shamiko/`.

### 3. Driver Installation

- **TC_07**: Simulate missing driver (Code 28). Verify `DeviceSignatureDetector` flags `IsProblemDevice: true`.
- **TC_08**: Install ADB Driver profile. Verify `pnputil /add-driver` is called with the correct `.inf` path.

### 4. Protocol Handshakes (Mocked)

- **TC_09**: Qualcomm Sahara Hello. Send bitmask, expect valid version negotiation.
- **TC_10**: MediaTek BROM Handshake. Send `0xA1`, expect `0x5A` ACK.

### 5. Partition & Flash Operations

- **TC_11**: GPT Table Parsing. Read 34 sectors from mocked device. Verify partition names and LBA matches.
- **TC_12**: Streaming Partition Backup. Stream 1GB from device to file. Verify memory usage stays below 50MB.
- **TC_13**: Qualcomm XML Parsing. Provide rawprogram0.xml. Verify FlashManager calculates correct sector offsets for all images.
- **TC_14**: Multi-Partition Flashing. Simulate flashing 5 partitions. Verify order (OrderBy physical_partition).

---

## 🛠 Testing Tools & Infrastructure

- **Framework**: xUnit + Moq (for mocking USB streams).
- **Mocking**: `Moq` for `IAdbClient` and `IUsbDriverInstaller`.
- **Hardware Simulation**: Use `LibUsbDotNet` loopback or virtual USB devices for protocol testing where possible.

## 📈 Success Metrics

- **Logic Coverage**: 90%+ on `Core` and `Drivers` namespaces.
- **Handshake Success Rate**: 100% on simulated packets.
- **Signature Detection**: No false positives on standard Windows system USB hubs.
