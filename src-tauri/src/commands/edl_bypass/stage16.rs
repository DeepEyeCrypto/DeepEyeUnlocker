use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage16_partition_write(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        16,
        "Partition Write",
        "Prepare bulk program packets for raw image flashing across validated labels.",
        "Stage 17: XML Console",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Match image size to num_partition_sectors before programming.".to_string(),
            "Prefer known-good sparse or raw images from trusted build outputs.".to_string(),
            "Read back a verification slice after every critical write.".to_string(),
        ],
    ))
}
