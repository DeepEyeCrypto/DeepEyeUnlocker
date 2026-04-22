import subprocess
import json
import sys

def check_activation_lock(udid=None):
    """
    Detect Activation Lock status from ideviceinfo.
    """
    try:
        cmd = ["ideviceinfo"]
        if udid:
            cmd.extend(["-u", udid])
        
        output = subprocess.check_output(cmd).decode().strip()
        data = {}
        for line in output.split("\n"):
            if ":" in line:
                k, v = line.split(":", 1)
                data[k.strip()] = v.strip()
        
        # Key fields: 
        # ActivationState: Unactivated, Activated
        # PasswordProtected: true/false
        # ProductType: iPhone10,6
        # CPUArchitecture: arm64
        
        fmi_enabled = data.get("FMIEnabled", "false").lower() == "true"
        activation_state = data.get("ActivationState", "Unknown")
        
        return {
            "locked": activation_state != "Activated" or fmi_enabled,
            "fmi_enabled": fmi_enabled,
            "activation_state": activation_state,
            "model": data.get("ProductType", "Unknown"),
            "chip": data.get("CPUArchitecture", "Unknown"),
            "udid": data.get("UniqueDeviceID", udid)
        }
    except Exception as e:
        return {"error": str(e)}

def parse_activation_record(udid):
    """
    Parse the activation record plist from the device via ideviceactivation.
    Returns the raw activation record fields or an error.
    """
    import plistlib
    try:
        cmd = ["ideviceactivation", "state"]
        if udid:
            cmd.extend(["-u", udid])
        output = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode().strip()

        # ideviceactivation outputs key: value pairs
        record = {}
        for line in output.split("\n"):
            if ":" in line:
                k, v = line.split(":", 1)
                record[k.strip()] = v.strip()

        if not record:
            return {"error": "Empty activation record — device may not support this query"}

        return {"raw": record}
    except FileNotFoundError:
        return {"error": "ideviceactivation not installed. Install via: brew install libideviceactivation"}
    except subprocess.CalledProcessError as e:
        return {"error": f"ideviceactivation failed: {e.output.decode().strip()}"}

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "activation-state":
            udid = sys.argv[2] if len(sys.argv) > 2 else None
            print(json.dumps(check_activation_lock(udid)))
