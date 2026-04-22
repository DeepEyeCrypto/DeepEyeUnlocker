# DeepEye Unlocker - Quick Start Guide for AI Development Sessions

**Context Handle**: `/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/`

---

## ⚡ 30-Second Overview

**DeepEye Unlocker** is a professional mobile forensics platform enabling:
- Locked device access via multi-protocol USB orchestration
- Double-layer AES-256 data decryption
- Signal integrity monitoring (Eye-Diagram analysis)
- Multi-SoC support (Qualcomm, MTK, Samsung, UniSoc)
- iOS + Android + desktop (Tauri) support

**Tech Stack**: React/TypeScript (frontend) | Kotlin (backend) | C++17 (native core)

---

## 🎯 Common Development Tasks

### Task: Add New Device Support
1. **Identify Chipset**: Qualcomm/MTK/Samsung/UniSoc
2. **Module**: Add to `/src/modules/ExploitOrchestrator`
3. **Protocol**: Implement in C++ native layer
4. **Database**: Update device database with signatures
5. **Test**: Run against target device

**Files to Edit**: 
- `src/modules/ExploitOrchestrator/index.tsx`
- `app/src/main/kotlin/com/deepeye/protocols/`
- Device database JSON

### Task: Add New Bypass Technique
1. **Feature Module**: Create in `/src/modules/`
2. **Orchestration**: Wire into `BypassEngine`
3. **Testing**: Add test cases in `/tests/`
4. **Documentation**: Update technique in `ExploitOrchestrator`

**Files to Edit**:
- `src/modules/BypassEngine/index.tsx`
- `src/modules/[NewModule]/index.tsx`
- Test files

### Task: Modify Decryption Engine
1. **Caution**: Security-critical code
2. **Location**: Native C++17 core or Kotlin backend
3. **Algorithm**: AES-256 implementation
4. **Review**: Cryptographic changes need security audit

**Files to Edit**:
- `app/src/main/kotlin/com/deepeye/crypto/`
- Native C++ decryption layer

### Task: Improve UI/UX
1. **Component**: React/TypeScript in `/src/components/`
2. **Styling**: Tailwind CSS utilities
3. **Animation**: Framer Motion for interactions
4. **Icons**: Lucide React or dicons

**Files to Edit**:
- `src/components/`
- `src/pages/`
- Tailwind config if new theme needed

### Task: Fix Build Issues
1. **Frontend**: `npm run build` → check Vite output
2. **Android**: `./gradlew build` → check Gradle logs
3. **Desktop**: `npm run tauri:build` → check Rust compilation
4. **Linting**: TypeScript strict mode enforcement

**Commands**:
```bash
npm run build              # Frontend build
./gradlew build           # Android build
npm run tauri:build       # Desktop build
npm run test              # Run Jest tests
```

---

## 📁 Critical File Locations

| What | Where | Priority |
|------|-------|----------|
| Entry Point (Frontend) | `src/App.tsx` | High |
| Main Orchestrator | `src/modules/ExploitOrchestrator/` | High |
| React Components | `src/components/` | Medium |
| Styling Config | `tailwind.config.js` | Medium |
| Type Definitions | `tsconfig.json` | High |
| Android Build | `build.gradle.kts` | High |
| Tauri Config | `src-tauri/tauri.conf.json` | Medium |
| Device Database | `DeepEyeDeviceDB/` | High |
| Build Scripts | `scripts/` | Medium |

---

## 🔑 Key Architecture Decisions

1. **Multi-Platform Approach**: React (desktop) + Kotlin (Android app) + Tauri (electron alternative)
2. **Module-Based**: 16 specialized forensic modules, not monolithic
3. **USB-First**: All device communication via libusb protocols
4. **Cryptographic-First**: AES-256 + RSA-4096 throughout
5. **Real-Time Processing**: Signal integrity monitoring during extraction
6. **Tamper Detection**: Eye-Diagram analysis prevents corruption

---

## ⚠️ Common Pitfalls

1. **Modifying Cryptographic Code**: Always audit thoroughly
2. **USB Protocol Changes**: Test against all supported SoCs
3. **Type Safety**: TypeScript strict mode is enforced
4. **Dependency Updates**: Check breaking changes in Kotlin/Gradle
5. **Signal Processing**: Real-time constraints, avoid blocking operations

---

## 🧪 Testing Strategy

### Unit Tests
```bash
npm run test  # Jest for React components
```

### Integration Tests
Run against real device or emulator:
```bash
./gradlew assembleDebug
adb install-multiple build/outputs/apk/*/debug/*.apk
```

### E2E Tests
Full workflow testing via Tauri app

---

## 🚀 Release Checklist

Before releasing new version:
- [ ] All TypeScript types checked (strict mode)
- [ ] Build passes: `npm run build` + `./gradlew build`
- [ ] Tests pass: `npm run test`
- [ ] No console errors in dev tools
- [ ] Security review for crypto changes
- [ ] Device database updated
- [ ] Release notes prepared
- [ ] Version bumped in package.json

---

## 🔐 Security Notes

- **Cryptographic Changes**: 🚨 HIGH SECURITY RISK - requires external audit
- **Device Database**: Maintain integrity of device signatures
- **Key Material**: Never log or expose private keys
- **Signal Processing**: Real-time monitoring prevents tampering
- **USB Communication**: ADB uses RSA-4096 encryption

---

## 📖 Documentation References

| Document | Purpose | Link |
|----------|---------|------|
| Project Summary | High-level overview | `PROJECT_SUMMARY.md` |
| Module Graph | Dependency visualization | `MODULE_GRAPH.md` |
| Tech Stack | Version/technology details | `TECH_STACK.md` |
| Architecture | System design | `ARCHITECTURE_MAP.md` |
| This File | Quick reference | `QUICK_START.md` |

---

## 🎓 Onboarding for New AI Sessions

When resuming development:

1. **Load Context**: Point me to `/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/`
2. **Ask Question**: "What part of DeepEye do you need to work on?"
3. **I will**: Read relevant documentation and assist with:
   - Code modifications
   - Architecture questions
   - Bug fixes
   - Feature additions
   - Build issues
   - Testing strategies

---

## 📞 Quick Command Reference

```bash
# Development
npm install                    # Install dependencies
npm run dev                   # Start frontend dev server
npm run tauri:dev            # Start desktop app in dev mode
./gradlew assembleDebug      # Build Android debug APK

# Production
npm run build                # Build frontend
./gradlew assembleRelease    # Build Android release
npm run tauri:build          # Build desktop app

# Utilities
npm run test                 # Run Jest tests
npm run bundle:analyze       # Analyze bundle size
./gradlew clean              # Clean build artifacts

# Specific to macOS
npm run tauri:build:macos-installers  # Build .pkg installer
```

---

## 🔍 Debugging Tips

### Frontend Issues
1. Check browser console for React errors
2. Use React DevTools extension
3. TypeScript strict mode: all errors must be fixed

### Android Issues
1. Check Gradle build output
2. Run `./gradlew --info assembleDebug` for verbose logs
3. Check logcat: `adb logcat`

### Native Issues
1. Check C++ compilation errors
2. Use lldb debugger (macOS)
3. Check signal processing logs

---

## 🎯 Priority Areas for Development

1. **Signal Integrity**: Eye-Diagram analysis (Stage 600.1)
2. **Protocol Support**: New SoC chipsets
3. **Decryption**: AES-256 optimization
4. **UI/UX**: Liquid Glass design improvements
5. **Performance**: Sub-millisecond USB operations

---

**Last Updated**: April 21, 2026  
**For Latest Context**: Refer to `/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/`
