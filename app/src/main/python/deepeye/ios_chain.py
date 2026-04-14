import json
import hashlib

def build_activation_request(
    udid: str,
    imei: str,
    serial: str,
    model: str,
    ios_version: str
) -> dict:
    """Build activation request payload for Apple servers."""
    return {
        "UniqueDeviceID": udid,
        "InternationalMobileEquipmentIdentity": imei,
        "SerialNumber": serial,
        "ProductType": model,
        "ProductVersion": ios_version,
        "ActivationRandomness": hashlib.md5(
            udid.encode()
        ).hexdigest().upper(),
        "RKCertification": "",
        "FairPlayKeyData": "",
    }

def parse_activation_response(plist_str: str) -> dict:
    """Parse Apple activation server PLIST response."""
    result = {
        "activated": False,
        "activation_state": "Unknown",
        "account_token": "",
        "error": None
    }
    if "ActivationState" in plist_str:
        if "Activated" in plist_str:
            result["activated"] = True
            result["activation_state"] = "Activated"
    if "Error" in plist_str or "error" in plist_str:
        result["error"] = "Server returned error"
    return result

def compute_device_hash(udid: str, serial: str) -> str:
    """Compute device-specific hash for server registration."""
    combined = f"{udid}:{serial}:DeepEye:2027"
    return hashlib.sha256(combined.encode()).hexdigest()
