#[tauri::command]
fn scan_usb_devices() -> Result<Vec<String>, String> {
    let mgr = deepeyecore::connection::usb::UsbManager::new();
    mgr.scan_devices().map_err(|e| e.to_string())
}

#[tauri::command]
fn get_detailed_usb_devices() -> Result<Vec<deepeyecore::connection::usb::UsbDeviceInfo>, String> {
    deepeyecore::connection::usb::UsbManager::list_detailed_devices().map_err(|e| e.to_string())
}

#[tauri::command]
async fn execute_feature(
    id: u32,
    title: String,
    options: Option<serde_json::Value>,
) -> Result<deepeyecore::models::FeatureExecutionResponse, String> {
    println!(
        "[DeepEye Tauri] Received request to execute Feature #{}: {}",
        id, title
    );

    // Auto detect what's connected to USB right now
    let detected_platform = deepeyecore::connection::usb::UsbManager::auto_detect_platform()
        .unwrap_or(deepeyecore::models::DevicePlatform::Unknown);

    // Convert to strict Core Engine Request Model
    let req = deepeyecore::models::FeatureExecutionRequest {
        feature_id: id,
        title: title.clone(),
        platform: detected_platform,
        options,
    };

    // Forward to Core Engine Orchestrator
    deepeyecore::dispatch_feature(req)
        .await
        .map_err(|e| format!("Core Engine Dispatch Error: {}", e))
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    // Initialize DeepEye Core Engine (Tracing & Persistence)
    deepeyecore::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            scan_usb_devices,
            get_detailed_usb_devices,
            execute_feature
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
