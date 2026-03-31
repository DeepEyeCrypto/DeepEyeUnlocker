# Installation Guide

Complete setup instructions for all supported platforms.

---

## macOS

### Download
1. Go to [Releases](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest)
2. Download `DeepEyeUnlocker_v[VERSION].dmg` (Universal build for Intel + Apple Silicon)

### Install
1. Open the downloaded `.dmg` file
2. Drag **DeepEye Unlocker** to the **Applications** folder
3. Eject the DMG

### Gatekeeper Workaround (First Launch)
macOS may show "cannot be opened because the developer cannot be verified":

```bash
# Option 1: Right-click method (Recommended)
# Right-click the app → Click "Open" → Click "Open" in dialog

# Option 2: Terminal method
xattr -cr /Applications/DeepEye\ Unlocker.app
```

### System Requirements
- macOS 11.0 (Big Sur) or later
- USB 3.0 port recommended
- 8GB RAM minimum (16GB+ for forensic operations)

---

## Windows

### Download
1. Go to [Releases](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest)
2. Download `DeepEyeUnlocker_[VERSION]_x64-setup.exe` (NSIS installer)
   - Or download `.msi` for enterprise deployment

### Install
1. Run the installer
2. Follow the setup wizard
3. Choose install location (default: `C:\Program Files\DeepEye Unlocker`)
4. Complete installation

### USB Drivers
Windows requires device-specific USB drivers:

| Manufacturer | Driver Package |
|--------------|----------------|
| Samsung | Samsung USB Driver / Odin |
| Xiaomi | MiFlash Pro drivers |
| OnePlus | OnePlus USB Driver |
| Google | Google USB Driver (SDK) |
| Qualcomm | QDLoader 9008 Driver |
| MediaTek | MTK USB Driver |

Download drivers from manufacturer websites or use [Universal ADB Driver](https://adb.clockworkmod.com/).

### System Requirements
- Windows 10 or Windows 11
- 64-bit processor
- USB 3.0 port recommended
- 8GB RAM minimum

---

## Linux

### AppImage (Recommended)

```bash
# Download
wget https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest/download/DeepEyeUnlocker.AppImage

# Make executable
chmod +x DeepEyeUnlocker.AppImage

# Run
./DeepEyeUnlocker.AppImage
```

### Debian/Ubuntu (.deb)

```bash
# Download
wget https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest/download/DeepEyeUnlocker_amd64.deb

# Install
sudo dpkg -i DeepEyeUnlocker_amd64.deb
sudo apt-get install -f  # Fix dependencies if needed

# Run
deepeye-unlocker
```

### USB Permissions (Linux)
Add udev rules for USB device access:

```bash
# Create udev rule
sudo tee /etc/udev/rules.d/51-deepeye.rules << 'EOF'
# Qualcomm EDL (9008)
SUBSYSTEM=="usb", ATTR{idVendor}=="05c6", ATTR{idProduct}=="9008", MODE="0666", GROUP="plugdev"
# MediaTek BROM
SUBSYSTEM=="usb", ATTR{idVendor}=="0e8d", MODE="0666", GROUP="plugdev"
# Samsung Odin/Download
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
# Apple devices
SUBSYSTEM=="usb", ATTR{idVendor}=="05ac", MODE="0666", GROUP="plugdev"
# Fastboot/ADB
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
EOF

# Reload rules
sudo udevadm control --reload-rules
sudo udevadm trigger

# Add user to plugdev group
sudo usermod -a -G plugdev $USER
# Log out and back in for group changes to take effect
```

### System Requirements
- Ubuntu 20.04+ / Debian 11+ / Fedora 35+
- glibc 2.31+
- USB 3.0 port recommended

---

## Android

### Download
1. Go to [Releases](https://github.com/DeepEyeCrypto/DeepEyeUnlocker/releases/latest)
2. Download `DeepEyeUnlocker-v[VERSION]-universal.apk`

### Enable Unknown Sources

**Android 8.0 - 10:**
1. Settings → Security
2. Enable "Unknown Sources"
3. Tap APK to install

**Android 11+:**
1. Download APK
2. Tap the downloaded file
3. When prompted, tap "Settings"
4. Enable "Allow from this source"
5. Return and complete installation

### OTG Requirements
- Host device must support USB OTG
- Use quality USB OTG cable/adapter
- Enable USB debugging on target device (see below)

### System Requirements
- Android 8.0+ (API 26+)
- USB OTG support
- 4GB RAM minimum
- ARM64 or ARMv7 processor

---

## USB Setup & Drivers

### Enable USB Debugging (Android Target Device)

1. **Enable Developer Options:**
   - Settings → About Phone
   - Tap "Build Number" 7 times
   - Enter PIN/password when prompted

2. **Enable USB Debugging:**
   - Settings → System → Developer Options
   - Toggle "USB Debugging" ON
   - Confirm the RSA fingerprint dialog when connecting

### ADB Setup (Desktop)

```bash
# Verify ADB installation
adb version

# List connected devices
adb devices

# Expected output:
# List of devices attached
# xxxxxxxx    device
```

### Fastboot Setup

```bash
# Reboot to bootloader
adb reboot bootloader

# Verify fastboot connection
fastboot devices

# Expected output:
# xxxxxxxx    fastboot
```

---

## First Device Connection

### Android Device (via Desktop App)

1. **Connect USB cable** to target device
2. **Allow USB debugging** on device (RSA key dialog)
3. **Launch DeepEye Unlocker** desktop app
4. **Wait for detection** — Device should appear in device bar
5. **Select operation** from feature catalog

### Android-to-Android (via Android App)

1. **Install DeepEye Unlocker** on host device (with OTG)
2. **Connect OTG cable** to host
3. **Connect USB cable** from OTG to target device
4. **Allow USB debugging** on target device
5. **Grant USB permission** in DeepEye app when prompted

### Apple Device Connection

1. **Connect USB cable** (use genuine Apple cable)
2. **Trust this computer** on iPhone/iPad
3. **Launch DeepEye Unlocker**
4. Device appears in Normal mode

For Recovery/DFU mode operations, see [Apple Features](Apple-Features.md).

---

## Verification

### Desktop App
```bash
# Check version
deepeye --version

# Verify bundled tools
deepeye doctor
```

### Android App
- Open app → Settings → About
- Verify version matches release

---

## Next Steps

- [Android Features](Android-Features.md) — FRP Bypass, Bootloader Unlock, EDL
- [Apple Features](Apple-Features.md) — Recovery, DFU, SHSH Blobs
- [Troubleshooting](Troubleshooting.md) — Common issues
