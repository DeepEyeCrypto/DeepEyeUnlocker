use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage18_power_control(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        18,
        "Power Control",
        "Plan reset, EDL reboot, and normal boot transitions after Firehose activity.",
        "Stage 19: Verification",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Choose reset or edl power values based on the next diagnostic step.".to_string(),
            "Allow enough time for USB re-enumeration after a power command.".to_string(),
            "Capture the final mode transition in the session log.".to_string(),
        ],
    ))
}
