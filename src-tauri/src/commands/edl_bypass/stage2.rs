use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage2_sahara(serial: Option<String>) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        2,
        "Sahara Handshake",
        "Validate the Qualcomm HELLO exchange and keep the 9008 session stable.",
        "Stage 3: Programmer Selection",
        serial,
        &["qdl", "edl"],
        false,
        vec![
            "Confirm the device remains enumerated as 05C6:9008.".to_string(),
            "Capture Sahara hello/version data before loading Firehose.".to_string(),
            "Retry with a short USB-C cable if handshake drops mid-session.".to_string(),
        ],
    ))
}
