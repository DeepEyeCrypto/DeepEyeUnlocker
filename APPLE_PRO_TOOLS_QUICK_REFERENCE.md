# Apple Pro Tools — Quick Reference Guide

## 🎯 Overview

The Apple Pro Tools section consolidates ALL Apple/iOS features into one dedicated tab with 8 categories and 35+ tools.

---

## 📂 Key Files

| File | Purpose |
|------|---------|
| `app/.../apple/AppleToolsModel.kt` | **Tool registry** — All tools defined here |
| `app/.../apple/AppleProToolsEnhancedScreen.kt` | **UI screen** — Main interface |
| `app/.../engine/ActivationEngine.kt` | **Backend** — Tool execution logic |
| `app/.../apple/AppleDeviceViewModel.kt` | **ViewModel** — UI state management |

---

## 🏷️ Tool Categories

1. **ACTIVATION_BYPASS** — iCloud/Activation Lock bypass
2. **MDM_BYPASS** — MDM/DEP profile removal
3. **PASSCODE_BYPASS** — Screen passcode removal
4. **FIRMWARE_TOOLS** — IPSW flash, OTA blocker
5. **CHECKM8_EXPLOIT** — checkm8 bootrom exploit
6. **ICLOUD_TOOLS** — iCloud account tools
7. **DIAGNOSTICS** — Device info, CVE scan
8. **NETWORK_UNLOCK** — Carrier/SIM unlock

---

## 🔧 How to Add a New Tool

### Step 1: Add to Registry

```kotlin
// In AppleToolsModel.kt
AppleTool(
    id = "my_new_tool",
    name = "My New Tool",
    description = "What it does",
    category = AppleCategory.ACTIVATION_BYPASS,
    supportedVersions = "iOS 12–17",
    requiresJailbreak = true,
    riskLevel = RiskLevel.HIGH,
    estimatedTime = "~5 min"
)
```

### Step 2: Add Handler

```kotlin
// In ActivationEngine.kt
"my_new_tool" -> performMyNewTool()

private suspend fun performMyNewTool() {
    _status.value = "Running my new tool..."
    // Implementation here
}
```

### Step 3: Wire Up Execution

```kotlin
// In AppleProToolsEnhancedScreen.kt
"my_new_tool" -> viewModel.executeMyNewTool()
```

---

## 🎨 UI Components

### AppleToolCard
Displays tool info with:
- Icon
- Name & description
- Risk level badge
- iOS version support
- Jailbreak requirement badge

### CategoryFilterChips
Horizontal scrollable chips for filtering tools by category.

### AppleToolDetailsCard
Expanded view showing:
- Full metadata
- Execute button
- Close button

---

## 🔌 ViewModel Functions

| Function | Purpose |
|----------|---------|
| `refreshAppleDevice()` | Get device info |
| `sendIrecoveryCommand(cmd)` | Send iRecovery command |
| `exitRecovery()` | Exit recovery mode |
| `enterDfu()` | Enter DFU mode |
| `clearError()` | Clear error message |
| `clearSuccess()` | Clear success message |

---

## 📊 Risk Levels

| Level | Color | Meaning |
|-------|-------|---------|
| LOW | Green | Safe, non-destructive |
| MEDIUM | Orange | May require reboot |
| HIGH | Red | Irreversible changes |
| CRITICAL | Bright Red | Potential brick risk |

---

## 🚦 Implementation Status

### ✅ Working
- Device detection (Normal/Recovery/DFU)
- iRecovery command execution
- Mode switching (DFU/Recovery)
- Device info retrieval
- MDM profile parsing
- CVE intelligence scan

### ⚠️ Stubbed (Need Implementation)
- Hello screen bypass
- Passcode removal
- MDM bypass execution
- FMI-OFF API submission
- OTA blocker
- Reset lock

### ❌ Not Started
- IPSW firmware flashing
- iOS downgrade
- SHSH blob saving
- Screen Time bypass
- Carrier unlock
- Baseband backup

---

## 🔍 Common Tasks

### Get All Tools
```kotlin
val allTools = AppleToolsRegistry.ALL_TOOLS
```

### Get Tools by Category
```kotlin
val activationTools = AppleToolsRegistry.getToolsByCategory(
    AppleCategory.ACTIVATION_BYPASS
)
```

### Get No-Jailbreak Tools
```kotlin
val safeTools = AppleToolsRegistry.getNoJailbreakTools()
```

### Get Tool by ID
```kotlin
val tool = AppleToolsRegistry.getToolById("activation_lock_bypass")
```

---

## 🐛 Troubleshooting

### Device Not Detected
1. Check USB connection
2. Verify device is in supported mode (Normal/Recovery/DFU)
3. Check `AppleDeviceMatrix.kt` for mode detection logic

### Tool Execution Fails
1. Check if jailbreak is required
2. Verify iOS version compatibility
3. Check logs in `Apple Console` section
4. Review `ActivationEngine` implementation

### UI Not Showing Tools
1. Verify `AppleToolsRegistry.ALL_TOOLS` is populated
2. Check category filter is not excluding tools
3. Verify Compose state is updating

---

## 📚 Related Documentation

- `APPLE_PRO_TOOLS_REMAPPING.md` — Complete audit and mapping
- `ActivationEngine.kt` — Backend orchestrator
- `AppleToolsModel.kt` — Tool registry (inline docs)

---

## 🆘 Need Help?

1. Check existing tool implementations for patterns
2. Review `AppleDeviceViewModel` for state management
3. See `MdmRemovalScreen.kt` for complex UI example
4. Check `ActivationEngine.kt` for execution patterns

---

*Last updated: 2026-04-18*
*Version: 1.0*
