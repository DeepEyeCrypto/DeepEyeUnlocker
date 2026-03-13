import subprocess
import json
import sys

def check_hello_screen_state(udid=None):
    """
    Check if the device is on the Hello screen using ideviceactivation.
    """
    try:
        # ideviceactivation state check
        cmd = ["ideviceactivation", "state"]
        if udid:
            cmd.extend(["-u", udid])
            
        output = subprocess.check_output(cmd).decode().strip()
        
        # Output usually contains "ActivationState: Unactivated" or similar
        return {
            "on_hello_screen": "Unactivated" in output or "NotActivated" in output,
            "raw_state": output
        }
    except Exception as e:
        return {"error": str(e)}

def simulate_bypass_checkm8(udid):
    """
    Logic for checkm8 bypass (A7-A11).
    Requires gaster or checkra1n for exploitation step.
    """
    return {
        "exploitable": True,
        "method": "checkm8_ramdisk",
        "steps": [
            "PwnDFU Exploitation",
            "Upload Custom Ramdisk",
            "Mount Data Partition",
            "Patch ActivationRecord.plist"
        ]
    }

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "hello-state":
            udid = sys.argv[2] if len(sys.argv) > 2 else None
            print(json.dumps(check_hello_screen_state(udid)))
