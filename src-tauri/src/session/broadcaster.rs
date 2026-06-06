use crate::session::types::{OperationSession, PreflightResult, ProgressStep, SessionLog};
use tauri::{AppHandle, Emitter};

pub struct SessionBroadcaster;

impl SessionBroadcaster {
    pub fn emit_status_changed(app: &AppHandle, session: Option<&OperationSession>) {
        let _ = app.emit("session://status-changed", session);
    }

    pub fn emit_preflight(app: &AppHandle, result: &PreflightResult) {
        let _ = app.emit("session://preflight", result);
    }

    pub fn emit_started(app: &AppHandle, session: &OperationSession) {
        let _ = app.emit("session://started", session);
    }

    pub fn emit_step_update(app: &AppHandle, step: &ProgressStep) {
        let _ = app.emit("session://step-update", step);
    }

    pub fn emit_log(app: &AppHandle, log: &SessionLog) {
        let _ = app.emit("session://log", log);
        let _ = app.emit("log_event", format!("[Session] {}", log.message));
    }

    pub fn emit_completed(app: &AppHandle, session: &OperationSession) {
        let _ = app.emit("session://completed", session);
    }

    pub fn emit_failed(app: &AppHandle, session: &OperationSession) {
        let _ = app.emit("session://failed", session);
    }

    pub fn emit_cancelled(app: &AppHandle, session: &OperationSession) {
        let _ = app.emit("session://cancelled", session);
    }
}
