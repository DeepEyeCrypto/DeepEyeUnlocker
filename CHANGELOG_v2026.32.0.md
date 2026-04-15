# DeepEye Unlocker v2026.32.0
**Release Date:** 2026-04-15
**Build:** 320

## Whats New
- Stage 12: Qualcomm EDL identification (35+ chip database)
- Stage 13: Samsung USB detection + PIT viewer + Odin validator
- Stage 14: MTK BROM identification (35+ chip database, scatter parser)
- iOS: iCloud bypass detection + MDM removal + firmware signature tools
- Android: Hilt DI throughout, @AndroidEntryPoint crash fix
- CI: Multi-platform GitHub Actions (Ubuntu 22.04)
- UI: GlassCard components, DeepEyeColors system, JetBrainsMono logs

- Stage 10: Apple ProTools Part 3 - Firmware, MDM, & Flash Method UI (DeepEyeCrypto)
- feat(apple): Stage 9 — Hello Screen bypass + iRemoval (DeepEyeCrypto)
- test: Stage 6 — TestSprite + instrumented + unit tests - bypass_screen.json: 10 TestSprite test cases - BypassScreenTest.kt: 7 Compose instrumented tests - MtkV6ExecutorTest.kt: 5 protocol unit tests - Coverage target: >80% protocol executors (DeepEyeCrypto)
- feat: Chaquopy offline Python — Stage 2-5 complete - DA validation before JUMP_DA (RealMtkV6Executor) - Python Luhn check replaces manual impl (RealServerBypassExecutor) - iOS activation payload builder wired (RealServerBypassExecutor) - UI: live IMEI validation with manufacturer detection - Unit tests: PythonBridgeTest - ABI filter: arm64-v8a only (APK size optimized) (DeepEyeCrypto)
- fix(frp): integrate USB permission UI with polling and validation flow (DeepEyeCrypto)
- fix(brom): prevent crashes in USB methods with comprehensive safety checks (DeepEyeCrypto)
- fix(mtk): fix BROM connection crash by adding USB permission checks and safe device handling (DeepEyeCrypto)
- docs(verification): add comprehensive REAL functionality verification report (DeepEyeCrypto)
- test(scripts): add verify_real_functionality.sh for end-to-end real code validation (DeepEyeCrypto)
- docs(adb): add comprehensive ADB setup, connection, and installation guides (DeepEyeCrypto)
- fix: Map SETTINGS NavTarget to PROFILE destination (DeepEyeCrypto)
- feat: Redesign SpotlightBottomBar with enhanced animations and theme support (DeepEyeCrypto)
- docs: Add theme system test report (DeepEyeCrypto)
- feat: Add Dark/Light/Monet theme system with Material You support (DeepEyeCrypto)
- feat: Add StrictMode for UI debugging in debug builds (DeepEyeCrypto)
- fix: Add @AndroidEntryPoint to MainActivity - ROOT CAUSE of APK crash (DeepEyeCrypto)
- fix: Complete Hilt dependency injection for all ViewModels (DeepEyeCrypto)
- fix: Hilt dependency injection for UsbViewModel crash (DeepEyeCrypto)


## Platform Coverage
| Platform | Detection | Info | Safe Scope |
|---|---|---|---|
| Apple iOS | ✅ | ✅ | ✅ |
| Qualcomm | ✅ | ✅ | ✅ |
| Samsung | ✅ | ✅ | ✅ |
| MediaTek | ✅ | ✅ | ✅ |
