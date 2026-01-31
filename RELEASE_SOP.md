# DeepEyeUnlocker Pro - Release SOP (Standard Operating Procedure)

This document ensures that every release contains the latest UI and core features, preventing the "Old UI shipping" issue.

## 1. Pre-Release Checklist

- [ ] **Verify Local Build**: Run the solution in Visual Studio/Rider using `DeepEye.UI.Modern` as the startup project. Confirm the new UI is visible.
- [ ] **Increment Version**: Update `<Version>` in `DeepEye.UI.Modern/DeepEye.UI.Modern.csproj` and `src/DeepEyeUnlocker.csproj`.
- [ ] **Clean Build**: Run `dotnet clean` to ensure no stale artifacts exist in `bin/` or `obj/` folders.

## 2. Release Steps (Automated via GitHub Actions)

1. **Commit Changes**: Ensure all UI changes are committed to the `main` branch.
2. **Push Tag**: Create and push a new semantic tag:

   ```bash
   git tag v3.0.1
   git push origin v3.0.1
   ```

3. **Monitor CI**: Go to GitHub Actions and ensure the **Release Build** workflow completes successfully.
   - It is now configured to build `DeepEye.UI.Modern` assembly named `DeepEyeUnlocker.exe`.

## 3. Manual Release (SOP for Local Builds)

If building manually for distribution:

```powershell
# 1. Clean previous artifacts
Remove-Item -Recurse -Force ./artifacts/portable

# 2. Publish the Modern UI project
dotnet publish DeepEye.UI.Modern/DeepEye.UI.Modern.csproj -c Release -r win-x64 --self-contained true /p:PublishSingleFile=true -o "artifacts/portable"

# 3. Verify
./artifacts/portable/DeepEyeUnlocker.exe
# Ensure "Driver Center Pro" and "Health Center" are present.
```

## 4. Troubleshooting

### "I see the old UI in the release zip!"

- **Cause**: The CI built `src/DeepEyeUnlocker.csproj` instead of `DeepEye.UI.Modern`.
- **Fix**: Check `.github/workflows/release.yml` and ensure it targets `DeepEye.UI.Modern`.

### "Auto-updater is not pulling the new version!"

- **Cause**: The `DeepEyeUnlocker.io` API or `version.json` has not been updated with the new tag URL.
- **Fix**: Update the backend version record to point to the new GitHub Release asset.
