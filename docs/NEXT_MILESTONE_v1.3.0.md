# DeepEyeUnlocker v1.3.0 – Next Milestone Roadmap

> **Status:** Planning  
> **Target Release:** ~4-6 weeks from v1.2.0  
> **Codename:** "Sentinel"

---

## 📋 Executive Summary

v1.3.0 focuses on **read-only diagnostics, safe backup, and foundational UI** for advanced features. No risky write operations. Ship fast, ship safe.

| Priority | Epic | Description | Est. Effort |
|----------|------|-------------|-------------|
| P0 | Device Health Center | IMEI/MAC/Battery/Kernel audit | 2-3 days |
| P0 | Partition Backup Center | Safe read + encrypt + verify | 3-4 days |
| P0 | Architecture Cleanup | Models, interfaces, tests | 2-3 days |
| P1 | ROM Sandbox Foundation | UI + GSI catalog (no flash) | 2 days |
| P1 | Cloak Center Detection | Root/dev mode detection only | 1-2 days |

**Total Estimate:** ~12-15 dev days

---

## 🎯 STAGE 1 – Scope Definition

### 1.1 What's IN Scope (v1.3.0)

#### ✅ P0 – Must Ship

| Feature | Description | Risk Level |
|---------|-------------|------------|
| **Device Health Center** | One-click IMEI/MAC/Battery/Kernel/Bootloader audit | Low |
| **Partition Backup Center** | Read partitions, encrypt backup, verify checksum | Low-Medium |
| **Report Export** | Export device report as Markdown/HTML/JSON | Low |
| **Architecture Cleanup** | Canonical DTOs (DeviceHealth, PartitionBackup, FrpStatus) | Low |
| **Protocol Interface Alignment** | IProtocolEngine standardization | Low |
| **Basic Test Coverage** | Unit tests for core models | Low |

#### ⚡ P1 – Nice-to-Have

| Feature | Description | Risk Level |
|---------|-------------|------------|
| **ROM Sandbox UI** | Tab skeleton + GSI catalog browser (no actual flashing) | Low |
| **Cloak Detection** | Detect root/Magisk/dev-options (no hiding yet) | Low |
| **Driver Health Check** | USB/Qualcomm/MTK driver status panel | Low |

#### 🔮 P2 – Stretch Goals

| Feature | Description | Risk Level |
|---------|-------------|------------|
| **Partition Table Viewer** | Visual GPT/MBR partition map | Low |
| **ADB Shell Console** | Embedded terminal for quick commands | Low |

---

### 1.2 What's OUT of Scope (v1.3.0)

| Feature | Reason | Target |
|---------|--------|--------|
| ❌ Partition Restore/Write | High risk, needs heavy testing | v1.4.0+ |
| ❌ IMEI/MAC Modification | Legal concerns, complex protocols | v1.5.0+ |
| ❌ FRP Bypass Engine | Requires deep protocol work | v1.4.0+ |
| ❌ Screen Lock Removal | Complex, device-specific | v1.4.0+ |
| ❌ Full DSU Orchestration | Flashing logic already built, UI integration later | v1.4.0 |
| ❌ Fleet Management | Enterprise feature, out of scope | v2.0.0 |
| ❌ Cloud Backup/Sync | Needs backend work | v2.0.0 |
| ❌ CVE Scanner | Research-heavy | v2.0.0 |

---

## 🏗️ STAGE 2 – Epics / Feature Tracks

### Epic A: Device Health Center

**Objective:** Provide a one-stop read-only dashboard showing IMEI, MAC, battery health, kernel version, bootloader status, and security patch level.

**Success Criteria:**

- [ ] User clicks "Scan" → all health data populates in <3 seconds
- [ ] User can export report to Markdown/HTML/JSON
- [ ] Works on any ADB-accessible device (no root required)

**Dependencies:**

- `IAdbClient` interface (existing)
- `DeviceContext` model (existing)

**New Models:**

```csharp
namespace DeepEyeUnlocker.Core.Models
{
    public class DeviceHealthReport
    {
        public string Imei { get; set; }
        public string Mac { get; set; }
        public int BatteryLevel { get; set; }
        public int BatteryHealth { get; set; } // 0-100
        public string KernelVersion { get; set; }
        public string BootloaderVersion { get; set; }
        public string SecurityPatchLevel { get; set; }
        public string BuildFingerprint { get; set; }
        public bool IsRooted { get; set; }
        public bool DevOptionsEnabled { get; set; }
        public DateTime ScanTimestamp { get; set; }
    }
}
```

**Files to Create:**

- `src/Features/DeviceHealth/DeviceHealthScanner.cs`
- `src/Features/DeviceHealth/ReportExporter.cs`
- `src/Features/DeviceHealth/UI/DeviceHealthPanel.cs`

---

### Epic B: Partition Backup Center

**Objective:** Allow users to safely backup device partitions (read-only), with encryption and checksum verification.

**Success Criteria:**

- [ ] User can select partitions from a list
- [ ] Backup creates encrypted archive with SHA256 manifest
- [ ] Progress bar shows transfer speed and ETA
- [ ] Works via ADB (no root) or EDL (Qualcomm)

**Dependencies:**

- `IProtocolEngine.ReadPartitionAsync()` (existing)
- `DeviceContext` (existing)

**New Models:**

```csharp
namespace DeepEyeUnlocker.Core.Models
{
    public class PartitionBackupJob
    {
        public string JobId { get; set; }
        public string DeviceSerial { get; set; }
        public List<string> Partitions { get; set; }
        public string OutputPath { get; set; }
        public bool Encrypt { get; set; }
        public string EncryptionKey { get; set; }
        public DateTime StartTime { get; set; }
        public BackupStatus Status { get; set; }
    }

    public enum BackupStatus
    {
        Pending, InProgress, Completed, Failed, Cancelled
    }

    public class BackupManifest
    {
        public string JobId { get; set; }
        public string DeviceModel { get; set; }
        public DateTime BackupTime { get; set; }
        public List<PartitionEntry> Partitions { get; set; }
    }

    public class PartitionEntry
    {
        public string Name { get; set; }
        public long SizeBytes { get; set; }
        public string Sha256 { get; set; }
        public bool Encrypted { get; set; }
    }
}
```

**Files to Create:**

- `src/Features/PartitionBackup/BackupOrchestrator.cs`
- `src/Features/PartitionBackup/BackupEncryption.cs`
- `src/Features/PartitionBackup/ManifestGenerator.cs`
- `src/Features/PartitionBackup/UI/PartitionBackupPanel.cs`

---

### Epic C: ROM Sandbox Foundation

**Objective:** Build the UI skeleton and GSI catalog browser for ROM Sandbox. No actual flashing in v1.3.0 (use existing DSU code in v1.4.0).

**Success Criteria:**

- [ ] "ROM Sandbox" tab visible in MainForm
- [ ] GSI catalog loads and displays available images
- [ ] User can browse image details (name, size, compatibility)
- [ ] "Flash" button disabled with "Coming in v1.4.0" tooltip

**Dependencies:**

- `GsiDatabase` (already built in v1.2.0)
- `RomSandboxPanel` (already built, needs MainForm integration)

**Files to Modify:**

- `src/UI/MainForm.cs` – Add ROM Sandbox tab
- `src/Features/DsuSandbox/UI/RomSandboxPanel.cs` – Disable flash button

---

### Epic D: Cloak Center Detection

**Objective:** Detect root/Magisk presence, developer options status, and USB debugging mode. No hiding/bypass yet.

**Success Criteria:**

- [ ] UI shows root detection status (Magisk/SuperSU/KernelSU)
- [ ] Shows if developer options are enabled
- [ ] Shows if USB debugging is on
- [ ] No write operations (detection only)

**Dependencies:**

- `IAdbClient` (existing)

**New Models:**

```csharp
namespace DeepEyeUnlocker.Cloak
{
    public class CloakDetectionResult
    {
        public bool IsRooted { get; set; }
        public string RootMethod { get; set; } // "Magisk", "SuperSU", "KernelSU", "Unknown"
        public string MagiskVersion { get; set; }
        public bool DevOptionsEnabled { get; set; }
        public bool UsbDebuggingEnabled { get; set; }
        public bool OemUnlockEnabled { get; set; }
        public List<string> SuspiciousPackages { get; set; }
    }
}
```

**Files to Create:**

- `src/Features/CloakDetection/CloakDetector.cs`
- `src/Features/CloakDetection/UI/CloakDetectionPanel.cs`

---

### Epic E: Architecture & Quality

**Objective:** Clean up core models, align protocol interfaces, add basic unit tests.

**Success Criteria:**

- [ ] All DTOs in `Core.Models` namespace
- [ ] `IProtocolEngine` interface standardized
- [ ] 10+ unit tests for core models
- [ ] Zero build warnings

**Tasks:**

1. Consolidate duplicate models (PartitionInfo, DeviceContext, FrpStatus)
2. Ensure all protocols implement IProtocolEngine consistently
3. Add xUnit test project with model validation tests
4. Fix all nullable warnings

---

## 🏷️ STAGE 3 – GitHub Labels & Milestone

### 3.1 Label Strategy

Create these labels in GitHub:

#### Feature Labels

| Label | Color | Description |
|-------|-------|-------------|
| `feature/health-center` | `#1D76DB` | Device Health Center |
| `feature/backup-center` | `#0E8A16` | Partition Backup Center |
| `feature/rom-sandbox` | `#5319E7` | ROM Sandbox (DSU) |
| `feature/cloak-center` | `#D93F0B` | Cloak Center |
| `meta/architecture` | `#FBCA04` | Architecture/refactoring |

#### Priority Labels

| Label | Color | Description |
|-------|-------|-------------|
| `P0` | `#B60205` | Must ship |
| `P1` | `#D93F0B` | Nice to have |
| `P2` | `#FBCA04` | Stretch goal |

#### Type Labels

| Label | Color | Description |
|-------|-------|-------------|
| `type/feature` | `#0075CA` | New feature |
| `type/bug` | `#D73A4A` | Bug fix |
| `type/refactor` | `#A2EEEF` | Code refactoring |
| `type/docs` | `#0075CA` | Documentation |
| `type/test` | `#BFD4F2` | Tests |

#### Status Labels

| Label | Color | Description |
|-------|-------|-------------|
| `Next Milestone` | `#7057FF` | Part of next release |
| `Later` | `#C5DEF5` | Backlog |
| `WIP` | `#FEF2C0` | Work in progress |

---

### 3.2 Milestone Setup

```
Milestone: v1.3.0 – Sentinel
Description: Read-only diagnostics, safe backup, and foundational UI.
Due Date: 2026-03-01 (adjust as needed)
```

### 3.3 Label Usage Rules

| Scenario | Required Labels |
|----------|-----------------|
| v1.3.0 feature | `Next Milestone` + `feature/*` + `type/feature` + `P0/P1/P2` |
| v1.3.0 bug fix | `Next Milestone` + `type/bug` + `P0/P1/P2` |
| v1.3.0 refactor | `Next Milestone` + `meta/architecture` + `type/refactor` |
| Future work | `Later` + `feature/*` |
| Random bug | `type/bug` (no milestone label) |

---

## 📝 STAGE 4 – Issue Templates

### 4.1 Feature Issue Template

Create `.github/ISSUE_TEMPLATE/feature.md`:

```markdown
---
name: Feature Request
about: Propose a new feature for DeepEyeUnlocker
title: "[FEATURE] "
labels: type/feature
assignees: ''
---

### Summary
<!-- Short description of what this feature will deliver -->

### Motivation / User Story
<!-- As a <type of user>, I want <feature> so that <benefit> -->

### Scope

**This issue WILL:**
- [ ] ...

**This issue WILL NOT:**
- [ ] ...

### Technical Notes

**Modules / Namespaces:**
- `DeepEyeUnlocker.Features.…`
- `DeepEyeUnlocker.Core.Models.…`

**Interfaces / DTOs:**
- ...

**Dependencies:**
- ...

### Acceptance Criteria
- [ ] ...
- [ ] ...

### Testing Checklist
- [ ] Unit tests added/updated
- [ ] Manual test on real device (if applicable)
- [ ] No new build warnings
- [ ] Documentation updated (if needed)

### Labels
<!-- Add: Next Milestone, feature/*, P0/P1/P2 -->
```

---

### 4.2 Bug Issue Template

Create `.github/ISSUE_TEMPLATE/bug.md`:

```markdown
---
name: Bug Report
about: Report a bug in DeepEyeUnlocker
title: "[BUG] "
labels: type/bug
assignees: ''
---

### Description
<!-- What happened? -->

### Steps to Reproduce
1. ...
2. ...
3. ...

### Expected Behavior
<!-- What should have happened? -->

### Actual Behavior
<!-- What actually happened? -->

### Environment
- DeepEyeUnlocker version:
- Windows version:
- Device model (if applicable):
- Android version (if applicable):

### Screenshots / Logs
<!-- Attach if available -->

### Possible Fix
<!-- Optional: suggest a fix -->
```

---

## 🚀 STAGE 5 – Implementation Order

### Phase 1: Foundation (Days 1-3)

```
Week 1
├── Epic E: Architecture Cleanup
│   ├── Consolidate DTOs in Core.Models
│   ├── Align IProtocolEngine interface
│   └── Fix nullable warnings
└── Epic A: Device Health Center (core)
    ├── DeviceHealthScanner.cs
    └── DeviceHealthReport model
```

**Branch:** `feature/v1.3.0-foundation`

### Phase 2: Health & Detection (Days 4-6)

```
Week 1-2
├── Epic A: Device Health Center (UI)
│   ├── DeviceHealthPanel.cs
│   └── ReportExporter (MD/HTML/JSON)
└── Epic D: Cloak Detection
    ├── CloakDetector.cs
    └── CloakDetectionPanel.cs
```

**Branch:** `feature/health-center`

### Phase 3: Backup Center (Days 7-10)

```
Week 2
└── Epic B: Partition Backup Center
    ├── BackupOrchestrator.cs
    ├── BackupEncryption.cs
    ├── ManifestGenerator.cs
    └── PartitionBackupPanel.cs
```

**Branch:** `feature/backup-center`

### Phase 4: ROM Sandbox Integration (Days 11-12)

```
Week 3
└── Epic C: ROM Sandbox Foundation
    ├── Integrate RomSandboxPanel into MainForm
    ├── Disable flash button (v1.4.0 placeholder)
    └── Polish GSI catalog UI
```

**Branch:** `feature/rom-sandbox-ui`

### Phase 5: Polish & Ship (Days 13-15)

```
Week 3
├── Add unit tests (10+ tests)
├── Update README changelog
├── Final QA pass
├── Merge to main
└── Tag v1.3.0 release
```

**Branch:** `release/v1.3.0`

---

## 🔀 Branching Strategy

```
main
├── feature/v1.3.0-foundation
├── feature/health-center
├── feature/backup-center
├── feature/rom-sandbox-ui
├── feature/cloak-detection
└── release/v1.3.0 (final merge → tag)
```

**Rules:**

1. All feature branches off `main`
2. PRs require passing CI before merge
3. Squash merge to keep history clean
4. Tag immediately after `release/v1.3.0` merges

---

## ✅ STAGE 6 – Success Criteria for v1.3.0

### Ship Checklist

- [ ] **P0 Features Complete:**
  - [ ] Device Health Center functional
  - [ ] Partition Backup Center functional
  - [ ] Report Export (MD/HTML/JSON)
  - [ ] Architecture cleanup done
  
- [ ] **P1 Features (at least 1):**
  - [ ] ROM Sandbox tab visible
  - [ ] Cloak Detection working

- [ ] **Quality Gates:**
  - [ ] Zero build errors
  - [ ] <5 warnings
  - [ ] 10+ unit tests passing
  - [ ] README updated with v1.3.0 changelog
  - [ ] GitHub Release created with assets

- [ ] **Documentation:**
  - [ ] Every new feature has inline XML docs
  - [ ] `docs/` folder has feature guides

---

## 📊 Issue Backlog (Ready to Create)

### P0 Issues

| # | Title | Labels | Epic |
|---|-------|--------|------|
| 1 | `[FEATURE] Device Health Scanner core implementation` | `P0` `feature/health-center` `type/feature` | A |
| 2 | `[FEATURE] Device Health Panel UI` | `P0` `feature/health-center` `type/feature` | A |
| 3 | `[FEATURE] Report Exporter (MD/HTML/JSON)` | `P0` `feature/health-center` `type/feature` | A |
| 4 | `[FEATURE] Partition Backup Orchestrator` | `P0` `feature/backup-center` `type/feature` | B |
| 5 | `[FEATURE] Backup Encryption (AES-256)` | `P0` `feature/backup-center` `type/feature` | B |
| 6 | `[FEATURE] Backup Manifest Generator` | `P0` `feature/backup-center` `type/feature` | B |
| 7 | `[FEATURE] Partition Backup Panel UI` | `P0` `feature/backup-center` `type/feature` | B |
| 8 | `[REFACTOR] Consolidate Core DTOs` | `P0` `meta/architecture` `type/refactor` | E |
| 9 | `[REFACTOR] Align IProtocolEngine interface` | `P0` `meta/architecture` `type/refactor` | E |
| 10 | `[TEST] Add unit tests for Core.Models` | `P0` `meta/architecture` `type/test` | E |

### P1 Issues

| # | Title | Labels | Epic |
|---|-------|--------|------|
| 11 | `[FEATURE] Integrate ROM Sandbox tab into MainForm` | `P1` `feature/rom-sandbox` `type/feature` | C |
| 12 | `[FEATURE] Cloak Detector implementation` | `P1` `feature/cloak-center` `type/feature` | D |
| 13 | `[FEATURE] Cloak Detection Panel UI` | `P1` `feature/cloak-center` `type/feature` | D |

### P2 Issues

| # | Title | Labels | Epic |
|---|-------|--------|------|
| 14 | `[FEATURE] Partition Table Viewer (visual GPT/MBR)` | `P2` `feature/backup-center` `type/feature` | B |
| 15 | `[FEATURE] ADB Shell Console` | `P2` `type/feature` | – |

---

## 🤖 Automation Script

Run this to create labels via GitHub CLI:

```bash
#!/bin/bash
REPO="DeepEyeCrypto/DeepEyeUnlocker"

# Feature labels
gh label create "feature/health-center" -d "Device Health Center" -c "1D76DB" -R $REPO
gh label create "feature/backup-center" -d "Partition Backup Center" -c "0E8A16" -R $REPO
gh label create "feature/rom-sandbox" -d "ROM Sandbox (DSU)" -c "5319E7" -R $REPO
gh label create "feature/cloak-center" -d "Cloak Center" -c "D93F0B" -R $REPO
gh label create "meta/architecture" -d "Architecture/refactoring" -c "FBCA04" -R $REPO

# Priority labels
gh label create "P0" -d "Must ship" -c "B60205" -R $REPO
gh label create "P1" -d "Nice to have" -c "D93F0B" -R $REPO
gh label create "P2" -d "Stretch goal" -c "FBCA04" -R $REPO

# Type labels
gh label create "type/feature" -d "New feature" -c "0075CA" -R $REPO
gh label create "type/bug" -d "Bug fix" -c "D73A4A" -R $REPO
gh label create "type/refactor" -d "Code refactoring" -c "A2EEEF" -R $REPO
gh label create "type/docs" -d "Documentation" -c "0075CA" -R $REPO
gh label create "type/test" -d "Tests" -c "BFD4F2" -R $REPO

# Status labels
gh label create "Next Milestone" -d "Part of next release" -c "7057FF" -R $REPO
gh label create "Later" -d "Backlog" -c "C5DEF5" -R $REPO
gh label create "WIP" -d "Work in progress" -c "FEF2C0" -R $REPO

echo "✅ Labels created!"
```

---

## 📅 Timeline Summary

| Week | Focus | Deliverables |
|------|-------|--------------|
| Week 1 (Days 1-5) | Foundation + Health | DTOs, DeviceHealthScanner, DeviceHealthPanel |
| Week 2 (Days 6-10) | Backup + Detection | BackupOrchestrator, CloakDetector, Panels |
| Week 3 (Days 11-15) | Integration + Ship | ROM Sandbox UI, Tests, Release |

---

## 🎯 Definition of Done

A feature is "done" when:

1. ✅ Code merged to `main`
2. ✅ No build errors or warnings
3. ✅ Unit tests passing
4. ✅ Manual test on real device (if applicable)
5. ✅ XML documentation on public APIs
6. ✅ README/docs updated

---

**Next Action:** Review this roadmap, then start creating GitHub issues from the backlog table above!
