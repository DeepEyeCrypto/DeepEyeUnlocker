#!/usr/bin/env python3
"""
validate_da.py — DeepEye DA validator
Usage: python3 scripts/validate_da.py app/src/main/assets/da/*.bin
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

    if size < 4096:
        return f"FAIL: too small ({size}B — need ≥ 4096B)"

    # Check magic patterns
    magic_v1   = data[:4] == b"\x4D\x4D\x4D\x01"   # MTK DA v1 Classic
    magic_v2   = data[:4] == b"\x4D\x4D\x4D\x02"   # MTK DA v2 / V6
    is_elf     = data[:4] == b"\x7fELF"
    is_arm_b   = data[3:4] == b"\xEA"               # ARM unconditional branch
    # Windows PE in rare SP Flash Tool bundles
    is_pe      = data[:2] == b"MZ"
    # AllInOne marker string sometimes present inside
    has_allinone = b"MTK_AllInOne" in data[:0x200]

    if not any([magic_v1, magic_v2, is_elf, is_arm_b, is_pe]):
        hdr = data[:4].hex()
        tag = f"WARN: unknown header 0x{hdr} (may still work)"
        return f"{tag}  size={size:,}B sha={sha}..."

    proto = "V6" if magic_v2 else "Classic"
    fmt   = "ELF" if is_elf else "MTK" if (magic_v1 or magic_v2) else "ARM" if is_arm_b else "PE"
    allinone = " [AllInOne]" if has_allinone else ""
    return f"OK  size={size:,}B sha={sha}... fmt={fmt} proto={proto}{allinone}"


def main():
    paths = sys.argv[1:]
    if not paths:
        print("Usage: validate_da.py <file.bin> [file.bin ...]")
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
