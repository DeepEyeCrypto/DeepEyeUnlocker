"""
DeepEye Flash Method — DFU + Recovery flashing
Reference: gsmgermany.com Flash method
"""
import hashlib
import json
import struct
from enum import Enum

class FlashMode(Enum):
    DFU      = "dfu"       # Device Firmware Update
    RECOVERY = "recovery"  # Recovery mode
    NORMAL   = "normal"    # Normal boot (partial)

class FlashResult(Enum):
    SUCCESS  = "success"
    FAILED   = "failed"
    PARTIAL  = "partial"
    ABORTED  = "aborted"

# IPSW restore protocol stages
RESTORE_STAGES = [
    {"id": 0, "name": "Device Detection",   "weight": 5},
    {"id": 1, "name": "IPSW Verification",  "weight": 10},
    {"id": 2, "name": "Enter DFU/Recovery",  "weight": 5},
    {"id": 3, "name": "Send iBSS",           "weight": 15},
    {"id": 4, "name": "Send iBEC",           "weight": 15},
    {"id": 5, "name": "Send DeviceTree",     "weight": 5},
    {"id": 6, "name": "Send RestoreRamdisk","weight": 10},
    {"id": 7, "name": "Send KernelCache",   "weight": 10},
    {"id": 8, "name": "Flash NAND",         "weight": 20},
    {"id": 9, "name": "Verify & Reboot",    "weight": 5},
]

def get_restore_stages() -> list:
    """Return ordered list of flash stages with weights."""
    return RESTORE_STAGES

def calculate_stage_progress(
    current_stage: int,
    stage_byte_progress: float = 0.0
) -> float:
    """
    Calculate total restore progress (0.0 - 1.0).
    current_stage: index into RESTORE_STAGES
    stage_byte_progress: 0.0-1.0 within current stage
    """
    total_weight = sum(s["weight"] for s in RESTORE_STAGES)
    completed    = sum(
        s["weight"] for s in RESTORE_STAGES[:current_stage]
    )
    current_w    = RESTORE_STAGES[current_stage]["weight"] \
        if current_stage < len(RESTORE_STAGES) else 0
    progress = (completed + current_w * stage_byte_progress)
    return min(progress / total_weight, 1.0)

def validate_ipsw_components(component_list: list) -> dict:
    """
    Validate required IPSW components are present.
    component_list: list of filenames inside IPSW
    """
    required = [
        "BuildManifest.plist",
        "Restore.plist",
    ]
    optional = [
        "ibss", "ibec", "kernelcache",
        "DeviceTree", "RestoreRamdisk"
    ]
    missing   = []
    found_opt = []
    for req in required:
        if not any(req.lower() in c.lower()
                   for c in component_list):
            missing.append(req)
    for opt in optional:
        if any(opt.lower() in c.lower()
               for c in component_list):
            found_opt.append(opt)
    return {
        "valid":      len(missing) == 0,
        "missing":    missing,
        "found":      found_opt,
        "component_count": len(component_list)
    }

def build_tss_request(
    model: str,
    board_config: str,
    chip_id: int,
    ecid: str,
    ios_version: str,
    build_id: str
) -> dict:
    """
    Build Apple TSS (signing server) request payload.
    Used for personalized SHSH blob fetching.
    """
    nonce = hashlib.sha1(
        f"{ecid}:{board_config}".encode()
    ).hexdigest()
    return {
        "@HostIpAddress":    "0.0.0.0",
        "@HostPlatformInfo": "linux",
        "@Locality":         "en_US",
        "@VersionInfo":      "libauthinstall-1050.0.1",
        "ApBoardID":         board_config,
        "ApChipID":          f"0x{chip_id:08X}",
        "ApECID":            ecid,
        "ApNonce":           nonce,
        "ApProductionMode":  True,
        "ApSecurityDomain":  1,
        "BuildIdentity":     {
            "ApBoardID":     board_config,
            "ApChipID":      f"0x{chip_id:08X}",
            "ProductType":   model,
            "ProductVersion":ios_version,
            "BuildVersion":  build_id,
        }
    }

def parse_shsh_blob(blob_data: str) -> dict:
    """Parse saved SHSH blob for offline restore."""
    result = {
        "valid":     False,
        "ecid":      "",
        "ios":       "",
        "build_id":  "",
        "model":     "",
        "error":     None
    }
    if not blob_data:
        result["error"] = "Empty SHSH data"
        return result
    if "ApImg4Ticket" in blob_data:
        result["valid"]   = True
        result["source"]  = "TSS"
    elif "shsh" in blob_data.lower():
        result["valid"]   = True
        result["source"]  = "Cydia/SHSH1"
    else:
        result["error"]   = "Unknown SHSH format"
    return result
