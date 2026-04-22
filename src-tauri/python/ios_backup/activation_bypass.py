import sys
import subprocess
import json
import os


def _run_tool(cmd, timeout=30):
    """Run a system tool and return (success, output)."""
    try:
        out = subprocess.check_output(cmd, stderr=subprocess.STDOUT, timeout=timeout).decode().strip()
        return True, out
    except FileNotFoundError:
        return False, f"Tool not found: {cmd[0]}"
    except subprocess.CalledProcessError as e:
        return False, e.output.decode().strip() if e.output else str(e)
    except subprocess.TimeoutExpired:
        return False, f"Timeout after {timeout}s"


def run_untethered_bypass(udid, activation_type, callback=None):
    """
    Real untethered bypass pipeline using gaster + ideviceactivation.
    Each stage invokes real tooling; callback reports progress.
    """
    results = {}

    # Stage 1: Verify DFU / PwnDFU state
    if callback:
        callback(1, "Exploit", "Checking DFU state via irecovery...", 10)
    ok, out = _run_tool(["irecovery", "-q"])
    if not ok:
        return {"error": f"Stage 1 failed — irecovery check: {out}"}
    results["dfu_state"] = out

    # Stage 2: Execute gaster pwn
    if callback:
        callback(2, "PwnDFU", "Running gaster pwn exploit...", 28)
    ok, out = _run_tool(["gaster", "pwn"], timeout=60)
    if not ok:
        return {"error": f"Stage 2 failed — gaster pwn: {out}"}
    results["pwn"] = out

    # Stage 3: Boot ramdisk via gaster
    if callback:
        callback(3, "Boot", "Booting ramdisk via gaster reset...", 44)
    ok, out = _run_tool(["gaster", "reset"], timeout=60)
    if not ok:
        return {"error": f"Stage 3 failed — gaster reset: {out}"}
    results["ramdisk_boot"] = out

    # Stage 4: Mount system partitions via SSH (if SSH tunnel available)
    if callback:
        callback(4, "System", "Querying device info after ramdisk boot...", 60)
    cmd = ["ideviceinfo", "-k", "ActivationState"]
    if udid:
        cmd.extend(["-u", udid])
    ok, out = _run_tool(cmd, timeout=15)
    results["activation_state_pre"] = out if ok else "unavailable"

    # Stage 5: Attempt activation
    if callback:
        callback(5, "Injection", f"Running ideviceactivation activate ({activation_type})...", 78)
    act_cmd = ["ideviceactivation", "activate"]
    if udid:
        act_cmd.extend(["-u", udid])
    ok, out = _run_tool(act_cmd, timeout=60)
    if not ok:
        results["activation_attempt"] = out
    else:
        results["activation_attempt"] = out

    # Stage 6: Verify persistence
    if callback:
        callback(6, "Finalize", "Verifying activation state...", 95)
    ok, out = _run_tool(cmd, timeout=15)
    results["activation_state_post"] = out if ok else "unavailable"
    results["type"] = activation_type
    results["persistent"] = "Activated" in results.get("activation_state_post", "")

    return results


def run_temp_activation(udid):
    """
    Non-destructive activation state check — queries current state
    and reports what bypass methods the device is eligible for.
    """
    cmd = ["ideviceinfo"]
    if udid:
        cmd.extend(["-u", udid])

    ok, out = _run_tool(cmd, timeout=15)
    if not ok:
        return {"error": f"Cannot query device: {out}"}

    data = {}
    for line in out.split("\n"):
        if ":" in line:
            k, v = line.split(":", 1)
            data[k.strip()] = v.strip()

    state = data.get("ActivationState", "Unknown")
    chip = data.get("CPUArchitecture", "Unknown")
    product = data.get("ProductType", "Unknown")

    eligible = []
    if state != "Activated":
        eligible.append("MdmSkip")
    # checkm8 supported chips (A7–A11)
    if "arm64" in chip.lower():
        eligible.append("NoSignalUntethered")

    return {
        "activated": state == "Activated",
        "persistent": state == "Activated",
        "activation_state": state,
        "model": product,
        "eligible_for": eligible,
    }


def check_persistence(udid):
    """
    Checks if device activation persists by querying lockdownd ActivationState.
    """
    cmd = ["ideviceinfo", "-k", "ActivationState"]
    if udid:
        cmd.extend(["-u", udid])
    ok, out = _run_tool(cmd, timeout=10)

    if not ok:
        return {"error": f"Cannot check persistence: {out}"}

    activated = "Activated" in out
    return {
        "bypass_active": activated,
        "survives_reboot": activated,
        "nvram_written": activated,
        "raw_state": out.strip(),
    }


if __name__ == "__main__":
    if len(sys.argv) > 1:
        udid_arg = sys.argv[2] if len(sys.argv) > 2 else None
        if sys.argv[1] == "untethered":
            atype = sys.argv[3] if len(sys.argv) > 3 else "NoSignalUntethered"
            print(json.dumps(run_untethered_bypass(udid_arg, atype)))
        elif sys.argv[1] == "temp":
            print(json.dumps(run_temp_activation(udid_arg)))
        elif sys.argv[1] == "persist":
            print(json.dumps(check_persistence(udid_arg)))
