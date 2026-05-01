use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

async fn run_bash(app: &AppHandle, s: &str) -> Result<String, String> {
    let output = app
        .shell()
        .command("bash")
        .args(["-c", s])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    Ok(format!(
        "{}{}",
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)
    ))
}

/// Check IMEI status by reading real device data and performing Luhn validation
#[tauri::command]
#[allow(clippy::manual_is_multiple_of)]
pub async fn check_imei_intel(app: AppHandle, imei: String) -> Result<String, String> {
    // Luhn validation
    let digits: Vec<u32> = imei.chars().filter_map(|c| c.to_digit(10)).collect();
    let luhn_valid = if digits.len() == 15 {
        let sum: u32 = digits
            .iter()
            .rev()
            .enumerate()
            .map(|(i, &x)| {
                if i % 2 == 1 {
                    let v = x * 2;
                    if v > 9 { v - 9 } else { v }
                } else {
                    x
                }
            })
            .sum();
        sum % 10 == 0
    } else {
        false
    };

    // TAC (first 8 digits) for manufacturer identification
    let tac = if digits.len() >= 8 {
        imei.chars().take(8).collect::<String>()
    } else {
        "N/A".to_string()
    };

    // Read real device data via ideviceinfo for cross-reference
    let cmd = format!(
        "export PATH=\"/usr/local/bin:/opt/homebrew/bin:$PATH\" && \
         echo '=== IMEI Intelligence Report ===' && \
         echo 'IMEI:     {}' && \
         echo 'TAC Code: {}' && \
         echo 'Luhn Check: {}' && \
         echo '' && \
         echo '--- Device Cross-Reference ---' && \
         UDID=$(idevice_id -l 2>/dev/null | head -1) && \
         if [ -n \"$UDID\" ]; then \
             DEV_IMEI=$(ideviceinfo -u \"$UDID\" -k InternationalMobileEquipmentIdentity 2>/dev/null) && \
             DEV_MODEL=$(ideviceinfo -u \"$UDID\" -k ProductType 2>/dev/null) && \
             DEV_SERIAL=$(ideviceinfo -u \"$UDID\" -k SerialNumber 2>/dev/null) && \
             DEV_ACT=$(ideviceinfo -u \"$UDID\" -k ActivationState 2>/dev/null) && \
             DEV_FMI=$(ideviceactivation state -u \"$UDID\" 2>/dev/null | head -1) && \
             echo \"Device IMEI: $DEV_IMEI\" && \
             echo \"Model:       $DEV_MODEL\" && \
             echo \"Serial:      $DEV_SERIAL\" && \
             echo \"Activation:  $DEV_ACT\" && \
             echo \"FMI State:   $DEV_FMI\" && \
             if [ \"$DEV_IMEI\" = '{}' ]; then \
                 echo '' && echo 'Match: ✅ IMEI matches connected device'; \
             else \
                 echo '' && echo 'Match: ⚠️  IMEI does not match connected device'; \
             fi; \
         else \
             echo 'No device connected — standalone IMEI check only'; \
         fi",
        imei,
        tac,
        if luhn_valid { "✅ VALID" } else { "❌ INVALID" },
        imei
    );

    run_bash(&app, &cmd).await
}

/// Get detailed device identity (MEID, Serial, IMEI, UDID) from real connected device
#[tauri::command]
pub async fn get_full_identity(app: AppHandle) -> Result<String, String> {
    run_bash(
        &app,
        "export PATH=\"/usr/local/bin:/opt/homebrew/bin:$PATH\" && \
         UDID=$(idevice_id -l 2>/dev/null | head -1) && \
         if [ -z \"$UDID\" ]; then echo '❌ No device connected'; exit 1; fi && \
         echo '=== Full Device Identity ===' && \
         echo \"UDID:       $UDID\" && \
         echo \"IMEI:       $(ideviceinfo -u $UDID -k InternationalMobileEquipmentIdentity 2>/dev/null)\" && \
         echo \"IMEI2:      $(ideviceinfo -u $UDID -k InternationalMobileEquipmentIdentity2 2>/dev/null)\" && \
         echo \"MEID:       $(ideviceinfo -u $UDID -k MobileEquipmentIdentifier 2>/dev/null)\" && \
         echo \"Serial:     $(ideviceinfo -u $UDID -k SerialNumber 2>/dev/null)\" && \
         echo \"ECID:       $(ideviceinfo -u $UDID -k UniqueChipID 2>/dev/null)\" && \
         echo \"Model:      $(ideviceinfo -u $UDID -k ProductType 2>/dev/null)\" && \
         echo \"iOS:        $(ideviceinfo -u $UDID -k ProductVersion 2>/dev/null)\" && \
         echo \"Build:      $(ideviceinfo -u $UDID -k BuildVersion 2>/dev/null)\" && \
         echo \"WiFi MAC:   $(ideviceinfo -u $UDID -k WiFiAddress 2>/dev/null)\" && \
         echo \"BT MAC:     $(ideviceinfo -u $UDID -k BluetoothAddress 2>/dev/null)\" && \
         echo \"Color:      $(ideviceinfo -u $UDID -k DeviceColor 2>/dev/null)\" && \
         echo \"Region:     $(ideviceinfo -u $UDID -k RegionInfo 2>/dev/null)\"",
    )
    .await
}
