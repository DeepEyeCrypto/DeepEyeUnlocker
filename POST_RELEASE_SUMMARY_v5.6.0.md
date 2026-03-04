# DeepEyeUnlocker v5.6.0 Post-Release Summary

**Generated**: March 4, 2026  
**Release Date**: March 4, 2026  
**Tag**: `v5.6.0`  
**Status**: 🚀 **RELEASED**

---

## ✅ Changes Summary

### New Files (2)

| File | Purpose |
|------|---------|
| `portable/android/.../engine/EngineDispatcher.kt` | Routes 24 ops × 4 protocol engines (MTK/QC/Samsung/UniSoc) with policy gate |
| `portable/android/.../policy/PolicyEngine.kt` | 4-tier × 5-role enforcement matrix, abuse detection, `PolicyDeniedException` |

### Modified Files (2)

| File | Change |
|------|--------|
| `portable/android/.../usb/UsbSessionManager.kt` | Replaced `[PSEUDO]` executeOperation with real EngineDispatcher; fixed empty-serial PhysicalDeviceKey bug; added error throttle |
| `deepeye-universal/desktop-app/src/DeepEyeFeaturePage.tsx` | Removed phantom feature #25; corrected 9 risk badges to match tier mapping; 6×4=24 cards |

### Version Sync

| File | Version |
|------|---------|
| `src/DeepEyeUnlocker.csproj` | 5.6.0 |
| `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj` | 5.6.0 |
| `portable/android/app/build.gradle` | versionName 5.6.0, versionCode 560 |
| `OtgActivity.kt` | v5.6.0 |
| `README.md` | v5.6.0 |
| `PROJECT_MANIFEST.md` | 5.6.0 |

---

## 🔧 Architecture Additions

```
UI (Compose / React)
  └─ Service Layer (Kotlin / TypeScript)
       └─ PolicyEngine.check(op, role)        ← NEW
            └─ EngineDispatcher.execute(...)   ← NEW
                 └─ executeMtk / executeQualcomm / executeSamsung / executeUnisoc
                      └─ NativeBridge → Device
```

### PolicyEngine Tier Matrix

| Tier | Badge | Min Role | Access |
|------|-------|----------|--------|
| 1 SAFE | `#22C55E` | Consumer | All roles |
| 2 POLICY | `#F59E0B` | Technician | Proof of ownership |
| 3 RESTRICTED | `#EF4444` | Enterprise | KYC required |
| 4 EXPLOIT | `#6B7280` | — | Always rejected |

### EngineDispatcher Coverage

| Engine | Full Routes | Fallback Routes |
|--------|------------|-----------------|
| MTK | 22 ops | 2 via generic |
| Qualcomm | 16 ops | 8 via generic |
| Samsung | 12 ops | 12 via generic |
| UniSoc | 8 ops | 16 via generic |

---

## 🐛 Bugs Fixed

1. **Empty-serial false re-enum match** — `buildPhysicalKey()` returned `PhysicalDeviceKey(vid, pid, "")` which matched any empty-serial device during ReenumerationWait. Now returns `null`.
2. **9 risk badge mismatches** — Desktop UI showed wrong tier colors for 9 of 24 features (e.g., FRP group showed POLICY instead of RESTRICTED).
3. **Phantom feature #25** — "Quick APK Install" was not one of the 24 defined operations; removed from UI.
4. **`[PSEUDO]` executeOperation** — Fake progress loop replaced with real EngineDispatcher call chain.
5. **No error throttle on engine failures** — Flapping USB could spam identical errors; now suppressed after 3× in 5s.

---

## 🎯 Next Steps

1. Wire real user role from auth/license system (currently hardcoded `UserRole.DEV`).
2. Replace engine progress stubs with actual NativeBridge Rust/C++ calls.
3. Add remaining 15 operation buttons to `activity_otg.xml` layout.
4. Implement feature-specific UI flows (MTK MetaMode wizard, Brand Config selector).
5. Monitor CI run for `v5.6.0` tag.

---
*Democratizing Mobile Repair & Security Tools.*
