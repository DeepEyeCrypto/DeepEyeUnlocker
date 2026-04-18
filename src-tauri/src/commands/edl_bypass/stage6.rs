use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage6_storage_probe(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        6,
        "Storage Probe",
        "Read initial storage parameters to confirm UFS or eMMC access characteristics.",
        "Stage 7: GPT Read",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Query physical partition zero before issuing partition-level commands.".to_string(),
            "Record total blocks and sector size for downstream read/write math.".to_string(),
            "Validate UFS vs eMMC hints before using target-specific loaders.".to_string(),
        ],
    ))
}
