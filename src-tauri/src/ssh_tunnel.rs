use std::process::{Command, Stdio};
use std::sync::Mutex;

static TUNNEL_PID: Mutex<Option<u32>> = Mutex::new(None);

fn bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash")
        .arg("-c")
        .arg(s)
        .output()
        .map_err(|e| e.to_string())?;
    Ok(format!(
        "{}\n{}",
        String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)
    ))
}

#[tauri::command]
pub fn start_ssh_tunnel() -> Result<String, String> {
    let child = Command::new("iproxy")
        .args(["2222", "22"])
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|e| format!("iproxy not found: {e}. Run: brew install libusbmuxd"))?;

    let pid = child.id();
    *TUNNEL_PID.lock().unwrap() = Some(pid);

    Ok(format!(
        "✅ SSH Tunnel started\n\
        PID: {pid}\n\
        Connect: ssh root@localhost -p 2222\n\
        Default pass: alpine"
    ))
}

#[tauri::command]
pub fn stop_ssh_tunnel() -> Result<String, String> {
    let mut pid_lock = TUNNEL_PID.lock().unwrap();
    if let Some(pid) = *pid_lock {
        let _ = bash(&format!("kill {pid} 2>&1"));
        *pid_lock = None;
        Ok(format!("✅ SSH tunnel stopped (PID {pid})"))
    } else {
        bash("pkill iproxy 2>&1").map(|_| "✅ iproxy instances killed".to_string())
    }
}

#[tauri::command]
pub fn check_tunnel_status() -> Result<String, String> {
    bash(
        "if pgrep iproxy > /dev/null 2>&1; then \
           echo '🟢 Tunnel ACTIVE'; \
           echo 'Port: localhost:2222'; \
           nc -z localhost 2222 2>&1 && echo 'Socket: OPEN' || echo 'Socket: pending...'; \
         else \
           echo '🔴 Tunnel NOT running. Start it first.'; \
         fi",
    )
}

#[tauri::command]
pub fn run_ssh_command(cmd: String) -> Result<String, String> {
    bash(&format!(
        "ssh -o StrictHostKeyChecking=no \
             -o ConnectTimeout=5 \
             -p 2222 root@localhost \
             '{cmd}' 2>&1"
    ))
}

#[tauri::command]
pub fn run_su_command(cmd: String) -> Result<String, String> {
    bash(&format!(
        "ssh -o StrictHostKeyChecking=no -p 2222 root@localhost \
         'su root -c \"{cmd}\"' 2>&1"
    ))
}

#[tauri::command]
pub fn ssh_upload_file(local_path: String, remote_path: String) -> Result<String, String> {
    bash(&format!(
        "scp -o StrictHostKeyChecking=no -P 2222 \
         '{local_path}' root@localhost:'{remote_path}' 2>&1 && \
         echo '✅ Uploaded to {remote_path}'"
    ))
}

#[tauri::command]
pub fn ssh_download_file(remote_path: String, local_path: String) -> Result<String, String> {
    bash(&format!(
        "scp -o StrictHostKeyChecking=no -P 2222 \
         root@localhost:'{remote_path}' '{local_path}' 2>&1 && \
         echo '✅ Downloaded to {local_path}'"
    ))
}

#[tauri::command]
pub fn install_sileo_pkg(package_name: String) -> Result<String, String> {
    bash(&format!(
        "ssh -o StrictHostKeyChecking=no -p 2222 root@localhost \
         'apt-get install -y \"{package_name}\" 2>&1 || \
          dpkg -i \"{package_name}\" 2>&1' 2>&1"
    ))
}
