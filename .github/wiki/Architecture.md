# Architecture Overview

Deep technical overview of the DeepEye Unlocker codebase.

---

## Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| **Frontend** | React + TypeScript | 18.3.1 |
| **Desktop Framework** | Tauri v2 | 2.10.1 |
| **Backend** | Rust | 2021 Edition |
| **Mobile UI** | Jetpack Compose | Latest |
| **Native Core** | C++17 (NDK) | 25.1.8937393 |
| **Build Tool** | Vite | 5.2.11 |
| **Bundler** | Gradle | 8.12 |

---

## Project Structure

```
DeepEyeUnlocker/
├── src/                          # React frontend source
│   ├── components/               # UI components
│   │   ├── Layout/              # Layout components
│   │   ├── pages/               # Page components
│   │   ├── ui/                  # UI primitives
│   │   └── *.tsx                # Component files
│   ├── lib/                     # Utility libraries
│   ├── modules/                 # Feature modules
│   ├── pages/                   # Route pages
│   ├── styles/                  # CSS stylesheets
│   ├── App.tsx                  # Main app component
│   └── main.tsx                 # Entry point
├── src-tauri/                   # Tauri Rust backend
│   ├── src/                     # Rust source
│   │   ├── commands/            # Tauri commands
│   │   │   ├── apple.rs        # Apple device commands
│   │   │   ├── rom_flasher.rs  # ROM flashing commands
│   │   │   └── ...
│   │   └── lib.rs              # Main library
│   ├── python/                 # Bundled Python scripts
│   ├── resources/              # Platform resources
│   ├── Cargo.toml              # Rust dependencies
│   └── tauri.conf.json         # Tauri configuration
├── app/                        # Android application
│   └── src/main/               # Kotlin source
├── DeepEyeDeviceDB/            # Device database module
└── docs/                       # Documentation
```

---

## Tauri Architecture

### Command Pattern

Tauri commands are the bridge between frontend JavaScript and Rust backend.

```rust
// src-tauri/src/commands/apple.rs

#[tauri::command]
pub async fn apple_device_info(
    app: AppHandle,
    udid: Option<String>,
) -> Result<AppleDeviceInfo, String> {
    let tool = get_tool_path(&app, "ideviceinfo")?;
    
    let output = app.shell()
        .command(tool)
        .args(&["-s", udid.as_deref().unwrap_or_default()])
        .output()
        .await
        .map_err(|e| e.to_string())?;
    
    parse_device_info(&output.stdout)
}
```

### Tool Path Resolver

The `get_tool_path()` function resolves bundled tool paths across platforms:

```rust
// src-tauri/src/commands/apple.rs

fn get_tool_path(app: &AppHandle, tool: &str) -> Result<PathBuf, String> {
    let resource_dir = app.path()
        .resource_dir()
        .map_err(|e| e.to_string())?;
    
    let tool_path = match std::env::consts::OS {
        "macos" => resource_dir
            .join("macos")
            .join(format!("{}", tool)),
        "windows" => resource_dir
            .join("windows")
            .join(format!("{}.exe", tool)),
        "linux" => resource_dir
            .join("linux")
            .join(tool),
        _ => return Err("Unsupported platform".to_string()),
    };
    
    if !tool_path.exists() {
        return Err(format!("Tool not found: {:?}", tool_path));
    }
    
    Ok(tool_path)
}
```

### Command Registration

Commands are registered in `lib.rs`:

```rust
// src-tauri/src/lib.rs

use commands::apple::{
    apple_device_info,
    apple_irecovery_cmd,
    apple_exit_recovery,
    apple_enter_dfu,
};

pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            // Apple commands
            apple_device_info,
            apple_irecovery_cmd,
            apple_exit_recovery,
            apple_enter_dfu,
            // Android commands
            adb_command,
            fastboot_command,
            // ... other commands
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

---

## Frontend Architecture

### React + TypeScript Pattern

```typescript
// src/lib/apple.ts

import { invoke } from '@tauri-apps/api/core';

export interface AppleDeviceInfo {
  serial: string;
  imei: string;
  model: string;
  ios_version: string;
  activation: {
    find_my_iphone: boolean;
    activation_lock: boolean;
  };
}

// Typed command invocations
export async function getAppleDeviceInfo(udid?: string): Promise<AppleDeviceInfo> {
  return invoke<AppleDeviceInfo>('apple_device_info', { udid });
}

export async function sendIrecoveryCommand(command: string): Promise<string> {
  return invoke<string>('apple_irecovery_cmd', { command });
}
```

### Component Structure

```typescript
// src/components/pages/AppleDevice.tsx

import { useState, useEffect } from 'react';
import { getAppleDeviceInfo } from '@/lib/apple';

export function AppleDevice() {
  const [deviceInfo, setDeviceInfo] = useState<AppleDeviceInfo | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDeviceInfo = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const info = await getAppleDeviceInfo();
      setDeviceInfo(info);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="apple-device-panel">
      <button onClick={fetchDeviceInfo} disabled={loading}>
        {loading ? 'Reading...' : 'Get Device Info'}
      </button>
      
      {error && <div className="error">{error}</div>}
      
      {deviceInfo && (
        <div className="device-info">
          <p>Model: {deviceInfo.model}</p>
          <p>iOS: {deviceInfo.ios_version}</p>
          <p>Serial: {deviceInfo.serial}</p>
        </div>
      )}
    </div>
  );
}
```

---

## Design System

### Platform Token System

CSS custom properties for consistent theming:

```css
/* src/styles/tokens.css */

:root {
  /* Background colors */
  --bg-start: #05050F;
  --bg-end: #0A0015;
  
  /* Accent colors */
  --accent-purple: #9C6FFF;
  --accent-purple-dark: #6B2FE0;
  
  /* Tier colors */
  --tier-safe: #69FF47;
  --tier-policy: #FFD740;
  --tier-restricted: #FF6E6E;
  
  /* Glass effects */
  --glass-bg: rgba(255, 255, 255, 0.05);
  --glass-border: rgba(255, 255, 255, 0.12);
  --glass-blur: 20px;
}
```

### Glassmorphism Components

```css
/* src/styles/glass.css */

.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(var(--glass-blur));
  border-radius: 12px;
  padding: 16px;
}

.glass-button {
  background: linear-gradient(135deg, var(--accent-purple), var(--accent-purple-dark));
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  color: white;
  font-weight: 600;
  cursor: pointer;
}
```

---

## Native Bridge (Android)

### JNI Architecture

```kotlin
// Native bridge for C++ core

class NativeBridge {
    companion object {
        init {
            System.loadLibrary("deepeye-core")
        }
        
        @JvmStatic
        external fun initialize(): Int
        
        @JvmStatic
        external fun detectDevice(vid: Int, pid: Int): DeviceInfo
        
        @JvmStatic
        external fun sendEdlCommand(command: ByteArray): ByteArray
    }
}
```

### C++ Core Interface

```cpp
// native-lib.cpp

extern "C" {
    JNIEXPORT jint JNICALL
    Java_com_deepeye_otg_bridge_NativeBridge_initialize(JNIEnv* env, jclass clazz) {
        return DeepEye::Core::initialize();
    }
    
    JNIEXPORT jobject JNICALL
    Java_com_deepeye_otg_bridge_NativeBridge_detectDevice(
        JNIEnv* env, 
        jclass clazz,
        jint vid,
        jint pid
    ) {
        auto device = DeepEye::Protocols::detect(vid, pid);
        return device.toJavaObject(env);
    }
}
```

---

## USB Protocol Layer

### Protocol Detection

```kotlin
// Protocol detection based on USB descriptors

object ProtocolDetector {
    fun detect(snapshot: UsbDescriptorSnapshot): DetectionResult {
        return when {
            // Apple devices (VID 0x05AC)
            snapshot.vid == 0x05AC -> detectApple(snapshot)
            
            // Qualcomm EDL (0x9008)
            snapshot.vid == 0x05C6 && snapshot.pid == 0x9008 -> 
                DetectionResult(DeviceMode.EDL, ProtocolFamily.QUALCOMM)
            
            // MediaTek BROM
            snapshot.vid == 0x0E8D ->
                DetectionResult(DeviceMode.BROM, ProtocolFamily.MEDIATEK)
            
            // Samsung Odin
            snapshot.vid == 0x04E8 ->
                DetectionResult(DeviceMode.ODIN, ProtocolFamily.SAMSUNG)
            
            // Fastboot
            isFastbootInterface(snapshot) ->
                DetectionResult(DeviceMode.FASTBOOT, ProtocolFamily.ANDROID)
            
            // ADB
            isAdbInterface(snapshot) ->
                DetectionResult(DeviceMode.ADB, ProtocolFamily.ANDROID)
            
            else -> DetectionResult(DeviceMode.UNKNOWN, ProtocolFamily.UNKNOWN)
        }
    }
}
```

---

## State Management

### Rust State (Tauri)

```rust
// src-tauri/src/state.rs

use std::sync::Mutex;
use tauri::State;

pub struct AppState {
    pub current_device: Mutex<Option<Device>>,
    pub operation_queue: Mutex<Vec<Operation>>,
}

#[tauri::command]
fn get_current_device(state: State<AppState>) -> Option<Device> {
    state.current_device.lock().unwrap().clone()
}
```

### React State

```typescript
// Using React hooks for local state
// No global state manager (Redux/Zustand) needed for this scale

export function useDevice() {
  const [device, setDevice] = useState<Device | null>(null);
  const [status, setStatus] = useState<DeviceStatus>('disconnected');
  
  useEffect(() => {
    // Poll for device changes
    const interval = setInterval(async () => {
      const current = await checkDeviceConnection();
      if (current?.id !== device?.id) {
        setDevice(current);
        setStatus(current ? 'connected' : 'disconnected');
      }
    }, 1000);
    
    return () => clearInterval(interval);
  }, [device]);
  
  return { device, status };
}
```

---

## Adding New Commands

### 1. Create Rust Command

```rust
// src-tauri/src/commands/my_feature.rs

use tauri::AppHandle;

#[tauri::command]
pub async fn my_new_command(
    app: AppHandle,
    param: String,
) -> Result<String, String> {
    // Implementation
    let result = do_something(param).await?;
    Ok(result)
}
```

### 2. Register in lib.rs

```rust
// src-tauri/src/lib.rs

mod commands {
    pub mod my_feature;
}

use commands::my_feature::my_new_command;

.invoke_handler(tauri::generate_handler![
    my_new_command,
    // ...
])
```

### 3. Create TypeScript Wrapper

```typescript
// src/lib/my_feature.ts

import { invoke } from '@tauri-apps/api/core';

export async function myNewCommand(param: string): Promise<string> {
  return invoke<string>('my_new_command', { param });
}
```

### 4. Use in Component

```typescript
// src/components/pages/MyFeature.tsx

import { myNewCommand } from '@/lib/my_feature';

export function MyFeature() {
  const handleClick = async () => {
    const result = await myNewCommand('test');
    console.log(result);
  };
  
  return <button onClick={handleClick}>Run</button>;
}
```

---

## Build Configuration

### Tauri Bundle Targets

```json
// src-tauri/tauri.conf.json
{
  "bundle": {
    "targets": ["dmg", "nsis", "msi", "appimage", "deb"],
    "resources": [
      "python/ios_backup/**",
      "python/ios_bypass/**",
      "resources/linux/**",
      "resources/macos/**",
      "resources/windows/**"
    ]
  }
}
```

### Vite Configuration

```typescript
// vite.config.ts

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    target: 'es2020',
    outDir: 'dist',
  },
});
```

---

## Security Considerations

### Shell Scope

Tauri shell commands are explicitly scoped in `tauri.conf.json`:

```json
{
  "plugins": {
    "shell": {
      "scope": [
        { "name": "ideviceinfo", "cmd": "ideviceinfo", "args": true },
        { "name": "adb", "cmd": "adb", "args": true },
        { "name": "fastboot", "cmd": "fastboot", "args": true }
      ]
    }
  }
}
```

### CSP Policy

Content Security Policy for frontend:

```json
{
  "app": {
    "security": {
      "csp": "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"
    }
  }
}
```
