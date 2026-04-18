use tauri::command;

use crate::commands::edl_bypass::shared::{build_stage_result, EdlPipelineStageResult};

#[command]
pub async fn edl_stage17_xml_console(
    serial: Option<String>,
) -> Result<EdlPipelineStageResult, String> {
    Ok(build_stage_result(
        17,
        "XML Console",
        "Expose raw Firehose XML planning for advanced configure, read, program, and erase flows.",
        "Stage 18: Power Control",
        serial,
        &["qdl", "edl"],
        true,
        vec![
            "Keep a transcript of XML packets and returned ACK/NAK responses.".to_string(),
            "Validate every label and numeric field before sending custom packets.".to_string(),
            "Fallback to read-only commands when probing unknown storage layouts.".to_string(),
        ],
    ))
}
