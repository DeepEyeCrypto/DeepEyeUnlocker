import json
import subprocess
import sys

def dfu_detect_mode(udid=None):
    """
    Detect device mode: normal, recovery, dfu, or unknown.
    Uses libimobiledevice and irecovery.
    """
    # 1. Check for normal mode
    try:
        cmd = ["idevice_id", "-l"]
        output = subprocess.check_output(cmd).decode().strip()
        if udid and udid in output:
            return "normal"
        elif not udid and output:
            return "normal"
    except:
        pass

    # 2. Check for Recovery / DFU via irecovery
    try:
        # irecovery -m returns: "Recovery Mode" or "DFU Mode"
        cmd = ["irecovery", "-m"]
        output = subprocess.check_output(cmd).decode().strip()
        if "Recovery" in output:
            return "recovery"
        if "DFU" in output:
            return "dfu"
    except:
        pass

    return "unknown"

def ipsw_select(model, ios_version):
    """
    Select the correct IPSW for a model and version.
    Returns path if exists in local cache, otherwise placeholder.
    """
    # This would typically query ipsw.me API
    # For now, return a placeholder or check ~/Documents/DeepEye/IPSW/
    return f"~/Documents/DeepEye/IPSW/{model}_{ios_version}_Restore.ipsw"

if __name__ == "__main__":
    # Bare minimum for CLI parity if called directly
    if len(sys.argv) > 1:
        if sys.argv[1] == "dfu-state":
            print(json.dumps({"mode": dfu_detect_mode()}))
