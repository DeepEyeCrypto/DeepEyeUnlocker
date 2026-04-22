# DeepEye Unlocker - Comprehensive Project Summary

**Last Updated:** April 21, 2026  
**Project Version:** 2027.18.1  
**Status:** Production (Stage 600.1 - Shielded)  
**Classification:** Professional Mobile Forensics Engine

---

## 🎯 Project Purpose

**DeepEye Unlocker** is a professional-grade mobile forensic engine designed for:
- **High-assurance device acquisition** from locked/encrypted mobile devices
- **Bit-level access** to secure storage via ultra-low latency USB orchestration
- **Signal integrity analysis** with tamper detection (Eye-Diagram Analysis)
- **Multi-SoC support** (Qualcomm Sahara/EDL, MTK Brom, Samsung Odin, UniSoc FDL)
- **Double-layer decryption** (FBE UserData + Adoptable Storage/SD card volumes)

---

## 📊 Project Statistics

| Metric | Value |
|--------|-------|
| **Primary Language** | TypeScript/React (Frontend) + Kotlin (Backend) + C++17 (Core) |
| **Build System** | Gradle (Android) + Vite (Frontend) + Tauri (Desktop) |
| **Repository Structure** | Monorepo (Frontend + Android App + Native Libraries) |
| **Module Count** | 16 major functional modules |
| **Target Platforms** | Android devices, macOS desktop (Tauri), Web interface |

---

## 🏗️ System Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│  Layer 0: User Interface (Jetpack Compose + React Liquid Glass) │
├─────────────────────────────────────────────────────────────────┤
│  Layer 1: ViewModel/State Management (Forensic ViewModel)       │
├─────────────────────────────────────────────────────────────────┤
│  Layer 2: JNI Native Bridge (Java/Kotlin ↔ C++ Interop)        │
├─────────────────────────────────────────────────────────────────┤
│  Layer 3: C++17 DeepEye Core Engine (Signal Processing)         │
├─────────────────────────────────────────────────────────────────┤
│  Layer 4: Transport & Protocol Layer (libusb 1.0.26)            │
├─────────────────────────────────────────────────────────────────┤
│  Layer 5: Physical Device Communication (USB HID/Bulk)          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔑 Core Capabilities

### 1. **Data Decryption (Stage 300.1)**
- Double-layer decryption for FBE-encrypted UserData
- Adoptable Storage (SD card) volume access
- MTK Dimensity 9000+ chipset support
- TEE Key Extraction from RPMB/Secure Contexts
- AES-256 hardware acceleration

### 2. **Physical Integrity Monitoring (Stage 600.1)**
- **Eye-Diagram Analysis**: Real-time USB signal integrity monitoring
- **Tamper Detection**: Hardware interposer detection
- **Signal Impedance Guard**: Auto-disconnect on anomalies
- **EMI Shielding**: Electromagnetic interference protection

### 3. **Multi-Protocol Support**
- Qualcomm Sahara Protocol (EDL mode)
- MTK Brom Handshake Protocol
- Samsung Odin Protocol
- UniSoc FDL Protocol
- ADB Protocol (RSA-4096 encrypted)

---

## 📦 Technology Stack

### Frontend (Tauri/Web UI)
- **Framework**: React 18.3.1 + TypeScript 5.4.5
- **Build Tool**: Vite 5.2.11
- **Styling**: Tailwind CSS 4.2.2 + PostCSS
- **UI Components**: Jetpack Compose (Liquid Glass Design)
- **Desktop**: Tauri 2.x with Shell/Dialog/FS plugins
- **Animations**: Framer Motion 12.38.0
- **Icons**: Lucide React, dicons

### Backend (Android/Kotlin)
- **Language**: Kotlin 2.0.21
- **Build System**: Gradle 8.6.0
- **Android Version**: Targets latest with backward compatibility
- **DI Framework**: Hilt (2.51.1)
- **Python Interop**: Chaquo Python (gradle:15.0.1)
- **Code Generation**: KSP 2.0.21
- **Compose Compiler**: 2.0.21

### Native Core (C++17)
- **Language**: C++17
- **USB Library**: libusb 1.0.26
- **Protocols**: Custom protocol implementations
- **Crypto**: AES-256, RSA-4096
- **Signal Processing**: Real-time analysis engine

### Development & Testing
- **Testing Framework**: Jest 29.7.0
- **Type Checking**: TypeScript with strict mode
- **Testing**: ts-jest 29.1.2

---

## 🗂️ Module Architecture

### Frontend Modules (React/TypeScript)
Located in `/src/modules/`:

1. **TicketEngine** - Device ticket/license management
2. **ActivationLock** - Apple Activation Lock handling
3. **AppleIdRemoval** - Apple ID credential removal
4. **DfuRestore** - DFU mode restoration protocol
5. **AdbTerminal** - ADB shell interaction interface
6. **BypassEngine** - Core bypass orchestration
7. **BypassAdvanced** - Advanced bypass techniques
8. **MdmAnalysis** - MDM policy analysis
9. **ScreenTimeCrack** - Screen Time passcode handling
10. **RamdiskMaster** - Ramdisk injection & control
11. **ExploitOrchestrator** - Exploit chain orchestration
12. **IdentityForensics** - Device identity forensics
13. **IOSBackup** - iOS backup handling
14. **DeepExtraction** - Deep data extraction
15. **DeepVaultExport** - Secure vault export
16. **IOSAnalysis** - iOS-specific analysis

### Backend Modules (Kotlin)
Located in `/app/src/main/kotlin/com/deepeye/`:

- Protocol handlers for each SoC type
- Device database and detection
- USB transport management
- Encryption/decryption engines
- Signal processing pipeline

---

## 🔄 Data Flow Architecture

```
Device Detection
    ↓
Protocol Selection (Sahara/Brom/Odin/FDL)
    ↓
USB Handshake & Authentication
    ↓
Signal Integrity Validation
    ↓
Device State Query
    ↓
Exploit Injection / Bypass Execution
    ↓
Data Extraction & Decryption
    ↓
Integrity Verification (Eye-Diagram)
    ↓
Export & Report Generation
```

---

## 📁 Directory Structure

```
DeepEyeUnlocker/
├── src/                           # React Frontend
│   ├── components/               # Reusable UI components
│   ├── hooks/                    # Custom React hooks
│   ├── modules/                  # Feature modules (16 major)
│   ├── pages/                    # Page-level components
│   ├── lib/                      # Utility libraries
│   ├── styles/                   # Global CSS
│   └── App.tsx                   # Root component
├── app/                          # Android Application
│   ├── build.gradle.kts         # Gradle config
│   └── src/main/kotlin/         # Kotlin source code
├── src-tauri/                    # Tauri desktop integration
├── scripts/                      # Build & deployment scripts
├── build.gradle.kts             # Root Gradle config
├── package.json                 # NPM dependencies
├── tsconfig.json                # TypeScript config
├── vite.config.ts               # Vite config
├── tailwind.config.js           # Tailwind CSS config
└── jest.config.js               # Jest testing config
```

---

## 🚀 Key Entry Points

### Frontend
- **Web/Electron Entry**: `/src/main.tsx`
- **Root Component**: `/src/App.tsx`
- **Build Command**: `npm run build`
- **Dev Server**: `npm run dev`

### Android
- **Build Command**: `./gradlew assembleRelease`
- **CI Build**: `./gradlew --no-daemon assembleRelease`
- **Main Activity**: `com.deepeye.MainActivity`

### Desktop (Tauri)
- **Dev**: `npm run tauri:dev`
- **Build**: `npm run tauri:build`
- **macOS Installers**: `npm run tauri:build:macos-installers`

---

## 🔐 Security Architecture

- **RSA-4096**: ADB communication hardening
- **AES-256**: Hardware-accelerated decryption
- **SHA256**: Cryptographic verification standard
- **Tamper Detection**: Signal integrity monitoring
- **Secure Context**: TEE key extraction
- **RPMB Storage**: Trusted key management

---

## 🎓 Setup Instructions for Future AI Sessions

### Prerequisites
1. Node.js 18+ (for React frontend)
2. Kotlin/Java 17+ (for Android build)
3. Rust (for Tauri desktop)
4. Xcode Command Line Tools (macOS)
5. Android SDK + NDK

### Installation
```bash
cd /Users/enayat/Documents/DeepEyeUnlocker
npm install                    # Install frontend dependencies
./gradlew build               # Build Android components
npm run build                 # Build production frontend
```

### Development
```bash
npm run dev                   # Frontend dev server (Vite)
npm run tauri:dev           # Desktop app dev mode
./gradlew assembleDebug     # Android debug build
```

### Testing
```bash
npm run test                 # Run Jest tests
```

---

## 📝 Important Notes for AI Sessions

1. **Multi-Platform Codebase**: Frontend (React/TS) + Backend (Kotlin) + Native (C++)
2. **USB Protocol Focus**: Core strength is multi-SoC USB protocol support
3. **Security Critical**: All changes must preserve cryptographic verification
4. **Real-Time Processing**: Signal integrity requires low-latency operations
5. **Device Database**: Maintains comprehensive device model database
6. **Bypass Techniques**: Exploit chains orchestrated through ExploitOrchestrator module

---

## 🔍 Recent Development Stages

- **Stage 600.1**: Physical Integrity Shielding (Eye-Diagram Analysis)
- **Stage 300.1**: Double-Layer Decryption Implementation
- **Stage 2**: BROM Session Persistence
- **Stage 3**: DA Protocol Handler Implementation

---

## 📞 Context Handle for Future Sessions

**To resume this analysis in a new AI session, provide this path:**
```
/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/
```

**The following files in this directory contain the complete codebase memory:**
- `PROJECT_SUMMARY.md` - This file
- `ARCHITECTURE_MAP.md` - System architecture details
- `MODULE_GRAPH.md` - Module dependencies and relationships
- `TECH_STACK.md` - Technology versions and configurations
- `QUICK_START.md` - Quick reference for development

---

**End of Project Summary**
