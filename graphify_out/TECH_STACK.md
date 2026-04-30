# DeepEye Unlocker - Technology Stack

## 💻 Frontend (Desktop/Web)
-   **Framework**: React 18.3.1
-   **Build Tool**: Vite 5.2.11
-   **Language**: TypeScript 5.4.5 (Strict Mode)
-   **Styling**: 
    -   Tailwind CSS 4.2.2 (using `@tailwindcss/postcss`)
    -   Framer Motion 12.38.0 (Animations)
    -   Radix UI (Primitives)
-   **Iconography**: `lucide-react`, `dicons`
-   **Testing**: Jest + TS-Jest

## 🦀 Desktop Backend (Rust)
-   **Framework**: Tauri 2.0
-   **Features**:
    -   `macos-private-api`: For advanced system integration.
    -   `tauri-plugin-shell`, `sql`, `dialog`, `fs`, `os`, `updater`.
-   **Core Libraries**:
    -   `rusb`: Cross-platform USB library.
    -   `tokio`: Async runtime (full features).
    -   `serialport` & `tokio-serial`: Hardware serial communication.
    -   `rusqlite`: Embedded SQLite for history.
    -   `reqwest`: HTTP client for cloud sync.
    -   `serde`: JSON serialization/deserialization.

## 🤖 Mobile Backend (Android)
-   **Language**: Kotlin
-   **Build System**: Gradle (Kotlin DSL)
-   **Architecture**: Clean Architecture (Domain, Data, UI)
-   **Dependency Injection**: Hilt / Dagger
-   **Concurrency**: Coroutines & Flow
-   **Networking**: Retrofit / OkHttp
-   **USB**: Android USB Host API (`UsbManager`)
-   **Logging**: Timber

## 🔌 Hardware Protocols
-   **Android**:
    -   **ADB**: RealAdbExecutor (Shell, Sideload, Pull/Push).
    -   **MTK**: BROM Handshake, DA Execution, META Mode.
    -   **Qualcomm**: Sahara, Firehose (EDL).
    -   **Samsung**: ODIN / Download Mode protocols.
-   **iOS**:
    -   **DFU**: Pwned DFU (Gaster/Checkm8).
    -   **Protocols**: AFC (Apple File Conduit), SHSH Blobs, Activation Records.

## 🗄️ Storage & Sync
-   **Local**: SQLite (History, Settings, Device DB cache).
-   **Cloud**: REST API (Bypass Server URL: `https://api.deepeye.bypass/v2`).
-   **Artifacts**: DMG (macOS), APK (Android).

---
**Last Updated**: April 24, 2026
