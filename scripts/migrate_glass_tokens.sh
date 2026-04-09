#!/bin/bash
# Remove GlassTokens imports and replace with DeepEyeColors equivalents

echo "🔍 Migrating GlassTokens references..."

FILES=$(grep -l "GlassTokens" app/src/main/kotlin/com/deepeye/otg/ui/**/*.kt 2>/dev/null)

for FILE in $FILES; do
    echo "📝 Processing: $FILE"
    
    # Replace GlassTokens references
    sed -i '' 's/GlassTokens\.backgroundBrush/Brush.verticalGradient(listOf(DeepEyeColors.BG_SURFACE, DeepEyeColors.BG_VOID))/g' "$FILE"
    sed -i '' 's/GlassTokens\.cardBorderColor/DeepEyeColors.WHITE_LOW.copy(0.4f)/g' "$FILE"
    sed -i '' 's/GlassTokens\.GlassSurface/DeepEyeColors.BG_SURFACE.copy(0.6f)/g' "$FILE"
    
    # Remove GlassTokens import
    sed -i '' '/import com\.deepeye\.otg\.ui\.theme\.GlassTokens/d' "$FILE"
    sed -i '' '/import com\.deepeye\.otg\.ui\.theme\.GlassTokens\.\*/d' "$FILE"
    
    # Add Brush import if needed
    if grep -q "Brush\." "$FILE" && ! grep -q "import androidx.compose.ui.graphics.Brush" "$FILE"; then
        sed -i '' 's/import androidx.compose.ui.graphics.Color/import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color/g' "$FILE"
    fi
    
    echo "✅ Updated: $FILE"
done

echo ""
echo "✅ GlassTokens migration complete!"
echo "📝 Run ./gradlew :app:compileDebugKotlin to verify"
