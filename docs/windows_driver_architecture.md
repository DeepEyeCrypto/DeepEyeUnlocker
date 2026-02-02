# DeepEyeUnlocker Driver Architecture Analysis

## 1.1 Chimera-Class vs Generic Tools

### Chimera's Superior Approach

Chimera Tool utilizes the **Windows SetupAPI** to perform registry-based installations rather than simple file copies. This ensures the driver is registered in the Windows Driver Store (`C:\Windows\System32\DriverStore\FileRepository`), making it visible to the Plug and Play (PnP) Manager.

Key architectural benefits of Chimera:

- **Registry-based installation**: Uses `Setupapi.InstallInf` for proper registration.
- **LibUSB filter integration**: Installs `libusb-win32` as an upper filter to prevent MTK BROM disconnects.
- **Smart architecture detection**: Native detection of 64-bit kernels even when running from a 32-bit wrapper (WOW64).

### Generic Tools' Mistakes

Generic tools often fail by:

1. **File copy only**: Copying `.sys` files manually to `drivers/` without registration.
2. **Architecture mismatch**: Attempting to load 32-bit drivers on 64-bit kernels.
3. **No conflict resolution**: Failing to remove old, conflicting drivers (e.g., legacy Miracle Box drivers).

---

## 2.1 The BROM Disconnect Problem Analysis

### Problem Description

1. MTK BootROM mode appears on the USB bus.
2. Windows attempt to load the default USB stack.
3. The BROM interface doesn't respond to standard USB descriptors in the way Windows expects.
4. Windows marks the device as "Failed" and cuts power/disconnects.

### The Filter Driver Solution

By installing **LibUSB-win32** as a **Filter Driver**, we intercept USB packets before they reach the Windows kernel's default handler. This "freezes" the device in BROM mode and allows DeepEyeUnlocker to communicate directly with the chipset via user-mode bulk transfers.

---

## 3.0 Driver Manager v4.0 Implementation Strategy

### Smart Driver Installer

- **Validation**: Verify digital signatures before installation.
- **Backup**: Automatically backup existing registry nodes before modification.
- **Cleanup**: Detect and purge known conflicting "Miracle" and "Generic VCOM" drivers.

### LibUSB Filter Registry Map

We target the following VID/PIDs for mandatory filtering:

- `0x0E8D`: `0x0003` (BROM), `0x2000` (Preloader)
- `0x05C6`: `0x9008` (Qualcomm EDL)
- `0x1782`: `0x4D00` (SPD FDL)
