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

# Function to generate icons for a specific density
generate_density() {
    local density=$1
    local size=$2
    local foreground_size=$3
    
    local dir="$OUTPUT_DIR/mipmap-$density"
    mkdir -p "$dir"
    
    echo "  - $density ($size x $size)"
    
    # Standard Launcher Icon (Legacy) - Transparent background
    convert "$SOURCE_IMAGE" -resize ${size}x${size} -background none -gravity center -extent ${size}x${size} \
        "$dir/ic_launcher.png"
        
    # Round Launcher Icon (Legacy) - Transparent background (same as standard for now)
    cp "$dir/ic_launcher.png" "$dir/ic_launcher_round.png"
    
    # Adaptive Foreground (108dp = full size, image within 72dp safe zone)
    convert "$SOURCE_IMAGE" -resize ${foreground_size}x${foreground_size} -background none -gravity center -extent ${foreground_size}x${foreground_size} \
        "$dir/ic_launcher_foreground.png"
}

# MDPI (baseline: 48dp = 48px, fg=108px)
generate_density "mdpi" 48 108

# HDPI (1.5x: 72px, fg=162px)
generate_density "hdpi" 72 162

# XHDPI (2x: 96px, fg=216px)
generate_density "xhdpi" 96 216

# XXHDPI (3x: 144px, fg=324px)
generate_density "xxhdpi" 144 324

# XXXHDPI (4x: 192px, fg=432px)
generate_density "xxxhdpi" 192 432

echo "✅ Launcher icons generated"

# Also create a high-res version for desktop/promotional use
echo "🖥️  Generating desktop icon (512x512)..."
convert "$SOURCE_IMAGE" -resize 512x512 -background none -gravity center -extent 512x512 \
    "DeepEye.UI.Modern/Resources/deepeye_icon.png"

# Create ICO for Windows
echo "🪟 Generating Windows ICO..."
convert "$SOURCE_IMAGE" -resize 256x256 -background none -gravity center -extent 256x256 \
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
