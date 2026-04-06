use std::{fs, sync::{Mutex, MutexGuard}, time::Duration};

use super::usb_utils::{
    debug_list_usb_devices, open_and_claim_with_options, ClaimOptions, EP_IN as USB_EP_IN,
    EP_OUT as USB_EP_OUT, TIMEOUT as USB_TIMEOUT,
};
use rusb::{
    Device, DeviceDescriptor, DeviceHandle, Direction, GlobalContext, TransferType, UsbContext,
};
use serde::Serialize;
use thiserror::Error;

const MTK_VENDOR_ID: u16 = 0x0e8d;
const MTK_BROM_PID: u16 = 0x0003;
const MTK_PRELOADER_PID: u16 = 0x2000;
const BROM_EP_OUT: u8 = USB_EP_OUT;
const BROM_EP_IN: u8 = USB_EP_IN;
const BROM_TIMEOUT: Duration = USB_TIMEOUT;
const BROM_DA_LOAD_ADDR: u32 = 0x0020_0000;
const BROM_DA_CHUNK_SIZE: usize = 4096;
const BROM_DA_CHUNK_ACK: u8 = 0x69;
const BROM_DA_FINAL_STATUS: u8 = 0x5A;
const BROM_SLA_ACK: u8 = 0x00;
const BROM_JUMP_READY_ACK: [u8; 2] = [0x5A, 0x5A];
const DA_STATUS_OK: u16 = 0x0000;
const DA_CMD_ERASE_PARTITION: u16 = 0x5000;
const DA_CMD_FORMAT_PARTITION: u16 = 0x5001;
const DA_CMD_READ_IMEI: u16 = 0x5003;
const DA_CMD_WRITE_IMEI: u16 = 0x5004;
const DA_CMD_REBOOT: u16 = 0x5010;
const DA_CMD_LIST_PARTITIONS: u16 = 0x5005;
const DA_CMD_READ_PARTITION: u16 = 0x5006;
const DA_CMD_WRITE_PARTITION: u16 = 0x5007;
const DA_CMD_ERASE_PARTITION_DA: u16 = 0x5008;
const DA_CHUNK_SIZE: usize = 4096;

// [INFERRED] MediaTek Boot ROM handshake bytes follow the public BROM bootstrap exchange used by mtkclient and SP Flash Tool traces.
const BROM_HANDSHAKE_SEQUENCE: [(u8, u8); 4] = [(0xA0, 0x5F), (0x0A, 0xF5), (0x50, 0xAF), (0x05, 0xFA)];

static ACTIVE_BROM_SESSION: Mutex<Option<ActiveBromSession>> = Mutex::new(None);

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
pub enum MtkMode {
    Brom,
    Preloader,
}

#[derive(Debug, Serialize, Clone)]
pub struct MtkDevice {
    pub mode: MtkMode,
    pub vid: u16,
    pub pid: u16,
    pub bus: u8,
    pub address: u8,
}

#[derive(Debug, Serialize, Clone)]
pub struct ChipInfo {
    pub hw_code: u16,
    pub chip_name: String,
    pub hw_sub_code: u16,
    pub hw_ver: u16,
    pub sw_ver: u16,
}

#[derive(Debug, Serialize, Clone, Copy, PartialEq, Eq)]
pub enum AuthType {
    None,
    Sla,
    Daa,
    SlaDaa,
}

#[derive(Debug, Serialize, Clone)]
pub struct DaUploadResult {
    pub bytes_uploaded: usize,
    pub checksum: u16,
}

#[derive(Debug, Serialize, Clone)]
pub struct DaJumpInfo {
    pub status: u8,
    pub version: u16,
    pub capability: u16,
}

#[derive(Debug, Serialize, Clone)]
pub struct ImeiInfo {
    pub imei1: String,
    pub imei2: Option<String>,
}

#[derive(Debug, Serialize, Clone)]
pub struct PartitionEntry {
    pub name: String,
    pub start: u64,
    pub size: u64,
    pub partition_type: u8,
}

#[derive(Debug, Error, Serialize)]
pub enum BromError {
    #[error("No MTK device found in BROM or Preloader mode")]
    DeviceNotFound,
    #[error("USB error: {0}")]
    UsbError(String),
    #[error("Handshake failed at byte {byte}: expected {expected:#x} got {got:#x}")]
    HandshakeFailed { byte: u8, expected: u8, got: u8 },
    #[error("Unknown chip code: {0:#x}")]
    UnknownChip(u16),
    #[error("Timeout waiting for device response")]
    Timeout,
    #[error("DA upload failed: {0}")]
    DaUploadFailed(String),
    #[error("Auth type not supported: {0:?}")]
    AuthNotSupported(AuthType),
    #[error("DA command {cmd:#06x} failed with status {status:#06x}")]
    DaCommandFailed { cmd: u16, status: u16 },
    #[error("Invalid IMEI: {0}")]
    InvalidImei(String),
    #[error("Partition not found: {0}")]
    PartitionNotFound(String),
}

#[derive(Debug, Clone, Copy)]
struct InterfaceLayout {
    config_value: u8,
    interface_number: u8,
    alternate_setting: u8,
}

struct ActiveBromSession {
    handle: DeviceHandle<GlobalContext>,
    layout: InterfaceLayout,
    auth_type: Option<AuthType>,
    sla_bypassed: bool,
    da_uploaded: bool,
}

fn debug_enabled() -> bool {
    match std::env::var("DEBUG") {
        Ok(value) => !matches!(value.trim(), "" | "0" | "false" | "FALSE" | "False"),
        Err(_) => false,
    }
}

fn debug_log(message: impl AsRef<str>) {
    if debug_enabled() {
        eprintln!("[mtk_brom] {}", message.as_ref());
    }
}

fn usb_access_hint() -> &'static str {
    "macOS: check USB entitlements or run with sudo during development; Windows: install WinUSB with Zadig; Linux: install 99-deepeye.rules and re-login"
}

fn map_usb_error(operation: &str, error: rusb::Error) -> BromError {
    match error {
        rusb::Error::Timeout => BromError::Timeout,
        rusb::Error::Access => BromError::UsbError(format!(
            "{operation}: {error} — {}",
            usb_access_hint()
        )),
        _ => BromError::UsbError(format!("{operation}: {error}")),
    }
}

fn mode_from_pid(pid: u16) -> Option<MtkMode> {
    match pid {
        MTK_BROM_PID => Some(MtkMode::Brom),
        MTK_PRELOADER_PID => Some(MtkMode::Preloader),
        _ => None,
    }
}

fn build_device<T: UsbContext>(device: &Device<T>, descriptor: &DeviceDescriptor) -> Option<MtkDevice> {
    let mode = mode_from_pid(descriptor.product_id())?;

    Some(MtkDevice {
        mode,
        vid: descriptor.vendor_id(),
        pid: descriptor.product_id(),
        bus: device.bus_number(),
        address: device.address(),
    })
}

// [INFERRED] MediaTek BROM and Preloader transports expose a bulk IN/OUT pair on EP 0x81/0x01 for bootstrap commands.
fn find_transport_interface(
    device: &Device<GlobalContext>,
    descriptor: &DeviceDescriptor,
) -> Option<InterfaceLayout> {
    for config_index in 0..descriptor.num_configurations() {
        let Ok(config) = device.config_descriptor(config_index) else {
            continue;
        };

        for interface in config.interfaces() {
            for interface_descriptor in interface.descriptors() {
                let mut has_in = false;
                let mut has_out = false;

                for endpoint in interface_descriptor.endpoint_descriptors() {
                    if endpoint.transfer_type() != TransferType::Bulk {
                        continue;
                    }

                    match (endpoint.direction(), endpoint.address()) {
                        (Direction::In, BROM_EP_IN) => has_in = true,
                        (Direction::Out, BROM_EP_OUT) => has_out = true,
                        _ => {}
                    }
                }

                if has_in && has_out {
                    return Some(InterfaceLayout {
                        config_value: config.number(),
                        interface_number: interface_descriptor.interface_number(),
                        alternate_setting: interface_descriptor.setting_number(),
                    });
                }
            }
        }
    }

    None
}

pub fn find_mtk_device() -> Option<MtkDevice> {
    let devices = rusb::devices().ok()?;

    #[cfg(debug_assertions)]
    debug_list_usb_devices();

    for device in devices.iter() {
        let Ok(descriptor) = device.device_descriptor() else {
            continue;
        };

        if descriptor.vendor_id() != MTK_VENDOR_ID {
            continue;
        }

        if let Some(mtk_device) = build_device(&device, &descriptor) {
            return Some(mtk_device);
        }
    }

    None
}

fn detect_mtk_device_blocking() -> Result<MtkDevice, BromError> {
    find_mtk_device().ok_or(BromError::DeviceNotFound)
}

fn locate_brom_device() -> Result<(Device<GlobalContext>, InterfaceLayout), BromError> {
    let devices = rusb::devices().map_err(|error| map_usb_error("enumerate USB devices", error))?;
    let mut preloader_candidate = None;

    for device in devices.iter() {
        let Ok(descriptor) = device.device_descriptor() else {
            continue;
        };

        if descriptor.vendor_id() != MTK_VENDOR_ID {
            continue;
        }

        match descriptor.product_id() {
            MTK_BROM_PID => {
                let layout = find_transport_interface(&device, &descriptor).ok_or_else(|| {
                    BromError::UsbError(format!(
                        "BROM bulk transport not found on bus {} address {} (expected OUT {BROM_EP_OUT:#04x}, IN {BROM_EP_IN:#04x})",
                        device.bus_number(),
                        device.address(),
                    ))
                })?;

                return Ok((device, layout));
            }
            MTK_PRELOADER_PID => {
                if preloader_candidate.is_none() {
                    preloader_candidate = build_device(&device, &descriptor);
                }
            }
            _ => {}
        }
    }

    if let Some(device) = preloader_candidate {
        return Err(BromError::UsbError(format!(
            "Detected MTK device in {:?} mode at bus {} address {}; BROM handshake requires PID {MTK_BROM_PID:#06x}",
            device.mode, device.bus, device.address,
        )));
    }

    Err(BromError::DeviceNotFound)
}

// [INFERRED] The BROM transport requires opening the vendor-specific interface and claiming the bulk endpoints before any command exchange.
fn open_brom_handle() -> Result<(DeviceHandle<GlobalContext>, InterfaceLayout), BromError> {
    let (device, layout) = locate_brom_device()?;
    let handle = open_and_claim_with_options(
        &device,
        ClaimOptions {
            config_value: Some(layout.config_value),
            interface_number: layout.interface_number,
            alternate_setting: Some(layout.alternate_setting),
        },
    )
    .map_err(|error| map_usb_error("open BROM device", error))?;

    Ok((handle, layout))
}

fn write_exact(handle: &DeviceHandle<GlobalContext>, payload: &[u8]) -> Result<(), BromError> {
    let written = handle
        .write_bulk(BROM_EP_OUT, payload, BROM_TIMEOUT)
        .map_err(|error| map_usb_error("bulk write", error))?;

    if written != payload.len() {
        return Err(BromError::UsbError(format!(
            "Short USB write: expected {} byte(s), wrote {written}",
            payload.len(),
        )));
    }

    Ok(())
}

fn read_exact(handle: &DeviceHandle<GlobalContext>, buffer: &mut [u8]) -> Result<(), BromError> {
    let read = handle
        .read_bulk(BROM_EP_IN, buffer, BROM_TIMEOUT)
        .map_err(|error| map_usb_error("bulk read", error))?;

    if read != buffer.len() {
        return Err(BromError::UsbError(format!(
            "Short USB read: expected {} byte(s), received {read}",
            buffer.len(),
        )));
    }

    Ok(())
}

fn release_claimed_interface(handle: &DeviceHandle<GlobalContext>, layout: InterfaceLayout) -> Result<(), BromError> {
    handle
        .release_interface(layout.interface_number)
        .map_err(|error| map_usb_error("release interface", error))
}

fn with_temporary_brom_handle<T, F>(callback: F) -> Result<T, BromError>
where
    F: FnOnce(&DeviceHandle<GlobalContext>) -> Result<T, BromError>,
{
    let (handle, layout) = open_brom_handle()?;
    let operation_result = callback(&handle);
    let release_result = release_claimed_interface(&handle, layout);

    match (operation_result, release_result) {
        (Err(error), _) => Err(error),
        (Ok(_), Err(error)) => Err(error),
        (Ok(value), Ok(())) => Ok(value),
    }
}

fn with_temporary_handshaken_brom_handle<T, F>(callback: F) -> Result<T, BromError>
where
    F: FnOnce(&DeviceHandle<GlobalContext>) -> Result<T, BromError>,
{
    with_temporary_brom_handle(|handle| {
        brom_handshake(handle)?;
        callback(handle)
    })
}

fn lock_active_session() -> Result<MutexGuard<'static, Option<ActiveBromSession>>, BromError> {
    ACTIVE_BROM_SESSION
        .lock()
        .map_err(|_| BromError::UsbError("Active BROM session lock poisoned".to_string()))
}

fn replace_active_session(session: ActiveBromSession) -> Result<(), BromError> {
    let previous_session = {
        let mut guard = lock_active_session()?;
        guard.replace(session)
    };

    if let Some(previous_session) = previous_session {
        if let Err(error) = release_claimed_interface(&previous_session.handle, previous_session.layout) {
            debug_log(format!("session cleanup warning: {error}"));
        }
    }

    Ok(())
}

fn take_active_session() -> Result<Option<ActiveBromSession>, BromError> {
    let mut guard = lock_active_session()?;
    Ok(guard.take())
}

fn clear_active_session() -> Result<(), BromError> {
    let previous_session = take_active_session()?;

    if let Some(previous_session) = previous_session {
        release_claimed_interface(&previous_session.handle, previous_session.layout)?;
    }

    Ok(())
}

fn with_active_session<T, F>(callback: F) -> Result<T, BromError>
where
    F: FnOnce(&mut ActiveBromSession) -> Result<T, BromError>,
{
    let mut guard = lock_active_session()?;
    let session = guard.as_mut().ok_or_else(|| {
        BromError::DaUploadFailed("No active BROM session. Run SLA bypass or upload DA first".to_string())
    })?;

    callback(session)
}

fn open_handshaken_session() -> Result<ActiveBromSession, BromError> {
    let (handle, layout) = open_brom_handle()?;

    if let Err(error) = brom_handshake(&handle) {
        if let Err(release_error) = release_claimed_interface(&handle, layout) {
            debug_log(format!("session cleanup warning after handshake failure: {release_error}"));
        }
        return Err(error);
    }

    Ok(ActiveBromSession {
        handle,
        layout,
        auth_type: None,
        sla_bypassed: false,
        da_uploaded: false,
    })
}

fn ensure_active_session() -> Result<(), BromError> {
    let has_active_session = {
        let guard = lock_active_session()?;
        guard.is_some()
    };

    if !has_active_session {
        replace_active_session(open_handshaken_session()?)?;
    }

    Ok(())
}

pub fn brom_handshake(handle: &DeviceHandle<GlobalContext>) -> Result<(), BromError> {
    debug_log("starting BROM handshake");

    for (host_byte, expected_byte) in BROM_HANDSHAKE_SEQUENCE {
        write_exact(handle, &[host_byte])?;

        let mut response = [0u8; 1];
        read_exact(handle, &mut response)?;

        debug_log(format!(
            "handshake step sent={host_byte:#04x} received={:#04x}",
            response[0]
        ));

        if response[0] != expected_byte {
            return Err(BromError::HandshakeFailed {
                byte: host_byte,
                expected: expected_byte,
                got: response[0],
            });
        }
    }

    Ok(())
}

fn auth_type_from_response(response: u8) -> AuthType {
    let compact_bits = if response & 0x0C != 0 { (response & 0x0C) >> 2 } else { response & 0x03 };

    match compact_bits {
        0 => AuthType::None,
        1 => AuthType::Sla,
        2 => AuthType::Daa,
        3 => AuthType::SlaDaa,
        _ => AuthType::None,
    }
}

pub fn detect_auth_type(handle: &DeviceHandle<GlobalContext>) -> Result<AuthType, BromError> {
    write_exact(handle, &[0xD8])?;

    let mut response = [0u8; 1];
    read_exact(handle, &mut response)?;

    let auth_type = auth_type_from_response(response[0]);
    debug_log(format!(
        "detect_auth_type raw={:#04x} mapped={auth_type:?}",
        response[0]
    ));

    Ok(auth_type)
}

pub fn sla_bypass(handle: &DeviceHandle<GlobalContext>) -> Result<(), BromError> {
    let mut nonce = [0u8; 16];
    read_exact(handle, &mut nonce)?;

    let response = nonce.map(|byte| byte ^ 0xA5);
    write_exact(handle, &response)?;

    let mut ack = [0u8; 1];
    read_exact(handle, &mut ack)?;

    debug_log(format!("sla_bypass ack={:#04x}", ack[0]));

    if ack[0] != BROM_SLA_ACK {
        return Err(BromError::UsbError(format!(
            "SLA bypass failed: expected ACK {BROM_SLA_ACK:#04x}, got {:#04x}",
            ack[0]
        )));
    }

    Ok(())
}

fn xor_checksum(da_bytes: &[u8]) -> u16 {
    let mut checksum = 0u16;

    for word in da_bytes.chunks(2) {
        let value = match word {
            [high, low] => u16::from_be_bytes([*high, *low]),
            [high] => u16::from_be_bytes([*high, 0]),
            _ => 0,
        };

        checksum ^= value;
    }

    checksum
}

pub fn upload_da(handle: &DeviceHandle<GlobalContext>, da_bytes: &[u8]) -> Result<DaUploadResult, BromError> {
    let size = u32::try_from(da_bytes.len()).map_err(|_| {
        BromError::DaUploadFailed(format!("DA binary is too large: {} byte(s)", da_bytes.len()))
    })?;
    let checksum = xor_checksum(da_bytes);

    debug_log(format!(
        "upload_da start size={} checksum={checksum:#06x}",
        da_bytes.len()
    ));

    write_exact(handle, &[0xD7])?;
    write_exact(handle, &BROM_DA_LOAD_ADDR.to_be_bytes())?;
    write_exact(handle, &size.to_be_bytes())?;
    write_exact(handle, &checksum.to_be_bytes())?;

    for (chunk_index, chunk) in da_bytes.chunks(BROM_DA_CHUNK_SIZE).enumerate() {
        write_exact(handle, chunk)?;

        let mut ack = [0u8; 1];
        read_exact(handle, &mut ack)?;

        debug_log(format!(
            "upload_da chunk={} size={} ack={:#04x}",
            chunk_index,
            chunk.len(),
            ack[0]
        ));

        if ack[0] != BROM_DA_CHUNK_ACK {
            return Err(BromError::DaUploadFailed(format!(
                "Chunk {chunk_index} ACK mismatch: expected {BROM_DA_CHUNK_ACK:#04x}, got {:#04x}",
                ack[0]
            )));
        }
    }

    let mut status = [0u8; 1];
    read_exact(handle, &mut status)?;

    debug_log(format!("upload_da final_status={:#04x}", status[0]));

    if status[0] != BROM_DA_FINAL_STATUS {
        return Err(BromError::DaUploadFailed(format!(
            "Final DA status mismatch: expected {BROM_DA_FINAL_STATUS:#04x}, got {:#04x}",
            status[0]
        )));
    }

    Ok(DaUploadResult {
        bytes_uploaded: da_bytes.len(),
        checksum,
    })
}

pub fn jump_to_da(handle: &DeviceHandle<GlobalContext>) -> Result<DaJumpInfo, BromError> {
    write_exact(handle, &[0xD5])?;
    write_exact(handle, &BROM_DA_LOAD_ADDR.to_be_bytes())?;

    let mut status = [0u8; 1];
    read_exact(handle, &mut status)?;

    let mut version = [0u8; 2];
    read_exact(handle, &mut version)?;

    let mut capability = [0u8; 2];
    read_exact(handle, &mut capability)?;

    write_exact(handle, &BROM_JUMP_READY_ACK)?;

    let version = u16::from_be_bytes(version);
    let capability = u16::from_be_bytes(capability);

    debug_log(format!(
        "jump_to_da status={:#04x} version={version:#06x} capability={capability:#06x}",
        status[0]
    ));

    Ok(DaJumpInfo {
        status: status[0],
        version,
        capability,
    })
}

fn ensure_auth_type(session: &mut ActiveBromSession) -> Result<AuthType, BromError> {
    if let Some(auth_type) = session.auth_type {
        return Ok(auth_type);
    }

    let auth_type = detect_auth_type(&session.handle)?;
    session.auth_type = Some(auth_type);
    Ok(auth_type)
}

fn ensure_sla_bypass(session: &mut ActiveBromSession) -> Result<(), BromError> {
    let auth_type = ensure_auth_type(session)?;

    match auth_type {
        AuthType::None => Ok(()),
        AuthType::Sla | AuthType::SlaDaa => {
            if !session.sla_bypassed {
                debug_log(format!("applying SLA bypass for auth_type={auth_type:?}"));
                sla_bypass(&session.handle)?;
                session.sla_bypassed = true;
            }
            Ok(())
        }
        AuthType::Daa => Err(BromError::AuthNotSupported(auth_type)),
    }
}

// [INFERRED] GET_HW_CODE (0xFD), GET_HW_SUB_CODE (0xFC), GET_HW_VER (0xFB), and GET_SW_VER (0xFA) return big-endian 16-bit values on the BROM bulk IN endpoint.
fn read_u16_command(handle: &DeviceHandle<GlobalContext>, command: u8) -> Result<u16, BromError> {
    write_exact(handle, &[command])?;

    let mut response = [0u8; 2];
    read_exact(handle, &mut response)?;

    Ok(u16::from_be_bytes(response))
}

fn chip_name_from_code(hw_code: u16) -> Result<&'static str, BromError> {
    match hw_code {
        0x0321 => Ok("MT6735"),
        0x0335 => Ok("MT6737"),
        0x0337 => Ok("MT6753"),
        0x0507 => Ok("MT6750"),
        0x0788 => Ok("MT6789 (Dimensity 1080)"),
        0x6572 => Ok("MT6572"),
        0x6580 => Ok("MT6580"),
        0x6735 => Ok("MT6735"),
        0x6755 => Ok("MT6755 (Helio P10)"),
        0x6757 => Ok("MT6757 (Helio P25)"),
        0x6763 => Ok("MT6763 (Helio P23)"),
        0x6765 => Ok("MT6765 (Helio G85)"),
        0x6768 => Ok("MT6768 (Helio G85)"),
        0x6771 => Ok("MT6771 (Helio P70)"),
        0x6779 => Ok("MT6779 (Helio G90)"),
        0x6785 => Ok("MT6785 (Helio G90T)"),
        0x6833 => Ok("MT6833 (Dimensity 700)"),
        0x6853 => Ok("MT6853 (Dimensity 720)"),
        0x6873 => Ok("MT6873 (Dimensity 800)"),
        0x6877 => Ok("MT6877 (Dimensity 900)"),
        0x6983 => Ok("MT6983 (Dimensity 9200)"),
        _ => Err(BromError::UnknownChip(hw_code)),
    }
}

pub fn get_chip_info(handle: &DeviceHandle<GlobalContext>) -> Result<ChipInfo, BromError> {
    let hw_code = read_u16_command(handle, 0xFD)?;
    let hw_sub_code = read_u16_command(handle, 0xFC)?;
    let hw_ver = read_u16_command(handle, 0xFB)?;
    let sw_ver = read_u16_command(handle, 0xFA)?;

    Ok(ChipInfo {
        hw_code,
        chip_name: chip_name_from_code(hw_code)?.to_string(),
        hw_sub_code,
        hw_ver,
        sw_ver,
    })
}

fn handshake_and_identify_blocking() -> Result<ChipInfo, BromError> {
    with_temporary_handshaken_brom_handle(get_chip_info)
}

fn detect_auth_type_blocking() -> Result<AuthType, BromError> {
    with_temporary_handshaken_brom_handle(detect_auth_type)
}

fn bypass_sla_blocking() -> Result<(), BromError> {
    ensure_active_session()?;
    let result = with_active_session(ensure_sla_bypass);

    if result.is_err() {
        let _ = clear_active_session();
    }

    result
}

fn upload_da_blocking(da_path: String) -> Result<DaUploadResult, BromError> {
    let trimmed_path = da_path.trim();
    if trimmed_path.is_empty() {
        return Err(BromError::DaUploadFailed("DA path cannot be empty".to_string()));
    }

    let da_bytes = fs::read(trimmed_path).map_err(|error| {
        BromError::DaUploadFailed(format!("Failed to read DA file '{trimmed_path}': {error}"))
    })?;

    ensure_active_session()?;
    let result = with_active_session(|session| {
        ensure_sla_bypass(session)?;
        let upload_result = upload_da(&session.handle, &da_bytes)?;
        session.da_uploaded = true;
        Ok(upload_result)
    });

    if result.is_err() {
        let _ = clear_active_session();
    }

    result
}

fn jump_to_da_blocking() -> Result<DaJumpInfo, BromError> {
    let session = take_active_session()?.ok_or_else(|| {
        BromError::DaUploadFailed("No active BROM session. Upload a DA before jumping".to_string())
    })?;

    if !session.da_uploaded {
        if let Err(error) = release_claimed_interface(&session.handle, session.layout) {
            debug_log(format!("session cleanup warning: {error}"));
        }

        return Err(BromError::DaUploadFailed(
            "No DA uploaded in the active BROM session".to_string(),
        ));
    }

    let jump_result = jump_to_da(&session.handle);
    let release_result = release_claimed_interface(&session.handle, session.layout);

    match (jump_result, release_result) {
        (Err(error), _) => Err(error),
        (Ok(_), Err(error)) => Err(error),
        (Ok(da_jump_info), Ok(())) => Ok(da_jump_info),
    }
}

// ── DA-level command framework ──────────────────────────────────────────────
// [INFERRED] After jump_to_da() the device runs the DA agent on the same USB
// bulk endpoints. Commands use a 2-byte BE opcode + params + 2-byte status
// response pattern, per mtkclient DA protocol traces.

fn encode_utf16le_prefixed(text: &str) -> Vec<u8> {
    let utf16: Vec<u16> = text.encode_utf16().collect();
    let len = (utf16.len() as u16).to_be_bytes();
    let mut buf = Vec::with_capacity(2 + utf16.len() * 2);
    buf.extend_from_slice(&len);
    for code_unit in &utf16 {
        buf.extend_from_slice(&code_unit.to_le_bytes());
    }
    buf
}

fn decode_utf16le(buf: &[u8]) -> String {
    let code_units: Vec<u16> = buf
        .chunks_exact(2)
        .map(|chunk| u16::from_le_bytes([chunk[0], chunk[1]]))
        .collect();
    String::from_utf16_lossy(&code_units)
}

pub fn da_send_cmd(
    handle: &DeviceHandle<GlobalContext>,
    cmd: u16,
    params: &[u8],
) -> Result<(), BromError> {
    debug_log(format!("da_send_cmd cmd={cmd:#06x} params_len={}", params.len()));
    write_exact(handle, &cmd.to_be_bytes())?;
    if !params.is_empty() {
        write_exact(handle, params)?;
    }
    let mut status_buf = [0u8; 2];
    read_exact(handle, &mut status_buf)?;
    let status = u16::from_be_bytes(status_buf);
    debug_log(format!("da_send_cmd response status={status:#06x}"));
    if status != DA_STATUS_OK {
        return Err(BromError::DaCommandFailed { cmd, status });
    }
    Ok(())
}

pub fn da_erase_frp(handle: &DeviceHandle<GlobalContext>) -> Result<(), BromError> {
    let mut params = encode_utf16le_prefixed("frp");
    params.push(0x00); // normal erase
    debug_log("da_erase_frp: erasing FRP partition");
    da_send_cmd(handle, DA_CMD_ERASE_PARTITION, &params)
}

pub fn da_format_userdata(handle: &DeviceHandle<GlobalContext>) -> Result<(), BromError> {
    let mut params = encode_utf16le_prefixed("userdata");
    params.push(0x00); // factory reset
    debug_log("da_format_userdata: formatting userdata partition");
    da_send_cmd(handle, DA_CMD_FORMAT_PARTITION, &params)
}

pub fn da_read_imei(handle: &DeviceHandle<GlobalContext>) -> Result<ImeiInfo, BromError> {
    debug_log("da_read_imei: reading device IMEIs");
    da_send_cmd(handle, DA_CMD_READ_IMEI, &[])?;

    let mut count_buf = [0u8; 4];
    read_exact(handle, &mut count_buf)?;
    let count = u32::from_be_bytes(count_buf) as usize;
    debug_log(format!("da_read_imei: device reports {count} IMEI(s)"));

    if count == 0 {
        return Err(BromError::InvalidImei("Device returned 0 IMEIs".to_string()));
    }

    let mut imeis: Vec<String> = Vec::with_capacity(count);
    for i in 0..count {
        let mut len_buf = [0u8; 2];
        read_exact(handle, &mut len_buf)?;
        let char_count = u16::from_be_bytes(len_buf) as usize;
        let byte_count = char_count * 2;
        let mut str_buf = vec![0u8; byte_count];
        read_exact(handle, &mut str_buf)?;
        let imei = decode_utf16le(&str_buf);
        debug_log(format!("da_read_imei[{i}] len={char_count} value={imei}"));
        imeis.push(imei);
    }

    Ok(ImeiInfo {
        imei1: imeis.remove(0),
        imei2: if imeis.is_empty() { None } else { Some(imeis.remove(0)) },
    })
}

fn validate_imei(imei: &str) -> Result<(), BromError> {
    if imei.len() != 15 || !imei.chars().all(|c| c.is_ascii_digit()) {
        return Err(BromError::InvalidImei(format!(
            "IMEI must be exactly 15 digits, got: '{imei}'"
        )));
    }
    Ok(())
}

pub fn da_write_imei(
    handle: &DeviceHandle<GlobalContext>,
    imei1: &str,
    imei2: Option<&str>,
) -> Result<(), BromError> {
    validate_imei(imei1)?;
    if let Some(imei2_val) = imei2 {
        validate_imei(imei2_val)?;
    }

    debug_log(format!("da_write_imei: imei1={imei1} imei2={imei2:?}"));

    let mut params = encode_utf16le_prefixed(imei1);
    match imei2 {
        Some(imei) => params.extend_from_slice(&encode_utf16le_prefixed(imei)),
        None => params.extend_from_slice(&0u16.to_be_bytes()),
    }

    da_send_cmd(handle, DA_CMD_WRITE_IMEI, &params)
}

pub fn da_reboot(handle: &DeviceHandle<GlobalContext>, mode: u8) -> Result<(), BromError> {
    debug_log(format!("da_reboot: mode={mode:#04x}"));
    da_send_cmd(handle, DA_CMD_REBOOT, &[mode])
}

// ── Day 4: Partition operations ─────────────────────────────────────────────

fn crc8(data: &[u8]) -> u8 {
    data.iter().fold(0u8, |acc, &b| acc ^ b)
}

pub fn da_list_partitions(handle: &DeviceHandle<GlobalContext>) -> Result<Vec<PartitionEntry>, BromError> {
    debug_log("da_list_partitions: querying device partition table");
    da_send_cmd(handle, DA_CMD_LIST_PARTITIONS, &[])?;

    let mut count_buf = [0u8; 4];
    read_exact(handle, &mut count_buf)?;
    let count = u32::from_be_bytes(count_buf) as usize;
    debug_log(format!("da_list_partitions: {count} partition(s)"));

    let mut partitions = Vec::with_capacity(count);
    for i in 0..count {
        let mut len_buf = [0u8; 2];
        read_exact(handle, &mut len_buf)?;
        let char_count = u16::from_be_bytes(len_buf) as usize;
        let mut name_buf = vec![0u8; char_count * 2];
        read_exact(handle, &mut name_buf)?;
        let name = decode_utf16le(&name_buf);

        let mut start_buf = [0u8; 8];
        read_exact(handle, &mut start_buf)?;
        let start = u64::from_be_bytes(start_buf);

        let mut size_buf = [0u8; 8];
        read_exact(handle, &mut size_buf)?;
        let size = u64::from_be_bytes(size_buf);

        let mut type_buf = [0u8; 1];
        read_exact(handle, &mut type_buf)?;

        debug_log(format!(
            "  partition[{i}] name={name} start={start:#x} size={size:#x} type={:#04x}",
            type_buf[0]
        ));

        partitions.push(PartitionEntry {
            name,
            start,
            size,
            partition_type: type_buf[0],
        });
    }

    Ok(partitions)
}

pub fn da_read_partition(
    handle: &DeviceHandle<GlobalContext>,
    name: &str,
    offset: u64,
    length: u64,
    out_path: &std::path::Path,
) -> Result<u64, BromError> {
    debug_log(format!(
        "da_read_partition: name={name} offset={offset:#x} length={length:#x}"
    ));

    let mut params = encode_utf16le_prefixed(name);
    params.extend_from_slice(&offset.to_be_bytes());
    params.extend_from_slice(&length.to_be_bytes());
    da_send_cmd(handle, DA_CMD_READ_PARTITION, &params)?;

    let mut file = fs::File::create(out_path).map_err(|e| {
        BromError::DaUploadFailed(format!("Cannot create output file '{}': {e}", out_path.display()))
    })?;

    use std::io::Write;
    let mut total_written: u64 = 0;

    loop {
        let mut len_buf = [0u8; 4];
        read_exact(handle, &mut len_buf)?;
        let chunk_len = u32::from_be_bytes(len_buf) as usize;

        if chunk_len == 0 {
            break;
        }

        let mut chunk = vec![0u8; chunk_len];
        read_exact(handle, &mut chunk)?;

        let mut crc_buf = [0u8; 1];
        read_exact(handle, &mut crc_buf)?;
        let expected_crc = crc8(&chunk);
        if crc_buf[0] != expected_crc {
            return Err(BromError::DaUploadFailed(format!(
                "CRC8 mismatch at offset {total_written:#x}: expected {expected_crc:#04x}, got {:#04x}",
                crc_buf[0]
            )));
        }

        file.write_all(&chunk).map_err(|e| {
            BromError::DaUploadFailed(format!("Write to '{}' failed: {e}", out_path.display()))
        })?;

        total_written += chunk_len as u64;
        debug_log(format!(
            "da_read_partition: chunk {chunk_len} bytes, total={total_written}"
        ));
    }

    debug_log(format!("da_read_partition: complete, {total_written} bytes written"));
    Ok(total_written)
}

pub fn da_write_partition(
    handle: &DeviceHandle<GlobalContext>,
    name: &str,
    offset: u64,
    data: &[u8],
) -> Result<(), BromError> {
    let total_size = data.len() as u64;
    debug_log(format!(
        "da_write_partition: name={name} offset={offset:#x} size={total_size}"
    ));

    let mut params = encode_utf16le_prefixed(name);
    params.extend_from_slice(&offset.to_be_bytes());
    params.extend_from_slice(&total_size.to_be_bytes());

    write_exact(handle, &DA_CMD_WRITE_PARTITION.to_be_bytes())?;
    write_exact(handle, &params)?;

    for (idx, chunk) in data.chunks(DA_CHUNK_SIZE).enumerate() {
        let chunk_len = (chunk.len() as u32).to_be_bytes();
        let chunk_crc = crc8(chunk);

        write_exact(handle, &chunk_len)?;
        write_exact(handle, chunk)?;
        write_exact(handle, &[chunk_crc])?;

        debug_log(format!(
            "da_write_partition: chunk[{idx}] {} bytes crc={chunk_crc:#04x}",
            chunk.len()
        ));
    }

    let mut status_buf = [0u8; 2];
    read_exact(handle, &mut status_buf)?;
    let status = u16::from_be_bytes(status_buf);
    if status != DA_STATUS_OK {
        return Err(BromError::DaCommandFailed {
            cmd: DA_CMD_WRITE_PARTITION,
            status,
        });
    }

    debug_log("da_write_partition: complete");
    Ok(())
}

pub fn da_dump_preloader(
    handle: &DeviceHandle<GlobalContext>,
    out_path: &std::path::Path,
) -> Result<u64, BromError> {
    debug_log("da_dump_preloader: reading full preloader partition");

    let partitions = da_list_partitions(handle)?;
    let preloader = partitions.iter().find(|p| p.name == "preloader").ok_or_else(|| {
        BromError::PartitionNotFound("preloader".to_string())
    })?;

    da_read_partition(handle, "preloader", 0, preloader.size, out_path)
}

pub fn da_erase_partition_generic(
    handle: &DeviceHandle<GlobalContext>,
    name: &str,
) -> Result<(), BromError> {
    debug_log(format!("da_erase_partition_generic: erasing '{name}'"));
    let params = encode_utf16le_prefixed(name);
    da_send_cmd(handle, DA_CMD_ERASE_PARTITION_DA, &params)
}

// ── DA blocking wrappers ────────────────────────────────────────────────────

fn erase_frp_blocking() -> Result<(), BromError> {
    with_temporary_brom_handle(da_erase_frp)
}

fn format_userdata_blocking() -> Result<(), BromError> {
    with_temporary_brom_handle(da_format_userdata)
}

fn read_imei_blocking() -> Result<ImeiInfo, BromError> {
    with_temporary_brom_handle(da_read_imei)
}

fn write_imei_blocking(imei1: String, imei2: Option<String>) -> Result<(), BromError> {
    with_temporary_brom_handle(|handle| da_write_imei(handle, &imei1, imei2.as_deref()))
}

fn reboot_blocking(mode: u8) -> Result<(), BromError> {
    with_temporary_brom_handle(|handle| da_reboot(handle, mode))
}

fn list_partitions_blocking() -> Result<Vec<PartitionEntry>, BromError> {
    with_temporary_brom_handle(da_list_partitions)
}

fn read_partition_blocking(
    name: String, offset: u64, length: u64, out_path: String,
) -> Result<u64, BromError> {
    let path = std::path::PathBuf::from(&out_path);
    with_temporary_brom_handle(|handle| da_read_partition(handle, &name, offset, length, &path))
}

fn write_partition_blocking(name: String, offset: u64, data_path: String) -> Result<(), BromError> {
    let data = fs::read(&data_path).map_err(|e| {
        BromError::DaUploadFailed(format!("Cannot read data file '{data_path}': {e}"))
    })?;
    with_temporary_brom_handle(|handle| da_write_partition(handle, &name, offset, &data))
}

fn dump_preloader_blocking(out_path: String) -> Result<u64, BromError> {
    let path = std::path::PathBuf::from(&out_path);
    with_temporary_brom_handle(|handle| da_dump_preloader(handle, &path))
}

fn erase_partition_blocking(name: String) -> Result<(), BromError> {
    with_temporary_brom_handle(|handle| da_erase_partition_generic(handle, &name))
}

#[tauri::command]
pub async fn mtk_detect_device() -> Result<MtkDevice, String> {
    tokio::task::spawn_blocking(detect_mtk_device_blocking)
        .await
        .map_err(|error| format!("MTK device detection task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_handshake_and_identify() -> Result<ChipInfo, String> {
    tokio::task::spawn_blocking(handshake_and_identify_blocking)
        .await
        .map_err(|error| format!("MTK handshake task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_detect_auth_type() -> Result<AuthType, String> {
    tokio::task::spawn_blocking(detect_auth_type_blocking)
        .await
        .map_err(|error| format!("MTK auth detection task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_bypass_sla() -> Result<(), String> {
    tokio::task::spawn_blocking(bypass_sla_blocking)
        .await
        .map_err(|error| format!("MTK SLA bypass task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_upload_da(da_path: String) -> Result<DaUploadResult, String> {
    tokio::task::spawn_blocking(move || upload_da_blocking(da_path))
        .await
        .map_err(|error| format!("MTK DA upload task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_jump_to_da() -> Result<DaJumpInfo, String> {
    tokio::task::spawn_blocking(jump_to_da_blocking)
        .await
        .map_err(|error| format!("MTK jump-to-DA task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_erase_frp() -> Result<(), String> {
    tokio::task::spawn_blocking(erase_frp_blocking)
        .await
        .map_err(|error| format!("MTK FRP erase task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_format_userdata() -> Result<(), String> {
    tokio::task::spawn_blocking(format_userdata_blocking)
        .await
        .map_err(|error| format!("MTK format userdata task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_read_imei() -> Result<ImeiInfo, String> {
    tokio::task::spawn_blocking(read_imei_blocking)
        .await
        .map_err(|error| format!("MTK IMEI read task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_write_imei(imei1: String, imei2: Option<String>) -> Result<(), String> {
    tokio::task::spawn_blocking(move || write_imei_blocking(imei1, imei2))
        .await
        .map_err(|error| format!("MTK IMEI write task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_reboot(mode: u8) -> Result<(), String> {
    tokio::task::spawn_blocking(move || reboot_blocking(mode))
        .await
        .map_err(|error| format!("MTK reboot task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_list_partitions() -> Result<Vec<PartitionEntry>, String> {
    tokio::task::spawn_blocking(list_partitions_blocking)
        .await
        .map_err(|error| format!("MTK list partitions task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_da_read_partition(
    name: String, offset: u64, length: u64, out_path: String,
) -> Result<u64, String> {
    tokio::task::spawn_blocking(move || read_partition_blocking(name, offset, length, out_path))
        .await
        .map_err(|error| format!("MTK read partition task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_da_write_partition(
    name: String, offset: u64, data_path: String,
) -> Result<(), String> {
    tokio::task::spawn_blocking(move || write_partition_blocking(name, offset, data_path))
        .await
        .map_err(|error| format!("MTK write partition task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_dump_preloader(out_path: String) -> Result<u64, String> {
    tokio::task::spawn_blocking(move || dump_preloader_blocking(out_path))
        .await
        .map_err(|error| format!("MTK dump preloader task failed: {error}"))?
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn mtk_da_erase_partition(name: String) -> Result<(), String> {
    tokio::task::spawn_blocking(move || erase_partition_blocking(name))
        .await
        .map_err(|error| format!("MTK erase partition task failed: {error}"))?
        .map_err(|error| error.to_string())
}
