#!/bin/bash
# Add DeepEyeType import to files that use it but don't import it

echo "🔍 Finding files that use DeepEyeType..."

FILES_WITH_USAGE=$(grep -l "DeepEyeType\." app/src/main/kotlin/com/deepeye/otg/ui/**/*.kt 2>/dev/null)
FILES_WITH_IMPORT=$(grep -l "import com.deepeye.otg.ui.theme.DeepEyeType" app/src/main/kotlin/com/deepeye/otg/ui/**/*.kt 2>/dev/null)

for FILE in $FILES_WITH_USAGE; do
    # Check if file already has the import
    if ! grep -q "import com.deepeye.otg.ui.theme.DeepEyeType" "$FILE"; then
        # Add import after DeepEyeColors import if it exists
        if grep -q "import com.deepeye.otg.ui.theme.DeepEyeColors" "$FILE"; then
            sed -i '' 's/import com.deepeye.otg.ui.theme.DeepEyeColors/import com.deepeye.otg.ui.theme.DeepEyeColors\nimport com.deepeye.otg.ui.theme.DeepEyeType/g' "$FILE"
            echo "✅ Added DeepEyeType import (after DeepEyeColors): $FILE"
        # Or add after any theme import
        elif grep -q "import com.deepeye.otg.ui.theme" "$FILE"; then
            sed -i '' 's/import com.deepeye.otg.ui.theme\./import com.deepeye.otg.ui.theme.DeepEyeType\nimport com.deepeye.otg.ui.theme./g' "$FILE"
            echo "✅ Added DeepEyeType import (after other theme): $FILE"
        # Or add after components import
        elif grep -q "import com.deepeye.otg.ui.components" "$FILE"; then
            sed -i '' 's/import com.deepeye.otg.ui.components\./import com.deepeye.otg.ui.theme.DeepEyeType\nimport com.deepeye.otg.ui.components./g' "$FILE"
            echo "✅ Added DeepEyeType import (after components): $FILE"
        else
            echo "⚠️  No suitable import location found: $FILE"
        fi
    fi
done

echo ""
echo "✅ DeepEyeType import addition complete!"
echo "📝 Run ./gradlew :app:compileDebugKotlin to verify"
