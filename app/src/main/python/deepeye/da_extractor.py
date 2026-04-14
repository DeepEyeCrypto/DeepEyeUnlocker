import struct
import hashlib
import binascii

def extract_da_info(da_bytes: bytes) -> dict:
    """Extract Download Agent metadata from binary."""
    if len(da_bytes) < 512:
        return {"error": "DA too small", "valid": False}

    # DA header magic check
    magic = da_bytes[:4]
    if magic == b'\x4D\x54\x4B\x5F':  # MTK_
        return {
            "valid": True,
            "type": "MTK_DA",
            "size": len(da_bytes),
            "sha256": hashlib.sha256(da_bytes).hexdigest(),
            "version": struct.unpack_from('<H', da_bytes, 4)[0]
        }
    return {"valid": False, "error": "Unknown DA format"}


def validate_da_checksum(da_bytes: bytes) -> bool:
    """Verify DA checksum before JUMP_DA."""
    if len(da_bytes) < 8:
        return False
    stored_crc = struct.unpack_from('<I', da_bytes, -4)[0]
    calc_crc = binascii.crc32(da_bytes[:-4]) & 0xFFFFFFFF
    return stored_crc == calc_crc
