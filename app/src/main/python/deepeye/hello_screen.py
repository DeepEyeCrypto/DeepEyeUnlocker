"""
DeepEye Hello Screen Bypass
Supports A7–A17 (iOS 15.x–18.x)
Reference: iRemoval GSMG method
"""
import hashlib
import struct
import json
from enum import Enum

class BypassMethod(Enum):
    IREMOVAL_PRO   = "iremoval_pro"     # A9–A11, iOS 16-17
    CHECKM8        = "checkm8"          # A7–A11 (hardware)
    MDM_REMOVAL    = "mdm_removal"      # MDM enrolled
    SERVER_BYPASS  = "server_bypass"    # IMEI server method

class ChipFamily(Enum):
    A7  = 0x8960  # iPhone 5s
    A8  = 0x7000  # iPhone 6/6+
    A9  = 0x8003  # iPhone 6s/SE1
    A10 = 0x8010  # iPhone 7/8
    A11 = 0x8015  # iPhone X/8P
    A12 = 0x8020  # iPhone XS/XR
    A13 = 0x8030  # iPhone 11
    A14 = 0x8101  # iPhone 12
    A15 = 0x8110  # iPhone 13/14
    A16 = 0x8120  # iPhone 14 Pro
    A17 = 0x8130  # iPhone 15 Pro

CHIP_BYPASS_MAP = {
    ChipFamily.A7:  [BypassMethod.CHECKM8],
    ChipFamily.A8:  [BypassMethod.CHECKM8],
    ChipFamily.A9:  [BypassMethod.CHECKM8,
                     BypassMethod.IREMOVAL_PRO],
    ChipFamily.A10: [BypassMethod.CHECKM8,
                     BypassMethod.IREMOVAL_PRO],
    ChipFamily.A11: [BypassMethod.CHECKM8,
                     BypassMethod.IREMOVAL_PRO],
    ChipFamily.A12: [BypassMethod.SERVER_BYPASS,
                     BypassMethod.MDM_REMOVAL],
    ChipFamily.A13: [BypassMethod.SERVER_BYPASS,
                     BypassMethod.MDM_REMOVAL],
    ChipFamily.A14: [BypassMethod.SERVER_BYPASS],
    ChipFamily.A15: [BypassMethod.SERVER_BYPASS],
    ChipFamily.A16: [BypassMethod.SERVER_BYPASS],
    ChipFamily.A17: [BypassMethod.SERVER_BYPASS],
}

def detect_chip_from_model(model: str) -> dict:
    """
    Map iPhone model to chip info.
    model: e.g. 'iPhone12,1' or 'iPhone14,2'
    """
    model_chip_map = {
        # iPhone 5s
        "iPhone6,1":  {"chip": "A7",  "id": 0x8960},
        "iPhone6,2":  {"chip": "A7",  "id": 0x8960},
        # iPhone 6/6+
        "iPhone7,1":  {"chip": "A8",  "id": 0x7000},
        "iPhone7,2":  {"chip": "A8",  "id": 0x7000},
        # iPhone 6s/SE1
        "iPhone8,1":  {"chip": "A9",  "id": 0x8003},
        "iPhone8,4":  {"chip": "A9",  "id": 0x8003},
        # iPhone 7/7+
        "iPhone9,1":  {"chip": "A10", "id": 0x8010},
        "iPhone9,3":  {"chip": "A10", "id": 0x8010},
        # iPhone X/8/8+
        "iPhone10,1": {"chip": "A11", "id": 0x8015},
        "iPhone10,3": {"chip": "A11", "id": 0x8015},
        # iPhone XS/XR
        "iPhone11,2": {"chip": "A12", "id": 0x8020},
        "iPhone11,8": {"chip": "A12", "id": 0x8020},
        # iPhone 11
        "iPhone12,1": {"chip": "A13", "id": 0x8030},
        "iPhone12,3": {"chip": "A13", "id": 0x8030},
        # iPhone 12
        "iPhone13,2": {"chip": "A14", "id": 0x8101},
        "iPhone13,4": {"chip": "A14", "id": 0x8101},
        # iPhone 13
        "iPhone14,2": {"chip": "A15", "id": 0x8110},
        "iPhone14,5": {"chip": "A15", "id": 0x8110},
        # iPhone 14 Pro
        "iPhone15,2": {"chip": "A16", "id": 0x8120},
        "iPhone15,3": {"chip": "A16", "id": 0x8120},
        # iPhone 15 Pro
        "iPhone16,1": {"chip": "A17", "id": 0x8130},
        "iPhone16,2": {"chip": "A17", "id": 0x8130},
    }
    info = model_chip_map.get(model, {
        "chip": "Unknown", "id": 0x0000
    })
    # Add bypass methods
    for chip_family in ChipFamily:
        if chip_family.name == info.get("chip", ""):
            methods = CHIP_BYPASS_MAP.get(chip_family, [])
            info["bypass_methods"] = [m.value for m in methods]
            info["checkm8_supported"] = BypassMethod.CHECKM8 \
                in methods
            break
    return info

def get_ios_bypass_eligibility(
    model: str,
    ios_version: str,
    is_jailbroken: bool = False
) -> dict:
    """
    Full eligibility check for Hello Screen bypass.
    Returns recommended method + instructions.
    """
    chip_info = detect_chip_from_model(model)
    chip_name = chip_info.get("chip", "Unknown")
    methods   = chip_info.get("bypass_methods", [])
    checkm8   = chip_info.get("checkm8_supported", False)

    # Parse iOS version
    try:
        major = int(ios_version.split(".")[0])
    except Exception:
        major = 0

    # Determine best method
    if checkm8 and major <= 17:
        best = "checkm8"
        instructions = [
            "Put iPhone in DFU mode",
            "Connect via USB-OTG cable",
            "Run checkm8 exploit",
            "Install bypass package",
            "Activate via DeepEye server"
        ]
        success_rate = 98
    elif "iremoval_pro" in methods and major <= 17:
        best = "iremoval_pro"
        instructions = [
            "Connect device via USB-OTG",
            "DeepEye runs iRemoval script",
            "Hello Screen bypassed in ~2min",
            "Device usable (SIM may need IMEI bypass)"
        ]
        success_rate = 94
    elif "server_bypass" in methods:
        best = "server_bypass"
        instructions = [
            "Enter IMEI in Server Method tab",
            "Select carrier & iOS version",
            "Submit to DeepEye server",
            "Wait 1-24 hours for activation token",
            "Apply token via DeepEye tool"
        ]
        success_rate = 85
    else:
        best = "unsupported"
        instructions = ["Device not supported"]
        success_rate = 0

    return json.dumps({
        "model": model,
        "chip": chip_name,
        "ios_version": ios_version,
        "checkm8_supported": checkm8,
        "best_method": best,
        "all_methods": methods,
        "success_rate": success_rate,
        "instructions": instructions,
        "eligible": best != "unsupported"
    })

def build_iremoval_payload(
    udid: str,
    model: str,
    ios_version: str,
    session_id: str
) -> str:
    """Build iRemoval GSMG compatible payload."""
    chip_info = detect_chip_from_model(model)
    token = hashlib.sha256(
        f"{udid}:{model}:{ios_version}:{session_id}".encode()
    ).hexdigest()
    payload = {
        "version":    "iRemoval-v3",
        "udid":       udid,
        "model":      model,
        "chip":       chip_info.get("chip", "Unknown"),
        "ios":        ios_version,
        "token":      token,
        "session_id": session_id,
        "timestamp":  __import__('time').time()
    }
    return json.dumps(payload)
