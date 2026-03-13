import subprocess
import json
import sys
import os

def luhn_check(imei):
    """
    Standard Luhn algorithm for IMEI validation.
    """
    if not imei or not imei.isdigit():
        return False
    digits = [int(d) for d in imei]
    total = 0
    for i, digit in enumerate(digits[::-1]):
        if i % 2 == 1:
            digit *= 2
            if digit > 9:
                digit -= 9
        total += digit
    return total % 10 == 0

def get_chip_generation(cpid):
    """
    Maps Chip ID (CPID) to Marketing Name.
    """
    db = {
        "0x8960": "A7",
        "0x7000": "A8", "0x7001": "A8X",
        "0x8000": "A9", "0x8003": "A9", "0x8001": "A9X",
        "0x8010": "A10", "0x8011": "A10X",
        "0x8012": "A10X", "0x8015": "A11 Bionic",
        "0x8101": "A12 Bionic", "0x8103": "M1",
        "0x8112": "A15 Bionic", "0x8020": "A12 Bionic",
        "0x8027": "A12 Bionic"
    }
    return db.get(cpid.lower(), f"Unknown ({cpid})")

def build_activation_matrix(udid=None):
    """
    MODULE 11: Comprehensive eligibility matrix builder.
    """
    from ios_backup.identity import get_device_identity
    identity = get_device_identity(udid)
    
    if "error" in identity: return identity
    
    version = identity.get("product_version", "0.0")
    major = int(version.split(".")[0]) if "." in version else 0
    minor = int(version.split(".")[1]) if "." in version and len(version.split(".")) > 1 else 0
    
    cpid = identity.get("chip_id", "0x0000")
    chip_gen = get_chip_generation(cpid)
    
    imei = identity.get("imei", "")
    imei_valid = luhn_check(imei) if imei != "N/A" else False
    
    meid = identity.get("meid", "N/A")
    is_meid = meid != "N/A" and any(meid.startswith(x) for x in ["A0", "A1", "8"])

    # Eligibility Logic
    eligible_types = []
    
    # 1. GSM_SIGNAL: iOS 13.0-14.5.1 + IMEI valid
    if 13 <= major <= 14 and (major < 14 or (major == 14 and minor <= 5)) and imei_valid:
        eligible_types.append("GsmSignal")
        
    # 2. NO_SIGNAL_TETHERED: A7-A11 (checkm8) + iOS 12-14
    is_checkm8 = any(x in cpid.upper() for x in ["0X8960", "0X7000", "0X7001", "0X8000", "0X8003", "0X8010", "0X8015"])
    if 12 <= major <= 14 and is_checkm8:
        eligible_types.append("NoSignalTethered")
        
    # 3. NO_SIGNAL_UNTETHERED: iOS 15-26.1
    if major >= 15:
        eligible_types.append("NoSignalUntethered")
        
    # 4. MDM_SKIP: iOS 15+
    if major >= 15:
        eligible_types.append("MdmSkip")
        
    # 5. TEMP_FREE: Always viable for test
    eligible_types.append("TempFree")

    # Recommendation
    recommended = "NoSignalUntethered"
    if "GsmSignal" in eligible_types: recommended = "GsmSignal"
    elif "MdmSkip" in eligible_types and major >= 15: recommended = "MdmSkip"

    return {
        "device_udid": identity.get("udid", udid),
        "chip_generation": chip_gen,
        "ios_version": version,
        "imei_present": imei != "N/A",
        "imei_valid": imei_valid,
        "is_meid_cdma": is_meid,
        "eligible_types": eligible_types,
        "recommended_type": recommended,
        "temp_test_viable": True
    }

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "activation-matrix":
            print(json.dumps(build_activation_matrix(sys.argv[2] if len(sys.argv) > 2 else None)))
