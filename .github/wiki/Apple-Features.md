# Apple Features

Complete guide to iOS device operations in DeepEye Unlocker.

---

## Feature Overview

| Feature | Mode Required | Device Support | Risk Level |
|---------|--------------|----------------|------------|
| FMI Check | Normal | iPhone, iPad, iPod | None |
| SHSH Blob Fetch | Normal/Recovery | All iOS devices | Low |
| Recovery Mode | Recovery | All iOS devices | Low |
| DFU Mode | DFU | All iOS devices | Medium |
| IPSW Restore | Recovery/DFU | All iOS devices | High |
| iCloud Backup | Normal | iPhone, iPad | Low |

---

## Apple Device Modes

DeepEye Unlocker detects and works with Apple devices in multiple modes:

| Mode | VID:PID | Description |
|------|---------|-------------|
| **Normal** | 0x05AC:xxxx | Regular iOS operation |
| **Recovery** | 0x05AC:1281 | Recovery mode (iTunes logo) |
| **DFU** | 0x05AC:1227 | Device Firmware Update mode |
| **WTF** | 0x05AC:1228 | Wrong firmware mode (rare) |
| **Pwned DFU** | 0x05AC:1227 | Exploited DFU (checkm8) |

---

## FMI Check (Find My iPhone)

Check the Find My iPhone / Activation Lock status of any iOS device.

### Prerequisites
- Device in Normal mode (booted to iOS)
- USB connection
- Trust this computer (tap "Trust" on device)

### Steps

1. **Connect Device:**
   - Use genuine Apple USB cable
   - Connect to computer
   - Tap "Trust" on device if prompted

2. **Select FMI Check:**
   - Open DeepEye Unlocker
   - Device appears in device bar
   - Click "FMI Check" from Apple features

3. **View Results:**
   ```json
   {
     "activation": {
       "find_my_iphone": true,
       "activation_lock": true,
       "device_locator": true,
       "lost_mode": false
     },
     "device": {
       "serial": "ABC123456789",
       "imei": "35xxxxxxxxxxxx",
       "model": "iPhone14,2",
       "ios_version": "16.5"
     }
   }
   ```

### Understanding Results

| Status | Meaning |
|--------|---------|
| **FMI: ON** | Device is linked to iCloud account |
| **FMI: OFF** | No iCloud account linked |
| **Lost Mode** | Device marked as lost by owner |
| **Clean** | No activation lock |

---

## SHSH Blob Fetch

Save SHSH blobs for future downgrades or restores.

### What are SHSH Blobs?

SHSH blobs are Apple's digital signatures for iOS firmware. Saving them allows:
- Future restores to unsigned iOS versions
- Downgrades (with compatible tools)
- Future jailbreak preservation

### Prerequisites
- Device connected (Normal or Recovery mode)
- Internet connection (for TSS request)

### Steps

1. **Connect Device**
2. **Select SHSH Fetch:**
   - Click "SHSH Blob Fetch" from features

3. **Auto-Detection:**
   - Tool reads ECID (Exclusive Chip ID)
   - Reads current iOS version
   - Reads board configuration (apnonce for A12+)

4. **Fetch Blobs:**
   - Tool contacts Apple TSS server
   - Requests signatures for:
     - Current signed iOS versions
     - Beta versions (if any)
   - Saves `.shsh2` files locally

### Saved Blob Format

```
[ECID]_iPhone[MODEL]_[iOS_VERSION]-[BUILD]_ [BOARDCONFIG].shsh2

Example:
1234567890_iPhone14,2_16.5-20F66_D63AP.shsh2
```

### Blob Storage

Blobs are saved to:
- **macOS:** `~/Library/Application Support/DeepEyeUnlocker/SHSH/`
- **Windows:** `%APPDATA%\DeepEyeUnlocker\SHSH\`
- **Linux:** `~/.local/share/DeepEyeUnlocker/SHSH/`

---

## Recovery Mode

Enter or exit Recovery mode for restores and diagnostics.

### Enter Recovery Mode

**Automatic (via DeepEye):**
1. Connect device in Normal mode
2. Click "Enter Recovery Mode"
3. Device reboots to Recovery (iTunes logo)

**Manual Button Sequence:**

| Device | Button Combination |
|--------|-------------------|
| iPhone 8+ / X+ | Vol+ → Vol- → Hold Side button |
| iPhone 7/7+ | Hold Vol- + Side button |
| iPhone 6s / SE | Hold Home + Side/Top button |
| iPad (Face ID) | Vol+ → Vol- → Hold Top button |
| iPad (Home) | Hold Home + Top button |

### Exit Recovery Mode

**Via DeepEye:**
1. Device connected in Recovery mode
2. Click "Exit Recovery Mode"
3. Device reboots normally

**Manual:**
- Force restart using button combination above

### Recovery Mode Commands

Once in Recovery mode, you can:

| Command | Description |
|---------|-------------|
| `irecovery -q` | Query device info |
| `irecovery -s` | Enter shell mode |
| `irecovery -f file` | Send file to device |
| `irecovery -c "command"` | Send command |

Common commands:
```bash
# Set auto-boot
irecovery -c "setenv auto-boot true"
irecovery -c "saveenv"

# Reboot
irecovery -c "reboot"
```

---

## DFU Mode

Device Firmware Update mode — lowest-level restore mode.

### DFU vs Recovery

| Feature | Recovery | DFU |
|---------|----------|-----|
| Screen | iTunes logo | Black |
| Bootloader | iBoot | SecureROM |
| Restores | Standard | All firmware |
| Exploits | No | checkm8 (A11-) |

### Enter DFU Mode

**Automatic (via DeepEye):**
1. Enter Recovery mode first
2. Click "Enter DFU Mode"
3. Tool sends `go` command via iRecovery

**Manual Button Sequence:**

**iPhone 8 / X / XS / XR / 11 / 12 / 13 / 14:**
1. Connect to computer
2. Vol+ (quick press)
3. Vol- (quick press)
4. Hold Side button (screen goes black)
5. Continue holding Side + press Vol- (5 seconds)
6. Release Side button, keep holding Vol- (10 seconds)
7. Screen stays black = DFU mode

**iPhone 7 / 7 Plus:**
1. Hold Side + Vol- (8 seconds)
2. Release Side, keep Vol- (10 seconds)
3. Screen stays black = DFU mode

**iPhone 6s / SE / iPad (Home):**
1. Hold Home + Side/Top (8 seconds)
2. Release Side/Top, keep Home (10 seconds)
3. Screen stays black = DFU mode

### Exit DFU Mode

**Via DeepEye:**
- Click "Exit DFU Mode"

**Manual:**
- Force restart device

### DFU Mode Detection

- **Screen:** Completely black (not just off)
- **iTunes/Finder:** Shows "recovery mode" device
- **DeepEye:** Shows "DFU Mode" indicator
- **System:** VID 0x05AC, PID 0x1227

---

## IPSW Restore

Restore iOS device using IPSW firmware file.

### Prerequisites
- Device in Recovery or DFU mode
- IPSW file for exact device model
- Sufficient disk space (~10GB)

### Steps

1. **Download IPSW:**
   - Get from [ipsw.me](https://ipsw.me) or Apple
   - Must match device model exactly

2. **Enter Recovery/DFU:**
   - DFU recommended for complete restore
   - Recovery works for standard restores

3. **Select IPSW Restore:**
   - Click "IPSW Restore" in DeepEye
   - Select downloaded `.ipsw` file

4. **Restore Options:**
   | Option | Description |
   |--------|-------------|
   | Standard | Normal restore (keeps baseband) |
   | Erase All | Clean restore (wipes data) |
   | Preserve Data | Update without erase |

5. **Start Restore:**
   - Tool extracts IPSW
   - Sends to device
   - Device shows progress bar
   - Reboots when complete

### Restore Process

```
Extracting IPSW...        [████████░░] 80%
Sending iBSS...           [██████████] 100%
Sending iBEC...           [██████████] 100%
Sending filesystem...     [██████░░░░] 60%
Verifying restore...      [██████████] 100%
Rebooting...              [██████████] 100%
```

### Troubleshooting Restores

| Issue | Solution |
|-------|----------|
| Error 3194 | Hosts file blocking Apple, use standard restore |
| Error 4013 | Bad cable or USB port, try different cable |
| Error 9 | Hardware issue, try DFU mode |
| Stuck on Apple logo | Force restart, retry restore |

---

## iCloud Backup Operations

Browse and extract data from iCloud backups.

### Prerequisites
- Apple ID credentials
- Two-factor authentication (if enabled)
- Internet connection

### Features

| Feature | Description |
|---------|-------------|
| List Backups | Show all iCloud backups for account |
| Download Backup | Download full backup to local storage |
| Browse Contents | View files within backup |
| Extract Data | Export specific data types |

### Supported Data Types

- Photos and videos
- Messages (iMessage, SMS)
- Contacts
- Call history
- App data
- Keychain (with credentials)
- Safari history
- Notes

### Security Notes

- Credentials are not stored
- 2FA codes required per session
- Data is decrypted locally
- No data sent to external servers

---

## Checkm8 Exploit (A11 and earlier)

Low-level bootrom exploit for compatible devices.

### Supported Devices

| Chip | Devices |
|------|---------|
| A5 | iPhone 4S, iPad 2/3, iPod 5G |
| A6 | iPhone 5/5C |
| A7 | iPhone 5S, iPad Air, Mini 2 |
| A8 | iPhone 6/6+, iPad Air 2, Mini 3/4 |
| A9 | iPhone 6S/6S+, SE (1st), iPad 5 |
| A10 | iPhone 7/7+, iPad 6/7 |
| A11 | iPhone 8/8+/X |

### Using Checkm8

1. **Enter DFU Mode**
2. **Select Checkm8:**
   - Click "Checkm8 Exploit"
   - Tool detects compatible device

3. **Run Exploit:**
   - Tool sends USB payload
   - Device enters Pwned DFU
   - Allows unsigned code execution

### Applications

- Verbose boot
- Custom boot logos
- Downgrades (with SHSH)
- Jailbreak installation

---

## Safety Guidelines

### Before Operations
- ✅ Backup device via iTunes/Finder
- ✅ Save SHSH blobs
- ✅ Verify IPSW matches device model
- ✅ Ensure stable USB connection

### During Restores
- ❌ Never disconnect USB during restore
- ❌ Never interrupt DFU sequence
- ❌ Never flash wrong firmware

### Recovery
If device is stuck:
1. Force restart (button combination)
2. Re-enter Recovery/DFU
3. Restore IPSW again

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Device not detected | Use genuine Apple cable, try different port |
| Trust dialog not appearing | Reconnect, try different cable |
| DFU not entering | Practice timing, use stopwatch |
| Restore fails | Download fresh IPSW, check model |
| Stuck in Recovery | Use "Exit Recovery" or force restart |

See [Troubleshooting](Troubleshooting.md) for more.
