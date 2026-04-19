use serde::{Deserialize, Serialize};

// Shared helpers used by ALL 20 stages

pub fn path_env() -> String {
    let base = std::env::var("PATH").unwrap_or_else(|_| "/usr/bin:/bin".to_string());
    format!("/usr/local/bin:/opt/homebrew/bin:/usr/local/sbin:{base}")
}

pub fn run_cmd(bin: &str, args: &[&str]) -> (bool, String) {
    match std::process::Command::new(bin)
        .env("PATH", path_env())
        .args(args)
        .output()
    {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout).trim().to_string();
            let stderr = String::from_utf8_lossy(&out.stderr).trim().to_string();
            (
                out.status.success(),
                if stdout.is_empty() { stderr } else { stdout },
            )
        }
        Err(error) => (false, format!("not found: {error}")),
    }
}

#[allow(dead_code)]
pub fn run_adb(serial: &str, args: &[&str]) -> (bool, String) {
    let mut full = vec!["-s", serial];
    full.extend_from_slice(args);
    run_cmd("adb", &full)
}

#[allow(dead_code)]
pub fn adb_shell(serial: &str, cmd: &str) -> String {
    run_adb(serial, &["shell", cmd]).1
}

pub fn wait_secs(s: u64) {
    std::thread::sleep(std::time::Duration::from_secs(s));
}

pub fn find_firehose() -> Option<String> {
    let home = std::env::var("HOME").unwrap_or_default();
    let paths = vec![
        format!("{home}/edl_loaders/prog_firehose_ddr.elf"),
        format!("{home}/edl_loaders/prog_firehose.elf"),
        "/usr/local/share/qdl/prog_firehose.elf".to_string(),
        "/opt/homebrew/share/qdl/prog_firehose.elf".to_string(),
        format!("{home}/Documents/edl_loaders/prog_firehose_ddr.elf"),
    ];

    paths
        .into_iter()
        .find(|path| std::path::Path::new(path).exists())
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EdlPipelineStageResult {
    pub stage: usize,
    pub title: String,
    pub subtitle: String,
    pub next_stage_title: String,
    pub serial: String,
    pub stage_passed: bool,
    pub stage_message: String,
    pub tool_name: String,
    pub tool_available: bool,
    pub firehose_path: String,
    pub firehose_found: bool,
    pub suggested_actions: Vec<String>,
}

pub fn tool_available(bin: &str) -> bool {
    run_cmd("which", &[bin]).0
}

pub fn resolve_tool(candidates: &[&str]) -> (bool, String) {
    for candidate in candidates {
        if tool_available(candidate) {
            return (true, (*candidate).to_string());
        }
    }

    (false, candidates.join("/"))
}

pub fn build_stage_result(
    stage: usize,
    title: &str,
    subtitle: &str,
    next_stage_title: &str,
    serial: Option<String>,
    tool_candidates: &[&str],
    requires_firehose: bool,
    suggested_actions: Vec<String>,
) -> EdlPipelineStageResult {
    let (tool_available, tool_name) = resolve_tool(tool_candidates);
    let firehose_path = find_firehose().unwrap_or_default();
    let firehose_found = !firehose_path.is_empty();
    let stage_passed = tool_available && (!requires_firehose || firehose_found);
    let serial_label = serial
        .clone()
        .filter(|value| !value.trim().is_empty())
        .unwrap_or_else(|| "USB transport".to_string());

    let stage_message = if stage_passed {
        format!(
            "✅ Stage {stage}/20 ready — {title} prepared for {serial_label}. Next: {next_stage_title}"
        )
    } else if !tool_available {
        format!(
            "⚠️ Stage {stage}/20 blocked — required tool missing from PATH: {}",
            tool_candidates.join(" / ")
        )
    } else {
        format!(
            "⚠️ Stage {stage}/20 waiting — firehose programmer not found before {next_stage_title}"
        )
    };

    EdlPipelineStageResult {
        stage,
        title: title.to_string(),
        subtitle: subtitle.to_string(),
        next_stage_title: next_stage_title.to_string(),
        serial: serial.unwrap_or_default(),
        stage_passed,
        stage_message,
        tool_name,
        tool_available,
        firehose_path,
        firehose_found,
        suggested_actions,
    }
}
