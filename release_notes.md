### ✨ DeepEye Stability God Release (v2026.14)

---

#### 🛡️ USB / OTG Hardening

- **SafeBulkTransfer Engine**: Implemented protocol-tuned timeouts, 16KB chunking, and automatic stall detection (CLEAR_FEATURE) to stabilize high-speed data transfers.
- **USB Lifecycle Manager**: 10-state machine (Idle→Connected→Dead) to prevent race conditions during rapid cable flapping and activity recreation.
- **OEM Compatibility Layer**: Hardened logic for Samsung (buffer padding), MIUI (50ms post-claim delay), Huawei/Honor (3x open retry), and Vivo (OTG settings link).
- **USB Foreground Service**: Prevents Android battery-savers from killing long-running USB sessions like full firmware flashing.

#### 📊 Live Diagnostics & UI

- **Connection Health UI**: Real-time health monitoring (Healthy, Degraded, Dead) integrated into the top bar.
- **Connection Test Screen**: Comprehensive 8-step diagnostic for testing phone-to-phone OTG connectivity without drivers.
- **OemWarningBanner**: Context-aware instructions for brand-specific USB settings (Vivo OTG, MIUI Power Saver, etc.).

#### 🛠️ Internal Refinements

- **Version Bump**: Migrated to `versionCode 20274`.
- **Logic Sync**: All USB logic now flows through the central `UsbLifecycleManager` for 100% thread-safety.
- **Cleanup**: Removed deprecated `UsbConnectionController` in favor of the new Lifecycle architecture.
