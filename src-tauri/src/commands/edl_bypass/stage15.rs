use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage15_partition_read(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        15,
        "Partition Read",
        "Stage large bulk reads with sector tracking for imaging and forensic capture.",
        "Stage 16: Partition Write",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Use bounded sector counts for first-pass validation reads.".to_string(),
            "Write images directly to disk with checksum verification after capture.".to_string(),
            "Watch for stalled bulk endpoints on long transfers.".to_string(),
        ],
    ))
}
