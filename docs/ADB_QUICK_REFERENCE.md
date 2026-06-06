# ADB Quick Reference for DeepEye Unlocker

## ✅ Current Setup Status

```
ADB Version: 37.0.0-14910828
Status: INSTALLED & VERIFIED
Connected Device: Motorola Edge 30 Pro (ZD2226X6RW)
Android: 14 (SDK 34)
```

---

## Essential ADB Commands

### Device Management

```bash
# List connected devices
adb devices

# Show device path and state
adb devices -l

# Restart ADB server (if devices not detected)
adb kill-server
adb start-server

# Connect to wireless ADB (if configured)
adb connect 192.168.1.100:5555
```

### Device Information

```bash
# Get device model
adb shell getprop ro.product.model

# Get Android version
adb shell getprop ro.build.version.release

# Get SDK level
adb shell getprop ro.build.version.sdk

# Get brand
adb shell getprop ro.product.brand

# Get build ID
adb shell getprop ro.build.id

# Get all properties
adb shell getprop
```

### Shell Commands

```bash
# Execute shell command
adb shell <command>

# Examples:
adb shell ls /sdcard/
adb shell pm list packages
adb shell dumpsys battery
adb shell getprop | grep ro.product

# Interactive shell
adb shell
```

### Reboot Operations

```bash
# Reboot to system
adb reboot

# Reboot to recovery
adb reboot recovery

# Reboot to bootloader
adb reboot bootloader

# Reboot to EDL mode (Qualcomm)
adb reboot edl
```

### File Operations

```bash
# Push file to device
adb push <local> <remote>
adb push myfile.apk /sdcard/

# Pull file from device
adb pull <remote> [local]
adb pull /sdcard/test.txt

# List files
adb shell ls /sdcard/
```

### App Management

```bash
# Install APK
adb install app.apk

# Install with grant permissions
adb install -g app.apk

# Reinstall (keep data)
adb install -r app.apk

# Uninstall app
adb uninstall com.package.name

# List installed packages
adb shell pm list packages

# List third-party packages
adb shell pm list packages -3
```

### FRP Operations (DeepEye Specific)

```bash
# Check FRP status
adb shell getprop ro.frp.pst

# List FRP-related partitions
adb shell ls -l /dev/block/bootdevice/by-name/ | grep frp

# Check if FRP is enabled
adb shell settings get global frp_enabled
```

### Root Detection

```bash
# Check for su binary
adb shell which su

# Check for Magisk
adb shell pm list packages | grep magisk

# Check build type (userdebug/eng = likely rootable)
adb shell getprop ro.build.type
```

### Logcat

```bash
# View logs
adb logcat

# Clear logs
adb logcat -c

# Save logs to file
adb logcat -d > logcat.txt

# Filter by priority
adb logcat *:E  # Errors only
```

---

## DeepEye Unlocker ADB Integration

### Tauri Backend Commands

The DeepEye Unlocker invokes these ADB operations via Tauri:

| UI Action     | Tauri Command             | ADB Command                 |
| ------------- | ------------------------- | --------------------------- |
| Scan Devices  | `adb_list_devices`        | `adb devices -l`            |
| Get Full Info | `adb_get_full_info`       | Multiple `getprop` calls    |
| Shell Command | `adb_shell_command`       | `adb shell <cmd>`           |
| Reboot        | `adb_reboot_device`       | `adb reboot [mode]`         |
| Install APK   | `adb_install_apk`         | `adb install`               |
| Push File     | `adb_push_file`           | `adb push`                  |
| Pull File     | `adb_pull_file`           | `adb pull`                  |
| Sideload      | `adb_sideload_zip`        | `adb sideload`              |
| Erase FRP     | `adb_erase_frp_partition` | Custom partition operations |
| Check Root    | `adb_check_root_access`   | `which su` + checks         |

---

## Troubleshooting

### Device Not Detected

```bash
# 1. Check USB connection
#    - Try different cable
#    - Try different USB port

# 2. Restart ADB server
adb kill-server
adb start-server

# 3. Check device settings
#    - USB Debugging enabled?
#    - USB mode set to MTP/PTP?

# 4. Re-authorize
#    - Revoke USB debugging authorizations on device
#    - Reconnect and accept RSA dialog
```

### "unauthorized" Status

```bash
# On device:
# Settings > Developer options > Revoke USB debugging authorizations
# Then reconnect USB and accept the prompt
```

### "offline" Status

```bash
# Restart ADB server
adb kill-server
adb start-server

# Or restart device
```

### Permission Denied

```bash
# On macOS: No additional setup needed

# On Linux:
sudo usermod -aG plugdev $USER
# Add udev rules (see ADB_SETUP_GUIDE.md)
```

---

## Device Setup Checklist

### Enable ADB on Device

- [ ] Enable Developer Options (tap Build Number 7 times)
- [ ] Enable USB Debugging
- [ ] Connect via USB
- [ ] Accept RSA authorization
- [ ] Verify with `adb devices`

### Verify Connection

```bash
# Should show:
# ZD2226X6RW      device

# NOT:
# ZD2226X6RW      unauthorized
# ZD2226X6RW      offline
```

---

## Quick Verification Script

Run the verification script to check everything:

```bash
bash scripts/verify_adb.sh
```

This will check:

- ✅ ADB installation
- ✅ PATH configuration
- ✅ ADB server status
- ✅ Connected devices
- ✅ Shell command execution

---

## Common Device States

| State          | Description                 | Solution                |
| -------------- | --------------------------- | ----------------------- |
| `device`       | ✅ Connected and authorized | Ready to use            |
| `unauthorized` | ❌ RSA key not accepted     | Accept dialog on device |
| `offline`      | ❌ Device not responding    | Restart ADB server      |
| `no device`    | ❌ No device connected      | Check USB cable         |
| `recovery`     | ⚠️ In recovery mode         | Reboot to system        |
| `bootloader`   | ⚠️ In fastboot mode         | Use `fastboot` commands |

---

## Advanced Commands

### Backup & Restore

```bash
# Full backup
adb backup -all -f backup.ab

# Restore backup
adb restore backup.ab

# Backup specific app
adb backup -f app.ab com.package.name
```

### Screen & Input

```bash
# Screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png

# Screen record
adb shell screenrecord /sdcard/video.mp4
adb pull /sdcard/video.mp4

# Tap at coordinates
adb shell input tap 500 500

# Swipe
adb shell input swipe 500 1000 500 500

# Text input
adb shell input text "HelloWorld"
```

### Network

```bash
# Forward port
adb forward tcp:8080 tcp:8080

# List forwards
adb forward --list

# Remove all forwards
adb forward --remove-all
```

### Multiple Devices

```bash
# Specify device by serial
adb -s ZD2226X6RW shell getprop ro.product.model

# List devices with paths
adb devices -l
```

---

## Resources

- **Official Docs**: https://developer.android.com/studio/command-line/adb
- **Platform Tools**: https://developer.android.com/studio/releases/platform-tools
- **DeepEye ADB Guide**: ADB_SETUP_GUIDE.md
- **Verification Report**: ADB_INSTALLATION_VERIFICATION.md

---

## Current Configuration

```yaml
System:
  OS: macOS Darwin 24.6.0
  Shell: zsh
  User: ejaj

ADB:
  Version: 37.0.0-14910828
  Path: /usr/local/bin/adb
  Server Port: tcp:5037
  Status: ✅ Running

Device:
  Serial: ZD2226X6RW
  Brand: Motorola
  Model: edge 30 pro
  Android: 14
  SDK: 34
  Status: ✅ Connected & Authorized

DeepEye Unlocker:
  ADB Integration: ✅ Ready
  All Commands: ✅ Available
  Backend: Tauri v2
```

---

**Last Updated**: April 15, 2026  
**Status**: ✅ FULLY OPERATIONAL
