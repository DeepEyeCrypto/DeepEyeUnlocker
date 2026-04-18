use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage20_complete(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        20,
        "Completion",
        "Finalize the 20-stage EDL session with a reusable summary of tool and loader readiness.",
        "Pipeline Complete",
        serial,
        &["qdl", "edl", "adb", "fastboot"],
        true,
        vec![
            "Export logs together with backup images and programmer metadata.".to_string(),
            "Document the working loader and chipset mapping for the next session.".to_string(),
            "Return the device to the target boot mode once verification is complete.".to_string(),
        ],
    ))
}
