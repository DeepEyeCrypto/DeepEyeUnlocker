# DeepEyeUnlocker Hybrid Engine v3.0 Strategy

## "Miracle Box Universal + UnlockTool Precision"

### 1. Architectural Philosophy

DeepEyeUnlocker v3.0 adopts a **Hybrid Engine** approach to solve the fragmentation problem in the mobile unlocking ecosystem.

* **Miracle Box Philosophy (Chipset-Centric)**:
  * Focuses on the *Silicon* (MTK, SPD, Qualcomm).
  * Pro: Universal compatibility (works on thousands of unbranded/legacy phones).
  * Pro: Offline-capable.
  * Con: High risk (can brick if preloader doesn't match), manual configuration often required.

* **UnlockTool Philosophy (Model-Centric)**:
  * Focuses on the *Device Model* (e.g., SM-S921B).
  * Pro: High precision and safety (handlers are tuned for specific partitions/auth).
  * Pro: Handles modern security (Knox, Auth-based EDL).
  * Con: Requires constant updates and internet connectivity.

### 2. The Hybrid Strategy

The **Hybrid Engine** dynamically routes operations based on device characteristics, balancing safety with versatility.

```mermaid
graph TD
    A[Device Connected] --> B{Device Profile Found?}
    B -- Yes, Model Specific Profile --> C[UnlockTool Mode]
    B -- No / Generic Profile --> D{Chipset Identified?}
    C --> E[Precision Handler (Safe, Online)]
    D -- Yes --> F[Miracle Mode (Universal, Offline)]
    D -- No --> G[Manual Selection Required]
    F --> H[Generic Chipset Protocol]
```

### 3. Strategy Matrix

| Feature | Legacy / Budget (Miracle Mode) | Flagship / Modern (UnlockTool Mode) |
| :--- | :--- | :--- |
| **Primary Identifier** | Chipset (MT67xx, SDM845) | Model Number (SM-A546E) |
| **Protocol Strategy** | Generic (BROM, FDL, Firehose) | Specialized (Odin, Secrecy, Auth-EDL) |
| **Connectivity** | Offline-First | Online-First (Cloud Sync) |
| **Risk Level** | Medium/High (User discretion) | Low (Verified) |
| **Target Era** | 2010 - 2021 | 2021 - Present |
| **Plugin Example** | `MtkBootRomUniversalPlugin` | `SamsungS24Handler` |

### 4. Implementation Components

#### 4.1 Miracle Mode (Universal Plugins)

Implements protocol-level attacks that work across an entire chipset family.

* **MTK**: BootROM exploits (SLA/DAA bypass) for read/write/format.
* **SPD**: FDL loader injection for generic Diag/Flash operations.
* **Qualcomm**: Generic Firehose loader database for unauthenticated EDL.

#### 4.2 UnlockTool Mode (Model-Specific)

Implements "Recipes" or scripts tied to a specific `DeviceProfile` ID.

* **Profile Sync**: Downloads definitions from DeepEye Cloud.
* **Auth Handling**: Manages server-side signatures (Xiaomi Auth, Samsung Knox).
* **Precision**: Uses exact partition offsets and specific payloads.

#### 4.3 Hybrid Router (`HybridOperationRouter`)

The brain of the operation.

1. **Auto-Detection**: Queries VID/PID and handshake data.
2. **Safety Check**: If a modern secure device is detected (e.g., Secure Boot enabled), forces Model-Specific mode or warns user.
3. **Routing**: Dispatches `ExecuteAsync` to the appropriate plugin.

### 5. Safety Boundaries

* **IMEI Repair**: Strictly disabled.
* **FRP Removal**: Requires user confirmation of ownership.
* **Warning System**: Universal methods must display a "Risk of Bricking" warning before execution on unrecognized devices.
