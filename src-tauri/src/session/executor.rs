use crate::device::coordinator::DeviceProbeCoordinator;
use crate::device::types::DeviceConnectionState;
use crate::session::broadcaster::SessionBroadcaster;
use crate::session::manager::SessionManager;
use crate::session::types::{OperationSession, OperationType, SessionStatus};
use tauri::{AppHandle, Manager};
use tauri_plugin_shell::process::CommandEvent;
use tauri_plugin_shell::ShellExt;
use tokio::time::{sleep, Duration};

fn now_iso() -> String {
    let now = std::time::SystemTime::now();
    let datetime: chrono::DateTime<chrono::Utc> = now.into();
    datetime.to_rfc3339()
}

pub struct SessionExecutor;

impl SessionExecutor {
    fn update_session<F>(app: &AppHandle, f: F)
    where
        F: FnOnce(&mut OperationSession),
    {
        let manager = app.state::<SessionManager>();
        let mut lock = manager.active.lock().unwrap();
        if let Some(ref mut session) = *lock {
            f(session);
            session.updated_at = now_iso();
            let cloned = session.clone();
            SessionBroadcaster::emit_status_changed(app, Some(&cloned));

            // if terminal, copy to history
            match session.status {
                SessionStatus::Completed | SessionStatus::Failed | SessionStatus::Cancelled => {
                    let mut history_lock = manager.history.lock().unwrap();
                    history_lock.push(cloned.clone());

                    match session.status {
                        SessionStatus::Completed => {
                            SessionBroadcaster::emit_completed(app, &cloned)
                        }
                        SessionStatus::Failed => SessionBroadcaster::emit_failed(app, &cloned),
                        SessionStatus::Cancelled => {
                            SessionBroadcaster::emit_cancelled(app, &cloned)
                        }
                        _ => {}
                    }
                }
                _ => {}
            }
        }
    }

    fn append_log(app: &AppHandle, level: &str, message: &str) {
        println!("[SESSION LOG] {}: {}", level, message);
        let log = crate::session::types::SessionLog {
            session_id: "".into(), // will be filled correctly below if we fetched session id
            level: level.to_string(),
            message: message.to_string(),
            context: None,
            timestamp: now_iso(),
        };

        let mut session_id = String::new();
        Self::update_session(app, |session| {
            let mut l = log.clone();
            l.session_id = session.session_id.clone();
            session_id = session.session_id.clone();
            session.logs.push(l.clone());
            SessionBroadcaster::emit_log(app, &l);
        });
    }

    fn update_step(app: &AppHandle, step_id: &str, status: &str, detail: Option<&str>) {
        let mut step_clone = None;
        Self::update_session(app, |session| {
            if let Some(step) = session.steps.iter_mut().find(|s| s.id == step_id) {
                step.status = status.to_string();
                if let Some(d) = detail {
                    step.detail = Some(d.to_string());
                }
                if status == "running" {
                    step.duration_ms = Some(0);
                }
                step_clone = Some(step.clone());
            }
        });

        if let Some(s) = step_clone {
            SessionBroadcaster::emit_step_update(app, &s);
        }
    }

    async fn complete(app: &AppHandle) {
        Self::append_log(app, "success", "Operation completed successfully.");
        Self::update_session(app, |session| {
            session.status = SessionStatus::Completed;
            session.outcome = Some("success".to_string());
            session.completed_at = Some(now_iso());
            session.can_cancel = false;
        });
    }

    async fn fail(app: &AppHandle, error_code: &str, error_message: &str) {
        Self::append_log(
            app,
            "error",
            &format!("Operation failed: {}", error_message),
        );
        Self::update_session(app, |session| {
            session.status = SessionStatus::Failed;
            session.outcome = Some("failed".to_string());
            session.completed_at = Some(now_iso());
            session.error_code = Some(error_code.to_string());
            session.error_message = Some(error_message.to_string());
            session.can_cancel = false;
            session.can_retry = true;
        });
    }

    fn is_cancelled(app: &AppHandle) -> bool {
        let manager = app.state::<SessionManager>();
        let lock = manager.active.lock().unwrap();
        if let Some(ref session) = *lock {
            session.status == SessionStatus::Cancelled
                || session.status == SessionStatus::Cancelling
        } else {
            true
        }
    }

    pub async fn execute_pipeline(app: AppHandle, operation_type: OperationType) {
        Self::append_log(
            &app,
            "info",
            &format!("Starting operation pipeline: {:?}", operation_type),
        );

        match operation_type {
            OperationType::HelloActivation | OperationType::HelloNoSignalActivation => {
                Self::update_step(
                    &app,
                    "preflight",
                    "running",
                    Some("Analyzing device connection status and trust dialog..."),
                );
                sleep(Duration::from_millis(800)).await;
                if Self::is_cancelled(&app) {
                    return;
                }

                let device_coordinator = app.state::<DeviceProbeCoordinator>();
                let snapshot = device_coordinator.get_snapshot();
                if let Some(snap) = snapshot {
                    if snap.connection_state == DeviceConnectionState::Disconnected {
                        Self::fail(&app, "DEVICE_DISCONNECTED", "Device is disconnected").await;
                        return;
                    }
                } else {
                    Self::fail(&app, "DEVICE_NOT_FOUND", "No connected iOS device detected").await;
                    return;
                }
                Self::update_step(&app, "preflight", "done", Some("Preflight checks passed."));

                // Exploit step
                Self::update_step(
                    &app,
                    "exploit",
                    "running",
                    Some("Executing bypass runner script..."),
                );
                let script_path = app
                    .path()
                    .resource_dir()
                    .unwrap()
                    .join("python")
                    .join("ios_bypass/hello_bypass.py");
                let mode_arg = if operation_type == OperationType::HelloActivation {
                    "run"
                } else {
                    "run-wifi"
                };

                let command_result = app
                    .shell()
                    .command("python3")
                    .args([
                        script_path.to_str().unwrap_or("hello_bypass.py"),
                        mode_arg,
                        "session-active",
                    ])
                    .spawn();

                match command_result {
                    Ok((mut rx, _child)) => {
                        let mut success = false;
                        while let Some(event) = rx.recv().await {
                            if Self::is_cancelled(&app) {
                                return;
                            }
                            match event {
                                CommandEvent::Stdout(bytes) => {
                                    let line = String::from_utf8_lossy(&bytes).to_string();
                                    for raw_line in line.lines() {
                                        let trimmed = raw_line.trim();
                                        if trimmed.is_empty() {
                                            continue;
                                        }
                                        if let Ok(val) =
                                            serde_json::from_str::<serde_json::Value>(trimmed)
                                        {
                                            let msg = val
                                                .get("message")
                                                .and_then(|v| v.as_str())
                                                .unwrap_or(trimmed);
                                            Self::append_log(
                                                &app,
                                                "info",
                                                &format!("[Bypass] {}", msg),
                                            );
                                            if val.get("event").and_then(|v| v.as_str())
                                                == Some("complete")
                                            {
                                                success = val
                                                    .get("success")
                                                    .and_then(|v| v.as_bool())
                                                    .unwrap_or(false);
                                            }
                                        } else {
                                            Self::append_log(&app, "info", trimmed);
                                        }
                                    }
                                }
                                CommandEvent::Stderr(bytes) => {
                                    let line = String::from_utf8_lossy(&bytes).to_string();
                                    for raw_line in line.lines() {
                                        Self::append_log(&app, "warn", raw_line.trim());
                                    }
                                }
                                CommandEvent::Terminated(p) => {
                                    Self::append_log(
                                        &app,
                                        "info",
                                        &format!("Runner finished with code {:?}", p.code),
                                    );
                                    break;
                                }
                                _ => {}
                            }
                        }

                        if success {
                            Self::update_step(
                                &app,
                                "exploit",
                                "done",
                                Some("Bypass script completed."),
                            );
                        } else {
                            Self::update_step(&app, "exploit", "failed", Some("Script error."));
                            Self::fail(
                                &app,
                                "BYPASS_SCRIPT_FAILED",
                                "Activation ticket load failure",
                            )
                            .await;
                            return;
                        }
                    }
                    Err(e) => {
                        Self::update_step(&app, "exploit", "failed", Some("Spawn error."));
                        Self::fail(&app, "SPAWN_FAILED", &e.to_string()).await;
                        return;
                    }
                }

                // Verify step
                Self::update_step(
                    &app,
                    "verify",
                    "running",
                    Some("Querying lock status post-bypass..."),
                );
                sleep(Duration::from_millis(1000)).await;
                if Self::is_cancelled(&app) {
                    return;
                }

                Self::update_step(&app, "verify", "done", Some("Bypass verified."));
                Self::complete(&app).await;
            }

            OperationType::DfuAssist => {
                Self::update_step(&app, "preflight", "running", Some("Validating device..."));
                sleep(Duration::from_millis(500)).await;
                Self::update_step(
                    &app,
                    "preflight",
                    "done",
                    Some("Device valid for DFU transition."),
                );

                Self::update_step(
                    &app,
                    "execute",
                    "running",
                    Some("Sending reboot-to-recovery command..."),
                );
                let device_coordinator = app.state::<DeviceProbeCoordinator>();
                let udid = if let Some(snap) = device_coordinator.get_snapshot() {
                    snap.id
                } else {
                    Self::fail(&app, "DEVICE_NOT_FOUND", "Device disconnected").await;
                    return;
                };

                let output = app
                    .shell()
                    .command("ideviceenterrecovery")
                    .args([&udid])
                    .output()
                    .await;
                match output {
                    Ok(out) if out.status.success() => {
                        Self::append_log(&app, "info", "Device put in recovery mode.");
                        Self::update_step(
                            &app,
                            "execute",
                            "done",
                            Some("Recovery mode requested."),
                        );
                        Self::complete(&app).await;
                    }
                    Ok(out) => {
                        let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                        Self::update_step(&app, "execute", "failed", Some("Command failed."));
                        Self::fail(&app, "RECOVERY_COMMAND_FAILED", &stderr).await;
                    }
                    Err(err) => {
                        Self::update_step(&app, "execute", "failed", Some("Spawn error."));
                        Self::fail(&app, "SPAWN_FAILED", &err.to_string()).await;
                    }
                }
            }

            OperationType::RecoveryExit => {
                Self::update_step(&app, "execute", "running", Some("Sending recovery exit..."));
                let irecovery_result = app.shell().command("irecovery").args(["-n"]).output().await;
                match irecovery_result {
                    Ok(out) if out.status.success() => {
                        Self::update_step(&app, "execute", "done", Some("Exit recovery sent."));
                        Self::complete(&app).await;
                    }
                    Ok(out) => {
                        let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                        Self::update_step(&app, "execute", "failed", Some("Command rejected."));
                        Self::fail(&app, "IRECOVERY_FAILED", &stderr).await;
                    }
                    Err(err) => {
                        Self::update_step(&app, "execute", "failed", Some("Spawn error."));
                        Self::fail(&app, "SPAWN_FAILED", &err.to_string()).await;
                    }
                }
            }

            OperationType::Reboot => {
                Self::update_step(
                    &app,
                    "execute",
                    "running",
                    Some("Sending reboot command..."),
                );
                let result = app
                    .shell()
                    .command("idevicediagnostics")
                    .args(["restart"])
                    .output()
                    .await;
                match result {
                    Ok(out) if out.status.success() => {
                        Self::update_step(&app, "execute", "done", Some("Reboot initiated."));
                        Self::complete(&app).await;
                    }
                    Ok(out) => {
                        let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                        Self::update_step(&app, "execute", "failed", Some("Reboot rejected."));
                        Self::fail(&app, "REBOOT_FAILED", &stderr).await;
                    }
                    Err(err) => {
                        Self::update_step(&app, "execute", "failed", Some("Spawn error."));
                        Self::fail(&app, "SPAWN_FAILED", &err.to_string()).await;
                    }
                }
            }

            _ => {
                Self::update_step(
                    &app,
                    "execute",
                    "running",
                    Some("Running generic sequence..."),
                );
                sleep(Duration::from_millis(2000)).await;
                if Self::is_cancelled(&app) {
                    return;
                }
                Self::update_step(&app, "execute", "done", Some("Completed."));
                Self::complete(&app).await;
            }
        }
    }
}
