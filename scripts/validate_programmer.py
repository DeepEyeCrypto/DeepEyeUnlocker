#!/usr/bin/env python3
"""
validate_programmer.py — DeepEye QC Firehose programmer validator
Usage: python3 scripts/validate_programmer.py app/src/main/assets/prog/*.elf
"""
import sys
import hashlib
import os


def validate(path: str) -> str:
    try:
        data = open(path, "rb").read()
    except FileNotFoundError:
        return "FAIL: file not found"
    except OSError as e:
        return f"FAIL: {e}"

    size = len(data)
    sha  = hashlib.sha256(data).hexdigest()[:16]

    if size < 65536:
        return f"FAIL: too small ({size}B — need ≥ 64KB; LFS pointer stub?)"

    is_elf = data[:4] == b"\x7fELF"
    # MBN formats used in older QC loaders
    is_mbn_v1 = data[:4] == b"\x7f\x01\x00\x00"
    is_mbn_v2 = data[:4] == b"\x7f\x02\x00\x00"
    is_mbn    = is_mbn_v1 or is_mbn_v2

    if not (is_elf or is_mbn):
        hdr = data[:4].hex()
        if size < 500:
            return f"FAIL: LFS pointer stub ({size}B) — run: cd /tmp/qc-loaders && git lfs pull"
        return f"FAIL: not ELF or MBN format (header=0x{hdr})"

    # Check for firehose strings within first 1MB
    search_area = data[:min(len(data), 0x100000)].lower()
    has_fh = any(kw in search_area for kw in [
        b"firehose", b"prog_emmc", b"prog_ufs", b"sahara",
    ])

    if not has_fh:
        return f"WARN: Firehose strings not found in ELF (may still work)  size={size:,}B sha={sha}..."

    # Detect storage type from filename + strings
    fname = os.path.basename(path).lower()
    if "ufs" in fname or b"ufs" in search_area:
        storage = "UFS"
    elif "emmc" in fname or b"emmc" in search_area:
        storage = "eMMC"
    else:
        storage = "UFS/eMMC?"

    fmt = "ELF" if is_elf else "MBN"
    return f"OK  size={size:,}B sha={sha}... fmt={fmt} storage={storage}"


def main():
    paths = sys.argv[1:]
    if not paths:
        print("Usage: validate_programmer.py <file.elf> [file.elf ...]")
        sys.exit(1)

    passed = 0
    failed = 0
    warned = 0

    for p in paths:
        result = validate(p)
        name   = os.path.basename(p)
        prefix = "PASS" if result.startswith("OK") else ("WARN" if result.startswith("WARN") else "FAIL")
        print(f"  [{prefix}] {name}: {result}")
        if prefix == "PASS":
            passed += 1
        elif prefix == "WARN":
            warned += 1
        else:
            failed += 1

    print(f"\nSummary: {passed} OK, {warned} WARN, {failed} FAIL  (total {len(paths)})")
    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
