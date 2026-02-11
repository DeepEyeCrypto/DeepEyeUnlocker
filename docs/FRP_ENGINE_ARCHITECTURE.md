# Universal FRP Service Engine Architecture

## 🛡️ Strategic Goal

To build a brand-agnostic, compliance-first engine for managing Factory Reset Protection (FRP) in legitimate repair and refurbishment scenarios. This engine abstracts low-level protocols (EDL, BROM, Fastboot) into a unified service API, strictly gating operations behind ownership verification.

---

## Stage 0: Brand & Protocol Capability Matrix

**FRP (Factory Reset Protection)** is a mechanism that effectively "locks" a partition (often `frp`, `persistent`, or `config`) to require a Google/OEM account login after a factory reset.

| Brand | SoC Families | Primary Service Protocol | FRP Mechanism | Official/Service Path |
|-------|-------------|-------------------------|---------------|-----------------------|
| **Samsung** | Exynos, Qualcomm, MTK | **Odin / Download Mode** | `persistent` partition, server-side lock | Samsung Knox / E-FOTA / Smart Switch |
| **Xiaomi / Redmi** | Qualcomm | **EDL (9008)** | `frp` or `config` partition | Mi Account Auth / Service Tool |
| **Xiaomi / Redmi** | MediaTek (MTK) | **BROM / Preloader** | `frp` partition | Mi Auth / DA Agent |
| **Oppo / Realme** | Qualcomm, MTK | **EDL / BROM** | proprietary (oppo_custom) | Authorized Service Center Tool |
| **Vivo** | Qualcomm, MTK | **EDL / BROM** | proprietary | Vivo Service Tool |
| **Motorola** | Qualcomm, MTK | **Fastboot / EDL** | `frp` partition | Motorola Rescue & Smart Assistant |
| **Google Pixel** | Tensor, Qualcomm | **Fastboot** | Server-side / `frp` | Android Enterprise / Zero-Touch |
| **Huawei / Honor** | Kirin, Qualcomm | **USB COM 1.0** | `oeminfo` / `frp` | HiSuite / eRecovery |
| **Generic Android** | SC9863A, MTK | **Fastboot / SP Diag** | `persistent` | Android Enterprise Wipe |

---

## Stage 1: DeviceProfile FRP Extensions

We extend the `DeviceProfile` schema to include explicit FRP capabilities. This allows the engine to determine available methods dynamically.

### 1.1 JSON Schema Extension

```json
{
  "frp_capabilities": {
    "supported_protocols": ["EDL", "FASTBOOT"], 
    "frp_partition_name": "frp",      // Logical name of partition storing limits
    "frp_type": "GOOGLE_STANDARD",    // or "SAMSUNG_KNOX", "XIAOMI_MICLOUD"
    "requires_auth_agent": true,      // If true, protocol handshake needs auth
    "official_service_method": "FASTBOOT_ERASE_PERSISTENT",
    "risk_level": "LOW",              // LOW, MEDIUM, HIGH (e.g. brick risk)
    "enterprise_support": true        // Supports standard Android Enterprise clear
  }
}
```

### 1.2 Device Profile Examples

#### A. Samsung Galaxy S23 (Qualcomm)

```json
"frp_capabilities": {
  "supported_protocols": ["ODIN_DOWNLOAD"],
  "frp_partition_name": "persistent",
  "frp_type": "SAMSUNG_KNOX",
  "requires_auth_agent": true,
  "official_service_method": "KNOX_DEPLOYMENT_APP",
  "risk_level": "MEDIUM",
  "enterprise_support": true
}
```

#### B. Xiaomi Redmi Note 11 (Qualcomm)

```json
"frp_capabilities": {
  "supported_protocols": ["EDL"],
  "frp_partition_name": "frp",
  "frp_type": "GOOGLE_STANDARD",
  "requires_auth_agent": false, // On older security patch
  "official_service_method": "EDL_WIPE_FRP_PARTITION",
  "risk_level": "LOW",
  "enterprise_support": false
}
```

#### C. Generic Enterprise Device

```json
"frp_capabilities": {
  "supported_protocols": ["FASTBOOT"],
  "frp_partition_name": "frp",
  "frp_type": "GOOGLE_STANDARD",
  "requires_auth_agent": false,
  "official_service_method": "FASTBOOT_OEM_UNLOCK",
  "risk_level": "LOW",
  "enterprise_support": true
}
```

---

## Stage 2: Universal FRP Engine API

This pseudo-code defines the service contract. It strictly separates the *intent* to clear FRP from the actual *execution*, ensuring verification happens in between.

### 2.1 Core Types

```csharp
// The verification status of the device/user
public enum OwnershipStatus {
    UNKNOWN,
    VERIFIED_ENTERPRISE_OWNER,  // Device belongs to managing org
    VERIFIED_INDIVIDUAL,        // Receipt/Invoice checked
    UNVERIFIED
}

// Context passed to every engine operation
public class FrpServiceContext {
    public DeviceProfile Profile { get; set; }
    public IUsbTransport Transport { get; set; } // Active connection (EDL/BROM/etc)
    public OwnershipStatus Ownership { get; set; }
    public string OperationReason { get; set; }  // e.g. "Refurbish", "Lost Password"
}

public class FrpResult {
    public bool Success { get; set; }
    public string Message { get; set; }
    public bool RequiresReboot { get; set; }
    public Dictionary<string, string> Logs { get; set; }
}
```

### 2.2 IFrpServiceEngine Interface

```csharp
public interface IFrpServiceEngine {
    
    // 1. Capability Check
    // Can we service this device in its current state?
    bool IsSupported(FrpServiceContext ctx);

    // 2. Safe Inspection
    // Check if FRP is actually active (if protocol allows reading)
    // Returns: "LOCKED", "UNLOCKED", "UNKNOWN"
    string CheckLockStatus(FrpServiceContext ctx);

    // 3. Official / User-Friendly Path
    // Returns instructions for official removal (e.g. "Go to Settings > Accounts")
    string GetOfficialInstructions(FrpServiceContext ctx);

    // 4. Service Action (The "Heavy Lifting")
    // executing valid partition operations based on profile
    // Guarded: Throws SecurityException if Ownership != VERIFIED
    FrpResult ExecuteServiceClear(FrpServiceContext ctx);
}
```

### 2.3 Universal Implementation Logic (Abstract)

```csharp
public class UniversalFrpEngine : IFrpServiceEngine {
    
    public FrpResult ExecuteServiceClear(FrpServiceContext ctx) {
        // GUARDRAIL 1: Verification
        if (ctx.Ownership == OwnershipStatus.UNVERIFIED) {
            return FrpResult.Fail("Operation refused: Ownership not verified.");
        }

        // GUARDRAIL 2: Protocol Availability
        var strategy = SelectStrategy(ctx.Profile, ctx.Transport.Protocol);
        
        switch (strategy) {
            case FrpStrategy.EDL_WIPE:
                return PerformEdlWipe(ctx); // Uses QualcommSahara/Firehose
                
            case FrpStrategy.BROM_FORMAT:
                return PerformBromFormat(ctx); // Uses MtkDaAgent
                
            case FrpStrategy.FASTBOOT_ERASE:
                return PerformFastbootErase(ctx); // Uses FastbootTransport
                
            case FrpStrategy.SAMSUNG_ODIN:
                 return FrpResult.Fail("Samsung FRP requires authorized Odin service tool.");
                 
            default:
                return FrpResult.Fail("No safe strategy available for this device.");
        }
    }
}
```

---

## Stage 3: Guardrails & Compliance

To ensure DeepEye remains a legitimate service tool:

1. **Read-Only First**: The engine always attempts to *read* lock status first. If a device is already unlocked, no write operations are performed.
2. **Audit Logging**: Every `ExecuteServiceClear` action logs the Device SN, Time, User ID, and Reason to a local, immutable audit log.
3. **No "Bypass" Hacks**: We do not inject APKs, use accessibility exploits, or crash wizards. We exclusively use **storage partition management** (erasing the config) which is the standard OEM refurbish method.
4. **Enterprise Mode**: Integration with MDM APIs (Google Zero Touch) is prioritized over raw partition wiping when available.

## Next Steps

1. Implement `DeviceProfile` JSON updates for top 20 models.
2. Build `FrpServiceContext` class in C# Core.
3. Implement `StandardQualcommFrpStrategy` (EDL) and `StandardMtkFrpStrategy` (BROM).
