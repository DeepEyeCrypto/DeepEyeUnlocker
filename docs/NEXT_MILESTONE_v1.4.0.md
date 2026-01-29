# Next Milestone: v1.3.0 "Sentinel Pro" (Operations Expansion)

> **Status:** Planning / Roadmap Formulation
> **Target Version:** v1.4.0
> **Codename:** Sentinel Pro
> **Objective:** Transition from a diagnostics-focused toolkit to a technician-grade operational suite with hardware-level partition management and security-bypass automation.

---

## 1. High-Level Product Overview

### 1.1 Description

DeepEyeUnlocker v1.4.0 is a low-level hardware orchestration suite designed for forensic analysts, repair technicians, and firmware engineers. It provides direct interfacing with Qualcomm (EDL/Firehose) and MediaTek (BROM/SLA-bypass) chipsets to perform operations that standard Android OS environments restrict. By leveraging raw protocol handshakes and ADB-root access, the toolkit automates complex recovery and security-patching workflows with a modular, safety-first architecture.

### 1.2 Core Modules

- **FRP Bypass Engine (Epic A)**: Standardized partition-level erase/reset workflows for Qualcomm/MTK via hardware-level programmers.
- **Partition Restore Center (Epic B)**: Granular image-to-block flashing with mandatory integrity checks and size-mismatch guards.
- **Cloak Stealth Mode (Epic C)**: Automated Zygisk/Shamiko orchestration to assist in hiding operational traces from RASP/Security SDKs.
- **Driver Center Pro (Epic D)**: A host-side utility to resolve INF conflicts and manage filter drivers for auth-bypass protocols.
- **Fleet Management (Epic E)**: A batch processing hub for managing multiple device contexts and mass-ADB command execution.

---

## 2. Epic A: FRP Bypass Engine

### 2.1 Research Notes

- **Qualcomm**: Modern QC devices store FRP status in `frp`, `config`, or `persist`. Handshake via `Sahara` mode to upload a `Firehose` programmer. GPT parsing is required to locate the specific LBA of the FRP-persistent data.
- **MediaTek**: FRP is typically at a fixed offset in a dedicated partition. Access requires a Download Agent (DA) and SLA/DAA bypass concepts for secure-boot devices.
- **Pitfalls**: Touching `persist` on certain models can corrupt sensors or Widevine certificates.
- **Legal/Safety**: FRP is an anti-theft measure. Bypassing it is strictly for owner-authorized recovery.

### 2.2 Feature Scope

- **In-Scope**:
  - Model/profile-based FRP flows for supported chipsets.
  - Transparent view of target partitions/blocks.
  - Mandatory pre-erase backup of target region.
  - Structured logging (time, commands, partitions, status).
- **Out-of-Scope**:
  - Bypassing server-side activations (Mi Account, Samsung KNOX).
  - Blind generic partition guessing.

### 2.3 Workflow & UX

1. **Detection**: Detect "Qualcomm HS-USB QDLoader 9008" or "MTK USB Port".
2. **Profile Match**: Select Model/Chipset and verify Firehose/DA MD5.
3. **The "Plan"**: Display plan (e.g., `[Partition: frp] [Size: 512KB] [Action: Erase] [Risk: Low]`).
4. **Safety Check**: Explicit ownership and data wipe consent checkboxes.
5. **Execution**: Real-time progress + live logs.

### 2.4 Technical Design

- **Profile System**: JSON mapping `VendorID/ProductID` to `ProgrammerPath` and `TargetPartitionNames`.
- **Validation**: Post-erase verification by reading the block back.
- **Extensibility**: Public `Profiles/` directory for community-contributed Firehose loaders.

### 2.5 Safety & Abuse-Mitigation

- **Danger Zones**: Explicitly block erasure of `modemst1`, `modemst2`, `fsg`, and `sec_efs`.
- **Pre-checks**: Battery >30% and USB 2.0 port stability verification.

---

## 3. Epic B: Partition Restore Center

### 3.1 Research Notes

- Selective read/write using protocol-specific handlers (EdlManager, MtkManager).
- Common failure point: Mismatched partition sizes or incorrect vbmeta state leading to bootloops.

### 3.2 Feature Scope

- **In-Scope**:
  - Granular partition backup/restore (per-partition selection).
  - Verification of image size vs partition size.
  - Mandatory backup of "IMEI-Critical" partitions before write.
- **Out-of-Scope**:
  - Automatic "fixing" of unknown partition layouts.

### 3.3 Workflow & UX

- **UI Grid**: Color-coded risk levels (Green: Safe, Amber: System, Red: IMEI/Calibration).

1. **User Selects Partition** (e.g., `recovery`).
2. **Compatibility Check**: `Image (12.4MB) <= Partition (16MB)`.
3. **Flash**: Progress bar with data transfer rate (MB/s).

- **Safety**: 3-second hold required for flashing "Red" (High-Risk) partitions.

### 3.4 Technical Design

- **GPT Parser**: Handle eMMC (Sector-based) and UFS (LUN-based) geometries.
- **Integrity**: MD5/SHA256 checksumming before and after flashing.

### 3.5 Safety & Abuse-Mitigation

- **Mandatory First-Run Backup**: Force backup of EFS/NV/IMEI on first connection to any device.
- **UI Friction**: Double confirmation and high-risk banners for modem/persist/vbmeta writes.

---

## 4. Epic C: Cloak Stealth Mode

### 4.1 Research Notes

- **Ecosystem**: Magisk/Zygisk + Shamiko + Play Integrity workarounds.
- **Reality**: No 100% bypass exists; advanced RASP SDKs monitor kernel syscalls and hardware keystores.

### 4.2 Feature Scope

- **In-Scope**:
  - Detection of Magisk/KSU/Zygisk status.
  - Profile-based stealth application (Banking, Games, Generic).
  - Automated installation of compatible modules (Shamiko, etc.).
  - System prop tweaking (ro.debuggable, ro.secure).
- **Out-of-Scope**:
  - Bypassing strong enterprise/Intune policies.
  - App repackaging or deep binary patching.

### 4.3 Workflow & UX

1. **Detect**: Tool reads current Zygisk/Denylist config via ADB root.
2. **Profile Selection**: User chooses "Max Stealth (Banking)".
3. **Execution**: Pushes module zips -> Installs via `magisk --install-module` -> Shell-modifies `magisk.db`.
4. **UX**: "Revert to Previous" button for quick recovery from module-induced loops.

### 4.4 Technical Design

- **Shell Bridge**: Uses `su -c` to query and modify `/data/adb/magisk.db` and `/data/adb/modules/`.
- **Prop Logic**: Uses `resetprop` for non-permanent or persistent prop injection.

### 4.5 Safety & Abuse-Mitigation

- **Warnings**: Explicit notice on bypassing financial app security and possible ToS violations.
- **Rollback**: Automatic backup of Magisk configuration before any modification.

---

## 5. Epic D: Driver Center Pro

### 5.1 Research Notes

- **Conflicts**: WinUSB vs QDLoader vs VCOM. libusb-win32 filters often "blind" standard OEM tools.
- **Goal**: Unified host-side environment cleanup.

### 5.2 Feature Scope

- **In-Scope**:
  - Scanning of INF bindings for connected VID/PID sets.
  - Identification of broken/unknown/hidden devices.
  - Preset cleaning: "Qualcomm Service Bench", "MTK Service Bench".
- **Out-of-Scope**:
  - OS-level kernel signing (DSE) modifications.

### 5.3 Workflow & UX

1. **Diagnosis**: "MTK Port not responding" -> User opens Driver Center.
2. **Detection**: Lists drivers (e.g., "VCOM: Installed, LibUsb: Missing").
3. **Repair**: One-click preset deployment for the specific chipset target.

### 5.4 Technical Design

- **WMI/DevNode**: Query `Win32_PnPEntity` for hardware IDs.
- **Deployment**: Silent execution of `DPInst` or `PnPUtil` for driver management.

### 5.5 Safety & Abuse-Mitigation

- **Mitigation**: Suggests System Restore point before bulk driver removal.
- **Dry Run**: Preview mode showing which .inf files will be uninstalled.

---

## 6. Epic E: Fleet Management (Multi-Device)

### 6.1 Research Notes

- Multi-device ADB orchestration (`-s <serial>`).
- Technicians often manage 3-5 devices simultaneously at different stages of the workflow.

### 6.2 Feature Scope

- **In-Scope**:
  - Multi-device enumeration (USB/TCP).
  - Parallel ADB command execution (Shell, APK Install, Logcat).
  - Device aliasing ("Bench-1", "Customer-A").
- **Out-of-Scope**:
  - Parallel EDL flashing on a single USB controller (bandwidth risk).

### 6.3 Workflow & UX

- **Grid View**: `Serial | Alias | Mode | Battery | Root Status`.
- **Batch Action**: Select multiple -> Right-click -> "Install Recovery APK".

### 6.4 Technical Design

- **Parallelism**: Thread-pooled ADB execution with per-device stdout redirection.
- **Queueing**: Prevents USB bus saturation by rate-limiting concurrent bulk transfers.

### 6.5 Safety & Abuse-Mitigation

- **Prevention**: Destructive actions (FRP, Flash) require explicit single-device selection.
- **Confirmation**: Bulk actions show a summary dialog: "Apply to [3] devices?"

---

## 7. Cross-Cutting Architecture

### 7.1 Common Abstractions

- **Device Object**: Unified state tracking (Serial, Chipset, Mode, HardwareTelemetry).
- **Operation Library**: Reusable tasks (ErasePartition, PushModule, RepairDriver).

### 7.2 Module Integration

- Fleet Management acts as the "Context Provider".
- Operational modules (FRP, Restore) attach to the currently focused context.

---

## 8. Contribution & Roadmap

### 8.1 Participation Rules

- **Testing**: Partition-write features require "Scrap Metal" device verification.
- **Safety**: Mandatory inclusion of safety design in any PR involving hardware writes.

### 8.2 Issue Templates

- **MODEL_PROFILE**: Requires GPT dump + verified Firehose/DA.
- **BUG_REPORT**: Requires `logs/` export + Windows Device Manager screenshot.

### 8.3 Progress Gates

1. **Research (v1.4.0-alpha)**: Protocol and partition geometry verification.
2. **Experimental (v1.4.0-beta)**: Expert mode only, write access enabled.
3. **Stable (v1.4.0)**: Sentinel Pro Main release.
