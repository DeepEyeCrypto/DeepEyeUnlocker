# DeepEyeUnlocker Project Manifest

## 💎 Project Identity

- **Name:** DeepEyeUnlocker
- **Version:** 1.1.0 "Gold"
- **Mission:** Democratizing mobile repair tools by providing a free, professional-grade open-source alternative to paid repair boxes.

## 🛠 Technical Stack

- **Core:** C# / .NET 6.0 (Windows Forms)
- **Protocols:** LibUsbDotNet (USB), Sahara/Firehose (Qualcomm), BROM/DA (MTK), Odin/Loke (Samsung)
- **CI/CD:** GitHub Actions (Auto-Build, Auto-Release)
- **Backend:** Node.js, Express, MongoDB
- **UI:** Custom Dark Mode, WinForms Themes, Hindi/English Localization
- **Installer:** WiX Toolset (MSI)

## 📂 Core Components

1. **Engines (`src/Protocols`)**: Hardware-level communication for the "Big Three" chipsets.
2. **Operations (`src/Operations`)**: FRP Bypass, Mi Account Bypass, Factory Reset, Flash, Backup, Bootloader Unlock.
3. **UI (`src/UI`)**: Dashboard with live driver diagnostics, progress tracking, and partition management.
4. **Cloud (`backend/`)**: Community analytics, firmware repository metadata.
5. **Cross-Platform (`scripts/linux`)**: Native Linux support with automated installation.

## 🚀 Accomplishments

- **Phase 1 Complete**: Full protocol handshakes for Qualcomm, MTK, and Samsung.
- **Bootloader Mastered**: Integrated EDL Auth Bypass, BROM Exploit, and E-Token methods for 100% brand coverage.
- **Brand Identity**: Premium Dark Teal (#224A47) ecosystem with AI-generated branding.
- **Global Ready**: Multi-language support (English/Hindi) and cross-platform (Linux/Windows).

---
**Status:** PRODUCTION READY.
**Authorized by:** DeepEye Community.
