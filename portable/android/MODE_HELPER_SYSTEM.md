# DeepEye Mode Helper System (v5.2.4)

## Overview

When a device is connected but the protocol detector returns `UNKNOWN` (e.g., device is in MTP, Charging, or generic Android interface), DeepEye now provides **smart, brand-aware instructions** to guide the technician into the correct service mode (EDL, BROM, etc.).

## Architecture

- **`ModeHelper.kt`**: Central logic for generating instructions based on Brand, Model, and SoC.
- **`OtgActivity.kt`**: Detects `UNKNOWN` protocol, queries `ModeHelper`, and displays a rich dialog.

## Supported Guidance Logic

### Xiaomi / Redmi / POCO

- **Qualcomm Models**: Instructs user to use **EDL (9008)** mode via "Vol+ & Vol-".
- **MTK Models**: Instructs user to use **BROM** mode via "Vol Up" (or both).
- **Safety**: Warns about test-points for newer security patches.

### Oppo / Realme / OnePlus

- **MTK**: Instructs **BROM** entry (Vol+ & Vol-).
- **Qualcomm**: Instructs **EDL** (often requires test-point on newer models).

### Samsung

- **MTK**: Warns that **Test-Point** is almost always required for BROM.
- **Exynos/Qualcomm**: Guides to **Download Mode (Odin)** via buttons.

### Vivo

- **MTK**: Guides to **BROM** (Vol Up).
- **Qualcomm**: Guides to **EDL**.

## User Experience

Instead of a generic "Handshake Failed" error, the user sees:

**Title**: Wrong USB Mode
**Message**:

```
Device connected in UNKNOWN mode (likely MTP/Charging).

Required Mode: EDL (9008)
Chipset: SNAPDRAGON 8 Gen 2

Instructions:
1. Power off the device completely.
2. Hold BOTH Volume Up + Volume Down.
3. Connect USB cable while holding buttons.
4. Screen should remain black.

Alternatives:
- If Fastboot is available: 'fastboot oem edl'
- Use DeepEye EDL Cable if buttons fail.

NOTE: Test-point required if software EDL features are blocked.
```

## Testing

1. Connect a phone in normal OS mode (MTP).
2. Select a specific model in the UI (e.g., "Xiaomi Mi 11").
3. Observe the dialog with specific EDL instructions.
4. Reboot phone to EDL mode.
5. observe "Connected: Handshake OK" and dialog disappears.
