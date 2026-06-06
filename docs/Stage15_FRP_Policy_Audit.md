# Stage 15 — FRP Bypass Protocol Policy & Audit Bundle

## Scope

This document defines FRP execution safety/policy gates and audit checks for:

- [`DeviceMatrix`](app/src/main/kotlin/com/deepeye/otg/usb/DeviceMatrix.kt)
- [`FrpUseCase`](app/src/main/kotlin/com/deepeye/otg/usecase/FrpUseCase.kt)
- [`EdlExecutor`](app/src/main/kotlin/com/deepeye/otg/usb/EdlExecutor.kt)
- [`AdbExecutor`](app/src/main/kotlin/com/deepeye/otg/usb/AdbExecutor.kt)
- [`FastbootExecutor`](app/src/main/kotlin/com/deepeye/otg/usb/FastbootExecutor.kt)
- [`apple_icloud_bypass`](src-tauri/src/commands/apple.rs)

---

## Mandatory Policy Rules

### 1) Matrix-first routing only

- FRP execution must resolve device strategy via [`lookupFrpProfile()`](app/src/main/kotlin/com/deepeye/otg/usb/DeviceMatrix.kt).
- No direct mode-specific bypass execution without a resolved profile.

### 2) No hardcoded FRP partition names

- Partition labels must come from [`FrpProfile.edlPartition`](app/src/main/kotlin/com/deepeye/otg/usb/DeviceMatrix.kt).
- If partition is missing for an EDL path, [`FrpUseCase.invoke()`](app/src/main/kotlin/com/deepeye/otg/usecase/FrpUseCase.kt) must emit error and stop.

### 3) Path-specific executors only

- Qualcomm EDL path → [`EdlExecutor.wipeFrpPartition()`](app/src/main/kotlin/com/deepeye/otg/usb/EdlExecutor.kt)
- ADB path → [`AdbExecutor.adbFrpUnlock()`](app/src/main/kotlin/com/deepeye/otg/usb/AdbExecutor.kt)
- Fastboot path → [`FastbootExecutor.fastbootFrpUnlock()`](app/src/main/kotlin/com/deepeye/otg/usb/FastbootExecutor.kt)

### 4) Explicit unsupported handling

- Unsupported model/path combinations must return [`FrpResult.Unsupported`](app/src/main/kotlin/com/deepeye/otg/model/FrpModels.kt) or [`FrpResult.Error`](app/src/main/kotlin/com/deepeye/otg/model/FrpModels.kt).
- No silent fallback to alternate paths.

### 5) Apple iCloud path isolation

- iCloud bypass entrypoint must remain isolated in Rust command layer via [`apple_icloud_bypass()`](src-tauri/src/commands/apple.rs).
- Android FRP router must not tunnel Apple flow through Qualcomm/ADB/Fastboot handlers.

---

## PASS 8 Audit — Hardcoded FRP Partition Guard

### Audit intent

Detect any hardcoded FRP partition literals outside profile matrix declarations.

### Baseline grep

```bash
grep -RIn '"frp"' app/src/main/kotlin/com/deepeye/otg/ \
  --include='*.kt'
```

### Compliance interpretation

- **Allowed:** declarations in profile matrices / test fixtures.
- **Blocked:** hardcoded literals inside usecases/executors (e.g. fallback `?: "frp"`).

---

## Never-Do Additions (Stage 15 FRP)

1. Never call EDL erase with a literal partition string in router/executor logic.
2. Never auto-upgrade unsupported FRP path to another path without operator decision.
3. Never emit success state without transport-level confirmation from selected executor.
4. Never route Apple/iCloud operation through Android FRP executors.
5. Never bypass [`FrpResult`](app/src/main/kotlin/com/deepeye/otg/model/FrpModels.kt) sealed-state reporting.

---

## Current Status

- [`FrpUseCase`](app/src/main/kotlin/com/deepeye/otg/usecase/FrpUseCase.kt) updated to remove hardcoded partition fallback and fail closed when EDL partition is not configured.
- Stage 15 FRP policy and PASS 8 auditing requirements are documented in this file.
