use serde::{Deserialize, Serialize};
use tauri::{AppHandle, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum MdmRemovalPath {
    None,
    ProfileRemove,
    JailbreakRequired,
    DfuRequired,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct MdmState {
    pub enrolled: bool,
    pub org_name: Option<String>,
    pub server_url: Option<String>,
    pub restrictions: Vec<String>,
    pub removal_path: MdmRemovalPath,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ConfigProfile {
    pub id: String,
    pub name: String,
}

fn python_path(app: &AppHandle) -> std::path::PathBuf {
    app.path().resource_dir().unwrap().join("python")
}

#[tauri::command]
pub async fn ios_mdm_state(app: AppHandle, udid: String) -> Result<MdmState, String> {
    println!("[COMMAND] ios_mdm_state udid={}", udid);

    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "mdm-state",
            &udid,
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    let json_str = String::from_utf8_lossy(&output.stdout);
    let val: serde_json::Value = serde_json::from_str(&json_str).map_err(|e| e.to_string())?;

    let enrolled = val["enrolled"].as_bool().unwrap_or(false);

    // Parse restrictions from JSON array if present
    let restrictions = val["restrictions"]
        .as_array()
        .map(|arr| {
            arr.iter()
                .filter_map(|v| v.as_str().map(|s| s.to_string()))
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();

    Ok(MdmState {
        enrolled,
        org_name: val["org_name"].as_str().map(|s| s.to_string()),
        server_url: val["server_url"].as_str().map(|s| s.to_string()),
        restrictions,
        removal_path: if enrolled {
            MdmRemovalPath::ProfileRemove
        } else {
            MdmRemovalPath::None
        },
    })
}

#[tauri::command]
pub async fn ios_list_profiles(app: AppHandle, udid: String) -> Result<Vec<ConfigProfile>, String> {
    println!("[COMMAND] ios_list_profiles udid={}", udid);

    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "list-profiles",
            &udid,
        ])
        .output()
        .await
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }

    // Parse JSON array of profile objects from Python CLI output
    let json_str = String::from_utf8_lossy(&output.stdout);
    let val: serde_json::Value =
        serde_json::from_str(json_str.trim()).map_err(|e| format!("Profile parse error: {e}"))?;

    let profiles = val
        .as_array()
        .map(|arr| {
            arr.iter()
                .filter_map(|item| {
                    Some(ConfigProfile {
                        id: item["id"].as_str()?.to_string(),
                        name: item["name"].as_str().unwrap_or("Unknown").to_string(),
                    })
                })
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();

    Ok(profiles)
}

#[tauri::command]
pub async fn ios_remove_mdm(app: AppHandle, udid: String) -> Result<bool, String> {
    println!("[COMMAND] ios_remove_mdm udid={}", udid);

    let output = app
        .shell()
        .command("python3")
        .args([
            python_path(&app)
                .join("ios_backup/cli.py")
                .to_str()
                .unwrap(),
            "remove-mdm",
            &udid,
        ])
        .output()
        .await
        .map_err(|e| format!("Failed to run MDM removal: {e}"))?;

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr).to_string();
        return Err(format!("MDM removal failed: {stderr}"));
    }

    let stdout = String::from_utf8_lossy(&output.stdout);
    let val: serde_json::Value =
        serde_json::from_str(stdout.trim()).unwrap_or(serde_json::json!({"success": false}));

    Ok(val["success"].as_bool().unwrap_or(false))
}
