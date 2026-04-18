use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage7_gpt(serial: Option<String>) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        7,
        "GPT Read",
        "Prepare the partition table pull used to enumerate logical targets safely.",
        "Stage 8: Partition Map",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Dump primary GPT headers before erase or program operations.".to_string(),
            "Verify backup GPT consistency if storage layout appears malformed.".to_string(),
            "Cache GPT metadata alongside the detected serial for session reuse.".to_string(),
        ],
    ))
}
