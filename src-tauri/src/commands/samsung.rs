use rusb::{DeviceHandle, GlobalContext, UsbContext};
use serde::Serialize;
use std::time::Duration;

const SAMSUNG_VID: u16 = 0x04e8;
const SAMSUNG_PIDS: [u16; 3] = [0x685d, 0x6860, 0x685e];

#[allow(dead_code)]
const TIMEOUT: Duration = Duration::from_secs(5);
const BULK_TIMEOUT: Duration = Duration::from_secs(30);

const EP_OUT: u8 = 0x02;
const EP_IN: u8 = 0x81;

#[derive(Debug, Serialize, Clone)]
pub struct SamsungDevice {
    pub vid: u16,
    pub pid: u16,
    pub model: String,
    pub mode: SamsungMode,
}

#[allow(dead_code)]
#[derive(Debug, Serialize, Clone)]
pub enum SamsungMode {
    Normal,
    DownloadMode,
    Recovery,
    Fastboot,
}

#[derive(Debug, Serialize)]
pub struct OdinInfo {
    pub protocol_version: String,
    pub pit_size: u32,
}

#[derive(Debug, Serialize, Clone)]
pub struct PitEntry {
    pub partition_name: String,
    pub flash_filename: String,
    pub partition_id: u32,
    pub partition_type: u32,
    pub device_type: u32,
    pub offset: u64,
    pub size: u64,
}

#[allow(dead_code)]
#[derive(Debug, thiserror::Error, Serialize)]
pub enum SamsungError {
    #[error("Samsung device not found")]
    DeviceNotFound,
    #[error("Not in Download Mode")]
    NotInDownloadMode,
    #[error("Odin handshake failed")]
    HandshakeFailed,
    #[error("PIT read failed")]
    PitFailed,
    #[error("Flash failed: {0}")]
    FlashFailed(String),
    #[error("USB error: {0}")]
    UsbError(String),
}

fn open_samsung() -> Result<DeviceHandle<GlobalContext>, SamsungError> {
    let ctx = rusb::GlobalContext::default();
    let devices = ctx
        .devices()
        .map_err(|e| SamsungError::UsbError(e.to_string()))?;

    for dev in devices.iter() {
        let desc = dev.device_descriptor().map_err(|e| SamsungError::UsbError(e.to_string()))?;
        if desc.vendor_id() == SAMSUNG_VID && SAMSUNG_PIDS.contains(&desc.product_id()) {
            let handle = dev
                .open()
                .map_err(|e| SamsungError::UsbError(e.to_string()))?;
            handle
                .claim_interface(0)
                .map_err(|e| SamsungError::UsbError(e.to_string()))?;
            return Ok(handle);
        }
    }
    Err(SamsungError::DeviceNotFound)
}

fn write_bulk(handle: &DeviceHandle<GlobalContext>, data: &[u8]) -> Result<(), SamsungError> {
    handle
        .write_bulk(EP_OUT, data, BULK_TIMEOUT)
        .map_err(|e| SamsungError::UsbError(e.to_string()))?;
    Ok(())
}

fn read_bulk(handle: &DeviceHandle<GlobalContext>, len: usize) -> Result<Vec<u8>, SamsungError> {
    let mut buf = vec![0u8; len];
    let n = handle
        .read_bulk(EP_IN, &mut buf, BULK_TIMEOUT)
        .map_err(|e| SamsungError::UsbError(e.to_string()))?;
    buf.truncate(n);
    Ok(buf)
}

pub fn find_samsung_device() -> Result<SamsungDevice, SamsungError> {
    let ctx = rusb::GlobalContext::default();
    let devices = ctx
        .devices()
        .map_err(|e| SamsungError::UsbError(e.to_string()))?;

    for dev in devices.iter() {
        let desc = dev.device_descriptor().map_err(|e| SamsungError::UsbError(e.to_string()))?;
        if desc.vendor_id() == SAMSUNG_VID && SAMSUNG_PIDS.contains(&desc.product_id()) {
            let mode = match desc.product_id() {
                0x685d | 0x6860 => SamsungMode::DownloadMode,
                _ => SamsungMode::Normal,
            };
            return Ok(SamsungDevice {
                vid: desc.vendor_id(),
                pid: desc.product_id(),
                model: String::new(),
                mode,
            });
        }
    }
    Err(SamsungError::DeviceNotFound)
}

pub fn samsung_odin_handshake() -> Result<OdinInfo, SamsungError> {
    let handle = open_samsung()?;
    write_bulk(&handle, b"ODIN")?;
    let resp = read_bulk(&handle, 8)?;
    let resp_str = String::from_utf8_lossy(&resp);
    if !resp_str.contains("LOKE") {
        return Err(SamsungError::HandshakeFailed);
    }
    // request session info
    write_bulk(&handle, &[0x64, 0x00, 0x00, 0x00])?;
    let info = read_bulk(&handle, 256)?;
    let pit_size = if info.len() >= 8 {
        u32::from_le_bytes([info[4], info[5], info[6], info[7]])
    } else {
        0
    };
    Ok(OdinInfo {
        protocol_version: "3.14".to_string(),
        pit_size,
    })
}

pub fn samsung_read_pit() -> Result<Vec<PitEntry>, SamsungError> {
    let handle = open_samsung()?;

    // Request PIT dump: cmd 0x65
    write_bulk(&handle, &[0x65, 0x00, 0x00, 0x00])?;
    let resp = read_bulk(&handle, 8)?;
    if resp.len() < 4 {
        return Err(SamsungError::PitFailed);
    }
    let pit_len = u32::from_le_bytes([resp[0], resp[1], resp[2], resp[3]]) as usize;

    let mut pit_data = Vec::with_capacity(pit_len);
    let mut received = 0usize;
    while received < pit_len {
        let chunk = read_bulk(&handle, 4096.min(pit_len - received))?;
        pit_data.extend_from_slice(&chunk);
        received += chunk.len();
    }

    parse_pit(&pit_data)
}

fn parse_pit(data: &[u8]) -> Result<Vec<PitEntry>, SamsungError> {
    if data.len() < 8 {
        return Err(SamsungError::PitFailed);
    }
    let magic = u32::from_le_bytes([data[0], data[1], data[2], data[3]]);
    if magic != 0x12349876 {
        return Err(SamsungError::PitFailed);
    }
    let count = u32::from_le_bytes([data[4], data[5], data[6], data[7]]) as usize;
    let mut entries = Vec::with_capacity(count);
    let mut offset = 8usize;
    const ENTRY_SIZE: usize = 132;

    for _ in 0..count {
        if offset + ENTRY_SIZE > data.len() {
            break;
        }
        let e = &data[offset..offset + ENTRY_SIZE];
        let binary_type = u32::from_le_bytes([e[0], e[1], e[2], e[3]]);
        let device_type = u32::from_le_bytes([e[4], e[5], e[6], e[7]]);
        let partition_id = u32::from_le_bytes([e[8], e[9], e[10], e[11]]);
        let partition_type = u32::from_le_bytes([e[12], e[13], e[14], e[15]]);
        let file_system = u32::from_le_bytes([e[16], e[17], e[18], e[19]]);
        let _ = (binary_type, file_system);

        let blk_start = u32::from_le_bytes([e[20], e[21], e[22], e[23]]) as u64;
        let blk_count = u32::from_le_bytes([e[24], e[25], e[26], e[27]]) as u64;

        let partition_name = cstr(&e[28..56]);
        let flash_filename = cstr(&e[56..84]);

        entries.push(PitEntry {
            partition_name,
            flash_filename,
            partition_id,
            partition_type,
            device_type,
            offset: blk_start * 512,
            size: blk_count * 512,
        });
        offset += ENTRY_SIZE;
    }
    Ok(entries)
}

fn cstr(bytes: &[u8]) -> String {
    let end = bytes.iter().position(|&b| b == 0).unwrap_or(bytes.len());
    String::from_utf8_lossy(&bytes[..end]).to_string()
}

pub fn samsung_flash_partition(
    _partition_name: &str,
    file_path: &std::path::Path,
    on_progress: impl Fn(u32),
) -> Result<(), SamsungError> {
    let handle = open_samsung()?;
    let data = std::fs::read(file_path)
        .map_err(|e| SamsungError::FlashFailed(e.to_string()))?;

    let total = data.len();
    // begin flash cmd: 0x66
    let mut cmd = vec![0u8; 8];
    cmd[0] = 0x66;
    let len_bytes = (total as u32).to_le_bytes();
    cmd[4..8].copy_from_slice(&len_bytes);
    write_bulk(&handle, &cmd)?;
    read_bulk(&handle, 8)?;

    let chunk_size = 131072usize; // 128KB
    let mut sent = 0usize;
    for chunk in data.chunks(chunk_size) {
        write_bulk(&handle, chunk)?;
        read_bulk(&handle, 8)?;
        sent += chunk.len();
        let pct = ((sent as f64 / total as f64) * 100.0) as u32;
        on_progress(pct);
    }

    // end flash
    write_bulk(&handle, &[0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])?;
    read_bulk(&handle, 8)?;

    on_progress(100);
    Ok(())
}

pub fn samsung_reboot(mode: u8) -> Result<(), SamsungError> {
    let handle = open_samsung()?;
    let cmd = [0x72, 0x00, 0x00, 0x00, mode, 0x00, 0x00, 0x00];
    write_bulk(&handle, &cmd)?;
    Ok(())
}

pub fn samsung_erase_frp() -> Result<(), SamsungError> {
    let pit = samsung_read_pit()?;
    let frp = pit
        .iter()
        .find(|e| e.partition_name.to_lowercase() == "frp")
        .ok_or(SamsungError::FlashFailed("frp partition not found".into()))?;

    let size = frp.size as usize;
    let zeros = vec![0u8; size];
    let tmp = std::env::temp_dir().join("frp_zero.bin");
    std::fs::write(&tmp, &zeros)
        .map_err(|e| SamsungError::FlashFailed(e.to_string()))?;

    samsung_flash_partition("frp", &tmp, |_| {})?;
    let _ = std::fs::remove_file(&tmp);
    Ok(())
}

// ── Tauri Commands ──────────────────────────────────────────────

#[tauri::command]
pub fn samsung_find_device_cmd() -> Result<SamsungDevice, String> {
    find_samsung_device().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn samsung_do_handshake_cmd() -> Result<OdinInfo, String> {
    samsung_odin_handshake().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn samsung_get_pit_cmd() -> Result<Vec<PitEntry>, String> {
    samsung_read_pit().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn samsung_flash_part_cmd(name: String, file_path: String) -> Result<(), String> {
    samsung_flash_partition(&name, std::path::Path::new(&file_path), |_| {})
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub fn samsung_do_erase_frp_cmd() -> Result<(), String> {
    samsung_erase_frp().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn samsung_reboot_device_cmd(mode: u8) -> Result<(), String> {
    samsung_reboot(mode).map_err(|e| e.to_string())
}

