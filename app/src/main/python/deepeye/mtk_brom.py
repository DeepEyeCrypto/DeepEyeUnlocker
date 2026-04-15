"""
DeepEye MTK BROM Module
Purpose : READ-ONLY MediaTek device identification
Safe scope: detect chip, parse DA metadata, validate SP Flash XML
No write, no flash, no security circumvention
Reference: MediaTek BROM public protocol documentation
"""
import json
import struct
import hashlib
import xml.etree.ElementTree as ET
from enum import Enum

# ── USB Constants ────────────────────────────────────────────────
MTK_VID = 0x0E8D
MTK_BROM_PIDS = {
    0x0003: "BROM Mode (Boot ROM)",
    0x2000: "BROM Mode (alt)",
    0x0001: "Preloader Mode",
    0x2001: "Preloader Mode (alt)",
    0x201C: "Meta Mode",
    0x0023: "BROM (secured)",
}

# ── Chip Database ─────────────────────────────────────────────────
MTK_CHIP_DATABASE = {
    0x6580: {"name": "MT6580", "arch": "ARMv7", "cores": 4, "process": "28nm"},
    0x6735: {"name": "MT6735", "arch": "ARMv8", "cores": 4, "process": "28nm"},
    0x6737: {"name": "MT6737", "arch": "ARMv8", "cores": 4, "process": "28nm"},
    0x6739: {"name": "MT6739", "arch": "ARMv8", "cores": 4, "process": "28nm"},
    0x6750: {"name": "MT6750", "arch": "ARMv8", "cores": 8, "process": "28nm"},
    0x6753: {"name": "MT6753", "arch": "ARMv8", "cores": 8, "process": "28nm"},
    0x6755: {"name": "Helio P10", "arch": "ARMv8", "cores": 8, "process": "28nm"},
    0x6757: {"name": "Helio P20", "arch": "ARMv8", "cores": 8, "process": "16nm"},
    0x6759: {"name": "Helio P22", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6761: {"name": "Helio A22", "arch": "ARMv8", "cores": 4, "process": "12nm"},
    0x6762: {"name": "Helio G25", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6763: {"name": "Helio P35", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6765: {"name": "Helio G35", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6768: {"name": "Helio G85", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6769: {"name": "Helio G85 (v2)", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6771: {"name": "Helio P60", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6779: {"name": "Helio P90", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6781: {"name": "Helio G96", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6785: {"name": "Helio G90T", "arch": "ARMv8", "cores": 8, "process": "12nm"},
    0x6789: {"name": "Helio G99", "arch": "ARMv8", "cores": 8, "process": "6nm"},
    0x6795: {"name": "Helio X10", "arch": "ARMv8", "cores": 8, "process": "28nm"},
    0x6797: {"name": "Helio X25", "arch": "ARMv8", "cores": 10, "process": "20nm"},
    0x6799: {"name": "Helio X30", "arch": "ARMv8", "cores": 10, "process": "10nm"},
    0x6873: {"name": "Dimensity 800", "arch": "ARMv8", "cores": 8, "process": "7nm"},
    0x6875: {"name": "Dimensity 820", "arch": "ARMv8", "cores": 8, "process": "7nm"},
    0x6877: {"name": "Dimensity 700", "arch": "ARMv8", "cores": 8, "process": "7nm"},
    0x6885: {"name": "Dimensity 1000L", "arch": "ARMv8", "cores": 8, "process": "7nm"},
    0x6889: {"name": "Dimensity 1000+", "arch": "ARMv8", "cores": 8, "process": "7nm"},
    0x6893: {"name": "Dimensity 1100", "arch": "ARMv9", "cores": 8, "process": "6nm"},
    0x6895: {"name": "Dimensity 1200", "arch": "ARMv9", "cores": 8, "process": "6nm"},
    0x6983: {"name": "Dimensity 9000", "arch": "ARMv9", "cores": 8, "process": "4nm"},
    0x6985: {"name": "Dimensity 9200", "arch": "ARMv9", "cores": 8, "process": "4nm"},
    0x6989: {"name": "Dimensity 9300", "arch": "ARMv9", "cores": 8, "process": "4nm"},
    0x6991: {"name": "Dimensity 9400", "arch": "ARMv9", "cores": 8, "process": "3nm"},
}


class BromMode(Enum):
    BROM = "brom"
    PRELOADER = "preloader"
    META = "meta"
    UNKNOWN = "unknown"


def detect_mtk_from_usb(vid: int, pid: int) -> dict:
    """
    Detect MediaTek device from USB VID:PID.
    Returns device mode and connection status.
    READ-ONLY identification only.
    """
    is_mtk = vid == MTK_VID
    mode_str = MTK_BROM_PIDS.get(pid, "Unknown")
    is_brom = pid in (0x0003, 0x2000, 0x0023)
    is_pre = pid in (0x0001, 0x2001)
    is_meta = pid in (0x201C,)
    mode = (
        BromMode.BROM.value
        if is_brom
        else BromMode.PRELOADER.value
        if is_pre
        else BromMode.META.value
        if is_meta
        else BromMode.UNKNOWN.value
    )
    return {
        "vendor": "MediaTek" if is_mtk else "Unknown",
        "vid": f"0x{vid:04X}",
        "pid": f"0x{pid:04X}",
        "mode": mode,
        "mode_label": mode_str,
        "is_mtk": is_mtk,
        "is_brom": is_brom,
        "is_preloader": is_pre,
        "status": (
            "BROM detected — ready for identification"
            if is_brom
            else "Preloader mode — partial info available"
            if is_pre
            else "Not in BROM mode"
        ),
        "action": (
            "Read chip info"
            if (is_brom or is_pre)
            else "Hold Vol Down + connect USB for BROM"
        ),
    }


def identify_chip(chip_id: int) -> dict:
    """
    Identify MTK chip from hardware ID.
    Returns chip name, architecture, process node.
    """
    chip = MTK_CHIP_DATABASE.get(chip_id)
    if chip:
        return {
            "chip_id": f"0x{chip_id:04X}",
            "name": chip["name"],
            "arch": chip["arch"],
            "cores": chip["cores"],
            "process": chip["process"],
            "found": True,
            "series": (
                "Dimensity"
                if chip_id >= 0x6873
                else "Helio"
                if chip_id >= 0x6750
                else "MT Classic"
            ),
        }
    return {
        "chip_id": f"0x{chip_id:04X}",
        "name": f"MT{chip_id:04X}",
        "found": False,
        "note": "Unknown chip — check MTK database",
    }


def identify_chip_from_name(name: str) -> dict:
    """Fuzzy chip lookup by name string."""
    name_upper = name.upper().replace(" ", "")
    if not name_upper:
        return {"name": name, "found": False, "note": "Check scatter file for chip ID"}
    for cid, info in MTK_CHIP_DATABASE.items():
        chip_name = info["name"].upper().replace(" ", "")
        if name_upper in chip_name or chip_name in name_upper:
            return identify_chip(cid)
    return {"name": name, "found": False, "note": "Check scatter file for chip ID"}


def parse_scatter_file(scatter_text: str) -> dict:
    """
    Parse MTK scatter file (scatter.txt / MT6xxx_Android_scatter.txt).
    Returns partition list with addresses and sizes.
    READ-ONLY: extracts partition map metadata only.
    """
    partitions = []
    lines = scatter_text.splitlines()
    current = {}
    for line in lines:
        line = line.strip()
        if line.startswith("partition_name:"):
            if current:
                partitions.append(current)
            current = {"name": line.split(":", 1)[1].strip()}
        elif line.startswith("file_name:"):
            current["file"] = line.split(":", 1)[1].strip()
        elif line.startswith("linear_start_addr:"):
            current["start_addr"] = line.split(":", 1)[1].strip()
        elif line.startswith("partition_size:"):
            raw = line.split(":", 1)[1].strip()
            try:
                sz = int(raw, 16) if raw.startswith("0x") else int(raw)
                current["size_bytes"] = sz
                current["size_mb"] = round(sz / (1024**2), 2)
            except ValueError:
                current["size_raw"] = raw
        elif line.startswith("region:"):
            current["region"] = line.split(":", 1)[1].strip()
        elif line.startswith("storage:"):
            current["storage"] = line.split(":", 1)[1].strip()
        elif line.startswith("type:"):
            current["type"] = line.split(":", 1)[1].strip()
    if current and "name" in current:
        partitions.append(current)
    chip_line = next((l for l in lines if "MT" in l.upper() and "scatter" in l.lower()), "")
    return {
        "partitions": partitions,
        "partition_count": len(partitions),
        "chip_hint": chip_line.strip(),
        "has_userdata": any(p.get("name", "").lower() in ("userdata", "data") for p in partitions),
        "storage_type": partitions[0].get("storage", "emmc") if partitions else "unknown",
        "valid": len(partitions) > 0,
    }


def parse_da_loader_header(da_bytes: bytes) -> dict:
    """
    Parse DA (Download Agent) loader binary header.
    Extracts chip target, version, and supported chips.
    READ-ONLY metadata extraction.
    """
    if len(da_bytes) < 64:
        return {"error": "DA file too small"}
    try:
        # MTK DA magic: 0x58881688
        magic = struct.unpack("<I", da_bytes[:4])[0]
        if magic != 0x58881688:
            return {
                "magic": f"0x{magic:08X}",
                "valid": False,
                "error": "Invalid DA magic (expected 0x58881688)",
            }
        version = struct.unpack("<I", da_bytes[4:8])[0]
        chip_count = struct.unpack("<I", da_bytes[8:12])[0]
        da_version = (
            f"{(version >> 16) & 0xFF}."
            f"{(version >> 8) & 0xFF}."
            f"{version & 0xFF}"
        )
        supported = []
        offset = 12
        for _ in range(min(chip_count, 32)):
            if offset + 4 > len(da_bytes):
                break
            cid = struct.unpack("<I", da_bytes[offset : offset + 4])[0]
            chip_info = identify_chip(cid)
            supported.append(chip_info)
            offset += 4
        return {
            "magic": f"0x{magic:08X}",
            "valid": True,
            "da_version": da_version,
            "chip_count": chip_count,
            "supported_chips": supported,
            "file_size": len(da_bytes),
            "sha256": hashlib.sha256(da_bytes).hexdigest()[:16],
        }
    except Exception as e:
        return {"error": str(e), "valid": False}


def validate_spflash_xml(xml_str: str) -> dict:
    """
    Validate SP Flash Tool download XML.
    Checks structure and lists target partitions.
    READ-ONLY validation — no flash commands.
    """
    try:
        root = ET.fromstring(xml_str)
        if root.tag != "DOWNLOAD":
            return {"valid": False, "error": f"Root tag '{root.tag}' != 'DOWNLOAD'"}
        items = []
        for child in root:
            item = {
                "tag": child.tag,
                "part_name": child.get("part_name", ""),
                "filename": child.get("filename", ""),
                "start_addr": child.get("start_address", "0x0"),
                "type": child.get("type", ""),
                "enabled": child.get("is_download", "1") == "1",
            }
            items.append(item)
        enabled_count = sum(1 for item in items if item["enabled"])
        return {
            "valid": True,
            "total_items": len(items),
            "enabled_items": enabled_count,
            "items": items,
            "has_scatter": any("scatter" in item.get("filename", "").lower() for item in items),
            "storage_type": root.get("connection", "BROM_DOWNLOAD"),
        }
    except ET.ParseError as e:
        return {"valid": False, "error": str(e)}


def generate_mtk_device_report(
    vid: int,
    pid: int,
    chip_id_hex: str,
    scatter_text: str,
    session_id: str,
) -> str:
    """Generate full MTK device identification report."""
    usb_info = detect_mtk_from_usb(vid, pid)
    normalized_chip = chip_id_hex.strip()
    if normalized_chip:
        try:
            cid = int(normalized_chip, 16)
            chip_info = identify_chip(cid)
        except ValueError:
            chip_info = identify_chip_from_name(normalized_chip)
    else:
        chip_info = {"name": "", "found": False, "note": "No chip ID provided"}
    scatter = parse_scatter_file(scatter_text) if scatter_text.strip() else {}
    report_id = hashlib.sha256(f"{chip_id_hex}:{session_id}".encode()).hexdigest()[:8].upper()
    return json.dumps(
        {
            "report_id": f"DEY-MTK-{report_id}",
            "session_id": session_id,
            "tool": "DeepEye Unlocker v2027.18",
            "usb": usb_info,
            "chip": chip_info,
            "scatter": scatter,
            "note": "READ-ONLY identification report",
        },
        indent=2,
    )
