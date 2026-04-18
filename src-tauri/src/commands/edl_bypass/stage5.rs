use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage5_firehose_config(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        5,
        "Firehose Configure",
        "Stage the XML configure packet with payload size and storage-init flags.",
        "Stage 6: Storage Probe",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Negotiate MaxPayloadSizeToTargetInBytes before bulk transfers.".to_string(),
            "Keep SECTOR_SIZE_IN_BYTES aligned to the target storage geometry.".to_string(),
            "Preserve ZLP-aware settings for macOS USB bulk behavior.".to_string(),
        ],
    ))
}
