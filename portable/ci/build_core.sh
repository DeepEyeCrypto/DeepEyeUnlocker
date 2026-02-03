#!/bin/bash
set -e

# build_core.sh - DeepEye Portable Unified Build Script
# Usage: ./build_core.sh [android|desktop]

PLATFORM=$1
BUILD_DIR="build_$PLATFORM"
mkdir -p $BUILD_DIR
cd $BUILD_DIR

if [ "$PLATFORM" == "android" ]; then
    echo "==> Building DeepEye Core for Android (arm64-v8a)..."
    cmake .. \
        -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-26 \
        -DCMAKE_BUILD_TYPE=Release
    make -j$(nproc)
    
    echo "==> Success. Artifact: libdeepeye_core.so"

elif [ "$PLATFORM" == "desktop" ]; then
    echo "==> Building DeepEye Core for Desktop..."
    cmake .. -DCMAKE_BUILD_TYPE=Release
    make -j$(nproc)
    
    echo "==> Success. Artifact: deepeye_core (shared lib)"
else
    echo "Usage: ./build_core.sh [android|desktop]"
    exit 1
fi
