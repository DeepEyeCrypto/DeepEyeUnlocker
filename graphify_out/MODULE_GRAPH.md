# DeepEye Unlocker - Module Graph

## 🧩 Module Mapping (Frontend to Backend)

| Frontend Module (`src/modules/`) | Tauri Command Hub (`commands/`) | Android Executor |
|---------------------------------|---------------------------------|-------------------|
| `ActivationLock`                | `apple.rs`, `activation.rs`     | `ServerExecutor`  |
| `AdbTerminal`                   | `adb.rs`                        | `AdbExecutor`     |
| `AppleIdRemoval`                | `apple_id.rs`                   | `ServerExecutor`  |
| `BypassEngine`                  | `bypass.rs`, `hello_bypass.rs`  | `BypassOperationEngine` |
| `DeepExtraction`                | `extraction.rs`, `afc.rs`       | `PartitionAdapter` |
| `DfuRestore`                    | `dfu_restore.rs`, `restore.rs`  | `IosOtgSession`   |
| `ExploitOrchestrator`           | `orchestrator.rs`, `rebuild.rs` | `BypassOperationEngine` |
| `IdentityForensics`             | `identity.rs`, `diagnostics.rs` | `ProtocolProbe`   |
| `RamdiskMaster`                 | `ramdisk.rs`, `checkm8.rs`      | `NativeBridge`    |
| `ScreenTimeCrack`               | `screentime.rs`, `ios_backup.rs`| `PythonBridge`    |

---

## 🌲 Rust Module Hierarchy (`src-tauri/src/`)
-   **`lib.rs`**: Root entry point and command registration.
-   **`commands/`**: Specialized handlers for Tauri IPC calls.
    -   `adb.rs`, `apple.rs`, `mtk_brom.rs`, `edl.rs`, `samsung.rs`, `unisoc.rs`.
-   **`usb/`**: Low-level USB session management and descriptors.
-   **`db/`**: Persistence layer (SQLite handlers for `history` and `config`).
-   **`qualcomm/`, `unisoc/`**: Chipset-specific protocol implementations.

---

## 🧬 Kotlin Package Structure (`app/src/main/kotlin/com/deepeye/otg/`)
-   **`data.gsmg`**: Core business logic and the `BypassOperationEngine`.
-   **`protocol`**: Concrete implementations of ADB, MTK V6, and Server APIs.
-   **`usb`**: `AdbSession`, `UsbPermissionManager`, and session state tracking.
-   **`python`**: `PythonBridge` for cross-language exploit execution (via Chaquopy).
-   **`ui`**: Compose-based screens mirroring the React module functionality.

---

## 🔗 Dependency Direction
1.  **UI Modules** depend on **Tauri API / Android Service**.
2.  **Logic Hubs** depend on **Protocol Executors**.
3.  **Executors** depend on **USB/Serial Transport Libraries**.
4.  **All** depend on **Device Database** for signature matching.

---
**Last Updated**: April 24, 2026
