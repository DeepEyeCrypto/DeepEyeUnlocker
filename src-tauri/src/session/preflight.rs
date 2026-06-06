use crate::device::types::{DeviceConnectionState, DeviceMode, DevicePlatform, DeviceSnapshot};
use crate::license::types::LicenseStatus;
use crate::session::types::{OperationType, PreflightCheck, PreflightResult};

pub struct PreflightEngine;

impl PreflightEngine {
    pub fn run_checks(
        snapshot: &DeviceSnapshot,
        op: &OperationType,
        license: &LicenseStatus,
    ) -> PreflightResult {
        let mut checks = Vec::new();
        let mut blocking_issues = Vec::new();
        let mut warnings = Vec::new();

        // 1. connection state
        let is_connected = snapshot.connection_state == DeviceConnectionState::Connected;
        checks.push(PreflightCheck {
            name: "device_connected".into(),
            required: true,
            passed: is_connected,
            message: if is_connected {
                "Device is connected".into()
            } else {
                "Device is not connected".into()
            },
        });
        if !is_connected {
            blocking_issues.push("Device is not connected".into());
        }

        // 2. unstable check
        let is_unstable = snapshot.connection_state == DeviceConnectionState::Unstable;
        checks.push(PreflightCheck {
            name: "device_not_unstable".into(),
            required: true,
            passed: !is_unstable,
            message: if !is_unstable {
                "Connection is stable".into()
            } else {
                "Connection is unstable".into()
            },
        });
        if is_unstable {
            blocking_issues.push("Device connection is unstable".into());
        }

        // 3. risk flags
        let has_missing_driver = snapshot.risk_flags.iter().any(|f| f == "missingDriver");
        checks.push(PreflightCheck {
            name: "driver_available".into(),
            required: true,
            passed: !has_missing_driver,
            message: if !has_missing_driver {
                "Driver available".into()
            } else {
                "Missing required USB driver".into()
            },
        });
        if has_missing_driver {
            blocking_issues.push("Missing USB driver for device".into());
        }

        let has_multiple_devices = snapshot
            .risk_flags
            .iter()
            .any(|f| f == "multipleDevicesConnected");
        checks.push(PreflightCheck {
            name: "not_multiple_devices".into(),
            required: false,
            passed: !has_multiple_devices,
            message: if !has_multiple_devices {
                "Single device connected".into()
            } else {
                "Multiple devices detected".into()
            },
        });
        if has_multiple_devices {
            warnings.push(
                "Multiple devices connected. Please ensure you are operating on the correct one."
                    .into(),
            );
        }

        // 4. Platform and capabilities per op type
        match op {
            OperationType::HelloActivation
            | OperationType::HelloNoSignalActivation
            | OperationType::DfuAssist => {
                if snapshot.platform != DevicePlatform::Ios {
                    checks.push(PreflightCheck {
                        name: "platform_ios".into(),
                        required: true,
                        passed: false,
                        message: "Operation requires an iOS device".into(),
                    });
                    blocking_issues.push("Operation requires an iOS device".into());
                } else {
                    checks.push(PreflightCheck {
                        name: "platform_ios".into(),
                        required: true,
                        passed: true,
                        message: "Platform is iOS".into(),
                    });
                }
            }
            OperationType::RecoveryExit => {
                if snapshot.mode != DeviceMode::Recovery {
                    checks.push(PreflightCheck {
                        name: "mode_recovery".into(),
                        required: true,
                        passed: false,
                        message: "Device must be in Recovery mode".into(),
                    });
                    blocking_issues.push("Device must be in Recovery mode to exit Recovery".into());
                }
            }
            _ => {}
        }

        // 5. License check
        let license_check = check_license_for_operation(op, license);
        if !license_check.passed {
            blocking_issues.push(license_check.message.clone());
        }
        checks.push(license_check);

        PreflightResult {
            passed: blocking_issues.is_empty(),
            checks,
            blocking_issues,
            warnings,
        }
    }
}

pub fn check_license_for_operation(
    op_type: &OperationType,
    license: &LicenseStatus,
) -> PreflightCheck {
    let required = match op_type {
        OperationType::JailbreakPalera1n | OperationType::JailbreakCheckra1n => {
            license.features.can_use_jailbreak_tools
        }
        OperationType::BootFilesActivation | OperationType::BootFilesBackup => {
            license.features.can_use_boot_files
        }
        OperationType::FmiOff => license.features.can_use_fmi_off,
        OperationType::EdlBypass => license.features.can_use_edl_pipeline,
        OperationType::MtkBrom => license.features.can_use_mtk_brom,
        _ => true,
    };
    PreflightCheck {
        name: "license_feature_allowed".into(),
        required: true,
        passed: required,
        message: if required {
            "Feature allowed by license".into()
        } else {
            "This feature requires a Pro license".into()
        },
    }
}
