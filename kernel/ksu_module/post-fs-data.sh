#!/system/bin/sh
# DeepEyeUnlocker Post-FS-Data Script
# This script runs early in the boot process as root.

MODDIR=${0%/*}

# 1. Load the DeepEye Kernel Module (LKM)
# We try to find the module based on the current kernel version
KERNEL_VER=$(uname -r)
MODULE_PATH="$MODDIR/modules/$KERNEL_VER/deepeye_kernel.ko"

if [ -f "$MODULE_PATH" ]; then
    echo "DeepEye: Loading kernel module $MODULE_PATH" > /dev/kmsg
    insmod "$MODULE_PATH"
else
    # Fallback/Generic module if applicable
    if [ -f "$MODDIR/deepeye_kernel.ko" ]; then
        insmod "$MODDIR/deepeye_kernel.ko"
    fi
fi

# 2. Setup Device Node Permissions
# The character device /dev/deepeye is created by the LKM
# We need to ensure the C# tool (running as shell or app) can talk to it
chmod 666 /dev/deepeye
