#!/bin/bash

set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

usage() {
  cat <<'EOF'
Usage: bash ./scripts/build_macos_pkg.sh [--target <triple>]... [--config <path>] [--skip-build]

Builds DeepEyeUnlocker macOS installer artifacts by generating a Tauri `.app`
bundle and wrapping it in a `.pkg` installer built with `pkgbuild`.

Options:
  --target <triple>  macOS target triple to package. May be supplied more than once.
  --config <path>    Tauri config file to read version and product metadata from.
  --skip-build       Reuse an existing Tauri build instead of invoking `tauri build`.
  --help             Show this help text.
EOF
}

config_path="src-tauri/tauri.conf.json"
skip_build="false"
declare -a targets=()
declare -a cleanup_dirs=()

cleanup() {
  local dir_path
  for dir_path in "${cleanup_dirs[@]:-}"; do
    if [ -n "$dir_path" ] && [ -d "$dir_path" ]; then
      rm -rf "$dir_path"
    fi
  done
}

trap cleanup EXIT

while [ "$#" -gt 0 ]; do
  case "$1" in
    --target)
      if [ "$#" -lt 2 ]; then
        echo "Missing value for --target" >&2
        exit 1
      fi
      targets+=("$2")
      shift 2
      ;;
    --config)
      if [ "$#" -lt 2 ]; then
        echo "Missing value for --config" >&2
        exit 1
      fi
      config_path="$2"
      shift 2
      ;;
    --skip-build)
      skip_build="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [ "$(uname -s)" != "Darwin" ]; then
  echo "This script must be run on macOS." >&2
  exit 1
fi

if ! command -v pkgbuild >/dev/null 2>&1; then
  echo "pkgbuild is required and was not found in PATH." >&2
  exit 1
fi

if [ "$skip_build" != "true" ] && ! command -v npx >/dev/null 2>&1; then
  echo "npx is required to invoke the local Tauri CLI." >&2
  exit 1
fi

if [ ! -f "$config_path" ]; then
  echo "Tauri config not found: $config_path" >&2
  exit 1
fi

if [ "${#targets[@]}" -eq 0 ]; then
  case "$(uname -m)" in
    arm64)
      targets=("aarch64-apple-darwin")
      ;;
    x86_64)
      targets=("x86_64-apple-darwin")
      ;;
    *)
      echo "Unsupported macOS host architecture: $(uname -m)" >&2
      exit 1
      ;;
  esac
fi

product_name="$(python3 - "$config_path" <<'PY'
from pathlib import Path
import json
import sys

config = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(config["productName"])
PY
)"

version="$(python3 - "$config_path" <<'PY'
from pathlib import Path
import json
import sys

config = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(config["version"])
PY
)"

identifier="$(python3 - "$config_path" <<'PY'
from pathlib import Path
import json
import sys

config = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(config["identifier"])
PY
)"

artifact_name="${product_name// /}"
scripts_dir="$project_root/src-tauri/resources/macos/pkg"
postinstall_script="$scripts_dir/postinstall"

if [ ! -f "$postinstall_script" ]; then
  echo "PKG postinstall script not found: $postinstall_script" >&2
  exit 1
fi

chmod +x "$postinstall_script"

if [ -n "${APPLE_INSTALLER_SIGNING_IDENTITY:-}" ]; then
  echo "Using installer signing identity: ${APPLE_INSTALLER_SIGNING_IDENTITY}"
else
  echo "APPLE_INSTALLER_SIGNING_IDENTITY not set; generating unsigned PKG artifacts."
fi

arch_label_for_target() {
  case "$1" in
    aarch64-apple-darwin)
      echo "aarch64"
      ;;
    x86_64-apple-darwin)
      echo "x86_64"
      ;;
    universal-apple-darwin)
      echo "universal"
      ;;
    *)
      echo "$1" | tr '/:' '__'
      ;;
  esac
}

build_target() {
  local target="$1"

  if [ "$skip_build" = "true" ]; then
    return
  fi

  echo "Building Tauri macOS bundles for ${target}"
  npx tauri build --config "$config_path" --target "$target" --bundles app
}

package_target() {
  local target="$1"
  local bundle_dir="target/${target}/release/bundle/macos"
  local app_bundle
  local arch_label
  local pkg_dir
  local pkg_path
  local temp_dir
  local payload_root
  local app_destination
  local -a pkgbuild_args

  if [ ! -d "$bundle_dir" ]; then
    echo "Expected Tauri bundle directory was not found: $bundle_dir" >&2
    exit 1
  fi

  app_bundle="$(find "$bundle_dir" -maxdepth 1 -type d -name '*.app' | sort | head -n 1 || true)"

  if [ -z "$app_bundle" ]; then
    echo "No macOS app bundle found in $bundle_dir" >&2
    exit 1
  fi

  arch_label="$(arch_label_for_target "$target")"
  pkg_dir="target/${target}/release/bundle/pkg"
  pkg_path="${pkg_dir}/${artifact_name}_${version}_${arch_label}.pkg"
  temp_dir="$(mktemp -d)"
  cleanup_dirs+=("$temp_dir")
  payload_root="${temp_dir}/payload"
  app_destination="${payload_root}/Applications/$(basename "$app_bundle")"

  mkdir -p "${payload_root}/Applications" "$pkg_dir"
  ditto "$app_bundle" "$app_destination"

  pkgbuild_args=(
    --root "$payload_root"
    --install-location "/"
    --identifier "$identifier"
    --version "$version"
    --scripts "$scripts_dir"
  )

  if [ -n "${APPLE_INSTALLER_SIGNING_IDENTITY:-}" ]; then
    pkgbuild_args+=(--sign "$APPLE_INSTALLER_SIGNING_IDENTITY")
  fi

  echo "Packaging ${app_bundle} -> ${pkg_path}"
  pkgbuild "${pkgbuild_args[@]}" "$pkg_path"
}

for target in "${targets[@]}"; do
  build_target "$target"
  package_target "$target"
done

echo "macOS installer packaging complete."
