//! Error handling infrastructure for DeepEyeUnlocker
//! Provides structured error types, retry logic, and user-friendly messages.

#![allow(dead_code)]
use serde::Serialize;
use std::time::Duration;
use tokio::time::sleep;

/// Error categories for user-friendly messaging
#[derive(Debug, Clone, Serialize)]
pub enum ErrorCategory {
    /// Device not connected or not detected
    DeviceNotFound,
    /// USB permission denied
    PermissionDenied,
    /// Operation timed out
    Timeout,
    /// Protocol error (BROM, EDL, etc.)
    ProtocolError,
    /// File not found or inaccessible
    FileNotFound,
    /// Invalid input parameters
    InvalidInput,
    /// Tool not found (adb, fastboot, edl, etc.)
    ToolNotFound,
    /// Unknown/unexpected error
    Unknown,
}

impl std::fmt::Display for ErrorCategory {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ErrorCategory::DeviceNotFound => write!(f, "Device Not Found"),
            ErrorCategory::PermissionDenied => write!(f, "Permission Denied"),
            ErrorCategory::Timeout => write!(f, "Operation Timeout"),
            ErrorCategory::ProtocolError => write!(f, "Protocol Error"),
            ErrorCategory::FileNotFound => write!(f, "File Not Found"),
            ErrorCategory::InvalidInput => write!(f, "Invalid Input"),
            ErrorCategory::ToolNotFound => write!(f, "Tool Not Found"),
            ErrorCategory::Unknown => write!(f, "Unknown Error"),
        }
    }
}

/// Structured error with category, message, and recovery hint
#[derive(Debug, Clone, Serialize)]
pub struct DeviceError {
    pub category: ErrorCategory,
    pub message: String,
    pub hint: String,
    pub recoverable: bool,
}

impl DeviceError {
    pub fn new(
        category: ErrorCategory,
        message: impl Into<String>,
        hint: impl Into<String>,
    ) -> Self {
        Self {
            category: category.clone(),
            message: message.into(),
            hint: hint.into(),
            recoverable: matches!(
                category,
                ErrorCategory::DeviceNotFound
                    | ErrorCategory::PermissionDenied
                    | ErrorCategory::Timeout
            ),
        }
    }

    pub fn device_not_found(msg: impl Into<String>) -> Self {
        Self::new(
            ErrorCategory::DeviceNotFound,
            msg,
            "Please connect your device via USB and ensure it's powered on.",
        )
    }

    pub fn permission_denied(msg: impl Into<String>) -> Self {
        Self::new(
            ErrorCategory::PermissionDenied,
            msg,
            "Enable USB debugging in Settings > Developer Options. On Linux, add udev rules.",
        )
    }

    pub fn timeout(msg: impl Into<String>) -> Self {
        Self::new(
            ErrorCategory::Timeout,
            msg,
            "Device not responding. Check cable connection and retry in 10 seconds.",
        )
    }

    pub fn protocol_error(msg: impl Into<String>) -> Self {
        Self::new(
            ErrorCategory::ProtocolError,
            msg,
            "Protocol handshake failed. Try re-entering the required mode (BROM/EDL/Download).",
        )
    }

    pub fn file_not_found(path: impl Into<String>) -> Self {
        let path = path.into();
        Self::new(
            ErrorCategory::FileNotFound,
            format!("File not found: {}", path),
            "Verify the file path and ensure the file exists.",
        )
    }

    pub fn invalid_input(msg: impl Into<String>) -> Self {
        Self::new(
            ErrorCategory::InvalidInput,
            msg,
            "Check your input and try again.",
        )
    }

    pub fn tool_not_found(tool: impl Into<String>) -> Self {
        let tool = tool.into();
        Self::new(
            ErrorCategory::ToolNotFound,
            format!("Required tool '{}' not found", tool),
            format!(
                "Install '{}' and ensure it's in your PATH. Check the setup guide.",
                tool
            ),
        )
    }

    pub fn unknown(msg: impl Into<String>) -> Self {
        Self::new(
            ErrorCategory::Unknown,
            msg,
            "An unexpected error occurred. Check logs for details.",
        )
    }
}

impl std::fmt::Display for DeviceError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "[{}] {}: {}", self.category, self.category, self.message)
    }
}

impl std::error::Error for DeviceError {}

/// Convert from std::io::Error to DeviceError
impl From<std::io::Error> for DeviceError {
    fn from(e: std::io::Error) -> Self {
        match e.kind() {
            std::io::ErrorKind::NotFound => Self::tool_not_found("command"),
            std::io::ErrorKind::PermissionDenied => Self::permission_denied(e.to_string()),
            std::io::ErrorKind::TimedOut => Self::timeout(e.to_string()),
            _ => Self::unknown(e.to_string()),
        }
    }
}

/// Convert from String to DeviceError
impl From<String> for DeviceError {
    fn from(s: String) -> Self {
        let lower = s.to_lowercase();
        if lower.contains("permission") || lower.contains("denied") {
            Self::permission_denied(&s)
        } else if lower.contains("not found") || lower.contains("no device") {
            Self::device_not_found(&s)
        } else if lower.contains("timeout") || lower.contains("timed out") {
            Self::timeout(&s)
        } else {
            Self::unknown(&s)
        }
    }
}

/// Convert DeviceError to String for Tauri command results
impl From<DeviceError> for String {
    fn from(e: DeviceError) -> Self {
        format!("{}: {}\n💡 {}", e.category, e.message, e.hint)
    }
}

/// Retry configuration
#[derive(Debug, Clone)]
pub struct RetryConfig {
    pub max_attempts: u32,
    pub initial_delay_ms: u64,
    pub max_delay_ms: u64,
    pub backoff_multiplier: f64,
}

impl Default for RetryConfig {
    fn default() -> Self {
        Self {
            max_attempts: 3,
            initial_delay_ms: 1000,
            max_delay_ms: 10000,
            backoff_multiplier: 2.0,
        }
    }
}

impl RetryConfig {
    pub fn quick() -> Self {
        Self {
            max_attempts: 2,
            initial_delay_ms: 500,
            max_delay_ms: 2000,
            backoff_multiplier: 1.5,
        }
    }

    pub fn aggressive() -> Self {
        Self {
            max_attempts: 5,
            initial_delay_ms: 2000,
            max_delay_ms: 15000,
            backoff_multiplier: 2.0,
        }
    }

    pub fn no_retry() -> Self {
        Self {
            max_attempts: 1,
            initial_delay_ms: 0,
            max_delay_ms: 0,
            backoff_multiplier: 1.0,
        }
    }
}

/// Execute a fallible async operation with retry logic
pub async fn with_retry<T, F, Fut, E>(
    config: &RetryConfig,
    operation: F,
    is_retryable: impl Fn(&E) -> bool,
) -> Result<T, E>
where
    F: Fn(u32) -> Fut,
    Fut: std::future::Future<Output = Result<T, E>>,
    E: std::fmt::Display,
{
    let mut last_error = None;

    for attempt in 1..=config.max_attempts {
        match operation(attempt).await {
            Ok(result) => return Ok(result),
            Err(e) => {
                let retryable = is_retryable(&e);
                last_error = Some(e);

                if attempt < config.max_attempts && retryable {
                    let delay_ms = (config.initial_delay_ms as f64
                        * config.backoff_multiplier.powi((attempt - 1) as i32))
                    .min(config.max_delay_ms as f64) as u64;

                    sleep(Duration::from_millis(delay_ms)).await;
                }
            }
        }
    }

    Err(last_error.unwrap())
}

/// Check if an error string indicates a retryable condition
pub fn is_retryable_error(msg: &str) -> bool {
    let lower = msg.to_lowercase();
    lower.contains("timeout")
        || lower.contains("timed out")
        || lower.contains("device not found")
        || lower.contains("no device")
        || lower.contains("connection reset")
        || lower.contains("broken pipe")
        || lower.contains("resource busy")
        || lower.contains("try again")
}

/// Progress event for emitting to frontend
#[derive(Debug, Clone, Serialize)]
pub struct ProgressEvent {
    pub operation: String,
    pub step: String,
    pub percent: f32,
    pub message: String,
    pub attempt: Option<u32>,
    pub max_attempts: Option<u32>,
}

impl ProgressEvent {
    pub fn new(operation: impl Into<String>, step: impl Into<String>, percent: f32) -> Self {
        Self {
            operation: operation.into(),
            step: step.into(),
            percent,
            message: String::new(),
            attempt: None,
            max_attempts: None,
        }
    }

    pub fn with_message(mut self, msg: impl Into<String>) -> Self {
        self.message = msg.into();
        self
    }

    pub fn with_attempt(mut self, attempt: u32, max: u32) -> Self {
        self.attempt = Some(attempt);
        self.max_attempts = Some(max);
        self
    }
}

/// Operation status for tracking
#[derive(Debug, Clone, Serialize)]
pub struct OperationStatus {
    pub success: bool,
    pub message: String,
    pub duration_ms: u64,
    pub error: Option<DeviceError>,
}

impl OperationStatus {
    pub fn success(message: impl Into<String>, duration_ms: u64) -> Self {
        Self {
            success: true,
            message: message.into(),
            duration_ms,
            error: None,
        }
    }

    pub fn failure(error: DeviceError, duration_ms: u64) -> Self {
        Self {
            success: false,
            message: error.message.clone(),
            duration_ms,
            error: Some(error),
        }
    }
}
