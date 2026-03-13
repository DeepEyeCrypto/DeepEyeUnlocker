import plistlib
import json
import os
import sys

def parse_activation_record(path):
    """
    Parses an ActivationRecord.plist and extracts forensic markers.
    """
    if not os.path.exists(path):
        return {"error": f"File not found: {path}"}
    
    try:
        with open(path, 'rb') as f:
            data = plistlib.load(f)
            
            # Common markers in activation records
            record = {
                "ActivationState": data.get("ActivationState"),
                "BasebandCertId": data.get("BasebandCertId"),
                "BasebandSerialNumber": data.get("BasebandSerialNumber", "").hex() if isinstance(data.get("BasebandSerialNumber"), bytes) else data.get("BasebandSerialNumber"),
                "IMEI": data.get("IMEI"),
                "SerialNumber": data.get("SerialNumber"),
                "UniqueChipID": hex(data.get("UniqueChipID")) if isinstance(data.get("UniqueChipID"), int) else data.get("UniqueChipID"),
                "FairPlayKeyID": data.get("FairPlayKeyID", "").hex() if isinstance(data.get("FairPlayKeyID"), bytes) else data.get("FairPlayKeyID"),
                "WildcardTicket": "WildcardTicket" in data
            }
            return record
    except Exception as e:
        return {"error": str(e)}

def find_backup_tickets(backup_path):
    """
    Scan a backup for potential activation records.
    Simplified: expects path to /Library/activation_records/ if extracted.
    """
    results = []
    if not os.path.isdir(backup_path):
        return results
        
    for root, dirs, files in os.walk(backup_path):
        for file in files:
            if file.endswith(".plist") and "Activation" in file:
                full_path = os.path.join(root, file)
                parsed = parse_activation_record(full_path)
                if "error" not in parsed:
                    parsed["file_path"] = full_path
                    results.append(parsed)
    return results

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "parse-ticket":
            print(json.dumps(parse_activation_record(sys.argv[2])))
        elif sys.argv[1] == "scan-tickets":
            print(json.dumps(find_backup_tickets(sys.argv[2])))
