use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage10_frp_erase(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        10,
        "FRP Erase",
        "Queue the XML erase flow that clears the validated FRP partition label.",
        "Stage 11: Userdata Plan",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Issue erase only after Firehose configure returns ACK.".to_string(),
            "Monitor for NAK responses that indicate unsupported labels.".to_string(),
            "Reboot back to Android or recovery only after wipe verification.".to_string(),
        ],
    ))
}
