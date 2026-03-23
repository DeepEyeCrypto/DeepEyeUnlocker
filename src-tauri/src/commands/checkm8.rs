use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

// All chip IDs vulnerable to checkm8
pub const CHECKM8_CHIPS: &[u16] = &[
    0x8960, // A7  — iPhone 5S
    0x7000, // A8  — iPhone 6/6+
    0x7001, // A8X — iPad Air 2
    0x8000, // A9  — iPhone 6S/SE
    0x8003, // A9X — iPad Pro 9.7
    0x8010, // A10 — iPhone 7/7+
    0x8011, // A10X— iPad Pro 10.5/12.9
    0x8015, // A11 — iPhone 8/8+/X
];

#[tauri::command]
pub async fn run_checkm8(
    app: AppHandle,
    chip_id: u16,
    operation: String,   // "pwn" | "frp_bypass" | "activation_patch"
    session_id: String,
) -> Result<String, String> {

    // Verify chip is vulnerable
    if !CHECKM8_CHIPS.contains(&chip_id) {
        return Err(format!(
            "Chip 0x{:04X} is NOT vulnerable to checkm8. \
             checkm8 only works on A7-A11 (iPhone 5S to X).",
            chip_id
        ));
    }

    let python_root = python_module_root(&app);
    let script_path = format!("{}/ios_exploit/checkm8_runner.py", python_root);

    // Run checkm8 via Python subprocess
    let output = app
        .shell()
        .command("python3")
        .args([
            &script_path,
            "--chip-id", &format!("0x{:04X}", chip_id),
            "--operation", &operation,
            "--session-id", &session_id,
        ])
        .env("PYTHONPATH", &python_root)
        .output()
        .await
        .map_err(|e| format!("Failed to start checkm8: {}", e))?;

    if output.status.success() {
        let stdout = String::from_utf8_lossy(&output.stdout).to_string();
        Ok(stdout)
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();
        Err(format!("checkm8 failed: {}", stderr))
    }
}

pub fn python_module_root(app: &AppHandle) -> String {
    app.path()
        .resource_dir()
        .unwrap()
        .join("python")
        .to_string_lossy()
        .to_string()
}
