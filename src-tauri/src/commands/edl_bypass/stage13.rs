use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage13_persist_backup(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        13,
        "Persist Backup",
        "Prepare a guarded backup path for calibration and persist partition data.",
        "Stage 14: Modem Backup",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Dump persist before any IMEI or radio-state modifications.".to_string(),
            "Store backups outside temporary directories if the session is long-lived.".to_string(),
            "Label images with device serial and timestamp for later recovery.".to_string(),
        ],
    ))
}
