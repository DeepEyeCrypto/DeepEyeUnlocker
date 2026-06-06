# Implementation Plan: F3arRa1n Features

## Overview

Implement the F3arRa1n iOS iCloud activation bypass engine across three execution layers:
Swift Core Engine (macOS), Rust Tauri Bridge (desktop), and Android Kotlin Executor (OTG USB).
Register four bypass modes in UnifiedBypassRegistry and wire a React/TypeScript frontend panel.
All 22 correctness properties from the design are covered by property-based tests.

## Tasks

- [ ] 1. Swift Core Engine — Data Models and Chip Configuration
  - [ ] 1.1 Create `ChipConfig`, `ExploitMode`, and `CHIP_CONFIGS` table in `F3arRa1nEngine.swift`
    - Define `struct ChipConfig` with `cpid`, `name`, `exploitTime`, `mode` fields
    - Define `enum ExploitMode { case buttons; case dfuLoop }`
    - Populate `CHIP_CONFIGS: [Int: ChipConfig]` with all 8 entries per spec table
    - _Requirements: 2.4, 2.5, 12.1, 12.2_
  - [ ]\* 1.2 Write property test for CPID-to-checkm8 mapping (SwiftCheck)
    - **Property 1: CPID-to-checkm8 mapping is exact**
    - **Validates: Requirements 1.5, 1.6, 3.1**
  - [ ]\* 1.3 Write property test for chip timing config correctness (SwiftCheck)
    - **Property 5: Chip timing config correctness**
    - **Validates: Requirements 2.4, 2.5**

- [ ] 2. Swift Core Engine — `F3arRa1nDevice`, `F3arRa1nError`, and `Logger`
  - [ ] 2.1 Define `F3arRa1nDevice` struct and `F3arRa1nError` enum
    - Implement `F3arRa1nDevice: Codable` with all fields: `udid`, `cpid`, `chipName`, `iosVersion`, `serial`, `isDfu`, `isCheckm8`, `sessionId`
    - Implement `F3arRa1nError: Error, LocalizedError` with all five cases
    - _Requirements: 1.3, 1.5, 1.6, 1.7, 13.1–13.6_
  - [ ] 2.2 Implement `Logger` struct
    - Implement `emit(_:_:)`, `progress(_:_:)`, `error(_:layer:retryable:)`, `success(_:extra:)`
    - Use `JSONSerialization` with `.sortedKeys` for deterministic output
    - Call `fflush(stdout)` after every `print()`
    - _Requirements: 6.5, 13.6_
  - [ ]\* 2.3 Write property test for `device_found` event field completeness (SwiftCheck)
    - **Property 2: device_found event contains all required fields**
    - **Validates: Requirements 1.3, 1.7**
  - [ ]\* 2.4 Write property test for error layer and retryable classification (SwiftCheck)
    - **Property 19: Error layer and retryable classification**
    - **Validates: Requirements 13.1–13.6**

- [ ] 3. Swift Core Engine — `ProcessRunner`
  - [ ] 3.1 Implement `ProcessRunner.run(_:args:env:timeout:)` synchronous variant
    - Resolve tool path from `resources/tools/` first, then fall back to `PATH` via `which()`
    - Implement `DispatchSemaphore`-based timeout; call `proc.terminate()` on expiry
    - Return synthetic `ProcessResult(exitCode: -1, stderr: "Timeout after Xs")` on timeout
    - _Requirements: 3.3, 4.1, 5.1, 16.5_
  - [ ] 3.2 Implement `ProcessRunner.stream(_:args:timeout:lineHandler:)` async variant
    - Call `lineHandler` for each stdout line as it arrives
    - Apply same timeout and termination logic as `run()`
    - _Requirements: 4.3, 16.5_

- [ ] 4. Swift Core Engine — `detectDevice` step
  - [ ] 4.1 Implement `F3arRa1nEngine.detectDevice(sessionId:)`
    - Run `system_profiler SPUSBDataType` to check for VID `0x05AC` / PID `0x1227` (DFU)
    - Run `idevice_id -l` to enumerate normal-mode devices
    - Run `ideviceinfo` to read `ChipID`, `ProductVersion`, `SerialNumber`, `UniqueDeviceID`
    - Emit `device_found` event; emit `noDevice` error if nothing found within 5 s
    - Set `isCheckm8` based on CPID membership in `CHIP_CONFIGS`
    - Handle missing `ChipID` field: emit `cpid: "unknown"`, `chip_name: "Unknown"`, `is_checkm8: false`
    - _Requirements: 1.1–1.7_
  - [ ]\* 4.2 Write property test for `ideviceinfo` output parsing round-trip (SwiftCheck)
    - **Property 3: ideviceinfo output parsing round-trip**
    - **Validates: Requirements 1.2**

- [ ] 5. Swift Core Engine — `enterDfu` step
  - [ ] 5.1 Implement `F3arRa1nEngine.enterDfu(cpid:sessionId:)`
    - Invoke `palera1n --dfuhelper` with 60 s timeout
    - Emit `dfu_guide` event with `chip`, `timing`, `mode` from `CHIP_CONFIGS`
    - Emit `dfu_ok` if exit code == 0 OR stdout contains "DFU" (case-insensitive)
    - Emit `dfu_warn` (non-fatal) otherwise; pipeline continues
    - Skip this step entirely when `isDfu == true`
    - _Requirements: 2.1–2.6_
  - [ ]\* 5.2 Write property test for DFU tool output → dfu_ok vs dfu_warn (SwiftCheck)
    - **Property 4: DFU tool output determines dfu_ok vs dfu_warn**
    - **Validates: Requirements 2.2, 2.3**

- [ ] 6. Swift Core Engine — `runCheckm8` step
  - [ ] 6.1 Implement `F3arRa1nEngine.runCheckm8(cpid:sessionId:)`
    - Guard: throw `notCheckm8Vulnerable(cpid)` if CPID not in `CHIP_CONFIGS`
    - Guard: verify DFU mode (VID `0x05AC`, PID `0x1227`) before invoking gaster
    - Invoke `gaster pwn` with 30 s timeout
    - Emit `checkm8_ok` if exit code == 0 OR stdout contains "PWND"/"pwned" (case-insensitive)
    - On non-timeout failure: wait 1 s, retry exactly once
    - On timeout failure: emit `gasterFailed` immediately, no retry
    - On both-attempts failure: emit `gasterFailed` with `stderr.suffix(200)` (fallback to `stdout.suffix(200)`)
    - _Requirements: 3.1–3.9, 16.1, 16.2, 16.6_
  - [ ]\* 6.2 Write property test for checkm8 success detection (SwiftCheck)
    - **Property 6: checkm8 success detection**
    - **Validates: Requirements 3.1, 3.4, 16.1, 16.2**
  - [ ]\* 6.3 Write property test for gasterFailed error message truncation (SwiftCheck)
    - **Property 7: gasterFailed error message truncation**
    - **Validates: Requirements 3.7**

- [ ] 7. Swift Core Engine — `bootRamdisk` step
  - [ ] 7.1 Implement `F3arRa1nEngine.bootRamdisk(iosVersion:sessionId:)`
    - Invoke `palera1n --no-colors -e rootdev=md0 --skip-fakefs` via `stream()` with 180 s timeout
    - Emit `ramdisk_line` for every stdout line received
    - Emit `ramdisk_ok` with parsed iOS version if exit code == 0 OR stdout contains "done"/"success"
    - Emit `ramdiskFailed` with `stderr.suffix(300)` (fallback to `stdout.suffix(300)`) on failure
    - _Requirements: 4.1–4.5_
  - [ ]\* 7.2 Write property test for ramdisk success detection (SwiftCheck)
    - **Property 8: ramdisk success detection**
    - **Validates: Requirements 4.2**
  - [ ]\* 7.3 Write property test for ramdisk streaming line count (SwiftCheck)
    - **Property 9: ramdisk streaming line count**
    - **Validates: Requirements 4.3**
  - [ ]\* 7.4 Write property test for ramdiskFailed error message truncation (SwiftCheck)
    - **Property 10: ramdiskFailed error message truncation**
    - **Validates: Requirements 4.4**

- [ ] 8. Swift Core Engine — `patchActivation` step
  - [ ] 8.1 Implement `F3arRa1nEngine.patchActivation(udid:sessionId:)`
    - Method A: invoke `ideviceactivation activate` (no args) with 60 s timeout
    - Emit `activation_ok` with `method: "A"` and stop if exit code == 0 OR stdout contains "success"
    - Method B: invoke with UDID argument on Method A failure
    - Method C: invoke with debug flag on Method B failure
    - Emit `activation_partial` with `wifi: true` when all three methods fail (non-fatal)
    - Guard: emit `activation_error` with "activation tool not found" if binary missing from PATH
    - _Requirements: 5.1–5.6_
  - [ ]\* 8.2 Write property test for Activation Method A early exit (SwiftCheck)
    - **Property 11: Activation Method A early exit**
    - **Validates: Requirements 5.3**

- [ ] 9. Swift Core Engine — `runFullChain` orchestration
  - [ ] 9.1 Implement `F3arRa1nEngine.runFullChain(sessionId:)` actor method
    - Execute steps in fixed order: detectDevice → (enterDfu if !isDfu) → runCheckm8 → bootRamdisk → patchActivation
    - Emit `progress` events at each step: Detect=5%, DFU=15%, checkm8=30%, Ramdisk=55%, Activation=80%, Complete=100%
    - Emit `bypass_complete` on success with all required fields per Req 6.3
    - Catch all errors internally; emit structured error event; halt pipeline on `retryable: false`
    - Generate UUID session_id if caller does not supply one
    - _Requirements: 6.1–6.7, 16.3–16.5_
  - [ ]\* 9.2 Write property test for pipeline step ordering invariant (SwiftCheck)
    - **Property 12: Pipeline step ordering invariant**
    - **Validates: Requirements 6.1, 6.2**
  - [ ]\* 9.3 Write property test for fatal error stops the pipeline (SwiftCheck)
    - **Property 13: Fatal error stops the pipeline**
    - **Validates: Requirements 6.4, 16.1–16.10**
  - [ ]\* 9.4 Write property test for progress percentage monotonicity (SwiftCheck)
    - **Property 14: Progress percentage monotonicity**
    - **Validates: Requirements 6.5**

- [ ] 10. Checkpoint — Swift Core Engine
  - Ensure all Swift unit tests and property tests pass, ask the user if questions arise.

- [ ] 11. Rust Tauri Bridge — `f3arrain.rs`
  - [ ] 11.1 Implement `F3arError` struct and `swift()` path resolver in `f3arrain.rs`
    - Define `F3arError { layer: String, reason: String, retryable: bool }` with `Serialize`
    - Implement `swift(app: &AppHandle) -> String` resolving binary from app resource directory
    - Return `F3arError { layer: "SPAWN", reason: "binary not found at <path>", retryable: false }` if file missing
    - _Requirements: 14.4, 14.5, 14.11_
  - [ ] 11.2 Implement `run_cmd()` stdout parsing and event collection
    - Spawn Swift binary, read stdout line by line
    - Include only lines starting with `{`; parse each as `serde_json::Value`
    - Return `F3arError { layer: "PARSE" }` on invalid JSON in a `{`-prefixed line
    - Return `Ok(events)` (partial result) on non-zero exit if events were collected
    - Return `F3arError { layer: "SPAWN" }` on OS spawn failure
    - _Requirements: 14.6–14.10_
  - [ ] 11.3 Implement `f3arrain_full`, `f3arrain_detect`, `f3arrain_checkm8` Tauri commands
    - Wire each command to `run_cmd()` with the correct Swift subcommand and args
    - Register all three commands in Tauri's command handler
    - _Requirements: 14.1–14.3_
  - [ ]\* 11.4 Write property test for Tauri bridge JSON line filtering (fast-check)
    - **Property 20: Tauri bridge JSON line filtering**
    - **Validates: Requirements 14.6, 14.7**

- [ ] 12. Android — `AppleDeviceMatrix` and chip support data
  - [ ] 12.1 Implement `AppleDeviceMatrix` object in Kotlin
    - Define `AppleChip` enum with all values: A5–A18, UNKNOWN
    - Populate `DEVICE_CHIP_MAP: Map<String, AppleChip>` with all "iPhoneX,Y" identifiers per Req 12.2
    - Populate `CHECKM8_SUPPORTED: Set<AppleChip>` with A7–A11
    - Implement `getChip(identifier: String): AppleChip` — return UNKNOWN for missing/malformed identifiers
    - Implement `isCheckm8Supported(identifier: String): Boolean`
    - _Requirements: 12.1–12.7_
  - [ ]\* 12.2 Write property test for device identifier chip lookup (kotest-property)
    - **Property 18: Device identifier chip lookup**
    - **Validates: Requirements 12.3, 12.4, 12.5, 12.7**

- [ ] 13. Android — `F3arrainExecutor` USB exploit
  - [ ] 13.1 Implement `F3arrainExecutor.runExploit()` precondition checks
    - Check `detectAppleMode() == DeviceMatrix.AppleMode.DFU`; return `Error` with product ID if not
    - Check `device.interfaceCount == 1`; return `Error` with actual count if not
    - Attempt `usbManager.openDevice(device)`; return `Error` on null
    - _Requirements: 7.1–7.5, 16.9_
  - [ ] 13.2 Implement USB control-transfer exploit loop (up to 3 attempts)
    - Send DFU_DNLOAD max packet (0x800 bytes, bmRequestType=0x21, bRequest=1), delay 20 ms
    - Send DFU_DNLOAD ZLP (0 bytes), delay 20 ms
    - Send DFU_GETSTATUS (bmRequestType=0xA1, bRequest=3), delay 100 ms
    - Wait 150 ms re-enumeration settle
    - Check `device.interfaceCount == 5`; return `PwnedDfu` with chip string on success
    - After 3 failed attempts return `Error("checkm8 failed — not pwned")`
    - Close USB connection in `finally` block regardless of outcome
    - _Requirements: 7.6–7.11, 17.1–17.3_
  - [ ]\* 13.3 Write property test for Android DFU mode precondition (kotest-property)
    - **Property 15: Android DFU mode precondition**
    - **Validates: Requirements 7.1, 7.2, 16.9**
  - [ ]\* 13.4 Write property test for Android interface count precondition (kotest-property)
    - **Property 16: Android interface count precondition**
    - **Validates: Requirements 7.3, 7.4, 16.9**
  - [ ]\* 13.5 Write property test for Android exploit retry count (kotest-property)
    - **Property 17: Android exploit retry count**
    - **Validates: Requirements 7.10, 7.11**

- [ ] 14. Checkpoint — Android and Rust layers
  - Ensure all Kotlin and Rust unit tests and property tests pass, ask the user if questions arise.

- [ ] 15. Android — `UnifiedBypassRegistry` F3arRa1n entries
  - [ ] 15.1 Register `F3ARRAIN_HELLO_BYPASS` in `UnifiedBypassRegistry`
    - Set `source = FeatureSource.F3ARRAIN`, `isFree = true`, `costCredits = 0`
    - Set `chipRange` A7–A11, `iosRange` "12.0–16.7.8", `isUntethered = false`, `signalAfter = false`
    - Add tags: "checkm8", "f3arrain", "hello", "activation", "a7"–"a11", "ios12", "ios16"
    - _Requirements: 8.1–8.7, 18.1–18.3, 18.5_
  - [ ] 15.2 Register `F3AR_A12_FREE` in `UnifiedBypassRegistry`
    - Set `isFree = true`, `costCredits = 0`, `chipRange` A12–A18, `iosRange` "15.0–26.1"
    - Set `isUntethered = true`, `signalAfter = false`, `requiresInternet = true`
    - Add tags: "f3arrain", "hello", "a12"–"a18", "ios15", "ios26"
    - _Requirements: 9.1–9.12, 18.1–18.3, 18.5_
  - [ ] 15.3 Register `F3AR_TEMP_TEST` in `UnifiedBypassRegistry`
    - Set `isFree = true`, `costCredits = 0`, `chipRange` A7–A18, `iosRange` "12.0–26.1"
    - Set `isUntethered = false`, `requiresDfu = false`, `requiresInternet = false`
    - Add tags: "f3arrain", "temp", "test", "a7"–"a18", "ios12"
    - _Requirements: 10.1–10.9, 18.1–18.3, 18.5_
  - [ ] 15.4 Register `F3AR_BOOT_FILES` in `UnifiedBypassRegistry`
    - Set `isFree = false`, `costCredits = 10`, `chipRange` A7–A11, `iosRange` "15.0–16.7"
    - Set `isUntethered = true`, `signalAfter = true`, `requiresDfu = true`
    - Add tags: "checkm8", "f3arrain", "boot-files", "nvram", "a7"–"a11", "ios15", "ios16"
    - _Requirements: 11.1–11.11, 18.1–18.3, 18.6_
  - [ ]\* 15.5 Write property test for registry free feature selection (kotest-property)
    - **Property 22: Registry free feature selection**
    - **Validates: Requirements 18.4, 18.5, 18.6**

- [ ] 16. Frontend — `F3arRa1nPanel.tsx` Zod schema and state
  - [ ] 16.1 Define `EventSchema` (Zod), `Phase` type, and `PanelState` interface in `F3arRa1nPanel.tsx`
    - Implement `EventSchema` with all optional fields per design spec
    - Define `Phase` union type and `PanelState` interface
    - Implement `handleEvent()` that calls `EventSchema.safeParse()` and discards invalid events silently
    - Cap event log at 200 entries
    - _Requirements: 15.8, 15.9_
  - [ ]\* 16.2 Write property test for frontend event schema validation (fast-check)
    - **Property 21: Frontend event schema validation**
    - **Validates: Requirements 15.9**

- [ ] 17. Frontend — `F3arRa1nPanel.tsx` UI rendering
  - [ ] 17.1 Implement 6-step pipeline indicator component
    - Render steps: Detect → DFU → checkm8 → Ramdisk → Bypass → Done
    - Active step: cyan color + spinning loader icon
    - Completed step: green color + checkmark icon
    - _Requirements: 15.1, 15.2, 15.3_
  - [ ] 17.2 Implement device card, progress bar, and event log
    - Device card: chip name, iOS version, CPID, checkm8 compatibility badge
    - Progress bar: current `pct` value + phase label, updated on each `progress` event
    - Scrollable event log: color-coded entries (green=success, red=error, yellow=warn, white=info)
    - _Requirements: 15.4, 15.5, 15.8_
  - [ ] 17.3 Implement result card, error card, and control buttons
    - Green result card with bypass notes on `bypass_complete`
    - Red error card showing `[layer] reason` on error event
    - "▶ START F3ARRA1N" button in idle state
    - "↻ RETRY" button (re-invoke with new session_id) when `retryable: true`
    - "RESET" button clears all state and returns to idle
    - _Requirements: 15.6, 15.7, 15.10, 15.11, 13.7_
  - [ ] 17.4 Wire `invoke('f3arrain_full')` call and event dispatch loop
    - Call `invoke('f3arrain_full', { sessionId })` on start
    - Iterate returned `Vec<Value>`, dispatch each through `handleEvent()`
    - Handle Tauri invoke rejection in `try/catch`; display error card
    - _Requirements: 15.6, 15.7, 15.10_

- [ ] 18. Integration wiring — connect all three layers
  - [ ] 18.1 Wire Swift binary CLI entry point to `F3arRa1nEngine`
    - Parse CLI args: `f3arrain <sessionId>`, `f3arrain detect <sessionId>`, `f3arrain checkm8 <sessionId>`
    - Dispatch to `runFullChain`, `detectDevice`, or `runCheckm8` accordingly
    - _Requirements: 6.1, 6.7, 14.1–14.3_
  - [ ] 18.2 Wire Rust Tauri commands into the app's command registry
    - Add `f3arrain_full`, `f3arrain_detect`, `f3arrain_checkm8` to `tauri::Builder::invoke_handler`
    - Verify binary resource path is bundled in `tauri.conf.json`
    - _Requirements: 14.1–14.4_
  - [ ] 18.3 Wire `F3arrainExecutor` into Android app's DI graph and USB permission flow
    - Inject `UsbManager` via Hilt; request USB permission before calling `runExploit()`
    - Connect `onProgress` callback to UI progress indicator
    - _Requirements: 7.1, 16.9_

- [ ] 19. Final Checkpoint — full integration
  - Ensure all tests pass across Swift, Rust, Kotlin, and TypeScript layers, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use SwiftCheck (Swift), kotest-property (Kotlin), fast-check (TypeScript) — minimum 100 iterations each
- All property test files must include the tag comment: `// Feature: f3arrain-features, Property <N>: <property_text>`
- Checkpoints ensure incremental validation across all three language layers
- The Swift binary must be built as a macOS CLI target (`deepeye-core`) and bundled in the Tauri app's resource directory
- All USB delays in `F3arrainExecutor` must use only the approved values from Req 17: 20 ms, 100 ms, 150 ms, 500 ms, 2000 ms, 3000 ms

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2"] },
    { "id": 1, "tasks": ["1.2", "1.3", "2.3", "2.4", "3.1", "12.1"] },
    { "id": 2, "tasks": ["3.2", "4.1", "12.2"] },
    { "id": 3, "tasks": ["4.2", "5.1", "13.1"] },
    { "id": 4, "tasks": ["5.2", "6.1", "13.2"] },
    { "id": 5, "tasks": ["6.2", "6.3", "7.1", "13.3", "13.4"] },
    { "id": 6, "tasks": ["7.2", "7.3", "7.4", "8.1", "13.5"] },
    { "id": 7, "tasks": ["8.2", "9.1"] },
    { "id": 8, "tasks": ["9.2", "9.3", "9.4", "11.1"] },
    { "id": 9, "tasks": ["11.2", "15.1", "15.2", "15.3", "15.4", "16.1"] },
    { "id": 10, "tasks": ["11.3", "11.4", "15.5", "16.2", "17.1"] },
    { "id": 11, "tasks": ["17.2", "17.3"] },
    { "id": 12, "tasks": ["17.4", "18.1"] },
    { "id": 13, "tasks": ["18.2", "18.3"] }
  ]
}
```
