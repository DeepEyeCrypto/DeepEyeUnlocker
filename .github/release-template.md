# DeepEyeUnlocker ${{ github.ref_name }}

**Build Date:** ${{ github.run_number }}
**Platforms:** macOS Universal, Windows x64, Linux x64, Android

## What's New

- Dashboard with live device detection and polling
- Typed state machine for reliable UI updates
- Adaptive progress tracker for desktop/mobile
- Quick actions grid with connection-aware enablement
- TerminalLog with clear/copy and Android collapse

## Downloads

### macOS
- [DeepEyeUnlocker.dmg]()
  - ⚠️ If macOS shows security warning: Right-click → Open, or `xattr -cr /Applications/DeepEyeUnlocker.app`

### Windows
- [DeepEyeUnlocker Setup.exe]()
- [DeepEyeUnlocker.msi]()

### Linux
- [DeepEyeUnlocker.AppImage]()
- [DeepEyeUnlocker.deb]()

### Android
- [DeepEyeUnlocker.apk]()
  - Enable "Unknown Sources" in Settings before installing

## Supported Devices

- Android (MTK, Qualcomm, Samsung)
- Apple (A7–A11, checkm8)

## Notes

- Windows: Code signing pending for production
- Linux: Requires libfuse2 for AppImage
- Android: Debug build (signed for direct download)