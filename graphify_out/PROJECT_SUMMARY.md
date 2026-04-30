# DeepEye Unlocker - Project Summary

**Context Handle**: `/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/`

---

## ⚡ High-Level Overview
**DeepEye Unlocker** is a professional-grade mobile forensics and device servicing platform. It is designed to handle complex security bypasses, data extraction, and protocol orchestration across multiple hardware platforms (Android & iOS).

### Key Mission
To provide a unified, ultra-low latency interface for mobile security research, enabling access to locked devices through hardware-level protocols (EDL, BROM, DFU) while ensuring data integrity via real-time signal monitoring.

---

## 🏗️ Core Architecture
The project employs a **Hybrid Orchestration Architecture**:

1.  **Desktop Layer (Tauri + Rust)**:
    -   **Backend**: Rust implementation of low-level USB protocols using `rusb` and `tokio-serial`.
    -   **Command Center**: `src-tauri/src/lib.rs` exports 100+ commands to the frontend, covering everything from `adb` to `checkm8`.
2.  **Mobile Layer (Kotlin)**:
    -   **Engine**: `BypassOperationEngine.kt` acts as the primary router for Android-based operations.
    -   **Pattern**: Clean Architecture (Domain -> Data -> UI) with Hilt for DI and Coroutines for async execution.
3.  **Frontend Layer (React + Vite)**:
    -   **Design**: "Liquid Glass" premium UI using Tailwind CSS 4 and Framer Motion.
    -   **Modularity**: 15+ specialized modules (e.g., `ExploitOrchestrator`, `RamdiskMaster`) that consume the Tauri API.

---

## 🛠️ Logic Hubs

### 1. Exploit Orchestrator (`src/modules/ExploitOrchestrator`)
The central "brain" of the application logic. It maps device states to specific exploit sequences (e.g., DFU -> checkm8 -> Ramdisk -> Bypass).

### 2. Device Database (`DeepEyeDeviceDB/`)
A comprehensive repository of SoC signatures, DA binaries, and Firehose programmers. It allows the tool to "auto-route" based on VID/PID or chip identity.

### 3. Protocol Engines
-   **MTK BROM/META**: Handshake, DA upload, and partition-level access for MediaTek devices.
-   **Qualcomm EDL**: Sahara and Firehose protocol implementations for partition reading/writing.
-   **Apple DFU/Recovery**: Low-level communication for SHSH blobs, activation records, and pwned DFU states.

---

## 🔐 Security & Integrity
-   **Cryptography**: Dual-layer AES-256 for data encryption.
-   **Signal Monitoring**: "Eye-Diagram" analysis (Stage 600.1) monitors signal integrity during high-speed USB extractions to prevent data corruption.
-   **Zero-Latency Path**: Keystroke and input handling are optimized to ensure a responsive, editor-grade experience.

---

## 🚀 Key Features
-   **iOS Bypass**: Signal-bypass for A12+ devices, MDM removal, iCloud activation bypass.
-   **Android Servicing**: Samsung FRP erase, Xiaomi Mi Account removal, MTK bootloader unlock.
-   **Forensics**: Deep extraction, ramdisk mounting, and filesystem analysis.
-   **Live Monitoring**: Real-time logcat streaming and protocol handshake diagnostics.

---

**Last Updated**: April 24, 2026
**Session ID**: a4ea6a23-bd40-43de-a6c9-96dbddbf9784
