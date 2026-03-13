import subprocess
import json
import sys
import os

def get_device_identity(udid=None):
    """
    Extracts core identity markers: ECID, Serial, IMEI, Model, BoardID.
    """
    identity = {
        "ecid": "N/A",
        "serial": "N/A",
        "imei": "N/A",
        "meid": "N/A",
        "model": "N/A",
        "board_id": "N/A",
        "chip_id": "N/A",
        "product_version": "N/A",
        "class": "Unknown"
    }
    
    try:
        # 1. Try ideviceinfo for serial/imei/version
        cmd = ["ideviceinfo"]
        if udid: cmd.extend(["-u", udid])
        output = subprocess.check_output(cmd).decode().strip()
        
        for line in output.split("\n"):
            if ":" in line:
                k, v = line.split(":", 1)
                k = k.strip()
                v = v.strip()
                if k == "UniqueDeviceID": identity["udid"] = v
                elif k == "SerialNumber": identity["serial"] = v
                elif k == "InternationalMobileEquipmentIdentity": identity["imei"] = v
                elif k == "MobileEquipmentIdentifier": identity["meid"] = v
                elif k == "ProductType": identity["model"] = v
                elif k == "ProductVersion": identity["product_version"] = v
                elif k == "UniqueChipID": identity["ecid"] = v # Decimal ECID usually here
                elif k == "BoardId": identity["board_id"] = v
                elif k == "HardwareModel": identity["hardware_model"] = v

        # 2. Try irecovery for DFU-specific IDs if ideviceinfo failed or for extra info
        # This is useful when the device is in DFU mode.
        try:
            dfu_cmd = ["irecovery", "-q"]
            if udid: dfu_cmd.extend(["-u", udid])
            dfu_output = subprocess.check_output(dfu_cmd, stderr=subprocess.STDOUT).decode().strip()
            for line in dfu_output.split("\n"):
                if ":" in line:
                    k, v = line.split(":", 1)
                    k = k.strip(); v = v.strip()
                    if k == "ECID": identity["ecid_hex"] = v
                    elif k == "CPID": identity["chip_id"] = v
                    elif k == "BDID": identity["board_id"] = v
        except: pass

        # Classification
        if identity["imei"] != "N/A":
            identity["class"] = "GSM" if identity["meid"] == "N/A" else "Global/CDMA"
        
        return identity

    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "identity":
            udid = sys.argv[2] if len(sys.argv) > 2 else None
            print(json.dumps(get_device_identity(udid)))
