use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage3_programmer(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        3,
        "Programmer Selection",
        "Resolve the correct Firehose loader path before payload transfer begins.",
        "Stage 4: Firehose Upload",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Match the target storage type before choosing eMMC or UFS programmer.".to_string(),
            "Prefer chipset-specific prog_firehose_ddr.elf binaries when available.".to_string(),
            "Keep a local loader cache under ~/edl_loaders for rapid reuse.".to_string(),
        ],
    ))
}
