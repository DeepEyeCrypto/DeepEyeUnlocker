# Troubleshooting Guide - DeepEyeUnlocker

## 1. Device Not Detected

- **Issue:** The device is plugged in but doesn't appear in the list after clicking "Refresh".
- **Solution:**
  - Ensure you are using a high-quality data cable.
  - Install the required USB drivers:
    - **Qualcomm:** QDLoader HS-USB 9008 driver.
    - **MediaTek:** MTK VCOM/Preloader driver.
    - **Samsung:** Samsung USB Driver for Mobile Phones.
  - Check Device Manager (Windows) to see if the device is listed under "Ports (COM & LPT)" or "Universal Serial Bus devices".

## 2. "Access Denied" Error

- **Issue:** The tool fails to open the USB port.
- **Solution:**
  - Right-click `DeepEyeUnlocker.exe` and select **Run as Administrator**.
  - Ensure no other tool (like QFIL, MiFlash, or Odin) is currently using the port.

## 3. Operation Stuck at X%

- **Issue:** The progress bar stops moving during a flash or format.
- **Solution:**
  - Do **NOT** unplug the device immediately if it's a flash operation.
  - Check the `logs/` folder for the specific error code.
  - Try a different USB port (preferably USB 2.0 instead of 3.0/3.1).

## 4. Device Boots to "Fastboot" or "Recovery" after Flash

- **Issue:** The flash completed, but the device won't boot to System.
- **Solution:**
  - Perform a **Format / Factory Reset** using the tool.
  - Ensure you flashed the correct firmware version for your specific model variant.
