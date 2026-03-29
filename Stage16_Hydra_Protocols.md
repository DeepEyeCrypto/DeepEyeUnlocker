# Stage 16 — Hydra Dongle: All Protocols

## Implemented Components

### Android/Kotlin
- `app/src/main/kotlin/com/deepeye/otg/usb/DeviceMatrix.kt`
  - `HydraProtocol` enum
  - `HYDRA_DEVICE_MATRIX`
  - `detectHydraProtocol(vendorId, productId)`
- `app/src/main/kotlin/com/deepeye/otg/util/UsbExtensions.kt`
  - `UsbDevice.detectHydraProtocol()`
- USB executors:
  - `app/src/main/kotlin/com/deepeye/otg/usb/OdinExecutor.kt`
  - `app/src/main/kotlin/com/deepeye/otg/usb/MtkBromExecutor.kt`
  - `app/src/main/kotlin/com/deepeye/otg/usb/MtkMetaExecutor.kt`
  - `app/src/main/kotlin/com/deepeye/otg/usb/SpdFdlExecutor.kt`
  - `app/src/main/kotlin/com/deepeye/otg/usb/LgLafExecutor.kt`
- Routing/state:
  - `app/src/main/kotlin/com/deepeye/otg/usecase/HydraProtocolUseCase.kt`
  - `app/src/main/kotlin/com/deepeye/otg/viewmodel/HydraViewModel.kt`

### Tauri/Rust
- `src-tauri/src/commands/hydra.rs`
  - `hydra_detect_protocol`
  - `hydra_run_mtk_meta`
  - `hydra_samsung_frp_bypass`
- Registered in:
  - `src-tauri/src/commands/mod.rs`
  - `src-tauri/src/lib.rs`

## Stage 3 Delay Rules (Hydra Addendum)

### Samsung ODIN
- `delay(20)` between chunk TX and ACK read
- `delay(500)` after session end for reconnect/reboot window

### MTK BROM
- `delay(50)` handshake stabilization
- `delay(150)` after DA upload
- `delay(2000)` DA readiness window

### SPD FDL
- `delay(100)` FDL1→FDL2 re-enumeration
- `delay(50)` frame/ACK gap

### LG LAF
- `delay(20)` packet/ACK gap
- `delay(500)` close/reconnect window

## Stage 9 Audit Additions

### PASS 8 — MTK BROM raw access
```bash
grep -rn "0x0E8D\|MTK_BROM\|brom" app/src/main/kotlin/ \
  | grep -v "DeviceMatrix\|UsbExtensions"
```

Fix rule:
- All MTK detection through `DeviceMatrix`.
- USB transport through `bulkOut` / `bulkIn` helpers.

### PASS 9 — Odin raw bulkTransfer
```bash
grep -rn "bulkTransfer" app/src/main/kotlin/ \
  | grep -i "odin\|samsung\|0x04E8"
```

Fix rule:
- Use `connection.bulkOut(...)` and `connection.bulkIn(...)` only.

## Stage 10 Never-Do Additions

- Never send Odin packets before `LOKE` handshake confirmation
- Never send MTK DA before BROM magic response (`0x5F 0xF5 0xAF`)
- Never skip SPD FDL1→FDL2 re-enumeration wait
- Never build SPD HDLC frame without CRC16
- Never hardcode MTK META COM port (`/dev/ttyUSB*` is dynamic)
- Never flash Odin chunks without per-chunk ACK verification
- Never execute Hydra operations without dongle presence/license check
- Never mix Samsung Odin flow with Qualcomm EDL flow

