use chrono::Utc;
use serde::{Deserialize, Serialize};
use std::cmp::Ordering;
use tauri::AppHandle;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateInfo {
    pub current_version: String,
    pub latest_version: Option<String>,
    pub update_available: bool,
    pub release_url: Option<String>,
    pub release_notes: Option<String>,
    pub checked_at: String,
}

#[derive(Deserialize)]
struct GithubRelease {
    tag_name: String,
    html_url: String,
    body: String,
}

// Basic semver compare (x.y.z)
fn semver_gt(v1: &str, v2: &str) -> bool {
    let parse = |s: &str| -> Vec<u32> {
        s.trim_start_matches('v')
            .split('.')
            .filter_map(|p| p.parse().ok())
            .collect()
    };
    let p1 = parse(v1);
    let p2 = parse(v2);

    for (a, b) in p1.iter().zip(p2.iter()) {
        match a.cmp(b) {
            Ordering::Greater => return true,
            Ordering::Less => return false,
            Ordering::Equal => continue,
        }
    }
    p1.len() > p2.len()
}

#[tauri::command]
pub async fn check_for_updates(_app: AppHandle) -> Result<UpdateInfo, String> {
    let current = env!("CARGO_PKG_VERSION").to_string();
    let checked_at = Utc::now().to_rfc3339();

    let client = reqwest::Client::builder()
        .user_agent("DeepEyeUnlocker/1.0")
        .build()
        .map_err(|e| e.to_string())?;

    // Attempt to fetch from GitHub API
    let res = client
        .get("https://api.github.com/repos/DeepEyeCrypto/DeepEyeUnlocker/releases/latest")
        .send()
        .await;

    match res {
        Ok(response) if response.status().is_success() => {
            if let Ok(release) = response.json::<GithubRelease>().await {
                let latest = release.tag_name;
                let update_available = semver_gt(&latest, &current);

                Ok(UpdateInfo {
                    current_version: current,
                    latest_version: Some(latest),
                    update_available,
                    release_url: Some(release.html_url),
                    release_notes: Some(release.body),
                    checked_at,
                })
            } else {
                Ok(UpdateInfo {
                    current_version: current,
                    latest_version: None,
                    update_available: false,
                    release_url: None,
                    release_notes: None,
                    checked_at,
                })
            }
        }
        _ => {
            // Graceful degradation
            Ok(UpdateInfo {
                current_version: current,
                latest_version: None,
                update_available: false,
                release_url: None,
                release_notes: None,
                checked_at,
            })
        }
    }
}
