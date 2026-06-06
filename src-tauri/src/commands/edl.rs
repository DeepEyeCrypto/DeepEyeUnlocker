use super::usb_utils::{check_winusb_installed, open_and_claim, EP_IN, EP_OUT};
use rusb::{Device, DeviceDescriptor, DeviceHandle, GlobalContext};
use serde::Serialize;
use std::time::Duration;

const EDL_VID: u16 = 0x05c6;
const EDL_PID_9008: u16 = 0x9008;
const EDL_PID_900E: u16 = 0x900e;

const TIMEOUT: Duration = Duration::from_millis(5000);

#[derive(Debug, Serialize, Clone)]
pub struct EdlDeviceInfo {
    pub vid: u16,
    pub pid: u16,
    pub serial: Option<String>,
    pub programmer_loaded: bool,
}

#[derive(Debug, Serialize, Clone)]
pub struct SaharaInfo {
    pub version: u32,
    pub mode: u32,
    pub max_packet_size: u32,
}

#[derive(Debug, Serialize)]
pub struct StorageInfo {
    pub total_blocks: u64,
    pub block_size: u32,
    pub storage_type: String,
}

#[allow(dead_code)]
#[derive(Debug, thiserror::Error, Serialize)]
pub enum EdlError {
    #[error("EDL device not found")]
    DeviceNotFound,
    #[error("USB error: {0}")]
    UsbError(String),
    #[error("Sahara handshake failed: {0}")]
    SaharaFailed(String),
    #[error("Programmer upload failed: {0}")]
    ProgrammerFailed(String),
    #[error("Firehose NAK: {0}")]
    FirehoseNak(String),
    #[error("XML parse error: {0}")]
    XmlError(String),
    #[error("Partition not found: {0}")]
    PartitionNotFound(String),
    #[error("Timeout")]
    Timeout,
}

fn usb_access_hint() -> &'static str {
    "macOS: check USB entitlements or run with sudo during development; Windows: install WinUSB with Zadig; Linux: install 99-deepeye.rules and re-login"
}

fn map_usb_error(operation: &str, error: rusb::Error) -> EdlError {
    match error {
        rusb::Error::Timeout => EdlError::Timeout,
        rusb::Error::Access => {
            EdlError::UsbError(format!("{operation}: {error} — {}", usb_access_hint()))
        }
        _ => EdlError::UsbError(format!("{operation}: {error}")),
    }
}

fn is_edl_pid(pid: u16) -> bool {
    pid == EDL_PID_9008 || pid == EDL_PID_900E
}

fn validate_xml_safe(value: &str) -> Result<(), EdlError> {
    if !value
        .chars()
        .all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '-')
    {
        return Err(EdlError::XmlError(
            "Invalid characters in XML parameter".into(),
        ));
    }
    Ok(())
}

fn find_edl_transport() -> Result<(Device<GlobalContext>, DeviceDescriptor), EdlError> {
    let devices = rusb::devices().map_err(|error| map_usb_error("enumerate USB devices", error))?;

    for device in devices.iter() {
        let desc = device
            .device_descriptor()
            .map_err(|error| map_usb_error("read EDL device descriptor", error))?;

        if desc.vendor_id() == EDL_VID && is_edl_pid(desc.product_id()) {
            return Ok((device, desc));
        }
    }

    Err(EdlError::DeviceNotFound)
}

fn ensure_driver_ready(vid: u16, pid: u16) -> Result<(), EdlError> {
    if check_winusb_installed(vid, pid) {
        return Ok(());
    }

    Err(EdlError::UsbError(format!(
        "WinUSB driver not installed for {vid:04x}:{pid:04x} — install with Zadig"
    )))
}

pub fn open_edl_device() -> Result<DeviceHandle<GlobalContext>, EdlError> {
    let (device, desc) = find_edl_transport()?;
    ensure_driver_ready(desc.vendor_id(), desc.product_id())?;
    open_and_claim(&device).map_err(|error| map_usb_error("open EDL device", error))
}

pub fn find_edl_device() -> Result<EdlDeviceInfo, EdlError> {
    let (device, desc) = find_edl_transport()?;

    println!(
        "[edl] Found EDL device: {:04x}:{:04x}",
        desc.vendor_id(),
        desc.product_id()
    );

    ensure_driver_ready(desc.vendor_id(), desc.product_id())?;

    let handle =
        open_and_claim(&device).map_err(|error| map_usb_error("open EDL device", error))?;
    let serial = handle.read_serial_number_string_ascii(&desc).ok();

    Ok(EdlDeviceInfo {
        vid: desc.vendor_id(),
        pid: desc.product_id(),
        serial,
        programmer_loaded: desc.product_id() == EDL_PID_900E,
    })
}

pub fn sahara_handshake(handle: &DeviceHandle<GlobalContext>) -> Result<SaharaInfo, EdlError> {
    let mut buf = [0u8; 1024];
    let len = handle
        .read_bulk(EP_IN, &mut buf, TIMEOUT)
        .map_err(|e| EdlError::UsbError(format!("Read failed: {}", e)))?;

    if len < 24 {
        return Err(EdlError::SaharaFailed("Incomplete Hello Packet".into()));
    }
    let cmd = u32::from_le_bytes(buf[0..4].try_into().unwrap());
    if cmd != 0x01 {
        // SAHARA_HELLO
        return Err(EdlError::SaharaFailed(format!(
            "Expected Hello (1), got {}",
            cmd
        )));
    }

    let version = u32::from_le_bytes(buf[8..12].try_into().unwrap());
    let max_packet_size = u32::from_le_bytes(buf[16..20].try_into().unwrap());
    let mode = u32::from_le_bytes(buf[20..24].try_into().unwrap());

    // Send Hello Response
    let mut resp = vec![0u8; 48];
    resp[0..4].copy_from_slice(&2u32.to_le_bytes()); // CMD: HELLO_RESP
    resp[4..8].copy_from_slice(&48u32.to_le_bytes()); // Length
    resp[8..12].copy_from_slice(&2u32.to_le_bytes()); // Version
    resp[12..16].copy_from_slice(&1u32.to_le_bytes()); // Version Compat
    resp[16..20].copy_from_slice(&0u32.to_le_bytes()); // Status OK
    resp[20..24].copy_from_slice(&mode.to_le_bytes()); // Mode

    handle
        .write_bulk(EP_OUT, &resp, TIMEOUT)
        .map_err(|e| EdlError::UsbError(format!("Write failed: {}", e)))?;

    Ok(SaharaInfo {
        version,
        mode,
        max_packet_size,
    })
}

pub fn sahara_upload_programmer(
    handle: &DeviceHandle<GlobalContext>,
    programmer_path: &std::path::Path,
) -> Result<(), EdlError> {
    let payload = std::fs::read(programmer_path)
        .map_err(|e| EdlError::ProgrammerFailed(format!("File read err: {}", e)))?;

    loop {
        let mut buf = [0u8; 1024];
        let len = match handle.read_bulk(EP_IN, &mut buf, TIMEOUT) {
            Ok(l) => l,
            Err(e) => return Err(EdlError::UsbError(e.to_string())),
        };

        if len < 8 {
            continue;
        }
        let cmd = u32::from_le_bytes(buf[0..4].try_into().unwrap());

        if cmd == 0x03 {
            // READ_DATA
            if len < 20 {
                return Err(EdlError::ProgrammerFailed("Short READ_DATA packet".into()));
            }
            let _image_id = u32::from_le_bytes(buf[8..12].try_into().unwrap());
            let offset = u32::from_le_bytes(buf[12..16].try_into().unwrap()) as usize;
            let length = u32::from_le_bytes(buf[16..20].try_into().unwrap()) as usize;

            if offset > payload.len() {
                return Err(EdlError::ProgrammerFailed(format!(
                    "Offset {} exceeds payload length {}",
                    offset,
                    payload.len()
                )));
            }

            let end = std::cmp::min(offset + length, payload.len());
            let chunk = &payload[offset..end];
            handle
                .write_bulk(EP_OUT, chunk, TIMEOUT)
                .map_err(|e| EdlError::UsbError(e.to_string()))?;
        } else if cmd == 0x04 {
            // END_OF_IMG
            // Send DONE
            let mut done_pkt = vec![0u8; 8];
            done_pkt[0..4].copy_from_slice(&5u32.to_le_bytes());
            done_pkt[4..8].copy_from_slice(&8u32.to_le_bytes());
            handle.write_bulk(EP_OUT, &done_pkt, TIMEOUT).ok();
            break;
        } else if cmd == 0x06 {
            // DONE_RESP
            break;
        } else {
            return Err(EdlError::ProgrammerFailed(format!(
                "Unexpected Sahara CMD: {}",
                cmd
            )));
        }
    }
    Ok(())
}

pub fn firehose_send(handle: &DeviceHandle<GlobalContext>, xml: &str) -> Result<String, EdlError> {
    handle
        .write_bulk(EP_OUT, xml.as_bytes(), TIMEOUT)
        .map_err(|e| EdlError::UsbError(e.to_string()))?;

    let mut buf = [0u8; 4096];
    let len = handle
        .read_bulk(EP_IN, &mut buf, TIMEOUT)
        .map_err(|e| EdlError::UsbError(e.to_string()))?;

    let resp = String::from_utf8_lossy(&buf[0..len]).to_string();
    if resp.contains("value=\"NAK\"") {
        return Err(EdlError::FirehoseNak(resp));
    }
    Ok(resp)
}

pub fn firehose_configure(
    handle: &DeviceHandle<GlobalContext>,
    max_payload: u32,
    _sector_size: u32,
) -> Result<(), EdlError> {
    let xml = format!(
        "<?xml version=\"1.0\" ?><data><configure TargetName=\"sdm660\" MaxPayloadSizeToTargetInBytes=\"{}\" ZlpAwareHost=\"1\" SkipStorageInit=\"0\"/></data>",
        max_payload
    );
    firehose_send(handle, &xml)?;
    Ok(())
}

pub fn firehose_erase(
    handle: &DeviceHandle<GlobalContext>,
    partition_name: &str,
) -> Result<(), EdlError> {
    validate_xml_safe(partition_name)?;
    let xml = format!(
        "<?xml version=\"1.0\" ?><data><erase SECTOR_SIZE_IN_BYTES=\"4096\" label=\"{}\" /></data>",
        partition_name
    );
    firehose_send(handle, &xml)?;
    Ok(())
}

pub fn firehose_read_partition(
    handle: &DeviceHandle<GlobalContext>,
    partition_name: &str,
    num_sectors: u64,
    out_path: &std::path::Path,
) -> Result<u64, EdlError> {
    validate_xml_safe(partition_name)?;
    let xml = format!(
        "<?xml version=\"1.0\" ?><data><read SECTOR_SIZE_IN_BYTES=\"4096\" label=\"{}\" num_partition_sectors=\"{}\" /></data>",
        partition_name, num_sectors
    );
    firehose_send(handle, &xml)?;

    // Real bulk read loop — read chunks until total expected size is received
    use std::io::Write;
    let mut file = std::fs::File::create(out_path)
        .map_err(|e| EdlError::UsbError(format!("Cannot create output file: {e}")))?;

    let total_expected = num_sectors * 4096;
    let mut total_read: u64 = 0;
    let mut buf = vec![0u8; 1048576]; // 1MB read buffer

    while total_read < total_expected {
        let n = handle.read_bulk(EP_IN, &mut buf, TIMEOUT).map_err(|e| {
            EdlError::UsbError(format!("Bulk read failed at offset {total_read}: {e}"))
        })?;
        if n == 0 {
            break;
        }
        file.write_all(&buf[..n])
            .map_err(|e| EdlError::UsbError(format!("File write failed: {e}")))?;
        total_read += n as u64;
    }

    // Read final ACK/response XML from Firehose
    let mut resp_buf = [0u8; 4096];
    let _ = handle.read_bulk(EP_IN, &mut resp_buf, TIMEOUT).ok();

    Ok(total_read)
}

pub fn firehose_write_partition(
    handle: &DeviceHandle<GlobalContext>,
    partition_name: &str,
    data_path: &std::path::Path,
) -> Result<(), EdlError> {
    validate_xml_safe(partition_name)?;
    let data = std::fs::read(data_path)
        .map_err(|e| EdlError::UsbError(format!("Cannot read data file: {e}")))?;
    let size = data.len() as u64;
    let num_sectors = size.div_ceil(4096);
    let xml = format!(
        "<?xml version=\"1.0\" ?><data><program SECTOR_SIZE_IN_BYTES=\"4096\" label=\"{}\" num_partition_sectors=\"{}\" /></data>",
        partition_name, num_sectors
    );
    firehose_send(handle, &xml)?;

    // Real bulk write loop — send data in chunks
    let chunk_size: usize = 1048576; // 1MB write chunks
    for chunk in data.chunks(chunk_size) {
        handle
            .write_bulk(EP_OUT, chunk, TIMEOUT)
            .map_err(|e| EdlError::UsbError(format!("Bulk write failed: {e}")))?;
    }

    // Read final ACK
    let mut resp_buf = [0u8; 4096];
    let n = handle
        .read_bulk(EP_IN, &mut resp_buf, TIMEOUT)
        .map_err(|e| EdlError::UsbError(format!("ACK read failed: {e}")))?;
    let resp = String::from_utf8_lossy(&resp_buf[..n]).to_string();
    if resp.contains("value=\"NAK\"") {
        return Err(EdlError::FirehoseNak(resp));
    }

    Ok(())
}

pub fn firehose_get_storage_info(
    handle: &DeviceHandle<GlobalContext>,
) -> Result<StorageInfo, EdlError> {
    let xml =
        "<?xml version=\"1.0\" ?><data><getStorageInfo physical_partition_number=\"0\"/></data>";
    let resp = firehose_send(handle, xml)?;

    // Parse real XML response for storage parameters
    let extract_attr = |attr: &str| -> u64 {
        resp.find(attr)
            .and_then(|i| {
                let start = i + attr.len();
                let rest = &resp[start..];
                // Find quoted value: attr="value"
                rest.find('"')
                    .and_then(|q1| {
                        let after = &rest[q1 + 1..];
                        after.find('"').map(|q2| &after[..q2])
                    })
                    .and_then(|val| val.parse::<u64>().ok())
            })
            .unwrap_or(0)
    };

    let total_blocks = extract_attr("num_physical_partitions=");
    let block_size_val = extract_attr("SECTOR_SIZE_IN_BYTES=");

    // Determine storage type from response
    let storage_type = if resp.contains("ufs") || resp.contains("UFS") {
        "UFS"
    } else if resp.contains("emmc") || resp.contains("eMMC") {
        "eMMC"
    } else {
        "Unknown"
    };

    Ok(StorageInfo {
        total_blocks,
        block_size: if block_size_val > 0 {
            block_size_val as u32
        } else {
            4096
        },
        storage_type: storage_type.to_string(),
    })
}

pub fn firehose_reboot(handle: &DeviceHandle<GlobalContext>, mode: &str) -> Result<(), EdlError> {
    validate_xml_safe(mode)?;
    let xml = format!(
        "<?xml version=\"1.0\" ?><data><power value=\"{}\"/></data>",
        mode
    );
    firehose_send(handle, &xml)?;
    Ok(())
}

// -----------------------------------------------------------------------------------------------
// Tauri Commands
// -----------------------------------------------------------------------------------------------

#[tauri::command]
pub async fn edl_find_device() -> Result<EdlDeviceInfo, String> {
    tokio::task::spawn_blocking(find_edl_device)
        .await
        .map_err(|e| e.to_string())?
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_sahara_handshake() -> Result<SaharaInfo, String> {
    tokio::task::spawn_blocking(|| {
        let handle = open_edl_device()?;
        sahara_handshake(&handle)
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_upload_programmer(path: String) -> Result<(), String> {
    tokio::task::spawn_blocking(move || {
        let handle = open_edl_device()?;
        sahara_upload_programmer(&handle, std::path::Path::new(&path))
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_configure(max_payload: u32, sector_size: u32) -> Result<(), String> {
    tokio::task::spawn_blocking(move || {
        let handle = open_edl_device()?;
        firehose_configure(&handle, max_payload, sector_size)
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_erase_partition(name: String) -> Result<(), String> {
    tokio::task::spawn_blocking(move || {
        let handle = open_edl_device()?;
        firehose_erase(&handle, &name)
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_read_partition(
    name: String,
    sectors: u64,
    out_path: String,
) -> Result<u64, String> {
    tokio::task::spawn_blocking(move || {
        let handle = open_edl_device()?;
        firehose_read_partition(&handle, &name, sectors, std::path::Path::new(&out_path))
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_write_partition(name: String, data_path: String) -> Result<(), String> {
    tokio::task::spawn_blocking(move || {
        let handle = open_edl_device()?;
        firehose_write_partition(&handle, &name, std::path::Path::new(&data_path))
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_get_storage_info() -> Result<StorageInfo, String> {
    tokio::task::spawn_blocking(|| {
        let handle = open_edl_device()?;
        firehose_get_storage_info(&handle)
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn edl_reboot(mode: String) -> Result<(), String> {
    tokio::task::spawn_blocking(move || {
        let handle = open_edl_device()?;
        firehose_reboot(&handle, &mode)
    })
    .await
    .map_err(|e| e.to_string())?
    .map_err(|e| e.to_string())
}
