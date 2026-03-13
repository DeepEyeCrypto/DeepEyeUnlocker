import subprocess
import json
import sys
import os
import time

# ──────────────────────────────────────────────────────────────
# MODULE 15: MASS ARTIFACT EXTRACTION (DeepExtraction v2)
# Reference: Forensic extraction from SSH-enabled ramdisk
# ──────────────────────────────────────────────────────────────

TARGET_ARTIFACTS = [
    {"name": "SMS", "path": "/mnt1/private/var/mobile/Library/SMS/sms.db"},
    {"name": "Call History", "path": "/mnt1/private/var/mobile/Library/CallHistoryDB/CallHistory.storedata"},
    {"name": "Address Book", "path": "/mnt1/private/var/mobile/Library/AddressBook/AddressBook.sqlitedb"},
    {"name": "Keychain", "path": "/mnt1/private/var/Keychains/keychain-2.db"},
    {"name": "AppleID Accounts", "path": "/mnt1/private/var/mobile/Library/Accounts/Accounts3.sqlite"},
    {"name": "Safari Bookmarks", "path": "/mnt1/private/var/mobile/Library/Safari/Bookmarks.db"},
    {"name": "Activation Ticket", "path": "/mnt1/private/var/containers/Data/System/com.apple.mobileactivationd/Library/ActivationRecords/wildcard.plist"},
    {"name": "FairPlay Keys", "path": "/mnt1/private/var/mobile/Library/FairPlay/"},
]

def run_ssh_command(command, port=2222):
    """
    Run a command over SSH (assumes iproxy is running on port).
    """
    ssh_cmd = [
        "ssh", "-p", str(port),
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "root@localhost", command
    ]
    try:
        output = subprocess.check_output(ssh_cmd, stderr=subprocess.STDOUT).decode().strip()
        return {"success": True, "output": output}
    except Exception as e:
        return {"success": False, "error": str(e)}

def mount_partitions(port=2222):
    """
    Mount the user data partition.
    """
    # Attempt to mount /dev/disk0s1s2 to /mnt1
    # Different iOS versions use different partition names
    commands = [
        "mount_apfs /dev/disk0s1s2 /mnt1",
        "mount_hfs /dev/disk0s1s2 /mnt1",
        "mount -o rw /dev/disk0s1s2 /mnt1"
    ]
    
    results = []
    for cmd in commands:
        res = run_ssh_command(cmd, port)
        results.append(res)
        if res["success"]:
            return {"success": True, "message": "Mounted successfully"}
    
    return {"success": False, "errors": results}

def extract_artifact(remote_path, local_save_dir, port=2222):
    """
    SCP a single artifact.
    """
    filename = os.path.basename(remote_path)
    local_path = os.path.join(local_save_dir, filename)
    
    scp_cmd = [
        "scp", "-P", str(port),
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        f"root@localhost:{remote_path}", local_path
    ]
    try:
        subprocess.check_output(scp_cmd, stderr=subprocess.STDOUT)
        return {"success": True, "path": local_path}
    except Exception as e:
        return {"success": False, "error": str(e)}

def mass_extract(save_dir, port=2222):
    """
    Run full extraction suite.
    """
    if not os.path.exists(save_dir):
        os.makedirs(save_dir)
        
    print(json.dumps({"status": "mounting", "message": "Mounting user partitions..."}))
    mount_res = mount_partitions(port)
    if not mount_res["success"]:
        return mount_res

    extracted = []
    total = len(TARGET_ARTIFACTS)
    
    for i, artifact in enumerate(TARGET_ARTIFACTS):
        print(json.dumps({
            "status": "extracting", 
            "current": artifact["name"], 
            "progress": int((i / total) * 100)
        }))
        res = extract_artifact(artifact["path"], save_dir, port)
        extracted.append({
            "name": artifact["name"],
            "remote": artifact["path"],
            "success": res["success"],
            "local": res.get("path")
        })
        
    return {
        "success": True,
        "results": extracted,
        "message": f"Extraction complete. {len([e for e in extracted if e['success']])} files saved."
    }

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "mount":
            print(json.dumps(mount_partitions()))
        elif sys.argv[1] == "mass-extract":
            save_path = sys.argv[2] if len(sys.argv) > 2 else "./extraction_vault"
            # In a real tool, we would handle the loop/print differently to pipe JSON to Tauri
            print(json.dumps(mass_extract(save_path)))
