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

echo "Dependency setup complete."
