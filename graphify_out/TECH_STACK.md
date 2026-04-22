# DeepEye Unlocker - Technology Stack Reference

**Updated:** April 21, 2026  
**Profile:** Production-grade mobile forensics engine

---

## 🏗️ Frontend Stack (React/TypeScript)

### Core Framework
```json
{
  "react": "^18.3.1",
  "react-dom": "^18.3.1",
  "typescript": "^5.4.5"
}
```
- **React Version**: 18.3.1 (latest with hooks, concurrent features)
- **TypeScript**: 5.4.5 (strict mode, latest language features)
- **JSX Runtime**: Automatic (Vite + React plugin)

### Build & Development Tools
```json
{
  "vite": "^5.2.11",
  "@vitejs/plugin-react": "^4.3.0",
  "vite-bundle-visualizer": "^1.2.1"
}
```
- **Build Tool**: Vite 5.2.11 (sub-second HMR)
- **Bundle Analysis**: vite-bundle-visualizer (1.2.1)
- **Dev Server**: Native ES modules, instant reload

### Styling & CSS
```json
{
  "tailwindcss": "^4.2.2",
  "@tailwindcss/postcss": "^4.2.2",
  "postcss": "^8.5.8",
  "autoprefixer": "^10.4.27",
  "tailwind-merge": "^3.5.0",
  "clsx": "^2.1.1"
}
```
- **CSS Framework**: Tailwind CSS 4.2.2 (utility-first)
- **PostCSS**: 8.5.8 (plugin system for CSS transformation)
- **Utilities**: tailwind-merge, clsx for conditional class names
- **Design System**: Radix UI + Tailwind for accessible components

### UI Components & Animation
```json
{
  "lucide-react": "^0.468.0",
  "dicons": "^1.1.7",
  "@radix-ui/react-slot": "^1.2.4",
  "class-variance-authority": "^0.7.1",
  "framer-motion": "^12.38.0",
  "@paper-design/shaders": "^0.0.72",
  "react-fast-marquee": "^1.6.5"
}
```
- **Icons**: Lucide React (468+ icons), dicons
- **Animation**: Framer Motion 12.38.0 (advanced motion library)
- **Component Patterns**: class-variance-authority (CVA) for variant patterns
- **Special Effects**: Paper Design shaders, liquid glass effects
- **Text Animation**: react-fast-marquee for scrolling effects

### Testing Framework
```json
{
  "jest": "^29.7.0",
  "@types/jest": "^29.5.12",
  "ts-jest": "^29.1.2"
}
```
- **Test Runner**: Jest 29.7.0
- **TypeScript Support**: ts-jest 29.1.2
- **Config**: jest.config.js in project root

### Type Definitions
```json
{
  "@types/react": "^18.3.3",
  "@types/react-dom": "^18.3.0"
}
```

---

## 🖥️ Desktop Stack (Tauri)

### Tauri Framework
```json
{
  "@tauri-apps/api": "^2.10.1",
  "@tauri-apps/cli": "^2.0.0",
  "@tauri-apps/plugin-dialog": "2.7.0",
  "@tauri-apps/plugin-fs": "2.5.0",
  "@tauri-apps/plugin-os": "^2.0.0",
  "@tauri-apps/plugin-shell": "^2.3.5"
}
```
- **Framework**: Tauri 2.x (lightweight desktop apps)
- **Dialog Plugin**: Native file/folder dialogs
- **Filesystem Plugin**: Secure file system access
- **OS Plugin**: System information queries
- **Shell Plugin**: Execute system commands safely

### Build Commands
```bash
npm run tauri:dev          # Development mode
npm run tauri:build        # Production build
npm run tauri:build:macos-installers  # macOS .pkg installers
```

### Architecture
- **Backend**: Rust (Tauri core)
- **Frontend**: React/TypeScript (bundled)
- **Security**: Isolated context, secure IPC

---

## 📱 Android/Kotlin Stack

### Gradle & Build System
```kotlin
// Root build.gradle.kts
dependencies {
  classpath("com.android.tools.build:gradle:8.6.0")
  classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
  classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.21")
  classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.21")
  classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
  classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.21-1.0.26")
  classpath("com.chaquo.python:gradle:15.0.1")
}
```

### Language & Compiler
- **Kotlin**: 2.0.21 (latest with K2 compiler)
- **Compose Compiler**: 2.0.21
- **Serialization**: Kotlin Serialization for JSON/Protocol Buffers
- **Build System**: Gradle 8.6.0 with Kotlin DSL

### Dependency Injection
```
com.google.dagger:hilt-android-gradle-plugin:2.51.1
```
- **Framework**: Hilt (Android dependency injection)
- **Scopes**: Application, Activity, Fragment, ViewModel
- **Code Generation**: KSP-based (instant builds)

### Code Generation
```
com.google.devtools.ksp:ksp:2.0.21-1.0.26
```
- **KSP**: Kotlin Symbol Processing (replaces kapt)
- **Speed**: 10x faster than kapt
- **Used By**: Hilt, Room, Moshi

### Python Integration
```
com.chaquo.python:gradle:15.0.1
```
- **Purpose**: Python interop for data processing scripts
- **Runtime**: Python 3.9 embedded in APK
- **Use Cases**: Cryptographic calculations, data parsing

### Build Variants
```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (signed)
./gradlew build              # All variants
```

### Java/Android Versions
- **Target SDK**: Latest Android (API 35+)
- **Minimum SDK**: Android 8 (API 26)
- **Java Compatibility**: Java 11

---

## ⚙️ Native C++17 Stack

### Core Engine
- **Language**: C++17 (ISO/IEC 14882:2017)
- **Compiler Flags**: `-std=c++17`, optimization flags
- **Standard Library**: STL (vector, unordered_map, etc.)

### USB Protocol Library
```
libusb 1.0.26
```
- **Purpose**: Low-level USB device communication
- **Protocols**: Bulk transfer, Interrupt, Control
- **Platform Support**: macOS, Linux, Windows
- **Latency**: Sub-millisecond USB operations

### Cryptographic Libraries
- **Algorithm**: AES-256 (encryption/decryption)
- **Key Size**: 256-bit keys
- **Modes**: CBC, CTR (depending on protocol)
- **RSA**: 4096-bit key size for digital signatures
- **Hashing**: SHA-256 for integrity verification

### Protocol Implementations
1. **Qualcomm Sahara Protocol**: EDL mode communication
2. **MTK Brom Protocol**: MediaTek bootloader
3. **Samsung Odin Protocol**: Samsung device protocol
4. **UniSoc FDL Protocol**: UniSoc/Spreadtrum devices

---

## 🔧 Build Configuration Files

### TypeScript
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true
  }
}
```
- **Strict Mode**: All type checking enabled
- **Target**: ES2020 (modern JavaScript)
- **Lib**: Browser + ES2020 APIs

### Tailwind CSS
```js
// tailwind.config.js
module.exports = {
  content: [
    "./src/**/*.{js,ts,jsx,tsx}"
  ],
  theme: {
    // Custom theme configuration
  }
}
```

### Jest Testing
```js
// jest.config.js
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  roots: ['<rootDir>/src']
}
```

### Vite
```ts
// vite.config.ts
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  build: {
    target: 'ES2020'
  }
})
```

---

## 📦 Dependency Management

### Frontend Dependencies Count
- **Production**: ~15 core dependencies
- **Development**: ~8 dev dependencies
- **Lock File**: package-lock.json (npm) + pnpm-lock.yaml

### Android Dependencies Count
- **Gradle Plugins**: 8 major plugins
- **Library Dependencies**: 50+ managed by Gradle
- **Transitive**: Hundreds of transitive dependencies

### Version Strategy
- **Frontend**: Semantic versioning with caret (^) for minor updates
- **Android**: Strict version pinning for stability
- **Native**: Version-locked C++17 standard

---

## 🚀 Performance Optimizations

### Frontend
- **Code Splitting**: Vite automatic chunk splitting
- **Tree Shaking**: Unused code elimination
- **Lazy Loading**: Dynamic imports for modules
- **Asset Optimization**: Image compression, CSS minification

### Build
- **Incremental Builds**: Gradle build cache
- **Parallel Compilation**: Multi-threaded Kotlin compilation
- **Caching**: .gradle, node_modules caching

---

## 🔐 Security Technologies

### Cryptography
- **Algorithm**: AES-256, RSA-4096
- **Key Storage**: Secure enclave (TEE), RPMB
- **Protocol**: TLS 1.3 for network communication
- **Signing**: SHA-256 code signing

### Access Control
- **Permissions**: Android permission model
- **Capabilities**: SELinux policies
- **Sandbox**: Process isolation, capability-based

---

## 📊 Version Compatibility Matrix

| Component | Version | Compatibility | Status |
|-----------|---------|----------------|--------|
| React | 18.3.1 | Node 18+ | ✅ Active |
| TypeScript | 5.4.5 | Node 18+ | ✅ Active |
| Kotlin | 2.0.21 | Java 11+ | ✅ Active |
| Gradle | 8.6.0 | Java 11+ | ✅ Active |
| Android API | 35+ (min 26) | All devices | ✅ Active |
| C++ | C++17 | GCC 7+ | ✅ Active |
| Tauri | 2.x | macOS 10.13+ | ✅ Active |
| libusb | 1.0.26 | All platforms | ✅ Active |

---

## 🔄 Update Schedule

- **Frontend**: Quarterly updates for React/TypeScript
- **Android**: Bi-annual Gradle/Kotlin updates
- **Native**: Stable C++17 (no major updates expected)
- **Security**: Monthly security patch updates

---

**End of Technology Stack Reference**
