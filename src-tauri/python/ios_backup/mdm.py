import subprocess
import json
import sys

def detect_mdm(udid=None):
    """
    Detect MDM enrollment and restrictions via ideviceinfo.
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
        
        # MDM key indicators
        is_enrolled = data.get("IsCloudProfileApplied", "false").lower() == "true"
        
        return {
            "enrolled": is_enrolled,
            "org_name": data.get("OrganizationName", None),
            "server_url": data.get("MDMServerURL", None),
            "restrictions": [], # would parse from profile list
            "udid": udid
        }
    except Exception as e:
        return {"error": str(e)}

def list_profiles(udid):
    """
    List installed configuration profiles.
    """
    try:
        # ideviceprofile -l
        cmd = ["ideviceprofile", "-l", "-u", udid]
        output = subprocess.check_output(cmd).decode().strip()
        return {"profiles": output.split("\n")}
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "mdm-state":
            udid = sys.argv[2] if len(sys.argv) > 2 else None
            print(json.dumps(detect_mdm(udid)))
        elif sys.argv[1] == "list-profiles":
            udid = sys.argv[2]
            print(json.dumps(list_profiles(udid)))
