# DeepEye Unlocker - Comprehensive System Architecture

**Generated**: April 21, 2026  
**System Version**: 2027.18.1 (Stage 600.1 - Shielded)

---

## 📐 System Architecture Overview

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                  USER INTERFACE LAYER                    ┃
┃  Jetpack Compose (Android) | React (Web) | Tauri (macOS) ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃              ORCHESTRATION & STATE LAYER                 ┃
┃         ExploitOrchestrator (Central Hub)                ┃
┃   - Device Management    - Workflow Sequencing          ┃
┃   - Error Handling       - Progress Tracking            ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃              FUNCTIONAL MODULES LAYER                    ┃
┃  ┌─────────────────────────────────────────────────┐   ┃
┃  │ Bypass Module Suite:                            │   ┃
┃  │ ├─ BypassEngine (core logic)                    │   ┃
┃  │ ├─ BypassAdvanced (special techniques)          │   ┃
┃  │ └─ RamdiskMaster (kernel injection)             │   ┃
┃  ├─ Apple-Specific Suite:                         │   ┃
┃  │ ├─ ActivationLock (iCloud removal)              │   ┃
┃  │ ├─ AppleIdRemoval (credentials)                 │   ┃
┃  │ ├─ ScreenTimeCrack (parental control)           │   ┃
┃  │ └─ DfuRestore (DFU protocol)                    │   ┃
┃  ├─ Protocol Suite:                               │   ┃
┃  │ ├─ AdbTerminal (ADB shell)                      │   ┃
┃  │ └─ [Native protocols in C++ layer]              │   ┃
┃  └─ Extraction Suite:                             │   ┃
┃      ├─ DeepExtraction (data mining)               │   ┃
┃      ├─ DeepVaultExport (secure vaults)            │   ┃
┃      ├─ IOSBackup (backup format)                  │   ┃
┃      └─ IOSAnalysis (forensics)                    │   ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃           JNI NATIVE BRIDGE (Java ↔ C++)                ┃
┃        - Type marshalling    - Memory management        ┃
┃        - Exception handling  - Thread coordination      ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃              C++17 CORE ENGINE LAYER                     ┃
┃  ┌──────────────────────────────────────────────┐      ┃
┃  │ Protocol Handlers (Sahara/Brom/Odin/FDL)    │      ┃
┃  │ - Device handshake                           │      ┃
┃  │ - State machine management                   │      ┃
┃  │ - Command serialization/deserialization      │      ┃
┃  ├──────────────────────────────────────────────┤      ┃
┃  │ Cryptographic Engine (AES-256, RSA-4096)    │      ┃
┃  │ - Key derivation                             │      ┃
┃  │ - Encryption/decryption                      │      ┃
┃  │ - Digital signatures                         │      ┃
┃  ├──────────────────────────────────────────────┤      ┃
┃  │ Signal Processing (Eye-Diagram Analysis)    │      ┃
┃  │ - Real-time USB signal monitoring            │      ┃
┃  │ - Impedance measurement                      │      ┃
┃  │ - Tamper detection                           │      ┃
┃  └──────────────────────────────────────────────┘      ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃           TRANSPORT & PROTOCOL LAYER                     ┃
┃  libusb 1.0.26 (Low-level USB communication)           ┃
┃  - Bulk transfers                - Interrupt transfers ┃
┃  - Control messages              - Sub-ms latency      ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃              PHYSICAL DEVICE LAYER                       ┃
┃  Target Android | Target iOS | Bootloader | Kernel     ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## 🔌 USB Protocol Stack

```
Application Layer (React/Kotlin UI)
    ↓
Orchestration Layer (ExploitOrchestrator)
    ↓
Device Abstraction Layer (Module interfaces)
    ↓
JNI Bridge Layer (Type marshalling)
    ↓
Protocol Implementation Layer (C++17)
    ├─ Qualcomm Sahara Protocol
    ├─ MTK Brom Protocol
    ├─ Samsung Odin Protocol
    ├─ UniSoc FDL Protocol
    └─ ADB Protocol (RSA-4096 encrypted)
    ↓
Transport Layer (libusb 1.0.26)
    ├─ Bulk Transfer (data)
    ├─ Interrupt Transfer (status)
    └─ Control Transfer (commands)
    ↓
Physical USB Interface (USB 2.0/3.0)
    ↓
Target Device Bootloader/Kernel
```

---

## 🔐 Cryptographic Architecture

### Key Material Management
```
Device Key Sources:
├─ RPMB (Replay Protected Memory Block)
│  └─ Hardware-backed TEE keys
├─ Secure Context (TEE Secure World)
│  └─ Cryptographic context storage
└─ Device Keystore
   └─ Application-level encryption keys

Key Derivation:
Device ← KDF (Key Derivation Function)
  ├─ Input: Device master key + salt + info
  └─ Output: AES-256 keys per partition
```

### Encryption Pipeline
```
Raw Data
    ↓
[AES-256-CBC or AES-256-CTR]
    ↓
Decrypted Data
    ↓
[Integrity Verification - SHA-256]
    ↓
Verified Data Output
```

### Digital Signature Architecture
```
ADB Authentication (RSA-4096):
1. Device generates RSA-4096 keypair
2. App signs challenge with private key
3. Device verifies with public key
4. Session established if verified

Firmware Verification:
1. Firmware blob includes signature
2. Public key extracted from device
3. Signature verified before flash
4. Prevents unauthorized firmware
```

---

## 🎯 Data Flow: Complete Acquisition Pipeline

### Stage 1: Device Detection & Identification
```
Physical USB Connection
    ↓
[USB Enumeration]
    ↓
Device VID/PID Detection
    ↓
[Device Database Lookup]
    ↓
SoC Identification (Qualcomm/MTK/Samsung/UniSoc)
    ↓
TicketEngine Validation (license check)
    ↓
Device Capabilities Assessment
    ↓
→ Protocol Selection Decision
```

### Stage 2: Protocol Handshake & Authentication
```
Protocol Selection
    ↓
[Based on SoC type]
    ├─ Qualcomm → Sahara Protocol
    ├─ MTK → Brom Protocol
    ├─ Samsung → Odin Protocol
    └─ UniSoc → FDL Protocol
    ↓
[USB Handshake Sequence]
    1. Send protocol handshake
    2. Device responds with ID block
    3. Exchange encryption keys
    4. Verify authentication
    ↓
[Signal Integrity Check]
    Eye-Diagram Analysis
    - USB signal quality
    - Impedance measurement
    - Tamper detection
    ↓
[Authentication Success/Failure Decision]
```

### Stage 3: Bypass Execution
```
ExploitOrchestrator
    ↓
Device State Query
    ├─ Bootloader version
    ├─ Security status
    ├─ Encryption state
    └─ Protection mechanisms
    ↓
[Select Bypass Technique]
    ├─ BypassEngine (core)
    ├─ BypassAdvanced (specialized)
    ├─ RamdiskMaster (kernel mod)
    ├─ ActivationLock (iCloud)
    └─ AppleIdRemoval (credentials)
    ↓
[Execute Bypass]
    1. Inject exploit chain
    2. Gain bootloader access
    3. Load custom ramdisk
    4. Modify system state
    5. Disable protections
    ↓
[Verification]
    Confirm bypass success
    Check device state
```

### Stage 4: Key Material Extraction
```
Bypass Complete → Device Unlocked
    ↓
[Access Secure Contexts]
    ├─ RPMB (TEE keys)
    ├─ Secure Enclave (iOS)
    └─ Secure Element
    ↓
[Extract Key Material]
    1. Query TEE key storage
    2. Retrieve encryption keys
    3. Extract device identifiers
    4. Dump secure context
    ↓
[Key Validation]
    Verify key integrity
    Test key material
```

### Stage 5: Data Decryption & Extraction
```
Decryption Engine
    ↓
[Initialize AES-256]
    Set encryption mode (CBC/CTR)
    Load decryption key
    ↓
[Decrypt Partitions]
    ├─ UserData (FBE encrypted)
    ├─ Adoptable Storage (SD card)
    ├─ System partitions
    └─ App data
    ↓
[Integrity Verification]
    SHA-256 hash comparison
    Checksum validation
    Data corruption detection
    ↓
[Format Normalization]
    Convert to standard formats
    Extract structured data
```

### Stage 6: Forensic Analysis & Export
```
DeepExtraction Pipeline
    ├─ Messages & communications
    ├─ Contact information
    ├─ Media files
    ├─ Application data
    └─ System artifacts
    ↓
[IOSAnalysis & MdmAnalysis]
    Device state analysis
    MDM policy extraction
    Forensic artifacts
    ↓
[Report Generation]
    Summary report
    Evidence documentation
    Hash verification
    ↓
[Export]
    ZIP/TAR archive
    Forensic format (E01)
    CSV reports
```

---

## 🛡️ Signal Integrity & Tamper Detection (Stage 600.1)

### Eye-Diagram Analysis
```
Real-time USB Signal Monitoring:
┌─────────────────────────────────────┐
│ USB Data Line (D+/D-) Signal        │
├─────────────────────────────────────┤
│ Eye Pattern Measurement:            │
│ • Rise time                         │
│ • Fall time                         │
│ • Jitter                            │
│ • Eye opening (margins)             │
│ • Signal-to-noise ratio             │
└─────────────────────────────────────┘
        ↓
┌─────────────────────────────────────┐
│ Impedance Measurement               │
├─────────────────────────────────────┤
│ • Characteristic impedance (90Ω)    │
│ • Return loss measurement           │
│ • Reflection coefficient            │
│ • Deviation from standard           │
└─────────────────────────────────────┘
        ↓
    Pass/Fail Decision
        ↓
    [Abort if anomalies detected]
```

### Tamper Detection Scenarios
```
Scenario 1: Hardware Interposer
- Signal reflection patterns change
- Impedance measurement shows deviation
- → Auto-disconnect

Scenario 2: MitM Attack
- Authentication keys don't match
- Protocol response invalid
- → Connection terminated

Scenario 3: Electromagnetic Interference
- Signal noise exceeds threshold
- Multiple bit errors detected
- → Retry or abort

Scenario 4: Cable Issue
- Impedance drift over time
- Intermittent signal loss
- → Connection warning/retry
```

---

## 🏢 Module Interface Architecture

### Module Interface Pattern
```
Each Module Implements:
┌─────────────────────────────┐
│ Module Interface            │
├─────────────────────────────┤
│ execute()                   │
│ validate()                  │
│ getStatus()                 │
│ onError()                   │
│ getProgress()               │
└─────────────────────────────┘
        ↓ Implements
┌─────────────────────────────┐
│ IForensicModule             │
├─────────────────────────────┤
│ → Device context            │
│ → Transport layer           │
│ → Crypto engine             │
│ → Signal monitor            │
└─────────────────────────────┘
```

### Inter-Module Communication
```
Synchronous RPC:
Module A → ExploitOrchestrator.invoke(Module B, params)
           ↓
           Module B executes
           ↓
           Returns result
           ↓
           Module A continues

Asynchronous Events:
Module A → EventBus.emit('device.status.changed', data)
           ↓
           Module B, C, D receive event
           ↓
           Each handles independently
```

---

## 🔄 State Machine: Main Workflow

```
[IDLE]
    ↓ Device connected
[DETECTING]
    ├─ USB enumeration
    ├─ Device identification
    └─ License validation
    ↓ Success
[AUTHENTICATING]
    ├─ Protocol handshake
    ├─ Key exchange
    └─ Signal integrity check
    ↓ Success
[EXPLOITING]
    ├─ Bypass injection
    ├─ Kernel modification
    └─ Security bypass
    ↓ Success
[EXTRACTING]
    ├─ Key extraction
    ├─ Partition decryption
    └─ Data mining
    ↓ Success
[ANALYZING]
    ├─ Forensic analysis
    ├─ Report generation
    └─ Export
    ↓ Complete
[IDLE] (ready for next device)

Error Paths:
[ANY STATE] → Device disconnected → [IDLE]
[ANY STATE] → Signal anomaly → [IDLE]
[ANY STATE] → Auth failure → [IDLE]
```

---

## 🎛️ Configuration & Settings Architecture

```
System Configuration Hierarchy:
┌─────────────────────────────────┐
│ Global Configuration            │
├─────────────────────────────────┤
│ • Protocol timeouts             │
│ • Signal thresholds             │
│ • Encryption modes              │
│ • Device database               │
└─────────────────────────────────┘
        ↑
    [Loaded by]
        ↓
┌─────────────────────────────────┐
│ User Preferences                │
├─────────────────────────────────┤
│ • Output format                 │
│ • Export location               │
│ • Bypass technique              │
│ • License key                   │
└─────────────────────────────────┘
        ↑
    [Overridden by]
        ↓
┌─────────────────────────────────┐
│ Runtime Parameters              │
├─────────────────────────────────┤
│ • Current device info           │
│ • Bypass status                 │
│ • Extract progress              │
│ • Error state                   │
└─────────────────────────────────┘
```

---

## 🔊 Error Handling Architecture

```
Error Detection Points:
1. Device Connection → USB layer
2. Protocol Communication → Transport layer  
3. Authentication → Crypto layer
4. Signal Processing → Monitoring layer
5. Data Extraction → Decryption layer

Error Classification:
CRITICAL
  - Device disconnected mid-extraction
  - Cryptographic verification failed
  - Signal anomaly detected (tamper)
  → Immediate abort, preserve state

RECOVERABLE
  - Protocol timeout (retry)
  - Signal noise (resample)
  - Partial data extraction (resume)
  → Retry with backoff

WARNING
  - Suboptimal signal quality
  - Slow data transfer
  - Device not optimized
  → Continue with notification
```

---

## 📊 Performance Characteristics

### USB Transfer Performance
```
Expected Throughput:
- USB 2.0: ~30-40 MB/s
- USB 3.0: ~100-150 MB/s
- USB 3.1: ~400+ MB/s

Latency:
- Command round-trip: 1-5ms (optimal)
- Handshake sequence: 100-500ms
- Full extraction: 10-60 min (device dependent)
```

### Cryptographic Performance
```
AES-256 Decryption:
- Hardware-accelerated: >1GB/s
- Software fallback: ~100MB/s

RSA-4096 Operations:
- Signing: ~50-100ms
- Verification: ~5-10ms
```

---

## 🎓 Platform-Specific Implementation Details

### Android Implementation
```
Java/Kotlin Layer:
- ViewModel state management
- UI rendering via Jetpack Compose
- ADB interface via shell

JNI Bridge:
- C++17 interop
- Memory marshalling
- Exception translation

C++ Core:
- Protocol handlers
- Cryptographic engines
- USB communication
```

### iOS Implementation
```
Tauri/Web Frontend:
- React components
- Tauri plugin calls
- Platform abstractions

Rust Backend (Tauri):
- USB communication
- File system access
- Process management

External Tools:
- libimobiledevice
- ideviceactivation
- DFU mode handling
```

---

## 🔐 Security Architecture Summary

```
Trust Boundaries:
1. Device Authentication
   ├─ RSA-4096 signature verification
   └─ Mutual authentication protocol

2. Data Integrity
   ├─ SHA-256 checksums
   ├─ AES-256 HMAC verification
   └─ Metadata validation

3. Signal Security
   ├─ Real-time tamper detection
   ├─ Eye-diagram analysis
   └─ Auto-disconnect on anomaly

4. Physical Security
   ├─ Hardware interposer detection
   ├─ Signal impedance monitoring
   └─ EMI/RFI shielding
```

---

**End of Architecture Documentation**
