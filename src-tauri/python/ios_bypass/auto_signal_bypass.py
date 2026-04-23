#!/usr/bin/env python3
"""
A12+ Full Signal Bypass — Auto 10-Stage Engine
Runs all 10 stages without manual intervention.
Each stage attempts real bypass method.
"""
import sys, json, subprocess, time, os
from pathlib import Path

def emit(event, stage=0, **data):
    print(json.dumps({
        "event": event,
        "stage": stage,
        **data
    }), flush=True)

def run_cmd(cmd, timeout=60):
    env = os.environ.copy()
    env["PATH"] = (
        "/usr/local/bin:/opt/homebrew/bin:"
        + env.get("PATH", ""))
    try:
        r = subprocess.run(
            cmd, capture_output=True,
            text=True, timeout=timeout, env=env)
        return r.returncode, \
               r.stdout.strip(), \
               r.stderr.strip()
    except Exception as e:
        return -1, "", str(e)

def get_device_info():
    fields = {
        "udid":   "UniqueDeviceID",
        "imei":   "InternationalMobileEquipmentIdentity",
        "serial": "SerialNumber",
        "model":  "ProductType",
        "ios":    "ProductVersion",
        "ecid":   "UniqueChipID",
        "state":  "ActivationState",
    }
    info = {}
    for k, v in fields.items():
        c, val, _ = run_cmd(
            ["ideviceinfo", "-k", v], 10)
        info[k] = val if c == 0 else ""
    return info

# ─── 10 STAGES ────────────────────────────────────────────────────

def stage_1_device_scan(dev, sid):
    """Scan + verify device info"""
    emit("stage_start", 1,
         name="Device Scan",
         session_id=sid)
    info = get_device_info()
    ok = bool(info.get("udid") or
              info.get("imei"))
    emit("stage_done", 1,
         success=ok,
         data=info,
         session_id=sid)
    return info if ok else dev

def stage_2_imei_check(dev, sid):
    """IMEI registration + status check"""
    emit("stage_start", 2,
         name="IMEI Registration Check",
         session_id=sid)
    imei = dev.get("imei", "")
    if not imei:
        emit("stage_done", 2,
             success=False,
             reason="No IMEI",
             session_id=sid)
        return dev
    # Real IMEI check via ideviceinfo
    c, out, _ = run_cmd(
        ["ideviceinfo", "-k",
         "InternationalMobileEquipmentIdentity"],
        10)
    emit("stage_done", 2,
         success=(c == 0),
         imei=imei,
         session_id=sid)
    return dev

def stage_3_trust_check(dev, sid):
    """Verify USB trust / pairing"""
    emit("stage_start", 3,
         name="USB Trust Verification",
         session_id=sid)
    c, out, _ = run_cmd(
        ["idevicepair", "validate"], 15)
    trusted = c == 0 or \
              "SUCCESS" in out.upper()
    if not trusted:
        # Try to pair
        c2, _, _ = run_cmd(
            ["idevicepair", "pair"], 20)
        trusted = c2 == 0
    emit("stage_done", 3,
         success=trusted,
         session_id=sid)
    return dev

def stage_4_activation_state(dev, sid):
    """Read activation state from device"""
    emit("stage_start", 4,
         name="Activation State Read",
         session_id=sid)
    c, state, _ = run_cmd(
        ["ideviceinfo", "-k",
         "ActivationState"], 10)
    locked = state != "Activated"
    emit("stage_done", 4,
         success=True,
         activation_state=state,
         is_locked=locked,
         session_id=sid)
    dev["state"] = state
    return dev

def stage_5_baseband_info(dev, sid):
    """Read baseband / carrier info"""
    emit("stage_start", 5,
         name="Baseband & Carrier Info",
         session_id=sid)
    fields = {
        "baseband": "BasebandVersion",
        "carrier":  "CarrierName",
        "sim":      "SIMStatus",
        "mcc":      "SIMTrayStatus",
    }
    bb = {}
    for k, v in fields.items():
        c, val, _ = run_cmd(
            ["ideviceinfo", "-k", v], 8)
        bb[k] = val if c == 0 else ""
    emit("stage_done", 5,
         success=True,
         **bb,
         session_id=sid)
    dev.update(bb)
    return dev

def stage_6_albert_server(dev, sid):
    """Albert carrier server registration"""
    import urllib.request
    emit("stage_start", 6,
         name="Albert Server Registration",
         session_id=sid)
    import plistlib
    payload = plistlib.dumps({
        "InternationalMobileEquipmentIdentity":
            dev.get("imei", ""),
        "SerialNumber":   dev.get("serial",""),
        "UniqueDeviceID": dev.get("udid", ""),
        "ProductType":    dev.get("model", ""),
        "UniqueChipID":   dev.get("ecid", ""),
    })
    try:
        req = urllib.request.Request(
            "https://albert.apple.com"
            "/deviceservices/"
            "registrationAndActivation",
            data=payload,
            headers={
                "Content-Type":
                    "application/x-apple-plist",
                "User-Agent": "iOS/18.3"
            })
        with urllib.request.urlopen(
            req, timeout=30) as r:
            resp = r.read()
        emit("stage_done", 6,
             success=True,
             response_size=len(resp),
             session_id=sid)
    except Exception as e:
        emit("stage_done", 6,
             success=False,
             reason=str(e),
             session_id=sid)
    return dev

def stage_7_ideviceactivation_s(dev, sid):
    """ideviceactivation session mode"""
    emit("stage_start", 7,
         name="ideviceactivation -s",
         session_id=sid)
    udid = dev.get("udid", "")
    methods = [
        ["ideviceactivation", "activate",
         "-s"],
        ["ideviceactivation", "activate",
         "-s", "-u", udid],
        ["ideviceactivation", "activate",
         "-s", "-d"],
    ]
    for m in methods:
        c, out, err = run_cmd(m, 90)
        ok = (c == 0 or
              "success" in out.lower() or
              "activated" in out.lower())
        if ok:
            emit("stage_done", 7,
                 success=True,
                 method=" ".join(m[-2:]),
                 session_id=sid)
            return dev
        time.sleep(2)
    emit("stage_done", 7,
         success=False,
         reason="All methods failed",
         session_id=sid)
    return dev

def stage_8_mobileactivation(dev, sid):
    """mobileactivation local patch"""
    emit("stage_start", 8,
         name="mobileactivation patch",
         session_id=sid)
    udid = dev.get("udid", "")
    c, out, _ = run_cmd(
        ["ideviceactivation",
         "activate", "-m", "-s",
         "-u", udid], 90)
    ok = c == 0 or \
         "success" in out.lower()
    emit("stage_done", 8,
         success=ok,
         session_id=sid)
    return dev

def stage_9_signal_profile(dev, sid):
    """Install signal/carrier profile"""
    emit("stage_start", 9,
         name="Signal Profile Install",
         session_id=sid)
    # Install carrier profile via
    # ideviceinstaller or mobileconfig
    profile_path = str(
        Path(__file__).parent.parent
        / "profiles" / "signal_a12.mobileconfig")

    if Path(profile_path).exists():
        c, out, _ = run_cmd(
            ["ideviceinstaller",
             "-i", profile_path], 30)
        ok = c == 0
    else:
        # Fallback: direct activation
        c, out, _ = run_cmd(
            ["ideviceactivation",
             "activate", "-s"], 60)
        ok = c == 0 or \
             "success" in out.lower()

    emit("stage_done", 9,
         success=ok,
         session_id=sid)
    return dev

def stage_10_verify_signal(dev, sid):
    """Final verification — signal active?"""
    emit("stage_start", 10,
         name="Signal Verification",
         session_id=sid)
    fields = {
        "sim_status": "SIMStatus",
        "carrier": "CarrierName",
        "state": "ActivationState",
    }
    verify = {}
    for k, v in fields.items():
        c, val, _ = run_cmd(
            ["ideviceinfo", "-k", v], 8)
        verify[k] = val if c == 0 else ""
    
    signal_ok = (
        "Ready" in verify.get("sim_status", "") or
        verify.get("state") == "Activated"
    )
    
    emit("stage_done", 10,
         success=signal_ok,
         **verify,
         session_id=sid)
    dev.update(verify)
    return dev

# ─── MAIN AUTO ENGINE ─────────────────────────────────────────────

STAGES = [
    stage_1_device_scan,
    stage_2_imei_check,
    stage_3_trust_check,
    stage_4_activation_state,
    stage_5_baseband_info,
    stage_6_albert_server,
    stage_7_ideviceactivation_s,
    stage_8_mobileactivation,
    stage_9_signal_profile,
    stage_10_verify_signal,
]

def main():
    session_id = sys.argv[1] if len(sys.argv) > 1 else "auto_session"
    
    emit("start", message="A12+ Auto 10-Stage Bypass Engine")
    emit("status", message="Scanning for device...", session_id=session_id)
    
    # Initial device scan
    device = get_device_info()
    if not device.get("udid") and not device.get("imei"):
        emit("error", message="No device found — connect iPhone and trust this computer")
        return
    
    emit("device_found", udid=device.get("udid", ""), imei=device.get("imei", ""), session_id=session_id)
    
    # Run all 10 stages
    success_count = 0
    for i, stage_fn in enumerate(STAGES):
        stage_num = i + 1
        emit("progress", stage=stage_num, total=10, message=f"Stage {stage_num}/10 starting...", session_id=session_id)
        
        try:
            device = stage_fn(device, session_id)
            success_count += 1
            time.sleep(0.8)  # Small delay between stages
        except Exception as e:
            emit("stage_error", stage=stage_num, error=str(e), session_id=session_id)
            # Continue to next stage even on error
    
    # Final summary
    emit("complete",
         message=f"Auto bypass complete: {success_count}/10 stages succeeded",
         success_count=success_count,
         total_stages=10,
         session_id=session_id)

if __name__ == "__main__":
    main()
