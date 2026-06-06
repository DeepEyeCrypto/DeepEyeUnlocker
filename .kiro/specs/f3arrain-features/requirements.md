# Requirements Document — F3arRa1n Features

# Requirements Document

## Introduction

F3arRa1n DeepEyeUnlocker ka ek built-in iOS iCloud activation bypass engine hai jo checkm8 bootrom exploit par based hai. Yeh engine Apple devices (iPhone 5S se iPhone X tak, A7–A11 chips) par iCloud Hello Screen bypass, activation lock removal, aur related operations perform karta hai. Engine teen layers mein kaam karta hai:

1. **Swift Core Engine** (`F3arRa1nEngine.swift`) — macOS par run hota hai, palera1n + gaster tools use karta hai
2. **Rust Tauri Commands** (`f3arrain.rs`) — Desktop app ka bridge layer
3. **Android Kotlin Executor** (`F3arrainExecutor.kt`) — Android OTG USB se direct checkm8 exploit

F3arRa1n ke features UnifiedBypassRegistry mein registered hain aur DeepEyeUnlocker ke bypass catalog ka hissa hain. Yeh document F3arRa1n ke saare features ki complete requirements capture karta hai.

---

## Glossary

- **F3arRa1n_Engine**: Swift-based iOS bypass engine jo checkm8 + palera1n pipeline orchestrate karta hai
- **checkm8**: Apple A5–A11 chips ka bootrom-level hardware exploit (hardware-patched, permanent vulnerability)
- **DFU_Mode**: Device Firmware Update mode — Apple device ka lowest-level USB mode (VID: 0x05AC, PID: 0x1227)
- **Pwned_DFU**: checkm8 exploit ke baad device ka state jisme unsigned code execute ho sakta hai (interfaceCount == 5)
- **gaster**: checkm8 exploit run karne wala open-source tool
- **palera1n**: Semi-tethered jailbreak tool jo ramdisk boot aur DFU helper provide karta hai
- **ideviceactivation**: iOS activation record patch karne wala libimobiledevice tool
- **ECID**: Exclusive Chip ID — har Apple device ka unique hardware identifier
- **CPID**: Chip ID — Apple SoC ka identifier (e.g., 0x8015 = A11)
- **Ramdisk**: Temporary in-memory filesystem jo palera1n boot karta hai bypass ke liye
- **Activation_Record**: iOS device ka activation state store karne wala system record
- **NVRAM**: Non-Volatile RAM — device reboot ke baad bhi data retain karta hai
- **iServices**: Apple services — iMessage, FaceTime, Push Notifications
- **Session_ID**: Har bypass operation ka unique UUID identifier
- **BypassResult**: F3arRa1n operation ka final output — success/failure + metadata
- **ChipConfig**: Per-chip timing configuration (exploit time, mode: buttons/dfuLoop)
- **ProcessRunner**: Swift actor jo external tools (gaster, palera1n, ideviceactivation) run karta hai
- **Logger**: Structured JSON event emitter jo progress events stream karta hai
- **F3arRa1nDevice**: Detected device ka data model (udid, cpid, chipName, iosVersion, isDfu, isCheckm8)

---

## Requirements

### Requirement 1: Device Detection

**User Story:** As a forensic researcher, I want F3arRa1n to automatically detect connected iOS devices, so that I can identify whether the device is checkm8-compatible before starting any bypass operation.

#### Acceptance Criteria

1. WHEN an iOS device is connected via USB in DFU mode, THE F3arRa1n_Engine SHALL detect it by matching USB VID `0x05AC` and PID `0x1227` in the USB device enumeration output
2. WHEN an iOS device is connected in Normal mode, THE F3arRa1n_Engine SHALL detect it via the libimobiledevice device enumeration API and read its CPID, UDID, iOS version, and serial number
3. WHEN a device is detected, THE F3arRa1n_Engine SHALL emit a `device_found` event containing: `udid` (string), `cpid` (hex string), `chip_name` (string), `ios_version` (dot-separated string), `serial` (string), and `is_checkm8` (boolean)
4. WHEN no device is found within 5 seconds, THE F3arRa1n_Engine SHALL emit a `noDevice` error event and stop detection
5. THE F3arRa1n_Engine SHALL set `is_checkm8: true` ONLY IF the device CPID is one of: `0x8960` (A7), `0x7000` (A8), `0x7001` (A8X), `0x8000` (A9), `0x8003` (A9X), `0x8010` (A10), `0x8011` (A10X), `0x8015` (A11)
6. WHEN a device CPID does not match any known checkm8-supported value, THE F3arRa1n_Engine SHALL set `is_checkm8: false` in the `device_found` event
7. IF a device is detected but its CPID cannot be read, THEN THE F3arRa1n_Engine SHALL emit a `device_found` event with `cpid: "unknown"`, `chip_name: "Unknown"`, and `is_checkm8: false`

---

### Requirement 2: DFU Mode Entry

**User Story:** As a forensic researcher, I want F3arRa1n to guide me through entering DFU mode, so that the device is in the correct state for the checkm8 exploit.

#### Acceptance Criteria

1. WHEN a device is detected in Normal mode and DFU entry is needed, THE F3arRa1n_Engine SHALL invoke the DFU helper tool with a 60-second timeout
2. WHEN the DFU helper tool exits with code 0 or its output contains the string "DFU", THE F3arRa1n_Engine SHALL emit a `dfu_ok` event containing the device CPID
3. WHEN the DFU helper tool exits with a non-zero code and its output does not contain "DFU", THE F3arRa1n_Engine SHALL emit a `dfu_warn` event and continue without stopping the pipeline (non-fatal — user may have manually entered DFU)
4. WHEN emitting DFU guidance, THE F3arRa1n_Engine SHALL include `chip` (chip name string), `timing` (exploit time in seconds as a float), and `mode` (one of: "buttons" or "dfuLoop") in the `dfu_guide` event
5. THE F3arRa1n_Engine SHALL use the following chip-specific timing values: A7=14.0s/buttons, A8=2.0s/dfuLoop, A8X=2.0s/dfuLoop, A9=2.0s/dfuLoop, A9X=2.0s/dfuLoop, A10=0.68s/buttons, A10X=0.68s/buttons, A11=0.66s/dfuLoop
6. WHEN a device is already in DFU mode at detection time (PID `0x1227` confirmed), THE F3arRa1n_Engine SHALL skip the DFU entry step and proceed directly to checkm8 without invoking the DFU helper tool

---

### Requirement 3: checkm8 Exploit Execution

**User Story:** As a forensic researcher, I want F3arRa1n to run the checkm8 bootrom exploit, so that the device enters Pwned DFU state enabling unsigned code execution.

#### Acceptance Criteria

1. WHEN checkm8 is initiated, THE F3arRa1n_Engine SHALL verify the device CPID is in the supported chip list BEFORE invoking the exploit tool
2. IF the device CPID is not in the supported list, THEN THE F3arRa1n_Engine SHALL emit a `notCheckm8Vulnerable` error event with the CPID value and stop without running the exploit
3. WHEN checkm8 is run, THE F3arRa1n_Engine SHALL invoke the exploit tool with a 30-second timeout
4. WHEN the exploit tool output contains "PWND" or "pwned" (case-insensitive), OR exits with code 0, THE F3arRa1n_Engine SHALL emit a `checkm8_ok` event and consider the exploit successful
5. WHEN the first checkm8 attempt fails — defined as: exit code non-zero AND output does not contain "PWND" or "pwned" AND timeout has not elapsed — THE F3arRa1n_Engine SHALL wait 1 second and retry exactly once
6. WHEN the first checkm8 attempt fails due to timeout expiry, THE F3arRa1n_Engine SHALL NOT retry and SHALL immediately emit a `gasterFailed` error event marked `retryable: true`
7. WHEN both checkm8 attempts fail (non-zero exit or missing success string), THE F3arRa1n_Engine SHALL emit a `gasterFailed` error event with the last 200 characters of stderr (falling back to stdout if stderr is empty), marked `retryable: true`
8. THE F3arRa1n_Engine SHALL NOT invoke the exploit tool on any device whose CPID is not in the supported checkm8 chip list (A7–A11)
9. THE F3arRa1n_Engine SHALL NOT invoke the exploit tool unless USB DFU mode (VID `0x05AC`, PID `0x1227`) is confirmed at the time of invocation

---

### Requirement 4: Ramdisk Boot

**User Story:** As a forensic researcher, I want F3arRa1n to boot a bypass ramdisk after checkm8, so that the activation record can be patched without modifying the device's NAND storage.

#### Acceptance Criteria

1. WHEN ramdisk boot is initiated, THE F3arRa1n_Engine SHALL invoke the ramdisk boot tool with flags `--no-colors`, `-e rootdev=md0`, and `--skip-fakefs`, with a 180-second timeout
2. WHEN the ramdisk tool output contains "done" or "success" (case-insensitive), OR exits with code 0, THE F3arRa1n_Engine SHALL emit a `ramdisk_ok` event containing the iOS version as a dot-separated string parsed from tool stdout (empty string if not present)
3. WHEN ramdisk tool output lines are received during streaming, THE F3arRa1n_Engine SHALL emit a `ramdisk_line` event for each line
4. WHEN the ramdisk tool exits with a non-zero code AND its output does not contain "done" or "success", THE F3arRa1n_Engine SHALL emit a `ramdiskFailed` error event with the last 300 characters of stderr (falling back to stdout if stderr is empty), marked `retryable: true`
5. WHEN the 180-second timeout elapses before the ramdisk tool exits, THE F3arRa1n_Engine SHALL terminate the tool process and emit a `ramdiskFailed` error event marked `retryable: true`

---

### Requirement 5: Activation Patch

**User Story:** As a forensic researcher, I want F3arRa1n to patch the iOS activation record after ramdisk boot, so that the iCloud Hello Screen is bypassed and the device becomes usable.

#### Acceptance Criteria

1. WHEN activation patching is initiated, THE F3arRa1n_Engine SHALL first attempt Method A: invoke the activation tool without arguments, with a 60-second timeout
2. IF the activation tool binary is not found on the system PATH, THEN THE F3arRa1n_Engine SHALL emit an `activation_error` event with reason "activation tool not found" and stop the activation phase
3. WHEN Method A exits with code 0 or its output contains "success" (case-insensitive), THE F3arRa1n_Engine SHALL emit `activation_ok` with `method: "A"` and stop without attempting further methods
4. WHEN Method A fails — defined as: exit code non-zero, OR timeout elapsed, OR output does not contain "success" — THE F3arRa1n_Engine SHALL attempt Method B: invoke the activation tool with the device UDID argument, with a 60-second timeout
5. WHEN Method B fails by the same criteria, THE F3arRa1n_Engine SHALL attempt Method C: invoke the activation tool with the debug flag, with a 60-second timeout
6. WHEN all three activation methods fail, THE F3arRa1n_Engine SHALL emit an `activation_partial` event with `wifi: true` indicating WiFi bypass is still active, and SHALL NOT emit a fatal error event

---

### Requirement 6: Full Chain Orchestration

**User Story:** As a forensic researcher, I want F3arRa1n to run the complete bypass pipeline in one command, so that I can bypass iCloud Hello Screen with minimal manual steps.

#### Acceptance Criteria

1. WHEN `f3arrain_full` is invoked with a caller-supplied `session_id`, THE F3arRa1n_Engine SHALL execute steps in this fixed order: Detect → Validate Chip → checkm8 → Ramdisk → Activation Patch
2. IF the detected device is not already in DFU mode, THEN THE F3arRa1n_Engine SHALL insert the DFU Entry step between Validate Chip and checkm8
3. WHEN the full chain completes successfully, THE F3arRa1n_Engine SHALL emit a `bypass_complete` event containing: `chip` (chip name string), `ios` (iOS version string), `signal: false`, `untethered: false`, `method: "checkm8+palera1n+ideviceactivation"`, and `notes` (array of strings: ["WiFi: Active", "Signal: Run Full Signal bypass for SIM", "Tethered: Re-run after power cycle", "iServices: Run iServices fix for iMessage+FaceTime"])
4. IF any step emits a fatal error (an error event with `retryable: false`), THEN THE F3arRa1n_Engine SHALL stop the pipeline immediately without executing subsequent steps
5. WHEN each pipeline step completes, THE F3arRa1n_Engine SHALL emit a `progress` event with the percentage value for that step: Detect=5%, DFU=15%, checkm8=30%, Ramdisk=55%, Activation=80%, Complete=100%
6. IF the caller does not supply a `session_id`, THEN THE F3arRa1n_Engine SHALL generate a UUID and use it as the `session_id` for all events in that operation
7. WHEN `f3arrain_detect` is invoked, THE F3arRa1n_Engine SHALL run only the detection step and return the last valid JSON event from stdout within a 30-second timeout; IF no valid JSON event is produced, THEN THE F3arRa1n_Engine SHALL return an error event

---

### Requirement 7: Android USB Executor

**User Story:** As a mobile forensic researcher using the Android app, I want F3arRa1n to run the checkm8 exploit directly via Android OTG USB, so that I can perform bypass operations from an Android device without a desktop computer.

#### Acceptance Criteria

1. WHEN `F3arrainExecutor.runExploit()` is called, THE F3arrainExecutor SHALL verify the connected Apple device is in DFU mode by checking `detectAppleMode() == DeviceMatrix.AppleMode.DFU`
2. IF the device is not in DFU mode, THEN THE F3arrainExecutor SHALL return `F3arrainResult.Error` with a message containing the device's current USB product ID
3. WHEN DFU mode is confirmed, THE F3arrainExecutor SHALL verify `device.interfaceCount == 1` before opening the USB connection
4. IF `device.interfaceCount != 1`, THEN THE F3arrainExecutor SHALL return `F3arrainResult.Error` with a message indicating the unexpected interface count
5. IF the USB connection cannot be opened, THEN THE F3arrainExecutor SHALL return `F3arrainResult.Error` with a message describing the connection failure and count it as a failed attempt
6. WHEN the USB connection is opened, THE F3arrainExecutor SHALL send a DFU_DNLOAD control transfer with a 0x800-byte max packet (bmRequestType=0x21, bRequest=1, wValue=0, wIndex=0)
7. WHEN the DFU_DNLOAD max packet is sent, THE F3arrainExecutor SHALL wait 20 ms before sending a zero-length packet (ZLP)
8. WHEN the ZLP is sent, THE F3arrainExecutor SHALL send DFU_GETSTATUS (bmRequestType=0xA1, bRequest=3) and wait 100 ms
9. WHEN `device.interfaceCount == 5` after the exploit sequence, THE F3arrainExecutor SHALL return `F3arrainResult.PwnedDfu` with the chip model string resolved from DeviceMatrix for the connected device's product ID
10. WHEN the exploit sequence does not result in `interfaceCount == 5` — due to USB transfer error, wrong interface count, or connection failure — THE F3arrainExecutor SHALL count it as a failed attempt and retry up to 3 total attempts before returning `F3arrainResult.Error("checkm8 failed — not pwned")`
11. WHEN the exploit sequence completes (success or failure), THE F3arrainExecutor SHALL close the USB connection regardless of outcome

---

### Requirement 8: Hello Screen Bypass (F3ARRAIN_HELLO_BYPASS)

**User Story:** As a forensic researcher, I want to bypass the iCloud Hello Screen on A7–A11 iPhones for free, so that I can access the device without iCloud credentials.

#### Acceptance Criteria

1. THE F3arRa1n_Engine SHALL support Hello Screen Bypass on devices with chips A7 through A11 (iPhone 5S, 6, 6 Plus, 6S, 6S Plus, SE 1st gen, 7, 7 Plus, 8, 8 Plus, X)
2. THE F3arRa1n_Engine SHALL support iOS versions 12.0 through 16.7.8 (inclusive) for Hello Screen Bypass; IF the detected iOS version is outside this range, THEN THE F3arRa1n_Engine SHALL emit an error event before starting the pipeline
3. THE Hello_Screen_Bypass SHALL be available at zero cost (`costCredits: 0`, `isFree: true`)
4. WHEN Hello Screen Bypass completes, THE F3arRa1n_Engine SHALL set `signalAfter: false` in the result (WiFi only, no SIM signal)
5. WHEN Hello Screen Bypass completes, THE F3arRa1n_Engine SHALL set `isUntethered: false` in the result (tethered — re-run required after power cycle)
6. THE Hello_Screen_Bypass SHALL set `dataLoss: false` in the result (no user data is modified)
7. THE Hello_Screen_Bypass SHALL require DFU mode entry (`requiresDfu: true`)
8. THE Hello_Screen_Bypass pipeline step timeouts SHALL be: Detect ≤10s, DFU Guide ≤30s, checkm8 ≤30s, Ramdisk ≤180s, Activation ≤60s; IF any step exceeds its timeout, THEN THE F3arRa1n_Engine SHALL emit a retryable error event for that step

---

### Requirement 9: A12+ Free Bypass (F3AR_A12_FREE)

**User Story:** As a forensic researcher, I want to bypass iCloud on A12+ devices for free using F3arRa1n's server exploit, so that I can access newer iPhones without paying credits.

#### Acceptance Criteria

1. THE F3arRa1n_Engine SHALL support A12+ Hello Bypass on chips A12 through A18 (iPhone XS and newer)
2. THE F3arRa1n_Engine SHALL support iOS versions 15.0 through 26.1 (inclusive) for A12+ Free Bypass; IF the detected iOS version is outside this range, THEN THE F3arRa1n_Engine SHALL emit an error event before starting the pipeline
3. THE A12_Free_Bypass SHALL be available at zero cost (`costCredits: 0`, `isFree: true`)
4. WHEN A12+ Free Bypass is executed, THE F3arRa1n_Engine SHALL read the device ECID and send a server request with 0 credits
5. WHEN the server responds with a bypass token, THE F3arRa1n_Engine SHALL write the token to the device activation record
6. IF the server request fails (network error, timeout, or non-200 response), THEN THE F3arRa1n_Engine SHALL emit an error event with the failure reason and stop
7. IF writing the bypass token to the activation record fails, THEN THE F3arRa1n_Engine SHALL emit an error event with the failure reason and stop
8. WHEN A12+ Free Bypass completes successfully, THE F3arRa1n_Engine SHALL set `isUntethered: true` in the result (survives reboot)
9. WHEN A12+ Free Bypass completes successfully, THE F3arRa1n_Engine SHALL set `signalAfter: false` in the result (WiFi only)
10. THE A12_Free_Bypass SHALL require internet connectivity (`requiresInternet: true`); IF no internet is available at start, THEN THE F3arRa1n_Engine SHALL emit an error event before making the server request
11. THE A12_Free_Bypass SHALL NOT require DFU mode (`requiresDfu: false`)
12. THE A12_Free_Bypass pipeline step timeouts SHALL be: Detect ECID ≤10s, Server Request ≤60s, Apply Token ≤10s, Verify Activation ≤15s

---

### Requirement 10: Free Temporary Test Mode (F3AR_TEMP_TEST)

**User Story:** As a forensic researcher, I want to run a free temporary activation test before purchasing credits, so that I can verify device compatibility without financial commitment.

#### Acceptance Criteria

1. THE F3arRa1n_Engine SHALL support Temporary Test Mode on ALL chip ranges (A7 through A18)
2. THE F3arRa1n_Engine SHALL support iOS versions 12.0 through 26.1 (inclusive) for Temporary Test Mode
3. THE Temporary_Test_Mode SHALL be available at zero cost (`costCredits: 0`, `isFree: true`)
4. WHEN Temporary Test Mode is executed, THE F3arRa1n_Engine SHALL apply a temporary activation token to the device; IF the token application fails, THEN THE F3arRa1n_Engine SHALL emit an error event with the failure reason
5. WHEN the temporary token is applied, THE F3arRa1n_Engine SHALL verify WiFi connectivity on the device; WHEN WiFi connectivity is confirmed, THE F3arRa1n_Engine SHALL consider activation successful
6. WHEN Temporary Test Mode completes, THE F3arRa1n_Engine SHALL emit a compatibility report event confirming device eligibility and the temporary activation status
7. THE Temporary_Test_Mode SHALL set `isUntethered: false` in the result (token reverts on power cycle)
8. THE Temporary_Test_Mode SHALL NOT require DFU mode, internet connection, or IMEI input
9. THE Temporary_Test_Mode pipeline step timeouts SHALL be: Test Activation ≤30s, Verify WiFi ≤30s, Report ≤5s

---

### Requirement 11: Boot Files Method — iOS 15+ (F3AR_BOOT_FILES)

**User Story:** As a forensic researcher, I want to use F3arRa1n's Boot Files Method for iOS 15+ devices, so that I can achieve an untethered bypass that survives device reboots.

#### Acceptance Criteria

1. THE F3arRa1n_Engine SHALL support Boot Files Method on A7–A11 chips (iPhone 5S through iPhone X)
2. THE F3arRa1n_Engine SHALL support iOS versions 15.0 through 16.7 (inclusive) for Boot Files Method; IF the detected iOS version is outside this range, THEN THE F3arRa1n_Engine SHALL emit an error event before starting the pipeline
3. THE Boot_Files_Method SHALL cost 10 credits (`costCredits: 10`, `isFree: false`)
4. WHEN Boot Files Method is initiated, THE F3arRa1n_Engine SHALL read the exact iOS version string from the device
5. WHEN the iOS version is read, THE F3arRa1n_Engine SHALL select boot files that exactly match that version string; IF no matching boot files exist, THEN THE F3arRa1n_Engine SHALL emit an error event before attempting any upload
6. WHEN matching boot files are selected, THE F3arRa1n_Engine SHALL require the user to enter DFU mode before proceeding
7. WHEN DFU mode is confirmed, THE F3arRa1n_Engine SHALL upload the boot files to the device; IF the upload fails, THEN THE F3arRa1n_Engine SHALL emit an error event with the failure reason
8. WHEN boot files are uploaded successfully, THE F3arRa1n_Engine SHALL inject the bypass into NVRAM; IF NVRAM injection fails, THEN THE F3arRa1n_Engine SHALL emit an error event with the failure reason
9. WHEN Boot Files Method completes successfully, THE F3arRa1n_Engine SHALL set `isUntethered: true` in the result (survives reboot)
10. WHEN Boot Files Method completes successfully, THE F3arRa1n_Engine SHALL set `signalAfter: true` in the result (SIM signal preserved)
11. THE Boot_Files_Method pipeline step timeouts SHALL be: Detect iOS ≤5s, Select Files ≤5s, DFU Entry ≤30s, Upload ≤60s, Inject ≤30s, Reboot ≤45s

---

### Requirement 12: Supported Device Matrix

**User Story:** As a forensic researcher, I want F3arRa1n to accurately identify which devices are supported, so that I don't attempt operations on incompatible hardware.

#### Acceptance Criteria

1. THE F3arRa1n_Engine SHALL maintain a chip support matrix mapping CPID hex values to chip names and supported device models
2. THE F3arRa1n_Engine SHALL recognize the following checkm8-supported chips: A7 (0x8960) → iPhone 5S; A8 (0x7000) → iPhone 6, 6 Plus, iPod Touch 6; A8X (0x7001) → iPad Air 2; A9 (0x8000) → iPhone 6S, 6S Plus, SE 1st gen; A9X (0x8003) → iPad Pro 9.7/12.9 1st gen; A10 (0x8010) → iPhone 7, 7 Plus, iPod Touch 7; A10X (0x8011) → iPad Pro 10.5/12.9 2nd gen; A11 (0x8015) → iPhone 8, 8 Plus, iPhone X
3. WHEN a device identifier string is provided, THE F3arRa1n_Engine SHALL return the correct chip enum value from the device-chip map
4. WHEN `isCheckm8Supported` is called with a device identifier, THE F3arRa1n_Engine SHALL return `true` only for identifiers that map to A7, A8, A8X, A9, A9X, A10, A10X, or A11 chips
5. WHEN a device identifier is not present in the device-chip map, THE F3arRa1n_Engine SHALL return an UNKNOWN chip value and `isCheckm8Supported` SHALL return `false`
6. THE F3arRa1n_Engine SHALL accept device identifiers in the format "iPhoneX,Y" (e.g., "iPhone10,6" maps to A11)
7. WHEN a malformed device identifier (not matching "iPhoneX,Y" or equivalent format) is provided, THE F3arRa1n_Engine SHALL treat it as not present in the map and return UNKNOWN

---

### Requirement 13: Error Classification and Retry Logic

**User Story:** As a forensic researcher, I want F3arRa1n to clearly classify errors and indicate which ones are retryable, so that I know whether to retry the operation or investigate a hardware issue.

#### Acceptance Criteria

1. THE F3arRa1n_Engine SHALL classify all errors into one of these layers: DETECT, CHIP, CHECKM8, RAMDISK, ACTIVATION, SPAWN, PARSE, UNKNOWN
2. WHEN a `gasterFailed` error occurs, THE F3arRa1n_Engine SHALL assign it to the CHECKM8 layer and mark it `retryable: true`
3. WHEN a `ramdiskFailed` error occurs, THE F3arRa1n_Engine SHALL assign it to the RAMDISK layer and mark it `retryable: true`
4. WHEN a `noDevice` error occurs, THE F3arRa1n_Engine SHALL assign it to the DETECT layer and mark it `retryable: false`
5. WHEN a `notCheckm8Vulnerable` error occurs, THE F3arRa1n_Engine SHALL assign it to the CHIP layer and mark it `retryable: false`
6. WHEN an error event is emitted, THE F3arRa1n_Engine SHALL include `layer` (string), `reason` (string), and `retryable` (boolean) fields in the event payload
7. IF an error event has `retryable: true`, THEN THE F3arRa1n_Panel SHALL display a "↻ RETRY" button that re-invokes the same pipeline command with the same session_id

---

### Requirement 14: Tauri Command Interface

**User Story:** As a developer integrating F3arRa1n into the DeepEyeUnlocker desktop app, I want well-defined Tauri commands, so that the React frontend can invoke F3arRa1n operations reliably.

#### Acceptance Criteria

1. THE Tauri_Bridge SHALL expose a `f3arrain_full(session_id: String) -> Result<Vec<Value>, F3arError>` command for full chain execution
2. THE Tauri_Bridge SHALL expose a `f3arrain_detect(session_id: String) -> Result<Value, F3arError>` command for device detection only
3. THE Tauri_Bridge SHALL expose a `f3arrain_checkm8(session_id: String) -> Result<Vec<Value>, F3arError>` command for checkm8-only testing
4. WHEN a Tauri command is invoked, THE Tauri_Bridge SHALL resolve the Swift binary path from the app's resource directory at runtime
5. IF the Swift binary file does not exist at the resolved path, THEN THE Tauri_Bridge SHALL return `F3arError { layer: "SPAWN", reason: "binary not found at <path>", retryable: false }` without attempting to spawn a process
6. WHEN the Swift binary produces a stdout line that starts with `{`, THE Tauri_Bridge SHALL parse it as a JSON value and append it to the result collection
7. WHEN a stdout line does not start with `{`, THE Tauri_Bridge SHALL ignore it without returning an error
8. WHEN the Swift binary exits with a non-zero code and the result collection is non-empty, THE Tauri_Bridge SHALL return the collected events as a partial result rather than an error
9. WHEN the Swift binary cannot be spawned due to an OS error, THE Tauri_Bridge SHALL return `F3arError { layer: "SPAWN", reason: <OS error message>, retryable: false }`
10. WHEN a stdout line starts with `{` but cannot be parsed as valid JSON, THE Tauri_Bridge SHALL return `F3arError { layer: "PARSE", reason: <parse error>, retryable: false }`
11. THE F3arError type SHALL contain `layer: String`, `reason: String`, and `retryable: bool` fields and SHALL be serializable to JSON

---

### Requirement 15: Frontend Panel (F3arRa1nPanel)

**User Story:** As a forensic researcher using the desktop app, I want a clear visual panel for F3arRa1n operations, so that I can monitor bypass progress and understand the current step.

#### Acceptance Criteria

1. THE F3arRa1n_Panel SHALL display a 6-step pipeline indicator with steps: Detect → DFU → checkm8 → Ramdisk → Bypass → Done
2. WHEN a step is active, THE F3arRa1n_Panel SHALL highlight it with cyan color and a spinning loader icon
3. WHEN a step is completed, THE F3arRa1n_Panel SHALL highlight it with green color and a checkmark icon
4. WHEN a device is detected, THE F3arRa1n_Panel SHALL display a device card showing chip name, iOS version, CPID, and checkm8 compatibility badge
5. WHEN the bypass is running, THE F3arRa1n_Panel SHALL display a progress bar with the current percentage value and the current phase label
6. WHEN bypass completes successfully, THE F3arRa1n_Panel SHALL display a green result card with bypass notes (WiFi status, signal status, tethered status, iServices status)
7. WHEN an error event is received, THE F3arRa1n_Panel SHALL display a red error card showing the `layer` and `reason` fields from the error event
8. THE F3arRa1n_Panel SHALL maintain a scrollable event log showing the last 200 events; each entry SHALL be color-coded as: green for success events, red for error events, yellow for warning events, and white for informational events
9. WHEN an incoming event fails Zod schema validation, THE F3arRa1n_Panel SHALL discard the event and log a warning entry in the event log without crashing
10. WHEN the panel is in idle state, THE F3arRa1n_Panel SHALL display a "▶ START F3ARRA1N" button; WHEN the panel is in error state with a retryable error, THE F3arRa1n_Panel SHALL display a "↻ RETRY" button
11. WHEN bypass completes or an error occurs, THE F3arRa1n_Panel SHALL display a "RESET" button that returns the panel to idle state and clears the event log

---

### Requirement 16: Safety Guardrails

**User Story:** As a system architect, I want F3arRa1n to enforce strict safety rules, so that the exploit pipeline cannot be misused or run on incompatible hardware.

#### Acceptance Criteria

1. THE F3arRa1n_Engine SHALL NEVER invoke the checkm8 exploit tool on a device whose CPID is not in the supported chip list (A7–A11)
2. THE F3arRa1n_Engine SHALL NEVER skip CPID verification before invoking the exploit tool, even if the caller asserts the device is compatible
3. THE F3arRa1n_Engine SHALL NEVER send iBoot payload before Pwned DFU is confirmed — confirmed means `interfaceCount == 5` on Android or exploit tool output containing "PWND" on desktop
4. THE F3arRa1n_Engine SHALL NEVER use hardcoded iBoot file paths — all iBoot paths must be resolved at runtime from user-provided input or a validated file catalog
5. THE F3arRa1n_Engine SHALL NEVER execute bypass pipeline steps on the UI thread — all pipeline steps must run on a background thread or async context
6. THE F3arRa1n_Engine SHALL NEVER invoke the checkm8 exploit tool unless USB DFU mode (VID `0x05AC`, PID `0x1227`) is confirmed at the time of invocation
7. THE F3arRa1n_Engine SHALL NEVER emit a bypass success event unless the tool exit code is 0 or the tool output contains an explicit success signal ("PWND", "pwned", "done", or "success")
8. THE F3arRa1n_Engine SHALL NEVER use a delay-based retry loop as a substitute for checking actual USB or device state between attempts
9. THE F3arrainExecutor SHALL NEVER open a USB connection to a device without first verifying `interfaceCount == 1`
10. WHEN bypass is initiated, THE F3arRa1n_Engine SHALL verify device authorization — defined as: ECID is readable AND CPID is in the supported chip list — before proceeding; IF either check fails, THEN THE F3arRa1n_Engine SHALL emit an error event and stop

---

### Requirement 17: Timing and Delay Rules

**User Story:** As a developer, I want F3arRa1n to use only approved timing delays in the USB pipeline, so that the exploit sequence is reliable and does not introduce artificial progress simulation.

#### Acceptance Criteria

1. THE F3arrainExecutor SHALL use a 20 ms delay for USB control-transfer inter-frame gaps
2. THE F3arrainExecutor SHALL use a 100 ms delay for USB STALL/reset hold
3. THE F3arrainExecutor SHALL use a 150 ms delay for post-exploit re-enumeration settle
4. IF an iBoot send-to-execution gap is required, THEN THE F3arRa1n_Engine SHALL use a 500 ms delay
5. IF a ramdisk boot wait is required, THEN THE F3arRa1n_Engine SHALL use a 2000 ms delay
6. IF a filesystem stabilization wait is required, THEN THE F3arRa1n_Engine SHALL use a 3000 ms delay
7. THE F3arRa1n_Engine SHALL NOT use any delay value not listed in criteria 1–6 within the USB pipeline or bypass pipeline; any new delay must be added to this list before use

---

### Requirement 18: Bypass Feature Registry Integration

**User Story:** As a developer, I want F3arRa1n features to be registered in the UnifiedBypassRegistry, so that they appear in the bypass catalog alongside other tools and can be filtered and compared.

#### Acceptance Criteria

1. THE UnifiedBypassRegistry SHALL contain at minimum these F3arRa1n feature entries: `F3ARRAIN_HELLO_BYPASS`, `F3AR_A12_FREE`, `F3AR_TEMP_TEST`, `F3AR_BOOT_FILES`
2. WHEN F3arRa1n features are registered, THE UnifiedBypassRegistry SHALL set `source = FeatureSource.F3ARRAIN` for all F3arRa1n entries
3. THE UnifiedBypassRegistry SHALL tag each F3arRa1n feature with: "checkm8", "f3arrain", "hello", "activation", the chip identifiers it supports (e.g., "a7", "a8", "a11"), and the iOS version range it covers (e.g., "ios12", "ios15", "ios16")
4. WHEN `cheapestCandidate()` is called with `requireFree: true`, THE UnifiedBypassRegistry SHALL return `F3ARRAIN_HELLO_BYPASS` for A7–A11 devices and `F3AR_A12_FREE` for A12+ devices, as these are the only free bypass options
5. THE UnifiedBypassRegistry SHALL report `isFree: true` and `costCredits: 0` for `F3ARRAIN_HELLO_BYPASS`, `F3AR_A12_FREE`, and `F3AR_TEMP_TEST`
6. THE UnifiedBypassRegistry SHALL report `isFree: false` and `costCredits: 10` for `F3AR_BOOT_FILES`
