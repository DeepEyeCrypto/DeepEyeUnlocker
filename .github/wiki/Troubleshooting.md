# Troubleshooting Guide

Common issues and their solutions for DeepEye Unlocker.

---

## macOS Issues

### "Cannot be opened because the developer cannot be verified"

**Cause:** macOS Gatekeeper blocking unsigned application.

**Solution:**
```bash
# Option 1: Right-click method (Recommended)
# 1. Right-click the DeepEye Unlocker app
# 2. Click "Open"
# 3. Click "Open" in the security dialog

# Option 2: Terminal method
xattr -cr /Applications/DeepEye\ Unlocker.app

# Option 3: System Preferences
# 1. System Settings → Privacy & Security
# 2. Scroll to "Security"
# 3. Click "Open Anyway" next to DeepEye Unlocker
```

### App crashes on startup

**Cause:** Missing dependencies or corrupted download.

**Solution:**
```bash
# 1. Delete the app
rm -rf /Applications/DeepEye\ Unlocker.app

# 2. Re-download from releases page
# 3. Re-install

# 4. Check system requirements
sw_vers  # Should show macOS 11.0+
```

### USB device not detected

**Cause:** USB permissions or cable issue.

**Solution:**
1. Use genuine Apple USB cable for iOS devices
2. Try different USB port (USB 3.0 recommended)
3. Check System Information → USB for device presence
4. Restart the app after connecting device

---

## Windows Issues

### "Windows protected your PC" (SmartScreen)

**Cause:** Windows Defender SmartScreen blocking unknown publisher.

**Solution:**
1. Click "More info" on the warning dialog
2. Click "Run anyway"

Or disable temporarily:
```powershell
# Run as Administrator
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
```

### USB driver not found

**Cause:** Missing device-specific USB drivers.

**Solution:**

| Device Type | Driver Solution |
|-------------|-----------------|
| Qualcomm EDL | Install [QDLoader 9008 Driver](https://developer.qualcomm.com/) |
| Samsung | Install [Samsung USB Driver](https://developer.samsung.com/) |
| MediaTek | Install [MTK USB Driver](https://spflashtool.com/) |
| Fastboot/ADB | Install [Google USB Driver](https://developer.android.com/studio/run/win-usb) |

### App won't start (missing DLL)

**Cause:** Visual C++ Redistributables not installed.

**Solution:**
1. Download [VC++ Redistributables](https://aka.ms/vs/17/release/vc_redist.x64.exe)
2. Install both x64 and x86 versions
3. Restart computer

---

## Linux Issues

### AppImage won't run

**Cause:** Missing execute permission or FUSE.

**Solution:**
```bash
# Add execute permission
chmod +x DeepEyeUnlocker.AppImage

# If FUSE error, install libfuse
sudo apt-get install libfuse2  # Debian/Ubuntu
sudo dnf install fuse-libs      # Fedora

# Or extract and run
./DeepEyeUnlocker.AppImage --appimage-extract
./squashfs-root/AppRun
```

### USB permission denied

**Cause:** User not in plugdev group or missing udev rules.

**Solution:**
```bash
# Add user to plugdev group
sudo usermod -a -G plugdev $USER

# Create udev rules
sudo tee /etc/udev/rules.d/51-deepeye.rules << 'EOF'
SUBSYSTEM=="usb", ATTR{idVendor}=="05c6", ATTR{idProduct}=="9008", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="0e8d", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="05ac", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
EOF

# Reload rules
sudo udevadm control --reload-rules
sudo udevadm trigger

# Log out and back in
```

### .deb install fails

**Cause:** Missing dependencies.

**Solution:**
```bash
# Fix broken dependencies
sudo dpkg -i DeepEyeUnlocker_amd64.deb
sudo apt-get install -f

# Or install with apt
sudo apt-get install ./DeepEyeUnlocker_amd64.deb
```

---

## Android App Issues

### "App not installed"

**Cause:** Unknown sources not enabled or existing app conflict.

**Solution:**
1. Uninstall existing DeepEye Unlocker app
2. Enable "Unknown Sources" or "Install unknown apps"
3. Try installing again

### USB device not detected (Android host)

**Cause:** OTG not supported or permission denied.

**Solution:**
1. Verify device supports USB OTG (check specs)
2. Use quality OTG cable/adapter
3. Grant USB permission when prompted
4. Check USB debugging enabled on target device

### App crashes on open

**Cause:** Incompatible Android version or corrupted install.

**Solution:**
```bash
# Clear app data
Settings → Apps → DeepEye Unlocker → Storage → Clear Data

# Or reinstall
adb uninstall com.deepeye.otg
adb install DeepEyeUnlocker.apk
```

---

## Device Connection Issues

### Android device not detected

**Checklist:**
- [ ] USB debugging enabled on device
- [ ] "Trust this computer" dialog accepted
- [ ] Quality USB cable (not charge-only)
- [ ] Device shows in `adb devices`

**Solution:**
```bash
# Verify ADB connection
adb devices

# Expected output:
# List of devices attached
# xxxxxxxx    device

# If unauthorized:
# 1. Revoke USB debugging authorizations
#    (Developer Options → Revoke...)
# 2. Reconnect and accept dialog

# If still not detected:
adb kill-server
adb start-server
adb devices
```

### iPhone not detected

**Checklist:**
- [ ] Genuine Apple USB cable
- [ ] "Trust this computer" tapped on device
- [ ] iTunes/Apple Device Support installed (Windows)
- [ ] Device not in Recovery/DFU mode (for normal detection)

**Solution:**
```bash
# macOS: Check system report
system_profiler SPUSBDataType | grep -A 10 "iPhone"

# Windows: Check Device Manager
# Should show under "Portable Devices" or "Apple Mobile Device USB Driver"

# Linux: Check lsusb
lsusb | grep Apple
```

### EDL mode not entering

**Cause:** Wrong button combination or EDL disabled.

**Solution:**

| Device | Correct Method |
|--------|---------------|
| Xiaomi | Vol+ + Vol- + Power (or EDL cable) |
| Samsung | Vol+ + Vol- + Power (some models) |
| OnePlus | Vol+ + Vol- + Power |
| Generic | ADB: `adb reboot edl` |

**EDL Cable Method:**
1. Get Deep Flash cable (USB with shorted D+/D-)
2. Connect cable while holding Vol+
3. Check for QDLoader 9008 in device manager

---

## CI Build Failures

### Android build fails: "NDK not found"

**Cause:** NDK not installed or wrong version.

**Solution:**
```yaml
# In workflow file, add:
- name: Setup NDK
  uses: nttld/setup-ndk@v1
  with:
    ndk-version: r27c
    link-to-sdk: true
```

### Android build fails: "Java version mismatch"

**Cause:** Wrong JDK distribution or version.

**Solution:**
```yaml
# Use Zulu distribution for Android
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'zulu'  # Critical!
    cache: gradle
```

### Gradle daemon OOM

**Cause:** Insufficient heap memory.

**Solution:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
```

### Tauri build fails on Linux

**Cause:** Missing system dependencies.

**Solution:**
```yaml
- name: Install dependencies
  run: |
    sudo apt-get update
    sudo apt-get install -y \
      libgtk-3-dev \
      libwebkit2gtk-4.0-dev \
      libappindicator3-dev \
      librsvg2-dev
```

### Signing fails

**Cause:** Missing or incorrect secrets.

**Solution:**
1. Verify secrets are set in GitHub → Settings → Secrets
2. Check secret names match workflow file
3. For Android, ensure keystore is base64 encoded:
   ```bash
   base64 -i release.jks | pbcopy
   ```

---

## Common Error Messages

### "tauri.conf.json identifier must be at root level"

**Cause:** Misplaced identifier field in tauri.conf.json.

**Solution:**
```json
{
  "identifier": "com.deepeye.otg",  // Must be at root level
  "build": { ... },
  "app": { ... },
  "bundle": { ... }
}
```

### "error.NonExistentClass" (Hilt)

**Cause:** Missing Hilt dependency or annotation processor.

**Solution:**
```kotlin
// Add to app/build.gradle
kapt {
    correctErrorTypes = true
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
}
```

### "Unresolved reference: clickable"

**Cause:** Missing Compose foundation dependency.

**Solution:**
```kotlin
// Add to app/build.gradle
dependencies {
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.compose.ui:ui:1.6.0")
}
```

### "Stuck on 'Waiting for device'"

**Cause:** Device not in correct mode or not connected.

**Solution:**
1. Check USB cable connection
2. Verify device mode matches operation:
   - Normal mode → FMI check, info dump
   - Recovery → Restore, update
   - DFU → Restore, exploit
   - EDL → Flash, FRP bypass
3. Try different USB port
4. Restart DeepEye Unlocker

---

## Performance Issues

### Slow USB transfer speeds

**Cause:** USB 2.0 connection or cable issue.

**Solution:**
1. Use USB 3.0 port (blue connector)
2. Use quality USB 3.0 cable
3. Close other USB-intensive applications
4. Disable USB power saving:
   - Windows: Device Manager → USB Root Hub → Power Management → Uncheck "Allow computer to turn off..."

### App UI laggy

**Cause:** Insufficient system resources.

**Solution:**
- Close unnecessary applications
- Ensure 8GB+ RAM available
- Use SSD for temporary files
- Disable animations in Settings

---

## Getting Help

If issue persists:

1. **Check logs:**
   - Desktop: `~/.config/DeepEyeUnlocker/logs/`
   - Android: `Settings → About → Export Logs`

2. **Create issue on GitHub:**
   - Include OS version
   - Include app version
   - Include device model
   - Include steps to reproduce
   - Attach logs (sanitized)

3. **Community support:**
   - [GitHub Discussions](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/discussions)
   - [Telegram Community](https://t.me/DeepEyeCrypto)
