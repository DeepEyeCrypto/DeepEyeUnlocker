# DeepEye Unlocker - Module Dependency Graph

**Generated:** April 21, 2026  
**Graph Type:** Complete dependency and data flow visualization

---

## 📊 Module Hierarchy & Relationships

```
DeepEyeUnlocker System
├── ExploitOrchestrator (Main Coordinator)
│   ├── TicketEngine (License/Device Verification)
│   ├── BypassEngine (Core Bypass Logic)
│   │   ├── BypassAdvanced (Specialized Techniques)
│   │   ├── RamdiskMaster (Ramdisk Control)
│   │   └── IdentityForensics (Device Identity)
│   ├── Device Protocol Handlers
│   │   ├── AdbTerminal (ADB Protocol)
│   │   ├── DfuRestore (DFU Mode - iOS)
│   │   └── [Native C++ Protocols]
│   └── Data Extraction Pipeline
│       ├── DeepExtraction (Deep Data Access)
│       ├── DeepVaultExport (Secure Vault)
│       ├── IOSBackup (iOS Backup Format)
│       └── IOSAnalysis (Analytics & Reporting)
├── Apple-Specific Modules
│   ├── ActivationLock (iCloud Lock Bypass)
│   ├── AppleIdRemoval (AppleID Credentials)
│   ├── ScreenTimeCrack (Screen Time Bypass)
│   └── DfuRestore (DFU Restore Protocol)
└── Analysis & Forensics
    ├── MdmAnalysis (Mobile Device Management)
    └── IdentityForensics (Device Signatures)
```

---

## 🔗 Data Flow Between Modules

### Phase 1: Device Detection & Connection
```
Physical Device (USB)
    ↓
[USB Transport Layer]
    ↓
Device Detection Engine
    ├─→ IdentityForensics (Identify device model)
    ├─→ TicketEngine (Verify license/device compatibility)
    └─→ Protocol Selection Logic
```

### Phase 2: Protocol Handshake
```
Protocol Selection
    ├─→ [For Android] AdbTerminal → USB Protocol
    ├─→ [For iOS] DfuRestore → DFU Protocol
    └─→ [For Locked] ExploitOrchestrator → Sahara/Brom/Odin/FDL
    
USB Handshake & Authentication
    ↓
Signal Integrity Check (Eye-Diagram Analysis)
    ↓
Device State Validation
```

### Phase 3: Bypass Execution
```
ExploitOrchestrator
    ├─→ BypassEngine (Core bypass logic)
    │   ├─→ BypassAdvanced (Special techniques)
    │   ├─→ RamdiskMaster (Ramdisk injection)
    │   └─→ IdentityForensics (Device spoofing)
    ├─→ ActivationLock (iCloud removal)
    ├─→ AppleIdRemoval (AppleID bypass)
    └─→ ScreenTimeCrack (Parental control bypass)
```

### Phase 4: Data Extraction
```
Extraction Pipeline
    ├─→ DeepExtraction (Core data access)
    ├─→ AES-256 Decryption Engine
    ├─→ DeepVaultExport (Secure storage access)
    ├─→ IOSBackup (Backup format handling)
    └─→ IOSAnalysis (Analysis & reporting)
```

---

## 📦 Module Specifications

### 1. **ExploitOrchestrator** (Main Hub)
- **Purpose**: Orchestrates entire forensic workflow
- **Dependencies**: All other modules
- **Status**: Production
- **Responsibility**: Execution sequencing and error handling

### 2. **TicketEngine**
- **Purpose**: Device ticket verification and licensing
- **Dependencies**: Device database, licensing service
- **Status**: Production
- **Input**: Device model, serial, IMEI
- **Output**: Verification status, compatibility flags

### 3. **BypassEngine** (Core)
- **Purpose**: Core bypass logic orchestration
- **Dependencies**: BypassAdvanced, RamdiskMaster, IdentityForensics
- **Status**: Production
- **Techniques**: Lock bypass, encryption bypass, signature spoofing

### 4. **BypassAdvanced**
- **Purpose**: Specialized bypass techniques
- **Dependencies**: BypassEngine, device-specific modules
- **Status**: Production
- **Specializations**: A-series, Snapdragon, Dimensity chipsets

### 5. **ActivationLock** (Apple)
- **Purpose**: iCloud Activation Lock removal
- **Dependencies**: AppleIdRemoval, DfuRestore
- **Status**: Production
- **Protocols**: APNS, X.509, RSA handshakes

### 6. **AppleIdRemoval** (Apple)
- **Purpose**: Remove AppleID credentials
- **Dependencies**: Keychain access, encryption keys
- **Status**: Production
- **Data Access**: Secure enclave, TEE

### 7. **ScreenTimeCrack** (Apple)
- **Purpose**: Screen Time parental control bypass
- **Dependencies**: Keychain, passcode storage
- **Status**: Production

### 8. **DfuRestore** (iOS Protocol)
- **Purpose**: DFU mode restoration and control
- **Dependencies**: USB protocol layer, firmware
- **Status**: Production
- **Protocol**: Apple DFU communication protocol

### 9. **AdbTerminal** (Android Protocol)
- **Purpose**: ADB shell interface and commands
- **Dependencies**: ADB protocol, device auth
- **Status**: Production
- **Commands**: Shell execution, file transfer

### 10. **DeepExtraction**
- **Purpose**: Deep data extraction from devices
- **Dependencies**: Decryption engines, file system access
- **Status**: Production
- **Data Types**: Messages, contacts, media, app data

### 11. **DeepVaultExport**
- **Purpose**: Secure vault and encrypted storage access
- **Dependencies**: Key extraction, AES-256
- **Status**: Production
- **Storage Types**: Encrypted partitions, secure folders

### 12. **IOSBackup**
- **Purpose**: iOS backup format handling and reconstruction
- **Dependencies**: Encryption, manifest parsing
- **Status**: Production
- **Format**: iOS backup mbdx format

### 13. **IOSAnalysis**
- **Purpose**: iOS-specific forensic analysis and reporting
- **Dependencies**: DeepExtraction, IOSBackup
- **Status**: Production
- **Analysis**: Device state, user activity, forensic artifacts

### 14. **RamdiskMaster**
- **Purpose**: Ramdisk injection and kernel module control
- **Dependencies**: Bootloader, kernel interfaces
- **Status**: Production
- **Capabilities**: Custom ramdisk loading, kernel patching

### 15. **MdmAnalysis**
- **Purpose**: Mobile Device Management policy analysis
- **Dependencies**: Device config parsing
- **Status**: Production
- **Policies**: Device restrictions, network policies

### 16. **IdentityForensics**
- **Purpose**: Device identity and signature forensics
- **Dependencies**: Key extraction, cryptographic verification
- **Status**: Production
- **Analysis**: Serial validation, IMEI verification, signature spoofing

---

## 🔄 Communication Patterns

### Synchronous Calls
```
Module A → Module B (blocking operation)
    - TicketEngine → Device Database
    - BypassEngine → AdbTerminal
    - ExploitOrchestrator → All execution modules
```

### Asynchronous Events
```
Module A → Event Bus → Module B (non-blocking)
    - Device status updates
    - Extraction progress
    - Error notifications
```

### Shared State
```
ExploitOrchestrator maintains global state:
    - Current device info
    - Bypass status
    - Extraction progress
    - Error log
```

---

## 🎯 Critical Path Analysis

### Longest Dependency Chain
```
ExploitOrchestrator
  → ActivationLock
    → AppleIdRemoval
      → Secure Enclave Access
        → AES-256 Decryption
          → Key Material
```

### Parallelizable Operations
```
These can run concurrently:
- DeepExtraction (data extraction)
- MdmAnalysis (policy analysis)
- IOSAnalysis (forensic analysis)
- IdentityForensics (identity verification)
```

---

## 🏥 Dependency Health

### Hard Dependencies (Cannot skip)
1. ExploitOrchestrator
2. Device identification
3. Protocol selection
4. USB communication layer

### Soft Dependencies (Can be bypassed)
1. TicketEngine (licensing can be disabled for testing)
2. MdmAnalysis (optional reporting)
3. IOSAnalysis (optional analysis)

---

## 📊 Module Statistics

| Module | Type | Complexity | Status | Entry Points |
|--------|------|-----------|--------|--------------|
| ExploitOrchestrator | Coordinator | High | Production | 1 main entry |
| TicketEngine | Service | Medium | Production | Device verify |
| BypassEngine | Core | Very High | Production | Bypass execution |
| ActivationLock | Protocol | High | Production | iCloud unlock |
| DeepExtraction | Pipeline | High | Production | Data extraction |
| AdbTerminal | Protocol | Medium | Production | Command execution |
| DfuRestore | Protocol | High | Production | iOS restoration |
| IOSBackup | Parser | Medium | Production | Backup processing |

---

## 🔐 Security Boundaries

```
Trust Boundary 1: Device Authentication
    USB Protocol ↔ Device Handshake
    
Trust Boundary 2: Data Encryption
    Raw Data ↔ AES-256 Decryption
    
Trust Boundary 3: Device Integrity
    Signal Processing ↔ Tamper Detection
```

---

## 📋 Module Checklist for Development

- [ ] Changes to ExploitOrchestrator reviewed by all module teams
- [ ] Protocol changes validated against device database
- [ ] Cryptographic changes reviewed for security implications
- [ ] Signal processing changes tested with eye-diagram validation
- [ ] Module integration tested end-to-end before release

---

**End of Module Dependency Graph**
