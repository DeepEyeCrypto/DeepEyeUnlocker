use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage9_frp_plan(serial: Option<String>) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        9,
        "FRP Plan",
        "Assemble the partition and offset strategy for Factory Reset Protection workflows.",
        "Stage 10: FRP Erase",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Confirm the exact FRP label from GPT before sending erase commands.".to_string(),
            "Prefer a partition-scoped wipe instead of whole-disk operations.".to_string(),
            "Save the original FRP block region before modifying persistent state.".to_string(),
        ],
    ))
}
