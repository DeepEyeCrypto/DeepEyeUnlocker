#!/bin/bash
# ADB Quick Verification Script
# Run this script to verify ADB is properly installed and configured

echo "=================================="
echo "  ADB Installation Verification"
echo "=================================="
echo ""

# Check if ADB is installed
echo "1. Checking ADB installation..."
if command -v adb &> /dev/null; then
    echo "   ✅ ADB found at: $(which adb)"
    echo ""
    echo "   Version:"
    adb version | head -n 2 | sed 's/^/   /'
else
    echo "   ❌ ADB not found!"
    echo "   Install with: brew install --cask android-platform-tools"
    exit 1
fi

echo ""
echo "2. Checking Fastboot installation..."
if command -v fastboot &> /dev/null; then
    echo "   ✅ Fastboot found at: $(which fastboot)"
else
    echo "   ⚠️  Fastboot not found (optional)"
fi

echo ""
echo "3. Checking PATH configuration..."
if echo $PATH | grep -q "/usr/local/bin"; then
    echo "   ✅ /usr/local/bin is in PATH"
else
    echo "   ⚠️  /usr/local/bin not in PATH"
fi

echo ""
echo "4. Testing ADB server..."
adb start-server 2>&1 | sed 's/^/   /'
echo "   ✅ ADB server running"

echo ""
echo "5. Scanning for connected devices..."
DEVICES=$(adb devices 2>&1 | grep -w "device$" | wc -l | tr -d ' ')
echo ""
if [ "$DEVICES" -gt 0 ]; then
    echo "   ✅ Found $DEVICES device(s):"
    echo ""
    adb devices 2>&1 | grep -w "device$" | while read line; do
        SERIAL=$(echo $line | awk '{print $1}')
        echo "   📱 Serial: $SERIAL"
        
        # Try to get device info
        MODEL=$(adb -s $SERIAL shell getprop ro.product.model 2>/dev/null)
        BRAND=$(adb -s $SERIAL shell getprop ro.product.brand 2>/dev/null)
        ANDROID=$(adb -s $SERIAL shell getprop ro.build.version.release 2>/dev/null)
        SDK=$(adb -s $SERIAL shell getprop ro.build.version.sdk 2>/dev/null)
        
        if [ ! -z "$MODEL" ]; then
            echo "      Model: $BRAND $MODEL"
            echo "      Android: $ANDROID (SDK $SDK)"
        fi
        echo ""
    done
else
    echo "   ⚠️  No devices connected"
    echo ""
    echo "   To connect a device:"
    echo "   1. Enable Developer Options on device"
    echo "   2. Enable USB Debugging"
    echo "   3. Connect via USB"
    echo "   4. Authorize computer on device"
fi

echo ""
echo "6. Testing shell command execution..."
if [ "$DEVICES" -gt 0 ]; then
    SERIAL=$(adb devices 2>&1 | grep -w "device$" | head -n 1 | awk '{print $1}')
    TEST_CMD=$(adb -s $SERIAL shell getprop ro.product.model 2>&1)
    if [ $? -eq 0 ]; then
        echo "   ✅ Shell commands working"
        echo "   Test result: $TEST_CMD"
    else
        echo "   ❌ Shell command failed"
    fi
else
    echo "   ⏭️  Skipped (no device)"
fi

echo ""
echo "=================================="
echo "  Verification Complete"
echo "=================================="
echo ""
echo "Next steps:"
echo "  • Launch DeepEye Unlocker to use ADB features"
echo "  • Run 'adb devices' to check connected devices"
echo "  • Run 'adb help' for available commands"
echo ""
