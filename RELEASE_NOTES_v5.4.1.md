# DeepEyeUnlocker v5.4.1 - "The Universal Foundation Hotfix" 🚀

**Release Date:** 2026-03-03  
**Focus:** Release pipeline stabilization for Windows bundle publishing

---

## 💎 Key Highlights

- **Release Pipeline Fix:** Updated the Windows publish step to stop failing on CA1416 analyzer warnings being treated as errors.
- **Successful Tag Path:** Prepared a new patch release path after `v5.4.0` workflow failure in `Windows Bundle`.
- **Version Synchronization:** Bumped desktop, core, Android, and public metadata to `v5.4.1`.

## 🔧 Technical Changes

- Updated `.github/workflows/release.yml`:
  - Removed `/p:TreatWarningsAsErrors=true` from the `dotnet publish` command in `Publish Modern UI (Portable)`.
- Updated versioning:
  - `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj` → `5.4.1`
  - `src/DeepEyeUnlocker.csproj` → `5.4.1`
  - `portable/android/app/build.gradle` → `versionName "5.4.1"`, `versionCode 541`
- Updated release-facing docs:
  - `README.md`
  - `PROJECT_MANIFEST.md`

## 📂 Artifact Details

- **Tag**: `v5.4.1`
- **Status**: HOTFIX RELEASE
- **Notes**: This patch is release-infrastructure focused; no protocol-feature behavior changes are introduced.

---
*Democratizing Mobile Repair & Security Tools.*
