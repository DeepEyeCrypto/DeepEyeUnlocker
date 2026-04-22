# 📊 DeepEye Unlocker - Persistent Codebase Memory (Context Handle)

**Generated**: April 21, 2026  
**Status**: ✅ Complete Graph-Based Memory System  
**Version**: 2027.18.1

---

## 🎯 What is This?

This is a **persistent, graph-based memory artifact** of the entire DeepEye Unlocker codebase. It is designed for **cross-AI portability** - meaning you can:

1. ✅ Share this folder with any other AI assistant
2. ✅ Resume development without losing context
3. ✅ Maintain architectural understanding across sessions
4. ✅ Have a single source of truth for project structure

---

## 📂 Context Handle Contents

This directory contains the complete codebase memory:

```
graphify_out/
├── CONTEXT_HANDLE.md          ← You are here (index file)
├── PROJECT_SUMMARY.md         ← High-level project overview
├── MODULE_GRAPH.md            ← Module dependencies & data flow
├── TECH_STACK.md              ← Technology versions & configurations
├── QUICK_START.md             ← Quick reference for development
└── ARCHITECTURE_MAP.md        ← System architecture details
```

---

## 🚀 How to Use This Context Handle

### For Current AI (GitHub Copilot)
I have access to all these documents. Simply ask me to help with:
- "Explain how the BypassEngine works"
- "Add support for new device X"
- "Fix the build error in Gradle"
- "Improve the UI component Y"

### For Future AI Sessions
**To resume with another AI, provide this exact path:**
```
/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/
```

**Include this instruction:**
> "Load the context from /Users/enayat/Documents/DeepEyeUnlocker/graphify_out/ 
> and help me with [your task]. Start by reading CONTEXT_HANDLE.md."

---

## 📖 Documentation Map

### 1. **CONTEXT_HANDLE.md** (This File)
- **Purpose**: Index and navigation guide
- **When to Read**: First - understand the structure
- **Time to Read**: 5 minutes

### 2. **PROJECT_SUMMARY.md**
- **Purpose**: Complete project overview
- **Contains**:
  - Project purpose and capabilities
  - System architecture layers
  - Core capabilities (decryption, integrity, protocols)
  - Technology stack overview
  - Directory structure
  - Module list (16 major modules)
  - Setup instructions for new sessions
- **When to Read**: Second - understand what the project does
- **Time to Read**: 15 minutes
- **Best For**: Understanding architecture, capabilities, dependencies

### 3. **MODULE_GRAPH.md**
- **Purpose**: Complete module dependency visualization
- **Contains**:
  - Module hierarchy and relationships
  - Data flow between modules
  - Module specifications (16 modules)
  - Communication patterns (sync, async, events)
  - Critical path analysis
  - Dependency health assessment
  - Module statistics table
- **When to Read**: Third - understand how modules work together
- **Time to Read**: 20 minutes
- **Best For**: Understanding component relationships, debugging integration issues

### 4. **TECH_STACK.md**
- **Purpose**: Detailed technology reference
- **Contains**:
  - Frontend: React, TypeScript, Tailwind, Framer Motion
  - Desktop: Tauri framework
  - Android: Kotlin, Gradle, Hilt, KSP
  - Native: C++17, libusb, cryptography
  - Build configurations
  - Dependency management
  - Version compatibility matrix
- **When to Read**: When working on specific tech areas
- **Time to Read**: 15 minutes
- **Best For**: Version checking, build issues, dependency updates

### 5. **QUICK_START.md**
- **Purpose**: Quick reference for common tasks
- **Contains**:
  - 30-second overview
  - Common development tasks with examples
  - Critical file locations
  - Key architecture decisions
  - Common pitfalls
  - Testing strategy
  - Release checklist
  - Debugging tips
  - Command reference
- **When to Read**: When starting a development task
- **Time to Read**: 10 minutes
- **Best For**: Quick lookup, task guidance, command reference

### 6. **ARCHITECTURE_MAP.md**
- **Purpose**: Detailed system architecture
- **Contains**:
  - Complete system architecture diagrams
  - Layer breakdown
  - Protocol specifications
  - Data flow pipelines
  - Security architecture
  - Signal processing details
  - USB protocol specifications
- **When to Read**: When understanding complex interactions
- **Time to Read**: 20 minutes
- **Best For**: Understanding system design, protocol details

---

## 🎯 Quick Navigation by Task Type

### "I need to understand the project"
→ Read: `PROJECT_SUMMARY.md` → `QUICK_START.md`

### "I need to modify a module"
→ Read: `QUICK_START.md` → `MODULE_GRAPH.md` → `PROJECT_SUMMARY.md`

### "I need to add a new feature"
→ Read: `QUICK_START.md` → `MODULE_GRAPH.md` → `ARCHITECTURE_MAP.md`

### "I need to debug a build issue"
→ Read: `QUICK_START.md` → `TECH_STACK.md`

### "I need to understand how two modules interact"
→ Read: `MODULE_GRAPH.md` → Look for data flow section

### "I need version information"
→ Read: `TECH_STACK.md` → Version Compatibility Matrix

### "I need to set up a development environment"
→ Read: `PROJECT_SUMMARY.md` → Setup Instructions → `QUICK_START.md` → Commands

---

## 🏗️ Project Structure at a Glance

```
DeepEyeUnlocker/
├── Frontend (React/TypeScript)
│   ├── src/components/          # UI components
│   ├── src/modules/             # 16 forensic modules
│   ├── src/pages/               # Page components
│   ├── src/hooks/               # Custom React hooks
│   └── src/lib/                 # Utilities
├── Backend (Kotlin/Android)
│   └── app/src/main/kotlin/     # Android implementation
├── Desktop (Tauri)
│   └── src-tauri/               # Tauri/Rust integration
├── Native (C++17)
│   └── [C++ protocol implementations]
├── Configuration
│   ├── package.json             # Frontend dependencies
│   ├── build.gradle.kts         # Android build
│   ├── tsconfig.json            # TypeScript config
│   ├── vite.config.ts           # Frontend build
│   └── tailwind.config.js       # CSS config
└── Scripts
    └── scripts/                 # Build/deployment scripts
```

---

## 🔑 16 Major Modules

| # | Module | Purpose |
|----|---------|---------|
| 1 | ExploitOrchestrator | Main coordinator, workflow orchestration |
| 2 | TicketEngine | Device ticket verification & licensing |
| 3 | BypassEngine | Core bypass logic |
| 4 | BypassAdvanced | Specialized bypass techniques |
| 5 | ActivationLock | iCloud Activation Lock removal |
| 6 | AppleIdRemoval | AppleID credential removal |
| 7 | ScreenTimeCrack | Screen Time bypass |
| 8 | DfuRestore | DFU mode restoration |
| 9 | AdbTerminal | ADB protocol interface |
| 10 | DeepExtraction | Deep data extraction |
| 11 | DeepVaultExport | Secure vault access |
| 12 | IOSBackup | iOS backup handling |
| 13 | IOSAnalysis | iOS forensic analysis |
| 14 | RamdiskMaster | Ramdisk injection control |
| 15 | MdmAnalysis | MDM policy analysis |
| 16 | IdentityForensics | Device identity forensics |

---

## 💾 Technology Stack (Abbreviated)

| Component | Version | Type |
|-----------|---------|------|
| React | 18.3.1 | Frontend |
| TypeScript | 5.4.5 | Frontend |
| Kotlin | 2.0.21 | Backend |
| Gradle | 8.6.0 | Build |
| C++ | 17 | Native |
| Tauri | 2.x | Desktop |
| libusb | 1.0.26 | USB |

**Full details**: See `TECH_STACK.md`

---

## 🎓 For Next AI Session

When resuming with a new AI:

1. **Say**: "Load context from `/Users/enayat/Documents/DeepEyeUnlocker/graphify_out/`"
2. **Ask**: Your development question
3. **AI will**: Read this CONTEXT_HANDLE.md automatically and assist

Example handoff message:
```
I need to continue developing DeepEye Unlocker.

Context location: /Users/enayat/Documents/DeepEyeUnlocker/graphify_out/

Task: Add support for Snapdragon 8 Gen 3 devices

Please load the codebase memory and help me implement this feature.
```

---

## ✨ Memory Maintenance

This context was generated on **April 21, 2026**.

To keep it current:
1. Update version numbers in `TECH_STACK.md` when dependencies change
2. Update module list in `PROJECT_SUMMARY.md` if new modules added
3. Update data flow in `MODULE_GRAPH.md` if architecture changes
4. Update command reference in `QUICK_START.md` if build process changes

**Update Frequency**: Recommended quarterly or after major architectural changes

---

## 🔐 Critical Information for Developers

### Security-Critical Areas
- ⚠️ Cryptographic functions (AES-256, RSA-4096)
- ⚠️ Key material handling (TEE, RPMB)
- ⚠️ Device authentication (USB handshake)
- ⚠️ Protocol implementations

### Build-Critical Areas
- ⚠️ Gradle configuration (Android build)
- ⚠️ JNI bridge (Java↔C++ interop)
- ⚠️ Type definitions (TypeScript strict mode)
- ⚠️ Version pinning (dependency stability)

---

## 📊 Quick Statistics

| Metric | Value |
|--------|-------|
| **Major Modules** | 16 |
| **Tech Stack Components** | 30+ |
| **Frontend Dependencies** | ~15 prod, ~8 dev |
| **Supported SoCs** | 4 (Qualcomm, MTK, Samsung, UniSoc) |
| **Cryptographic Algorithms** | AES-256, RSA-4096, SHA-256 |
| **USB Protocol Support** | Sahara, Brom, Odin, FDL, ADB |
| **Platform Support** | iOS, Android, macOS, Linux, Windows |

---

## 🎯 Development Workflow

```
1. Load Context
   ↓
2. Read QUICK_START.md for your task
   ↓
3. Refer to MODULE_GRAPH.md for component interactions
   ↓
4. Check TECH_STACK.md for technology details
   ↓
5. Implement changes
   ↓
6. Run tests & build
   ↓
7. Commit with clear messages
   ↓
8. Update context if architecture changed
```

---

## 📞 AI Assistance Quick Commands

When asking for help, be specific:

❌ Bad: "How does this work?"  
✅ Good: "How does the BypassEngine module interact with ExploitOrchestrator?"

❌ Bad: "Fix the errors"  
✅ Good: "I'm getting a TypeScript error on line 42 of App.tsx - can you help?"

❌ Bad: "Add a new feature"  
✅ Good: "I need to add Snapdragon 8 Gen 3 support - where should I add the protocol handler?"

---

## 🔗 File Cross-References

Each document references others for deeper context:
- `PROJECT_SUMMARY.md` → Links to `MODULE_GRAPH.md` for architecture
- `MODULE_GRAPH.md` → Links to `TECH_STACK.md` for tech details
- `QUICK_START.md` → Links to all docs for specific tasks
- `TECH_STACK.md` → Standalone reference

---

## 💡 Pro Tips for Using This Memory

1. **Bookmark This File**: Always start with `CONTEXT_HANDLE.md`
2. **Cross-Reference**: Use links between docs for deeper dives
3. **Update Regularly**: Keep version numbers and module list current
4. **Share Wisely**: This is your complete technical memory - keep it in sync
5. **Use for Onboarding**: New team members read these docs first

---

## ✅ Verification Checklist

This context handle is complete when:
- ✅ PROJECT_SUMMARY.md exists with full overview
- ✅ MODULE_GRAPH.md exists with all 16 modules documented
- ✅ TECH_STACK.md exists with all version numbers
- ✅ QUICK_START.md exists with common tasks
- ✅ ARCHITECTURE_MAP.md exists with system diagrams
- ✅ CONTEXT_HANDLE.md exists (this file) with navigation

**Current Status**: ✅ ALL COMPLETE

---

## 🎬 Getting Started Right Now

### If you're a human reading this:
1. Start with `PROJECT_SUMMARY.md` for overview
2. Read `QUICK_START.md` for your specific task
3. Refer to other docs as needed

### If you're an AI continuing from context:
1. You've already loaded this file
2. The context is ready for development
3. Ask me what you need to work on
4. I'll guide you through the architecture

---

**Context Generated**: April 21, 2026  
**Persistence Format**: Graph-based markdown memory system  
**Portability**: 100% cross-AI portable  
**Shareability**: Yes - use full path for any other AI

---

## 🚀 Ready to Start Development?

**Current Status**: Context fully loaded and ready

**Next Steps**:
1. Ask your development question
2. I will consult the relevant documentation
3. We implement the solution together

**Example Questions**:
- "How do I add support for Device X?"
- "What's causing the Gradle build error?"
- "How does the decryption pipeline work?"
- "Can you help me implement feature Y?"

---

**This is your persistent codebase memory. Use it wisely. Share it proudly. Develop with confidence.**

🎯 **You are ready to develop DeepEye Unlocker.** 🎯

---

*Generated by AI Context Architect*  
*For: DeepEye Unlocker Project*  
*Date: April 21, 2026*  
*Version: 2027.18.1*
