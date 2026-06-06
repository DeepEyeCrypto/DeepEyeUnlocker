use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct AppSettings {
    pub theme: String,
    pub language: String,
    pub log_level: String,
    pub auto_detect_device: bool,
    pub confirm_dangerous_actions: bool,
    pub show_risk_badges: bool,
    pub auto_check_updates: bool,
    pub send_anonymous_diagnostics: bool,
    pub export_path: Option<String>,
}

impl Default for AppSettings {
    fn default() -> Self {
        Self {
            theme: "dark".to_string(),
            language: "en".to_string(),
            log_level: "info".to_string(),
            auto_detect_device: true,
            confirm_dangerous_actions: true,
            show_risk_badges: true,
            auto_check_updates: true,
            send_anonymous_diagnostics: false,
            export_path: None,
        }
    }
}
