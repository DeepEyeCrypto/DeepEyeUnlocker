# OLD UI DIAGNOSIS REPORT

## Issue Summary

The GitHub Releases for DeepEyeUnlocker Pro are shipping the outdated WinForms UI instead of the new WPF "Modern" UI, even though the source code contains both and local development is correctly using the new UI.

## Root Cause Analysis

### 1. Build Artifact Mismatch (Confirmed)

The GitHub Actions workflow (`.github/workflows/release.yml`) is hardcoded to publish the old project:

```yaml
run: |
  dotnet publish src/DeepEyeUnlocker.csproj ...
```

This builds the legacy WinForms entry point. The new UI lives in `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj`, which is not referenced in the build script.

### 2. Startup Project Divergence

Local development likely uses `DeepEye.UI.Modern` as the startup project in Visual Studio. However, the CI pipeline was never updated to reflect this switch, resulting in "stale" binaries being shipped to users.

### 3. Versioning Desync

Both projects are currently set to version `3.0.0` in their respective `.csproj` files. This makes it difficult for the auto-updater to distinguish between the "Old 3.0.0" and "New 3.0.0" if binary signatures aren't checked.

## Applied Fixes

- [ ] **Updated Release Pipeline**: Modified `.github/workflows/release.yml` to target `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj`.
- [ ] **Artifact Naming**: Standardized output names to ensure the WPF executable is the one being zipped and uploaded.
- [ ] **Build Stamping**: (Proposed) Integrating Git SHA into the build to ensure 100% traceability.

## Verification Steps

1. Push a new tag (e.g., `v3.0.1`).
2. Verify that the GitHub Action builds `DeepEye.UI.Modern`.
3. Download the resulting `DeepEyeUnlocker-Portable.zip`.
4. Run the EXE and confirm the presence of "Driver Center Pro" and "Health Center" tabs.
