# DeepEyeUnlocker - Quick Installation Guide

## ✅ Installation Status

| Platform | Status       | Location                            | Size  |
| -------- | ------------ | ----------------------------------- | ----- |
| macOS    | ✅ Installed | `/Applications/DeepEyeUnlocker.app` | 30 MB |
| Android  | ✅ Installed | `com.deepeye.otg`                   | 75 MB |

---

## 🚀 Quick Launch

### macOS

```bash
open /Applications/DeepEyeUnlocker.app
```

### Android

```bash
adb shell am start -n com.deepeye.otg/.MainActivity
```

---

## 📦 Installation Commands

### Install macOS (.pkg)

```bash
sudo installer -pkg \
  target/x86_64-apple-darwin/release/bundle/pkg/DeepEyeUnlocker_2027.18.1_x86_64.pkg \
  -target /
```

### Install Android (.apk)

```bash
adb install -r -d app/build/outputs/apk/release/app-release.apk
```

---

## 🔧 Automated Installation Script

```bash
# Interactive menu
./scripts/install_deepeyeunlocker.sh

# Install both platforms
./scripts/install_deepeyeunlocker.sh both

# Check status
./scripts/install_deepeyeunlocker.sh status
```

---

## ✅ Verification

### macOS

```bash
ls -la /Applications/DeepEyeUnlocker.app
```

### Android

```bash
adb shell pm list packages | grep com.deepeye.otg
```

---

## 🗑️ Uninstall

### macOS

```bash
sudo rm -rf /Applications/DeepEyeUnlocker.app
```

### Android

```bash
adb uninstall com.deepeye.otg
```

---

## 🔍 Troubleshooting

### macOS: App won't open

```bash
sudo xattr -rd com.apple.quarantine /Applications/DeepEyeUnlocker.app
```

### Android: Device not found

```bash
adb kill-server
adb start-server
adb devices
```

### Android: Check logs

```bash
adb logcat | grep -i deepeye
```

---

## 📋 Build Artifacts

| File        | Size   | Location                                         |
| ----------- | ------ | ------------------------------------------------ |
| macOS PKG   | 8.3 MB | `target/x86_64-apple-darwin/release/bundle/pkg/` |
| macOS App   | 30 MB  | `target/release/bundle/macos/`                   |
| Android APK | 75 MB  | `app/build/outputs/apk/release/`                 |

---

## 🔐 Permissions

### macOS

- USB Access (for device communication)
- File Access (for backup/restore)
- Network Access (for updates)

### Android

- USB Debugging (Developer options)
- USB OTG (for device communication)
- Storage (for file operations)

---

## 📚 Documentation

- **Full Report:** `INSTALLATION_REPORT.md`
- **CI/CD Pipeline:** `.github/CI_CD_PIPELINE.md`
- **Quick Reference:** `.github/CI_CD_QUICK_REFERENCE.md`
- **Architecture:** `.github/CI_CD_ARCHITECTURE.md`

---

## 🆘 Support

**Package Name:** `com.deepeye.otg`  
**Version:** 2027.18.1  
**Build Date:** April 23, 2026

For issues, check:

1. Installation logs
2. Application logs (Console.app or adb logcat)
3. USB device connections
4. Permission settings
