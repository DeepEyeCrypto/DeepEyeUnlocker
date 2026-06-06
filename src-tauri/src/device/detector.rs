use rusb::{Context, UsbContext};
use serde::{Deserialize, Serialize};
use std::sync::Mutex;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

/// Device operational mode classification based on USB VID/PID
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum DeviceMode {
    /// MediaTek Boot ROM (0x0E8D:0x0003)
    Brom,
    /// MediaTek PreLoader (0x0E8D:0x2000 / 0x0E8D:0x0006)
    PreLoader,
    /// MediaTek Download Agent (0x0E8D:0x2001)
    MtkDa,
    /// Qualcomm Emergency Download Mode (0x05C6:0x9008)
    Edl,
    /// Generic Fastboot (multiple vendors)
    Fastboot,
    /// Android Debug Bridge (multiple vendors)
    Adb,
    /// Media Transfer Protocol
    Mtp,
    /// Android Recovery mode
    Recovery,
    /// Samsung Download/Odin mode
    SamsungOdin,
    /// UniSoc Flash Download Mode
    UnisocFdl,
    /// Unknown or unsupported device
    Unknown(u16),
}

/// Detected USB device with protocol classification
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DetectedDevice {
    pub mode: DeviceMode,
    pub vid: u16,
    pub pid: u16,
    pub serial: Option<String>,
    pub manufacturer: Option<String>,
    pub product: Option<String>,
    pub bus: u8,
    pub address: u8,
    pub chipset: Option<String>,
    pub detected_at: u64,
}

// ─── Known USB VID constants ───────────────────────────────────────────────

const MTK_VID: u16 = 0x0E8D;
const QCOM_VID: u16 = 0x05C6;
const SAMSUNG_VID: u16 = 0x04E8;
const GOOGLE_VID: u16 = 0x18D1;
const UNISOC_VID: u16 = 0x1782;
const XIAOMI_VID: u16 = 0x2717;
const HUAWEI_VID: u16 = 0x12D1;
const OPPO_VID: u16 = 0x22D9;
const ONEPLUS_VID: u16 = 0x2A70;
const VIVO_VID: u16 = 0x2D95;
const SONY_VID: u16 = 0x0FCE;
const LG_VID: u16 = 0x1004;
const MOTOROLA_VID: u16 = 0x22B8;
const NOKIA_VID: u16 = 0x0421;
const LENOVO_VID: u16 = 0x17EF;

// ─── Scan cache ────────────────────────────────────────────────────────────

static SCAN_CACHE: Mutex<Option<(Vec<DetectedDevice>, Instant)>> = Mutex::new(None);
const CACHE_TTL: Duration = Duration::from_millis(500);

/// Invalidate scan cache (call after device connect/disconnect events)
#[allow(dead_code)]
pub fn invalidate_scan_cache() {
    if let Ok(mut cache) = SCAN_CACHE.lock() {
        *cache = None;
    }
}

// ─── VID/PID classification ────────────────────────────────────────────────

/// Classify device mode from VID/PID pair
fn classify_device(vid: u16, pid: u16) -> DeviceMode {
    match (vid, pid) {
        // ── MediaTek ──
        (MTK_VID, 0x0003) => DeviceMode::Brom,
        (MTK_VID, 0x2000) | (MTK_VID, 0x0006) => DeviceMode::PreLoader,
        (MTK_VID, 0x2001) => DeviceMode::MtkDa,
        (MTK_VID, 0x0C01) => DeviceMode::Fastboot,
        (MTK_VID, 0x201D) => DeviceMode::Mtp,

        // ── Qualcomm ──
        (QCOM_VID, 0x9008) | (QCOM_VID, 0x900E) => DeviceMode::Edl,
        (QCOM_VID, 0x9048) => DeviceMode::Mtp,

        // ── Google / AOSP ──
        (GOOGLE_VID, 0xD00D) => DeviceMode::Fastboot,
        (GOOGLE_VID, 0x4EE7) => DeviceMode::Recovery,
        (GOOGLE_VID, 0x4EE2) | (GOOGLE_VID, 0x4EE1) => DeviceMode::Adb,
        (GOOGLE_VID, 0x4EE0) => DeviceMode::Mtp,

        // ── Samsung ──
        // 0x685D = Odin pure, 0x6860 = composite (Odin+MTP), 0x6861 = Odin CDC
        (SAMSUNG_VID, 0x685D) | (SAMSUNG_VID, 0x6860) | (SAMSUNG_VID, 0x6861) => {
            DeviceMode::SamsungOdin
        }
        (SAMSUNG_VID, 0x6862) => DeviceMode::Recovery,
        (SAMSUNG_VID, 0x685E) => DeviceMode::Adb,

        // ── Xiaomi / Redmi / POCO ──
        (XIAOMI_VID, 0xFF40) => DeviceMode::Adb,
        (XIAOMI_VID, 0xFF68) | (XIAOMI_VID, 0xFF60) => DeviceMode::Fastboot,
        (XIAOMI_VID, 0xFF48) => DeviceMode::Mtp,

        // ── Huawei / Honor ──
        (HUAWEI_VID, 0x1057) | (HUAWEI_VID, 0x1038) => DeviceMode::Adb,
        (HUAWEI_VID, 0x107E) | (HUAWEI_VID, 0x107D) => DeviceMode::Fastboot,
        (HUAWEI_VID, 0x1052) => DeviceMode::Mtp,

        // ── OPPO / Realme ──
        (OPPO_VID, 0x2769) => DeviceMode::Adb,
        (OPPO_VID, 0x2764) => DeviceMode::Fastboot,

        // ── OnePlus ──
        (ONEPLUS_VID, 0x9011) => DeviceMode::Adb,
        (ONEPLUS_VID, 0x9012) => DeviceMode::Fastboot,

        // ── Vivo / iQOO ──
        (VIVO_VID, 0x6003) | (VIVO_VID, 0x6001) => DeviceMode::Adb,
        (VIVO_VID, 0x6000) => DeviceMode::Fastboot,

        // ── Sony ──
        (SONY_VID, 0xB00B) => DeviceMode::Fastboot,
        (SONY_VID, 0x51A7) => DeviceMode::Adb,

        // ── LG ──
        (LG_VID, 0x6300) | (LG_VID, 0x631C) => DeviceMode::Adb,
        (LG_VID, 0x631F) => DeviceMode::Fastboot,

        // ── Motorola ──
        (MOTOROLA_VID, 0x2E76) => DeviceMode::Adb,
        (MOTOROLA_VID, 0x2E81) => DeviceMode::Fastboot,

        // ── Nokia ──
        (NOKIA_VID, 0x0105) => DeviceMode::Adb,

        // ── Lenovo ──
        (LENOVO_VID, 0x7774) => DeviceMode::Adb,
        (LENOVO_VID, 0x7775) => DeviceMode::Fastboot,

        // ── UniSoc ──
        (UNISOC_VID, 0x4D00) => DeviceMode::UnisocFdl,

        // ── Fallback: unknown ──
        _ => DeviceMode::Unknown(pid),
    }
}

// ─── ADB interface-class fallback ──────────────────────────────────────────

/// Detect ADB by USB interface class (0xFF / 0x42 / 0x01).
/// Works for ANY Android vendor, even if VID/PID is not in our table.
fn has_adb_interface(device: &rusb::Device<impl UsbContext>) -> bool {
    let config = match device.active_config_descriptor() {
        Ok(c) => c,
        Err(_) => return false,
    };

    config.interfaces().any(|iface| {
        iface.descriptors().any(|desc| {
            desc.class_code() == 0xFF
                && desc.sub_class_code() == 0x42
                && desc.protocol_code() == 0x01
        })
    })
}

/// Detect Fastboot by USB interface class (0xFF / 0x42 / 0x03).
fn has_fastboot_interface(device: &rusb::Device<impl UsbContext>) -> bool {
    let config = match device.active_config_descriptor() {
        Ok(c) => c,
        Err(_) => return false,
    };

    config.interfaces().any(|iface| {
        iface.descriptors().any(|desc| {
            desc.class_code() == 0xFF
                && desc.sub_class_code() == 0x42
                && desc.protocol_code() == 0x03
        })
    })
}

// ─── USB scan ──────────────────────────────────────────────────────────────

/// Scan all USB devices and return detected supported devices.
/// Results are cached for 500ms to avoid redundant bus scans.
pub fn scan_usb_devices() -> Result<Vec<DetectedDevice>, String> {
    // Check cache first
    if let Ok(cache) = SCAN_CACHE.lock() {
        if let Some((ref devices, ref timestamp)) = *cache {
            if timestamp.elapsed() < CACHE_TTL {
                return Ok(devices.clone());
            }
        }
    }

    let result = scan_usb_devices_uncached()?;

    // Store in cache
    if let Ok(mut cache) = SCAN_CACHE.lock() {
        *cache = Some((result.clone(), Instant::now()));
    }

    Ok(result)
}

/// Actual USB bus scan (uncached).
fn scan_usb_devices_uncached() -> Result<Vec<DetectedDevice>, String> {
    let context = Context::new().map_err(|e| format!("USB context init failed: {}", e))?;
    let mut devices = Vec::new();

    let device_list = context
        .devices()
        .map_err(|e| format!("USB enumerate failed: {}", e))?;

    for device in device_list.iter() {
        let desc = match device.device_descriptor() {
            Ok(d) => d,
            Err(_) => continue,
        };

        let vid = desc.vendor_id();
        let pid = desc.product_id();
        let mut mode = classify_device(vid, pid);

        // ── Interface-class fallback for unknown devices ──
        if matches!(mode, DeviceMode::Unknown(_)) {
            if has_adb_interface(&device) {
                mode = DeviceMode::Adb;
                log::debug!(
                    "[Detector] Unknown VID:{:04X} PID:{:04X} detected as ADB via interface class",
                    vid,
                    pid
                );
            } else if has_fastboot_interface(&device) {
                mode = DeviceMode::Fastboot;
                log::debug!(
                    "[Detector] Unknown VID:{:04X} PID:{:04X} detected as Fastboot via interface class",
                    vid,
                    pid
                );
            }
        }

        // Skip truly unknown devices (no VID/PID match AND no interface match)
        if matches!(mode, DeviceMode::Unknown(_)) {
            continue;
        }

        // Try to open device for string descriptors
        let handle = device.open().ok();
        let timeout = Duration::from_millis(200);
        let languages = handle
            .as_ref()
            .and_then(|h| h.read_languages(timeout).ok())
            .unwrap_or_default();
        let lang = languages.first().copied();

        // Extract serial number
        let serial = handle.as_ref().zip(lang).and_then(|(h, l)| {
            desc.serial_number_string_index()
                .and_then(|i| h.read_string_descriptor(l, i, timeout).ok())
        });

        // Extract manufacturer
        let manufacturer = handle.as_ref().zip(lang).and_then(|(h, l)| {
            desc.manufacturer_string_index()
                .and_then(|i| h.read_string_descriptor(l, i, timeout).ok())
        });

        // Extract product name
        let product = handle.as_ref().zip(lang).and_then(|(h, l)| {
            desc.product_string_index()
                .and_then(|i| h.read_string_descriptor(l, i, timeout).ok())
        });

        devices.push(DetectedDevice {
            mode,
            vid,
            pid,
            serial,
            manufacturer,
            product,
            bus: device.bus_number(),
            address: device.address(),
            chipset: None, // Populated by protocol-specific code
            detected_at: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap_or_default()
                .as_millis() as u64,
        });
    }

    Ok(devices)
}

/// Check if any device of specific mode is connected (uses cached scan).
pub fn has_device_mode(mode: DeviceMode) -> Result<bool, String> {
    let devices = scan_usb_devices()?; // Uses cache, no redundant rescan
    Ok(devices.iter().any(|d| d.mode == mode))
}
