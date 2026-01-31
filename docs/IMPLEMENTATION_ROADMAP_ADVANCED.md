# IMPLEMENTATION ROADMAP: Advanced Security & Diagnostics Features

## ROLE & OBJECTIVE

As the **Senior Android Security Architect**, this document outlines the systematic implementation of 15 advanced features for DeepEyeUnlocker.
This roadmap prevents scope creep, ensures architecture stability, and provides a clear "God Prompt" for each development phase.

---

## STAGE 1 – FEATURE LANDSCAPE & INTERDEPENDENCIES

### 1.1 – Map the 15 advanced features and their dependencies

**Tier 0 (Foundation – EXISTING):**

- Device detection (DeviceInfoManager)
- ADB/Fastboot/Qualcomm EDL connection (AdToolsManager, EdlOperations)
- Basic Partition enumeration (FirehoseOperations)

**Tier 1 (Core read-only diagnostics):**

- IMEI Reader (ADB + EDL validation)
- MAC Address Reader (Wifi + Bluetooth + Checksum)
- Battery Health Reader (Cycles, Capacity, Health %)
- Kernel & Bootloader Audit (Version, Security Patch, Config check)
- Fingerprint Logic (Reading props)

**Tier 2 (Backup/Restore Infrastructure):**

- Calibration Operations (Backup/Restore /persist, /nvdata)
- Partition Backup Engine (Selective, Compressed, Encrypted)
- Certificate Inspection
- OTA Compatibility Check

**Tier 3 (Active Modifications):**

- Fingerprint Spoofing (Magisk Prop generation)
- Custom ROM Helper (Compatibility DB + Flash logic)
- Hook Management (Frida/LSPosed script generation)
- Automated Workflows (Recorder/Player)

**Tier 4 (Analysis & Reporting):**

- CVE/Vulnerability Scanner (Local DB lookup)
- Fleet Analysis (Multi-device reporting)
- Live Device Monitoring (Real-time dashboard)
- Cloud Sync (Encrypted backup upload)

### 1.2 – Critical Dependencies

- **IMEI/MAC** -> Depends on reliable **Partition Read** (EDL/ADB).
- **Partition Restore** -> Depends on solid **Backup Verification** (Checksums).
- **Automation** -> Depends on atomic **Operations** (Tier 1-3) being stable.

---

## STAGE 2 – PHASE BREAKDOWN & TIMELINE

### Phase 1: Diagnostics Foundation (Weeks 1–4)

*Goal: Read-only health checks. Safe, high-value, no risk of bricking.*

- **Features**: IMEI Reader, MAC Reader, Battery Health, Kernel/Security Audit, **FRP Bypass Engine (2026)**. [DONE]
- **Deliverable**: Diagnostics Dashboard & Automated FRP Bypass Suite. [DONE]

### Phase 2: Backup & Safety Infrastructure (Weeks 5–10)

*Goal: Robust backup engine before ever allowing writes.*

- **Features**: Partition Enumeration (Visualization), Selective Backup, Encryption (AES-256-GCM), Restore Simulation (Dry-Run). [DONE]
- **Deliverable**: "Partition Backup Center" Tab with AES-secured streaming engine. [DONE]

### Phase 3: Active Modifications (Weeks 11–16)

*Goal: Experts-only write capabilities.*

- **Features**: Calibration Restore, Fingerprint Spoofing (Magisk), Frida Hooks, ROM Helper, Workflow Automation. [DONE]
- **Deliverable**: "Modifications" Tab (Locked behind Expert Mode) + Workflow Builder. [DONE]

### Phase 4: Analytics & Fleet (Weeks 17–22)

*Goal: Enterprise-grade reporting.*

- **Features**: CVE Scanner, Fleet Analysis, Live Monitor, Cloud Sync (Encrypted Upload), Driver Center Pro (v1.4.0). [DONE]
- **Deliverable**: Dashboard, PDF Reporting, Secure Cloud Vault & Universal Driver Installer. [DONE]

---

## STAGE 3 – EXECUTION "GOD PROMPTS"

Use these prompts to initialize each phase. Copy and paste them into the AI session when starting the phase.

### GOD PROMPT – PHASE 1 (Diagnostics)

```markdown
ROLE: Android Security Engineer & C# Specialist
TASK: Implement the "Device Health Center" module for DeepEyeUnlocker.
CONTEXT: We have existing ADB/EDL classes in `src/Operations` and `src/Protocols`.
OBJECTIVE: Create a read-only diagnostics suite that does NOT modify the device.

DELIVERABLES:
1. Architecture:
   - Create `src/Features/DeviceHealth/`
   - Define `IDeviceHealthReader` interface.
2. Components:
   - `ImeiReader.cs`: unique logic for ADB (`getprop`) vs EDL (Hex parse /persist partition). Implement Luhn validation.
   - `MacReader.cs`: WiFi/BT MAC extraction. Validate OUI.
   - `BatteryReader.cs`: Parse `/sys/class/power_supply` and `dumpsys batterystats`.
   - `KernelAudit.cs`: Parse `ro.build.version.security_patch`, kernel version, and `config.gz` for security flags.
3. UI:
   - Create `DeviceHealthPanel` (UserControl).
   - Display cards for each component with status indicators (Green/Red).
   - Add "Export Report" button (CSV/JSON).

CONSTRAINTS:
- Use existing `AdbToolsManager` for shell execution.
- Handle "Device Not Authorized" and "Root Required" gracefully.
- No writes allowed.
```

### GOD PROMPT – PHASE 2 (Backup Infrastructure)

```markdown
ROLE: Systems Architect & Crypto Specialist
TASK: Build the "Partition Backup Center" infrastructure.
CONTEXT: Phase 1 is complete. We need a safety net before writing data.
OBJECTIVE: Implement a cryptographically secure, verified partition backup system.

DELIVERABLES:
1. Core Logic (`src/Features/PartitionBackup/`):
   - `PartitionMetadataCollector`: Abstraction to unify GPT (EDL) and mounts (ADB) into `PartitionInfo` objects.
   - `BackupEngine`: Stream-based reader.
     - Compression: GZipStream.
     - Encryption: AES-256-GCM (Key derived from device serial).
     - Integrity: Calculate SHA-256 hash *during* stream read.
2. UI (`BackupPanel`):
   - Partition List with checkboxes (Sort by size/importance).
   - Presets: "EFS/IMEI Only", "Full Dump".
   - Progress bar with estimated time remaining.
3. Verification:
   - `RestoreSimulator`: A class that validates a backup file's header, decrypts it in memory (chunked), and verifies the checksum without writing to disk.

CONSTRAINTS:
- Low memory footprint (stream everything).
- Fail-safe: If disconnected, delete partial files.
```

### GOD PROMPT – PHASE 3 (Active Modifications)

```markdown
ROLE: Android Modding Expert & Automator
TASK: Implement "Expert Mode" features: Restore, Spoofing, and Workflows.
CONTEXT: Backup system (Phase 2) is verified. Now we allow writes.
OBJECTIVE: Enable advanced modification workflows with maximum safety rails.

DELIVERABLES:
1. Write Operations (`src/Features/Modifications/`):
   - `PartitionRestorer`: The inverse of BackupEngine. MUST run `RestoreSimulator` logic first.
   - `CalibrationFixer`: Specific logic to inject IMEI/MAC into decrypted /persist dumps.
2. Magisk/Root Tools:
   - `FingerprintSpoofer`: Generate `system.prop` module for Magisk based on certified fingerprints db.
   - `HookGenerator`: Template-based generator for Frida/LSPosed scripts (Bypass Root detection).
3. Workflow System (`src/Features/Workflows/`):
   - `WorkflowEngine`: Execute a list of `IOperation` sequentially.
   - `WorkflowBuilder`: Drag-and-drop UI to chain steps (e.g., Backup -> Unlock -> Restore).

CONSTRAINTS:
- **Gated Access**: All features require a specific "I acknowledge risks" check in UI.
- **Safety**: Auto-trigger Backup of target partition before any Write.
```

---

## STAGE 4 – DEEP DIVES (Implementation Details)

### 4.1 – IMEI/MAC Reader (Phase 1)

- **Sources**:
  - `adb shell service call iphonesubinfo 1` (Legacy).
  - `/persist/` (EDL raw read).
  - NVRAM items (MTK).
- **Validation**: Implement `LuhnCheck(string imei)` utils. Warn if "000000..." or "123456...".

### 4.2 – Partition Backup (Phase 2)

- **Format**: `.debk` (DeepEye Backup).
- **Header**: JSON metadata (Version, DeviceID, PartitionMap, Salt, IV) -> 4KB padded.
- **Body**: Encrypted binary stream.

### 4.3 – Workflow Automation (Phase 3)

- Define a `TaskStep` class:

  ```json
  { "Action": "ReadPartition", "Target": "modem", "OnError": "Abort" }
  ```

- Serialize workflows to JSON for sharing.

---

## STAGE 9 – FILE ORGANIZATION & GIT BRANCHING

### 9.1 – Folder Structure (Target State)

```text
src/
├── DeepEyeUnlocker.csproj
├── Features/                  <-- NEW MODULES
│   ├── DeviceHealth/          (Phase 1)
│   │   ├── Readers/
│   │   ├── Models/
│   │   └── DeviceHealthPanel.cs
│   ├── PartitionBackup/       (Phase 2)
│   │   ├── Engine/
│   │   ├── Storage/
│   │   └── BackupPanel.cs
│   ├── Modifications/         (Phase 3)
│   │   ├── Magisk/
│   │   ├── Frida/
│   │   └── ExpertPanel.cs
│   └── Analytics/             (Phase 4)
│       └── ...
├── Operations/                (Legacy/Core Ops)
│   ├── AdbToolsManager.cs
│   └── ...
├── Protocols/                 (EDL/Fastboot)
└── UI/
    └── MainForm.cs            (Tab Host)
```

### 9.2 – Branching Strategy

- `main`: Stable releases.
- `dev`: Integration branch.
- `feat/diagnostics`: Phase 1 work.
- `feat/backup-engine`: Phase 2 work.
- `feat/expert-mods`: Phase 3 work.

### 9.3 – Immediate Next Steps

1. Create the `feat/diagnostics` branch.
2. Initialize the `src/Features/DeviceHealth/` directory structure.
3. Run **God Prompt – Phase 1**.
