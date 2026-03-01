#[tauri::command]
fn scan_usb_devices() -> Result<Vec<String>, String> {
    let mgr = deepeyecore::connection::usb::UsbManager::new();
    mgr.scan_devices().map_err(|e| e.to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![scan_usb_devices])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
