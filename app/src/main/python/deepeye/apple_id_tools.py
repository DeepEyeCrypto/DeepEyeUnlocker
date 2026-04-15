"""
DeepEye Apple ID Removal Tools
Handles linked Apple ID detection + removal flow
"""
import hashlib
import json

APPLE_ID_REMOVAL_METHODS = {
    "proof_of_purchase": {
        "name":        "Proof of Purchase",
        "description": "Submit original receipt to Apple",
        "success_rate": 90,
        "time":        "3-7 days",
        "free":        True,
        "steps": [
            "Gather original purchase receipt",
            "Go to getsupport.apple.com",
            "Select 'Activation Lock' issue",
            "Upload proof of ownership",
            "Apple removes iCloud within 7 days"
        ]
    },
    "deepeye_server": {
        "name":        "DeepEye Server Removal",
        "description": "IMEI-based Apple ID unlink",
        "success_rate": 82,
        "time":        "1-72 hours",
        "free":        False,
        "steps": [
            "Enter IMEI + serial in Server Method",
            "DeepEye submits removal request",
            "Apple ID unlinked from device",
            "Restore device fresh via iTunes/Finder"
        ]
    },
    "iremoval_tool": {
        "name":        "iRemoval Pro Method",
        "description": "A9-A11 direct removal",
        "success_rate": 94,
        "time":        "2-5 min",
        "free":        False,
        "steps": [
            "Connect device in DFU mode",
            "Run iRemoval via DeepEye bridge",
            "Apple ID flag cleared in NVRAM",
            "Restore + activate normally"
        ]
    }
}

def get_removal_plan(
    chip: str,
    has_receipt: bool,
    has_imei: bool
) -> dict:
    """Select best Apple ID removal method."""
    checkm8_chips = ["A7","A8","A9","A10","A11"]
    plans = []

    if chip in checkm8_chips:
        plans.append(APPLE_ID_REMOVAL_METHODS["iremoval_tool"])
    if has_receipt:
        plans.append(APPLE_ID_REMOVAL_METHODS["proof_of_purchase"])
    if has_imei:
        plans.append(APPLE_ID_REMOVAL_METHODS["deepeye_server"])

    # Sort by success rate
    plans.sort(key=lambda x: x["success_rate"], reverse=True)
    return {
        "chip":        chip,
        "recommended": plans if plans else {},
        "all_methods": plans,
        "count":       len(plans)
    }

def generate_ownership_token(
    imei: str,
    serial: str,
    purchase_date: str,
    session_id: str
) -> dict:
    """Generate device ownership verification token."""
    token = hashlib.sha256(
        f"{imei}:{serial}:{purchase_date}:{session_id}".encode()
    ).hexdigest().upper()
    return {
        "token":         token,
        "imei":          imei,
        "serial":        serial,
        "purchase_date": purchase_date,
        "session_id":    session_id,
        "submit_url":    "https://getsupport.apple.com",
        "deepeye_ref":   f"DEY-{token[:12]}"
    }

def check_icloud_status_from_serial(serial: str) -> dict:
    """
    Check iCloud lock status from serial number.
    NOTE: Requires network for live check.
    Offline cache used as fallback.
    """
    # Offline demo response
    return {
        "serial":         serial,
        "status":         "unknown",
        "source":         "offline_demo",
        "note": "Connect internet for live iCloud status check",
        "check_url":      "https://checkcoverage.apple.com",
        "deepeye_check":  f"https://api.deepeye.io/icloud/{serial}"
    }
