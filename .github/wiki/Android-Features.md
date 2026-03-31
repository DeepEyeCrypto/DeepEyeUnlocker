# Android Features

Complete guide to Android device operations in DeepEye Unlocker.

---

## Feature Overview

| Feature | Mode Required | SoC Support | Risk Level |
|---------|--------------|-------------|------------|
| FRP Bypass | EDL / Fastboot / ADB | Qualcomm, MTK, Samsung | Medium |
| Bootloader Unlock | Fastboot | All | Low |
| EDL Mode (9008) | EDL | Qualcomm only | Medium |
| ROM Flasher | EDL / Fastboot / Odin | Qualcomm, MTK, Samsung | High |
| Reboot Controls | ADB / Fastboot | All | Low |
| Device Info Dump | ADB / MTP | All | None |

---

## FRP Bypass

Factory Reset Protection (FRP) is a Google security feature that requires the previous Google account credentials after a factory reset.

### Bypass Paths

DeepEye Unlocker supports multiple FRP bypass methods:

| Path | Requirements | Success Rate |
|------|-------------|--------------|
| **EDL + Firehose** | Qualcomm device, EDL access | High |
| **ADB Sideload** | Debug build, ADB enabled | Medium |
| **CVE Exploit** | Specific Android versions | Device-specific |
| **Fastboot OEM** | Unlocked bootloader | High |

### EDL Firehose Method (Qualcomm)

**Prerequisites:**
- Qualcomm Snapdragon device
- EDL mode access (button combo or ADB)
- Firehose programmer for device

**Steps:**

1. **Enter EDL Mode:**
   ```bash
   # Via ADB (if device boots)
   adb reboot edl
   
   # Or use button combination (device-specific)
   # Typically: Vol+ + Vol- + Power (hold 10-15 seconds)
   ```

2. **Verify EDL Connection:**
   - Device appears with PID `9008` in DeepEye
   - USB descriptor shows "Qualcomm EDL"

3. **Select FRP Bypass:**
   - Open DeepEye Unlocker
   - Select device from device bar
   - Click "FRP Bypass" from feature catalog

4. **Execute:**
   - Tool performs SAHARA handshake
   - Sends Firehose programmer
   - Erases FRP partition
   - Reboots device

**Safety Rules:**
- Only erases `frp` partition — never userdata
- Uses partition name, not hardcoded sectors
- Waits for ACK before each step

### ADB Method (Debug Builds)

**Prerequisites:**
- Engineering or debug ROM
- ADB debugging enabled

**Steps:**
1. Connect device with ADB enabled
2. Select "FRP Bypass" → "ADB Path"
3. Tool sends intent to bypass setup wizard
4. Device reboots to unlocked state

### CVE Exploit Method

**Supported CVEs:**
- CVE-2022-20233 (Samsung, Google — Android 9-11)
- CVE-2023-21087 (Samsung — Android 10-12)

**Steps:**
1. Tool auto-detects applicable CVE
2. Sends exploit payload via ADB
3. Bypasses setup wizard

---

## Bootloader Unlock

Unlocking the bootloader allows flashing custom firmware and modifications.

### Prerequisites
- Device with unlockable bootloader
- USB debugging enabled
- OEM Unlocking enabled in Developer Options

### Steps

1. **Enable OEM Unlocking:**
   - Settings → Developer Options
   - Enable "OEM unlocking"
   - (May require SIM card and WiFi on some devices)

2. **Reboot to Bootloader:**
   ```bash
   adb reboot bootloader
   ```

3. **Select Bootloader Unlock:**
   - In DeepEye, select device
   - Click "Bootloader Unlock"

4. **Confirm on Device:**
   - Use volume keys to select "Unlock"
   - Press power to confirm
   - Device will wipe data and reboot

### Brand-Specific Notes

| Brand | Unlock Method | Wipes Data |
|-------|--------------|------------|
| **Google Pixel** | `fastboot flashing unlock` | Yes |
| **OnePlus** | `fastboot oem unlock` | Yes |
| **Xiaomi** | Mi Unlock Tool + wait time | Yes |
| **Samsung** | OEM toggle + `fastboot` | Yes |
| **Motorola** | Motorola unlock code | Yes |
| **Samsung Knox** | Not unlockable | N/A |

---

## EDL Mode (9008)

Emergency Download Mode is a Qualcomm-specific low-level flashing mode.

### Entering EDL Mode

**Method 1: ADB (if device boots)**
```bash
adb reboot edl
```

**Method 2: Fastboot**
```bash
fastboot oem edl
```

**Method 3: Button Combination (Device-Specific)**

| Device Type | Button Combination |
|-------------|-------------------|
| Most Qualcomm | Vol+ + Vol- + Power (hold 10-15s) |
| Xiaomi | Vol+ + Vol- + Power (EDL cable helps) |
| Samsung | Vol+ + Vol- + Power (some models) |
| OnePlus | Vol+ + Vol- + Power |

**Method 4: EDL Cable (Deep Flash)**
- Special USB cable with shorted D+/D-
- Connect while holding Vol+ or Vol-

### EDL Mode Indicators

- **Windows:** Device Manager shows "Qualcomm HS-USB QDLoader 9008"
- **Linux:** `lsusb` shows `05c6:9008`
- **DeepEye:** Device bar shows "EDL Mode" with purple indicator

### EDL Operations

Once in EDL mode, you can:

| Operation | Description |
|-----------|-------------|
| Flash Firmware | Write full or partial firmware |
| Erase Partitions | Wipe specific partitions |
| Read/Backup | Dump partitions for backup |
| FRP Bypass | Erase FRP partition |
| IMEI Repair | Write NV data (advanced) |

### SAHARA Protocol

EDL uses the SAHARA protocol for communication:

1. **Hello:** Tool sends SAHARA_HELLO_REQ
2. **Response:** Device sends device info + MSM ID
3. **Programmer:** Tool sends Firehose programmer
4. **Execution:** Programmer handles flash operations

---

## ROM Flasher

Flash custom or stock firmware to Android devices.

### Supported Formats

| Format | Extension | Use Case |
|--------|-----------|----------|
| Fastboot Images | `.img` | Individual partitions |
| Sparse Images | `.img` | Android sparse format |
| Samsung Tar | `.tar`, `.tar.md5` | Odin flashable |
| Qualcomm ELF | `.elf`, `.mbn` | EDL programmers |
| ZIP Packages | `.zip` | Recovery flashable |

### Fastboot Flashing

**Prerequisites:**
- Unlocked bootloader
- Device in fastboot mode

**Steps:**
1. Reboot to fastboot: `adb reboot bootloader`
2. Select "ROM Flasher" in DeepEye
3. Choose firmware package or individual images
4. Select partitions to flash (boot, system, vendor, etc.)
5. Click "Flash" and wait for completion

**Individual Partition Flash:**
```bash
# Example fastboot commands used by tool
fastboot flash boot boot.img
fastboot flash system system.img
fastboot flash vendor vendor.img
fastboot reboot
```

### EDL Flashing (Qualcomm)

**Prerequisites:**
- Device in EDL mode (9008)
- RawProgram XML + patch0.xml
- Firehose programmer (.elf/.mbn)

**Steps:**
1. Enter EDL mode
2. Select "ROM Flasher" → "EDL Mode"
3. Load programmer file
4. Load rawprogram XML
5. Click "Flash"

### Samsung Odin Flashing

**Prerequisites:**
- Device in Download mode
- Odin-compatible .tar or .tar.md5 file

**Enter Download Mode:**
1. Power off device
2. Hold Vol+ + Vol- + Power
3. Press Vol+ to confirm

**Steps:**
1. Connect in Download mode
2. Select "ROM Flasher" → "Odin Mode"
3. Load tar.md5 firmware
4. Click "Flash"

---

## Reboot Controls

Quick reboot to different modes.

| Command | Result |
|---------|--------|
| Reboot System | Normal reboot to Android |
| Reboot Recovery | Reboot to recovery mode |
| Reboot Bootloader | Reboot to fastboot/download |
| Reboot EDL | Reboot to EDL mode (Qualcomm) |
| Power Off | Shutdown device |

**Usage:**
1. Select device in DeepEye
2. Click "Reboot Controls"
3. Select target mode
4. Confirm

---

## Device Info Dump

Extract comprehensive device information.

### Information Collected

```json
{
  "device": {
    "model": "SM-G991B",
    "brand": "samsung",
    "manufacturer": "Samsung",
    "board": "exynos2100",
    "bootloader": "G991BXXU3AUK7",
    "fingerprint": "samsung/p3sxxx/p3s:12/SP1A..."
  },
  "software": {
    "android_version": "12",
    "api_level": 31,
    "security_patch": "2022-01-01",
    "build_id": "SP1A.210812.016"
  },
  "hardware": {
    "soc": "Exynos 2100",
    "ram": "8192MB",
    "storage": "256GB",
    "battery": "4000mAh"
  },
  "security": {
    "bootloader_locked": true,
    "verity_enabled": true,
    "encryption": "file-based"
  }
}
```

**Steps:**
1. Connect device with ADB enabled
2. Select "Device Info Dump"
3. Choose output format (JSON, TXT, or PDF)
4. Save report

---

## Safety Guidelines

### Before Any Operation
- ✅ Verify device model matches target
- ✅ Backup important data
- ✅ Ensure sufficient battery (>50%)
- ✅ Use quality USB cable

### During Operations
- ❌ Never disconnect USB during flash
- ❌ Never interrupt EDL handshake
- ❌ Never flash incompatible firmware

### Recovery
If device becomes unresponsive:
1. Force reboot (hold Power 10+ seconds)
2. Re-enter bootloader/EDL mode
3. Re-flash stock firmware

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Device not detected | Check USB cable, enable USB debugging |
| EDL not entering | Try different button combo or EDL cable |
| Flash fails | Verify firmware matches device model |
| Bootloop | Re-flash stock firmware |
| FRP still active | Try alternative bypass path |

See [Troubleshooting](Troubleshooting.md) for more.
