"""
DeepEye Firmware Download — IPSW catalog + manifest
Reference: gsmgermany.com Firmware Download
"""
import json
import hashlib

# Offline IPSW catalog (top models)
# Keys: "Model|iOS" → {url, sha256, size_gb, build_id}
IPSW_CATALOG = {
    "iPhone14,2|17.2": {
        "model":    "iPhone 13 Pro",
        "url":      "https://updates.cdn-apple.com/2023FallFCS/fullrestores/042-01901/F4A5D2D3-C2AD-4B5D-B5E9-D9E3A1B17B5A/iPhone_5.5_15.6_19G71_Restore.ipsw",
        "sha256":   "placeholder_sha256_verify_live",
        "size_gb":  6.8,
        "build_id": "21C62",
        "signed":   True
    },
    "iPhone13,2|17.2": {
        "model":    "iPhone 12",
        "url":      "https://updates.cdn-apple.com/ipsw/iPhone13,2/17.2/iPhone13,2_17.2_21C62_Restore.ipsw",
        "sha256":   "placeholder_sha256_verify_live",
        "size_gb":  6.2,
        "build_id": "21C62",
        "signed":   True
    },
    "iPhone12,1|16.7.8": {
        "model":    "iPhone 11",
        "url":      "https://updates.cdn-apple.com/ipsw/iPhone12,1/16.7.8/iPhone12,1_16.7.8_20H343_Restore.ipsw",
        "sha256":   "placeholder_sha256_verify_live",
        "size_gb":  5.9,
        "build_id": "20H343",
        "signed":   True
    },
    "iPhone10,3|16.7.8": {
        "model":    "iPhone X",
        "url":      "https://updates.cdn-apple.com/ipsw/iPhone10,3/16.7.8/iPhone10,3_16.7.8_20H343_Restore.ipsw",
        "sha256":   "placeholder_sha256_verify_live",
        "size_gb":  5.4,
        "build_id": "20H343",
        "signed":   True
    },
    "iPhone9,1|15.8.2": {
        "model":    "iPhone 7",
        "url":      "https://updates.cdn-apple.com/ipsw/iPhone9,1/15.8.2/iPhone9,1_15.8.2_19H384_Restore.ipsw",
        "sha256":   "placeholder_sha256_verify_live",
        "size_gb":  4.8,
        "build_id": "19H384",
        "signed":   False  # Old — may not be signed
    },
}

def get_firmware_for_model(model: str) -> list:
    """Get all available IPSW entries for a model."""
    results = []
    for key, data in IPSW_CATALOG.items():
        m, ios = key.split("|")
        if m == model:
            results.append({
                "ios_version": ios,
                "build_id":    data["build_id"],
                "size_gb":     data["size_gb"],
                "signed":      data["signed"],
                "model_name":  data["model"],
                "url":         data["url"],
            })
    return sorted(
        results,
        key=lambda x: x["ios_version"],
        reverse=True
    )

def get_latest_signed(model: str) -> dict:
    """Return latest signed IPSW for model."""
    all_fw = get_firmware_for_model(model)
    signed = [f for f in all_fw if f["signed"]]
    return signed if signed else {}

def generate_ipsw_download_id(
    model: str,
    ios: str,
    ecid: str
) -> str:
    """Generate unique download session ID."""
    return hashlib.sha256(
        f"{model}:{ios}:{ecid}:deepeye".encode()
    ).hexdigest()[:16].upper()

def estimate_download_time(
    size_gb: float,
    speed_mbps: float = 10.0
) -> dict:
    """Estimate IPSW download time."""
    size_mb     = size_gb * 1024
    seconds     = (size_mb * 8) / speed_mbps
    minutes     = int(seconds // 60)
    secs_remain = int(seconds % 60)
    return {
        "size_mb":    round(size_mb, 1),
        "size_gb":    size_gb,
        "speed_mbps": speed_mbps,
        "minutes":    minutes,
        "seconds":    secs_remain,
        "display":    f"~{minutes}m {secs_remain}s"
    }
