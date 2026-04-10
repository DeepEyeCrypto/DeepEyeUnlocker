# Device Protocol Integration - Usage Guide

## 🚀 Quick Start

### 1. Import the API

```typescript
import { 
  scanDevices,
  autoConnectDevice,
  getDeviceModeName,
  getDeviceModeColor,
  isFlashMode,
  isMtkDevice,
  isQualcommDevice,
  fastbootDetect,
  fastbootGetInfo,
  fastbootFlashPartition,
  fastbootErasePartition,
  fastbootReboot
} from '@/lib/device';
```

### 2. Detect Devices

```typescript
// Scan all USB devices
const result = await scanDevices();
console.log(`Found ${result.count} devices`);

if (result.hasSupported) {
  // Auto-connect to best available device
  const connected = await autoConnectDevice();
  console.log(`Connected: ${connected.message}`);
  console.log(`Protocol: ${connected.protocol}`);
}

// Check for specific mode
const hasFastboot = await checkDeviceMode('fastboot');
const hasEdl = await checkDeviceMode('edl');
const hasBrom = await checkDeviceMode('brom');
```

### 3. Use React Components

```tsx
import { DeviceDetector } from '@/components/DeviceDetector';
import { FastbootOperations } from '@/components/FastbootOperations';

function App() {
  return (
    <div>
      <DeviceDetector 
        autoScan={true}
        scanInterval={3000}
        onDeviceConnected={(device) => {
          console.log('Device connected:', device);
        }}
      />
      
      <FastbootOperations 
        onProgress={(msg) => console.log('[Progress]', msg)}
      />
    </div>
  );
}
```

---

## 📱 Device Detection

### Basic Usage

```typescript
import { scanDevices, DeviceScanResult } from '@/lib/device';

const result: DeviceScanResult = await scanDevices();

// Result structure:
// {
//   devices: [
//     {
//       connected: true,
//       device: {
//         mode: 'Brom',
//         vid: 0x0E8D,
//         pid: 0x0003,
//         serial: 'ABC123',
//         bus: 20,
//         address: 5,
//         chipset: null,
//         detectedAt: 1234567890
//       },
//       protocol: 'MtkBrom',
//       message: 'MediaTek BROM [ABC123]'
//     }
//   ],
//   count: 1,
//   hasSupported: true
// }
```

### Auto-Connect

```typescript
import { autoConnectDevice } from '@/lib/device';

const device = await autoConnectDevice();

if (device.connected) {
  console.log('Mode:', device.device.mode);
  console.log('Protocol:', device.protocol);
  console.log('Info:', device.message);
}
```

### Check Device Mode

```typescript
import { checkDeviceMode } from '@/lib/device';

// Check if specific mode is available
const isMtkConnected = await checkDeviceMode('brom');
const isEdlConnected = await checkDeviceMode('edl');
const isFastbootConnected = await checkDeviceMode('fastboot');
```

---

## ⚡ Fastboot Operations

### Detect Fastboot Device

```typescript
import { fastbootDetect, fastbootGetInfo } from '@/lib/device';

const hasFastboot = await fastbootDetect();

if (hasFastboot) {
  const info = await fastbootGetInfo();
  console.log('Serial:', info.serial);
  console.log('Product:', info.product);
  console.log('Bootloader:', info.bootloaderVersion);
  console.log('Unlocked:', info.unlocked);
}
```

### Flash Partition

```typescript
import { fastbootFlashPartition } from '@/lib/device';

try {
  await fastbootFlashPartition('boot', '/path/to/boot.img');
  console.log('✅ Boot partition flashed successfully');
} catch (error) {
  console.error('❌ Flash failed:', error);
}
```

### Erase Partition

```typescript
import { fastbootErasePartition } from '@/lib/device';

await fastbootErasePartition('cache');
await fastbootErasePartition('userdata');
```

### Reboot Options

```typescript
import { 
  fastbootReboot, 
  fastbootRebootBootloader, 
  fastbootRebootRecovery 
} from '@/lib/device';

// Reboot to system
await fastbootReboot();

// Reboot to bootloader
await fastbootRebootBootloader();

// Reboot to recovery
await fastbootRebootRecovery();
```

### Bootloader Unlock/Lock

```typescript
import { 
  fastbootUnlockBootloader, 
  fastbootLockBootloader 
} from '@/lib/device';

// Unlock (WARNING: Wipes all data!)
await fastbootUnlockBootloader();

// Lock
await fastbootLockBootloader();
```

---

## 🎨 UI Integration

### Device Detector Component

```tsx
import { DeviceDetector } from '@/components/DeviceDetector';

<DeviceDetector 
  autoScan={true}              // Auto-scan on mount
  scanInterval={3000}          // Scan every 3 seconds
  onDeviceConnected={(device) => {
    console.log('Connected:', device.message);
    // Enable protocol-specific operations
  }}
/>
```

### Fastboot Operations Component

```tsx
import { FastbootOperations } from '@/components/FastbootOperations';

<FastbootOperations 
  onProgress={(message) => {
    // Log or display progress
    addLog(message);
  }}
/>
```

---

## 🔧 Utility Functions

### Get Human-Readable Names

```typescript
import { getDeviceModeName, getDeviceModeColor } from '@/lib/device';

const modeName = getDeviceModeName(DeviceMode.Brom);
// Returns: "MediaTek BROM"

const modeColor = getDeviceModeColor(DeviceMode.Edl);
// Returns: "#9C27B0" (purple)
```

### Check Device Type

```typescript
import { isFlashMode, isMtkDevice, isQualcommDevice } from '@/lib/device';

if (isFlashMode(device.mode)) {
  // Device is in flash/download mode
  showFlashWarning();
}

if (isMtkDevice(device.mode)) {
  // Enable MTK-specific operations
  showMtkOperations();
}

if (isQualcommDevice(device.mode)) {
  // Enable Qualcomm EDL operations
  showEdlOperations();
}
```

---

## 📊 Protocol Support

### MediaTek BROM
- **VID/PID**: 0x0E8D:0x0003
- **Use**: Flash firmware, unlock bootloader, repair IMEI
- **Operations**: Handshake, SLA bypass, DA upload, partition flash

### MediaTek PreLoader
- **VID/PID**: 0x0E8D:0x2000, 0x0006
- **Use**: Pre-flash operations
- **Operations**: Similar to BROM but different entry point

### Qualcomm EDL
- **VID/PID**: 0x05C6:0x9008, 0x900E
- **Use**: Emergency download mode for Qualcomm devices
- **Operations**: Sahara handshake, Firehose programmer, partition operations

### Fastboot
- **VID/PID**: 0x18D1:0xD00D, 0x0E8D:0x0C01
- **Use**: Android bootloader protocol
- **Operations**: Flash, erase, reboot, unlock

### Samsung Odin
- **VID/PID**: 0x04E8:0x685D, 0x6860, 0x6861
- **Use**: Samsung download mode
- **Operations**: Flash firmware (PDA, CSC, PHONE, PIT)

### UniSoc FDL
- **VID/PID**: 0x1782:0x4D00
- **Use**: UniSoc flash download mode
- **Operations**: Flash firmware

---

## ⚠️ Important Notes

### USB Permissions

**macOS:**
- Ensure USB entitlements are configured
- May require running with sudo during development
- Check `Info.plist` for USB device access permissions

**Windows:**
- Install WinUSB driver using Zadig tool
- Select device in Zadig → Install WinUSB driver

**Linux:**
- Install udev rules: `sudo cp 99-deepeye.rules /etc/udev/rules.d/`
- Reload rules: `sudo udevadm control --reload-rules`
- Re-login or run: `sudo udevadm trigger`

### Error Handling

```typescript
try {
  await fastbootFlashPartition('boot', '/path/to/boot.img');
} catch (error) {
  if (error.includes('Device not found')) {
    showError('No fastboot device detected. Please connect device in fastboot mode.');
  } else if (error.includes('Timeout')) {
    showError('Operation timed out. Check USB connection.');
  } else {
    showError(`Operation failed: ${error}`);
  }
}
```

### Best Practices

1. **Always scan before operations**: Verify device is connected
2. **Check device mode**: Ensure correct mode for operation
3. **Handle errors gracefully**: Provide helpful error messages
4. **Show progress**: Use progress callbacks for long operations
5. **Confirm destructive operations**: Warn before erase/unlock

---

## 🧪 Testing

### Test Device Detection

```typescript
// Connect device in different modes and test:

// 1. MTK BROM mode
const mtkDevices = await scanDevices();
console.log('MTK devices:', mtkDevices.devices.filter(d => 
  d.device?.mode === 'Brom' || d.device?.mode === 'PreLoader'
));

// 2. Fastboot mode
const hasFastboot = await fastbootDetect();
console.log('Fastboot detected:', hasFastboot);

// 3. Auto-connect
const connected = await autoConnectDevice();
console.log('Auto-connected:', connected.message);
```

### Test Fastboot Operations

```typescript
// With device in fastboot mode:

// Get device info
const info = await fastbootGetInfo();
console.log('Device:', info.product, info.serial);

// Flash boot image
await fastbootFlashPartition('boot', '/path/to/boot.img');

// Erase cache
await fastbootErasePartition('cache');

// Reboot
await fastbootReboot();
```

---

## 📚 API Reference

### Core Functions

| Function | Returns | Description |
|----------|---------|-------------|
| `scanDevices()` | `DeviceScanResult` | Scan all USB devices |
| `autoConnectDevice()` | `DeviceConnectionStatus` | Auto-detect and connect |
| `checkDeviceMode(mode)` | `boolean` | Check if mode available |
| `getProtocolName(proto)` | `string` | Get protocol display name |

### Fastboot Functions

| Function | Returns | Description |
|----------|---------|-------------|
| `fastbootDetect()` | `boolean` | Detect fastboot device |
| `fastbootGetInfo()` | `FastbootDeviceInfo` | Get device information |
| `fastbootFlashPartition(part, path)` | `void` | Flash partition |
| `fastbootErasePartition(part)` | `void` | Erase partition |
| `fastbootReboot()` | `void` | Reboot to system |
| `fastbootRebootBootloader()` | `void` | Reboot to bootloader |
| `fastbootRebootRecovery()` | `void` | Reboot to recovery |
| `fastbootUnlockBootloader()` | `void` | Unlock bootloader |
| `fastbootLockBootloader()` | `void` | Lock bootloader |

### Utility Functions

| Function | Returns | Description |
|----------|---------|-------------|
| `getDeviceModeName(mode)` | `string` | Human-readable mode name |
| `getDeviceModeColor(mode)` | `string` | UI color for mode |
| `isFlashMode(mode)` | `boolean` | Check if flash mode |
| `isMtkDevice(mode)` | `boolean` | Check if MTK device |
| `isQualcommDevice(mode)` | `boolean` | Check if Qualcomm device |

---

## 🎯 Common Workflows

### Flash Custom ROM

```typescript
// 1. Detect device
const connected = await autoConnectDevice();
if (connected.protocol !== 'Fastboot') {
  throw new Error('Please connect device in fastboot mode');
}

// 2. Flash boot image
await fastbootFlashPartition('boot', '/path/to/boot.img');

// 3. Flash system image
await fastbootFlashPartition('system', '/path/to/system.img');

// 4. Flash vendor image
await fastbootFlashPartition('vendor', '/path/to/vendor.img');

// 5. Reboot
await fastbootReboot();
```

### Unlock Bootloader

```typescript
// 1. Verify device is in fastboot mode
const hasFastboot = await fastbootDetect();
if (!hasFastboot) {
  throw new Error('Device not in fastboot mode');
}

// 2. Get device info
const info = await fastbootGetInfo();
if (info.unlocked) {
  console.log('Bootloader already unlocked');
  return;
}

// 3. Confirm with user
const confirmed = confirm('This will wipe all data. Continue?');
if (!confirmed) return;

// 4. Unlock
await fastbootUnlockBootloader();
console.log('Bootloader unlocked successfully');

// 5. Reboot to bootloader
await fastbootRebootBootloader();
```

### Erase FRP (Factory Reset Protection)

```typescript
// 1. Connect device in fastboot mode
const connected = await autoConnectDevice();

// 2. Erase FRP partition
await fastbootErasePartition('frp');
await fastbootErasePartition('config');
await fastbootErasePartition('persistent');

// 3. Erase userdata (factory reset)
await fastbootErasePartition('userdata');
await fastbootErasePartition('cache');

// 4. Reboot
await fastbootReboot();
```

---

## 🐛 Troubleshooting

### "No devices found"
- Check USB cable connection
- Verify device is in correct mode
- Install proper USB drivers
- Check USB permissions (see USB Permissions section)

### "Operation timed out"
- Try different USB port
- Use high-quality USB cable
- Check if device disconnected during operation
- Retry operation

### "Access denied"
- **macOS**: Check USB entitlements, try with sudo
- **Windows**: Install WinUSB driver via Zadig
- **Linux**: Install udev rules and re-login

### "Flash failed"
- Verify image file matches device
- Check file path is correct
- Ensure sufficient battery (>50%)
- Try rebooting to bootloader and retry

---

## 📖 Examples

See the following files for complete examples:
- `src/components/DeviceDetector.tsx` - Device detection UI
- `src/components/FastbootOperations.tsx` - Fastboot operations UI
- `src/lib/device.ts` - Complete API implementation

---

**Happy Flashing! 🔧⚡**
