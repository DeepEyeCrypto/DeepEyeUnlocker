use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage14_modem_backup(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        14,
        "Modem/EFS Backup",
        "Queue read operations for modem, fsg, modemst, and related radio partitions.",
        "Stage 15: Partition Read",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Capture modemst1 and modemst2 before writeback experiments.".to_string(),
            "Hash backup images immediately after download to catch transfer corruption."
                .to_string(),
            "Verify slot-specific modem labels on A/B devices.".to_string(),
        ],
    ))
}
