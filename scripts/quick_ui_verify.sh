#!/bin/bash
# Simple UI-Backend Verification
echo "=== DEEPEYE UNLOCKER - UI/BACKEND VERIFICATION ==="
echo ""

PASS=0
FAIL=0

# MTK UI → VM → Engine
echo "[MTK Screen Verification]"
for method in "BROM_WIPE" "ADB_BACKUP" "FRIDA_HOOK" "META_MODE" "FRP_BYPASS"; do
    if grep -q "$method" app/src/main/kotlin/com/deepeye/otg/ui/screens/MtkExploitScreen.kt && \
       grep -q "bypassScreenLock" app/src/main/kotlin/com/deepeye/otg/viewmodel/MtkExploitViewModel.kt && \
       grep -q "fun bypassScreenLock" app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt; then
        echo "  ✅ $method mapped"
        ((PASS++))
    else
        echo "  ❌ $method NOT mapped"
        ((FAIL++))
    fi
done

for method in "runVoltageGlitch" "runDaAuthBypass" "runForceBlUnlock" "runSlaBypass"; do
    if grep -q "$method" app/src/main/kotlin/com/deepeye/otg/ui/screens/MtkExploitScreen.kt && \
       grep -q "fun $method" app/src/main/kotlin/com/deepeye/otg/viewmodel/MtkExploitViewModel.kt; then
        echo "  ✅ $method mapped"
        ((PASS++))
    else
        echo "  ❌ $method NOT mapped"
        ((FAIL++))
    fi
done

echo ""
echo "[Xiaomi Screen Verification]"
for method in "EDL_PATCH" "ADB_FRP_WIPE" "MIUI_LOOPHOLE" "FLASH_AUTH_PARTITION"; do
    if grep -q "$method" app/src/main/kotlin/com/deepeye/otg/ui/screens/XiaomiExploitScreen.kt && \
       grep -q "bypassMiAccount" app/src/main/kotlin/com/deepeye/otg/viewmodel/XiaomiExploitViewModel.kt && \
       grep -q "fun bypassMiAccount" app/src/main/kotlin/com/deepeye/otg/engine/xiaomi/XiaomiExploitEngine.kt; then
        echo "  ✅ Mi Account: $method mapped"
        ((PASS++))
    else
        echo "  ❌ Mi Account: $method NOT mapped"
        ((FAIL++))
    fi
done

echo ""
echo "[Navigation Verification]"
if grep -q "MTK_EXPLOIT" app/src/main/kotlin/com/deepeye/otg/ui/screens/NavTarget.kt && \
   grep -q "NavTarget.MTK_EXPLOIT -> MtkExploitScreen" app/src/main/kotlin/com/deepeye/otg/ui/screens/MainScreen.kt; then
    echo "  ✅ MTK_EXPLOIT navigation"
    ((PASS++))
else
    echo "  ❌ MTK_EXPLOIT navigation"
    ((FAIL++))
fi

if grep -q "XIAOMI_EXPLOIT" app/src/main/kotlin/com/deepeye/otg/ui/screens/NavTarget.kt && \
   grep -q "NavTarget.XIAOMI_EXPLOIT -> XiaomiExploitScreen" app/src/main/kotlin/com/deepeye/otg/ui/screens/MainScreen.kt; then
    echo "  ✅ XIAOMI_EXPLOIT navigation"
    ((PASS++))
else
    echo "  ❌ XIAOMI_EXPLOIT navigation"
    ((FAIL++))
fi

echo ""
echo "=== SUMMARY ==="
echo "PASS: $PASS"
echo "FAIL: $FAIL"

if [ $FAIL -eq 0 ]; then
    echo "✅ ALL CHECKS PASSED"
fi
