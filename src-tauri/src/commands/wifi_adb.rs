use tauri::{command, AppHandle};
use super::rebuild::run_binary;

#[command]
pub async fn pair_wifi_adb(
    app: AppHandle,
    ip: String,
    port: String,
    pairing_code: String
) -> Result<String, String> {
    // adb pair IP:PORT PAIRING_CODE
    let target = format!("{}:{}", ip, port);
    println!("[WiFi ADB] Attempting to pair with {} using code {}", target, pairing_code);
    
    let result = run_binary(&app, "adb", &["pair", &target, &pairing_code]).await?;
    
    if result.to_lowercase().contains("successfully paired") {
         Ok(format!("✅ Successfully paired with {}!", target))
    } else {
         Ok(format!("Pairing result: {}", result))
    }
}

#[command]
pub async fn connect_wifi_adb(
    app: AppHandle,
    ip: String,
    port: String
) -> Result<String, String> {
    // adb connect IP:PORT
    let target = format!("{}:{}", ip, port);
    println!("[WiFi ADB] Connecting to {}...", target);
    
    let result = run_binary(&app, "adb", &["connect", &target]).await?;
    
    if result.contains("connected to") {
        Ok(format!("✅ Connected to {}!", target))
    } else {
        Err(format!("❌ Failed to connect: {}", result))
    }
}

#[command]
pub async fn disconnect_wifi_adb(
    app: AppHandle,
    ip: String,
    port: String
) -> Result<String, String> {
    // adb disconnect IP:PORT
    let target = format!("{}:{}", ip, port);
    let result = run_binary(&app, "adb", &["disconnect", &target]).await?;
    Ok(format!("Disconnected: {}\n{}", target, result))
}

#[command]
pub async fn enable_adb_wifi_mode(
    app: AppHandle
) -> Result<String, String> {
    // Required step for older Android versions: adb tcpip 5555 via USB first
    println!("[WiFi ADB] Switching device to TCPIP mode (requires USB connection)...");
    let result = run_binary(&app, "adb", &["tcpip", "5555"]).await?;
    Ok(format!("✅ TCPIP 5555 mode enabled. You can now disconnect USB and connect via IP.\n{}", result))
}
