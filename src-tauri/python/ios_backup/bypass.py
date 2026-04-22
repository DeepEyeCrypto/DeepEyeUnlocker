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

def detect_checkm8_eligibility(udid):
    """
    Detect if device is eligible for checkm8 exploit (A7-A11 chips).
    Queries real device info via ideviceinfo to determine chip and exploitability.
    """
    try:
        cmd = ["ideviceinfo"]
        if udid:
            cmd.extend(["-u", udid])

        output = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode().strip()
        data = {}
        for line in output.split("\n"):
            if ":" in line:
                k, v = line.split(":", 1)
                data[k.strip()] = v.strip()

        product_type = data.get("ProductType", "Unknown")
        hw_model = data.get("HardwareModel", "Unknown")
        chip_id = data.get("ChipID", "Unknown")

        # checkm8 supported: A7 (s5l8960x) through A11 (t8015)
        # Product types: iPhone6,x through iPhone10,x
        checkm8_prefixes = [
            "iPhone6,", "iPhone7,", "iPhone8,", "iPhone9,", "iPhone10,",  # iPhone 5s-X
            "iPad4,", "iPad5,", "iPad6,", "iPad7,",                       # iPad Air-6th gen
            "iPod7,", "iPod9,",                                            # iPod Touch 6-7
        ]
        exploitable = any(product_type.startswith(p) for p in checkm8_prefixes)

        # Check if gaster or checkra1n are available
        tools_available = {}
        for tool in ["gaster", "checkra1n", "palera1n"]:
            try:
                subprocess.check_output(["which", tool], stderr=subprocess.STDOUT)
                tools_available[tool] = True
            except (subprocess.CalledProcessError, FileNotFoundError):
                tools_available[tool] = False

        return {
            "exploitable": exploitable,
            "product_type": product_type,
            "hw_model": hw_model,
            "chip_id": chip_id,
            "method": "checkm8_ramdisk" if exploitable else "not_supported",
            "tools_available": tools_available,
            "steps": [
                "PwnDFU Exploitation (gaster pwn)",
                "Upload Custom Ramdisk (gaster reset)",
                "Mount Data Partition (SSH)",
                "Patch ActivationRecord (ideviceactivation)"
            ] if exploitable else [],
        }
    except FileNotFoundError:
        return {"error": "ideviceinfo not installed. Install via: brew install libimobiledevice"}
    except subprocess.CalledProcessError as e:
        return {"error": f"ideviceinfo failed: {e.output.decode().strip()}"}

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "hello-state":
            udid = sys.argv[2] if len(sys.argv) > 2 else None
            print(json.dumps(check_hello_screen_state(udid)))
        elif sys.argv[1] == "checkm8-detect":
            udid = sys.argv[2] if len(sys.argv) > 2 else None
            print(json.dumps(detect_checkm8_eligibility(udid)))
