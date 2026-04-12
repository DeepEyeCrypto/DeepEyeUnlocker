#!/bin/bash

# FRP USB Permission Integration Verification
# Checks that all permission components are properly connected

echo "========================================="
echo "  FRP Permission Integration Check"
echo "========================================="
echo ""

PASS=0
FAIL=0
TOTAL=0

check() {
    TOTAL=$((TOTAL + 1))
    if eval "$2" > /dev/null 2>&1; then
        echo "   ✅ $1"
        PASS=$((PASS + 1))
    else
        echo "   ❌ $1"
        FAIL=$((FAIL + 1))
    fi
}

echo "Checking FrpBypassScreen.kt integration..."
echo "-------------------------------------------"

# Check 1: Permission state observation
check "Permission StateFlow observed" \
    "grep -q 'permissionGranted.collectAsStateWithLifecycle' app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt"

# Check 2: Permission status card exists
check "Permission status card UI present" \
    "grep -q 'USB Permission Granted' app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt"

# Check 3: Request permission button
check "Request permission button exists" \
    "grep -q 'viewModel.requestUsbPermission' app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt"

# Check 4: Input field permission check
check "Input field checks permission" \
    "grep -q 'enabled = !uiState.isRunning && permissionGranted' app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt"

# Check 5: Start button permission check
check "Start button checks permission" \
    "grep -A5 'Button(' app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt | grep -q 'permissionGranted'"

# Check 6: Permission required text
check "Dynamic button text for permission" \
    "grep -q 'Permission Required' app/src/main/kotlin/com/deepeye/otg/ui/screens/FrpBypassScreen.kt"

echo ""
echo "Checking FrpViewModel.kt permission management..."
echo "-------------------------------------------"

# Check 7: Permission state tracking
check "Permission StateFlow defined" \
    "grep -q 'val permissionGranted: StateFlow<Boolean>' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt"

# Check 8: Device tracking
check "Current device tracked" \
    "grep -q 'private var currentDevice: UsbDevice?' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt"

# Check 9: Polling mechanism
check "Permission polling implemented" \
    "grep -q 'isPollingActive' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt"

# Check 10: Polling in init block
check "Polling starts in init" \
    "grep -A10 'init {' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt | grep -q 'while (isPollingActive)'"

# Check 11: onCleared override
check "Polling stops in onCleared" \
    "grep -A3 'override fun onCleared()' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt | grep -q 'isPollingActive = false'"

# Check 12: Device tracking in startBypass
check "startBypass tracks device" \
    "grep -A2 'fun startBypass' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt | grep -q 'currentDevice = device'"

# Check 13: Device tracking in requestUsbPermission
check "requestUsbPermission tracks device" \
    "grep -A2 'fun requestUsbPermission' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt | grep -q 'currentDevice = device'"

# Check 14: Device cleared in clearState
check "clearState clears device" \
    "grep -A4 'fun clearState()' app/src/main/kotlin/com/deepeye/otg/viewmodel/FrpViewModel.kt | grep -q 'currentDevice = null'"

echo ""
echo "Checking FrpUseCase.kt permission validation..."
echo "-------------------------------------------"

# Check 15: Permission check in use case
check "UseCase checks permission" \
    "grep -q 'if (!usbManager.hasPermission(device))' app/src/main/kotlin/com/deepeye/otg/usecase/FrpUseCase.kt"

# Check 16: Error emission for no permission
check "UseCase emits error for no permission" \
    "grep -q 'USB permission not granted' app/src/main/kotlin/com/deepeye/otg/usecase/FrpUseCase.kt"

# Check 17: SecurityException handling
check "UseCase handles SecurityException" \
    "grep -q 'catch (e: SecurityException)' app/src/main/kotlin/com/deepeye/otg/usecase/FrpUseCase.kt"

echo ""
echo "Checking USB infrastructure..."
echo "-------------------------------------------"

# Check 18: UsbPermissionGuard exists
check "UsbPermissionGuard.kt exists" \
    "test -f app/src/main/kotlin/com/deepeye/otg/usb/UsbPermissionGuard.kt"

# Check 19: UsbBroadcastReceiver exists
check "UsbBroadcastReceiver.kt exists" \
    "test -f app/src/main/kotlin/com/deepeye/otg/usb/UsbBroadcastReceiver.kt"

# Check 20: Permission action in manifest
check "Permission action in AndroidManifest" \
    "grep -q 'com.deepeye.otg.USB_PERMISSION' app/src/main/AndroidManifest.xml"

# Check 21: USB host feature
check "USB host feature declared" \
    "grep -q 'android.hardware.usb.host' app/src/main/AndroidManifest.xml"

# Check 22: USB permission declared
check "USB permission declared" \
    "grep -q 'android.permission.USB_PERMISSION' app/src/main/AndroidManifest.xml"

echo ""
echo "========================================="
echo "  Verification Results"
echo "========================================="
echo ""
echo "   Total Checks: $TOTAL"
echo "   ✅ Passed: $PASS"
echo "   ❌ Failed: $FAIL"
echo ""

if [ $FAIL -eq 0 ]; then
    echo "   🎉 ALL CHECKS PASSED!"
    echo "   FRP permission integration is complete."
    echo ""
    exit 0
else
    echo "   ⚠️  SOME CHECKS FAILED"
    echo "   Please review the failed checks above."
    echo ""
    exit 1
fi
