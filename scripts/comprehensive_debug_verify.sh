#!/bin/bash

# DeepEyeUnlocker - Comprehensive Debugging Verification
# Checks all critical areas: USB permission, BROM safety, UI-Backend sync, error handling

echo "============================================================"
echo "  DeepEyeUnlocker - Comprehensive Debugging Verification"
echo "============================================================"
echo ""

PASS=0
FAIL=0
WARN=0
TOTAL=0

check() {
    TOTAL=$((TOTAL + 1))
    local status=$1
    local message=$2
    
    case $status in
        "PASS")
            echo "   ✅ $message"
            PASS=$((PASS + 1))
            ;;
        "FAIL")
            echo "   ❌ $message"
            FAIL=$((FAIL + 1))
            ;;
        "WARN")
            echo "   ⚠️  $message"
            WARN=$((WARN + 1))
            ;;
    esac
}

# ═══════════════════════════════════════════════════════════════
# 1. BUILD STATUS
# ═══════════════════════════════════════════════════════════════
echo "1. BUILD STATUS"
echo "-----------------------------------------------------------"

# Check if build outputs exist
if [ -d "app/build/outputs/apk" ]; then
    check "PASS" "Build outputs directory exists"
else
    check "WARN" "Build outputs directory not found (run build first)"
fi

# Check for compilation errors in recent logs
if [ -f "app/build/reports/problems/problems-report.html" ]; then
    errors=$(grep -c "error" app/build/reports/problems/problems-report.html 2>/dev/null || echo "0")
    if [ "$errors" -eq 0 ]; then
        check "PASS" "No compilation errors in build report"
    else
        check "FAIL" "Found $errors errors in build report"
    fi
else
    check "WARN" "Build report not found (run build first)"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 2. USB PERMISSION HANDLING
# ═══════════════════════════════════════════════════════════════
echo "2. USB PERMISSION HANDLING"
echo "-----------------------------------------------------------"

# Check UsbPermissionGuard
if [ -f "app/src/main/kotlin/com/deepeye/otg/usb/UsbPermissionGuard.kt" ]; then
    check "PASS" "UsbPermissionGuard.kt exists"
    
    if grep -q "hasPermission" app/src/main/kotlin/com/deepeye/otg/usb/UsbPermissionGuard.kt; then
        check "PASS" "Permission validation present"
    else
        check "FAIL" "Missing permission validation"
    fi
    
    if grep -q "FLAG_MUTABLE" app/src/main/kotlin/com/deepeye/otg/usb/UsbPermissionGuard.kt; then
        check "PASS" "Android 12+ FLAG_MUTABLE present"
    else
        check "FAIL" "Missing FLAG_MUTABLE for Android 12+"
    fi
    
    if grep -q "setPackage" app/src/main/kotlin/com/deepeye/otg/usb/UsbPermissionGuard.kt; then
        check "PASS" "Package scoping for security"
    else
        check "FAIL" "Missing package scoping"
    fi
else
    check "FAIL" "UsbPermissionGuard.kt not found"
fi

# Check UsbPermissionManager
if [ -f "app/src/main/kotlin/com/deepeye/otg/UsbPermissionManager.kt" ]; then
    check "PASS" "UsbPermissionManager.kt exists"
    
    if grep -q "PermissionState" app/src/main/kotlin/com/deepeye/otg/UsbPermissionManager.kt; then
        check "PASS" "State machine implementation present"
    else
        check "FAIL" "Missing permission state machine"
    fi
else
    check "FAIL" "UsbPermissionManager.kt not found"
fi

# Check AndroidManifest permissions
if grep -q "android.permission.USB_PERMISSION" app/src/main/AndroidManifest.xml; then
    check "PASS" "USB permission declared in manifest"
else
    check "FAIL" "USB permission not declared in manifest"
fi

if grep -q "android.hardware.usb.host" app/src/main/AndroidManifest.xml; then
    check "PASS" "USB host feature declared"
else
    check "FAIL" "USB host feature not declared"
fi

if grep -q "com.deepeye.otg.USB_PERMISSION" app/src/main/AndroidManifest.xml; then
    check "PASS" "Permission broadcast action registered"
else
    check "FAIL" "Permission broadcast action not registered"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 3. BROM CONNECTION SAFETY
# ═══════════════════════════════════════════════════════════════
echo "3. BROM CONNECTION SAFETY"
echo "-----------------------------------------------------------"

# Check MtkExploitEngine safety checks
if [ -f "app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt" ]; then
    check "PASS" "MtkExploitEngine.kt exists"
    
    # Count safety checks
    perm_checks=$(grep -c "hasPermission" app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt || echo "0")
    if [ "$perm_checks" -ge 3 ]; then
        check "PASS" "USB permission checks present ($perm_checks found)"
    else
        check "FAIL" "Insufficient permission checks ($perm_checks found, need ≥3)"
    fi
    
    if grep -q "try {" app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt && \
       grep -q "catch.*SecurityException" app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt; then
        check "PASS" "SecurityException handling present"
    else
        check "FAIL" "Missing SecurityException handling"
    fi
    
    if grep -q "interfaceCount" app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt; then
        check "PASS" "Interface validation present"
    else
        check "FAIL" "Missing interface validation"
    fi
    
    if grep -q "listOf(1, 0)" app/src/main/kotlin/com/deepeye/otg/engine/mtk/MtkExploitEngine.kt; then
        check "PASS" "Dual interface endpoint search"
    else
        check "WARN" "Single interface check only (may miss BROM on interface #1)"
    fi
else
    check "FAIL" "MtkExploitEngine.kt not found"
fi

# Check RealMtkBromExecutor
if [ -f "app/src/main/kotlin/com/deepeye/otg/protocol/mtk/RealMtkBromExecutor.kt" ]; then
    check "PASS" "RealMtkBromExecutor.kt exists"
    
    if grep -q "performHandshake" app/src/main/kotlin/com/deepeye/otg/protocol/mtk/RealMtkBromExecutor.kt; then
        check "PASS" "BROM handshake implementation present"
    else
        check "FAIL" "Missing BROM handshake"
    fi
else
    check "WARN" "RealMtkBromExecutor.kt not found"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 4. UI-BACKEND SYNCHRONIZATION
# ═══════════════════════════════════════════════════════════════
echo "4. UI-BACKEND SYNCHRONIZATION"
echo "-----------------------------------------------------------"

# Check FrpBypassScreen integration
if [ -f "app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt" ]; then
    check "PASS" "FrpBypassScreen.kt exists"
    
    if grep -q "permissionGranted.collectAsStateWithLifecycle" app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt; then
        check "PASS" "Permission state observed"
    else
        check "FAIL" "Permission state not observed"
    fi
    
    if grep -q "USB Permission Granted" app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt; then
        check "PASS" "Permission status indicator present"
    else
        check "FAIL" "Missing permission status indicator"
    fi
    
    if grep -q "viewModel.requestUsbPermission" app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt; then
        check "PASS" "Permission request button present"
    else
        check "FAIL" "Missing permission request button"
    fi
else
    check "FAIL" "FrpBypassScreen.kt not found"
fi

# Check MtkExploitScreen integration
if [ -f "app/src/main/kotlin/com/deepeye/otg/ui/screens/MtkExploitScreen.kt" ]; then
    check "PASS" "MtkExploitScreen.kt exists"
    
    button_count=$(grep -c "ExploitButton" app/src/main/kotlin/com/deepeye/otg/ui/screens/MtkExploitScreen.kt || echo "0")
    if [ "$button_count" -ge 5 ]; then
        check "PASS" "Exploit buttons present ($button_count found)"
    else
        check "WARN" "Few exploit buttons ($button_count found)"
    fi
else
    check "FAIL" "MtkExploitScreen.kt not found"
fi

# Check XiaomiExploitScreen integration
if [ -f "app/src/main/kotlin/com/deepeye/otg/ui/screens/XiaomiExploitScreen.kt" ]; then
    check "PASS" "XiaomiExploitScreen.kt exists"
    
    button_count=$(grep -c "ExploitButton" app/src/main/kotlin/com/deepeye/otg/ui/screens/XiaomiExploitScreen.kt || echo "0")
    if [ "$button_count" -ge 5 ]; then
        check "PASS" "Exploit buttons present ($button_count found)"
    else
        check "WARN" "Few exploit buttons ($button_count found)"
    fi
else
    check "FAIL" "XiaomiExploitScreen.kt not found"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 5. ERROR HANDLING
# ═══════════════════════════════════════════════════════════════
echo "5. ERROR HANDLING"
echo "-----------------------------------------------------------"

# Check for Result types
result_count=$(grep -r "sealed class.*Result" app/src/main/kotlin --include="*.kt" | wc -l || echo "0")
if [ "$result_count" -ge 3 ]; then
    check "PASS" "Result types defined ($result_count found)"
else
    check "WARN" "Few Result types ($result_count found)"
fi

# Check for try-catch blocks
try_count=$(grep -r "catch.*Exception" app/src/main/kotlin --include="*.kt" | wc -l || echo "0")
if [ "$try_count" -ge 20 ]; then
    check "PASS" "Exception handling present ($try_count catch blocks)"
else
    check "WARN" "Limited exception handling ($try_count catch blocks)"
fi

# Check for error dialogs in UI
error_dialog_count=$(grep -r "AlertDialog" app/src/main/kotlin/com/deepeye/otg/ui/screens --include="*.kt" | wc -l || echo "0")
if [ "$error_dialog_count" -ge 3 ]; then
    check "PASS" "Error dialogs present ($error_dialog_count found)"
else
    check "WARN" "Few error dialogs ($error_dialog_count found)"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 6. VIEWMODEL INTEGRATION
# ═══════════════════════════════════════════════════════════════
echo "6. VIEWMODEL INTEGRATION"
echo "-----------------------------------------------------------"

# Count ViewModels
viewmodel_count=$(find app/src/main/kotlin -name "*ViewModel.kt" -type f | wc -l || echo "0")
if [ "$viewmodel_count" -ge 10 ]; then
    check "PASS" "ViewModels present ($viewmodel_count found)"
else
    check "WARN" "Few ViewModels ($viewmodel_count found)"
fi

# Check for StateFlow usage
stateflow_count=$(grep -r "MutableStateFlow\|StateFlow" app/src/main/kotlin --include="*.kt" | wc -l || echo "0")
if [ "$stateflow_count" -ge 20 ]; then
    check "PASS" "StateFlow reactive pattern used ($stateflow_count found)"
else
    check "WARN" "Limited StateFlow usage ($stateflow_count found)"
fi

# Check for Hilt dependency injection
hilt_count=$(grep -r "@HiltViewModel\|@Inject" app/src/main/kotlin --include="*.kt" | wc -l || echo "0")
if [ "$hilt_count" -ge 10 ]; then
    check "PASS" "Hilt DI present ($hilt_count found)"
else
    check "WARN" "Limited Hilt usage ($hilt_count found)"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 7. NAVIGATION SYSTEM
# ═══════════════════════════════════════════════════════════════
echo "7. NAVIGATION SYSTEM"
echo "-----------------------------------------------------------"

if [ -f "app/src/main/kotlin/com/deepeye/otg/ui/screens/NavTarget.kt" ]; then
    check "PASS" "NavTarget.kt exists"
    
    nav_count=$(grep -c "NavTarget\." app/src/main/kotlin/com/deepeye/otg/ui/screens/NavTarget.kt || echo "0")
    if [ "$nav_count" -ge 20 ]; then
        check "PASS" "Navigation targets defined ($nav_count found)"
    else
        check "WARN" "Few navigation targets ($nav_count found)"
    fi
else
    check "FAIL" "NavTarget.kt not found"
fi

if [ -f "app/src/main/kotlin/com/deepeye/otg/ui/screens/MainScreen.kt" ]; then
    check "PASS" "MainScreen.kt exists (navigation host)"
else
    check "FAIL" "MainScreen.kt not found"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 8. SECURITY AUDIT
# ═══════════════════════════════════════════════════════════════
echo "8. SECURITY AUDIT"
echo "-----------------------------------------------------------"

# Check for hardcoded credentials
cred_count=$(grep -r "password\s*=\|apiKey\s*=\|secret\s*=" app/src/main/kotlin --include="*.kt" | grep -v "//\|*" | wc -l || echo "0")
if [ "$cred_count" -eq 0 ]; then
    check "PASS" "No hardcoded credentials found"
else
    check "FAIL" "Potential hardcoded credentials ($cred_count found)"
fi

# Check for secure file sharing
if grep -q "FileProvider" app/src/main/AndroidManifest.xml; then
    check "PASS" "FileProvider for secure file sharing"
else
    check "WARN" "FileProvider not found"
fi

# Check for network security config
if grep -q "networkSecurityConfig" app/src/main/AndroidManifest.xml; then
    check "PASS" "Network security config present"
else
    check "WARN" "Network security config not found"
fi

# Check for receiver export control
if grep -q "RECEIVER_NOT_EXPORTED\|exported=\"false\"" app/src/main/AndroidManifest.xml; then
    check "PASS" "Broadcast receiver export control"
else
    check "WARN" "Broadcast receiver export control not found"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# 9. CODE QUALITY
# ═══════════════════════════════════════════════════════════════
echo "9. CODE QUALITY"
echo "-----------------------------------------------------------"

# Count TODO/FIXME items
todo_count=$(grep -r "TODO\|FIXME\|XXX\|HACK" app/src/main/kotlin --include="*.kt" | wc -l || echo "0")
if [ "$todo_count" -le 20 ]; then
    check "PASS" "TODO/FIXME count acceptable ($todo_count found)"
else
    check "WARN" "High TODO/FIXME count ($todo_count found)"
fi

# Check for proper package structure
if [ -d "app/src/main/kotlin/com/deepeye/otg/ui/screens" ] && \
   [ -d "app/src/main/kotlin/com/deepeye/otg/viewmodel" ] && \
   [ -d "app/src/main/kotlin/com/deepeye/otg/usb" ]; then
    check "PASS" "Proper package structure"
else
    check "FAIL" "Package structure incomplete"
fi

# Check for test files
test_count=$(find app/src/test -name "*.kt" -type f 2>/dev/null | wc -l || echo "0")
if [ "$test_count" -ge 5 ]; then
    check "PASS" "Unit tests present ($test_count found)"
else
    check "WARN" "Few unit tests ($test_count found)"
fi

echo ""

# ═══════════════════════════════════════════════════════════════
# FINAL RESULTS
# ═══════════════════════════════════════════════════════════════
echo "============================================================"
echo "  VERIFICATION RESULTS"
echo "============================================================"
echo ""
echo "   Total Checks: $TOTAL"
echo "   ✅ Passed: $PASS"
echo "   ❌ Failed: $FAIL"
echo "   ⚠️  Warnings: $WARN"
echo ""

if [ $FAIL -eq 0 ]; then
    echo "   🎉 EXCELLENT - No critical issues found!"
    echo ""
    if [ $WARN -gt 0 ]; then
        echo "   ℹ️  $WARN warning(s) documented (non-blocking)"
        echo "   ℹ️  Review warnings for potential improvements"
    fi
    echo ""
    exit 0
else
    echo "   🔴 CRITICAL - $FAIL issue(s) require attention!"
    echo ""
    echo "   Please fix the failed checks above before proceeding"
    echo ""
    exit 1
fi
