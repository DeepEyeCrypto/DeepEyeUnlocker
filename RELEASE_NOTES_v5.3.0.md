# DeepEyeUnlocker v5.3.0 - "The Parity & Security Update" 🚀

This release fulfills the **UnlockTool Parity (Stages 1-8)** milestone and addresses critical vulnerabilities identified by the TestSprite Audit.

## 💎 Key Highlights

- **UnlockTool Parity**: Full protocol support for Stage 1-8 (Handshake, Auth Bypass, Partition Management, and Advanced Service Mode).
- **Streaming 2.0**: Completely refactored `BackupOperation` and `FlashOperation` to use direct streaming. This resolves **TC_05 (Memory Overflow)** during large partition backups (e.g., `userdata`).
- **Sahara Hardening**: Added bounds checking and size validation for all Sahara packets. This fixes **TC_04** and **TC_06**, preventing crashes or memory corruption from malformed device data.
- **Unified Versioning**: Synchronized all components (Windows WPF, Core Library, and Android Bridge) to a single version tree.

## 🔧 Technical Changes

- **Qualcomm**: Fixed `FirehoseProtocol` memory usage in `ReadPartitionAsync`.
- **MediaTek**: Implemented real `ReadToStreamAsync` and `WriteFromStreamAsync` in `MTKDAProtocol`.
- **Core:** Deprecated `byte[]` based partition reading for operations exceeding 100MB.
- **UI:** Updated Dashboard to reflect "UnlockTool Parity" status.

## 📂 Artifact Details

- **Tag**: `v5.3.0`
- **Build**: [GitHub Actions #XXXX]
- **Status**: PRODUCTION READY

---
*Democratizing Mobile Repair & Security Tools.*
