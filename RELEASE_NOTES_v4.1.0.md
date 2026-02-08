# DeepEyeUnlocker v4.1.0 - The "Deep Research" Update

**Release Date:** 2026-02-08
**Codename:** Antigravity

This massive update introduces support for **3,758 devices** across all major brands (Samsung, Xiaomi, Vivo, Oppo, OnePlus, Realme, Tecno, Infinix, etc.) based on deep research data.

## 🚀 New Features

### 1. Massive Device Database
- Added support for **3,758 models** (imported from comprehensive CSV research).
- Includes Flags: FRP Bypass, Screen Lock Reset, Knox Guard Check (Flagships).
- Full coverage for brands: Samsung, Xiaomi, Vivo, Oppo, OnePlus, Realme, Tecno, Infinix, Motorola, Nokia, Sony, Google Pixel, Huawei/Honor.

### 2. Samsung QR Code Bypass (One UI 5.x/6.x)
- New **QR Code Generator** for bypassing Setup Wizard.
- Forces "Device Owner Provisioning" flow to skip Google Account setup.
- Instructions included in CLI output.

### 3. Xiaomi Mi Account Reset
- Added **ADB/Sideload Method** to disable Mi Account services.
- Disables `com.xiaomi.finddevice` and `com.miui.cloudservice`.
- Prevents relock on reboot (OTA updates must be avoided).

### 4. CLI Enhancements
- New `models import` command to load custom CSV databases.
- Improved device detection and capability listing.

## 🛠 Fixes & Improvements
- Fixed database path resolution in CLI.
- Updated `SupportedModel` schema to include "Marketing Name" and "Chipset Family".
- Default capabilities now auto-assigned based on device type (Flagship vs Budget).

## 📦 How to Install & Run

1. **Clone the Repo:**
   ```bash
   git clone https://github.com/DeepEyeCrypto/DeepEyeUnlocker.git
   cd DeepEyeUnlocker
   ```

2. **Restore Dependencies:**
   ```bash
   dotnet restore
   ```

3. **Import Database (First Run):**
   ```bash
   python3 scripts/import_models.py
   ```
   *(Or use the CLI command if compiled)*

4. **Run the Tool:**
   ```bash
   dotnet run --project src/CLI/DeepEyeUnlocker.CLI.csproj -- models list
   ```

## ⚠️ Disclaimer
This tool is for educational and authorized repair purposes only. Do not use on stolen devices. bypassing FRP/Mi Account may void warranties.
