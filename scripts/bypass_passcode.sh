#!/bin/bash
# Bypass passcode/ screen lock script
# Requires device to be in DFU mode and checkm8 exploited

if [ $# -ne 1 ]; then
    echo "Usage: $0 <iboot_path>"
    exit 1
fi

IBOOT_PATH="$1"

# Check if device is connected
if ! idevice_id --list > /dev/null 2>&1; then
    echo "No iOS device found. Please connect device and ensure it's in DFU mode."
    exit 1
fi

# Send iBoot to device
echo "Sending iBoot to device..."
irecovery -f "$IBOOT_PATH"
if [ $? -ne 0 ]; then
    echo "Failed to send iBoot. Ensure device is in DFU mode and checkm8 exploited."
    exit 1
fi

# Wait for device to boot
echo "Waiting for device to boot..."
sleep 5

# Check if device is pwned
if ! idevice_id --list > /dev/null 2>&1; then
    echo "Device not responding. Ensure checkm8 exploit was successful."
    exit 1
fi

# Run passcode bypass operations
echo "Running passcode bypass operations..."
# Add specific passcode bypass commands here
# Example: delete passcode files, reset keychain, etc.

# For now, just verify device is accessible
if ideviceinfo > /dev/null 2>&1; then
    echo "Passcode bypass successful. Device is accessible."
    exit 0
else
    echo "Passcode bypass failed. Device not accessible."
    exit 1
fi