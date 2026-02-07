# Building DeepEyeUnlocker APK and ZIP - Guide

**Generated**: February 5, 2026  
**Version**: v4.0.0

---

## ✅ Windows Portable ZIP - COMPLETE

The Windows portable release package has been successfully created locally!

### Package Details

- **File**: `artifacts/DeepEyeUnlocker-v4.0.0-Portable.zip`
- **Size**: 8.7 MB (compressed from ~9.6 MB)
- **SHA256**: `022cdc6406285d2931b9c611289cfbd5d793fd4e73353cd1d746b512e7a282b7`

### Contents

- ✅ DeepEyeUnlocker.exe (main executable)
- ✅ Required DLLs (WPF runtime, SQLite, DirectX)
- ✅ USB Drivers (Qualcomm, MediaTek - x86 & x64)
- ✅ Documentation (README, LICENSE, User Manual, Legal, Troubleshooting, Supported Devices, Build Guide)
- ✅ SHA256 checksum for verification

### Testing

To test on Windows:

```powershell
# Extract the ZIP
Expand-Archive -Path DeepEyeUnlocker-v4.0.0-Portable.zip -DestinationPath C:\DeepEyeUnlocker

# Verify checksum
Get-FileHash C:\DeepEyeUnlocker\release\DeepEyeUnlocker.exe -Algorithm SHA256

# Run as Administrator
cd C:\DeepEyeUnlocker\release
.\DeepEyeUnlocker.exe
```

---

## 📱 Android APK Build - Guide

The Android APK requires a Linux environment or GitHub Actions to build. Here are your options:

### Option 1: GitHub Actions (Recommended)

The Android APK can be built automatically using GitHub Actions:

1. **Trigger the Workflow**:

   ```bash
   # The workflow runs on tag push
   git tag v4.0.0-android
   git push origin v4.0.0-android
   ```

2. **Download Artifacts**:
   - Go to: <https://github.com/DeepEyeCrypto/DeepEyeUnlocker/actions>
   - Find the "DeepEye Portable - Cross-Platform Release" workflow
   - Download the `deepeye-portable-apk` artifact

3. **Known Issue**:
   - ⚠️ The Android build is currently failing in CI due to CMake path resolution
   - This needs to be fixed in v4.1.0
   - Issue tracked in POST_RELEASE_SUMMARY_v4.0.0.md

### Option 2: Manual Build on Linux

If you have access to a Linux machine with Android NDK:

1. **Install Prerequisites**:

   ```bash
   # Install Android SDK and NDK
   sudo apt-get update
   sudo apt-get install -y openjdk-17-jdk gradle cmake build-essential
   
   # Download Android NDK
   wget https://dl.google.com/android/repository/android-ndk-r25c-linux.zip
   unzip android-ndk-r25c-linux.zip -d ~/android-ndk
   export ANDROID_NDK_HOME=~/android-ndk/android-ndk-r25c
   ```

2. **Build Native Core**:

   ```bash
   cd portable/core
   mkdir -p build && cd build
   cmake .. -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
            -DANDROID_ABI=arm64-v8a \
            -DANDROID_PLATFORM=android-26 \
            -DCMAKE_BUILD_TYPE=Release
   make deepeye_core
   cd ../../..
   ```

3. **Build Android APK**:

   ```bash
   cd portable/android
   gradle assembleRelease
   cd ../..
   ```

4. **Locate APK**:

   ```bash
   ls -lh portable/android/app/build/outputs/apk/release/*.apk
   ```

### Option 3: Docker Container (Cross-Platform)

Build the Android APK using Docker on any platform (macOS, Windows, Linux):

```bash
# Create a Dockerfile for Android builds
docker run --rm -v $(pwd):/workspace -w /workspace \
  mingc/android-build-box:latest \
  bash -c "cd /workspace && ./scripts/build-android-docker.sh"
```

---

## 🔧 Fixing the Android JNI Build Issue

The current Android build failure is due to CMake not finding the core source files. Here's the fix needed for v4.1.0:

### Problem

```cmake
# In portable/core/CMakeLists.txt
# Current (broken):
set(CORE_DIR "${CMAKE_SOURCE_DIR}")

# Issue: CMAKE_SOURCE_DIR points to wrong directory in CI
```

### Solution

```cmake
# Fixed version:
get_filename_component(CORE_DIR "${CMAKE_CURRENT_SOURCE_DIR}" ABSOLUTE)
set(CORE_SRC_DIR "${CORE_DIR}/src")

# Verify path exists
if(NOT EXISTS "${CORE_SRC_DIR}")
    message(FATAL_ERROR "Core source directory not found: ${CORE_SRC_DIR}")
endif()
```

This fix is already identified and will be implemented in v4.1.0.

---

## 📦 Upload to GitHub Release

Once you have both artifacts:

### Method 1: GitHub Web Interface

1. Navigate to: <https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/tag/v4.0.0>
2. Click "Edit release"
3. Drag and drop files:
   - `DeepEyeUnlocker-v4.0.0-Portable.zip` (local build)
   - `DeepEyeUnlocker-v4.0.0-Portable.zip.sha256`
   - `deepeye-portable-v4.0.0.apk` (from GitHub Actions or Linux build)
4. Click "Update release"

### Method 2: GitHub CLI

```bash
# Install GitHub CLI if needed
brew install gh

# Authenticate
gh auth login

# Upload assets to existing release
gh release upload v4.0.0 \
  artifacts/DeepEyeUnlocker-v4.0.0-Portable.zip \
  artifacts/DeepEyeUnlocker-v4.0.0-Portable.zip.sha256 \
  --clobber
```

---

## ✅ Verification Checklist

Before uploading to GitHub Release:

### Windows Portable ZIP

- [x] File size reasonable (~8-10 MB)
- [x] SHA256 checksum generated
- [x] Contains DeepEyeUnlocker.exe
- [x] Contains all required DLLs
- [x] Contains USB drivers
- [x] Contains documentation
- [ ] Tested on Windows 10/11

### Android APK (when built)

- [ ] File size reasonable (~5-15 MB)
- [ ] APK is signed (release build)
- [ ] Contains native libraries (arm64-v8a)
- [ ] Tested on Android device with OTG support

---

## 🎯 Current Status

**Windows Portable**: ✅ Built and ready to upload  
**Android APK**: ⚠️ Requires Linux/CI build (issue tracked for v4.1.0)

### Next Actions

1. **Immediate** (Optional):
   - Upload Windows portable ZIP to GitHub release
   - Update release notes with local build details

2. **v4.1.0 Planning**:
   - Fix Android JNI CMake path issue
   - Enable successful Android builds in CI
   - Create unified release with both Windows and Android artifacts

---

## 📚 Reference Files

- **Build Script**: `scripts/build-release.sh` (macOS/Linux)
- **PowerShell Build**: `scripts/build.ps1` (Windows)
- **CI Workflow**: `.github/workflows/portable_release.yml`
- **Release Notes**: `docs/RELEASE_NOTES_v4.0.0.md`
- **Roadmap**: `POST_RELEASE_SUMMARY_v4.0.0.md`

---

**Generated by DeepEyeUnlocker Build System v4.0.0**
