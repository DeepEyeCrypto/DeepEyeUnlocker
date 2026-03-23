#!/usr/bin/env python3
"""Read iPhone info via libimobiledevice"""
import subprocess, json, sys

def read_device_info(session_id: str) -> dict:
    try:
        # Get UDID
        r = subprocess.run(
            ["idevice_id", "-l"], capture_output=True, text=True, timeout=5
        )
        udid = r.stdout.strip().split("\n")[0]
        if not udid:
            return {"error": "No device found — connect iPhone and trust computer"}

        # Get full info
        r2 = subprocess.run(
            ["ideviceinfo", "-u", udid],
            capture_output=True, text=True, timeout=5
        )

        info = {}
        for line in r2.stdout.splitlines():
            if ":" in line:
                k, _, v = line.partition(":")
                info[k.strip()] = v.strip()

        return {
            "udid":        udid,
            "deviceName":  info.get("DeviceName", ""),
            "iosVersion":  info.get("ProductVersion", ""),
            "build":       info.get("BuildVersion", ""),
            "productType": info.get("ProductType", ""),
            "chipId":      info.get("ChipID", ""),
            "ecid":        info.get("UniqueChipID", ""),
            "imei":        info.get("InternationalMobileEquipmentIdentity", ""),
            "serial":      info.get("SerialNumber", ""),
            "fmiStatus":   info.get("ActivationState", ""),
            "sessionId":   session_id,
        }
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    session_id = sys.argv[1] if len(sys.argv) > 1 else "unknown"
    print(json.dumps(read_device_info(session_id)))
