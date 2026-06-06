use crate::device::coordinator::DeviceProbeCoordinator;
use crate::license::manager::LicenseManager;
use crate::session::broadcaster::SessionBroadcaster;
use crate::session::executor::SessionExecutor;
use crate::session::preflight::PreflightEngine;
use crate::session::{
    OperationSession, OperationType, ProgressStep, SessionManager, SessionStatus,
};
use tauri::{AppHandle, State};
use uuid::Uuid;

fn now_iso() -> String {
    let now = std::time::SystemTime::now();
    let datetime: chrono::DateTime<chrono::Utc> = now.into();
    datetime.to_rfc3339()
}

fn get_default_steps(op_type: &OperationType) -> Vec<ProgressStep> {
    let now = now_iso();
    match op_type {
        OperationType::HelloActivation | OperationType::HelloNoSignalActivation => vec![
            ProgressStep {
                id: "preflight".into(),
                index: 0,
                label: "Preflight Validation".into(),
                detail: Some("Verifying device state and certificates".into()),
                status: "pending".into(),
                duration_ms: None,
                emitted_at: now.clone(),
            },
            ProgressStep {
                id: "exploit".into(),
                index: 1,
                label: "Triggering Activation Bypass".into(),
                detail: Some("Applying activation records via server".into()),
                status: "pending".into(),
                duration_ms: None,
                emitted_at: now.clone(),
            },
            ProgressStep {
                id: "verify".into(),
                index: 2,
                label: "Verifying Activation Status".into(),
                detail: Some("Querying lock status from device".into()),
                status: "pending".into(),
                duration_ms: None,
                emitted_at: now.clone(),
            },
        ],
        OperationType::DfuAssist => vec![
            ProgressStep {
                id: "preflight".into(),
                index: 0,
                label: "Preflight Check".into(),
                detail: Some("Checking iOS device status".into()),
                status: "pending".into(),
                duration_ms: None,
                emitted_at: now.clone(),
            },
            ProgressStep {
                id: "execute".into(),
                index: 1,
                label: "DFU Entry Sequence".into(),
                detail: Some("Putting device into recovery/DFU".into()),
                status: "pending".into(),
                duration_ms: None,
                emitted_at: now.clone(),
            },
        ],
        _ => vec![ProgressStep {
            id: "execute".into(),
            index: 0,
            label: "Executing Action".into(),
            detail: Some("Running request sequence".into()),
            status: "pending".into(),
            duration_ms: None,
            emitted_at: now.clone(),
        }],
    }
}

#[tauri::command]
pub async fn start_session(
    app: AppHandle,
    operation_type: OperationType,
    session_mgr: State<'_, SessionManager>,
    device_mgr: State<'_, DeviceProbeCoordinator>,
    license_mgr: State<'_, LicenseManager>,
) -> Result<OperationSession, String> {
    // 1. Double check no active session
    {
        let lock = session_mgr.active.lock().unwrap();
        if lock.is_some() {
            return Err("SESSION_ALREADY_ACTIVE".into());
        }
    }

    // 2. Fetch device snapshot
    let snapshot = match device_mgr.get_snapshot() {
        Some(s) => s,
        None => return Err("No device connected".into()),
    };

    // 3. Preflight
    let license_status = license_mgr.get_status();
    let preflight_result = PreflightEngine::run_checks(&snapshot, &operation_type, &license_status);
    SessionBroadcaster::emit_preflight(&app, &preflight_result);

    if !preflight_result.passed {
        return Err("PREFLIGHT_FAILED".into());
    }

    // 4. Create Session
    let session = OperationSession {
        session_id: Uuid::new_v4().to_string(),
        operation_type: operation_type.clone(),
        device_snapshot_at_start: snapshot,
        status: SessionStatus::Running,
        steps: get_default_steps(&operation_type),
        current_step_index: 0,
        logs: Vec::new(),
        preflight: Some(preflight_result),
        started_at: now_iso(),
        updated_at: now_iso(),
        completed_at: None,
        outcome: None,
        result_payload: None,
        error_code: None,
        error_message: None,
        retry_count: 0,
        can_retry: false,
        can_cancel: true,
    };

    {
        let mut lock = session_mgr.active.lock().unwrap();
        *lock = Some(session.clone());
    }

    SessionBroadcaster::emit_status_changed(&app, Some(&session));
    SessionBroadcaster::emit_started(&app, &session);

    // 5. Spawn background executor
    let app_clone = app.clone();
    tauri::async_runtime::spawn(async move {
        SessionExecutor::execute_pipeline(app_clone, operation_type).await;
    });

    Ok(session)
}

#[tauri::command]
pub fn get_active_session(session_mgr: State<'_, SessionManager>) -> Option<OperationSession> {
    session_mgr.active.lock().unwrap().clone()
}

#[tauri::command]
pub async fn cancel_session(
    app: AppHandle,
    session_mgr: State<'_, SessionManager>,
) -> Result<(), String> {
    let mut lock = session_mgr.active.lock().unwrap();
    if let Some(ref mut session) = *lock {
        if !session.can_cancel {
            return Err("Session cannot be cancelled".into());
        }
        session.status = SessionStatus::Cancelling;
        session.updated_at = now_iso();

        let cloned = session.clone();
        SessionBroadcaster::emit_status_changed(&app, Some(&cloned));

        Ok(())
    } else {
        Err("No active session to cancel".into())
    }
}

#[tauri::command]
pub fn clear_active_session(
    app: AppHandle,
    session_mgr: State<'_, SessionManager>,
) -> Result<(), String> {
    let mut lock = session_mgr.active.lock().unwrap();
    *lock = None;
    SessionBroadcaster::emit_status_changed(&app, None);
    Ok(())
}

#[tauri::command]
pub fn get_session_history(
    limit: usize,
    session_mgr: State<'_, SessionManager>,
) -> Vec<OperationSession> {
    let history = session_mgr.history.lock().unwrap();
    history.iter().rev().take(limit).cloned().collect()
}

#[tauri::command]
pub fn clear_session_history(session_mgr: State<'_, SessionManager>) -> Result<(), String> {
    let mut history = session_mgr.history.lock().map_err(|e| e.to_string())?;
    history.clear();
    Ok(())
}
