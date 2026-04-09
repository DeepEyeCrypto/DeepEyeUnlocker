#!/bin/bash
# Migration script: Replace StitchTokens with DeepEyeColors across all Kotlin files
# Run from project root: bash scripts/migrate_to_deepeye_colors.sh

set -e

echo "🎨 Starting DeepEyeColors migration..."

# Find all Kotlin files in the UI directory
FILES=$(find app/src/main/kotlin/com/deepeye/otg/ui -name "*.kt" -type f)

for FILE in $FILES; do
    echo "Processing: $FILE"
    
    # Replace import statement
    sed -i '' 's/import com\.deepeye\.otg\.ui\.theme\.StitchTokens/import com.deepeye.otg.ui.theme.DeepEyeColors/g' "$FILE"
    
    # Replace color references (order matters - more specific first)
    sed -i '' 's/StitchTokens\.Semantic\.BackgroundBase/DeepEyeColors.BG_VOID/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.BackgroundElevated/DeepEyeColors.BG_SURFACE/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.SurfaceCard/DeepEyeColors.BG_SURFACE/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.SurfaceGlass/DeepEyeColors.BG_SURFACE.copy(0.6f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.BorderSubtle/DeepEyeColors.WHITE_LOW.copy(0.3f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.BorderActive/DeepEyeColors.NEON_PURPLE.copy(0.3f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.TextMain/DeepEyeColors.WHITE_HIGH/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.TextMuted/DeepEyeColors.WHITE_MED/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.TextTechnical/DeepEyeColors.NEON_CYAN/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.StatusConnected/DeepEyeColors.NEON_GREEN/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.StatusIdle/DeepEyeColors.WHITE_MED/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.StatusHandshaking/DeepEyeColors.NEON_YELLOW/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.ProtocolMtk/DeepEyeColors.NEON_GREEN/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.ProtocolAdb/DeepEyeColors.NEON_BLUE/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.ProtocolEdl/DeepEyeColors.NEON_PURPLE/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.ProtocolSamsung/DeepEyeColors.NEON_CYAN/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.ProtocolFastboot/DeepEyeColors.NEON_ORANGE/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.ProtocolApple/DeepEyeColors.WHITE_HIGH/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.RiskSafeFill/DeepEyeColors.NEON_GREEN.copy(0.15f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.RiskAdvancedFill/DeepEyeColors.NEON_YELLOW.copy(0.15f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.RiskDangerFill/DeepEyeColors.NEON_PINK.copy(0.15f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.RiskSafeBorder/DeepEyeColors.NEON_GREEN.copy(0.3f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.RiskAdvancedBorder/DeepEyeColors.NEON_YELLOW.copy(0.3f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.RiskDangerBorder/DeepEyeColors.NEON_PINK.copy(0.3f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.TerminalBackground/DeepEyeColors.BG_VOID/g' "$FILE"
    sed -i '' 's/StitchTokens\.Semantic\.TerminalText/DeepEyeColors.NEON_CYAN/g' "$FILE"
    
    # Replace direct StitchTokens references
    sed -i '' 's/StitchTokens\.Primary/DeepEyeColors.NEON_PURPLE/g' "$FILE"
    sed -i '' 's/StitchTokens\.BackgroundDark/DeepEyeColors.BG_VOID/g' "$FILE"
    sed -i '' 's/StitchTokens\.SurfaceDark/DeepEyeColors.BG_SURFACE/g' "$FILE"
    sed -i '' 's/StitchTokens\.TextPrimary/DeepEyeColors.WHITE_HIGH/g' "$FILE"
    sed -i '' 's/StitchTokens\.TextSecondary/DeepEyeColors.WHITE_MED/g' "$FILE"
    sed -i '' 's/StitchTokens\.TextMono/DeepEyeColors.NEON_CYAN/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentBrom/DeepEyeColors.NEON_GREEN/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentAdb/DeepEyeColors.NEON_BLUE/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentEdl/DeepEyeColors.NEON_PURPLE/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentSamsung/DeepEyeColors.NEON_CYAN/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentError/DeepEyeColors.NEON_PINK/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentFastboot/DeepEyeColors.NEON_ORANGE/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentApple/DeepEyeColors.WHITE_HIGH/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentSuccess/DeepEyeColors.NEON_GREEN/g' "$FILE"
    sed -i '' 's/StitchTokens\.AccentWarning/DeepEyeColors.NEON_YELLOW/g' "$FILE"
    sed -i '' 's/StitchTokens\.GlassSurface/DeepEyeColors.BG_SURFACE.copy(0.6f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.GlassBorder/DeepEyeColors.WHITE_LOW.copy(0.3f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.GlassBorderActive/DeepEyeColors.NEON_PURPLE.copy(0.3f)/g' "$FILE"
    sed -i '' 's/StitchTokens\.ConnectionPulse/DeepEyeColors.NEON_PURPLE/g' "$FILE"
    sed -i '' 's/StitchTokens\.ConnectionIdle/DeepEyeColors.WHITE_MED/g' "$FILE"
    sed -i '' 's/StitchTokens\.ConnectionError/DeepEyeColors.NEON_PINK/g' "$FILE"
    
    # Replace typography references
    sed -i '' 's/StitchTokens\.DisplayLarge/DeepEyeType.HEADER.copy(fontSize = 32.sp)/g' "$FILE"
    sed -i '' 's/StitchTokens\.TitleLarge/DeepEyeType.SUBHEADER.copy(fontSize = 20.sp)/g' "$FILE"
    sed -i '' 's/StitchTokens\.BodyMedium/DeepEyeType.BODY.copy(fontSize = 14.sp)/g' "$FILE"
    sed -i '' 's/StitchTokens\.LabelSmall/DeepEyeType.CAPTION.copy(fontSize = 11.sp)/g' "$FILE"
    sed -i '' 's/StitchTokens\.MonoCode/DeepEyeType.MONO.copy(fontSize = 12.sp)/g' "$FILE"
    
done

echo "✅ Migration complete!"
echo "📝 Next steps:"
echo "  1. Add import for DeepEyeType where typography is used"
echo "  2. Run ./gradlew :app:compileDebugKotlin to check for errors"
echo "  3. Manually review complex files (BypassScreen.kt, MainScreen.kt)"
