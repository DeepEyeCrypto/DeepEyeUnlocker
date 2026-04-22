use serde::Deserialize;

/// GitHub release response (minimal fields)
#[derive(Deserialize)]
struct GitHubRelease {
    tag_name: String,
    html_url: String,
    published_at: Option<String>,
}

/// Current embedded DB version — bump when device_db JSON ships with a new build.
const LOCAL_DB_VERSION: &str = "v2027.13.1";

#[tauri::command]
pub async fn cloud_sync_db() -> Result<String, String> {
    let url = "https://api.github.com/repos/DeepEyeCrypto/DeepEyeUnlocker/releases/latest";

    let client = reqwest::Client::builder()
        .user_agent("DeepEyeUnlocker/2027")
        .timeout(std::time::Duration::from_secs(10))
        .build()
        .map_err(|e| format!("HTTP client error: {e}"))?;

    let resp = client
        .get(url)
        .send()
        .await
        .map_err(|e| format!("Network error: {e}"))?;

    if !resp.status().is_success() {
        return Err(format!(
            "GitHub API returned HTTP {}",
            resp.status().as_u16()
        ));
    }

    let release: GitHubRelease = resp
        .json()
        .await
        .map_err(|e| format!("JSON parse error: {e}"))?;

    let remote_tag = release.tag_name.trim_start_matches('v');
    let local_tag = LOCAL_DB_VERSION.trim_start_matches('v');

    if remote_tag == local_tag {
        Ok(format!(
            "Database is already up to date ({LOCAL_DB_VERSION})"
        ))
    } else {
        Ok(format!(
            "Update available: {} → {} — {}",
            LOCAL_DB_VERSION,
            release.tag_name,
            release.html_url
        ))
    }
}
