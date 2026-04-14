"""
DeepEye MDM Removal
Enterprise + School MDM bypass methods
"""
import hashlib
import json
from enum import Enum

class MdmType(Enum):
    APPLE_MDM    = "apple_mdm"       # Apple Business Manager
    JAMF         = "jamf"            # Jamf Pro
    MOSYLE       = "mosyle"          # Mosyle MDM
    MERAKI       = "meraki"          # Cisco Meraki
    MICROSOFT    = "microsoft_intune"
    UNKNOWN      = "unknown"

class MdmBypassMethod(Enum):
    NETWORK_BLOCK = "network_block"   # Block MDM server DNS
    PROFILE_DEL   = "profile_delete"  # Delete config profile
    CHECKM8_BYPASS= "checkm8_bypass"  # A7-A11 hardware bypass
    DEP_BYPASS    = "dep_bypass"      # Skip DEP enrollment
    SERVER_METHOD = "server_method"   # DeepEye server

MDM_SERVER_DOMAINS = {
    MdmType.JAMF:         ["*.jamfcloud.com", "*.jamf.com"],
    MdmType.MOSYLE:       ["*.mosyle.com", "business.mosyle.com"],
    MdmType.MERAKI:       ["*.meraki.com", "systems.meraki.com"],
    MdmType.MICROSOFT:    ["*.manage.microsoft.com",
                           "*.microsoftonline.com"],
    MdmType.APPLE_MDM:    ["*.apple.com", "mdmenrollment.apple.com"],
}

def detect_mdm_type(server_url: str) -> str:
    """Detect MDM vendor from server URL."""
    url_lower = server_url.lower()
    if "jamf"    in url_lower: return MdmType.JAMF.value
    if "mosyle"  in url_lower: return MdmType.MOSYLE.value
    if "meraki"  in url_lower: return MdmType.MERAKI.value
    if "intune"  in url_lower or "microsoft" in url_lower:
        return MdmType.MICROSOFT.value
    if "apple"   in url_lower: return MdmType.APPLE_MDM.value
    return MdmType.UNKNOWN.value

def get_bypass_method(
    mdm_type: str,
    chip_family: str,
    is_supervised: bool
) -> dict:
    """
    Determine best MDM bypass for device profile.
    chip_family: A7-A11 = checkm8 eligible
    """
    checkm8_chips = ["A7","A8","A9","A10","A11"]
    has_checkm8   = chip_family in checkm8_chips

    if has_checkm8 and not is_supervised:
        method = MdmBypassMethod.CHECKM8_BYPASS
        steps  = [
            "Run checkm8 exploit via DeepEye",
            "Install MDM bypass package",
            "Respring — MDM profile removed",
            "Device fully usable ✅"
        ]
        success = 96
    elif not is_supervised:
        method = MdmBypassMethod.NETWORK_BLOCK
        steps  = [
            "Block MDM server in hosts file",
            "Install custom APN config",
            "MDM check-in prevented",
            "Semi-bypass — enrollment still shows"
        ]
        success = 72
    elif is_supervised and has_checkm8:
        method = MdmBypassMethod.CHECKM8_BYPASS
        steps  = [
            "Device is supervised — full checkm8 needed",
            "Run DeepEye checkm8 bypass",
            "Flash modified ramdisk",
            "Remove supervision flag",
            "Re-activate via DeepEye server"
        ]
        success = 88
    else:
        method = MdmBypassMethod.SERVER_METHOD
        steps  = [
            "Submit IMEI to DeepEye server",
            "MDM removal token generated",
            "Apply token — 24-48 hour process",
            "Full MDM removal guaranteed"
        ]
        success = 80

    return {
        "method":       method.value,
        "steps":        steps,
        "success_rate": success,
        "is_supervised":is_supervised,
        "mdm_type":     mdm_type,
        "chip":         chip_family
    }

def parse_mdm_profile_plist(plist_str: str) -> dict:
    """
    Parse MDM configuration profile PLIST.
    (Reference: PLIST icon in gsmgermany.com)
    Extracts server URL, org name, supervision status.
    """
    result = {
        "server_url":     "",
        "org_name":       "",
        "is_supervised":  False,
        "profile_id":     "",
        "mdm_type":       "unknown",
        "removable":      True,
        "payload_count":  0
    }
    if not plist_str:
        return result

    # Basic PLIST field extraction
    lines = plist_str.split('\n')
    for i, line in enumerate(lines):
        if "ServerURL"    in line and i+1 < len(lines):
            result["server_url"] = lines[i+1].strip() \
                .replace("<string>","").replace("</string>","")
        if "OrganizationName" in line and i+1 < len(lines):
            result["org_name"] = lines[i+1].strip() \
                .replace("<string>","").replace("</string>","")
        if "IsSupervised" in line:
            result["is_supervised"] = "<true/>" in line \
                or (i+1 < len(lines) and "<true/>" in lines[i+1])
        if "PayloadUUID" in line and i+1 < len(lines):
            result["profile_id"] = lines[i+1].strip() \
                .replace("<string>","").replace("</string>","")
        if "PayloadContent" in line:
            result["payload_count"] += 1

    if result["server_url"]:
        result["mdm_type"] = detect_mdm_type(result["server_url"])

    return result

def generate_bypass_report(
    model: str,
    chip: str,
    mdm_type: str,
    is_supervised: bool,
    session_id: str
) -> str:
    """Generate full MDM bypass report as JSON string."""
    bypass    = get_bypass_method(mdm_type, chip, is_supervised)
    report_id = hashlib.md5(
        f"{model}:{session_id}".encode()
    ).hexdigest()[:8].upper()
    return json.dumps({
        "report_id":    f"DEY-MDM-{report_id}",
        "model":        model,
        "chip":         chip,
        "mdm_type":     mdm_type,
        "is_supervised":is_supervised,
        "bypass":       bypass,
        "session_id":   session_id,
        "tool":         "DeepEye Unlocker v2027.18"
    }, indent=2)
