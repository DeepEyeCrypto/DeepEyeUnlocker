use std::path::PathBuf;

use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

pub(crate) fn get_tool_path(app: &AppHandle, tool: &str) -> Result<PathBuf, String> {
    #[cfg(target_os = "windows")]
    let (resource_subdir, executable_name) = {
        let exe_name = if tool.ends_with(".exe") {
            tool.to_string()
        } else {
            format!("{tool}.exe")
        };
        ("windows", exe_name)
    };

    #[cfg(target_os = "linux")]
    let (resource_subdir, executable_name) = ("linux", tool.to_string());

    #[cfg(all(not(target_os = "windows"), not(target_os = "linux")))]
    let (resource_subdir, executable_name) = ("macos", tool.to_string());

    let resource_path = app
        .path()
        .resource_dir()
        .map_err(|e| format!("resource dir error: {e}"))?
        .join(resource_subdir)
        .join(executable_name);

    let resolved = if resource_path.exists() {
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            std::fs::set_permissions(&resource_path, std::fs::Permissions::from_mode(0o755))
                .map_err(|e| format!("chmod error: {e}"))?;
        }
        resource_path
    } else {
        PathBuf::from(tool)
    };

    Ok(resolved)
}

fn augmented_path_env() -> String {
    let path_env =
        std::env::var("PATH").unwrap_or_else(|_| "/usr/bin:/bin:/usr/sbin:/sbin".to_string());
    format!("/usr/local/bin:/opt/homebrew/bin:{path_env}")
}

pub(crate) async fn run_tool(
    app: &AppHandle,
    bin: &str,
    args: Vec<String>,
) -> Result<String, String> {
    let tool_path = get_tool_path(app, bin)?;
    let tool_str = tool_path
        .to_str()
        .ok_or_else(|| format!("invalid {bin} path"))?;
    let arg_refs: Vec<&str> = args.iter().map(String::as_str).collect();

    let output = app
        .shell()
        .command(tool_str)
        .env("PATH", augmented_path_env())
        .args(arg_refs)
        .output()
        .await
        .map_err(|e| format!("{bin} exec failed: {e}"))?;

    let stdout = String::from_utf8_lossy(&output.stdout).trim().to_string();
    let stderr = String::from_utf8_lossy(&output.stderr).trim().to_string();
    let combined = match (stdout.is_empty(), stderr.is_empty()) {
        (true, true) => String::new(),
        (false, true) => stdout,
        (true, false) => stderr,
        (false, false) => format!("{stdout}\n{stderr}"),
    };

    if !output.status.success() {
        return Err(if combined.is_empty() {
            format!("{bin} failed with exit code {:?}", output.status.code())
        } else {
            combined
        });
    }

    Ok(combined)
}
