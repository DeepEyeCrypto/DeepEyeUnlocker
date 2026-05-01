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

/// Query local CVE database for specific iOS/Chip vulnerabilities
/// Uses a curated offline database of known iOS exploitation vectors
#[tauri::command]
pub fn query_cve_database(version: String, chip: String) -> Result<String, String> {
    let mut vulnerabilities = Vec::new();

    // iOS 14.x exploits
    if version.starts_with("14.") {
        vulnerabilities.push("CVE-2021-1782: Kernel race condition (cicuta_virosa) — jailbreak vector");
        vulnerabilities.push("CVE-2021-1870: WebKit type confusion — remote code execution");
        vulnerabilities.push("CVE-2020-9907: AppleAVD memory corruption");
    }

    // iOS 15.x exploits
    if version.starts_with("15.") {
        vulnerabilities.push("CVE-2022-26766: WebKit UAF (checkra1n/palera1n compatible)");
        vulnerabilities.push("CVE-2021-30883: IOMobileFrameBuffer LPE");
        vulnerabilities.push("CVE-2022-32917: Kernel LPE — active exploitation reported");
        vulnerabilities.push("CVE-2021-30900: IOMobileFrameBuffer OOB write");
    }

    // iOS 16.x exploits
    if version.starts_with("16.") {
        vulnerabilities.push("CVE-2023-28205: WebKit Use-After-Free");
        vulnerabilities.push("CVE-2022-46689: MacDirtyCow (FilzaEscaped/TrollStore vector)");
        vulnerabilities.push("CVE-2023-23529: WebKit type confusion — actively exploited");
        vulnerabilities.push("CVE-2023-32434: Kernel integer overflow (Triangulation exploit)");
    }

    // iOS 17.x exploits
    if version.starts_with("17.") {
        vulnerabilities.push("CVE-2024-23222: WebKit type confusion — actively exploited");
        vulnerabilities.push("CVE-2023-42824: Kernel LPE — local attacker elevation");
        vulnerabilities.push("CVE-2024-23296: RTKit memory corruption");
    }

    // Chip-specific hardware exploits
    let chip_lower = chip.to_lowercase();
    if chip_lower.contains("a7") || chip_lower.contains("a8") || chip_lower.contains("a9")
        || chip_lower.contains("a10") || chip_lower.contains("a11")
    {
        vulnerabilities.push("Hardware: checkm8 bootrom exploit (A5-A11) — unpatchable, persistent");
    }

    if chip_lower.contains("a12") || chip_lower.contains("a13") || chip_lower.contains("a14")
        || chip_lower.contains("a15") || chip_lower.contains("a16") || chip_lower.contains("a17")
        || chip_lower.contains("a18")
    {
        vulnerabilities.push("Hardware: No bootrom exploit available — software-only vectors apply");
        vulnerabilities.push("Strategy: Activation bypass via ideviceactivation + carrier server");
    }

    if vulnerabilities.is_empty() {
        Ok(format!(
            "No known exploitation vectors for iOS {} on {} in local database.\n\
             💡 Update the CVE database or check for newer entries.",
            version, chip
        ))
    } else {
        Ok(format!(
            "=== CVE Intelligence Report ===\n\
             Target: iOS {} on {}\n\
             Found {} known vectors:\n\n{}",
            version,
            chip,
            vulnerabilities.len(),
            vulnerabilities.join("\n")
        ))
    }
}

/// Perform a real intelligence scan using connected device data
#[tauri::command]
pub async fn run_intelligence_scan(app: AppHandle, ecid: String) -> Result<String, String> {
    // Real intelligence scan: reads device state via libimobiledevice tools
    run_bash(
        &app,
        &format!(
            "export PATH=\"/usr/local/bin:/opt/homebrew/bin:$PATH\" && \
             echo '=== DeepEye Intelligence Scan ===' && \
             echo 'ECID: {ecid}' && \
             echo '' && \
             UDID=$(idevice_id -l 2>/dev/null | head -1) && \
             if [ -z \"$UDID\" ]; then \
                 echo '❌ No device connected — connect iPhone to run scan'; \
                 exit 1; \
             fi && \
             echo '--- Device Identity ---' && \
             echo \"UDID:       $UDID\" && \
             echo \"Model:      $(ideviceinfo -u $UDID -k ProductType 2>/dev/null)\" && \
             echo \"iOS:        $(ideviceinfo -u $UDID -k ProductVersion 2>/dev/null)\" && \
             echo \"Chip ID:    $(ideviceinfo -u $UDID -k HardwarePlatform 2>/dev/null)\" && \
             echo \"Baseband:   $(ideviceinfo -u $UDID -k BasebandVersion 2>/dev/null)\" && \
             echo '' && \
             echo '--- Security State ---' && \
             ACT=$(ideviceinfo -u $UDID -k ActivationState 2>/dev/null) && \
             echo \"Activation: $ACT\" && \
             FMI=$(ideviceactivation state -u $UDID 2>/dev/null | head -3) && \
             echo \"FMI State:  $FMI\" && \
             SUP=$(ideviceinfo -u $UDID -k IsSupervised 2>/dev/null) && \
             echo \"Supervised: $SUP\" && \
             DEP=$(ideviceinfo -u $UDID -k IsDEPEnrolled 2>/dev/null) && \
             echo \"DEP/ABM:    $DEP\" && \
             echo '' && \
             echo '--- Network State ---' && \
             echo \"Carrier:    $(ideviceinfo -u $UDID -k CarrierName 2>/dev/null)\" && \
             echo \"SIM Status: $(ideviceinfo -u $UDID -k SIMStatus 2>/dev/null)\" && \
             echo \"IMEI:       $(ideviceinfo -u $UDID -k InternationalMobileEquipmentIdentity 2>/dev/null)\" && \
             echo \"ICCID:      $(ideviceinfo -u $UDID -k IntegratedCircuitCardIdentity 2>/dev/null)\" && \
             echo '' && \
             echo '--- Apple Server Check ---' && \
             HTTP=$(curl -s --max-time 4 -o /dev/null -w '%{{http_code}}' https://albert.apple.com/WebObjects/MZInit.woa/wa/deviceActivation 2>/dev/null) && \
             if [ \"$HTTP\" = '405' ] || [ \"$HTTP\" = '200' ] || [ \"$HTTP\" = '401' ] || [ \"$HTTP\" = '403' ]; then \
                 echo 'Activation Server: ✅ Reachable'; \
             else \
                 echo \"Activation Server: ⚠️  HTTP $HTTP\"; \
             fi && \
             echo '' && \
             echo '--- Exploit Surface Analysis ---' && \
             IOS_VER=$(ideviceinfo -u $UDID -k ProductVersion 2>/dev/null) && \
             MAJOR=$(echo $IOS_VER | cut -d. -f1) && \
             if [ \"$MAJOR\" -le 14 ] 2>/dev/null; then \
                 echo 'Risk Level: HIGH — Multiple known exploit chains available'; \
             elif [ \"$MAJOR\" -le 16 ] 2>/dev/null; then \
                 echo 'Risk Level: MEDIUM — Software bypass vectors available'; \
             else \
                 echo 'Risk Level: LOW — Latest iOS, activation bypass only'; \
             fi && \
             echo '' && \
             echo '✅ Intelligence scan complete'"
        ),
    )
    .await
}
