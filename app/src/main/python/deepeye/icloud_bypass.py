"""
DeepEye iCloud Bypass Engine
Methods: DNS bypass, PLIST token, checkm8,
         server activation, Apple ID removal
Reference: PLIST security chain architecture
"""
import hashlib
import json
import struct
import time
from enum import Enum

class ActivationState(Enum):
    NOT_ACTIVATED      = "not_activated"
    ACTIVATION_REQUIRED= "activation_required"
    ICLOUD_LOCKED      = "icloud_locked"
    MDM_LOCKED         = "mdm_locked"
    ACTIVATED          = "activated"
    BYPASS_APPLIED     = "bypass_applied"

class ICloudBypassMethod(Enum):
    DNS_BYPASS         = "dns_bypass"      # Free, partial
    PLIST_TOKEN        = "plist_token"     # PLIST activation
    CHECKM8_BYPASS     = "checkm8_bypass"  # A7-A11 hardware
    SERVER_ACTIVATION  = "server_activation" # IMEI server
    APPLE_ID_REMOVAL   = "apple_id_removal"  # Remove linked ID

# Bypass DNS servers (redirect activation to bypass server)
BYPASS_DNS_SERVERS = {
    "primary":   "208.67.222.123",   # OpenDNS bypass
    "secondary": "78.109.17.60",     # iCloud bypass DNS
    "deepeye":   "bypass.deepeye.io" # DeepEye custom DNS
}

# PLIST activation record structure
ACTIVATION_PLIST_TEMPLATE = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>ActivationState</key>
    <string>Activated</string>
    <key>ActivationStateAcknowledged</key>
    <true/>
    <key>AccountToken</key>
    <string>{account_token}</string>
    <key>AccountTokenCertificate</key>
    <string>{cert}</string>
    <key>UniqueDeviceID</key>
    <string>{udid}</string>
    <key>DeviceCertificate</key>
    <string>{device_cert}</string>
    <key>FMiPAccountToken</key>
    <string></string>
    <key>InternationalMobileEquipmentIdentity</key>
    <string>{imei}</string>
    <key>ActivationInfoXML</key>
    <string>{activation_info}</string>
</dict>
</plist>"""

def get_bypass_methods_for_device(
    chip: str,
    ios_major: int,
    is_find_my_enabled: bool,
    has_imei: bool
) -> list:
    """
    Return ordered list of bypass methods (best first).
    chip: A7-A17
    ios_major: 15/16/17/18
    """
    checkm8_chips = ["A7","A8","A9","A10","A11"]
    methods = []

    if chip in checkm8_chips:
        methods.append({
            "method":       ICloudBypassMethod.CHECKM8_BYPASS.value,
            "success_rate": 97,
            "time":         "5-10 min",
            "requires":     ["USB-OTG cable", "DFU mode"],
            "limitation":   "SIM calls limited on some carriers",
            "free":         True
        })

    if has_imei:
        methods.append({
            "method":       ICloudBypassMethod.SERVER_ACTIVATION.value,
            "success_rate": 88,
            "time":         "1-48 hours",
            "requires":     ["IMEI", "Internet"],
            "limitation":   "Paid service, device must be online",
            "free":         False
        })

    methods.append({
        "method":       ICloudBypassMethod.DNS_BYPASS.value,
        "success_rate": 60,
        "time":         "2-5 min",
        "requires":     ["WiFi access"],
        "limitation":   "Partial — calls/SMS may not work",
        "free":         True
    })

    if not is_find_my_enabled:
        methods.append({
            "method":       ICloudBypassMethod.PLIST_TOKEN.value,
            "success_rate": 75,
            "time":         "10-20 min",
            "requires":     ["UDID", "IMEI", "Serial"],
            "limitation":   "Find My must be disabled",
            "free":         False
        })

    return methods

def generate_activation_plist(
    udid: str,
    imei: str,
    serial: str,
    session_id: str
) -> str:
    """
    Generate fake PLIST activation record.
    This is the PLIST token used for bypass.
    Reference: PLIST security chain icon (gsmgermany)
    """
    # Generate tokens
    account_token = hashlib.sha256(
        f"{udid}:{imei}:{session_id}:deepeye_token".encode()
    ).hexdigest()
    device_cert = hashlib.sha256(
        f"{serial}:{udid}:cert".encode()
    ).hexdigest()
    activation_info = hashlib.md5(
        f"{udid}:{imei}:{time.time()}".encode()
    ).hexdigest().upper()

    plist = ACTIVATION_PLIST_TEMPLATE.format(
        account_token   = account_token,
        cert            = f"MIIBIjANBgkq{account_token[:32]}",
        udid            = udid,
        device_cert     = device_cert,
        imei            = imei,
        activation_info = activation_info
    )
    return plist

def parse_activation_plist(plist_str: str) -> dict:
    """
    Parse existing activation PLIST from device.
    Determine if device is iCloud locked.
    """
    result = {
        "activation_state":  ActivationState.NOT_ACTIVATED.value,
        "udid":              "",
        "imei":              "",
        "account_token":     "",
        "find_my_enabled":   False,
        "icloud_account":    "",
        "is_locked":         False,
        "error":             None
    }
    if not plist_str:
        result["error"] = "Empty PLIST"
        return result

    lines = plist_str.split('\n')
    for i, line in enumerate(lines):
        next_line = lines[i+1].strip() if i+1 < len(lines) else ""

        if "ActivationState" in line:
            val = next_line.replace("<string>","") \
                           .replace("</string>","")
            if "Activated" in val:
                result["activation_state"] = \
                    ActivationState.ACTIVATED.value
            elif "iCloud" in val or "locked" in val.lower():
                result["activation_state"] = \
                    ActivationState.ICLOUD_LOCKED.value
                result["is_locked"] = True

        if "UniqueDeviceID" in line:
            result["udid"] = next_line \
                .replace("<string>","").replace("</string>","")

        if "InternationalMobile" in line:
            result["imei"] = next_line \
                .replace("<string>","").replace("</string>","")

        if "FMiP" in line and "<false/>" not in next_line:
            result["find_my_enabled"] = True

        if "AppleID" in line or "accountName" in line.lower():
            result["icloud_account"] = next_line \
                .replace("<string>","").replace("</string>","")

    return result

def build_dns_bypass_config(
    ssid: str,
    session_id: str
) -> dict:
    """
    Build WiFi DNS bypass configuration.
    User sets these DNS on their WiFi router/phone.
    """
    return {
        "ssid":        ssid,
        "dns_primary": BYPASS_DNS_SERVERS["primary"],
        "dns_secondary":BYPASS_DNS_SERVERS["secondary"],
        "deepeye_dns": BYPASS_DNS_SERVERS["deepeye"],
        "instructions": [
            f"1. Connect iPhone to WiFi: {ssid}",
            "2. Go to WiFi settings → tap (i) icon",
            "3. Tap 'Configure DNS' → Manual",
            f"4. Remove existing DNS",
            f"5. Add: {BYPASS_DNS_SERVERS['primary']}",
            f"6. Add: {BYPASS_DNS_SERVERS['secondary']}",
            "7. Save → Go back → tap 'Activation Help'",
            "8. Wait 30 seconds → bypass active",
        ],
        "session_id": session_id,
        "method": "dns_bypass",
        "note": "Partial bypass — iMessage/FaceTime may not work"
    }

def calculate_bypass_score(
    chip: str,
    ios_major: int,
    has_imei: bool,
    find_my_on: bool
) -> dict:
    """
    Score overall bypass difficulty 1-10.
    1 = easy, 10 = nearly impossible
    """
    score = 5  # base
    checkm8_chips = ["A7","A8","A9","A10","A11"]

    if chip in checkm8_chips:   score -= 3  # Much easier
    if ios_major >= 18:          score += 2  # Harder on iOS 18
    if find_my_on:               score += 2  # Harder with FMiP
    if has_imei:                 score -= 1  # Server option helps
    if ios_major <= 15:          score -= 1  # Older = easier

    score = max(1, min(10, score))
    difficulty = (
        "🟢 Easy"    if score <= 3 else
        "🟡 Medium"  if score <= 6 else
        "🔴 Hard"    if score <= 8 else
        "⛔ Very Hard"
    )
    return {
        "score":      score,
        "difficulty": difficulty,
        "chip":       chip,
        "ios":        ios_major,
        "find_my":    find_my_on,
    }
