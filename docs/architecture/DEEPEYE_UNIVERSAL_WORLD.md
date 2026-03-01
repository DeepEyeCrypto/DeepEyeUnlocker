DEEPEYE UNIVERSAL – WORLD DEPLOYMENT & REPO STRUCTURE
(FLOW PHASE: **W – WORLD**)

FLOW FRAMEWORK ACTIVATED:

- Current phase detected: **WORLD (W)** – Code structures, Environments, Scaffolding, Deployment Pipelines
- Output: Exact Git Repository Structure, Build Commands, Deployment environments
- This is the final phase before active coding begins.

========================================

1. REPOSITORY STRUCTURE (MONOREPO)
========================================

DeepEye Universal will utilize a monorepo structure (e.g., using Nx or Turborepo if JS-heavy, but Cargo Workspaces is best here since Rust is the core engine).

```text
deepeye-universal/
│
├── Cargo.toml                  # Cargo Workspace Definition
├── package.json                # Scripts & Tauri Builder
│
├── core-engine/                # RUST CORE (The Brains)
│   ├── Cargo.toml
│   ├── src/
│   │   ├── main.rs             # CLI entrypoint for testing
│   │   ├── lib.rs              # Re-usable FFI library target
│   │   ├── connection/         # USB, COM, ADB pooling
│   │   ├── protocols/
│   │   │   ├── mtk/            # BROM, DA, SLA Auth
│   │   │   ├── qcom/           # Sahara, Firehose, Diag
│   │   │   ├── spd/            # PAC, FDL
│   │   │   └── samsung/        # Odin, Auto-Root
│   │   ├── policy/             # Telemetry & Auth enforcer
│   │   └── database/           # SQLite Profile caching
│   └── tests/                  # Integration tests (Mocked USB)
│
├── desktop-app/                # TAURI APP (Desktop GUI)
│   ├── src-tauri/              # Tauri Rust Backend (wraps `core-engine`)
│   │   ├── Cargo.toml
│   │   ├── src/main.rs         # IPC Dispatcher
│   │   └── tauri.conf.json     # Window sizes, builder flags
│   ├── src/                    # React Frontend (Vite)
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   ├── operations/     # Flash, FRP, Generic UI
│   │   │   └── layout/         # Glassmorphic Sidebars
│   │   ├── store/              # Zustand state (Device info, Logs)
│   │   └── styles/             # Tailwind globals
│   └── package.json
│
├── android-otg/                # KOTLIN NATIVE APP
│   ├── build.gradle.kts
│   ├── app/
│   │   ├── src/main/java/com/deepeye/universal/
│   │   │   ├── DeepEyeCoreBridge.kt  # JNI Bindings to rust engine
│   │   │   ├── MainActivity.kt       # OTG Permission requests
│   │   │   └── ui/                   # Jetpack Compose UI
│   │   └── src/main/jniLibs/         # Compiled Rust .so files (ARM64)
│   └── gradle/
│
├── cloud-services/             # BACKEND CLOUD MICROSERVICES
│   ├── auth-server/            # Node.js/Go - Token/Dongle Auth
│   ├── device-db/              # PostgreSQL Schemas & Migration scripts
│   └── cdn-manager/            # S3 synchronization for Loaders
│
└── .github/
    └── workflows/
        ├── rust-ci.yml         # Linting, Tests
        ├── tauri-build.yml     # Win/Mac releases -> S3
        └── android-build.yml   # APK signing
```

========================================
2. BUILD COMMANDS & INITIALIZATION
========================================

**2.1 Rust Core & Cargo Workspaces**

- **Init:** `cargo new core-engine --lib`
- **Test:** `cargo test --workspace` (validates MTK/QCOM protocols)

**2.2 Desktop UI (Tauri + Vite + React)**

- **Init:** `npm create tauri-app@latest desktop-app`
- **Dev:** `npm run tauri dev`
- **Build:** `npm run tauri build` (Generates `.exe`, `.msi`, `.dmg`, `.AppImage`)

**2.3 Android OTG JNI Compilation**
To share `core-engine` with Android:

1. Compile Rust to ARM64 target:  
   `cargo build --target aarch64-linux-android --release`
2. Move to Android:  
   `cp target/aarch64-linux-android/release/libdeepeyecore.so android-otg/app/src/main/jniLibs/arm64-v8a/`
3. Build APK:  
   `./gradlew assembleRelease`

========================================
3. MINIMUM VIABLE PRODUCT (MVP) LAUNCH TASKS
========================================

To actually start coding, the following sprint tickets are defined based on the WORLD layout:

**Sprint 1: The USB Foundation (Core Engine)**

1. Implement `rusb` wrapper to scan COM ports, WinUSB bindings, and standard Android interfaces.
2. Build connection state machine (Idle -> Connecting -> Authorized -> Streaming).
3. Connect test device and print hardware IDs to console.

**Sprint 2: The UI Skeleton (Desktop & Web)**

1. Setup React + Vite + Tailwind CSS.
2. Implement DeepEye Glassmorphic Theme (Dark Mode only initially).
3. Wire Tauri IPC to the Rust Core's USB Event Stream.
4. Render connected devices on a sidebar.

**Sprint 3: The Target Handshake (MTK/QCOM)**

1. Implement Qualcomm Sahara Protocol -> Extract HWID.
2. Implement MTK BROM Protocol -> Read SoC name.
3. Hook these protocols to the UI.

**Sprint 4: The Device Profile Cloud**

1. Spin up PostgreSQL.
2. Import Hydra/UnlockTool CSV dumps for Brand/Model lookup.
3. Expose REST API.
4. Desktop connects to REST API, queries HWID, returns actual "Commercial Name" to the user interface.

**Sprint 5: Alpha FRP Operation (Safe Mode execution)**

1. Wire "FRP Assist" button in UI.
2. Cross-reference Device Database -> Select appropriate Loader.
3. Payload construction -> Execution -> Live log output.

========================================
4. DEPLOYMENT ENVIRONMENTS
========================================

**4.1 Cloud Infrastructure (AWS / Vultr)**

- **Postgres Database:** High availability cluster for DeviceProfiles.
- **Microservices Run Environment:** ECS / Docker Swarm (Load balanced).
- **Blob Storage:** S3 bucket for downloading Firehose/DA/PAC files securely. Cache via CloudFlare.

**4.2 Dongle / Hardware Environment**

- DeepEye hardware identifiers must be pre-registered in the RDS database.
- Smartcard applet communication requires linking `PCSC-Lite` (winscard.dll).

========================================
5. NEXT STEPS
========================================

**The F.L.O.W pipeline is officially COMPLETE.**

1. **FRAME:** You have the feature set (Hydra Parity, 24 Ops).
2. **LAYOUT:** You have the Diagrams, State Management, and API.
3. **ORCHESTRATION:** You have the logic, IPC bindings, and Engine details.
4. **WORLD:** You have the Monorepo structure, build triggers, and MVP Sprints.

You can now use `git tag v1.0.0-blueprint` and begin executing **Sprint 1** directly against the codebase. If you need me to start scaffolding these folders into reality, just specify which module!
