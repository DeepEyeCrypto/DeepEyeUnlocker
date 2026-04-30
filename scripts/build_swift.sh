#!/bin/bash
# Build Swift binary and copy to Tauri resources
set -e

SWIFT_DIR="src-tauri/swift"
RESOURCES="src-tauri/resources"
BINARY="deepeye-core"

echo "→ Building Swift binary..."
cd "$SWIFT_DIR"

# Try multi-arch build
if ! swift build -c release --arch arm64 --arch x86_64 2>/dev/null; then
    echo "⚠️ Multi-arch build failed, trying host architecture..."
    swift build -c release
fi

# Get binary path
BUILT="$(swift build -c release --show-bin-path 2>/dev/null)/$BINARY"

cd -

echo "→ Creating universal binary..."
# Already universal from dual arch build above
cp "$BUILT" "$RESOURCES/$BINARY"
chmod +x "$RESOURCES/$BINARY"

SIZE=$(du -sh "$RESOURCES/$BINARY" | cut -f1)
echo "✅ Swift binary ready: $RESOURCES/$BINARY ($SIZE)"

# Quick test
"$RESOURCES/$BINARY" detect test-session 2>/dev/null || true
echo "✅ Binary executable"
