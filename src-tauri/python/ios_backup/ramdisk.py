import subprocess
import json
import sys
import os

def check_pwned_dfu(udid=None):
    """
    Check if the device is in pwned DFU mode using gaster or irecovery.
    """
    try:
        # irecovery -q
        cmd = ["irecovery", "-q"]
        if udid:
            cmd.extend(["-u", udid])
            
        output = subprocess.check_output(cmd).decode().strip()
        data = {}
        for line in output.split("\n"):
            if ":" in line:
                k, v = line.split(":", 1)
                data[k.strip()] = v.strip()
        
        # Check for pwnd prefix in CPID or other keys
        is_pwned = "PWND" in output.upper() or data.get("PWND", "") != ""
        
        return {
            "pwned": is_pwned,
            "cpid": data.get("CPID", "Unknown"),
            "model": data.get("PRODUCT", "Unknown")
        }
    except Exception as e:
        return {"error": str(e)}

def run_gaster_pwn(udid=None):
    """
    Trigger checkm8 exploitation via gaster.
    """
    try:
        cmd = ["gaster", "pwn"]
        # gaster usually targets the first found DFU device
        output = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode().strip()
        return {"success": "Done" in output or "pwned!" in output.lower(), "output": output}
    except Exception as e:
        return {"success": False, "error": str(e)}

def load_ramdisk(ramdisk_path, udid=None):
    """
    Load a custom ramdisk via gaster or irecovery.
    """
    try:
        # Example: gaster boot
        cmd = ["gaster", "boot"]
        # This assumes the ramdisk is bundled or provided
        output = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode().strip()
        return {"success": True, "output": output}
    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "pwn-state":
            print(json.dumps(check_pwned_dfu()))
        elif sys.argv[1] == "gaster-pwn":
            print(json.dumps(run_gaster_pwn()))
