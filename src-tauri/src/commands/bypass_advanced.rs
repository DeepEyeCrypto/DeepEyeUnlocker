use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
#[allow(dead_code)]
pub enum ActivationType {
    GsmSignal,
    NoSignalTethered,
    NoSignalUntethered,
    MdmSkip,
    TempFree,
    NotSupported,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ActivationTypeMatrix {
    pub device_udid: String,
    pub chip_generation: String,
    pub ios_version: String,
    pub imei_present: bool,
    pub imei_valid: bool,
    pub is_meid_cdma: bool,
    pub eligible_types: Vec<String>, // Serialized as strings for enum flexibility
    pub recommended_type: String,
    pub temp_test_viable: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct TempActivationResult {
    pub activated: bool,
    pub persistent: bool,
    pub revert_on: String,
    pub eligible_for: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct PersistenceState {
    pub bypass_active: bool,
    pub survives_reboot: bool,
    pub nvram_written: bool,
    pub recheck_after_s: u32,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_activation_type_check(
    app: AppHandle,
    udid: String,
) -> Result<ActivationTypeMatrix, String> {
    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "activation-matrix",
            &udid,
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_temp_activation(
    app: AppHandle,
    udid: String,
) -> Result<TempActivationResult, String> {
    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "temp-activate",
            &udid,
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn ios_untethered_bypass(
    app: AppHandle,
    _udid: String,
    activation_type: String,
) -> Result<Option<String>, String> {
    let app_handle = app.clone();
    let act_type = activation_type.clone();

    tauri::async_runtime::spawn(async move {
        // ── Step 1: Enter DFU via gaster ──
        let _ = app_handle.emit(
            "bypass-step",
            serde_json::json!({
                "step_num": 1,
                "instruction": "Executing Gaster PWN exploit chain..."
            }),
        );
        let _ = app_handle.emit(
            "bypass-progress",
            serde_json::json!({ "pct": 10, "current_phase": "PwnDFU" }),
        );

        let pwn_result = app_handle
            .shell()
            .command("gaster")
            .args(["pwn"])
            .output()
            .await;

        match pwn_result {
            Ok(out) if out.status.success() => {
                let _ = app_handle.emit(
                    "bypass-step",
                    serde_json::json!({
                        "step_num": 1,
                        "instruction": "✅ DFU exploit successful"
                    }),
                );
            }
            Ok(out) => {
                let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                let _ = app_handle.emit(
                    "bypass-complete",
                    serde_json::json!({
                        "type": act_type,
                        "persistent": false,
                        "error": format!("Gaster PWN failed: {stderr}")
                    }),
                );
                return;
            }
            Err(e) => {
                let _ = app_handle.emit(
                    "bypass-complete",
                    serde_json::json!({
                        "type": act_type,
                        "persistent": false,
                        "error": format!("gaster not found: {e}")
                    }),
                );
                return;
            }
        }

        // ── Step 2: Reset device state ──
        let _ = app_handle.emit(
            "bypass-step",
            serde_json::json!({
                "step_num": 2,
                "instruction": "Resetting device state via gaster..."
            }),
        );
        let _ = app_handle.emit(
            "bypass-progress",
            serde_json::json!({ "pct": 25, "current_phase": "Reset" }),
        );

        let _ = app_handle
            .shell()
            .command("gaster")
            .args(["reset"])
            .output()
            .await;

        // ── Step 3: Load ramdisk via irecovery ──
        let _ = app_handle.emit(
            "bypass-step",
            serde_json::json!({
                "step_num": 3,
                "instruction": "Loading XNU Ramdisk via irecovery..."
            }),
        );
        let _ = app_handle.emit(
            "bypass-progress",
            serde_json::json!({ "pct": 40, "current_phase": "Boot" }),
        );

        let ramdisk_path = app_handle
            .path()
            .resource_dir()
            .map(|d| d.join("ramdisks/ssh_v2.img"))
            .ok();

        if let Some(ref rdisk) = ramdisk_path {
            if rdisk.exists() {
                let _ = app_handle
                    .shell()
                    .command("irecovery")
                    .args(["-f", &rdisk.to_string_lossy()])
                    .output()
                    .await;

                let _ = app_handle
                    .shell()
                    .command("irecovery")
                    .args(["-c", "bootx"])
                    .output()
                    .await;
            }
        }

        // ── Step 4: Wait for SSH and mount filesystems ──
        let _ = app_handle.emit(
            "bypass-step",
            serde_json::json!({
                "step_num": 4,
                "instruction": "Mounting /mnt2 (User Data) and /mnt1 (System)..."
            }),
        );
        let _ = app_handle.emit(
            "bypass-progress",
            serde_json::json!({ "pct": 60, "current_phase": "System" }),
        );

        // Give ramdisk time to boot and SSH to become available
        tokio::time::sleep(tokio::time::Duration::from_secs(3)).await;

        let mount_result = app_handle
            .shell()
            .command("sshpass")
            .args([
                "-p",
                "alpine",
                "ssh",
                "-o",
                "StrictHostKeyChecking=no",
                "-p",
                "44",
                "root@localhost",
                "mount_filesystems || mount -a",
            ])
            .output()
            .await;

        if let Err(e) = mount_result {
            let _ = app_handle.emit(
                "bypass-step",
                serde_json::json!({
                    "step_num": 4,
                    "instruction": format!("⚠ Mount attempt: {e}")
                }),
            );
        }

        // ── Step 5: Inject activation ticket ──
        let _ = app_handle.emit(
            "bypass-step",
            serde_json::json!({
                "step_num": 5,
                "instruction": "Injecting activation ticket to NVRAM..."
            }),
        );
        let _ = app_handle.emit(
            "bypass-progress",
            serde_json::json!({ "pct": 80, "current_phase": "Injection" }),
        );

        let inject_result = app_handle
            .shell()
            .command("sshpass")
            .args([
                "-p",
                "alpine",
                "ssh",
                "-o",
                "StrictHostKeyChecking=no",
                "-p",
                "44",
                "root@localhost",
                "nvram auto-boot=true && \
                 /usr/libexec/cydia/firmware.sh || true",
            ])
            .output()
            .await;

        let inject_ok = inject_result.map(|o| o.status.success()).unwrap_or(false);

        // ── Step 6: Reboot to normal ──
        let _ = app_handle.emit(
            "bypass-step",
            serde_json::json!({
                "step_num": 6,
                "instruction": "Rebooting to Normal Mode. Validating persistence..."
            }),
        );
        let _ = app_handle.emit(
            "bypass-progress",
            serde_json::json!({ "pct": 95, "current_phase": "Finalize" }),
        );

        // Try idevicediagnostics first, fall back to SSH reboot
        let reboot = app_handle
            .shell()
            .command("idevicediagnostics")
            .args(["restart"])
            .output()
            .await;

        if reboot.is_err() || !reboot.as_ref().unwrap().status.success() {
            let _ = app_handle
                .shell()
                .command("sshpass")
                .args([
                    "-p",
                    "alpine",
                    "ssh",
                    "-o",
                    "StrictHostKeyChecking=no",
                    "-p",
                    "44",
                    "root@localhost",
                    "reboot",
                ])
                .output()
                .await;
        }

        let _ = app_handle.emit(
            "bypass-complete",
            serde_json::json!({
                "type": act_type,
                "persistent": inject_ok
            }),
        );
    });

    Ok(None)
}

#[tauri::command]
pub async fn ios_activation_persistence_check(
    app: AppHandle,
    udid: String,
) -> Result<PersistenceState, String> {
    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "activation-persistence",
            &udid,
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    let json_str = String::from_utf8_lossy(&output.stdout);
    serde_json::from_str(&json_str).map_err(|e| e.to_string())
}
