"""Read-only Samsung Odin helper utilities for DeepEye."""

import json
import struct

SAMSUNG_VID = 0x04E8
ODIN_PIDS = {
    0x6601: "MTP + ADB",
    0x685D: "Download Mode (alternate)",
    0x6860: "Download Mode (Odin)",
    0xFFFF: "Engineering/Test Mode",
}

HEIMDALL_MAGIC = b"ODIN"
PIT_MAGIC = 0x12349876
PIT_ENTRY_SIZE = 132


class OdinSlot:
    BL = "BL"
    AP = "AP"
    CP = "CP"
    CSC = "CSC"


def _json(data) -> str:
    return json.dumps(data, indent=2)


def _normalize_file_list(file_list) -> list[str]:
    if isinstance(file_list, str):
        try:
            decoded = json.loads(file_list)
            if isinstance(decoded, list):
                return [str(item).strip() for item in decoded if str(item).strip()]
        except json.JSONDecodeError:
            items = []
            for raw in file_list.replace(",", "\n").splitlines():
                cleaned = raw.strip()
                if cleaned:
                    items.append(cleaned)
            return items

    if isinstance(file_list, (list, tuple)):
        return [str(item).strip() for item in file_list if str(item).strip()]

    return []


def detect_samsung_from_usb(vid: int, pid: int) -> str:
    """Return read-only Samsung USB detection metadata as JSON."""
    is_samsung = vid == SAMSUNG_VID
    is_download = pid in (0x6860, 0x685D)
    payload = {
        "vendor": "Samsung" if is_samsung else "Unknown",
        "vid": f"0x{vid:04X}",
        "pid": f"0x{pid:04X}",
        "mode": ODIN_PIDS.get(pid, "Unknown"),
        "is_download": is_download,
        "is_samsung": is_samsung,
        "action": "Read-only Odin analysis available" if is_download else "Connect a Samsung device in Odin download mode",
    }
    return _json(payload)


def parse_pit_binary(pit_data: bytes) -> str:
    """Parse a Samsung PIT binary blob into structured JSON."""
    if not pit_data or len(pit_data) < 8:
        return _json({"valid": False, "error": "PIT data too short"})

    try:
        magic = struct.unpack("<I", pit_data[:4])[0]
        if magic != PIT_MAGIC:
            return _json(
                {
                    "valid": False,
                    "error": f"Invalid PIT magic: 0x{magic:08X} (expected 0x{PIT_MAGIC:08X})",
                }
            )

        entry_count = struct.unpack("<I", pit_data[4:8])[0]
        entries = []
        offset = 8

        for index in range(entry_count):
            if offset + PIT_ENTRY_SIZE > len(pit_data):
                break

            entry = pit_data[offset : offset + PIT_ENTRY_SIZE]
            block_size = struct.unpack("<I", entry[20:24])[0]
            block_count = struct.unpack("<I", entry[24:28])[0]

            entries.append(
                {
                    "index": index,
                    "binary_type": struct.unpack("<I", entry[0:4])[0],
                    "device_type": struct.unpack("<I", entry[4:8])[0],
                    "identifier": struct.unpack("<I", entry[8:12])[0],
                    "attributes": struct.unpack("<I", entry[12:16])[0],
                    "update_attributes": struct.unpack("<I", entry[16:20])[0],
                    "blk_size": block_size,
                    "blk_count": block_count,
                    "file_offset": struct.unpack("<I", entry[28:32])[0],
                    "file_size": struct.unpack("<I", entry[32:36])[0],
                    "partition": entry[36:68].split(b"\x00")[0].decode("latin-1", errors="replace"),
                    "filename": entry[68:100].split(b"\x00")[0].decode("latin-1", errors="replace"),
                    "delta_name": entry[100:132].split(b"\x00")[0].decode("latin-1", errors="replace"),
                    "size_mb": round((block_size * block_count) / (1024**2), 2),
                }
            )
            offset += PIT_ENTRY_SIZE

        return _json(
            {
                "valid": True,
                "magic": f"0x{magic:08X}",
                "entry_count": entry_count,
                "parsed": len(entries),
                "entries": entries,
            }
        )
    except Exception as exc:
        return _json({"valid": False, "error": f"PIT parse error: {exc}"})


def build_heimdall_handshake() -> bytes:
    """Build the standard Heimdall/Odin handshake packet."""
    return HEIMDALL_MAGIC + (b"\x00" * 4)


def validate_odin_tar(file_list) -> str:
    """Validate Odin package slot coverage using filenames only."""
    files = _normalize_file_list(file_list)
    slots = {
        OdinSlot.BL: [],
        OdinSlot.AP: [],
        OdinSlot.CP: [],
        OdinSlot.CSC: [],
    }
    unrecognized = []

    for filename in files:
        lower = filename.lower()
        if any(token in lower for token in ("bl_", "bootloader", "sboot", "abl")):
            slots[OdinSlot.BL].append(filename)
        elif any(token in lower for token in ("ap_", "system", "boot", "vendor", "super")):
            slots[OdinSlot.AP].append(filename)
        elif any(token in lower for token in ("cp_", "modem", "radio", "non-hlos")):
            slots[OdinSlot.CP].append(filename)
        elif any(token in lower for token in ("home_csc", "csc_", "omc", "cache")):
            slots[OdinSlot.CSC].append(filename)
        elif lower.endswith(".pit"):
            continue
        else:
            unrecognized.append(filename)

    filled_slots = {slot: names for slot, names in slots.items() if names}
    payload = {
        "valid": bool(slots[OdinSlot.AP]) and len(filled_slots) >= 2,
        "slots": filled_slots,
        "missing_slots": [slot for slot, names in slots.items() if not names],
        "unrecognized": unrecognized,
        "total_files": len(files),
        "has_pit": any(name.lower().endswith(".pit") for name in files),
        "flash_type": "FULL_PACKAGE" if len(filled_slots) == 4 else "PARTIAL_PACKAGE",
    }
    return _json(payload)
