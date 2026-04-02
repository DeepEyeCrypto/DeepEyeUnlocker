#!/bin/bash
# [INFERRED] Mirrors the DeepEye v7 pre-commit verification sequence from the April 2, 2026 brief.

set -euo pipefail

echo "=== DeepEye v7 Pre-Commit Test Suite ==="

echo "[1/5] cargo check..."
cargo_check_log="$(mktemp)"
cargo check --manifest-path src-tauri/Cargo.toml 2>&1 | tee "$cargo_check_log"
if grep -q "unused manifest key" "$cargo_check_log"; then
  echo "❌ FAIL: cargo check reported an unused manifest key"
  grep "unused manifest key" "$cargo_check_log"
  rm -f "$cargo_check_log"
  exit 1
fi
rm -f "$cargo_check_log"
echo "✅ cargo check passed"

echo "[2/5] cargo clippy (all targets)..."
cargo clippy \
  --manifest-path src-tauri/Cargo.toml \
  --all-targets \
  --all-features \
  -- -D warnings 2>&1
echo "✅ clippy passed (zero warnings)"

echo "[3/5] Pattern scan..."
unused_count="$(cargo clippy --manifest-path src-tauri/Cargo.toml --all-targets --all-features 2>&1 | grep -c "unused import" || true)"
if [ "$unused_count" -gt "0" ]; then
  echo "❌ FAIL: $unused_count unused import(s) found"
  cargo clippy --manifest-path src-tauri/Cargo.toml --all-targets --all-features 2>&1 | grep "unused import"
  exit 1
fi

version="$(python3 - <<'PY'
import json
from pathlib import Path

config = json.loads(Path("src-tauri/tauri.conf.json").read_text(encoding="utf-8"))
print(config.get("version", "0"))
PY
)"
major="${version%%.*}"
has_msi="$(python3 - <<'PY'
import json
from pathlib import Path

config = json.loads(Path("src-tauri/tauri.conf.json").read_text(encoding="utf-8"))
targets = config.get("bundle", {}).get("targets", [])
print(str("msi" in targets))
PY
)"
if [ "$major" -gt "255" ] && [ "$has_msi" = "True" ]; then
  echo "❌ FAIL: CalVer $version + MSI target = WiX crash (P002)"
  exit 1
fi

duplicate_publishers="$(grep -c "action-gh-release" .github/workflows/release.yml || true)"
if [ "$duplicate_publishers" -gt "1" ]; then
  echo "❌ FAIL: Multiple action-gh-release entries found (P006)"
  exit 1
fi

echo "✅ Pattern scan passed"

echo "[4/5] YAML validate..."
python3 - <<'PY'
try:
    import yaml
except ModuleNotFoundError as exc:
    raise SystemExit("PyYAML is required for workflow validation: pip3 install pyyaml") from exc

with open(".github/workflows/release.yml", "r", encoding="utf-8") as handle:
    yaml.safe_load(handle)
PY
echo "✅ YAML valid"

echo "[5/5] cargo test..."
cargo_test_log="$(mktemp)"
cargo test --manifest-path src-tauri/Cargo.toml >"$cargo_test_log" 2>&1
tail -20 "$cargo_test_log"
rm -f "$cargo_test_log"
echo "✅ Tests passed"

echo ""
echo "==================================="
echo "✅ ALL CHECKS PASSED — Safe to commit"
echo "==================================="
