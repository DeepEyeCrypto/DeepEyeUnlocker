# CI/CD Pipeline

Complete guide to the DeepEye Unlocker build and release system.

---

## Overview

DeepEye Unlocker uses GitHub Actions for continuous integration and automated releases. The pipeline supports multi-platform builds for desktop (Tauri) and mobile (Android).

## Workflow Files

| Workflow | File | Purpose |
|----------|------|---------|
| **Android Build** | `.github/workflows/build.yml` | Debug APK builds on PR/push |
| **Release** | `.github/workflows/release.yml` | Multi-platform release builds |
| **Tauri Build** | `.github/workflows/tauri.yml` | Desktop app builds |

---

## Android Build Workflow

### Trigger Conditions

```yaml
on:
  push:
    branches: [ main ]
    paths-ignore:
      - '**.md'
      - 'CHANGELOG.md'
  pull_request:
    branches: [ main ]
```

### Build Matrix

| Parameter | Value |
|-----------|-------|
| Runner | `ubuntu-latest` |
| JDK | 17 (Temurin distribution) |
| Android SDK | Latest (via setup-android) |
| Gradle | 8.12 |

### Build Steps

```yaml
jobs:
  build-debug:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
      
      - name: Build debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace
      
      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: DeepEyeUnlocker-debug
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 7
```

### Key Configuration

**gradle.properties:**
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

**app/build.gradle:**
```groovy
android {
    compileSdk 35
    
    defaultConfig {
        minSdk 26
        targetSdk 35
        versionCode 202631
        versionName "2026.31.0"
    }
    
    ndkVersion "25.1.8937393"
}
```

---

## Release Workflow

### Trigger

```yaml
on:
  push:
    tags:
      - 'v*'
```

### Release Matrix

| Platform | Target | Artifact |
|----------|--------|----------|
| macOS | `dmg` | `.dmg` Universal |
| Windows | `nsis`, `msi` | `.exe`, `.msi` |
| Linux | `appimage`, `deb` | `.AppImage`, `.deb` |
| Android | `apk` | `-universal.apk` |

### macOS Build

```yaml
build-macos:
  runs-on: macos-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Setup Node
      uses: actions/setup-node@v4
      with:
        node-version: 20
    
    - name: Setup Rust
      uses: dtolnay/rust-action@stable
    
    - name: Install dependencies
      run: |
        npm install -g pnpm
        pnpm install
    
    - name: Build Tauri
      run: pnpm tauri build --target universal-apple-darwin
    
    - name: Upload DMG
      uses: actions/upload-release-asset@v1
      with:
        asset_path: src-tauri/target/universal-apple-darwin/release/bundle/dmg/*.dmg
```

### Windows Build

```yaml
build-windows:
  runs-on: windows-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Setup Node
      uses: actions/setup-node@v4
      with:
        node-version: 20
    
    - name: Setup Rust
      uses: dtolnay/rust-action@stable
    
    - name: Install dependencies
      run: |
        npm install -g pnpm
        pnpm install
    
    - name: Build Tauri
      run: pnpm tauri build
    
    - name: Upload artifacts
      uses: actions/upload-release-asset@v1
      with:
        asset_path: |
          src-tauri/target/release/bundle/nsis/*.exe
          src-tauri/target/release/bundle/msi/*.msi
```

### Linux Build

```yaml
build-linux:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Install dependencies
      run: |
        sudo apt-get update
        sudo apt-get install -y libgtk-3-dev libwebkit2gtk-4.0-dev
    
    - name: Setup Node
      uses: actions/setup-node@v4
      with:
        node-version: 20
    
    - name: Setup Rust
      uses: dtolnay/rust-action@stable
    
    - name: Build
      run: |
        npm install -g pnpm
        pnpm install
        pnpm tauri build
    
    - name: Upload artifacts
      uses: actions/upload-release-asset@v1
      with:
        asset_path: |
          src-tauri/target/release/bundle/appimage/*.AppImage
          src-tauri/target/release/bundle/deb/*.deb
```

### Android Release Build

```yaml
build-android:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'zulu'  # Critical for Android builds
        cache: gradle
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
    
    - name: Setup NDK
      uses: nttld/setup-ndk@v1
      with:
        ndk-version: r27c
        link-to-sdk: true
    
    - name: Build release APK
      run: ./gradlew assembleRelease --no-daemon
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
    
    - name: Sign APK
      uses: r0adkll/sign-android-release@v1
      with:
        releaseDirectory: app/build/outputs/apk/release
        signingKeyBase64: ${{ secrets.SIGNING_KEY }}
        alias: ${{ secrets.KEY_ALIAS }}
        keyStorePassword: ${{ secrets.KEYSTORE_PASSWORD }}
```

---

## How to Trigger a Release

### 1. Update Version

Update version in all required files:

```bash
# package.json
# src-tauri/tauri.conf.json
# src-tauri/Cargo.toml
# app/build.gradle
```

### 2. Update Changelog

Add release notes to `CHANGELOG.md`:

```markdown
## [2026.32.0] — 2026-03-24

### Added
- New feature description

### Fixed
- Bug fix description
```

### 3. Commit and Tag

```bash
# Stage changes
git add -A

# Commit
git commit -m "Release v2026.32.0"

# Create tag
git tag -a v2026.32.0 -m "Release version 2026.32.0"

# Push
git push origin main
git push origin v2026.32.0
```

### 4. Monitor Build

1. Go to GitHub → Actions tab
2. Watch release workflow progress
3. All platforms build in parallel
4. Artifacts uploaded to release

### 5. Verify Release

1. Go to Releases page
2. Verify all artifacts present:
   - `.dmg` (macOS)
   - `.exe` (Windows NSIS)
   - `.msi` (Windows MSI)
   - `.AppImage` (Linux)
   - `.deb` (Linux Debian)
   - `.apk` (Android)

---

## Build Configuration Details

### Tauri Bundle Configuration

```json
// src-tauri/tauri.conf.json
{
  "bundle": {
    "active": true,
    "targets": ["dmg", "nsis", "msi", "appimage", "deb"],
    "icon": [
      "icons/32x32.png",
      "icons/128x128.png",
      "icons/icon.icns",
      "icons/icon.ico"
    ],
    "resources": [
      "python/ios_backup/**",
      "python/ios_bypass/**",
      "resources/linux/**",
      "resources/macos/**",
      "resources/windows/**"
    ],
    "macOS": {
      "minimumSystemVersion": "11.0",
      "hardenedRuntime": true,
      "entitlements": "Entitlements.plist"
    },
    "windows": {
      "nsis": {
        "installMode": "both"
      }
    }
  }
}
```

### Rust Release Profile

```toml
# src-tauri/Cargo.toml
[profile.release]
opt-level = 3
lto = true
codegen-units = 1
strip = true
panic = "abort"

[target.universal-apple-darwin]
rustflags = [
  "-C", "link-arg=-arch",
  "-C", "link-arg=arm64",
  "-C", "link-arg=-arch",
  "-C", "link-arg=x86_64"
]
```

### Android Signing

```groovy
// app/build.gradle
android {
    signingConfigs {
        release {
            storeFile file("release.jks")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## Secrets Configuration

Required repository secrets:

| Secret | Purpose |
|--------|---------|
| `SIGNING_KEY` | Base64-encoded Android keystore |
| `KEYSTORE_PASSWORD` | Android keystore password |
| `KEY_ALIAS` | Android key alias |
| `KEY_PASSWORD` | Android key password |
| `TAURI_PRIVATE_KEY` | Tauri updater signing key |
| `TAURI_KEY_PASSWORD` | Tauri key password |

### Setting Up Secrets

1. Go to GitHub → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add each secret with appropriate value

---

## Local Development Build

### Desktop (Tauri)

```bash
# Install dependencies
pnpm install

# Development mode
pnpm tauri:dev

# Production build
pnpm tauri:build
```

### Android

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

---

## Troubleshooting CI Builds

| Issue | Solution |
|-------|----------|
| Gradle daemon OOM | Increase heap: `org.gradle.jvmargs=-Xmx4096m` |
| NDK not found | Use `setup-ndk` action with version `r27c` |
| JDK version mismatch | Use `zulu` distribution for Android |
| Rust build fails | Ensure `libgtk-3-dev` installed on Linux |
| Signing fails | Verify secrets are set correctly |

See [Troubleshooting](Troubleshooting.md) for more.
