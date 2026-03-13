import subprocess
import json
import sys
import time

def poll_usb_mode():
    """
    Polls the connected device mode and returns the current stage.
    """
    try:
        # Check Normal/Recovery/Restore mode
        try:
            res = subprocess.check_output(["idevice_id", "-l"]).decode().strip()
            if res:
                # Get mode via ideviceinfo
                info = subprocess.check_output(["ideviceinfo", "-s"]).decode().strip()
                if "Recovery" in info: return "RECOVERY"
                return "NORMAL"
        except: pass

        # Check DFU mode (irecovery)
        try:
            res = subprocess.check_output(["irecovery", "-q"]).decode().strip()
            if "CPID" in res:
                if "PWND" in res.upper(): return "PWNDFU"
                return "DFU"
        except: pass

        return "DISCONNECTED"
    except Exception as e:
        return f"ERROR: {str(e)}"

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "poll":
            print(json.dumps({"mode": poll_usb_mode()}))
