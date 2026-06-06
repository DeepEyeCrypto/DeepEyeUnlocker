use super::types::{LicenseFeatureSet, LicenseStatus, LicenseType};
use chrono::Utc;
use regex::Regex;
use std::sync::OnceLock;

static KEY_REGEX: OnceLock<Regex> = OnceLock::new();

pub fn sanitize_key(key: &str) -> String {
    key.trim().to_uppercase()
}

pub fn validate_key_format(key: &str) -> Result<(), String> {
    let re = KEY_REGEX.get_or_init(|| Regex::new(r"^[A-Z0-9\-]{20,40}$").unwrap());
    if re.is_match(key) {
        Ok(())
    } else {
        Err("Invalid license key format".to_string())
    }
}

pub fn validate_license(key: &str) -> Result<LicenseStatus, String> {
    let sanitized = sanitize_key(key);
    validate_key_format(&sanitized)?;

    // Here you would do actual online validation or offline HMAC check.
    // For now, we mock the validation based on prefix.
    let license_type = if sanitized.starts_with("PRO-") {
        LicenseType::Pro
    } else if sanitized.starts_with("TRIAL-") {
        LicenseType::Trial
    } else if sanitized.starts_with("EXP-") {
        LicenseType::Expired
    } else {
        return Err("License key rejected".to_string());
    };

    let now_iso = Utc::now().to_rfc3339();

    Ok(LicenseStatus {
        license_type: license_type.clone(),
        is_valid: license_type == LicenseType::Pro || license_type == LicenseType::Trial,
        expires_at: match license_type {
            LicenseType::Trial => Some("2026-12-31T00:00:00Z".to_string()),
            _ => None,
        },
        days_remaining: match license_type {
            LicenseType::Trial => Some(30),
            _ => None,
        },
        seat_id: Some("MOCK-SEAT-1234".to_string()),
        activated_at: Some(now_iso.clone()),
        last_validated_at: now_iso,
        features: LicenseFeatureSet::for_type(&license_type),
    })
}
