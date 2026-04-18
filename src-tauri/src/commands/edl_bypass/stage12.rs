use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage12_userdata_format(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        12,
        "Userdata Format",
        "Ready the destructive userdata erase cycle through Firehose XML operations.",
        "Stage 13: Persist Backup",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Re-check battery stability before long erase cycles.".to_string(),
            "Keep logs of sector size and ACK timing for regression analysis.".to_string(),
            "Avoid unplugging the device until the final success banner is received.".to_string(),
        ],
    ))
}
