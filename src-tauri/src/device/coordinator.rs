use super::normalizer::SnapshotNormalizer;
use super::types::{DeviceConnectionState, DeviceSnapshot};
use crate::commands::usb_detector::DetectedUsbDevice;
use std::sync::Mutex;
use tauri::{AppHandle, Emitter, Manager};

pub struct DeviceProbeCoordinator {
    current_device: Mutex<Option<DeviceSnapshot>>,
}

impl DeviceProbeCoordinator {
    pub fn new() -> Self {
        Self {
            current_device: Mutex::new(None),
        }
    }

    pub fn get_snapshot(&self) -> Option<DeviceSnapshot> {
        self.current_device.lock().unwrap().clone()
    }

    pub fn set_snapshot(&self, snapshot: Option<DeviceSnapshot>, app: &AppHandle) {
        {
            let mut lock = self.current_device.lock().unwrap();
            *lock = snapshot.clone();
        }
        let _ = app.emit("device://status-changed", snapshot);
    }

    pub fn handle_usb_change(&self, mut devices: Vec<DetectedUsbDevice>, app: &AppHandle) {
        if devices.is_empty() {
            // Unstable / Disconnect sequence
            let current = self.get_snapshot();
            if let Some(mut snap) = current {
                if snap.connection_state == DeviceConnectionState::Connected {
                    snap.connection_state = DeviceConnectionState::Unstable;
                    snap.risk_flags.push("unstableUsb".to_string());
                    self.set_snapshot(Some(snap), app);

                    // Spawn a task to confirm disconnection after 500ms
                    let app_clone = app.clone();
                    tauri::async_runtime::spawn(async move {
                        tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
                        // Retrieve coordinator from Tauri state and verify if it's still unstable
                        let coordinator = app_clone.state::<DeviceProbeCoordinator>();
                        if let Some(current_now) = coordinator.get_snapshot() {
                            if current_now.connection_state == DeviceConnectionState::Unstable {
                                coordinator.set_snapshot(None, &app_clone);
                            }
                        }
                    });
                }
            } else {
                self.set_snapshot(None, app);
            }
        } else {
            // Clean up list and sort to find primary device (prefer active modes)
            devices.sort_by_key(|d| match d.mode {
                crate::commands::usb_detector::DeviceMode::Unknown => 5,
                crate::commands::usb_detector::DeviceMode::Mtp => 4,
                _ => 1,
            });

            let primary = &devices[0];

            // If we didn't have a device, or the primary device ID changed, transition to Detecting first
            let current = self.get_snapshot();
            let new_id = primary
                .serial
                .clone()
                .unwrap_or_else(|| format!("usb-{:04x}-{:04x}", primary.vid, primary.pid));

            let should_detect = current.is_none() || current.unwrap().id != new_id;

            if should_detect {
                let mut detecting_snap = SnapshotNormalizer::normalize_usb_device(primary);
                detecting_snap.connection_state = DeviceConnectionState::Detecting;
                self.set_snapshot(Some(detecting_snap), app);
            }

            // Perform normalization probe
            let final_snap = SnapshotNormalizer::normalize_usb_device(primary);
            self.set_snapshot(Some(final_snap), app);
        }
    }
}
