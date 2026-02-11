# DeepEye Protocol Detector (v5.2.3)

## Problem

Previously, DeepEye assumed the protocol based purely on VID/PID.

- Connected MTK device → Native Core tried "Generic" init → **Handshake Failed**.
- Connected generic Qualcomm device → Native Core tried "Generic" init → **Handshake Failed or Timeout**.
- Connected standard Phone (MTP) → Native Core tried to talk to `/data` → **Error**.

## Solution: Protocol Probing Layer

We implemented a **multi-protocol probe** (`ProtocolProbe.kt`) that runs *before* initializing the native core engine.

### How it works

1. **Open Device**: USB permission granted, file descriptor (FD) obtained.
2. **Probe**: `ProtocolProbe` inspects the USB interface and sends safe, non-destructive identifying commands.
   - **Fastboot**: Sends `getvar:version`. Expects `OKAY` or `FAIL`.
   - **MTK BROM**: Sends Sync Pattern (`0xA0 0x0A...`). Expects valid BROM/Preloader signature.
   - **Qualcomm EDL**: Sends Sahara Hello (`0x01...`). Expects Sahara Hello Response.
3. **Dispatch**:
   - IF `DetectedProtocol` is KNOWN (e.g. `MTK_BROM`):
     - Pass FD + Protocol ID to `NativeBridge.initCore`.
     - Update UI: "Connected: Handshake OK (MTK_BROM)".
   - IF `DetectedProtocol` is `UNKNOWN`:
     - **ABORT INITIALIZATION**.
     - Show Error: `"Wrong Mode: Please invoke EDL/BROM (Vol+ & Vol-)."`.

### Supported Modes

| Mode | Probe Logic | Detection ID |
|------|-------------|--------------|
| **Qualcomm EDL (9008)** | Sahara Hello (Cmd 0x01) | `DetectedProtocol.QUALCOMM_EDL` |
| **MTK BROM (BootROM)** | Sync Pattern + Handshake | `DetectedProtocol.MTK_BROM` |
| **MTK Preloader** | (Uses BROM pattern variant) | `DetectedProtocol.MTK_BROM` |
| **Fastboot** | `getvar:version` -> OKAY/FAIL | `DetectedProtocol.FASTBOOT` |

## Testing

1. **Connect MTK Device (BROM Mode)**:
   - Hold Vol+ & Vol-, insert USB.
   - Log should show: `Protocol Detected: MTK_BROM`.
   - UI: "Connected: Handshake OK (MTK_BROM)".

2. **Connect Normal Phone (MTP/Home Screen)**:
   - Insert USB.
   - Log should show: `Probe yielded UNKNOWN protocol`.
   - UI: "Wrong Mode: Please invoke EDL/BROM".
   - **Verify NO native crash or generic error.**

3. **Connect Fastboot Device**:
   - Boot into Fastboot.
   - Log: `Protocol Detected: FASTBOOT`.
   - UI: "Connected: Handshake OK (FASTBOOT)".

## Architecture

- `ProtocolProbe.kt`: Pure Java/Kotlin USB IO logic.
- `OtgActivity.kt`: Orchestrator. Blocks invalid modes.
- `UsbHostManager.kt`: Triggers probe on connection.
