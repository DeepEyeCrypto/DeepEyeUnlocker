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
pub async fn frida_ps(app: AppHandle) -> Result<String, String> {
    bash(&app, "frida -U --ps 2>&1").await
}

#[tauri::command]
pub async fn frida_attach(app: AppHandle, process_name: String) -> Result<String, String> {
    bash(
        &app,
        &format!("frida -U '{process_name}' --no-pause 2>&1 | head -30"),
    )
    .await
}

#[tauri::command]
pub async fn frida_spawn(app: AppHandle, bundle_id: String) -> Result<String, String> {
    bash(
        &app,
        &format!("frida -U -f '{bundle_id}' --no-pause 2>&1 | head -30"),
    )
    .await
}

#[tauri::command]
pub async fn frida_run_script(
    app: AppHandle,
    process: String,
    script: String,
) -> Result<String, String> {
    let script_path = "/tmp/deepeye_frida_script.js";
    std::fs::write(script_path, &script).map_err(|e| format!("Cannot write script: {e}"))?;
    bash(
        &app,
        &format!("frida -U '{process}' -l {script_path} --no-pause 2>&1"),
    )
    .await
}

#[tauri::command]
pub async fn frida_kill_process(app: AppHandle, pid: String) -> Result<String, String> {
    bash(&app, &format!("frida-kill -U {pid} 2>&1")).await
}

#[tauri::command]
pub async fn frida_list_exports(app: AppHandle, process: String) -> Result<String, String> {
    let script = r#"
var mods = Process.enumerateModulesSync();
mods.slice(0,5).forEach(function(m) {
  var exports = Module.enumerateExportsSync(m.name);
  exports.slice(0,20).forEach(function(e) {
    console.log('[' + m.name + '] ' + e.type + ' ' + e.name + ' @ ' + e.address);
  });
});
"#;
    frida_run_script(app, process, script.to_string()).await
}

#[tauri::command]
pub async fn inject_dylib(
    app: AppHandle,
    bundle_id: String,
    dylib_path: String,
) -> Result<String, String> {
    bash(
        &app,
        &format!(
            "ideviceinstaller -i '{dylib_path}' 2>/dev/null; \
             frida -U -f '{bundle_id}' \
               -e 'var lib = Module.load(\"{dylib_path}\"); console.log(\"Injected: \" + lib.name);' \
               --no-pause 2>&1"
        ),
    )
    .await
}

#[tauri::command]
pub async fn dump_app_memory(
    app: AppHandle,
    bundle_id: String,
    output_path: String,
) -> Result<String, String> {
    let script = format!(
        r#"
var mem_ranges = Process.enumerateRangesSync('r-x');
console.log('[+] Dumping ' + mem_ranges.length + ' executable regions...');
mem_ranges.forEach(function(r) {{
    try {{
        var _buf = Memory.readByteArray(r.base, r.size);
        console.log('[region] ' + r.base + ' size=' + r.size);
    }} catch(e) {{ }}
}});
console.log('[+] Dump complete -> {output_path}');
"#
    );
    frida_run_script(app, bundle_id, script).await
}

#[tauri::command]
pub async fn ssl_kill_switch(app: AppHandle, process: String) -> Result<String, String> {
    let script = r#"
try {
  var secTrustEvaluateAddr = Module.findExportByName('Security', 'SecTrustEvaluate');
  if (secTrustEvaluateAddr) {
    Interceptor.replace(secTrustEvaluateAddr,
      new NativeCallback(function(trust, result) {
        var ret = new NativeFunction(secTrustEvaluateAddr, 'int', ['pointer','pointer']);
        var _orig = ret(trust, result);
        result.writeS32(1);
        return 0;
      }, 'int', ['pointer','pointer'])
    );
    console.log('[+] SSL Kill Switch: SecTrustEvaluate hooked');
  }
} catch(e) { console.log('[-] Hook error: ' + e); }

try {
  var trustKit = ObjC.classes.TKPinningValidator;
  if (trustKit) {
    Interceptor.attach(trustKit['+ evaluateTrust:forHostname:'].implementation, {
      onLeave: function(r) { r.replace(0x1); }
    });
    console.log('[+] TrustKit hooked');
  }
} catch(e) {}

console.log('[+] SSL Kill Switch ACTIVE');
"#;
    frida_run_script(app, process, script.to_string()).await
}

#[tauri::command]
pub async fn frida_inject(
    app: AppHandle,
    process: String,
    script_content: String,
) -> Result<String, String> {
    // Use existing frida_run_script logic for consistency
    frida_run_script(app, process, script_content).await
}
