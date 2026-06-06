use super::super::types::{LicenseFeatureSet, LicenseType};
use super::super::validator::{sanitize_key, validate_key_format, validate_license};
use crate::session::preflight::check_license_for_operation;
use crate::session::types::{OperationType, PreflightCheck};

#[test]
fn licenseFeatureSet_free_blocks_jailbreak() {
    let features = LicenseFeatureSet::for_type(&LicenseType::Free);
    assert_eq!(features.can_use_jailbreak_tools, false);
    assert_eq!(features.can_use_boot_files, false);
    assert_eq!(features.can_use_fmi_off, false);
}

#[test]
fn licenseFeatureSet_pro_allows_all() {
    let features = LicenseFeatureSet::for_type(&LicenseType::Pro);
    assert_eq!(features.can_use_jailbreak_tools, true);
    assert_eq!(features.can_use_boot_files, true);
    assert_eq!(features.can_use_fmi_off, true);
    assert_eq!(features.can_export_logs, true);
    assert_eq!(features.can_use_edl_pipeline, true);
    assert_eq!(features.can_use_mtk_brom, true);
}

#[test]
fn activateLicense_rejects_invalid_format() {
    let key = "short";
    let res = validate_key_format(key);
    assert!(res.is_err());
    assert_eq!(res.unwrap_err(), "Invalid license key format");
}

#[test]
fn sanitize_license_key_trims_whitespace() {
    let key = "  ABC-123-DEF  ";
    let sanitized = sanitize_key(key);
    assert_eq!(sanitized, "ABC-123-DEF");
}

#[test]
fn check_license_for_operation_blocks_jailbreak_on_free() {
    use crate::license::types::LicenseStatus;
    use chrono::Utc;

    let free_license = LicenseStatus {
        license_type: LicenseType::Free,
        is_valid: true,
        expires_at: None,
        days_remaining: None,
        seat_id: None,
        activated_at: None,
        last_validated_at: Utc::now().to_rfc3339(),
        features: LicenseFeatureSet::for_type(&LicenseType::Free),
    };

    let check = check_license_for_operation(&OperationType::JailbreakPalera1n, &free_license);
    assert_eq!(check.passed, false);
    assert_eq!(check.message, "This feature requires a Pro license");
}

#[test]
fn licenseStatus_never_contains_key_field() {
    // This is essentially a compile-time check in Rust since LicenseStatus struct
    // doesn't have a 'key' field, but we can verify our validate_license returns correctly.
    let status = validate_license("PRO-1111-2222-3333-4444").unwrap();
    assert_eq!(status.license_type, LicenseType::Pro);
    assert_eq!(status.is_valid, true);
    // Key is consumed and dropped, only Status is returned.
}

#[test]
fn settings_defaults_applied_when_no_stored_settings() {
    use crate::config::settings::AppSettings;
    let default_settings = AppSettings::default();
    assert_eq!(default_settings.theme, "dark");
    assert_eq!(default_settings.language, "en");
    assert_eq!(default_settings.log_level, "info");
    assert_eq!(default_settings.auto_detect_device, true);
    assert_eq!(default_settings.confirm_dangerous_actions, true);
    assert_eq!(default_settings.show_risk_badges, true);
    assert_eq!(default_settings.auto_check_updates, true);
    assert_eq!(default_settings.send_anonymous_diagnostics, false);
    assert_eq!(default_settings.export_path, None);
}
