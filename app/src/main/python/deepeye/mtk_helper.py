import struct

HW_CODE_MAP = {
    0x1209: "MT6835T (Dimensity 6300) — Realme 14x",
    0x6580: "MT6580",
    0x6739: "MT6739",
    0x6765: "MT6765 (Helio G85)",
    0x6785: "MT6785 (Helio G95)",
    0x6833: "MT6833 (Dimensity 700)",
    0x6877: "MT6877 (Dimensity 900)",
    0x6983: "MT6983 (Dimensity 9000)",
}

def identify_chip(hw_code: int) -> str:
    return HW_CODE_MAP.get(hw_code, f"Unknown: 0x{hw_code:04X}")

def build_handshake_packet() -> bytes:
    """MTK BROM handshake sequence."""
    return bytes([0xA0, 0x0A, 0x50, 0x05])

def parse_hw_response(data: bytes) -> dict:
    """Parse BROM HW code response."""
    if len(data) < 2:
        return {"valid": False, "hw_code": 0}
    hw_code = struct.unpack_from('>H', data[:2])[0]
    return {
        "valid": True,
        "hw_code": hw_code,
        "chip_name": identify_chip(hw_code),
        "hex": f"0x{hw_code:04X}"
    }
