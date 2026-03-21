from __future__ import annotations

import json
import plistlib
import sys
from pathlib import Path


def _find_value(container, candidate_keys: tuple[str, ...]):
    if isinstance(container, dict):
        lowered = {str(key).lower(): value for key, value in container.items()}
        for key in candidate_keys:
            if key.lower() in lowered:
                return lowered[key.lower()]
        for value in container.values():
            found = _find_value(value, candidate_keys)
            if found is not None:
                return found
    elif isinstance(container, list):
        for item in container:
            found = _find_value(item, candidate_keys)
            if found is not None:
                return found
    return None


def _to_hex(value):
    if value is None:
        return None
    if isinstance(value, bytes):
        return value.hex()
    if isinstance(value, str):
        return value.strip().replace(" ", "").replace(":", "")
    return None


def extract_hash(backup_path: str) -> dict[str, object]:
    """Extract Screen Time / restrictions hash metadata from a backup directory or plist."""
    target = Path(backup_path).expanduser().resolve()
    if not target.exists():
        return {"error": f"Backup path not found: {target}"}

    candidate_files: list[Path] = []
    if target.is_file():
        candidate_files.append(target)
    else:
        candidate_files.extend(
            [
                target / "Library/Preferences/com.apple.restrictionspassword.plist",
                target / "Library/Preferences/com.apple.ScreenTimeAgent.plist",
                target / "com.apple.restrictionspassword.plist",
                target / "com.apple.ScreenTimeAgent.plist",
                target / "screentime.hash.json",
            ]
        )

    for candidate in candidate_files:
        if not candidate.exists():
            continue
        try:
            if candidate.suffix.lower() == ".json":
                data = json.loads(candidate.read_text())
            else:
                with candidate.open("rb") as handle:
                    data = plistlib.load(handle)

            salt = _to_hex(_find_value(data, ("RestrictionsPasswordSalt", "PasswordSalt", "salt")))
            digest = _to_hex(_find_value(data, ("RestrictionsPasswordKey", "PasswordHash", "hash", "Key")))
            iterations = _find_value(data, ("RestrictionsPasswordIterations", "Iterations", "iteration_count"))

            if salt and digest:
                parsed_iterations = int(iterations) if iterations is not None else 1000
                return {
                    "version": "iOS Screen Time / Restrictions",
                    "algorithm": "PBKDF2",
                    "iterations": parsed_iterations,
                    "salt": salt,
                    "hash": digest,
                    "hashcat_mode": 14800,
                    "source": str(candidate),
                }
        except Exception as exc:  # noqa: BLE001 - bubble parse failure as structured error
            return {"error": f"Failed to parse {candidate}: {exc}"}

    return {"error": f"No Screen Time hash source found under {target}"}


def extract_screentime_hash(backup_path: str) -> dict[str, object]:
    return extract_hash(backup_path)


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "screentime-hash":
        print(json.dumps(extract_screentime_hash(sys.argv[2])))
