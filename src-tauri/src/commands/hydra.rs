use tauri::AppHandle;
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;

#[tauri::command]
pub async fn hydra_detect_protocol(_app: AppHandle, vid: u16, pid: u16) -> Result<String, String> {
    let protocol = match (vid, pid) {
        (0x05C6, 0x9008) => "EDL_SAHARA_FIREHOSE",
        (0x05C6, 0x9006) => "EDL_SAHARA_NAND",
        (0x04E8, 0x685D) | (0x04E8, 0x6860) | (0x04E8, 0x685E) => "SAMSUNG_ODIN",
        (0x0E8D, 0x0003) => "MTK_BROM",
        (0x0E8D, 0x0001) => "MTK_META",
        (0x1782, 0x4D00) => "SPD_FDL1",
        (0x1782, 0x4D01) => "SPD_FDL2",
        (0x1004, 0x6300) => "LG_LAF",
        (0x1004, 0x633E) => "LG_DOWNLOAD_MODE",
        (0x22B8, 0x2E76) => "MOTO_EDL",
        (0x12D1, 0x1037) => "HUAWEI_HISI",
        _ => "UNKNOWN",
    };
    Ok(protocol.to_string())
}

#[tauri::command]
pub async fn hydra_run_mtk_meta(app: AppHandle, imei: String) -> Result<String, String> {
    let shell = app.shell();
    let at_cmd = format!("AT+EGMR=1,7,\"{}\"", imei);
    let (mut rx, _child) = shell
        .command("minicom")
        .args(["-D", "/dev/ttyUSB0", "-b", "115200", "--send-cmd", &at_cmd])
        .spawn()
        .map_err(|e| format!("META mode error: {e}"))?;

    let mut out = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Terminated(_) => break,
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

#[tauri::command]
pub async fn hydra_samsung_frp_bypass(app: AppHandle, method: String) -> Result<String, String> {
    let shell = app.shell();
    let args: Vec<&str> = match method.as_str() {
        "odin_flash" => vec!["--frp", "--odin"],
        "adb_sideload" => vec!["--frp", "--adb"],
        _ => return Err(format!("Unknown method: {method}")),
    };

    let (mut rx, _child) = shell
        .command("hydra-cli")
        .args(args)
        .spawn()
        .map_err(|e| format!("hydra-cli error: {e}"))?;

    let mut out = String::new();
    let mut err = String::new();
    while let Some(event) = rx.recv().await {
        match event {
            CommandEvent::Stdout(b) => out.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Stderr(b) => err.push_str(&String::from_utf8_lossy(&b)),
            CommandEvent::Error(e) => return Err(e),
            CommandEvent::Terminated(s) => {
                if s.code.unwrap_or(-1) != 0 {
                    return Err(format!("hydra-cli exit: {err}"));
                }
                break;
            }
            _ => {}
        }
    }
    Ok(out.trim().to_string())
}

