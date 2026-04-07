use tauri::AppHandle;
use tauri_plugin_shell::ShellExt;

async fn bash(app: &AppHandle, s: &str) -> Result<String, String> {
    let output = app
        .shell()
        .command("bash")
        .args(["-c", s])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    Ok(format!(
        "{}\n{}",
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)
    ))
}

#[tauri::command]
pub async fn get_signed_firmwares(app: AppHandle, identifier: String) -> Result<String, String> {
    let cmd = "curl -s 'https://api.ipsw.me/v4/device/__ID__?type=ipsw' | python3 -c \"import json,sys; d=json.load(sys.stdin); fw=d.get('firmwares',[]); s=[f for f in fw if f.get('signed')]; [print('✅ iOS %s | Build %s | %.2f GB' % (f.get('version','?'), f.get('buildid','?'), (f.get('filesize',0)/1e9))) for f in s]; print('\\n%d version(s) currently signed' % len(s))\" 2>&1".replace("__ID__", &identifier);
    bash(&app, &cmd).await
}

#[tauri::command]
pub async fn get_all_firmwares(app: AppHandle, identifier: String) -> Result<String, String> {
    let cmd = "curl -s 'https://api.ipsw.me/v4/device/__ID__?type=ipsw' | python3 -c \"import json,sys; d=json.load(sys.stdin); fw=d.get('firmwares',[]); [print(('✅' if f.get('signed') else '❌') + (' iOS %s | %s | %.2f GB' % (f.get('version','?'), f.get('buildid','?'), (f.get('filesize',0)/1e9)))) for f in fw[:30]]\" 2>&1".replace("__ID__", &identifier);
    bash(&app, &cmd).await
}

#[tauri::command]
pub async fn download_ipsw(
    app: AppHandle,
    identifier: String,
    build_id: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "URL=$(curl -s 'https://api.ipsw.me/v4/ipsw/{identifier}/{build_id}' | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get(\"url\",\"\"))') && FNAME=$(basename \"$URL\") && mkdir -p ~/DeepEyeUnlocker/ipsw && echo \"Downloading: $FNAME\" && curl -L --progress-bar -o ~/DeepEyeUnlocker/ipsw/$FNAME \"$URL\" && echo \"✅ Saved: ~/DeepEyeUnlocker/ipsw/$FNAME\""
        ),
    )
    .await
}

#[tauri::command]
pub async fn get_download_progress(app: AppHandle) -> Result<String, String> {
    bash(
        &app,
        "ls -lh ~/DeepEyeUnlocker/ipsw/*.ipsw 2>/dev/null || echo 'No IPSW files found'",
    )
    .await
}

#[tauri::command]
pub async fn verify_ipsw_sha1(
    app: AppHandle,
    ipsw_path: String,
    identifier: String,
    build_id: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "EXPECTED=$(curl -s 'https://api.ipsw.me/v4/ipsw/{identifier}/{build_id}' | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get(\"sha1sum\",\"\"))') && ACTUAL=$(shasum '{ipsw_path}' | awk '{{print $1}}') && echo \"Expected: $EXPECTED\" && echo \"Actual:   $ACTUAL\" && [ \"$EXPECTED\" = \"$ACTUAL\" ] && echo '✅ SHA1 MATCH — IPSW valid' || echo '❌ SHA1 MISMATCH — IPSW corrupt!'"
        ),
    )
    .await
}
