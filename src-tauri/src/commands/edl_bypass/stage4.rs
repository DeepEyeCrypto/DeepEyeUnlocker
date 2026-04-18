use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage4_firehose_upload(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        4,
        "Firehose Upload",
        "Prepare the Sahara READ_DATA exchange used to stream the programmer image.",
        "Stage 5: Firehose Configure",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Watch for READ_DATA and END_OF_IMAGE requests from the device.".to_string(),
            "Validate the uploaded programmer size against the on-disk ELF.".to_string(),
            "Retry upload after reconnecting if Firehose does not respond.".to_string(),
        ],
    ))
}
