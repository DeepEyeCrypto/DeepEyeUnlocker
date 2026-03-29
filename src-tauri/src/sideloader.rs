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
pub async fn install_ipa(app: AppHandle, ipa_path: String) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "ideviceinstaller -i '{ipa_path}' 2>&1 || ios-deploy --bundle '{ipa_path}' 2>&1 || echo '❌ Need jailbreak (ideviceinstaller) or Xcode (ios-deploy)'"
        ),
    )
    .await
}

#[tauri::command]
pub async fn sign_and_install(
    app: AppHandle,
    ipa_path: String,
    cert_name: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "TMPDIR=$(mktemp -d) && cd $TMPDIR && cp '{ipa_path}' app.ipa && unzip -q app.ipa && APP=$(ls Payload/*.app | head -1) && codesign -f -s '{cert_name}' --deep \"$APP\" 2>&1 && zip -qr signed.ipa Payload/ && ideviceinstaller -i signed.ipa 2>&1 && echo '✅ Signed and installed' && cd && rm -rf $TMPDIR"
        ),
    )
    .await
}

#[tauri::command]
pub async fn list_installed_apps(app: AppHandle) -> Result<String, String> {
    bash(&app, "ideviceinstaller -l 2>&1 | head -100").await
}

#[tauri::command]
pub async fn uninstall_app(app: AppHandle, bundle_id: String) -> Result<String, String> {
    bash(
        &app,
        &format!("ideviceinstaller -U '{bundle_id}' 2>&1 && echo '✅ Uninstalled: {bundle_id}'"),
    )
    .await
}

#[tauri::command]
pub async fn get_app_info(app: AppHandle, bundle_id: String) -> Result<String, String> {
    bash(&app, &format!("ideviceinstaller -l | grep '{bundle_id}' 2>&1")).await
}

#[tauri::command]
pub async fn reinstall_app(
    app: AppHandle,
    bundle_id: String,
    ipa_path: String,
) -> Result<String, String> {
    uninstall_app(app.clone(), bundle_id).await?;
    install_ipa(app, ipa_path).await
}
