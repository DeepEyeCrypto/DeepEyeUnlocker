# DeepEye Unlocker - Context Handle

**Context Handle**: `/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/`

---

## 🧠 Summary Node (Session Persistence)

**Project State**: DeepEye Unlocker v2027.18.1
**Primary Objective**: Finalizing production-ready protocol implementations for mobile forensics and device unlocking.

### Current Progress
-   [x] **Architecture**: Fully mapped hybrid Rust/Kotlin/React system.
-   [x] **UI**: Premium Liquid Glass system implemented with modular architecture.
-   [x] **Protocols**: ADB, MTK BROM (V6), and Apple DFU/Bypass sequences are functional.
-   [x] **Context**: Graphify memory generated and exported to `graphify_out`.

### Critical Context for Next Session
1.  **Entry Point**: `src-tauri/src/lib.rs` for desktop commands; `BypassOperationEngine.kt` for Android.
2.  **Hot Path**: All USB logic is in Rust (`rusb`) or Kotlin (`UsbManager`). Avoid adding latency-heavy logic here.
3.  **UI Tokens**: Use the glassmorphism tokens defined in `tailwind.config.js` and `src/index.css`.
4.  **Database**: Refer to `DeepEyeDeviceDB` for device-specific signatures and binaries.

### How to Resume
Paste this handle into any AI assistant to load the project's technical architecture, tech stack, and module graph instantly.

---
**Timestamp**: 2026-04-24T00:05:00+05:30
**Architect**: Antigravity (Senior Software Context Architect)
