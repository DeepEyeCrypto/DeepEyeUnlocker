use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage8_partition_map(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        8,
        "Partition Map",
        "Translate GPT output into actionable labels for FRP, userdata, modem, and persist.",
        "Stage 9: FRP Plan",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Map label names exactly as exposed by Firehose XML responses.".to_string(),
            "Identify duplicated slots such as boot_a / boot_b before flashing.".to_string(),
            "Mark protected partitions for backup before destructive commands.".to_string(),
        ],
    ))
}
