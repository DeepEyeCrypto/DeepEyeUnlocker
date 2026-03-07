### ✨ DeepEye Unconditional Architecture (v2026.15)

---

#### 🏛️ Domain Models & Data Integrity

- **Unified Truth Source**: Established `DomainModels.kt` to house robust definitions for `ProtocolFamily`, `DeviceMode`, `PolicyTier`, `OperationAvailability`, and `DeepEyeOperation` to replace fragile ad-hoc primitives and strings.
- **DeepEyeCatalogs Source Tree**: Implemented `DeepEyeCatalogs` ensuring the `MODE_CATALOG` and `FEATURE_GROUPS` definitions act as the strict foundational layout elements across the entire lifespan of the application context.

#### 👁️ Unconditional UI Generation

- **Full Transparency Engine**: Scrapped legacy dynamically disappearing maps. The `MainScreen` is now 100% unconditional. All known modes and features remain visually present regardless of detected state.
- **Availability Calculations**: Designed `AvailabilityEngine` to run live state resolutions returning `OperationAvailability`, allowing unfulfilled prerequisites (wrong mode, missing permissions, lower policy tier) to naturally gray out cards and label *why* the restriction exists instead of invisibly rendering.

#### 🛠️ Security Enforcement Upgrades

- **Strict Enums for PolicyTier**: `PolicyEngine` enforces exact policy compliance via immutable definitions (`SAFE`, `POLICY`, `RESTRICTED`, `NEVER`). The `NEVER` flag strictly blocks system-exploit or unstable routes natively up to the C++ NDK boundary.
- **NDK Warnings Repaired**: Re-mapped system `local.properties` to fully drop `ndk.dir` bindings, eliminating noisy Gradle deprecation alerts in favor of pure `android.ndkVersion` alignment.
