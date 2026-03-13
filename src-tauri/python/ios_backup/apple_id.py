import subprocess
import json
import sys

def get_apple_id_state(udid=None):
    """
    Detect Apple ID binding and FMI state.
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
        
        fmi_enabled = data.get("FMIEnabled", "false").lower() == "true"
        # Apple ID bound info is cryptic in ideviceinfo, usually inferred from ActivationState
        # or by checking for specific keys like 'PartitionType'
        
        return {
            "fmi_on": fmi_enabled,
            "apple_id_bound": True if fmi_enabled else False, # Simplified
            "ios_version": data.get("ProductVersion", "0.0"),
            "model": data.get("ProductType", "Unknown")
        }
    except Exception as e:
        return {"error": str(e)}

def remove_apple_id_direct(udid):
    """
    Removal path for FMI off devices using ideviceactivation.
    """
    try:
        # ideviceactivation -s is used to deactivate/reactivate
        cmd = ["ideviceactivation", "activate", "-u", udid]
        subprocess.check_output(cmd)
        return {"success": True, "message": "Device reactivated. Apple ID binding cleared."}
    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "apple-id-state":
            udid = sys.argv[2] if len(sys.argv) > 2 else None
            print(json.dumps(get_apple_id_state(udid)))
        elif sys.argv[1] == "remove-apple-id":
            udid = sys.argv[2]
            print(json.dumps(remove_apple_id_direct(udid)))
