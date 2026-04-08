use serde::{Deserialize, Serialize};
use std::sync::OnceLock;

static DB: OnceLock<DeviceDb> = OnceLock::new();

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct DeviceEntry {
    pub brand: String,
    pub model: String,
    pub codename: String,
    pub soc: String,
    pub soc_family: SocFamily,
    pub chipset_id: String,
    pub protocol: FlashProtocol,
    pub vid: Option<u16>,
    pub pid: Option<u16>,
    pub firehose_path: Option<String>,
    pub da_file: Option<String>,
    pub frp_partition: Option<String>,
    pub notes: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq)]
pub enum SocFamily {
    Qualcomm,
    MediaTek,
    Samsung,
    Unisoc,
    Kirin,
    Unknown,
}

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq)]
pub enum FlashProtocol {
    Edl,
    MtkBrom,
    SamsungOdin,
    Adb,
    Fastboot,
    Unknown,
}

impl FlashProtocol {
    pub fn route(&self) -> &'static str {
        match self {
            FlashProtocol::Edl => "/edl",
            FlashProtocol::MtkBrom => "/mtk",
            FlashProtocol::SamsungOdin => "/samsung",
            FlashProtocol::Adb => "/adb",
            FlashProtocol::Fastboot => "/romflasher", // Adjusted based on PAGE_PATHS mapping
            FlashProtocol::Unknown => "/adb",
        }
    }
}

#[derive(Debug, Serialize, Clone, Deserialize)]
pub struct HardwareGuide {
    pub mode_name: String,
    pub button_combo: String,
    pub test_point: Option<String>,
    pub steps: Vec<String>,
    pub warning: Option<String>,
    pub danger_zone: bool,
}

#[derive(Debug, Serialize, Deserialize)]
struct DbFile {
    version: String,
    updated: String,
    devices: Vec<DeviceEntry>,
}

pub struct DeviceDb {
    entries: Vec<DeviceEntry>,
}

#[derive(Debug, Serialize)]
pub struct RoutingResult {
    pub device: Option<DeviceEntry>,
    pub protocol: FlashProtocol,
    pub route_to: String,
    pub confidence: u8,
    pub pre_fill: RoutingPreFill,
    pub hardware_guide: HardwareGuide,
    pub frp_partitions: Vec<String>,
    pub danger_zone: bool,
}

#[derive(Debug, Serialize)]
pub struct RoutingPreFill {
    pub firehose_path: Option<String>,
    pub da_path: Option<String>,
    pub partition_hints: Vec<String>,
}

#[derive(Debug, thiserror::Error, Serialize)]
pub enum DbError {
    #[error("Parse error: {0}")]
    ParseError(String),
}

impl DeviceDb {
    pub fn load() -> Result<Self, DbError> {
        let json = include_str!("../../../src/assets/supported_devices.json");
        let db_file: DbFile = serde_json::from_str(json)
            .map_err(|e| DbError::ParseError(e.to_string()))?;
        Ok(Self { entries: db_file.devices })
    }

    pub fn global() -> &'static DeviceDb {
        DB.get_or_init(|| DeviceDb::load().unwrap_or(DeviceDb { entries: vec![] }))
    }

    pub fn lookup_by_model(&self, model: &str) -> Option<&DeviceEntry> {
        let q = model.to_lowercase();
        self.entries.iter().find(|e| {
            e.model.to_lowercase() == q
                || e.model.to_lowercase().contains(&q)
                || q.contains(&e.model.to_lowercase())
        })
    }

    pub fn lookup_by_vid_pid(&self, vid: u16, pid: u16) -> Option<&DeviceEntry> {
        self.entries
            .iter()
            .find(|e| e.vid == Some(vid) && e.pid == Some(pid))
    }

    pub fn search(&self, query: &str) -> Vec<&DeviceEntry> {
        let q = query.to_lowercase();
        let mut results: Vec<(&DeviceEntry, u8)> = self
            .entries
            .iter()
            .filter_map(|e| {
                let score = Self::score(e, &q);
                if score > 0 { Some((e, score)) } else { None }
            })
            .collect();
        results.sort_by(|a, b| b.1.cmp(&a.1));
        results.into_iter().take(10).map(|(e, _)| e).collect()
    }

    fn score(entry: &DeviceEntry, q: &str) -> u8 {
        let model = entry.model.to_lowercase();
        let brand = entry.brand.to_lowercase();
        let codename = entry.codename.to_lowercase();
        let soc = entry.soc.to_lowercase();

        if model == *q || brand == *q || codename == *q { return 100; }
        let mut score: u8 = 0;
        if model.contains(q) { score = score.saturating_add(40); }
        if brand.contains(q) { score = score.saturating_add(30); }
        if codename.contains(q) { score = score.saturating_add(20); }
        if soc.contains(q) { score = score.saturating_add(10); }
        score
    }

    pub fn auto_route(&self, model: &str) -> RoutingResult {
        let m = model.to_lowercase();
        if let Some(entry) = self.lookup_by_model(model) {
            let route_to = entry.protocol.route().to_string();
            let confidence = if entry.model.to_lowercase() == model.to_lowercase() {
                95
            } else {
                75
            };
            
            let (guide_name, combo, steps, warning, danger) = match entry.soc_family {
                SocFamily::MediaTek => ("BROM Mode", "Vol+ & Vol-", vec!["Power off", "Hold Vol Up/Down", "Connect USB"], Some("Ensure battery > 30%"), true),
                SocFamily::Qualcomm => ("EDL Mode (9008)", "Vol+ & Vol-", vec!["Power off", "Hold Vol Up/Down", "Connect USB"], Some("Check Device Manager for 9008"), true),
                SocFamily::Samsung => ("Download Mode", "Vol- + Home + Power", vec!["Power off", "Hold combo", "Vol Up to confirm"], None, false),
                _ => ("ADB / MTP", "N/A", vec!["Enable ADB", "Trust Computer"], Some("Requires valid authorization"), false),
            };

            RoutingResult {
                device: Some(entry.clone()),
                protocol: entry.protocol.clone(),
                route_to,
                confidence,
                pre_fill: RoutingPreFill {
                    firehose_path: entry.firehose_path.clone(),
                    da_path: entry.da_file.clone(),
                    partition_hints: vec!["frp".into(), "config".into()],
                },
                hardware_guide: HardwareGuide {
                    mode_name: guide_name.into(),
                    button_combo: combo.into(),
                    test_point: match entry.codename.as_str() {
                        "merlin" | "surya" | "o1s" => Some(format!("{}_tp", entry.codename)),
                        _ => None,
                    },
                    steps: steps.into_iter().map(String::from).collect(),
                    warning: warning.map(String::from),
                    danger_zone: danger,
                },
                frp_partitions: match &entry.frp_partition {
                    Some(p) => vec![p.clone()],
                    None => vec!["frp".into()],
                },
                danger_zone: danger,
            }
        } else {
            let (protocol, route_to, confidence) =
                if m.contains("samsung") || m.contains("exynos") {
                    (FlashProtocol::SamsungOdin, "/samsung".into(), 60u8)
                } else if m.contains("mt") || m.contains("helio") || m.contains("dimensity") {
                    (FlashProtocol::MtkBrom, "/mtk".into(), 55u8)
                } else if m.contains("snapdragon") || m.contains("sm") || m.contains("sdm") {
                    (FlashProtocol::Edl, "/edl".into(), 55u8)
                } else {
                    (FlashProtocol::Adb, "/adb".into(), 30u8)
                };

            let danger = matches!(protocol, FlashProtocol::MtkBrom | FlashProtocol::Edl);

            RoutingResult {
                device: None,
                protocol,
                route_to,
                confidence,
                pre_fill: RoutingPreFill {
                    firehose_path: None,
                    da_path: None,
                    partition_hints: vec![],
                },
                hardware_guide: HardwareGuide {
                    mode_name: "Generic Mode".into(),
                    button_combo: "Power + Vol Down".into(),
                    test_point: None,
                    steps: vec!["Check online for your specific model".into()],
                    warning: Some("Unrecognized model - proceed with caution".into()),
                    danger_zone: danger,
                },
                frp_partitions: vec!["frp".into()],
                danger_zone: danger,
            }
        }
    }

    pub fn list_all(&self) -> &[DeviceEntry] {
        &self.entries
    }
}

// ════════════════════════════════════════════════════════
// TAURI COMMANDS
// ════════════════════════════════════════════════════════

#[tauri::command]
pub fn db_search_devices(query: String) -> Result<Vec<DeviceEntry>, String> {
    let db = DeviceDb::global();
    Ok(db.search(&query).into_iter().cloned().collect())
}

#[tauri::command]
pub fn db_lookup_model(model: String) -> Result<Option<DeviceEntry>, String> {
    Ok(DeviceDb::global().lookup_by_model(&model).cloned())
}

#[tauri::command]
pub fn db_auto_route(model: String) -> Result<RoutingResult, String> {
    Ok(DeviceDb::global().auto_route(&model))
}

#[tauri::command]
pub fn db_lookup_vid_pid(vid: u16, pid: u16) -> Result<Option<DeviceEntry>, String> {
    Ok(DeviceDb::global().lookup_by_vid_pid(vid, pid).cloned())
}

#[tauri::command]
pub fn db_list_all() -> Result<Vec<DeviceEntry>, String> {
    Ok(DeviceDb::global().list_all().to_vec())
}

#[tauri::command]
pub async fn frp_execute_protocol(
    app: tauri::AppHandle,
    protocol: FlashProtocol,
    partitions: Vec<String>,
) -> Result<(), String> {
    use crate::commands::adb;
    use crate::commands::edl;
    use crate::commands::mtk_brom;
    use crate::commands::samsung;
    use tauri::Emitter;

    app.emit("frp-progress", 10).ok();

    match protocol {
        FlashProtocol::MtkBrom => {
            app.emit("frp-progress", 30).ok();
            for part in partitions {
                mtk_brom::mtk_da_erase_partition(part).await?;
            }
        }
        FlashProtocol::Edl => {
            app.emit("frp-progress", 30).ok();
            for part in partitions {
                edl::edl_erase_partition(part).await?;
            }
        }
        FlashProtocol::Adb => {
            app.emit("frp-progress", 30).ok();
            let devices = adb::adb_list_devices(app.clone()).await?;
            if let Some(dev) = devices.first() {
                adb::adb_erase_frp_partition(app.clone(), dev.serial.clone()).await?;
            } else {
                return Err("No ADB device found".into());
            }
        }
        FlashProtocol::SamsungOdin => {
            app.emit("frp-progress", 30).ok();
            samsung::samsung_erase_frp().map_err(|e| e.to_string())?;
        }
        _ => return Err(format!("Protocol {:?} not yet automated via Guided FRP", protocol)),
    }

    app.emit("frp-progress", 100).ok();
    app.emit("frp-complete", true).ok();

    Ok(())
}
