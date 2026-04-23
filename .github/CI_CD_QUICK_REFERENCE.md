# CI/CD Pipeline Quick Reference

## Quick Start

### Trigger Manual Build
```bash
# Build everything
gh workflow run complete-pipeline.yml \
  --field build_type=full

# Build only desktop apps
gh workflow run complete-pipeline.yml \
  --field build_type=desktop-only

# Build only Android APKs
gh workflow run complete-pipeline.yml \
  --field build_type=android-only

# Create release with specific version
gh workflow run complete-pipeline.yml \
  --field release_tag=v2027.18.1 \
  --field build_type=full
```

### Create Release
```bash
# Tag and push
git tag v2027.18.1
git push origin v2027.18.1

# Pipeline automatically triggers full build + release
```

## Build Commands (Local)

### Full Build
```bash
# 1. Install dependencies
npm install
pnpm install  # if using pnpm

# 2. Build React frontend
npm run build

# 3. Build Tauri desktop app
npm run tauri:build

# 4. Build Android APK
./gradlew assembleRelease
```

### Quick Tests
```bash
# TypeScript validation
npx tsc --noEmit

# Rust tests
cd src-tauri && cargo test

# Android tests
./gradlew testDebugUnitTest

# Python tests
pytest
```

### Verify Build
```bash
# Check all artifacts
./scripts/verify_complete_build.sh all

# Check desktop only
./scripts/verify_complete_build.sh desktop

# Check Android only
./scripts/verify_complete_build.sh android
```

## Workflow Files

| File | Purpose | Trigger |
|------|---------|---------|
| `complete-pipeline.yml` | Full CI/CD pipeline | Push, PR, tags, manual |
| `release.yml` | Production release | Tags, manual |
| `build.yml` | Quick debug build | Push, PR |

## Required Secrets

### Minimum (for builds)
- `GITHUB_TOKEN` (auto-provided)

### Recommended (for releases)
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

### Optional (for signed updates)
- `TAURI_SIGNING_PRIVATE_KEY`
- `TAURI_SIGNING_PRIVATE_KEY_PASSWORD`
- `APPLE_INSTALLER_SIGNING_IDENTITY`

## Build Outputs

### Desktop (Tauri)
```
target/
├── aarch64-apple-darwin/release/bundle/
│   ├── dmg/*.dmg              # macOS ARM64 installer
│   └── macos/*.app.tar.gz     # macOS ARM64 archive
├── x86_64-apple-darwin/release/bundle/
│   └── dmg/*.dmg              # macOS Intel installer
├── x86_64-unknown-linux-gnu/release/bundle/
│   ├── appimage/*.AppImage    # Linux AppImage
│   └── deb/*.deb              # Linux DEB package
└── x86_64-pc-windows-msvc/release/bundle/
    └── nsis/*.exe             # Windows installer
```

### Android
```
app/build/outputs/apk/
├── debug/*.apk                # Debug APK (unsigned/debug signed)
└── release/*.apk              # Release APK (release signed)
```

## Pipeline Stages

```
1. Validation & Testing    → Type checks, linting, unit tests
2. Desktop Build           → Tauri apps (4 platforms)
3. Android Build           → Debug + Release APKs
4. Integration Testing     → Cross-component validation
5. Release & Deployment    → GitHub Release creation
6. Notification            → Build summary & reporting
```

## Troubleshooting

### Build Fails
```bash
# Check logs
gh run view --log

# Re-run failed jobs
gh run rerun <run-id> --failed
```

### Slow Builds
- First build: 25-40 minutes (no cache)
- Cached build: 10-18 minutes
- Use `build_type` to build only what you need

### Missing Artifacts
```bash
# Verify locally
./scripts/verify_complete_build.sh all

# Download from GitHub
gh run download <run-id>
```

## Common Scenarios

### Scenario 1: Test PR Changes
```bash
# Push to feature branch, create PR
# Pipeline runs: validate + tests only (fast)
```

### Scenario 2: Build Desktop Update
```bash
gh workflow run complete-pipeline.yml \
  --field build_type=desktop-only \
  --ref feature/desktop-update
```

### Scenario 3: Release New Version
```bash
# 1. Update CHANGELOG.md
# 2. Tag release
git tag v2027.19.0
git push origin v2027.19.0

# 3. Pipeline auto-runs: full build + release
```

### Scenario 4: Emergency Hotfix
```bash
# 1. Fix issue
# 2. Quick build
gh workflow run complete-pipeline.yml \
  --field build_type=android-only \
  --ref hotfix/critical-fix

# 3. Manual release after verification
```

## Performance Tips

1. **Use selective builds during development:**
   - `desktop-only` for UI/backend changes
   - `android-only` for mobile changes
   - `test-only` for validation

2. **Leverage caching:**
   - Push to same branch for cache hits
   - Avoid force pushes (clears cache)

3. **Parallel execution:**
   - Desktop builds run in parallel (4 platforms)
   - Tests run in parallel (Rust, Android, Python)

## Monitoring

### View Pipeline Status
```bash
# List recent runs
gh run list --workflow=complete-pipeline.yml

# View specific run
gh run view <run-id>

# Watch live
gh run watch <run-id>
```

### Download Artifacts
```bash
# List artifacts
gh run view <run-id> --json artifacts

# Download all
gh run download <run-id>

# Download specific
gh run download <run-id> -n tauri-macos-arm64
```

## Contact & Support

- **Documentation:** `.github/CI_CD_PIPELINE.md`
- **Issues:** Use `[CI/CD]` prefix
- **Logs:** GitHub Actions → Workflow Runs
