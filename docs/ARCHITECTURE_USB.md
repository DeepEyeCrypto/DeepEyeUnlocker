### DeepEye OTG — USB Detection & Lifecycle Architecture

---

#### 1. Classifier (`ProtocolDetector`)

- **Input**: Immutable `UsbDescriptorSnapshot` built by `UsbSnapshotFactory.from(UsbDevice)`.
- **Output**: `DetectionResult(deviceMode, protocolFamily, confidence, reason)` with a strict precedence (spec Stage 1.4):
  - Apple: `APPLE_DFU`, `APPLE_RECOVERY`, `APPLE_NORMAL` (VID 0x05AC)
  - MTK: `MTK_BROM`, `MTK_PRELOADER`, `MTK_META`
  - Qualcomm: `QC_EDL`, `QC_DIAG`
  - UniSoc: `UNISOC_FDL` (VID 0x1782)
  - Samsung: `SAMSUNG_ODIN`
  - Generic: `FASTBOOT`, `ADB`, `MTP_ONLY`
  - Fallback: `UNKNOWN`
- **Rules enforced**:
  - **No ADB fallback**: ADB is only returned when interface `FF/42/01` has both BULK IN + BULK OUT endpoints.
  - **UNKNOWN is safe default**: When no explicit signature matches, classifier returns `DeviceMode.UNKNOWN` / `ProtocolFamily.UNKNOWN` with confidence 0 and logs an `unknown-summary`.
  - **Known-vendor, unknown-PID**: For MTK (`0x0E8D`), Qualcomm (`0x05C6`), Samsung (`0x04E8`), unknown PIDs produce:
    - `DeviceMode.UNKNOWN` / `ProtocolFamily.UNKNOWN`
    - Structured log: `[MODE] known-vendor-unknown-pid vendor=... vid=0x.... pid=0x....`
  - **Text heuristics are gated**:
    - Samsung Odin text heuristic only fires when `vid == 0x04E8`.
    - Fastboot text heuristic requires at least one vendor-specific interface (`class == 0xFF`); pure strings never classify by themselves.

Logging:

- Snapshot: `[MODE] attach ...`, `[MODE] intf[i] ...`
- Decision: `[MODE] classify mode=... family=... confidence=... reason="..."`
- UNKNOWN summary: `[MODE] unknown-summary vid=0x.... pid=0x.... intfTuples="..."`.

Unit tests:

- `ProtocolDetectorTest.kt` covers:
  - T01/T02/T03/T04/T07: BROM/EDL, strict ADB, MTP-only, UNKNOWN, known-vendor-unknown-PID.
  - Odin/fastboot heuristics with VID/interface gating.

---

#### 2. Lifecycle (`UsbLifecycleManager`)

Location: `app/src/main/kotlin/com/deepeye/otg/usb/UsbLifecycleManager.kt`

- **Key types**:
  - `UsbLifecycleState`: Idle, DeviceDetected, PermissionPending, PermissionDenied, Connecting, Connected, Degraded, Dead, Operating, Error, NoOtgSupport.
  - `deviceKey`: `"${vendorId}:${productId}:${deviceId}"` (deviceId changes on every re-enumeration).
- **Attach flow**:
  1. Broadcast `ACTION_USB_DEVICE_ATTACHED` is debounced by 400ms in `UsbBroadcastReceiver`.
  2. `onDeviceAttached(device)` acquires `lifecycleMutex`, computes `deviceKey`, and:
     - Ignores stale same-key attaches when permission is already pending or session is active.
     - Logs re-enumeration if `deviceKey` changed.
  3. Calls `closeInternal()` to clear any old session.
  4. Builds a fresh `UsbDescriptorSnapshot` and calls `ProtocolDetector.detect(snapshot)`.
  5. Emits `DeviceDetected` with detection details and snapshot.
  6. If permission is granted:
     - Cancels any pending permission timeout and opens connection immediately.
     - Transitions to `Connecting` → `Connected`.
     - Starts a watchdog based on `GET_STATUS`.
  7. If permission is not granted:
     - Sets `_state = PermissionPending(device)`.
     - Stores `pendingPermissionDeviceKey`.
     - Requests permission via `UsbPermissionGuard.requestPermission(...)`.
     - Starts a 10s permission timeout job.

- **Permission result**:
  - `onPermissionResult(device, granted)`:
    - Validates that result matches the pending `deviceKey` and current `PermissionPending` state.
    - On grant:
      - Cancels permission timeout.
      - Takes a **fresh** `UsbDescriptorSnapshot` and re-runs the classifier.
      - Opens connection and emits `Connected`.
    - On denial:
      - Cancels timeout.
      - Emits `PermissionDenied`.

- **Detach & cleanup**:
  - `onDeviceDetached(device)`:
    - Validates against `activeDeviceKey` or `pendingPermissionDeviceKey`.
    - Logs `[MODE] detach key=...`.
    - Calls `closeInternal()` and moves back to `Idle`.
  - `closeInternal()`:
    - Cancels watchdog + permission timeout.
    - Releases interface, closes connection in a `try/finally`.
    - Clears all session fields: device, connection, interface, endpoints, detection, snapshot, keys.

---

#### 3. Session Layer (`UsbSessionManager`)

Location: `app/src/main/kotlin/com/deepeye/otg/usb/UsbSessionManager.kt`

- Provides a lower-level event-driven API:
  - `UsbConnectionEvent`: DeviceDetected, DevicePermissionGranted/Denied, ConnectionOpened, ConnectionFailed, DeviceDisconnected, NoOtgSupport.
  - `events: SharedFlow<UsbConnectionEvent>` for observers (ViewModels/engines).
- Uses the same `ProtocolDetector` (`detectModeFromDescriptors`) and a consistent `deviceKey = "${vid}:${pid}:${deviceId}"`.
- Key behaviors:
  - Initializes by scanning existing `usbManager.deviceList`.
  - On attach:
    - Logs `[MODE] phys-attach ...`.
    - Emits `DeviceDetected`.
    - Requests permission or opens connection directly if already granted.
  - On permission result:
    - Emits `DevicePermissionGranted/Denied`.
    - Re-detects mode and opens connection on grant.
  - On `openConnection`:
    - Serializes via `connectMutex` and runs on `Dispatchers.IO`.
    - Safely opens via `UsbPermissionGuard.safeOpenDevice`.
    - Resolves endpoints via `UsbEndpointResolver`.
    - Claims interface, initializes `UsbTransferQueue`, and starts `UsbConnectionWatchdog`.
    - Logs `[MODE] session-opened key=... mode=...`.
    - Emits `ConnectionOpened`.
  - On detach:
    - Compares `deviceKey` against `activeDeviceKey`.
    - Logs `[MODE] phys-detach key=...` or `[MODE] phys-detach-ignored ...`.
    - Closes device and emits `DeviceDisconnected` on match.

Cleanup:

- `closeDeviceInternal()` stops the watchdog, releases/close connection, and clears `activeDevice`, `activeConnection`, `activeEndpoints`, `transferQueue`, `activeDeviceKey`.

---

#### 4. Transfers & Watchdog

**SafeBulkTransfer**

- Provides direction-checked, chunked bulk `write/read/exchange` helpers returning a `BulkResult` sealed class.
- Handles:
  - Direction mismatch (`Error`).
  - Chunking writes up to 16KB.
  - Empty ACKs (`EmptyAck`).
  - Timeouts vs stalls (detects repeated `-1` to mark `Stall`).
  - Optional partial reads.

**UsbTransferQueue**

- Serializes access to `UsbDeviceConnection` via `Mutex`.
- Implements:
  - Chunked writes with retries + exponential backoff.
  - Stall detection via `GET_STATUS` and `CLEAR_FEATURE` to clear endpoint halt.
  - Structured `TransferResult` with retry counts and error messages.

**UsbConnectionWatchdog**

- Periodic `GET_STATUS`-based health checks:
  - Exposes `StateFlow<ConnectionHealth>`: HEALTHY, DEGRADED, DEAD, PAUSED.
  - Calls `disconnectHandler` after `maxMissedPings`, then resets state.
  - Supports pause/resume around critical flash operations.

---

#### 5. UI Wiring (`MainScreen`)

- Observes:
  - `lifecycleState: StateFlow<UsbLifecycleState>`
  - `sessionState: SessionState` (domain session)
  - `currentNav`, `performanceMode`, `userRole`.
- State routing:
  - `Idle` → `DisconnectedView` (no feature cards).
  - `DeviceDetected` / `PermissionPending` → `WaitingScreen`.
  - `NoOtgSupport` → `ConnectionTestScreen`.
  - `Connected` + `deviceMode == MTP_ONLY` → `MtpOnlyScreen`.
  - `Connected` (+ other modes) → `ActiveSessionView`.
  - `Degraded` / `Dead` → `ErrorScreen` with explicit reason.
  - `Error` → `ErrorOverlay`.
- Feature cards:
  - Sourced from `DeepEyeCatalogs.FEATURE_GROUPS`.
  - Availability computed via `AvailabilityEngine.availabilityFor`, using `SessionState`, `PolicyTier`, and `DeviceMode`.
  - Disabled cards show explanation strings (wrong mode, missing model, insufficient policy tier).
