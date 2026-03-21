from __future__ import annotations

import plistlib
import sqlite3
from pathlib import Path


def parse_manifest(path: str) -> dict[str, object]:
    """Inspect an iOS backup manifest directory or manifest file and return a summary."""
    target = Path(path).expanduser().resolve()
    if not target.exists():
        return {"error": f"Backup path not found: {target}"}

    backup_root = target if target.is_dir() else target.parent
    manifest_db = backup_root / "Manifest.db"
    manifest_plist = backup_root / "Manifest.plist"
    info_plist = backup_root / "Info.plist"
    status_plist = backup_root / "Status.plist"

    result: dict[str, object] = {
        "backup_path": str(backup_root),
        "manifest_db": str(manifest_db) if manifest_db.exists() else None,
        "manifest_plist": str(manifest_plist) if manifest_plist.exists() else None,
        "info_plist": str(info_plist) if info_plist.exists() else None,
        "status_plist": str(status_plist) if status_plist.exists() else None,
        "file_count": 0,
        "domain_count": 0,
        "is_encrypted": None,
    }

    if manifest_db.exists():
        try:
            with sqlite3.connect(manifest_db) as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT COUNT(*) FROM Files")
                result["file_count"] = int(cursor.fetchone()[0])
                cursor.execute("SELECT COUNT(DISTINCT domain) FROM Files")
                result["domain_count"] = int(cursor.fetchone()[0])
        except sqlite3.Error as exc:
            result["manifest_db_error"] = str(exc)

    for plist_path in (manifest_plist, status_plist, info_plist):
        if not plist_path.exists():
            continue
        try:
            with plist_path.open("rb") as handle:
                data = plistlib.load(handle)
            if plist_path == manifest_plist:
                if "IsEncrypted" in data:
                    result["is_encrypted"] = bool(data.get("IsEncrypted"))
                result["lockdown"] = data.get("Lockdown", {})
            elif plist_path == status_plist:
                if "IsEncrypted" in data and result["is_encrypted"] is None:
                    result["is_encrypted"] = bool(data.get("IsEncrypted"))
                result["snapshot_state"] = data.get("SnapshotState")
            elif plist_path == info_plist:
                result["device_name"] = data.get("Device Name")
                result["product_type"] = data.get("Product Type")
                result["product_version"] = data.get("Product Version")
        except Exception as exc:  # noqa: BLE001 - surface plist parse failures in result payload
            result[f"{plist_path.name.lower()}_error"] = str(exc)

    return result