#!/bin/bash
# DeepEyeUnlocker - Build Android APK and Release ZIP
# Platform: macOS/Linux

set -e  # Exit on error

VERSION="4.0.0"
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}🔨 DeepEyeUnlocker v$VERSION - Build Script${NC}"
echo ""

# ============================================================================
# PART 1: Build Android APK
# ============================================================================

echo -e "${CYAN}📱 Building Android APK (Portable OTG)...${NC}"

# Check if we're on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "${YELLOW}⚠️  Android NDK build requires Linux or CI environment${NC}"
    echo -e "${YELLOW}⚠️  Skipping Android APK build on macOS${NC}"
    echo ""
    echo "To build Android APK, you have two options:"
    echo "  1. Use GitHub Actions (push a tag to trigger workflow)"
    echo "  2. Use a Linux machine or Docker container"
    echo ""
    SKIP_ANDROID=true
else
    SKIP_ANDROID=false
fi

if [ "$SKIP_ANDROID" = false ]; then
    # Check for required tools
    if ! command -v gradle &> /dev/null; then
        echo -e "${RED}❌ Gradle not found. Please install Gradle${NC}"
        exit 1
    fi

    if [ -z "$ANDROID_NDK_LATEST_HOME" ] && [ -z "$ANDROID_NDK_HOME" ]; then
        echo -e "${RED}❌ Android NDK not found. Please set ANDROID_NDK_HOME${NC}"
        exit 1
    fi

    # Build native core
    echo "Building native C++ core..."
    cd portable/core
    mkdir -p build && cd build
    cmake .. -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake \
             -DANDROID_ABI=arm64-v8a \
             -DANDROID_PLATFORM=android-26 \
             -DCMAKE_BUILD_TYPE=Release
    make deepeye_core
    cd ../../..

    # Build Android APK
    echo "Building Android APK..."
    cd portable/android
    ./gradlew assembleRelease
    cd ../..

    # Copy APK to artifacts
    mkdir -p artifacts/android
    cp portable/android/app/build/outputs/apk/release/*.apk artifacts/android/ 2>/dev/null || true
    
    if [ -f artifacts/android/*.apk ]; then
        echo -e "${GREEN}✅ Android APK built successfully!${NC}"
        ls -lh artifacts/android/*.apk
    else
        echo -e "${RED}❌ Android APK build failed${NC}"
    fi
    echo ""
fi

# ============================================================================
# PART 2: Create Windows Portable Release ZIP
# ============================================================================

echo -e "${CYAN}📦 Creating Windows Portable Release ZIP...${NC}"

# Check if Windows portable build exists
if [ ! -f "artifacts/portable/DeepEyeUnlocker.exe" ]; then
    echo -e "${RED}❌ Windows portable build not found at artifacts/portable/DeepEyeUnlocker.exe${NC}"
    echo -e "${YELLOW}Please run the Windows build first using build.ps1 or GitHub Actions${NC}"
    exit 1
fi

# Create release directory
RELEASE_DIR="artifacts/release"
ZIP_NAME="DeepEyeUnlocker-v${VERSION}-Portable.zip"

echo "Creating release package..."
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"

# Copy main executables and DLLs
echo "  → Copying executables..."
cp artifacts/portable/DeepEyeUnlocker.exe "$RELEASE_DIR/"
cp artifacts/portable/*.dll "$RELEASE_DIR/" 2>/dev/null || true
cp artifacts/portable/*.json "$RELEASE_DIR/" 2>/dev/null || true

# Copy documentation
echo "  → Copying documentation..."
cp README.md "$RELEASE_DIR/"
cp LICENSE "$RELEASE_DIR/"
cp CHANGELOG.md "$RELEASE_DIR/" 2>/dev/null || echo "CHANGELOG.md not found, skipping"

# Copy essential docs
mkdir -p "$RELEASE_DIR/docs"
cp docs/USER_MANUAL.md "$RELEASE_DIR/docs/" 2>/dev/null || true
cp docs/TROUBLESHOOTING.md "$RELEASE_DIR/docs/" 2>/dev/null || true
cp docs/LEGAL.md "$RELEASE_DIR/docs/" 2>/dev/null || true
cp docs/SUPPORTED_DEVICES.md "$RELEASE_DIR/docs/" 2>/dev/null || true
cp docs/BUILD.md "$RELEASE_DIR/docs/" 2>/dev/null || true

# Copy drivers if they exist
if [ -d "drivers" ]; then
    echo "  → Copying drivers..."
    cp -r drivers "$RELEASE_DIR/"
fi

# Copy resources if they exist
if [ -d "artifacts/portable/Resources" ]; then
    echo "  → Copying resources..."
    cp -r artifacts/portable/Resources "$RELEASE_DIR/"
fi

# Generate SHA256 checksums
echo "  → Generating checksums..."
cd "$RELEASE_DIR"
shasum -a 256 DeepEyeUnlocker.exe > DeepEyeUnlocker.exe.sha256
if [ -f "DeepEye.UI.Modern.exe" ]; then
    shasum -a 256 DeepEye.UI.Modern.exe > DeepEye.UI.Modern.exe.sha256
fi
cd ../..

# Create ZIP archive
echo "  → Creating ZIP archive..."
cd artifacts
zip -r "$ZIP_NAME" release/
cd ..

# Verify ZIP was created
if [ -f "artifacts/$ZIP_NAME" ]; then
    echo ""
    echo -e "${GREEN}✅ Release ZIP created successfully!${NC}"
    echo -e "${GREEN}   Location: artifacts/$ZIP_NAME${NC}"
    echo -e "${GREEN}   Size: $(du -h "artifacts/$ZIP_NAME" | cut -f1)${NC}"
    
    # Generate final checksum for the ZIP
    cd artifacts
    shasum -a 256 "$ZIP_NAME" > "${ZIP_NAME}.sha256"
    cd ..
    
    echo ""
    echo -e "${CYAN}📋 Release Package Contents:${NC}"
    unzip -l "artifacts/$ZIP_NAME" | head -20
else
    echo -e "${RED}❌ Failed to create release ZIP${NC}"
    exit 1
fi

# ============================================================================
# PART 3: Summary
# ============================================================================

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 Build Complete!${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

if [ "$SKIP_ANDROID" = false ] && [ -f artifacts/android/*.apk ]; then
    echo -e "${GREEN}✅ Android APK:${NC} artifacts/android/*.apk"
fi

echo -e "${GREEN}✅ Windows Portable ZIP:${NC} artifacts/$ZIP_NAME"
echo -e "${GREEN}✅ SHA256 Checksum:${NC} artifacts/${ZIP_NAME}.sha256"
echo ""

echo -e "${CYAN}Next Steps:${NC}"
echo "  1. Test the portable ZIP on a Windows machine"
if [ "$SKIP_ANDROID" = false ]; then
    echo "  2. Test the Android APK on an Android device with OTG support"
    echo "  3. Upload both artifacts to GitHub Release"
else
    echo "  2. Build Android APK using GitHub Actions or Linux environment"
    echo "  3. Upload artifacts to GitHub Release"
fi
echo ""
