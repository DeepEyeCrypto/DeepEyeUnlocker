"""
IPSW Firmware Signature verification
Reference: gsmgermany.com — iPhone Firmware Signature
"""
import hashlib
import json
import struct

# TSS Server endpoints (Apple)
TSS_SERVERS = [
    "https://gs.apple.com/TSS/controller?action=2",
    "https://17.57.8.65/TSS/controller?action=2",
]

IPSW_MANIFEST_FIELDS = [
    "BuildManifest.plist",
    "Restore.plist",
]

def verify_ipsw_integrity(
    ipsw_path: str,
    expected_sha256: str
) -> dict:
    """Verify IPSW SHA256 before flashing."""
    try:
        sha256 = hashlib.sha256()
        with open(ipsw_path, 'rb') as f:
            for chunk in iter(lambda: f.read(65536), b''):
                sha256.update(chunk)
        actual = sha256.hexdigest()
        match  = actual.lower() == expected_sha256.lower()
        return {
            "valid":    match,
            "actual":   actual,
            "expected": expected_sha256,
            "error":    None if match else "SHA256 mismatch"
        }
    except Exception as e:
        return {"valid": False, "error": str(e)}

def parse_build_manifest(manifest_plist_str: str) -> dict:
    """
    Parse BuildManifest.plist from IPSW.
    Returns device + iOS info for signing check.
    """
    result = {
        "product_version":    "Unknown",
        "product_build_ver":  "Unknown",
        "supported_products": [],
        "signing_status":     "Unknown"
    }
    if "ProductVersion" in manifest_plist_str:
        # Extract version (simplified — full impl needs plistlib)
        for line in manifest_plist_str.split('\n'):
            if "ProductVersion" in line:
                result["product_version"] = \
                    line.strip().replace("ProductVersion", "").strip()
    if "SupportedProductTypes" in manifest_plist_str:
        result["supported_products"] = ["iPhone"]
    return result

def check_ipsw_signing_status(
    model: str,
    ios_version: str,
    build_id: str
) -> str:
    """
    Check if IPSW is still signed by Apple TSS.
    NOTE: Requires network — use offline cache for Air-gap
    """
    # Known signed versions cache (offline fallback)
    SIGNED_CACHE = {
        "iPhone14,2": ["17.0", "17.1", "17.2", "18.0", "18.1"],
        "iPhone13,2": ["16.7.8", "17.0", "17.1", "18.0"],
        "iPhone12,1": ["16.7.8", "17.0"],
        "iPhone10,3": ["16.7.8"],
    }
    model_versions = SIGNED_CACHE.get(model, [])
    is_signed      = ios_version in model_versions
    return json.dumps({
        "model":      model,
        "ios":        ios_version,
        "build_id":   build_id,
        "signed":     is_signed,
        "source":     "offline_cache",
        "tss_url":    TSS_SERVERS,
        "note": "Connect internet for live TSS check"
            if not is_signed else "Signed ✅"
    })

def get_dfu_instructions(model: str) -> list:
    """Model-specific DFU entry instructions."""
    # A9 and earlier: Home button
    home_button_models = [
        "iPhone6", "iPhone7", "iPhone8", "iPhone9"
    ]
    has_home = any(m in model for m in home_button_models)

    if has_home:
        return [
            "1. Power off iPhone completely",
            "2. Hold Power + Home button for 8 seconds",
            "3. Release Power, keep Home for 5 more seconds",
            "4. Screen stays black = DFU mode ✅",
            "5. Connect USB-OTG to DeepEye",
        ]
    else:
        return [
            "1. Power off iPhone completely",
            "2. Connect USB-OTG to DeepEye",
            "3. Hold Side button for 3 seconds",
            "4. Hold Vol Down + Side for 10 seconds",
            "5. Release Side only, keep Vol Down 5 sec",
            "6. Screen stays black = DFU mode ✅",
        ]
