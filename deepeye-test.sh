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

# [INFERRED] Release CI patches tauri.conf.json from the current git tag; warn if a pinned version sneaks back in.
version_warning=0
if [[ "$version" != *AUTO* ]]; then
  echo "⚠️ WARN: src-tauri/tauri.conf.json version is pinned to $version; expected an AUTO placeholder for tag-synced releases"
  version_warning=1
fi

latest_tag="$(git tag --list 'v[0-9]*' --sort=-v:refname | head -n1 || true)"
if [ -n "$latest_tag" ] && [[ "$version" != *AUTO* ]]; then
  latest_version="${latest_tag#v}"
  if [ "$version" != "$latest_version" ]; then
    echo "⚠️ WARN: src-tauri/tauri.conf.json version ($version) does not match latest release tag $latest_tag"
    version_warning=1
  fi
fi

if [ "$version_warning" -eq 1 ]; then
  echo "⚠️ WARN: Release CI will patch the version from the tag, but the checked-in config looks stale"
fi

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

duplicate_publishers="$(grep -Rho "softprops/action-gh-release" .github/workflows 2>/dev/null | wc -l | tr -d ' ')"
if [ "$duplicate_publishers" -gt "1" ]; then
  echo "❌ FAIL: Multiple action-gh-release entries found across .github/workflows (P006)"
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

echo "[5/6] cargo test..."
cargo_test_log="$(mktemp)"
cargo test --manifest-path src-tauri/Cargo.toml >"$cargo_test_log" 2>&1
tail -20 "$cargo_test_log"
rm -f "$cargo_test_log"
echo "✅ Tests passed"

# [INFERRED] P009 guard launches the Tauri app long enough to catch startup-time plugin config panics before release tagging.
echo "[6/6] Tauri startup smoke test (P009)..."
startup_smoke_log="$(mktemp)"
startup_smoke_pid=""

cleanup_startup_smoke() {
  if [ -n "${startup_smoke_pid:-}" ] && kill -0 "$startup_smoke_pid" 2>/dev/null; then
    kill "$startup_smoke_pid" 2>/dev/null || true
    wait "$startup_smoke_pid" 2>/dev/null || true
  fi
  rm -f "$startup_smoke_log"
}

RUST_BACKTRACE=1 cargo run --manifest-path src-tauri/Cargo.toml >"$startup_smoke_log" 2>&1 &
startup_smoke_pid=$!
trap cleanup_startup_smoke EXIT

launch_deadline=$((SECONDS + 180))
launch_seen=0
stable_deadline=0

while [ "$SECONDS" -lt "$launch_deadline" ]; do
  if ! kill -0 "$startup_smoke_pid" 2>/dev/null; then
    echo "❌ FAIL P009: App died during startup"
    tail -50 "$startup_smoke_log"
    cleanup_startup_smoke
    trap - EXIT
    exit 1
  fi

  if [ "$launch_seen" -eq 0 ] && grep -q 'Running `' "$startup_smoke_log"; then
    launch_seen=1
    stable_deadline=$((SECONDS + 5))
  fi

  if [ "$launch_seen" -eq 1 ] && [ "$SECONDS" -ge "$stable_deadline" ]; then
    break
  fi

  sleep 1
done

if [ "$launch_seen" -eq 0 ]; then
  echo "❌ FAIL P009: cargo run did not reach the app launch phase within 180s"
  tail -50 "$startup_smoke_log"
  cleanup_startup_smoke
  trap - EXIT
  exit 1
fi

cleanup_startup_smoke
trap - EXIT
echo "✅ Startup smoke test passed (P009 guard)"

echo ""
echo "==================================="
echo "✅ ALL CHECKS PASSED — Safe to commit"
echo "==================================="
