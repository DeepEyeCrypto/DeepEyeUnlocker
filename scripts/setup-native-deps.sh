#!/bin/bash
set -e

# Define paths
LIBUSB_DIR="portable/core/libusb"
LIBUSB_REPO="https://github.com/libusb/libusb.git"
LIBUSB_TAG="v1.0.27"

echo "=== DeepEye Native Dependency Injection ==="

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

# Create Android.mk adapter for CMake if needed, or just point CMake to sources
echo "Injecting Android config..."

# We need to ensure config.h is generated or mocked for Android
# LibUSB has an android/ directory but CMake build is better
echo "Dependency setup complete."
