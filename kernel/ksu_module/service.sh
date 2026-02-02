#!/system/bin/sh
# DeepEyeUnlocker Late-Boot Service Script

MODDIR=${0%/*}

# Wait for boot completion
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done

echo "DeepEye: System boot completed, initializing late-boot hooks" > /dev/kmsg

# Example: Periodically check if the kernel bridge is alive
(
    while true; do
        if [ ! -c /dev/deepeye ]; then
            echo "DeepEye: Bridge device missing, attempting reload" > /dev/kmsg
            sh "$MODDIR/post-fs-data.sh"
        fi
        sleep 60
    done
) &
