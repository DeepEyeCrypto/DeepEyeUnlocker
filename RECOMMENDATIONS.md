# DeepEyeUnlocker - Strategic Research & Exploit Roadmap (2026)

## 🚀 Priority 1: MediaTek (MTK) Universal Auth Bypass
**Problem:** Modern MTK devices (Oppo, Vivo, Xiaomi) require "Secure Boot Auth" (SLA/DAA) to flash or unlock via BROM.
**Solution:** Implement the "Kamakiri" / "Bypass Utility" payload injection.
**Mechanism:**
1. Send specific "Watchdog" disable pattern via USB.
2. Exploit the BootROM to disable the "Security Configuration" register.
3. Allow any Download Agent (DA) to load without signature verification.
**Status:** **Must Implement immediately.** (File: `src/Protocols/MTK/AuthBypass.cs`)

## 🚀 Priority 2: Samsung MTP Execution (FRP Injection)
**Problem:** Users cannot access settings to reset phone.
**Solution:** MTP Launch Browser Command.
**Mechanism:**
1. Send USB Control Transfer `0x80` (Samsung Specific).
2. Inject URI payload (`https://www.youtube.com/`).
3. Phone prompts "View" -> Opens Chrome.
**Status:** **High Priority.** (File: `src/Protocols/Samsung/MtpBrowser.cs`)

## 🚀 Priority 3: Qualcomm Firehose Auto-Loader
**Problem:** EDL mode is useless without the exact `prog_firehose_ddr.elf` or `.mbn` file.
**Solution:** Cloud-based or Local Hash-Map Loader Database.
**Mechanism:**
1. Read Device HW_ID (PK_HASH) via Sahara.
2. Match HW_ID against a database of known Loaders.
3. Auto-select and inject the loader.
**Recommendation:** Create a `loaders/` directory and a JSON map `loaders/map.json`.

## 🛡️ Security & Stability
- **Driver Conflict:** Implement "LibUsb Filter" auto-installer to detach stock drivers.
- **Safe Mode:** Always backup `seccfg` (MTK) and `modemst1/2` (Qualcomm) before erase operations to prevent IMEI loss.

## 🔮 Future Exploits (Researching)
- **UniSoc SPD Auth:** Spreadtrum chips (Itel, Tecno) utilize RSA-signed FDL1. Need to implement RSA bypass for T606/T616.
- **Xiaomi HyperOS Account Disable:** Requires patching `persist` partition via EDL. (Risky but effective).
