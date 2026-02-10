#!/bin/bash
set -e

# Define paths
LIBUSB_DIR="portable/core/libusb-source"
LIBUSB_REPO="https://github.com/libusb/libusb.git"
LIBUSB_TAG="v1.0.27"

echo "=== DeepEye Native Dependency Injection ==="

# Clean old dir if exists (legacy)
rm -rf "portable/core/libusb"

# Check if libusb source already exists (embedded in repo)
if [ -d "$LIBUSB_DIR" ] && [ -f "$LIBUSB_DIR/libusb/core.c" ]; then
    echo "LibUSB source found embedded in repo. Skipping clone."
else
    echo "LibUSB source missing. Cloning..."
    git clone --depth 1 --branch "$LIBUSB_TAG" "$LIBUSB_REPO" "$LIBUSB_DIR"
    # Remove .git to prevent submodule issues
    rm -rf "$LIBUSB_DIR/.git"
fi

# Generate config.h for Android
CONFIG_H="$LIBUSB_DIR/libusb/config.h"
echo "Generating config.h at $CONFIG_H..."
cat > "$CONFIG_H" <<EOF
#ifndef LIBUSB_CONFIG_H
#define LIBUSB_CONFIG_H

#define HAVE_DLFCN_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_MEMORY_H 1
#define HAVE_POLL_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRINGS_H 1
#define HAVE_STRING_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TIME_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1
#define HAVE_GETTIMEOFDAY 1
#define HAVE_NFDS_T 1

#define OS_LINUX 1
#define THREADS_POSIX 1
#define DEFAULT_VISIBILITY __attribute__((visibility("default")))
#define ENABLE_LOGGING 1
#define USE_SYSTEM_LOGGING_FACILITY 1

#endif
EOF

echo "Dependency setup complete."
