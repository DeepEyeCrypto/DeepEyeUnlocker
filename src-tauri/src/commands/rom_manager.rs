use std::{
    collections::{BTreeSet, HashMap},
    fs::File,
    io::Read,
    path::{Path, PathBuf},
    sync::{Mutex, OnceLock},
};

use chrono::Utc;
use quick_xml::{events::Event, Reader};
use serde::Serialize;
use sha2::{Digest, Sha256};
use tauri::AppHandle;
use tauri_plugin_dialog::DialogExt;
use zip::ZipArchive;

use crate::commands::connected_devices::{get_connected_devices, ConnectedDevice};
use crate::commands::device_db::{
    DeviceDb, DeviceEntry as DbDeviceEntry, FlashProtocol, SocFamily,
};

const MAX_TEXT_ENTRY_SIZE: u64 = 512 * 1024;
const MAX_BUILD_HINT_LENGTH: usize = 160;

static ROM_QUEUE: OnceLock<Mutex<Vec<QueueItem>>> = OnceLock::new();

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum RomType {
    Fastboot,
    RecoveryZip,
    OtaPackage,
    SpFlashTool,
    OdinPackage,
    QualcommEdl,
    Unknown,
}

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum PlatformFamily {
    Qualcomm,
    MediaTek,
    Samsung,
    GenericAndroid,
    Unknown,
}

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum FlashMode {
    Adb,
    Fastboot,
    RecoverySideload,
    Edl,
    BromDownload,
    OdinDownload,
    Unknown,
}

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum CompatibilityState {
    Compatible,
    LikelyCompatible,
    Incompatible,
    Unknown,
}

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum FlashActionType {
    FlashPartition,
    ApplyOta,
    FlashPackage,
    ErasePartition,
    ProgramRaw,
    PatchRaw,
    BootloaderStep,
}

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum RiskLevel {
    Low,
    Medium,
    High,
    Critical,
}

#[derive(Debug, Serialize, Clone, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum QueueStatus {
    Pending,
    Validating,
    Ready,
    Blocked,
    Flashing,
    Completed,
    Failed,
}

const FLASH_MODE_CATALOG: &[FlashMode] = &[
    FlashMode::Adb,
    FlashMode::Fastboot,
    FlashMode::RecoverySideload,
    FlashMode::Edl,
    FlashMode::BromDownload,
    FlashMode::OdinDownload,
    FlashMode::Unknown,
];

const FLASH_ACTION_CATALOG: &[FlashActionType] = &[
    FlashActionType::FlashPartition,
    FlashActionType::ApplyOta,
    FlashActionType::FlashPackage,
    FlashActionType::ErasePartition,
    FlashActionType::ProgramRaw,
    FlashActionType::PatchRaw,
    FlashActionType::BootloaderStep,
];

const QUEUE_STATUS_CATALOG: &[QueueStatus] = &[
    QueueStatus::Pending,
    QueueStatus::Validating,
    QueueStatus::Ready,
    QueueStatus::Blocked,
    QueueStatus::Flashing,
    QueueStatus::Completed,
    QueueStatus::Failed,
];

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedDeviceSummary {
    pub id: String,
    pub model: String,
    pub serial: String,
    pub mode: String,
    pub source: String,
    pub bootloader_status: String,
    pub carrier: Option<String>,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ArchiveEntryInfo {
    pub path: String,
    pub is_dir: bool,
    pub compressed_size: u64,
    pub uncompressed_size: u64,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct RomIndicators {
    pub has_images_dir: bool,
    pub has_flash_all_script: bool,
    pub has_android_info: bool,
    pub has_payload_bin: bool,
    pub has_payload_properties: bool,
    pub has_care_map: bool,
    pub has_meta_inf: bool,
    pub has_scatter: bool,
    pub has_preloader: bool,
    pub has_rawprogram_xml: bool,
    pub has_patch_xml: bool,
    pub has_firehose: bool,
    pub has_odin_packages: bool,
    pub has_pit: bool,
    pub has_tar_md5: bool,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct PartitionCandidate {
    pub name: String,
    pub source: String,
    pub source_file: String,
    pub estimated_size: Option<u64>,
    pub action_type: FlashActionType,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct RomSummary {
    pub file_name: String,
    pub file_path: String,
    pub archive_entry_count: usize,
    pub total_compressed_size: u64,
    pub total_uncompressed_size: u64,
    pub rom_type: RomType,
    pub detected_brand: Option<String>,
    pub detected_platform: PlatformFamily,
    pub supported_flash_mode: FlashMode,
    pub top_level_folders: Vec<String>,
    pub product_hints: Vec<String>,
    pub codename_hints: Vec<String>,
    pub build_hints: Vec<String>,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct DeviceMatch {
    pub brand: String,
    pub model: String,
    pub codename: String,
    pub soc: String,
    pub soc_family: String,
    pub protocol: String,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct CompatibilityReport {
    pub state: CompatibilityState,
    pub score: u8,
    pub reasons: Vec<String>,
    pub connected_device: Option<ConnectedDeviceSummary>,
    pub matched_database_entry: Option<DeviceMatch>,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct FlashEntry {
    pub partition: String,
    pub source_file: String,
    pub action_type: FlashActionType,
    pub estimated_size: Option<u64>,
    pub checksum_available: bool,
    pub required_protocol: FlashMode,
    pub order: usize,
    pub enabled: bool,
    pub risk_level: RiskLevel,
    pub notes: Vec<String>,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct FlashPlan {
    pub rom_summary: RomSummary,
    pub detected_platform: PlatformFamily,
    pub detected_brand: Option<String>,
    pub supported_flash_mode: FlashMode,
    pub required_device_state: String,
    pub flash_entries: Vec<FlashEntry>,
    pub data_wipe_implied: bool,
    pub bootloader_unlock_required: bool,
    pub looks_dangerous_or_incomplete: bool,
    pub execution_supported: bool,
    pub warnings: Vec<String>,
    pub blockers: Vec<String>,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct PackageValidation {
    pub valid: bool,
    pub status: QueueStatus,
    pub warnings: Vec<String>,
    pub blockers: Vec<String>,
    pub execution_supported: bool,
    pub dangerous: bool,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct RomAnalysis {
    pub package_id: String,
    pub generated_at: String,
    pub summary: RomSummary,
    pub archive_sha256: String,
    pub archive_entries: Vec<ArchiveEntryInfo>,
    pub payload_files: Vec<String>,
    pub manifest_files: Vec<String>,
    pub partition_candidates: Vec<PartitionCandidate>,
    pub indicators: RomIndicators,
    pub compatibility: CompatibilityReport,
    pub validation: PackageValidation,
    pub flash_plan: FlashPlan,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct QueueItem {
    pub id: String,
    pub file_path: String,
    pub file_name: String,
    pub rom_type: RomType,
    pub flash_mode: FlashMode,
    pub detected_brand: Option<String>,
    pub detected_platform: PlatformFamily,
    pub status: QueueStatus,
    pub execution_supported: bool,
    pub selected_partitions: Vec<String>,
    pub warnings: Vec<String>,
    pub blockers: Vec<String>,
    pub added_at: String,
    pub analysis: RomAnalysis,
}

#[derive(Debug, Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct RomDetection {
    pub rom_type: RomType,
    pub detected_brand: Option<String>,
    pub detected_platform: PlatformFamily,
    pub supported_flash_mode: FlashMode,
    pub warnings: Vec<String>,
    pub blockers: Vec<String>,
}

#[derive(Debug, thiserror::Error)]
enum RomManagerError {
    #[error("ROM package path cannot be empty")]
    EmptyPath,
    #[error("ROM package not found: {0}")]
    FileNotFound(String),
    #[error("Expected a .zip firmware package: {0}")]
    InvalidExtension(String),
    #[error("I/O error: {0}")]
    Io(String),
    #[error("ZIP archive error: {0}")]
    Archive(String),
    #[error("Dialog error: {0}")]
    Dialog(String),
    #[error("XML parse error in {source_file}: {message}")]
    Xml { source_file: String, message: String },
    #[error("Queue index out of range")]
    QueueIndexOutOfRange,
    #[error("Queue item not found: {0}")]
    QueueItemNotFound(String),
}

#[derive(Debug, Clone)]
struct CollectedEntry {
    path: String,
    lower_path: String,
    file_name: String,
    lower_file_name: String,
    is_dir: bool,
    uncompressed_size: u64,
}

#[derive(Debug, Default)]
struct InspectionContext {
    archive_entries: Vec<ArchiveEntryInfo>,
    collected_entries: Vec<CollectedEntry>,
    text_files: HashMap<String, String>,
    payload_files: BTreeSet<String>,
    manifest_files: BTreeSet<String>,
    top_level_folders: BTreeSet<String>,
    checksum_files: BTreeSet<String>,
    unsafe_paths: Vec<String>,
    total_compressed_size: u64,
    total_uncompressed_size: u64,
}

#[derive(Debug, Default)]
struct HintAccumulator {
    product: BTreeSet<String>,
    codename: BTreeSet<String>,
    build: BTreeSet<String>,
}

#[derive(Debug, Clone)]
struct FlashCandidateRecord {
    partition: String,
    source_file: String,
    estimated_size: Option<u64>,
    action_type: FlashActionType,
    notes: Vec<String>,
}

fn queue_state() -> &'static Mutex<Vec<QueueItem>> {
    let _ = FLASH_MODE_CATALOG.len() + FLASH_ACTION_CATALOG.len() + QUEUE_STATUS_CATALOG.len();
    ROM_QUEUE.get_or_init(|| Mutex::new(Vec::new()))
}

fn queue_lock() -> Result<std::sync::MutexGuard<'static, Vec<QueueItem>>, RomManagerError> {
    queue_state()
        .lock()
        .map_err(|error| RomManagerError::Io(format!("queue lock poisoned: {error}")))
}

fn now_rfc3339() -> String {
    Utc::now().to_rfc3339()
}

fn canonicalize_or_original(path: &Path) -> PathBuf {
    std::fs::canonicalize(path).unwrap_or_else(|_| path.to_path_buf())
}

fn path_to_string(path: &Path) -> String {
    path.to_string_lossy().to_string()
}

fn validate_zip_path(file_path: &str) -> Result<PathBuf, RomManagerError> {
    let trimmed = file_path.trim();
    if trimmed.is_empty() {
        return Err(RomManagerError::EmptyPath);
    }

    let path = PathBuf::from(trimmed);
    if !path.is_file() {
        return Err(RomManagerError::FileNotFound(trimmed.to_string()));
    }

    let extension = path
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or_default();
    if !extension.eq_ignore_ascii_case("zip") {
        return Err(RomManagerError::InvalidExtension(trimmed.to_string()));
    }

    Ok(canonicalize_or_original(&path))
}

fn file_name_string(path: &Path) -> String {
    path.file_name()
        .and_then(|value| value.to_str())
        .map(ToOwned::to_owned)
        .unwrap_or_else(|| path_to_string(path))
}

fn normalize_archive_path(raw_path: &str) -> String {
    raw_path
        .replace('\\', "/")
        .trim()
        .trim_start_matches("./")
        .trim_start_matches('/')
        .to_string()
}

fn is_unsafe_archive_path(path: &str) -> bool {
    path.starts_with('/')
        || path.contains("../")
        || path.split('/').any(|segment| segment == "..")
        || path.contains(':')
}

fn archive_file_name(path: &str) -> String {
    path.rsplit('/').next().unwrap_or(path).to_string()
}

fn sorted_strings(set: &BTreeSet<String>) -> Vec<String> {
    set.iter().cloned().collect()
}

fn build_package_id(file_path: &Path) -> String {
    let mut hasher = Sha256::new();
    hasher.update(path_to_string(file_path).as_bytes());
    hasher.update(now_rfc3339().as_bytes());
    hex::encode(hasher.finalize())
        .chars()
        .take(16)
        .collect::<String>()
}

fn sha256_file(path: &Path) -> Result<String, RomManagerError> {
    let mut file = File::open(path).map_err(|error| RomManagerError::Io(error.to_string()))?;
    let mut hasher = Sha256::new();
    let mut buffer = [0u8; 64 * 1024];

    loop {
        let read = file
            .read(&mut buffer)
            .map_err(|error| RomManagerError::Io(error.to_string()))?;
        if read == 0 {
            break;
        }
        hasher.update(&buffer[..read]);
    }

    Ok(hex::encode(hasher.finalize()))
}

fn is_manifest_file(lower_path: &str) -> bool {
    lower_path.ends_with("android-info.txt")
        || lower_path.ends_with("build.prop")
        || lower_path.ends_with("payload_properties.txt")
        || lower_path.ends_with("care_map.pb")
        || lower_path.ends_with("updater-script")
        || lower_path.ends_with("update-binary")
        || lower_path.ends_with("metadata")
        || lower_path.ends_with("flash_all.bat")
        || lower_path.ends_with("flash_all.sh")
        || lower_path.ends_with("flash_all_lock.bat")
        || lower_path.ends_with("flash_all_except_storage.bat")
        || lower_path.ends_with("flash_all_except_data_storage.bat")
        || is_scatter_file(lower_path)
        || is_rawprogram_file(lower_path)
        || is_patch_file(lower_path)
        || lower_path.ends_with(".pit")
        || lower_path.ends_with(".xml")
        || lower_path.ends_with(".txt")
        || lower_path.ends_with(".cfg")
        || lower_path.ends_with(".md5")
}

fn is_payload_file(lower_path: &str) -> bool {
    lower_path.ends_with("payload.bin")
        || lower_path.ends_with(".img")
        || lower_path.contains(".img_sparsechunk.")
        || lower_path.ends_with(".bin")
        || lower_path.ends_with(".mbn")
        || lower_path.ends_with(".elf")
        || lower_path.ends_with(".tar")
        || lower_path.ends_with(".tar.md5")
        || lower_path.ends_with(".pit")
}

fn is_checksum_file(lower_path: &str) -> bool {
    lower_path.ends_with(".md5")
        || lower_path.ends_with(".sha1")
        || lower_path.ends_with(".sha256")
        || lower_path.contains("checksum")
        || lower_path.contains("md5sum")
        || lower_path.contains("sha256sum")
}

fn looks_like_text_manifest(lower_path: &str) -> bool {
    is_manifest_file(lower_path)
        || lower_path.ends_with(".prop")
        || lower_path.ends_with(".bat")
        || lower_path.ends_with(".sh")
}

fn is_scatter_file(lower_path: &str) -> bool {
    lower_path.ends_with("_android_scatter.txt")
        || (lower_path.ends_with(".txt") && lower_path.contains("scatter"))
        || archive_file_name(lower_path).starts_with("mt") && lower_path.ends_with(".txt")
}

fn is_rawprogram_file(lower_path: &str) -> bool {
    archive_file_name(lower_path).starts_with("rawprogram") && lower_path.ends_with(".xml")
}

fn is_patch_file(lower_path: &str) -> bool {
    archive_file_name(lower_path).starts_with("patch") && lower_path.ends_with(".xml")
}

fn detect_odin_package_name(lower_name: &str) -> bool {
    lower_name.starts_with("bl_")
        || lower_name.starts_with("ap_")
        || lower_name.starts_with("cp_")
        || lower_name.starts_with("csc_")
        || lower_name.starts_with("home_csc_")
}

fn inspect_archive(file_path: &Path) -> Result<InspectionContext, RomManagerError> {
    let file = File::open(file_path).map_err(|error| RomManagerError::Io(error.to_string()))?;
    let mut archive = ZipArchive::new(file).map_err(|error| RomManagerError::Archive(error.to_string()))?;
    let mut context = InspectionContext::default();

    for index in 0..archive.len() {
        let mut entry = archive
            .by_index(index)
            .map_err(|error| RomManagerError::Archive(error.to_string()))?;
        let normalized_path = normalize_archive_path(entry.name());
        if normalized_path.is_empty() {
            continue;
        }

        if is_unsafe_archive_path(&normalized_path) {
            context.unsafe_paths.push(normalized_path.clone());
        }

        let lower_path = normalized_path.to_ascii_lowercase();
        let file_name = archive_file_name(&normalized_path);
        let lower_file_name = file_name.to_ascii_lowercase();
        let is_dir = entry.is_dir();
        let compressed_size = entry.compressed_size();
        let uncompressed_size = entry.size();

        if normalized_path.contains('/') {
            if let Some(folder) = normalized_path.split('/').next() {
                if !folder.is_empty() {
                    context.top_level_folders.insert(folder.to_string());
                }
            }
        }

        if is_payload_file(&lower_path) {
            context.payload_files.insert(normalized_path.clone());
        }
        if is_manifest_file(&lower_path) {
            context.manifest_files.insert(normalized_path.clone());
        }
        if is_checksum_file(&lower_path) {
            context.checksum_files.insert(normalized_path.clone());
        }

        if !is_dir && looks_like_text_manifest(&lower_path) && uncompressed_size <= MAX_TEXT_ENTRY_SIZE {
            let mut bytes = Vec::with_capacity(uncompressed_size as usize);
            entry
                .read_to_end(&mut bytes)
                .map_err(|error| RomManagerError::Io(error.to_string()))?;
            context
                .text_files
                .insert(lower_path.clone(), String::from_utf8_lossy(&bytes).to_string());
        }

        context.total_compressed_size = context.total_compressed_size.saturating_add(compressed_size);
        context.total_uncompressed_size = context.total_uncompressed_size.saturating_add(uncompressed_size);

        context.archive_entries.push(ArchiveEntryInfo {
            path: normalized_path.clone(),
            is_dir,
            compressed_size,
            uncompressed_size,
        });
        context.collected_entries.push(CollectedEntry {
            path: normalized_path,
            lower_path,
            file_name,
            lower_file_name,
            is_dir,
            uncompressed_size,
        });
    }

    Ok(context)
}

fn build_indicators(context: &InspectionContext) -> RomIndicators {
    let has_images_dir = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_path.starts_with("images/") || entry.lower_path.contains("/images/"));
    let has_flash_all_script = context.collected_entries.iter().any(|entry| {
        matches!(
            entry.lower_file_name.as_str(),
            "flash_all.bat"
                | "flash_all.sh"
                | "flash_all_lock.bat"
                | "flash_all_except_storage.bat"
                | "flash_all_except_data_storage.bat"
        )
    });
    let has_android_info = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_file_name == "android-info.txt");
    let has_payload_bin = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_file_name == "payload.bin");
    let has_payload_properties = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_file_name == "payload_properties.txt");
    let has_care_map = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_file_name == "care_map.pb");
    let has_meta_inf = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_path.starts_with("meta-inf/") || entry.lower_path.contains("/meta-inf/"));
    let has_scatter = context
        .collected_entries
        .iter()
        .any(|entry| is_scatter_file(&entry.lower_path));
    let has_preloader = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_file_name.starts_with("preloader") || entry.lower_file_name.contains("preloader"));
    let has_rawprogram_xml = context
        .collected_entries
        .iter()
        .any(|entry| is_rawprogram_file(&entry.lower_path));
    let has_patch_xml = context
        .collected_entries
        .iter()
        .any(|entry| is_patch_file(&entry.lower_path));
    let has_firehose = context.collected_entries.iter().any(|entry| {
        entry.lower_file_name.contains("firehose")
            && (entry.lower_file_name.ends_with(".elf") || entry.lower_file_name.ends_with(".mbn"))
    });
    let has_tar_md5 = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_file_name.ends_with(".tar.md5"));
    let has_odin_packages = context
        .collected_entries
        .iter()
        .any(|entry| detect_odin_package_name(&entry.lower_file_name));
    let has_pit = context
        .collected_entries
        .iter()
        .any(|entry| entry.lower_file_name.ends_with(".pit"));

    RomIndicators {
        has_images_dir,
        has_flash_all_script,
        has_android_info,
        has_payload_bin,
        has_payload_properties,
        has_care_map,
        has_meta_inf,
        has_scatter,
        has_preloader,
        has_rawprogram_xml,
        has_patch_xml,
        has_firehose,
        has_odin_packages,
        has_pit,
        has_tar_md5,
    }
}

fn detect_rom_type(indicators: &RomIndicators) -> RomType {
    if indicators.has_rawprogram_xml || indicators.has_patch_xml || indicators.has_firehose {
        RomType::QualcommEdl
    } else if indicators.has_scatter || indicators.has_preloader {
        RomType::SpFlashTool
    } else if indicators.has_odin_packages || indicators.has_tar_md5 || indicators.has_pit {
        RomType::OdinPackage
    } else if indicators.has_meta_inf && indicators.has_payload_bin {
        RomType::OtaPackage
    } else if indicators.has_meta_inf {
        RomType::RecoveryZip
    } else if indicators.has_images_dir || indicators.has_flash_all_script || indicators.has_android_info {
        RomType::Fastboot
    } else {
        RomType::Unknown
    }
}

fn flash_mode_for_rom_type(rom_type: &RomType) -> FlashMode {
    match rom_type {
        RomType::Fastboot => FlashMode::Fastboot,
        RomType::RecoveryZip | RomType::OtaPackage => FlashMode::RecoverySideload,
        RomType::SpFlashTool => FlashMode::BromDownload,
        RomType::OdinPackage => FlashMode::OdinDownload,
        RomType::QualcommEdl => FlashMode::Edl,
        RomType::Unknown => FlashMode::Unknown,
    }
}

fn required_device_state(mode: &FlashMode) -> String {
    match mode {
        FlashMode::Adb => "Device online in Android with USB debugging authorized".to_string(),
        FlashMode::Fastboot => {
            "Device in fastboot / bootloader mode with an unlocked bootloader".to_string()
        }
        FlashMode::RecoverySideload => {
            "Device booted into recovery with ADB sideload enabled".to_string()
        }
        FlashMode::Edl => {
            "Device in Qualcomm 9008 / EDL mode with a matching firehose programmer".to_string()
        }
        FlashMode::BromDownload => {
            "Device in MediaTek BROM / Download Mode with a matching scatter or DA payload"
                .to_string()
        }
        FlashMode::OdinDownload => {
            "Samsung device in Download Mode with an Odin-compatible flashing path".to_string()
        }
        FlashMode::Unknown => "Unsupported or unknown device state".to_string(),
    }
}

fn execution_supported(mode: &FlashMode) -> bool {
    matches!(mode, FlashMode::Fastboot | FlashMode::RecoverySideload)
}

fn unsupported_execution_blocker(mode: &FlashMode) -> Option<String> {
    match mode {
        FlashMode::Edl => Some(
            "Inspection completed, but Qualcomm EDL queue execution is not wired into the desktop ROM Manager yet."
                .to_string(),
        ),
        FlashMode::BromDownload => Some(
            "Inspection completed, but MediaTek scatter/BROM queue execution is not wired into the desktop ROM Manager yet."
                .to_string(),
        ),
        FlashMode::OdinDownload => Some(
            "Inspection completed, but Samsung Odin queue execution is not wired into the desktop ROM Manager yet."
                .to_string(),
        ),
        FlashMode::Unknown | FlashMode::Adb => Some(
            "ROM type could not be mapped to a supported flashing pipeline.".to_string(),
        ),
        FlashMode::Fastboot | FlashMode::RecoverySideload => None,
    }
}

fn normalize_match_token(value: &str) -> String {
    value
        .chars()
        .filter(|character| character.is_ascii_alphanumeric())
        .flat_map(char::to_lowercase)
        .collect::<String>()
}

fn hint_token_is_reserved(token: &str) -> bool {
    matches!(
        token,
        "images"
            | "image"
            | "firmware"
            | "update"
            | "payload"
            | "package"
            | "meta"
            | "inf"
            | "android"
            | "stable"
            | "global"
            | "recovery"
            | "fastboot"
            | "rom"
            | "signed"
            | "unsigned"
            | "release"
            | "target"
            | "files"
            | "flash"
            | "all"
            | "backup"
            | "user"
            | "userdebug"
            | "eng"
            | "bin"
            | "meta-inf"
    )
}

fn hint_token_is_usable(token: &str) -> bool {
    let normalized = token.trim().to_ascii_lowercase();
    !normalized.is_empty()
        && normalized.len() >= 3
        && normalized.len() <= 32
        && normalized.chars().any(|character| character.is_ascii_alphabetic())
        && !normalized.chars().all(|character| character.is_ascii_digit())
        && !hint_token_is_reserved(&normalized)
}

fn push_hint(set: &mut BTreeSet<String>, value: &str) {
    let trimmed = value.trim();
    if trimmed.is_empty() {
        return;
    }

    let bounded = trimmed.chars().take(MAX_BUILD_HINT_LENGTH).collect::<String>();
    if bounded.is_empty() {
        return;
    }

    set.insert(bounded);
}

fn add_name_tokens(input: &str, target: &mut BTreeSet<String>) {
    for token in input
        .split(|character: char| !character.is_ascii_alphanumeric() && character != '_' && character != '-')
    {
        if hint_token_is_usable(token) {
            push_hint(target, token);
        }
    }
}

fn add_split_values(values: &str, target: &mut BTreeSet<String>) {
    for item in values.split(|character: char| {
        matches!(character, '|' | ',' | ';' | ' ' | '\t' | '(' | ')' | '[' | ']')
    }) {
        if hint_token_is_usable(item) {
            push_hint(target, item);
        }
    }
}

fn parse_android_info(text: &str, hints: &mut HintAccumulator) {
    for line in text.lines() {
        let trimmed = line.trim();
        let lower = trimmed.to_ascii_lowercase();

        if let Some(index) = lower.find("product=") {
            add_split_values(&trimmed[index + 8..], &mut hints.product);
        }
        if let Some(index) = lower.find("board=") {
            add_split_values(&trimmed[index + 6..], &mut hints.codename);
        }
        if lower.contains("version") || lower.contains("fingerprint") || lower.contains("build") {
            push_hint(&mut hints.build, trimmed);
        }
    }
}

fn parse_metadata_text(text: &str, hints: &mut HintAccumulator) {
    for line in text.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() {
            continue;
        }
        let (key, value) = trimmed
            .split_once('=')
            .or_else(|| trimmed.split_once(':'))
            .map(|(left, right)| (left.trim().to_ascii_lowercase(), right.trim().to_string()))
            .unwrap_or_else(|| (String::new(), String::new()));

        match key.as_str() {
            "pre-device" | "post-device" | "serialno" => add_split_values(&value, &mut hints.codename),
            "post-build" | "pre-build" | "post-build-incremental" | "ota-type" => {
                push_hint(&mut hints.build, &format!("{key}={value}"));
            }
            _ => {
                if trimmed.len() <= MAX_BUILD_HINT_LENGTH {
                    push_hint(&mut hints.build, trimmed);
                }
            }
        }
    }
}

fn parse_build_prop(text: &str, hints: &mut HintAccumulator) {
    for line in text.lines() {
        let trimmed = line.trim();
        let Some((key, value)) = trimmed.split_once('=') else {
            continue;
        };

        match key.trim() {
            "ro.product.device"
            | "ro.product.vendor.device"
            | "ro.product.system.device"
            | "ro.product.odm.device"
            | "ro.build.product" => add_split_values(value, &mut hints.codename),
            "ro.product.model" | "ro.product.name" => add_split_values(value, &mut hints.product),
            "ro.build.fingerprint"
            | "ro.build.description"
            | "ro.build.version.release"
            | "ro.build.version.security_patch" => {
                push_hint(&mut hints.build, &format!("{}={}", key.trim(), value.trim()));
            }
            _ => {}
        }
    }
}

fn parse_updater_script(text: &str, hints: &mut HintAccumulator) {
    for marker in ["ro.product.device", "ro.build.product", "ro.product.model"] {
        let mut search_index = 0usize;
        while let Some(found) = text[search_index..].find(marker) {
            let offset = search_index + found;
            let rest = &text[offset..];
            if let Some(first_quote) = rest.find('"') {
                let remainder = &rest[first_quote + 1..];
                if let Some(second_quote) = remainder.find('"') {
                    let value = &remainder[..second_quote];
                    if marker == "ro.product.model" {
                        add_split_values(value, &mut hints.product);
                    } else {
                        add_split_values(value, &mut hints.codename);
                    }
                    search_index = offset + first_quote + second_quote + 2;
                    continue;
                }
            }
            break;
        }
    }
}

fn collect_hints(context: &InspectionContext, package_path: &Path) -> HintAccumulator {
    let mut hints = HintAccumulator::default();

    for (path, text) in &context.text_files {
        if path.ends_with("android-info.txt") {
            parse_android_info(text, &mut hints);
        } else if path.ends_with("/metadata") || archive_file_name(path) == "metadata" {
            parse_metadata_text(text, &mut hints);
        } else if path.ends_with("build.prop") {
            parse_build_prop(text, &mut hints);
        } else if path.ends_with("updater-script") {
            parse_updater_script(text, &mut hints);
        }
    }

    if let Some(stem) = package_path.file_stem().and_then(|value| value.to_str()) {
        add_name_tokens(stem, &mut hints.codename);
    }
    for folder in &context.top_level_folders {
        add_name_tokens(folder, &mut hints.codename);
    }

    hints
}

fn flash_protocol_name(protocol: &FlashProtocol) -> String {
    match protocol {
        FlashProtocol::Edl => "Edl",
        FlashProtocol::MtkBrom => "MtkBrom",
        FlashProtocol::SamsungOdin => "SamsungOdin",
        FlashProtocol::Adb => "Adb",
        FlashProtocol::Fastboot => "Fastboot",
        FlashProtocol::Unknown => "Unknown",
    }
    .to_string()
}

fn soc_family_name(value: &SocFamily) -> String {
    match value {
        SocFamily::Qualcomm => "Qualcomm",
        SocFamily::MediaTek => "MediaTek",
        SocFamily::Samsung => "Samsung",
        SocFamily::Unisoc => "Unisoc",
        SocFamily::Kirin => "Kirin",
        SocFamily::Unknown => "Unknown",
    }
    .to_string()
}

fn platform_from_soc_family(value: &SocFamily) -> PlatformFamily {
    match value {
        SocFamily::Qualcomm => PlatformFamily::Qualcomm,
        SocFamily::MediaTek => PlatformFamily::MediaTek,
        SocFamily::Samsung => PlatformFamily::Samsung,
        _ => PlatformFamily::GenericAndroid,
    }
}

fn device_match_from_entry(entry: &DbDeviceEntry) -> DeviceMatch {
    DeviceMatch {
        brand: entry.brand.clone(),
        model: entry.model.clone(),
        codename: entry.codename.clone(),
        soc: entry.soc.clone(),
        soc_family: soc_family_name(&entry.soc_family),
        protocol: flash_protocol_name(&entry.protocol),
    }
}

fn detect_brand(
    rom_type: &RomType,
    context: &InspectionContext,
    hints: &HintAccumulator,
    matched_entry: Option<&DbDeviceEntry>,
) -> Option<String> {
    if let Some(entry) = matched_entry {
        return Some(entry.brand.clone());
    }

    let mut tokens = BTreeSet::new();
    for token in &hints.product {
        tokens.insert(token.to_ascii_lowercase());
    }
    for token in &hints.codename {
        tokens.insert(token.to_ascii_lowercase());
    }
    for entry in &context.top_level_folders {
        tokens.insert(entry.to_ascii_lowercase());
    }

    let has_token = |needle: &str| tokens.iter().any(|token| token.contains(needle));
    if matches!(rom_type, RomType::OdinPackage) || has_token("samsung") {
        Some("Samsung".to_string())
    } else if has_token("poco") {
        Some("POCO".to_string())
    } else if has_token("redmi") {
        Some("Redmi".to_string())
    } else if has_token("xiaomi") || has_token("miui") {
        Some("Xiaomi".to_string())
    } else {
        None
    }
}

fn detect_platform(
    rom_type: &RomType,
    indicators: &RomIndicators,
    matched_entry: Option<&DbDeviceEntry>,
    detected_brand: Option<&str>,
) -> PlatformFamily {
    if let Some(entry) = matched_entry {
        return platform_from_soc_family(&entry.soc_family);
    }

    match rom_type {
        RomType::QualcommEdl => PlatformFamily::Qualcomm,
        RomType::SpFlashTool => PlatformFamily::MediaTek,
        RomType::OdinPackage => PlatformFamily::Samsung,
        RomType::Fastboot | RomType::RecoveryZip | RomType::OtaPackage => {
            if indicators.has_firehose {
                PlatformFamily::Qualcomm
            } else if indicators.has_scatter || indicators.has_preloader {
                PlatformFamily::MediaTek
            } else if matches!(detected_brand, Some("Samsung")) {
                PlatformFamily::Samsung
            } else {
                PlatformFamily::GenericAndroid
            }
        }
        RomType::Unknown => PlatformFamily::Unknown,
    }
}

fn find_best_database_entry(
    hints: &HintAccumulator,
    detected_brand: Option<&str>,
) -> Option<DbDeviceEntry> {
    let detected_brand = detected_brand.map(normalize_match_token);
    let products = hints
        .product
        .iter()
        .map(|item| normalize_match_token(item))
        .collect::<Vec<_>>();
    let codenames = hints
        .codename
        .iter()
        .map(|item| normalize_match_token(item))
        .collect::<Vec<_>>();

    let mut best: Option<(u16, DbDeviceEntry)> = None;
    for entry in DeviceDb::global().list_all() {
        let entry_brand = normalize_match_token(&entry.brand);
        let entry_model = normalize_match_token(&entry.model);
        let entry_codename = normalize_match_token(&entry.codename);
        let mut score = 0u16;

        if !entry_codename.is_empty() && codenames.iter().any(|hint| hint == &entry_codename) {
            score += 120;
        }
        if products.iter().any(|hint| !hint.is_empty() && (hint == &entry_model || entry_model.contains(hint))) {
            score += 80;
        }
        if let Some(brand) = &detected_brand {
            if brand == &entry_brand {
                score += 25;
            }
        }

        if score == 0 {
            continue;
        }

        match &best {
            Some((best_score, _)) if *best_score >= score => {}
            _ => best = Some((score, entry.clone())),
        }
    }

    best.map(|(_, entry)| entry)
}

fn strip_known_extensions(file_name: &str) -> String {
    let lower = file_name.to_ascii_lowercase();
    if let Some(index) = lower.find(".img_sparsechunk.") {
        return file_name[..index].to_string();
    }

    for suffix in [
        ".tar.md5",
        ".new.dat.br",
        ".new.dat",
        ".dat.br",
        ".payload.bin",
        ".img",
        ".bin",
        ".mbn",
        ".elf",
        ".pit",
        ".tar",
        ".txt",
    ] {
        if lower.ends_with(suffix) {
            let new_len = file_name.len().saturating_sub(suffix.len());
            return file_name[..new_len].to_string();
        }
    }

    Path::new(file_name)
        .file_stem()
        .and_then(|value| value.to_str())
        .map(ToOwned::to_owned)
        .unwrap_or_else(|| file_name.to_string())
}

fn partition_from_source_file(file_name: &str) -> String {
    let mut base = strip_known_extensions(file_name)
        .trim()
        .trim_start_matches("./")
        .trim_matches(|character: char| character == '_' || character == '-' || character == ' ')
        .to_string();

    if base.eq_ignore_ascii_case("super_empty") {
        base = "super".to_string();
    }
    if base.to_ascii_lowercase().starts_with("preloader") {
        return "preloader".to_string();
    }

    base.replace(' ', "_")
}

fn is_bootloader_binary_partition(partition: &str) -> bool {
    matches!(
        partition,
        "xbl"
            | "xbl_config"
            | "abl"
            | "tz"
            | "hyp"
            | "devcfg"
            | "cmnlib"
            | "cmnlib64"
            | "keymaster"
            | "qupfw"
            | "uefisecapp"
            | "bluetooth"
            | "dsp"
            | "modem"
            | "preloader"
            | "lk"
            | "logo"
            | "scp"
            | "md1img"
            | "spmfw"
            | "tee"
    )
}

fn is_fastboot_candidate(entry: &CollectedEntry) -> bool {
    if entry.is_dir {
        return false;
    }

    if entry.lower_file_name == "payload.bin" || entry.lower_path.starts_with("meta-inf/") {
        return false;
    }

    if entry.lower_file_name.ends_with(".img") || entry.lower_file_name.contains(".img_sparsechunk.") {
        return true;
    }

    if entry.lower_file_name.ends_with(".bin")
        || entry.lower_file_name.ends_with(".mbn")
        || entry.lower_file_name.ends_with(".elf")
    {
        let partition = partition_from_source_file(&entry.file_name).to_ascii_lowercase();
        return is_bootloader_binary_partition(&partition);
    }

    false
}

fn partition_priority(partition: &str) -> usize {
    match partition {
        "gpt" | "pgpt" | "pit" => 5,
        "preloader" | "bootloader" => 10,
        "xbl" | "xbl_config" => 20,
        "abl" => 30,
        "tz" => 40,
        "hyp" => 50,
        "devcfg" => 60,
        "cmnlib" | "cmnlib64" => 70,
        "keymaster" | "qupfw" | "uefisecapp" => 80,
        "modem" | "bluetooth" | "dsp" => 90,
        "vbmeta" | "vbmeta_system" | "vbmeta_vendor" => 100,
        "dtbo" => 110,
        "boot" | "boot_a" | "boot_b" => 120,
        "init_boot" => 130,
        "vendor_boot" => 140,
        "recovery" => 150,
        "super" => 160,
        "system" | "system_ext" => 170,
        "vendor" => 180,
        "product" => 190,
        "odm" | "cust" => 200,
        "metadata" => 210,
        "frp" => 220,
        "cache" => 230,
        "userdata" => 1000,
        _ => 500,
    }
}

fn risk_for_partition(partition: &str) -> RiskLevel {
    match partition {
        "gpt" | "pgpt" | "pit" | "preloader" | "bootloader" => RiskLevel::Critical,
        "xbl" | "xbl_config" | "abl" | "vbmeta" | "vbmeta_system" | "vbmeta_vendor" => {
            RiskLevel::High
        }
        "super" | "frp" | "modem" | "userdata" => RiskLevel::High,
        "boot" | "init_boot" | "vendor_boot" | "recovery" | "dtbo" | "system" | "vendor"
        | "product" => RiskLevel::Medium,
        _ => RiskLevel::Low,
    }
}

fn partition_notes(partition: &str) -> Vec<String> {
    match partition {
        "preloader" | "bootloader" => vec![
            "Low-level boot chain payload detected; mismatched images can hard-brick the target."
                .to_string(),
        ],
        "vbmeta" | "vbmeta_system" | "vbmeta_vendor" => vec![
            "Verified Boot metadata partition detected; flashing can change boot verification state."
                .to_string(),
        ],
        "super" => vec![
            "Dynamic super partition payload detected; ensure the target partition layout matches the package."
                .to_string(),
        ],
        "userdata" => vec!["Userdata payload detected; flashing implies a device wipe.".to_string()],
        "frp" => vec!["FRP partition detected; flashing affects reset-protection state.".to_string()],
        _ => Vec::new(),
    }
}

fn resolve_source_path(context: &InspectionContext, requested: &str) -> String {
    let normalized = normalize_archive_path(requested);
    let lower = normalized.to_ascii_lowercase();
    let file_name = archive_file_name(&normalized).to_ascii_lowercase();

    if let Some(entry) = context
        .collected_entries
        .iter()
        .find(|entry| entry.lower_path == lower || entry.lower_file_name == file_name)
    {
        return entry.path.clone();
    }

    if let Some(entry) = context
        .collected_entries
        .iter()
        .find(|entry| entry.lower_path.ends_with(&format!("/{}", file_name)))
    {
        return entry.path.clone();
    }

    normalized
}

fn lookup_entry_size(context: &InspectionContext, source_file: &str) -> Option<u64> {
    let normalized = source_file.to_ascii_lowercase();
    let file_name = archive_file_name(source_file).to_ascii_lowercase();
    context
        .collected_entries
        .iter()
        .find(|entry| entry.lower_path == normalized || entry.lower_file_name == file_name)
        .map(|entry| entry.uncompressed_size)
}

fn has_checksum_for_source(context: &InspectionContext, source_file: &str) -> bool {
    source_file.to_ascii_lowercase().ends_with(".tar.md5") || !context.checksum_files.is_empty()
}

fn candidate_to_flash_entry(
    context: &InspectionContext,
    mode: FlashMode,
    record: FlashCandidateRecord,
) -> FlashEntry {
    let risk_level = risk_for_partition(&record.partition);
    let mut notes = record.notes;
    notes.extend(partition_notes(&record.partition));

    FlashEntry {
        partition: record.partition,
        source_file: record.source_file.clone(),
        action_type: record.action_type,
        estimated_size: record.estimated_size,
        checksum_available: has_checksum_for_source(context, &record.source_file),
        required_protocol: mode,
        order: 0,
        enabled: true,
        risk_level,
        notes,
    }
}

fn sort_and_number_entries(entries: &mut [FlashEntry]) {
    entries.sort_by(|left, right| {
        partition_priority(&left.partition)
            .cmp(&partition_priority(&right.partition))
            .then_with(|| left.partition.cmp(&right.partition))
            .then_with(|| left.source_file.cmp(&right.source_file))
    });

    for (index, entry) in entries.iter_mut().enumerate() {
        entry.order = index + 1;
    }
}

fn build_fastboot_entries(context: &InspectionContext) -> Vec<FlashEntry> {
    let mut dedup = HashMap::<String, FlashEntry>::new();

    for entry in &context.collected_entries {
        if !is_fastboot_candidate(entry) {
            continue;
        }

        let partition = partition_from_source_file(&entry.file_name).to_ascii_lowercase();
        if partition.is_empty() {
            continue;
        }

        let mut notes = Vec::new();
        if entry.lower_path.contains("/images/") || entry.lower_path.starts_with("images/") {
            notes.push("Derived from images/ fastboot payload directory.".to_string());
        }

        let record = FlashCandidateRecord {
            partition: partition.clone(),
            source_file: entry.path.clone(),
            estimated_size: Some(entry.uncompressed_size),
            action_type: FlashActionType::FlashPartition,
            notes,
        };
        let flash_entry = candidate_to_flash_entry(context, FlashMode::Fastboot, record);

        let replace = dedup
            .get(&partition)
            .map(|existing| existing.estimated_size.unwrap_or(0) < entry.uncompressed_size)
            .unwrap_or(true);
        if replace {
            dedup.insert(partition, flash_entry);
        }
    }

    let mut entries = dedup.into_values().collect::<Vec<_>>();
    sort_and_number_entries(&mut entries);
    entries
}

fn build_recovery_entries(
    context: &InspectionContext,
    package_file_name: &str,
    rom_type: &RomType,
) -> Vec<FlashEntry> {
    let mut entries = Vec::new();

    if let Some(payload_entry) = context
        .collected_entries
        .iter()
        .find(|entry| entry.lower_file_name == "payload.bin")
    {
        entries.push(FlashEntry {
            partition: "ota-payload".to_string(),
            source_file: payload_entry.path.clone(),
            action_type: FlashActionType::ApplyOta,
            estimated_size: Some(payload_entry.uncompressed_size),
            checksum_available: has_checksum_for_source(context, &payload_entry.path),
            required_protocol: FlashMode::RecoverySideload,
            order: 1,
            enabled: true,
            risk_level: RiskLevel::Medium,
            notes: vec![
                "payload.bin detected; apply the package through recovery or an OTA-compatible update path."
                    .to_string(),
            ],
        });
        return entries;
    }

    let partition = if matches!(rom_type, RomType::OtaPackage) {
        "ota-package"
    } else {
        "update-package"
    };
    entries.push(FlashEntry {
        partition: partition.to_string(),
        source_file: package_file_name.to_string(),
        action_type: FlashActionType::FlashPackage,
        estimated_size: None,
        checksum_available: !context.checksum_files.is_empty(),
        required_protocol: FlashMode::RecoverySideload,
        order: 1,
        enabled: true,
        risk_level: RiskLevel::Medium,
        notes: vec![
            "Entire archive must be applied as a recovery / sideload package rather than flashing inner files directly."
                .to_string(),
        ],
    });
    entries
}

fn parse_scatter_file(
    text: &str,
    source_file: &str,
    context: &InspectionContext,
) -> Vec<FlashCandidateRecord> {
    let mut records = Vec::new();
    let mut current_partition: Option<String> = None;
    let mut current_file_name: Option<String> = None;
    let mut is_download = true;

    let flush = |records: &mut Vec<FlashCandidateRecord>,
                 current_partition: &mut Option<String>,
                 current_file_name: &mut Option<String>,
                 is_download: &mut bool| {
        let Some(partition_name) = current_partition.take() else {
            current_file_name.take();
            *is_download = true;
            return;
        };

        let file_name = current_file_name.take().unwrap_or_default();
        let normalized_file_name = file_name.trim();
        if !*is_download || normalized_file_name.is_empty() || normalized_file_name.eq_ignore_ascii_case("none") {
            *is_download = true;
            return;
        }

        let resolved_source = resolve_source_path(context, normalized_file_name);
        let estimated_size = lookup_entry_size(context, &resolved_source);
        let mut notes = vec![format!("Derived from scatter file {source_file}")];
        if estimated_size.is_none() {
            notes.push("Referenced payload is not present at the expected archive path.".to_string());
        }

        records.push(FlashCandidateRecord {
            partition: partition_name.to_ascii_lowercase(),
            source_file: resolved_source,
            estimated_size,
            action_type: FlashActionType::FlashPartition,
            notes,
        });
        *is_download = true;
    };

    for line in text.lines() {
        let trimmed = line.trim();
        if let Some(value) = trimmed.strip_prefix("partition_name:") {
            flush(
                &mut records,
                &mut current_partition,
                &mut current_file_name,
                &mut is_download,
            );
            current_partition = Some(value.trim().to_ascii_lowercase());
        } else if let Some(value) = trimmed.strip_prefix("file_name:") {
            current_file_name = Some(value.trim().to_string());
        } else if let Some(value) = trimmed.strip_prefix("is_download:") {
            let normalized = value.trim().to_ascii_lowercase();
            is_download = !matches!(normalized.as_str(), "false" | "0" | "no");
        }
    }

    flush(
        &mut records,
        &mut current_partition,
        &mut current_file_name,
        &mut is_download,
    );

    records
}

fn build_scatter_entries(context: &InspectionContext) -> Vec<FlashEntry> {
    let mut dedup = HashMap::<String, FlashEntry>::new();

    for (path, text) in &context.text_files {
        if !is_scatter_file(path) {
            continue;
        }

        for record in parse_scatter_file(text, path, context) {
            let partition = record.partition.clone();
            let flash_entry = candidate_to_flash_entry(context, FlashMode::BromDownload, record);
            let replace = dedup
                .get(&partition)
                .map(|existing| existing.estimated_size.unwrap_or(0) < flash_entry.estimated_size.unwrap_or(0))
                .unwrap_or(true);
            if replace {
                dedup.insert(partition, flash_entry);
            }
        }
    }

    let mut entries = dedup.into_values().collect::<Vec<_>>();
    sort_and_number_entries(&mut entries);
    entries
}

fn parse_rawprogram_xml(
    xml: &str,
    source_file: &str,
    context: &InspectionContext,
) -> Result<Vec<FlashCandidateRecord>, RomManagerError> {
    let mut reader = Reader::from_str(xml);
    reader.config_mut().trim_text(true);
    let mut buffer = Vec::new();
    let mut records = Vec::new();

    loop {
        match reader.read_event_into(&mut buffer) {
            Ok(Event::Start(event)) | Ok(Event::Empty(event))
                if event.name().as_ref().eq_ignore_ascii_case(b"program") =>
            {
                let mut label: Option<String> = None;
                let mut filename: Option<String> = None;
                let mut sectors: Option<u64> = None;
                let mut sector_size: Option<u64> = None;

                for attribute in event.attributes().with_checks(false).flatten() {
                    let key = String::from_utf8_lossy(attribute.key.as_ref()).to_ascii_lowercase();
                    let value = attribute
                        .decode_and_unescape_value(reader.decoder())
                        .map(|inner| inner.into_owned())
                        .unwrap_or_default();
                    match key.as_str() {
                        "label" | "partition_name" => label = Some(value),
                        "filename" => filename = Some(value),
                        "num_partition_sectors" => sectors = value.parse::<u64>().ok(),
                        "sector_size_in_bytes" => sector_size = value.parse::<u64>().ok(),
                        _ => {}
                    }
                }

                let Some(file_name) = filename.filter(|value| !value.trim().is_empty()) else {
                    buffer.clear();
                    continue;
                };

                let resolved_source = resolve_source_path(context, &file_name);
                let estimated_size = sectors
                    .zip(sector_size)
                    .map(|(count, size)| count.saturating_mul(size))
                    .or_else(|| lookup_entry_size(context, &resolved_source));
                let mut notes = vec![format!("Derived from rawprogram XML {source_file}")];
                if estimated_size.is_none() {
                    notes.push("Referenced payload is not present at the expected archive path.".to_string());
                }

                records.push(FlashCandidateRecord {
                    partition: label
                        .map(|value| value.to_ascii_lowercase())
                        .filter(|value| !value.trim().is_empty())
                        .unwrap_or_else(|| partition_from_source_file(&file_name).to_ascii_lowercase()),
                    source_file: resolved_source,
                    estimated_size,
                    action_type: FlashActionType::ProgramRaw,
                    notes,
                });
            }
            Ok(Event::Eof) => break,
            Ok(_) => {}
            Err(error) => {
                return Err(RomManagerError::Xml {
                    source_file: source_file.to_string(),
                    message: error.to_string(),
                });
            }
        }
        buffer.clear();
    }

    Ok(records)
}

fn build_edl_entries(context: &InspectionContext) -> Result<Vec<FlashEntry>, RomManagerError> {
    let mut dedup = HashMap::<String, FlashEntry>::new();

    for (path, text) in &context.text_files {
        if is_rawprogram_file(path) {
            for record in parse_rawprogram_xml(text, path, context)? {
                let partition = record.partition.clone();
                let flash_entry = candidate_to_flash_entry(context, FlashMode::Edl, record);
                let replace = dedup
                    .get(&partition)
                    .map(|existing| existing.estimated_size.unwrap_or(0) < flash_entry.estimated_size.unwrap_or(0))
                    .unwrap_or(true);
                if replace {
                    dedup.insert(partition, flash_entry);
                }
            }
        } else if is_patch_file(path) {
            let partition = partition_from_source_file(&archive_file_name(path)).to_ascii_lowercase();
            let record = FlashCandidateRecord {
                partition,
                source_file: path.clone(),
                estimated_size: lookup_entry_size(context, path),
                action_type: FlashActionType::PatchRaw,
                notes: vec![format!("Patch manifest detected in {path}")],
            };
            let flash_entry = candidate_to_flash_entry(context, FlashMode::Edl, record);
            dedup.insert(flash_entry.partition.clone(), flash_entry);
        }
    }

    let mut entries = dedup.into_values().collect::<Vec<_>>();
    sort_and_number_entries(&mut entries);
    Ok(entries)
}

fn build_odin_entries(context: &InspectionContext) -> Vec<FlashEntry> {
    let mut entries = Vec::new();

    for entry in &context.collected_entries {
        if entry.is_dir {
            continue;
        }

        let lower_name = entry.lower_file_name.as_str();
        let partition = if lower_name.starts_with("bl_") {
            Some("bl-package")
        } else if lower_name.starts_with("ap_") {
            Some("ap-package")
        } else if lower_name.starts_with("cp_") {
            Some("cp-package")
        } else if lower_name.starts_with("home_csc_") {
            Some("home-csc-package")
        } else if lower_name.starts_with("csc_") {
            Some("csc-package")
        } else if lower_name.ends_with(".pit") {
            Some("pit")
        } else {
            None
        };

        let Some(partition) = partition else {
            continue;
        };

        let action_type = if partition == "pit" {
            FlashActionType::BootloaderStep
        } else {
            FlashActionType::FlashPackage
        };
        let mut notes = vec!["Detected inside Samsung Odin archive structure.".to_string()];
        if partition == "csc-package" {
            notes.push("CSC package typically applies a factory-reset style wipe.".to_string());
        }
        if partition == "home-csc-package" {
            notes.push("HOME_CSC package is typically preferred when preserving userdata.".to_string());
        }

        entries.push(FlashEntry {
            partition: partition.to_string(),
            source_file: entry.path.clone(),
            action_type,
            estimated_size: Some(entry.uncompressed_size),
            checksum_available: has_checksum_for_source(context, &entry.path),
            required_protocol: FlashMode::OdinDownload,
            order: 0,
            enabled: true,
            risk_level: risk_for_partition(partition),
            notes,
        });
    }

    sort_and_number_entries(&mut entries);
    entries
}

fn flash_entries_for_rom(
    context: &InspectionContext,
    rom_type: &RomType,
    package_file_name: &str,
) -> Result<Vec<FlashEntry>, RomManagerError> {
    match rom_type {
        RomType::Fastboot => Ok(build_fastboot_entries(context)),
        RomType::RecoveryZip | RomType::OtaPackage => {
            Ok(build_recovery_entries(context, package_file_name, rom_type))
        }
        RomType::SpFlashTool => Ok(build_scatter_entries(context)),
        RomType::QualcommEdl => build_edl_entries(context),
        RomType::OdinPackage => Ok(build_odin_entries(context)),
        RomType::Unknown => Ok(Vec::new()),
    }
}

fn partition_candidates_from_entries(entries: &[FlashEntry]) -> Vec<PartitionCandidate> {
    entries
        .iter()
        .map(|entry| PartitionCandidate {
            name: entry.partition.clone(),
            source: match entry.required_protocol {
                FlashMode::Fastboot => "fastboot".to_string(),
                FlashMode::RecoverySideload => "recovery".to_string(),
                FlashMode::Edl => "rawprogram".to_string(),
                FlashMode::BromDownload => "scatter".to_string(),
                FlashMode::OdinDownload => "odin".to_string(),
                FlashMode::Adb => "adb".to_string(),
                FlashMode::Unknown => "unknown".to_string(),
            },
            source_file: entry.source_file.clone(),
            estimated_size: entry.estimated_size,
            action_type: entry.action_type.clone(),
        })
        .collect()
}

fn detect_data_wipe(context: &InspectionContext, entries: &[FlashEntry]) -> bool {
    if entries.iter().any(|entry| {
        matches!(entry.partition.as_str(), "userdata" | "cache") || entry.partition == "csc-package"
    }) {
        return true;
    }

    if context.collected_entries.iter().any(|entry| {
        matches!(
            entry.lower_file_name.as_str(),
            "flash_all.bat" | "flash_all.sh" | "flash_all_lock.bat"
        )
    }) {
        return true;
    }

    context.text_files.iter().any(|(path, text)| {
        path.ends_with("updater-script")
            && (text.contains("format(\"/data\"")
                || text.contains("format('/data'")
                || text.contains("delete_recursive(\"/data\"")
                || text.contains("delete_recursive('/data'"))
    })
}

fn build_summary(
    file_path: &Path,
    context: &InspectionContext,
    rom_type: RomType,
    detected_brand: Option<String>,
    detected_platform: PlatformFamily,
    flash_mode: FlashMode,
    hints: &HintAccumulator,
) -> RomSummary {
    RomSummary {
        file_name: file_name_string(file_path),
        file_path: path_to_string(file_path),
        archive_entry_count: context.archive_entries.len(),
        total_compressed_size: context.total_compressed_size,
        total_uncompressed_size: context.total_uncompressed_size,
        rom_type,
        detected_brand,
        detected_platform,
        supported_flash_mode: flash_mode,
        top_level_folders: sorted_strings(&context.top_level_folders),
        product_hints: sorted_strings(&hints.product),
        codename_hints: sorted_strings(&hints.codename),
        build_hints: sorted_strings(&hints.build),
    }
}

fn connected_device_summary(device: &ConnectedDevice) -> ConnectedDeviceSummary {
    ConnectedDeviceSummary {
        id: device.id.clone(),
        model: device.model.clone(),
        serial: device.serial.clone(),
        mode: device.mode.clone(),
        source: device.source.clone(),
        bootloader_status: device.bootloader_status.clone(),
        carrier: device.carrier.clone(),
    }
}

fn is_android_device(device: &ConnectedDevice) -> bool {
    !device.source.eq_ignore_ascii_case("apple") && !device.os.to_ascii_lowercase().contains("ios")
}

async fn build_compatibility_report(
    app: &AppHandle,
    hints: &HintAccumulator,
    detected_brand: Option<&str>,
    matched_entry: Option<&DbDeviceEntry>,
    detected_platform: &PlatformFamily,
) -> CompatibilityReport {
    let android_devices = get_connected_devices(app.clone())
        .await
        .unwrap_or_default()
        .into_iter()
        .filter(is_android_device)
        .collect::<Vec<_>>();

    let connected_device = if android_devices.len() == 1 {
        android_devices.first().map(connected_device_summary)
    } else {
        None
    };

    let mut reasons = Vec::new();
    if android_devices.len() > 1 {
        reasons.push(
            "Multiple Android devices are connected, so auto-association was skipped for compatibility checks."
                .to_string(),
        );
    } else if android_devices.is_empty() {
        reasons.push(
            "No Android device is connected; compatibility remains package-only until a device is attached."
                .to_string(),
        );
    }

    if let Some(entry) = matched_entry {
        reasons.push(format!(
            "Package metadata matched Device DB entry {} {} ({})",
            entry.brand, entry.model, entry.codename
        ));
    } else {
        reasons.push("No exact Device DB match was extracted from package hints.".to_string());
    }

    let matched_database_entry = matched_entry.map(device_match_from_entry);

    let Some(connected) = android_devices.first().filter(|_| android_devices.len() == 1) else {
        return CompatibilityReport {
            state: CompatibilityState::Unknown,
            score: 35,
            reasons,
            connected_device,
            matched_database_entry,
        };
    };

    let connected_db_entry = DeviceDb::global().lookup_by_model(&connected.model).cloned();
    if connected_db_entry.is_none() {
        reasons.push(
            "Connected device could not be resolved in Device DB, so only generic model matching was applied."
                .to_string(),
        );
    }

    let connected_model = normalize_match_token(&connected.model);
    let connected_brand = connected_db_entry
        .as_ref()
        .map(|entry| normalize_match_token(&entry.brand));
    let connected_codename = connected_db_entry
        .as_ref()
        .map(|entry| normalize_match_token(&entry.codename));
    let package_codenames = hints
        .codename
        .iter()
        .map(|value| normalize_match_token(value))
        .collect::<Vec<_>>();
    let package_products = hints
        .product
        .iter()
        .map(|value| normalize_match_token(value))
        .collect::<Vec<_>>();
    let package_brand = detected_brand.map(normalize_match_token);

    let score: u8;
    let state = if let (Some(package_entry), Some(device_entry)) = (matched_entry, connected_db_entry.as_ref()) {
        if !package_entry.codename.is_empty()
            && !device_entry.codename.is_empty()
            && normalize_match_token(&package_entry.codename) == normalize_match_token(&device_entry.codename)
        {
            score = 98;
            reasons.push("Connected device codename matches the package codename exactly.".to_string());
            CompatibilityState::Compatible
        } else if normalize_match_token(&package_entry.model) == normalize_match_token(&device_entry.model) {
            score = 92;
            reasons.push("Connected device model matches the package model exactly.".to_string());
            CompatibilityState::Compatible
        } else if normalize_match_token(&package_entry.brand) == normalize_match_token(&device_entry.brand) {
            score = 72;
            reasons.push("Connected device brand matches the package brand, but codename/model differ.".to_string());
            CompatibilityState::LikelyCompatible
        } else {
            score = 18;
            reasons.push("Connected device Device DB entry conflicts with the package metadata.".to_string());
            CompatibilityState::Incompatible
        }
    } else if package_codenames.iter().any(|hint| Some(hint) == connected_codename.as_ref())
        || package_products.iter().any(|hint| !hint.is_empty() && connected_model.contains(hint))
    {
        score = 78;
        reasons.push("Connected device model hints align with the package metadata.".to_string());
        CompatibilityState::LikelyCompatible
    } else if package_brand.as_ref().is_some() && package_brand.as_ref() == connected_brand.as_ref() {
        score = 62;
        reasons.push("Connected device brand aligns with the detected package brand.".to_string());
        CompatibilityState::LikelyCompatible
    } else if package_brand.is_some() && connected_brand.is_some() && package_brand != connected_brand {
        score = 20;
        reasons.push("Connected device brand does not match the package brand.".to_string());
        CompatibilityState::Incompatible
    } else if matches!(detected_platform, PlatformFamily::Unknown) {
        score = 30;
        CompatibilityState::Unknown
    } else {
        score = 40;
        CompatibilityState::Unknown
    };

    CompatibilityReport {
        state,
        score,
        reasons,
        connected_device,
        matched_database_entry,
    }
}

fn derive_validation_status(
    blockers: &[String],
    entries: &[FlashEntry],
    execution_supported: bool,
) -> QueueStatus {
    if blockers.is_empty() && !entries.is_empty() && execution_supported {
        QueueStatus::Ready
    } else {
        QueueStatus::Blocked
    }
}

async fn analyze_rom(app: &AppHandle, file_path: &Path) -> Result<RomAnalysis, RomManagerError> {
    let context = inspect_archive(file_path)?;
    let archive_sha256 = sha256_file(file_path)?;
    let indicators = build_indicators(&context);
    let hints = collect_hints(&context, file_path);
    let rom_type = detect_rom_type(&indicators);
    let preliminary_match = find_best_database_entry(&hints, None);
    let detected_brand = detect_brand(&rom_type, &context, &hints, preliminary_match.as_ref());
    let matched_entry = preliminary_match.or_else(|| find_best_database_entry(&hints, detected_brand.as_deref()));
    let detected_platform = detect_platform(
        &rom_type,
        &indicators,
        matched_entry.as_ref(),
        detected_brand.as_deref(),
    );
    let flash_mode = flash_mode_for_rom_type(&rom_type);
    let package_file_name = file_name_string(file_path);
    let flash_entries = flash_entries_for_rom(&context, &rom_type, &package_file_name)?;
    let compatibility = build_compatibility_report(
        app,
        &hints,
        detected_brand.as_deref(),
        matched_entry.as_ref(),
        &detected_platform,
    )
    .await;

    let mut warnings = Vec::new();
    let mut blockers = Vec::new();

    if !context.unsafe_paths.is_empty() {
        blockers.push(format!(
            "Archive contains unsafe inner paths that would be rejected for extraction: {}",
            context.unsafe_paths.join(", ")
        ));
    }
    if matches!(rom_type, RomType::Unknown) {
        blockers.push("ROM type could not be determined from the archive structure.".to_string());
    }
    if flash_entries.is_empty() {
        blockers.push("No flashable entries were derived from the archive contents.".to_string());
    }
    if hints.product.is_empty() && hints.codename.is_empty() {
        warnings.push("No explicit product or codename hints were extracted from package metadata.".to_string());
    }
    if context.checksum_files.is_empty() {
        warnings.push("No checksum manifest was detected inside the archive.".to_string());
    }
    if flash_entries
        .iter()
        .any(|entry| matches!(entry.risk_level, RiskLevel::High | RiskLevel::Critical))
    {
        warnings.push(
            "High-risk partitions were detected in the flash plan; verify target compatibility before execution."
                .to_string(),
        );
    }
    if flash_entries.iter().any(|entry| entry.partition == "super") {
        warnings.push(
            "A dynamic super partition payload was detected; confirm slot and partition-layout compatibility."
                .to_string(),
        );
    }

    let data_wipe_implied = detect_data_wipe(&context, &flash_entries);
    if data_wipe_implied {
        warnings.push("Package contents imply a userdata or factory-reset style wipe.".to_string());
    }

    if compatibility.state == CompatibilityState::Incompatible && compatibility.connected_device.is_some() {
        blockers.push("Connected device metadata conflicts with the imported ROM package.".to_string());
    }

    if let Some(message) = unsupported_execution_blocker(&flash_mode) {
        blockers.push(message);
    }

    let dangerous = data_wipe_implied
        || flash_entries
            .iter()
            .any(|entry| matches!(entry.risk_level, RiskLevel::High | RiskLevel::Critical));
    let validation_status = derive_validation_status(&blockers, &flash_entries, execution_supported(&flash_mode));
    let summary = build_summary(
        file_path,
        &context,
        rom_type.clone(),
        detected_brand.clone(),
        detected_platform.clone(),
        flash_mode.clone(),
        &hints,
    );
    let validation = PackageValidation {
        valid: blockers.is_empty(),
        status: validation_status.clone(),
        warnings: warnings.clone(),
        blockers: blockers.clone(),
        execution_supported: execution_supported(&flash_mode),
        dangerous,
    };
    let flash_plan = FlashPlan {
        rom_summary: summary.clone(),
        detected_platform: detected_platform.clone(),
        detected_brand: detected_brand.clone(),
        supported_flash_mode: flash_mode.clone(),
        required_device_state: required_device_state(&flash_mode),
        flash_entries: flash_entries.clone(),
        data_wipe_implied,
        bootloader_unlock_required: matches!(flash_mode, FlashMode::Fastboot),
        looks_dangerous_or_incomplete: dangerous || !blockers.is_empty(),
        execution_supported: execution_supported(&flash_mode),
        warnings: warnings.clone(),
        blockers: blockers.clone(),
    };

    Ok(RomAnalysis {
        package_id: build_package_id(file_path),
        generated_at: now_rfc3339(),
        summary,
        archive_sha256,
        archive_entries: context.archive_entries,
        payload_files: sorted_strings(&context.payload_files),
        manifest_files: sorted_strings(&context.manifest_files),
        partition_candidates: partition_candidates_from_entries(&flash_entries),
        indicators,
        compatibility,
        validation,
        flash_plan,
    })
}

fn refresh_queue_item_status(item: &mut QueueItem) {
    let has_selection = !item.selected_partitions.is_empty();
    item.status = if has_selection && item.blockers.is_empty() && item.execution_supported {
        QueueStatus::Ready
    } else {
        QueueStatus::Blocked
    };
    item.analysis.validation.status = item.status.clone();
}

#[tauri::command]
pub async fn rom_select_file(app: AppHandle) -> Result<Option<String>, String> {
    let handle = app.clone();
    let selected = tokio::task::spawn_blocking(move || {
        let result = handle
            .dialog()
            .file()
            .add_filter("ROM ZIP", &["zip"])
            .blocking_pick_file();

        match result {
            Some(file_path) => file_path
                .into_path()
                .map(|path| Some(path_to_string(&path)))
                .map_err(|error| RomManagerError::Dialog(error.to_string())),
            None => Ok(None),
        }
    })
    .await
    .map_err(|error| RomManagerError::Dialog(error.to_string()))
    .and_then(|result| result)
    .map_err(|error| error.to_string())?;

    Ok(selected)
}

#[tauri::command]
pub async fn rom_detect_type(app: AppHandle, file_path: String) -> Result<RomDetection, String> {
    let resolved_path = validate_zip_path(&file_path).map_err(|error| error.to_string())?;
    let analysis = analyze_rom(&app, &resolved_path)
        .await
        .map_err(|error| error.to_string())?;

    Ok(RomDetection {
        rom_type: analysis.summary.rom_type,
        detected_brand: analysis.summary.detected_brand,
        detected_platform: analysis.summary.detected_platform,
        supported_flash_mode: analysis.summary.supported_flash_mode,
        warnings: analysis.validation.warnings,
        blockers: analysis.validation.blockers,
    })
}

#[tauri::command]
pub async fn rom_inspect_zip(app: AppHandle, file_path: String) -> Result<RomAnalysis, String> {
    let resolved_path = validate_zip_path(&file_path).map_err(|error| error.to_string())?;
    analyze_rom(&app, &resolved_path)
        .await
        .map_err(|error| error.to_string())
}

#[tauri::command]
pub async fn rom_build_flash_plan(app: AppHandle, file_path: String) -> Result<FlashPlan, String> {
    let resolved_path = validate_zip_path(&file_path).map_err(|error| error.to_string())?;
    let analysis = analyze_rom(&app, &resolved_path)
        .await
        .map_err(|error| error.to_string())?;
    Ok(analysis.flash_plan)
}

#[tauri::command]
pub async fn rom_validate_package(
    app: AppHandle,
    file_path: String,
) -> Result<PackageValidation, String> {
    let resolved_path = validate_zip_path(&file_path).map_err(|error| error.to_string())?;
    let analysis = analyze_rom(&app, &resolved_path)
        .await
        .map_err(|error| error.to_string())?;
    Ok(analysis.validation)
}

#[tauri::command]
pub async fn rom_get_queue() -> Result<Vec<QueueItem>, String> {
    let queue = queue_lock().map_err(|error| error.to_string())?;
    Ok(queue.clone())
}

#[tauri::command]
pub async fn rom_add_to_queue(app: AppHandle, file_path: String) -> Result<QueueItem, String> {
    let resolved_path = validate_zip_path(&file_path).map_err(|error| error.to_string())?;
    let analysis = analyze_rom(&app, &resolved_path)
        .await
        .map_err(|error| error.to_string())?;

    let mut item = QueueItem {
        id: analysis.package_id.clone(),
        file_path: analysis.summary.file_path.clone(),
        file_name: analysis.summary.file_name.clone(),
        rom_type: analysis.summary.rom_type.clone(),
        flash_mode: analysis.summary.supported_flash_mode.clone(),
        detected_brand: analysis.summary.detected_brand.clone(),
        detected_platform: analysis.summary.detected_platform.clone(),
        status: analysis.validation.status.clone(),
        execution_supported: analysis.validation.execution_supported,
        selected_partitions: analysis
            .flash_plan
            .flash_entries
            .iter()
            .filter(|entry| entry.enabled)
            .map(|entry| entry.partition.clone())
            .collect(),
        warnings: analysis.validation.warnings.clone(),
        blockers: analysis.validation.blockers.clone(),
        added_at: now_rfc3339(),
        analysis,
    };
    refresh_queue_item_status(&mut item);

    let mut queue = queue_lock().map_err(|error| error.to_string())?;
    if let Some(index) = queue.iter().position(|entry| entry.file_path == item.file_path) {
        queue[index] = item.clone();
    } else {
        queue.push(item.clone());
    }

    Ok(item)
}

#[tauri::command]
pub async fn rom_remove_from_queue(queue_id: String) -> Result<Vec<QueueItem>, String> {
    let mut queue = queue_lock().map_err(|error| error.to_string())?;
    let original_len = queue.len();
    queue.retain(|item| item.id != queue_id);
    if queue.len() == original_len {
        return Err(RomManagerError::QueueItemNotFound(queue_id).to_string());
    }
    Ok(queue.clone())
}

#[tauri::command]
pub async fn rom_clear_queue() -> Result<Vec<QueueItem>, String> {
    let mut queue = queue_lock().map_err(|error| error.to_string())?;
    queue.clear();
    Ok(Vec::new())
}

#[tauri::command]
pub async fn rom_move_queue_item(
    from_index: usize,
    to_index: usize,
) -> Result<Vec<QueueItem>, String> {
    let mut queue = queue_lock().map_err(|error| error.to_string())?;
    if from_index >= queue.len() || to_index >= queue.len() {
        return Err(RomManagerError::QueueIndexOutOfRange.to_string());
    }

    let item = queue.remove(from_index);
    queue.insert(to_index, item);
    Ok(queue.clone())
}

#[tauri::command]
pub async fn rom_toggle_queue_partition(
    queue_id: String,
    partition: String,
    enabled: bool,
) -> Result<QueueItem, String> {
    let mut queue = queue_lock().map_err(|error| error.to_string())?;
    let item = queue
        .iter_mut()
        .find(|item| item.id == queue_id)
        .ok_or_else(|| RomManagerError::QueueItemNotFound(queue_id.clone()).to_string())?;

    let partition_key = partition.to_ascii_lowercase();
    for entry in &mut item.analysis.flash_plan.flash_entries {
        if entry.partition.to_ascii_lowercase() == partition_key {
            entry.enabled = enabled;
        }
    }

    if enabled {
        if !item
            .selected_partitions
            .iter()
            .any(|value| value.eq_ignore_ascii_case(&partition))
        {
            item.selected_partitions.push(partition.clone());
            item.selected_partitions.sort();
        }
    } else {
        item.selected_partitions
            .retain(|value| !value.eq_ignore_ascii_case(&partition));
    }

    refresh_queue_item_status(item);
    Ok(item.clone())
}
