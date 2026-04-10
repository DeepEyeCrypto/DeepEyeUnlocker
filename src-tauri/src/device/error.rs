#![allow(dead_code)]

use serde::Serialize;
use thiserror::Error;

/// Unified device error types across all protocols
#[derive(Debug, Error, Serialize)]
pub enum DeviceError {
    #[error("Device not found: {0}")]
    DeviceNotFound(String),

    #[error("USB error: {operation} failed: {details}")]
    UsbError {
        operation: String,
        details: String,
    },

    #[error("Protocol error: {protocol} — {message}")]
    ProtocolError {
        protocol: String,
        message: String,
    },

    #[error("Handshake failed: {0}")]
    HandshakeFailed(String),

    #[error("Timeout after {timeout_ms}ms")]
    Timeout { timeout_ms: u64 },

    #[error("Authentication required: {auth_type}")]
    AuthenticationRequired { auth_type: String },

    #[error("Invalid response: expected {expected:#04x}, got {got:#04x}")]
    ResponseMismatch { expected: u8, got: u8 },

    #[error("Partition not found: {0}")]
    PartitionNotFound(String),

    #[error("Flash operation failed: {0}")]
    FlashFailed(String),

    #[error("Invalid parameters: {0}")]
    InvalidParameters(String),
}

/// Map rusb errors to unified DeviceError
pub fn map_usb_error(operation: &str, error: rusb::Error) -> DeviceError {
    match error {
        rusb::Error::Timeout => DeviceError::Timeout { timeout_ms: 5000 },
        rusb::Error::Access => DeviceError::UsbError {
            operation: operation.to_string(),
            details: "USB access denied — check permissions/entitlements".to_string(),
        },
        rusb::Error::NoDevice => DeviceError::DeviceNotFound(
            "Device disconnected during operation".to_string(),
        ),
        rusb::Error::Io => DeviceError::UsbError {
            operation: operation.to_string(),
            details: "USB I/O error — check cable connection".to_string(),
        },
        _ => DeviceError::UsbError {
            operation: operation.to_string(),
            details: error.to_string(),
        },
    }
}

/// Create protocol-specific error
pub fn protocol_error(protocol: &str, message: impl Into<String>) -> DeviceError {
    DeviceError::ProtocolError {
        protocol: protocol.to_string(),
        message: message.into(),
    }
}

/// Create handshake failure error
pub fn handshake_failed(step: &str, expected: u8, got: u8) -> DeviceError {
    DeviceError::HandshakeFailed(format!(
        "Step '{}': expected {:#04x}, got {:#04x}",
        step, expected, got
    ))
}
