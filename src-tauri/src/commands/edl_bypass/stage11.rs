use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage11_userdata_plan(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        11,
        "Userdata Plan",
        "Prepare the sector-level strategy for formatting or reading userdata safely.",
        "Stage 12: Userdata Format",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Confirm userdata capacity and sparse image expectations first.".to_string(),
            "Back up metadata-critical regions when preserving user assets matters.".to_string(),
            "Prefer explicit label-based operations over guessed offsets.".to_string(),
        ],
    ))
}
