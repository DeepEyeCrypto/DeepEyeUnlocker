---
description: Debug and fix DeepEye Unlocker build/runtime errors using structured classification and fix patterns
---

# DeepEye Unlocker — Debug & Fix Workflow

## Error Classification

When a log is pasted, classify FIRST:

| Class | Signals | Fix Area |
|-------|---------|----------|
| **A** C++ Compile | `fatal error:`, `error:`, clang in path | Header path, missing define, syntax |
| **B** C++ Linker | `ld: error: undefined symbol:` | Missing method body, wrong namespace, CMakeLists |
| **C** CMake Config | `CMake Error at` | Wrong path, missing target |
| **D** Kotlin Compile | `unresolved reference` | Missing import, wrong type |
| **E** Gradle Config | `Could not resolve`, repository conflict | Dependency version, plugin version |
| **F** Runtime Crash | `FATAL EXCEPTION`, `UnsatisfiedLinkError` | Stack trace → exact line |
| **G** CI Infra | `Process completed with exit code`, SDK errors | Workflow YAML, SDK license |

## Answer Format (mandatory)

```
GOAL:   1 line — what is broken and why
CLASS:  A/B/C/D/E/F/G
FILE:   exact path + line number
ROOT:   ≤2 lines — actual cause
FIX:    complete, runnable code (CURRENT → REPLACE WITH)
VERIFY: exact command to confirm
COMMIT: git add + git commit -m message
```

## Known Fixed Errors (do NOT re-fix)

- ✅ `config.h` missing → created at `core/libusb-source/libusb/config.h`
- ✅ `hotplug.c` + `linux_netlink.c` → excluded from CMake `usb_static`
- ✅ `EdlManager` undefined symbols → `edl_manager.h` redirects to `edl_proto.h`
- ✅ `allprojects{}` conflict → removed from root build.gradle.kts
- ✅ `odin_manager.cpp:392` overflow → removed unreachable `> 4GB` check
- ✅ Kotlin `clickable`/`SessionState`/`SurfaceDark` → imports fixed
- ✅ Missing XML layouts → `item_model.xml` + `item_partition.xml` created
- ✅ Gradle OOM → heap bumped to 4096m
- ✅ Java 25 incompatibility → JDK 17 via JAVA_HOME

## C++ Linker Fix Rules

1. Symbol mismatch → check namespace + class + method signature in `.h` vs `.cpp`
2. Missing file → add `.cpp` to `add_library()` in `CMakeLists.txt`
3. Multiple definition → remove impl from `.h`, keep declaration only
4. STL ABI → all `.cpp` must use same STL: `c++_shared`
5. ITransport → must be in `core/include/itransport.h`, use `"quotes"` not `<angles>`

## Kotlin Fix Rules

1. Compose import → check package per BOM version (2024.02.00)
2. Sealed class `when()` → add `else` or missing subclass branch
3. Single `companion object` per class
4. StateFlow → use `collectAsStateWithLifecycle()`
5. `System.loadLibrary("deepeye_core")` — no `lib` prefix, no `.so`

## Gradle Fix Rules

- AGP 8.2.x + Kotlin 1.9.22 + KCE 1.5.8 + BOM 2024.02.00
- Never mix Kotlin 2.x with KCE 1.5.x
- `ndkVersion = "25.1.8937393"` (not ndkPath)
- No `repositories{}` in root when `FAIL_ON_PROJECT_REPOS` is set

## Verify Commands

```bash
// turbo
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew assembleRelease 2>&1 | tail -20
```

## Commit Message Pattern

```
fix(edl):      linker symbol mismatch
fix(cmake):    wrong include path
fix(kotlin):   unresolved reference in OtgActivity
fix(gradle):   repository conflict
fix(runtime):  null transport_ SIGSEGV
fix(ci):       add --stacktrace flag
fix(libusb):   config.h missing for Android NDK
fix(jni):      method name mismatch NativeBridge
```
