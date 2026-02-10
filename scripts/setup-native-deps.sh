#!/bin/bash
set -e

# Define paths (New Directory to avoid submodule conflicts)
LIBUSB_DIR="portable/core/libusb-source"
LIBUSB_REPO="https://github.com/libusb/libusb.git"
LIBUSB_TAG="v1.0.27"

echo "=== DeepEye Native Dependency Injection ==="

# Clean old dir if exists
rm -rf "portable/core/libusb"

# Check if libusb exists
if [ -d "$LIBUSB_DIR" ]; then
    echo "Updating libusb..."
    cd "$LIBUSB_DIR"
    git fetch
    git checkout "$LIBUSB_TAG"
    cd -
else
    echo "Cloning libusb ($LIBUSB_TAG)..."
    git clone --depth 1 --branch "$LIBUSB_TAG" "$LIBUSB_REPO" "$LIBUSB_DIR"
fi

# NUCLEAR: Remove .git to prevent submodule issues forever
rm -rf "$LIBUSB_DIR/.git"

echo "Dependency setup complete."
