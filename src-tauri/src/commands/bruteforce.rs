use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

#[tauri::command]
pub async fn run_pin_bruteforce(
    app: AppHandle,
    pins: Vec<String>,
    delay_ms: u64,
) -> Result<String, String> {
    let mut results = Vec::new();
    for pin in pins {
        // Simulate ADB input for each PIN
        let cmd = format!("adb shell input text {}", pin);
        let output = app
            .shell()
            .command("bash")
            .args(["-c", &cmd])
            .output()
            .await
            .map_err(|e| e.to_string())?;
        
        results.push(format!("PIN {}: {}", pin, String::from_utf8_lossy(&output.stdout)));
        tokio::time::sleep(std::time::Duration::from_millis(delay_ms)).await;
    }
    Ok(results.join("\n"))
}
