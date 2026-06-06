# DeepEye OTG — Gap Analysis vs Master Spec

**Generated**: PerceptiveBuilder-AI gap pass vs God Prompt (26 stages, 4 phases)

---

## 1. PROJECT STRUCTURE

| Spec                                                                                                             | Current                | Gap                                    |
| ---------------------------------------------------------------------------------------------------------------- | ---------------------- | -------------------------------------- |
| Multi-module: `:core:usb`, `:core:session`, `:core:transport`, `:protocol:*`, `:feature:*`, `:data:*`, `:plugin` | Single `:app` module   | **REFACTOR**: Module split not started |
| `settings.gradle.kts` includes ~30 subprojects                                                                   | `include(":app")` only | **REFACTOR**: Add module declarations  |
| Root project name `DeepEyeOTG`                                                                                   | `DeepEyeUnlocker`      | **MINOR**: Align naming                |

---

## 2. DEPENDENCIES (libs.versions.toml)

| Spec                                                              | Current                                   | Gap                                        |
| ----------------------------------------------------------------- | ----------------------------------------- | ------------------------------------------ |
| Hilt, Room, DataStore, Retrofit, WorkManager, Timber, Moshi, Coil | Partial: compose-bom, lifecycle, core-ktx | **BUILD**: Add Hilt, Room, DataStore, etc. |
| Navigation Compose, Hilt Nav                                      | Not in catalog                            | **BUILD**: Add navigation                  |
| compose-bom 2024.02                                               | 2024.02.00                                | OK                                         |
| kotlin 1.9.22                                                     | 1.9.22                                    | OK                                         |
| agp 8.3.0                                                         | 8.2.2                                     | **MINOR**: Bump AGP                        |

---

## 3. ANDROID MANIFEST

| Spec                                      | Current                              | Gap                                      |
| ----------------------------------------- | ------------------------------------ | ---------------------------------------- |
| `android.hardware.usb.host` required=true | `uses-feature` present, not required | **MINOR**: Add `android:required="true"` |
| RECEIVE_BOOT_COMPLETED                    | Missing                              | **BUILD**: Add                           |
| USE_BIOMETRIC                             | Missing                              | **BUILD**: Add                           |
| WRITE/READ_EXTERNAL_STORAGE (maxSdk)      | Missing                              | **BUILD**: Add for legacy                |
| MANAGE_EXTERNAL_STORAGE (tools:ignore)    | Missing                              | **BUILD**: Add if needed                 |
| USB_DEVICE_ATTACHED (explicit)            | Implicit via receiver                | OK                                       |
| Android 13+ RECEIVER_NOT_EXPORTED         | Not checked                          | **AUDIT**: Verify receiver registration  |

---

## 4. USB DEVICE FILTER

| Spec                                                         | Current                     | Gap            |
| ------------------------------------------------------------ | --------------------------- | -------------- |
| Apple DFU 0x05AC:0x1227                                      | Missing                     | **BUILD**: Add |
| Apple Recovery 0x05AC:0x1281, 0x1282, 0x12A0, 0x12A8, 0x12AB | Missing                     | **BUILD**: Add |
| UNISOC FDL 0x1782:0x4D00, 0x3D00                             | Present (6018:19712, 15616) | OK             |
| MTK, QC, Samsung                                             | Present                     | OK             |

---

## 5. CORE USB LAYER (Stage 1)

| Spec                                                                              | Current              | Gap                                |
| --------------------------------------------------------------------------------- | -------------------- | ---------------------------------- |
| `UsbDescriptorSnapshot` with `vidPidHex()`, `shortDump()`, `isDegenerate()`       | Basic snapshot only  | **BUILD**: Add computed properties |
| `UsbInterfaceSnapshot` with `isExplicitAdb`, `hasBulkBidirectional`, `isAppleDfu` | Basic interface only | **BUILD**: Add computed properties |
| **detectApple()** first in pipeline (VID 0x05AC)                                  | Missing              | **BUILD**: Add Apple detection     |
| **detectUnisoc()** (VID 0x1782) before Samsung                                    | Missing              | **BUILD**: Add Unisoc detection    |
| Degenerate guard + 500ms retry                                                    | Missing              | **BUILD**: Add                     |
| Confidence threshold &lt; 50 → UNKNOWN                                            | Not enforced         | **BUILD**: Add                     |
| `ProtocolFamily`: APPLE_DFU, APPLE_RECOVERY, APPLE_NORMAL, CDC_SERIAL             | Missing              | **BUILD**: Add enum values         |
| `DeviceMode`: APPLE_DFU, APPLE_RECOVERY, APPLE_NORMAL, CDC_SERIAL                 | Missing              | **BUILD**: Add enum values         |

---

## 6. SESSION STATE MACHINE (Stage 2)

| Spec                                                                               | Current                                              | Gap                                          |
| ---------------------------------------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------- |
| `UsbEvent` sealed class (Attached, Detached, PermissionGranted, PermissionDenied)  | Implicit in receiver callbacks                       | **PARTIAL**: Logic present, type not unified |
| `UsbSessionState` with ChipInfo, SecureBootStatus, PermissionState, OperationState | `SessionState` in domain, `UsbLifecycleState` in usb | **PARTIAL**: Different shape, merge or map   |
| Application-scoped USB relay `MutableSharedFlow<UsbEvent>`                         | `UsbLifecycleManager` + `UsbBroadcastReceiver`       | **PARTIAL**: Different architecture          |
| 400ms debounce                                                                     | Implemented                                          | OK                                           |
| 10s permission timeout                                                             | Implemented                                          | OK                                           |
| deviceKey format                                                                   | Implemented                                          | OK                                           |

---

## 7. TRANSPORT ENGINE (Stage 3)

| Spec                                                                           | Current                                           | Gap                                      |
| ------------------------------------------------------------------------------ | ------------------------------------------------- | ---------------------------------------- |
| `UsbTransport` interface                                                       | `UsbTransferQueue` + `SafeBulkTransfer`           | **PARTIAL**: No shared interface         |
| `BulkTransport` class                                                          | Logic in `UsbTransferQueue`                       | **REFACTOR**: Extract to `BulkTransport` |
| `ControlTransport` for Apple DFU, Sahara                                       | Not present                                       | **BUILD**: Add                           |
| `TransferResult` sealed (Success, Timeout, DeviceGone, ProtocolError, IOError) | `BulkResult` + `TransferResult` (different shape) | **PARTIAL**: Align naming/shape          |

---

## 8. PROTOCOL MODULES (Stages 4–9)

| Stage      | Spec                                                        | Current                                     | Gap                                 |
| ---------- | ----------------------------------------------------------- | ------------------------------------------- | ----------------------------------- |
| 4 MTK      | BromSession, PreloaderSession, MetaSession, DaManager       | DaManager, PartitionAdapter, NvBridge stubs | **BUILD**: Full BROM/Preloader/META |
| 5 Qualcomm | SaharaSession, FirehoseSession, DiagSession                 | No protocol impl                            | **BUILD**: From scratch             |
| 6 Samsung  | OdinSession, PIT parser                                     | No protocol impl                            | **BUILD**: From scratch             |
| 7 ADB      | AdbSession, AdbSyncChannel, AdbFeatures                     | No pure Kotlin ADB client                   | **BUILD**: From scratch             |
| 8 Fastboot | FastbootSession, all getvar/flash/erase                     | No protocol impl                            | **BUILD**: From scratch             |
| 9 Apple    | AppleDfuSession, AppleRecoverySession, libimobiledevice NDK | No Apple support                            | **BUILD**: From scratch             |

---

## 9. FEATURE MODULES (Stages 10–18)

| Stage           | Spec                                                                   | Current                                    | Gap                                 |
| --------------- | ---------------------------------------------------------------------- | ------------------------------------------ | ----------------------------------- |
| 10 Backup       | BackupEngine, manifest, verify, restore                                | Partial (EngineDispatcher, BinaryDeployer) | **BUILD**: Full backup pipeline     |
| 11 Forensics    | ForensicAcquisition, CustodyLog, ReportGenerator                       | ForensicExplorer, SafeDumpDialog           | **BUILD**: Chain-of-custody, report |
| 12 Flash        | RomParser, BootImageTools, FlashPipeline                               | Partial                                    | **BUILD**: Full flash pipeline      |
| 13 Security     | SecurityAudit, RootDetection (22 methods), AttestationAnalyzer, VulnDB | PolicyEngine, HWIDEngine                   | **BUILD**: Full audit suite         |
| 14 Terminal     | TerminalSession, CommandHistory, ScriptEngine                          | No terminal                                | **BUILD**: From scratch             |
| 15 Monitor      | LogcatMonitor, SystemMonitor, NetworkMonitor                           | No monitor                                 | **BUILD**: From scratch             |
| 16 App Manager  | listPackages, extractApk, DebloatProfile                               | No dedicated module                        | **BUILD**: From scratch             |
| 17 Network      | WirelessAdb, PortForwardManager, SOCKS                                 | TunnelManager                              | **PARTIAL**: Extend                 |
| 18 File Manager | DeviceFileManager, FileEntry                                           | No file manager                            | **BUILD**: From scratch             |

---

## 10. PLUGIN SYSTEM (Stage 19)

| Spec                                                   | Current | Gap                     |
| ------------------------------------------------------ | ------- | ----------------------- |
| Plugin SDK, manifest format, `DeepEyePlugin` interface | None    | **BUILD**: From scratch |

---

## 11. UI (Stage 20)

| Spec                                  | Current                           | Gap                                                         |
| ------------------------------------- | --------------------------------- | ----------------------------------------------------------- |
| DeepEyeTokens (all mode accents)      | StitchTokens, GlassTokens partial | **PARTIAL**: Add APPLE_DFU, APPLE_RECOVERY, UNKNOWN accents |
| ModeCard, ActionGrid, StatusIndicator | GlassCard, DeviceIdentityCard     | **PARTIAL**: Align with spec                                |
| TerminalView, HexViewer               | Not present                       | **BUILD**: From scratch                                     |
| NavGraph with all routes              | MainScreen + some screens         | **PARTIAL**: Add missing routes                             |

---

## 12. DATA LAYER (Stage 21)

| Spec                                                     | Current                         | Gap                             |
| -------------------------------------------------------- | ------------------------------- | ------------------------------- |
| Room: DeviceRecord, SessionRecord, OperationRecord, etc. | No Room                         | **BUILD**: Add Room DB          |
| DataStore: AppPreferences                                | SettingsManager (likely custom) | **AUDIT**: Align with DataStore |
| EncryptedStorage (AES-256-GCM, Keystore)                 | Not present                     | **BUILD**: Add                  |

---

## 13. BACKGROUND OPS (Stage 22)

| Spec                                    | Current              | Gap                                     |
| --------------------------------------- | -------------------- | --------------------------------------- |
| UsbOperationService (ForegroundService) | UsbForegroundService | **PARTIAL**: Verify type + notification |
| BackupWorker, VulnDbUpdateWorker        | No WorkManager       | **BUILD**: Add                          |

---

## 14. ACCEPTANCE TESTS (T01–T30)

| Tests                                             | Status                                                                |
| ------------------------------------------------- | --------------------------------------------------------------------- |
| T01–T10 (detection, UNKNOWN, re-enum, permission) | **PARTIAL**: ProtocolDetectorTest, UsbLifecycleManagerTest cover many |
| T11–T20 (rotation, cable yank, R8, Android 14)    | **TODO**: Add instrumentation / manual matrix                         |
| T21–T30 (BROM dump, EDL flash, ADB backup, etc.)  | **TODO**: Requires protocol impl                                      |

---

## 15. EXECUTION PRIORITY

**Phase 1 (Weeks 1–4) — Align with spec:**

1. **Stage 0 bootstrap**: Update `libs.versions.toml`, manifest, usb filter, add Apple/Unisoc to detector.
2. **Stage 1 completion**: Add `detectApple()`, `detectUnisoc()`, degenerate guard, confidence threshold, computed props on snapshot.
3. **Stage 2 alignment**: Unify `UsbEvent` / `UsbSessionState` shape if needed; ensure ChipInfo/OperationState flow exists.
4. **Stage 7 ADB**: Highest daily value — implement pure Kotlin ADB wire protocol client.

**Phase 2 (Module split):**

- Create `:core:usb`, `:core:session`, `:core:transport` as library modules.
- Migrate `ProtocolDetector`, `UsbSnapshotFactory`, `UsbDescriptorSnapshot` → `:core:usb`.
- Migrate `UsbLifecycleManager`, `UsbSessionManager` → `:core:session`.
- Migrate `SafeBulkTransfer`, `UsbTransferQueue` → `:core:transport`.

---

## 16. REFACTOR PLAN (MODULE SPLIT)

```
Step 1: Create core modules (empty)
  - core/usb/build.gradle.kts
  - core/session/build.gradle.kts
  - core/transport/build.gradle.kts

Step 2: Move code
  - app/.../usb/{ProtocolDetector, UsbSnapshotFactory, UsbDescriptorSnapshot, ...} → core/usb
  - app/.../usb/{UsbLifecycleManager, UsbSessionManager, UsbLifecycleState} → core/session
  - app/.../usb/{SafeBulkTransfer, UsbTransferQueue} → core/transport

Step 3: Fix dependencies
  - core/session depends on core/usb
  - core/transport depends on core/usb (or android SDK only)
  - app depends on core/session, core/transport

Step 4: Protocol modules (later)
  - protocol/mtk, protocol/qualcomm, protocol/samsung, protocol/adb, protocol/fastboot, protocol/apple
```

---

_End of gap analysis_
