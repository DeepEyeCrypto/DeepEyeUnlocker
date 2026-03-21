use std::process::Command;

fn bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

#[tauri::command]
pub fn install_ipa(ipa_path: String) -> Result<String, String> {
    bash(&format!(
        "ideviceinstaller -i '{ipa_path}' 2>&1 || \
         ios-deploy --bundle '{ipa_path}' 2>&1 || \
         echo '❌ Need jailbreak (ideviceinstaller) or Xcode (ios-deploy)'"
    ))
}

#[tauri::command]
pub fn sign_and_install(ipa_path: String, cert_name: String) -> Result<String, String> {
    bash(&format!(
        "TMPDIR=$(mktemp -d) && \
         cd $TMPDIR && \
         cp '{ipa_path}' app.ipa && \
         unzip -q app.ipa && \
         APP=$(ls Payload/*.app | head -1) && \
         codesign -f -s '{cert_name}' --deep \"$APP\" 2>&1 && \
         zip -qr signed.ipa Payload/ && \
         ideviceinstaller -i signed.ipa 2>&1 && \
         echo '✅ Signed and installed' && \
         cd && rm -rf $TMPDIR"
    ))
}

#[tauri::command]
pub fn list_installed_apps() -> Result<String, String> {
    bash("ideviceinstaller -l 2>&1 | head -100")
}

#[tauri::command]
pub fn uninstall_app(bundle_id: String) -> Result<String, String> {
    bash(&format!(
        "ideviceinstaller -U '{bundle_id}' 2>&1 && \
         echo '✅ Uninstalled: {bundle_id}'"
    ))
}

#[tauri::command]
pub fn get_app_info(bundle_id: String) -> Result<String, String> {
    bash(&format!(
        "ideviceinstaller -l | grep '{bundle_id}' 2>&1"
    ))
}

#[tauri::command]
pub fn reinstall_app(bundle_id: String, ipa_path: String) -> Result<String, String> {
    uninstall_app(bundle_id)
        .and_then(|_| install_ipa(ipa_path))
}
