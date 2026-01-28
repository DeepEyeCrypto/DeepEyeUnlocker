# DeepEyeUnlocker v1.0 User Manual

## 📘 Introduction

DeepEyeUnlocker is a one-stop tool for mobile device repair, supporting Qualcomm, MediaTek, and Samsung devices.

## ⚙️ Initial Setup

1. **Driver Installation**: Go to the `/scripts` folder and run `setup-dev.ps1` if you are a developer, or install drivers from the `Drivers/` directory.
2. **Launch**: Open `DeepEyeUnlocker.exe` as Administrator.
3. **Check Connection**: Look at the top-right corner of the tool. You should see Green dots (●) for installed drivers.

## 📱 How to Connect Your Device

- **Qualcomm**: Hold `Volume Up + Volume Down` and connect USB to enter EDL mode (9008).
- **MediaTek**: Device must be Power Off. Connect USB. Some models require holding `Volume Down`.
- **Samsung**: Hold `Volume Down + Bixby + Power` and select "Connect to PC" (Odin/Download Mode).

## 🛠 Operation Guide

### 🔓 FRP Bypass

Removes Google Account Lock.

1. Connect device in correct mode.
2. Click **FRP Bypass**.
3. Wait for success message.

### 🧹 Format / Factory Reset

Clears Pattern, PIN, and User Data.

1. Connect device.
2. Click **Format**.
3. Device will reboot automatically.

### 💾 Backup

Extracts current firmware to your PC.

1. Go to **Backup**.
2. Select destination folder.
3. Wait for progress to reach 100%.

## ❓ Frequently Asked Questions

- **Q: Tool stuck at "Establishing connection"?**
  - A: Check your cable and ensure drivers are green (●).
- **Q: Is it safe for my data?**
  - A: "Pattern Clear" tries to keep data, but most other operations (Format, Flash) wipe the device.

---
*Developed by DeepEye Community.*
