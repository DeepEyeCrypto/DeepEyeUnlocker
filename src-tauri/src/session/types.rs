use crate::device::types::DeviceSnapshot;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub enum OperationType {
    HelloActivation,
    HelloNoSignalActivation,
    HelloWifiActivation,
    HelloGsmActivation,
    PasscodeActivation,
    FmiOff,
    DfuAssist,
    RecoveryEnter,
    RecoveryExit,
    DfuExit,
    BootFilesActivation,
    BootFilesBackup,
    PurpleModeEntry,
    PurpleModeRestore,
    JailbreakPalera1n,
    JailbreakCheckra1n,
    OtaBlock,
    RestoreBlock,
    Reboot,
    RebootToHello,
    DeviceCheck,
    EdlBypass,
    MtkBrom,
    CustomCommand(String),
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum SessionStatus {
    Idle,
    PreflightPending,
    PreflightFailed,
    Starting,
    Running,
    Paused,
    Cancelling,
    Cancelled,
    Completing,
    Completed,
    Failed,
    Retrying,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProgressStep {
    pub id: String,
    pub index: usize,
    pub label: String,
    pub detail: Option<String>,
    pub status: String, // pending, running, done, skipped, failed
    pub duration_ms: Option<u64>,
    pub emitted_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLog {
    pub session_id: String,
    pub level: String, // debug, info, warn, error
    pub message: String,
    pub context: Option<serde_json::Value>,
    pub timestamp: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct PreflightCheck {
    pub name: String,
    pub required: bool,
    pub passed: bool,
    pub message: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct PreflightResult {
    pub passed: bool,
    pub checks: Vec<PreflightCheck>,
    pub blocking_issues: Vec<String>,
    pub warnings: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OperationSession {
    pub session_id: String,
    pub operation_type: OperationType,
    pub device_snapshot_at_start: DeviceSnapshot,
    pub status: SessionStatus,
    pub steps: Vec<ProgressStep>,
    pub current_step_index: usize,
    pub logs: Vec<SessionLog>,
    pub preflight: Option<PreflightResult>,
    pub started_at: String,
    pub updated_at: String,
    pub completed_at: Option<String>,
    pub outcome: Option<String>, // success, partial, failed, cancelled
    pub result_payload: Option<serde_json::Value>,
    pub error_code: Option<String>,
    pub error_message: Option<String>,
    pub retry_count: u32,
    pub can_retry: bool,
    pub can_cancel: bool,
}
