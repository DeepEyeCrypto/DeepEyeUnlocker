import plistlib
import os
import sys
import json

def detect_ticket_source(data):
    """
    Heuristics to detect if a ticket is Apple-signed or injected.
    """
    # Patched/Injected tickets often lack specific Apple signature fields 
    # or have hardcoded 'SetupDone=true' markers in the plist structure.
    has_sig = "AccountTokenSignature" in data or "Storefront" in data
    has_bypass_marker = "DeepEyeBypass" in str(data) or "F3arRa1n" in str(data)
    
    if has_bypass_marker: return "Injected"
    if has_sig: return "Apple"
    return "Patched"

def parse_activation_record(path):
    """
    MODULE 13: Advanced Ticket Parsing.
    """
    if not os.path.exists(path):
        return {"error": "File not found"}
    
    try:
        with open(path, 'rb') as f:
            data = plistlib.load(f)
            
            imei = data.get("IMEI")
            serial = data.get("SerialNumber")
            ecid = hex(data.get("UniqueChipID")) if isinstance(data.get("UniqueChipID"), int) else None
            
            source = detect_ticket_source(data)
            
            return {
                "imei": imei,
                "meid": data.get("MEID"),
                "serial": serial,
                "unique_device_id": data.get("UniqueDeviceID"),
                "device_class": data.get("DeviceClass"),
                "activation_state": data.get("ActivationState", "Unknown"),
                "ticket_present": True,
                "ticket_valid": True, # Structurally valid
                "ticket_source": source,
                "signed_fields": list(data.keys())
            }
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "parse-ticket":
        print(json.dumps(parse_activation_record(sys.argv[2])))
