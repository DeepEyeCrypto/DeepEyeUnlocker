# DeepEyeUnlocker CI/CD Pipeline Documentation

## Overview

This document describes the comprehensive CI/CD pipeline for the DeepEyeUnlocker project, which automates the complete build, test, and deployment process for all components of this multi-platform security research tool.

## Pipeline Architecture

The pipeline is organized into **6 stages** that execute sequentially with parallel job execution within each stage:

```
┌─────────────────────────────────────────────────────────┐
│  STAGE 1: VALIDATION & TESTING                         │
│  ├─ validate-codebase                                  │
│  ├─ test-rust-components                               │
│  ├─ test-android-components                            │
│  └─ test-python-components                             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  STAGE 2: DESKTOP BUILD (TAURI)                        │
│  ├─ build-tauri-macos-arm                              │
│  ├─ build-tauri-macos-intel                            │
│  ├─ build-tauri-linux                                  │
│  └─ build-tauri-windows                                │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  STAGE 3: ANDROID BUILD                                │
│  ├─ build-android-debug                                │
│  └─ build-android-release                              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  STAGE 4: INTEGRATION TESTING                          │
│  └─ integration-tests                                  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  STAGE 5: RELEASE & DEPLOYMENT                         │
│  └─ create-release                                     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  STAGE 6: NOTIFICATION & REPORTING                     │
│  └─ notify-completion                                  │
└─────────────────────────────────────────────────────────┘
```

## Workflow Files

### 1. `complete-pipeline.yml` (Main Pipeline)

The comprehensive CI/CD pipeline that handles all aspects of the build process.

**Trigger Events:**
- Push to `main` or `develop` branches
- Tag pushes (e.g., `v2027.18.1`)
- Pull requests to `main`
- Manual trigger via `workflow_dispatch`

**Manual Trigger Options:**
```yaml
release_tag: "v2027.18.1"  # Optional: specify release version
build_type:                # Required: select build scope
  - full                   # Build everything
  - desktop-only           # Build only Tauri desktop apps
  - android-only           # Build only Android APKs
  - test-only              # Run only tests
```

### 2. `release.yml` (Production Release)

Focused workflow for production releases with signing and publishing.

**Trigger Events:**
- Tag pushes matching `v*`
- Manual dispatch with release tag

### 3. `build.yml` (Quick Build)

Lightweight workflow for quick debug builds and PR validation.

**Trigger Events:**
- Push to `main`
- Pull requests to `main`

## Build Components

### Desktop Applications (Tauri v2 + Rust)

| Platform | Architecture | Output Format | Bundle Size |
|----------|-------------|---------------|-------------|
| macOS    | ARM64 (Apple Silicon) | `.dmg`, `.app`, `.pkg` | ~80-120 MB |
| macOS    | x86_64 (Intel) | `.dmg`, `.app` | ~80-120 MB |
| Linux    | x86_64 | `.AppImage`, `.deb` | ~70-100 MB |
| Windows  | x86_64 | `.exe` (NSIS installer) | ~60-90 MB |

**Features Compiled:**
- iOS bypass tools (palera1n integration)
- Device protocol handlers (USB, BROM, EDL)
- Forensic data extraction utilities
- Firmware flashing utilities
- Python integration layer

### Mobile Applications (Android)

| Build Type | Architecture | Output Format | Size |
|------------|-------------|---------------|------|
| Debug      | arm64-v8a | `.apk` (debug signed) | ~50-80 MB |
| Release    | arm64-v8a | `.apk` (release signed) | ~40-60 MB |

**Features Compiled:**
- Android FRP removal tools
- ADB integration
- Xiaomi Flash Tool
- Samsung Odin protocol
- MTK BROM support
- Qualcomm EDL mode
- USB OTG device communication

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Frontend | React 18 + TypeScript + Vite | UI/UX |
| Desktop Backend | Rust + Tauri v2 | Native system access |
| Mobile Backend | Kotlin + Jetpack Compose | Android native |
| Python Layer | Python 3.11 + Chaquopy | iOS tools, forensic scripts |
| Native Libs | C/C++ (libusb) | USB device communication |
| State Management | Hilt (Android), Tauri State (Rust) | DI & state |

## Pipeline Stages Detail

### Stage 1: Validation & Testing

**Purpose:** Catch issues early before expensive build processes.

**Jobs:**
1. **validate-codebase**
   - Detects changed components (Rust, Android, Frontend, Python)
   - Runs TypeScript type checking
   - Executes Jest test suite
   - Uploads coverage to Codecov

2. **test-rust-components**
   - Runs `cargo fmt` (formatting check)
   - Runs `cargo clippy` (linting with strict warnings)
   - Executes unit tests with `cargo test --release`
   - Security audit with `cargo audit`

3. **test-android-components**
   - Kotlin lint (`lintDebug`)
   - Detekt static analysis
   - Unit tests (`testDebugUnitTest`)
   - Uploads test reports

4. **test-python-components**
   - Flake8 linting
   - Pytest execution
   - Dependency validation

### Stage 2: Desktop Build (Tauri)

**Purpose:** Compile optimized desktop applications for all platforms.

**Configuration:**
```toml
# Cargo.toml - Release Profile
[profile.release]
opt-level = 3          # Maximum optimization
lto = true             # Link Time Optimization
codegen-units = 1      # Single codegen unit for best optimization
strip = true           # Remove debug symbols
panic = "abort"        # Smaller binary size
```

**Jobs (Parallel):**
- `build-tauri-macos-arm`: macOS ARM64 (Apple Silicon)
- `build-tauri-macos-intel`: macOS x86_64 (Intel)
- `build-tauri-linux`: Linux (AppImage + DEB)
- `build-tauri-windows`: Windows (NSIS installer)

**Build Steps:**
1. Install system dependencies (libusb, GTK, WebKit)
2. Setup Rust toolchain with target triple
3. Install JavaScript dependencies (pnpm)
4. Build React frontend (Vite)
5. Compile Rust backend with LTO
6. Bundle into platform-specific installers
7. Upload artifacts (30-day retention)

### Stage 3: Android Build

**Purpose:** Build debug and release APKs with proper signing.

**Jobs:**
1. **build-android-debug**
   - Fast debug builds for PR validation
   - Signed with debug keystore
   - Includes debug symbols

2. **build-android-release**
   - Production-ready release builds
   - Requires signing secrets (GitHub Secrets)
   - Minification disabled for forensic tools
   - ProGuard rules applied

**Signing Configuration:**

Required GitHub Secrets:
```
ANDROID_KEYSTORE_BASE64  # Base64-encoded JKS keystore
ANDROID_STORE_PASSWORD   # Keystore password
ANDROID_KEY_ALIAS        # Key alias
ANDROID_KEY_PASSWORD     # Key password
```

### Stage 4: Integration Testing

**Purpose:** Validate that all components work together.

**Prerequisites:** Requires successful desktop and Android builds.

**Tests:**
- Cross-component communication
- API contract validation
- End-to-end workflows (placeholder for custom tests)

### Stage 5: Release & Deployment

**Purpose:** Create GitHub Release with all artifacts.

**Trigger Conditions:**
- Tag push matching `v*` pattern
- Manual dispatch with `release_tag` specified

**Steps:**
1. Download all build artifacts
2. Generate release notes from `CHANGELOG.md`
3. Stage and rename artifacts with version
4. Create GitHub Release (auto-publish)
5. Mark as pre-release if tag contains `beta`, `alpha`, or `rc`

**Artifact Naming Convention:**
```
DeepEyeUnlocker_{version}_{platform}.{ext}

Examples:
- DeepEyeUnlocker_2027.18.1_aarch64.dmg
- DeepEyeUnlocker_2027.18.1_x86_64.AppImage
- DeepEyeUnlocker_2027.18.1_android.apk
- DeepEyeUnlocker_2027.18.1_x86_64-setup.exe
```

### Stage 6: Notification & Reporting

**Purpose:** Provide build summary and metrics.

**Output:**
- GitHub Actions summary with build stats
- Artifact inventory
- Pass/fail status

## Required GitHub Secrets

### Mandatory Secrets

| Secret Name | Description | Required For |
|------------|-------------|--------------|
| `GITHUB_TOKEN` | Auto-provided by GitHub | All workflows |

### Optional Secrets (for signed releases)

| Secret Name | Description | Required For |
|------------|-------------|--------------|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded JKS file | Android release signing |
| `ANDROID_STORE_PASSWORD` | Keystore password | Android release signing |
| `ANDROID_KEY_ALIAS` | Key alias name | Android release signing |
| `ANDROID_KEY_PASSWORD` | Key password | Android release signing |
| `TAURI_SIGNING_PRIVATE_KEY` | Tauri updater private key | Signed updates |
| `TAURI_SIGNING_PRIVATE_KEY_PASSWORD` | Updater key password | Signed updates |
| `APPLE_INSTALLER_SIGNING_IDENTITY` | macOS installer signing cert | PKG signing |

### Setting Up Secrets

1. Go to repository **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add each secret with appropriate value

**Example: Generate Android Keystore Base64**
```bash
base64 -i app/deepeye-release.jks | pbcopy
# Paste the output as ANDROID_KEYSTORE_BASE64 secret
```

## Caching Strategy

The pipeline uses intelligent caching to speed up builds:

| Cache Type | Scope | Strategy |
|-----------|-------|----------|
| Gradle | Android builds | Cache `~/.gradle/caches` |
| Rust/Cargo | Tauri builds | Cache `src-tauri/target` |
| pnpm | Frontend builds | Cache `pnpm-store` |
| Node modules | All platforms | Cache `node_modules` |

**Cache Hit Rates:**
- Incremental builds: 60-80% faster
- Full rebuilds: 30-40% faster
- Average build time after cache: 8-12 minutes

## Branch Strategy

| Branch | Purpose | Pipeline Behavior |
|--------|---------|-------------------|
| `main` | Production code | Full build + tests |
| `develop` | Development integration | Full build + tests |
| `feature/*` | Feature development | PR validation only |
| `release/*` | Release preparation | Full build + staging |
| Tags `v*` | Versioned releases | Full build + publish |

## Path Filtering

The pipeline only runs relevant jobs based on changed files:

```yaml
# Component detection
rust:
  - 'src-tauri/**'
  - 'Cargo.toml'
  - 'Cargo.lock'
android:
  - 'app/**'
  - 'build.gradle.kts'
frontend:
  - 'src/**'
  - 'package.json'
  - 'vite.config.ts'
python:
  - 'src-tauri/python/**'
  - 'app/src/main/python/**'
```

**Benefits:**
- Faster PR validation (only affected components)
- Reduced CI/CD costs
- Parallel execution optimization

## Build Verification

After pipeline execution, verify artifacts locally:

```bash
# Run complete build verification
./scripts/verify_complete_build.sh all

# Verify only desktop builds
./scripts/verify_complete_build.sh desktop

# Verify only Android builds
./scripts/verify_complete_build.sh android

# Check specific artifact directory
./scripts/verify_complete_build.sh all ./artifacts
```

**Verification Checks:**
- ✓ React frontend bundles
- ✓ Tauri desktop installers (all platforms)
- ✓ Android APKs (debug + release)
- ✓ Rust binaries compiled
- ✓ Python scripts included
- ✓ Build configuration files
- ✓ CI/CD pipeline files present

## Troubleshooting

### Common Issues

#### 1. Rust Build Fails on Linux
**Error:** Missing system dependencies
```bash
# Solution: Install required packages
sudo apt-get update
sudo apt-get install -y \
  build-essential \
  libdbus-1-dev \
  libssl-dev \
  libudev-dev \
  libusb-1.0-0-dev \
  libgtk-3-dev \
  libwebkit2gtk-4.1-dev \
  librsvg2-dev \
  pkg-config
```

#### 2. Android Build Fails
**Error:** Keystore not found
```bash
# Solution: Create debug keystore for local testing
keytool -genkey -v \
  -keystore app/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

#### 3. Tauri Build Timeout
**Error:** Build takes too long
**Solution:** The LTO optimization is CPU-intensive. Expected times:
- macOS Apple Silicon: 8-12 minutes
- macOS Intel: 12-18 minutes
- Linux: 10-15 minutes
- Windows: 10-14 minutes

#### 4. Missing Python Dependencies
**Error:** Python modules not found
```bash
# Install required Python packages
pip install pyusb==1.2.1 construct==2.10.68 pycryptodome==3.20.0
```

### Debug Mode

To debug pipeline issues:

1. **Enable verbose logging:**
   ```yaml
   env:
     RUST_BACKTRACE: "1"
     RUST_LOG: "debug"
   ```

2. **Download workflow logs:**
   - Go to Actions → Workflow Run
   - Click on failed job
   - Download log archive

3. **Re-run with SSH access:**
   - Use `tmate` action for interactive debugging
   - Add to workflow:
     ```yaml
     - uses: mxschmitt/action-tmate@v3
       if: ${{ failure() }}
     ```

## Performance Metrics

### Typical Build Times

| Component | First Build | Cached Build |
|-----------|-------------|--------------|
| React Frontend | 2-3 min | 30-45 sec |
| Rust (macOS ARM) | 10-15 min | 3-5 min |
| Rust (Linux) | 12-18 min | 4-6 min |
| Rust (Windows) | 10-14 min | 3-5 min |
| Android Debug | 3-5 min | 1-2 min |
| Android Release | 5-8 min | 2-3 min |
| **Total Pipeline** | **25-40 min** | **10-18 min** |

### Resource Usage

| Job Type | CPU | Memory | Disk |
|----------|-----|--------|------|
| Rust Build | 100% (LTO) | 4-8 GB | 10-15 GB |
| Android Build | 60-80% | 2-4 GB | 5-8 GB |
| Frontend Build | 40-60% | 1-2 GB | 2-3 GB |

## Customization

### Adding New Platform

To add support for a new platform:

1. Add matrix entry in `build-tauri-*` job:
   ```yaml
   - os: ubuntu-latest
     target: aarch64-unknown-linux-gnu
     bundles: appimage,deb
   ```

2. Add system dependencies installation step

3. Add artifact upload step with new platform name

### Adding New Test Suite

1. Create new job in Stage 1:
   ```yaml
   test-custom-component:
     name: Test Custom Component
     runs-on: ubuntu-latest
     steps:
       - uses: actions/checkout@v4
       - run: ./scripts/run_custom_tests.sh
   ```

2. Add to `integration-tests` dependencies

### Custom Deployment

To deploy to additional platforms (e.g., Homebrew, AUR):

1. Add new job in Stage 5:
   ```yaml
   deploy-homebrew:
     name: Deploy to Homebrew
     needs: create-release
     runs-on: macos-latest
     steps:
       - name: Update Homebrew tap
         run: ./scripts/update_homebrew.sh
   ```

## Best Practices

1. **Always test locally before pushing:**
   ```bash
   # Desktop build
   npm run tauri:build
   
   # Android build
   ./gradlew assembleRelease
   ```

2. **Use semantic versioning:**
   ```
   v{year}.{major}.{minor}
   Example: v2027.18.1
   ```

3. **Keep CHANGELOG.md updated:**
   - Pipeline extracts release notes from changelog
   - Use format: `## [version] - YYYY-MM-DD`

4. **Monitor build times:**
   - Use GitHub Actions billing page
   - Optimize slow jobs with better caching

5. **Use manual triggers for testing:**
   ```bash
   gh workflow run complete-pipeline.yml \
     --field build_type=desktop-only \
     --ref feature/my-branch
   ```

## Security Considerations

1. **Secrets Management:**
   - Never commit secrets or keys
   - Use GitHub Secrets for sensitive data
   - Rotate signing keys regularly

2. **Dependency Auditing:**
   - Rust: `cargo audit` runs automatically
   - Node: `npm audit` in CI
   - Python: `pip-audit` can be added

3. **Artifact Signing:**
   - Tauri: Ed25519 signatures for updates
   - Android: JKS keystore signing
   - macOS: Apple notarization (optional)

## Support

For pipeline issues:
1. Check this documentation
2. Review GitHub Actions logs
3. Open issue with `[CI/CD]` prefix
4. Include workflow run URL

## Changelog

- **v2.0.0** (Current): Complete pipeline with 6 stages
- **v1.0.0**: Basic build and release workflows
