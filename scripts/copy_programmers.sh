#!/usr/bin/env bash
# copy_programmers.sh — Copy QC firehose ELFs from bkerler/Loaders to assets
# Run AFTER: git clone https://github.com/bkerler/Loaders /tmp/qc-loaders && cd /tmp/qc-loaders && git lfs pull
#
# Usage: bash scripts/copy_programmers.sh [--loaders-dir /tmp/qc-loaders]
set -euo pipefail

LOADERS_DIR="${1:-/tmp/qc-loaders}"
DEST="app/src/main/assets/prog"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Go to project root
cd "$PROJECT_ROOT"
mkdir -p "$DEST"

echo "=== DeepEye Programmer Copy Script ==="
echo "Source: $LOADERS_DIR"
echo "Dest:   $DEST"
echo ""

# Chipsets to find — ordered by priority
declare -A CHIPS
# Format: CHIPS[dest_suffix]="search_pattern"
CHIPS[sm8650_ufs]="sm8650*ufs*firehose*.elf"
CHIPS[sm8550_ufs]="sm8550*ufs*firehose*.elf"
CHIPS[sm8475_ufs]="sm8475*ufs*firehose*.elf"
CHIPS[sm8450_ufs]="sm8450*ufs*firehose*.elf"
CHIPS[sm8350_ufs]="sm8350*ufs*firehose*.elf"
CHIPS[sm8250_ufs]="sm8250*ufs*firehose*.elf"
CHIPS[sm8150_ufs]="sm8150*ufs*firehose*.elf"
CHIPS[sm7450_ufs]="sm7450*ufs*firehose*.elf"
CHIPS[sm7325_ufs]="sm7325*ufs*firehose*.elf"
CHIPS[sm7250_ufs]="sm7250*ufs*firehose*.elf"
CHIPS[sm7225_emmc]="sm7225*emmc*firehose*.elf"
CHIPS[sm6450_emmc]="sm6450*emmc*firehose*.elf"
CHIPS[sm6375_emmc]="sm6375*emmc*firehose*.elf"
CHIPS[sm6115_emmc]="sm6115*emmc*firehose*.elf"
CHIPS[msm8998_ufs]="msm8998*ufs*firehose*.elf"
CHIPS[msm8953_emmc]="msm8953*emmc*firehose*.elf"
CHIPS[msm8937_emmc]="msm8937*emmc*firehose*.elf"
CHIPS[msm8909_emmc]="msm8909*emmc*firehose*.elf"

COPIED=0
MISSING=0

for key in "${!CHIPS[@]}"; do
    pattern="${CHIPS[$key]}"
    dest_file="$DEST/${key}_firehose.elf"
    
    # Already there? skip
    if [ -f "$dest_file" ]; then
        sz=$(stat -f%z "$dest_file" 2>/dev/null || stat -c%s "$dest_file" 2>/dev/null || echo "?")
        echo "  [SKIP] $key — already exists (${sz}B)"
        COPIED=$((COPIED + 1))
        continue
    fi
    
    # Search with and without ufs/emmc prefix (some repos don't have it)
    found=$(find "$LOADERS_DIR" -iname "$pattern" 2>/dev/null | head -1)
    
    # Fallback: search without storage type infix
    if [ -z "$found" ]; then
        chip_only="${key%_ufs}"
        chip_only="${chip_only%_emmc}"
        found=$(find "$LOADERS_DIR" -iname "${chip_only}*firehose*.elf" 2>/dev/null | head -1)
        # Also try .mbn
        if [ -z "$found" ]; then
            found=$(find "$LOADERS_DIR" -iname "${chip_only}*firehose*.mbn" 2>/dev/null | head -1)
        fi
    fi
    
    if [ -n "$found" ]; then
        cp "$found" "$dest_file"
        sz=$(stat -f%z "$dest_file" 2>/dev/null || stat -c%s "$dest_file" 2>/dev/null || echo "?")
        echo "  [OK]   $key → $(basename "$found") (${sz}B)"
        COPIED=$((COPIED + 1))
    else
        echo "  [MISS] $key — pattern: $pattern"
        MISSING=$((MISSING + 1))
    fi
done

echo ""
echo "Done: $COPIED copied, $MISSING missing"
echo ""
echo "Next: validate with:"
echo "  python3 scripts/validate_programmer.py $DEST/*.elf"
