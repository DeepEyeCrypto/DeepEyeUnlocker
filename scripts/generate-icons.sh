#!/bin/bash

# DeepEye Unlocker - App Icon Generator
# Generates Android app icons from source image in multiple densities

SOURCE_IMAGE="portable/android/app/src/main/res/deepeye_icon_source.png"
OUTPUT_DIR="portable/android/app/src/main/res"

echo "🎨 DeepEye Icon Generator"
echo "=========================="

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "⚠️  ImageMagick not found. Installing via Homebrew..."
    brew install imagemagick
fi

# Generate launcher icons for different densities
# Android icon sizes (foreground should be 108x108 dp with 18dp safe zone = 72x72 visible)

echo "📱 Generating launcher icons..."

# MDPI (baseline: 48dp = 48px)
convert "$SOURCE_IMAGE" -resize 48x48 -background black -gravity center -extent 48x48 \
    "$OUTPUT_DIR/mipmap-mdpi/ic_launcher.png"
convert "$SOURCE_IMAGE" -resize 108x108 -background none -gravity center -extent 108x108 \
    "$OUTPUT_DIR/mipmap-mdpi/ic_launcher_foreground.png"

# HDPI (1.5x: 72px)
convert "$SOURCE_IMAGE" -resize 72x72 -background black -gravity center -extent 72x72 \
    "$OUTPUT_DIR/mipmap-hdpi/ic_launcher.png"
convert "$SOURCE_IMAGE" -resize 162x162 -background none -gravity center -extent 162x162 \
    "$OUTPUT_DIR/mipmap-hdpi/ic_launcher_foreground.png"

# XHDPI (2x: 96px)
convert "$SOURCE_IMAGE" -resize 96x96 -background black -gravity center -extent 96x96 \
    "$OUTPUT_DIR/mipmap-xhdpi/ic_launcher.png"
convert "$SOURCE_IMAGE" -resize 216x216 -background none -gravity center -extent 216x216 \
    "$OUTPUT_DIR/mipmap-xhdpi/ic_launcher_foreground.png"

# XXHDPI (3x: 144px)
convert "$SOURCE_IMAGE" -resize 144x144 -background black -gravity center -extent 144x144 \
    "$OUTPUT_DIR/mipmap-xxhdpi/ic_launcher.png"
convert "$SOURCE_IMAGE" -resize 324x324 -background none -gravity center -extent 324x324 \
    "$OUTPUT_DIR/mipmap-xxhdpi/ic_launcher_foreground.png"

# XXXHDPI (4x: 192px)
convert "$SOURCE_IMAGE" -resize 192x192 -background black -gravity center -extent 192x192 \
    "$OUTPUT_DIR/mipmap-xxxhdpi/ic_launcher.png"
convert "$SOURCE_IMAGE" -resize 432x432 -background none -gravity center -extent 432x432 \
    "$OUTPUT_DIR/mipmap-xxxhdpi/ic_launcher_foreground.png"

echo "✅ Launcher icons generated"

# Also create a high-res version for desktop/promotional use
echo "🖥️  Generating desktop icon (512x512)..."
convert "$SOURCE_IMAGE" -resize 512x512 -background black -gravity center -extent 512x512 \
    "DeepEye.UI.Modern/Resources/deepeye_icon.png"

# Create ICO for Windows
echo "🪟 Generating Windows ICO..."
convert "$SOURCE_IMAGE" -resize 256x256 -background black -gravity center -extent 256x256 \
    -define icon:auto-resize=256,128,64,48,32,16 \
    "DeepEye.UI.Modern/Resources/deepeye_icon.ico"

echo ""
echo "✅ All icons generated successfully!"
echo ""
echo "📋 Files created:"
echo "  - Android launcher icons (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi)"
echo "  - Desktop icon (512x512 PNG)"
echo "  - Windows icon (ICO with multiple sizes)"
echo ""
echo "🚀 Ready to build!"
