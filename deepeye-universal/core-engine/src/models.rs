use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq, Eq)]
pub enum DevicePlatform {
    MTK,
    Qualcomm,
    UniSoc,
    Samsung,
    Unknown,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct FeatureExecutionRequest {
    pub feature_id: u32,
    pub title: String,
    pub platform: DevicePlatform,
    pub options: Option<serde_json::Value>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct FeatureExecutionResponse {
    pub success: bool,
    pub message: String,
    pub log_output: Vec<String>,
}
