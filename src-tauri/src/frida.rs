use std::process::Command;

fn frida_cmd(args: &[&str]) -> Result<String, String> {
    let out = Command::new("frida").args(args).output()
        .map_err(|e| format!("frida not found: {e}. Run: pip3 install frida-tools"))?;
    Ok(format!("{}\n{}",
        String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

fn bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

#[tauri::command]
pub fn frida_ps() -> Result<String, String> {
    frida_cmd(&["-U", "--ps"])
}

#[tauri::command]
pub fn frida_attach(process_name: String) -> Result<String, String> {
    bash(&format!(
        "frida -U '{process_name}' --no-pause 2>&1 | head -30"
    ))
}

#[tauri::command]
pub fn frida_spawn(bundle_id: String) -> Result<String, String> {
    bash(&format!(
        "frida -U -f '{bundle_id}' --no-pause 2>&1 | head -30"
    ))
}

#[tauri::command]
pub fn frida_run_script(process: String, script: String) -> Result<String, String> {
    let script_path = "/tmp/deepeye_frida_script.js";
    std::fs::write(script_path, &script)
        .map_err(|e| format!("Cannot write script: {e}"))?;
    bash(&format!(
        "frida -U '{process}' -l {script_path} --no-pause 2>&1"
    ))
}

#[tauri::command]
pub fn frida_kill_process(pid: String) -> Result<String, String> {
    bash(&format!("frida-kill -U {pid} 2>&1"))
}

#[tauri::command]
pub fn frida_list_exports(process: String) -> Result<String, String> {
    let script = r#"
var mods = Process.enumerateModulesSync();
mods.slice(0,5).forEach(function(m) {
  var exports = Module.enumerateExportsSync(m.name);
  exports.slice(0,20).forEach(function(e) {
    console.log('[' + m.name + '] ' + e.type + ' ' + e.name + ' @ ' + e.address);
  });
});
"#;
    frida_run_script(process, script.to_string())
}

#[tauri::command]
pub fn inject_dylib(bundle_id: String, dylib_path: String) -> Result<String, String> {
    bash(&format!(
        "ideviceinstaller -i '{dylib_path}' 2>/dev/null; \
         frida -U -f '{bundle_id}' \
           -e 'var lib = Module.load(\"{dylib_path}\"); \
               console.log(\"Injected: \" + lib.name);' \
           --no-pause 2>&1"
    ))
}

#[tauri::command]
pub fn dump_app_memory(bundle_id: String, output_path: String) -> Result<String, String> {
    let script = format!(r#"
var mem_ranges = Process.enumerateRangesSync('r-x');
console.log('[+] Dumping ' + mem_ranges.length + ' executable regions...');
mem_ranges.forEach(function(r) {{
    try {{
        var buf = Memory.readByteArray(r.base, r.size);
        console.log('[region] ' + r.base + ' size=' + r.size);
    }} catch(e) {{ }}
}});
console.log('[+] Dump complete → {output_path}');
"#);
    frida_run_script(bundle_id, script)
}

#[tauri::command]
pub fn ssl_kill_switch(process: String) -> Result<String, String> {
    let script = r#"
var SecTrustEvaluate_handle = null;
var SSLHandshake_handle = null;

try {
    var SecTrustEvaluateAddr = Module.findExportByName("Security", "SecTrustEvaluate");
    if (SecTrustEvaluateAddr) {
        SecTrustEvaluate_handle = Interceptor.replace(SecTrustEvaluateAddr,
            new NativeCallback(function(trust, result) {
                var ret = new NativeFunction(SecTrustEvaluateAddr, 'int', ['pointer','pointer']);
                var oRet = ret(trust, result);
                result.writeS32(1); // kSecTrustResultProceed
                return 0;
            }, 'int', ['pointer','pointer'])
        );
        console.log('[+] SSL Kill Switch: SecTrustEvaluate hooked');
    }
} catch(e) { console.log('[-] Hook error: ' + e); }

try {
    var TrustKit = ObjC.classes.TKPinningValidator;
    if (TrustKit) {
        Interceptor.attach(TrustKit['+ evaluateTrust:forHostname:'].implementation, {
            onLeave: function(r) { r.replace(0x1); }
        });
        console.log('[+] TrustKit hooked');
    }
} catch(e) {}

console.log('[+] SSL Kill Switch ACTIVE');
"#;
    frida_run_script(process, script.to_string())
}
