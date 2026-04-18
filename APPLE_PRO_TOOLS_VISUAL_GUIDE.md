# Apple Pro Tools — Visual Structure Guide

## 📱 UI Layout

```
┌─────────────────────────────────────────────────┐
│           Apple Pro Tools (35 Tools)            │
├─────────────────────────────────────────────────┤
│  📱 Apple Device: DFU                           │
│  iPhone 13,2 • DFU Mode                         │
│  [Refresh]                                      │
├─────────────────────────────────────────────────┤
│  [All] [Activation] [MDM] [Passcode] [Firmware] │
├─────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐            │
│  │ 🔓 [HIGH]    │  │ 🛡️ [HIGH]    │            │
│  │ Activation   │  │ MDM Profile  │            │
│  │ Lock Bypass  │  │ Bypass       │            │
│  │ iOS 12-16.7  │  │ iOS 12-18    │            │
│  │ JB Required  │  │ JB Required  │            │
│  └──────────────┘  └──────────────┘            │
│  ┌──────────────┐  ┌──────────────┐            │
│  │ 🔐 [CRIT]    │  │ 🐛 [MED]     │            │
│  │ Passcode     │  │ checkm8 DFU  │            │
│  │ Remove       │  │ Exploit      │            │
│  │ iOS 12-15.8  │  │ A5-A11       │            │
│  │ JB Required  │  │              │            │
│  └──────────────┘  └──────────────┘            │
│  ┌──────────────┐  ┌──────────────┐            │
│  │ ☁️ [HIGH]    │  │ ℹ️ [LOW]     │            │
│  │ iCloud       │  │ Device Info  │            │
│  │ Remove       │  │ Extractor    │            │
│  │ iOS 12-16.7  │  │ All iOS      │            │
│  │ JB Required  │  │              │            │
│  └──────────────┘  └──────────────┘            │
├─────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────┐   │
│  │ Activation Lock Bypass            [✕]   │   │
│  │                                          │   │
│  │ Bypass iCloud Activation Lock on         │   │
│  │ iPhone/iPad                              │   │
│  │                                          │   │
│  │ Category:     Activation Lock            │   │
│  │ iOS Versions: iOS 12–16.7                │   │
│  │ Risk Level:   HIGH                       │   │
│  │ Est. Time:    ~5 min                     │   │
│  │ Jailbreak:    Required                   │   │
│  │                                          │   │
│  │ [Execute Activation Lock Bypass]         │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

---

## 🗂️ Category Structure

```
Apple Pro Tools
│
├── 🔓 ACTIVATION_BYPASS (6 tools)
│   ├── Activation Lock Bypass
│   ├── FMI Status Check
│   ├── GSM Activation Bypass
│   ├── Signal Activation Bypass
│   ├── Hello Screen Bypass (Signal)
│   └── Hello Screen Bypass (No Signal)
│
├── 🛡️ MDM_BYPASS (4 tools)
│   ├── MDM Profile Bypass
│   ├── DEP Bypass
│   ├── Supervised Mode Bypass
│   └── MDM Profile Parser
│
├── 🔐 PASSCODE_BYPASS (3 tools)
│   ├── Screen Passcode Remove
│   ├── Screen Time Bypass
│   └── Activation Token Backup
│
├── 🐛 CHECKM8_EXPLOIT (4 tools)
│   ├── checkm8 DFU Exploit
│   ├── Force DFU Mode
│   ├── Recovery Mode Toggle
│   └── Pwned DFU Entry
│
├── ⬇️ FIRMWARE_TOOLS (5 tools)
│   ├── IPSW Firmware Flash
│   ├── iOS Downgrade
│   ├── SHSH Blob Saver
│   ├── OTA Update Blocker
│   └── Reset & Settings Lock
│
├── ☁️ ICLOUD_TOOLS (5 tools)
│   ├── iCloud Account Remove
│   ├── Apple ID Disabled Fix
│   ├── FMI-OFF API Submit
│   ├── Activation Status Check
│   └── GetEnv Snapshot
│
├── ℹ️ DIAGNOSTICS (5 tools)
│   ├── Device Info Extractor
│   ├── CVE Intelligence Scan
│   ├── Mode Probe
│   ├── Refresh Device Mode
│   └── IMEI/Serial Check
│
└── 📶 NETWORK_UNLOCK (2 tools)
    ├── Carrier/SIM Unlock
    └── Baseband Backup
```

---

## 🔄 Data Flow Diagram

```
┌─────────────────────┐
│   User Interface    │
│ AppleProToolsScreen │
└──────────┬──────────┘
           │
           ▼
┌──────────────────────┐
│    ViewModel Layer   │
│ AppleDeviceViewModel │
│  - state: StateFlow  │
│  - refreshDevice()   │
│  - sendCommand()     │
└──────────┬───────────┘
           │
           ▼
┌───────────────────────┐
│   Use Case Layer      │
│ AppleDeviceUseCase    │
│  - observeDevice()    │
│  - refreshDeviceInfo()│
└──────────┬────────────┘
           │
           ▼
┌──────────────────────┐
│    Engine Layer      │
│  ActivationEngine    │
│  - executeActivation()│
│  - performBypass()   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   Protocol Layer     │
│  AppleDfuProtocol    │
│  AppleSession        │
│  UsbAppleSession     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   Hardware Layer     │
│   USB Connection     │
│   Apple Device       │
└──────────────────────┘
```

---

## 📊 Tool Metadata Structure

```
AppleTool
│
├── id: String              // "activation_lock_bypass"
├── name: String            // "Activation Lock Bypass"
├── description: String     // "Bypass iCloud Activation Lock..."
├── category: Enum          // ACTIVATION_BYPASS
├── isSupported: Boolean    // true
├── requiresJailbreak: Bool // true
├── supportedVersions: Str  // "iOS 12–16.7"
├── iconRes: String         // "lock_open"
├── riskLevel: Enum         // HIGH
│   ├── LOW                 // Green
│   ├── MEDIUM              // Orange
│   ├── HIGH                // Red
│   └── CRITICAL            // Bright Red
└── estimatedTime: String   // "~5 min"
```

---

## 🎨 Color Coding System

```
Risk Levels:
┌─────────────────────────────────────────────┐
│ LOW      🟢 Green   (#4CAF50)  Safe         │
│ MEDIUM   🟠 Orange  (#FFA500)  Caution      │
│ HIGH     🔴 Red     (#FF4444)  Warning      │
│ CRITICAL 🔴 Bright  (#FF0000)  Danger       │
└─────────────────────────────────────────────┘

Categories:
┌─────────────────────────────────────────────┐
│ ACTIVATION_BYPASS  🔓 LockOpen              │
│ MDM_BYPASS         🛡️ Shield                │
│ PASSCODE_BYPASS    🔐 Lock                  │
│ FIRMWARE_TOOLS     ⬇️ Download              │
│ CHECKM8_EXPLOIT    🐛 BugReport             │
│ ICLOUD_TOOLS       ☁️ Cloud                 │
│ DIAGNOSTICS        ℹ️ Info                  │
│ NETWORK_UNLOCK     📶 SignalCellularAlt     │
└─────────────────────────────────────────────┘
```

---

## 📁 File Structure

```
app/src/main/kotlin/com/deepeye/
│
├── apple/
│   └── AppleToolsModel.kt                    ← Tool Registry (NEW)
│
├── otg/
│   ├── engine/
│   │   └── ActivationEngine.kt               ← Backend Orchestrator
│   │
│   ├── protocol/apple/
│   │   ├── AppleDfuProtocol.kt               ← DFU Protocol
│   │   ├── AppleSession.kt                   ← Session Manager
│   │   ├── UsbAppleSession.kt                ← USB Session
│   │   └── model/
│   │       └── AppleDeviceProfile.kt         ← Device Profile
│   │
│   ├── usecase/
│   │   └── AppleDeviceUseCase.kt             ← Use Case
│   │
│   ├── usb/
│   │   ├── AppleDeviceMatrix.kt              ← Mode Detection
│   │   ├── IosRecoveryManager.kt             ← Recovery Mgr
│   │   └── IosSessionCoordinator.kt          ← Session Coord
│   │
│   └── ui/
│       ├── apple/
│       │   ├── AppleProToolsScreen.kt        ← Original Screen
│       │   ├── AppleProToolsEnhancedScreen.kt← Enhanced (NEW)
│       │   └── AppleDeviceViewModel.kt       ← ViewModel
│       │
│       └── screens/apple/
│           ├── MdmRemovalScreen.kt           ← MDM Screen
│           └── MdmViewModel.kt               ← MDM ViewModel
```

---

## 🔧 Integration Points

```
Existing Components                    New Components
┌──────────────────────┐              ┌──────────────────────┐
│ ActivationEngine     │◄─────────────┤ AppleToolsRegistry   │
│  - performHello()    │  executes    │  - ALL_TOOLS         │
│  - performPasscode() │              │  - getToolsByCat()   │
│  - performMdm()      │              │  - getToolById()     │
└──────────┬───────────┘              └──────────┬───────────┘
           │                                     │
           │              ┌──────────────────────┘
           │              │
           ▼              ▼
┌──────────────────────────────────┐
│    AppleDeviceViewModel          │
│  - refreshAppleDevice()          │
│  - sendIrecoveryCommand()        │
│  - exitRecovery()                │
│  - enterDfu()                    │
└──────────┬───────────────────────┘
           │
           ▼
┌──────────────────────────────────┐
│  AppleProToolsEnhancedScreen     │
│  - Category filters              │
│  - Tool grid                     │
│  - Details panel                 │
│  - Execute buttons               │
└──────────────────────────────────┘
```

---

## 🎯 Tool Execution Flow

```
User taps "Execute"
       │
       ▼
┌──────────────────────┐
│ executeAppleTool()   │
│ Map tool ID to       │
│ viewModel function   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ AppleDeviceViewModel │
│ Call appropriate     │
│ method               │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ ActivationEngine     │
│ executeActivation()  │
│ Perform operation    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Update UI State      │
│ Show success/error   │
│ Update logs          │
└──────────────────────┘
```

---

## 📋 Implementation Checklist

```
Phase 1: Foundation ✅
├── Create AppleToolsModel.kt          ✅
├── Define all 35+ tools               ✅
├── Create 8 category enums            ✅
├── Add helper functions               ✅
└── Create risk level enum             ✅

Phase 2: UI ✅
├── Create enhanced screen             ✅
├── Add device status card             ✅
├── Add category filters               ✅
├── Add tool grid layout               ✅
├── Add tool cards                     ✅
├── Add details panel                  ✅
└── Add risk visualization             ✅

Phase 3: Documentation ✅
├── Create audit document              ✅
├── Create quick reference             ✅
├── Create summary document            ✅
└── Create visual guide                ✅

Phase 4: Integration (TODO)
├── Add navigation route               ⏳
├── Wire up tool execution             ⏳
├── Test on devices                    ⏳
└── Add missing implementations        ⏳

Phase 5: Polish (TODO)
├── Add tutorials                      ⏳
├── Add history tracking               ⏳
├── Add notifications                  ⏳
├── Add favorites                      ⏳
└── Add offline mode                   ⏳
```

---

## 🚀 Quick Start Commands

```bash
# Build the project
./gradlew :app:compileDebugKotlin

# Run on device
./gradlew :app:installDebug

# View documentation
cat APPLE_PRO_TOOLS_QUICK_REFERENCE.md
cat APPLE_PRO_TOOLS_REMAPPING.md
cat APPLE_PRO_TOOLS_COMPLETE.md
```

---

*Visual Structure Guide v1.0*
*Generated: 2026-04-18*
