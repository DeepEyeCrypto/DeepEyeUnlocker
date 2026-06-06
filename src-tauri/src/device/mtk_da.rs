#![allow(dead_code)]

use rusb::{DeviceHandle, GlobalContext};
use serde::Serialize;
use std::time::Duration;

/// Flash operation progress update
#[derive(Debug, Clone, Serialize)]
pub struct FlashProgress {
    pub written: usize,
    pub total: usize,
    pub percent: u8,
    pub partition: String,
}

/// MTK Download Agent session for post-BROM operations
pub struct DaSession {
    handle: DeviceHandle<GlobalContext>,
    interface_claimed: bool,
    interface_num: u8,
    ep_in: u8,
    ep_out: u8,
}

impl DaSession {
    const TIMEOUT: Duration = Duration::from_secs(30);
    const CHUNK_SIZE: usize = 0x8000; // 32KB chunks
    const MAX_RETRIES: u8 = 1;

    pub fn new(handle: DeviceHandle<GlobalContext>) -> Result<Self, String> {
        let device = handle.device();
        let config = device
            .active_config_descriptor()
            .map_err(|e| format!("Failed to read config: {}", e))?;

        let mut interface_num = 0;
        let mut ep_in = 0;
        let mut ep_out = 0;
        let mut found = false;

        for iface in config.interfaces() {
            for desc in iface.descriptors() {
                let mut has_in = 0;
                let mut has_out = 0;
                for ep in desc.endpoint_descriptors() {
                    if ep.transfer_type() == rusb::TransferType::Bulk {
                        if ep.direction() == rusb::Direction::In {
                            has_in = ep.address();
                        }
                        if ep.direction() == rusb::Direction::Out {
                            has_out = ep.address();
                        }
                    }
                }
                if has_in != 0 && has_out != 0 {
                    interface_num = iface.number();
                    ep_in = has_in;
                    ep_out = has_out;
                    found = true;
                    break;
                }
            }
            if found {
                break;
            }
        }

        if !found {
            return Err("Failed to find DA bulk endpoints".into());
        }

        // Detach kernel driver if active (Linux/macOS)
        #[cfg(any(target_os = "linux", target_os = "macos"))]
        {
            if handle.kernel_driver_active(interface_num).unwrap_or(false) {
                log::info!(
                    "[MTK DA] Detaching kernel driver from interface {}",
                    interface_num
                );
                handle.detach_kernel_driver(interface_num).map_err(|e| {
                    format!(
                        "Failed to detach kernel driver: {}. Try running with sudo.",
                        e
                    )
                })?;
            }
        }

        // Claim the USB interface
        handle
            .claim_interface(interface_num)
            .map_err(|e| format!("Failed to claim USB interface {}: {}", interface_num, e))?;

        log::info!(
            "[MTK DA] USB interface {} claimed successfully (IN: {:02X}, OUT: {:02X})",
            interface_num,
            ep_in,
            ep_out
        );

        Ok(Self {
            handle,
            interface_claimed: true,
            interface_num,
            ep_in,
            ep_out,
        })
    }

    /// Flash partition with progress callbacks.
    /// Sends data in chunks with per-chunk ACK verification.
    pub fn flash_partition(
        &self,
        partition: &str,
        data: &[u8],
        progress_tx: tokio::sync::mpsc::Sender<FlashProgress>,
    ) -> Result<(), String> {
        let total = data.len();
        if total == 0 {
            return Err("Cannot flash empty data".into());
        }

        log::info!(
            "[MTK DA] Flashing partition '{}' with {} bytes",
            partition,
            total
        );

        // Build DA flash command
        let mut cmd = Vec::new();
        cmd.push(0xD9); // FLASH command
        cmd.extend_from_slice(partition.as_bytes());
        cmd.push(0x00); // Null terminator
        cmd.extend_from_slice(&(total as u32).to_be_bytes());

        // Send command
        self.write_with_retry(&cmd)?;

        // Verify command accepted
        let resp = self.read_with_retry(2)?;
        if resp[0] != 0x00 {
            return Err(format!(
                "Flash command rejected for '{}': status {:02X}",
                partition, resp[0]
            ));
        }

        // Transfer data in chunks with per-chunk ACK
        let mut written = 0usize;
        for chunk in data.chunks(Self::CHUNK_SIZE) {
            self.write_with_retry(chunk)?;

            // Read per-chunk ACK from device — critical for flow control.
            // Without this, host can outrun device flash write speed.
            let ack = self.read_with_retry(1)?;
            if ack[0] != 0x00 {
                return Err(format!(
                    "Flash chunk rejected at offset {}: ACK={:02X}",
                    written, ack[0]
                ));
            }

            written += chunk.len();

            let percent = ((written as f64 / total as f64) * 100.0) as u8;

            // Send progress update (non-blocking)
            let _ = progress_tx.try_send(FlashProgress {
                written,
                total,
                percent,
                partition: partition.to_string(),
            });

            log::debug!(
                "[MTK DA] Flash progress: {}/{} bytes ({}%)",
                written,
                total,
                percent
            );
        }

        // Read final status
        let status = self.read_with_retry(2)?;
        if status[0] != 0x00 {
            return Err(format!(
                "Flash operation failed for '{}': final status {:02X}",
                partition, status[0]
            ));
        }

        log::info!(
            "[MTK DA] Successfully flashed {} bytes to '{}'",
            written,
            partition
        );

        Ok(())
    }

    /// Read partition data
    pub fn read_partition(&self, partition: &str, size: u32) -> Result<Vec<u8>, String> {
        log::info!(
            "[MTK DA] Reading partition '{}' ({} bytes)",
            partition,
            size
        );

        // Build read command
        let mut cmd = Vec::new();
        cmd.push(0xDA); // READ command
        cmd.extend_from_slice(partition.as_bytes());
        cmd.push(0x00); // Null terminator
        cmd.extend_from_slice(&size.to_be_bytes());

        // Send command
        self.write_with_retry(&cmd)?;

        // Verify command accepted
        let resp = self.read_with_retry(2)?;
        if resp[0] != 0x00 {
            return Err(format!(
                "Read command rejected for '{}': status {:02X}",
                partition, resp[0]
            ));
        }

        // Read data in chunks — guard against underflow on short reads
        let mut data = Vec::with_capacity(size as usize);
        let mut remaining = size as usize;

        while remaining > 0 {
            let chunk_size = remaining.min(Self::CHUNK_SIZE);
            let chunk = self.read_with_retry(chunk_size)?;

            // Validate: chunk must not exceed what we asked for
            let actual_len = chunk.len().min(remaining);
            data.extend_from_slice(&chunk[..actual_len]);

            // Saturating subtraction prevents underflow panic
            remaining = remaining.saturating_sub(actual_len);

            log::debug!(
                "[MTK DA] Read progress: {}/{} bytes (chunk: {} bytes)",
                data.len(),
                size,
                actual_len
            );
        }

        log::info!(
            "[MTK DA] Successfully read {} bytes from '{}'",
            data.len(),
            partition
        );

        Ok(data)
    }

    /// Erase partition
    pub fn erase_partition(&self, partition: &str) -> Result<(), String> {
        log::info!("[MTK DA] Erasing partition '{}'", partition);

        let mut cmd = Vec::new();
        cmd.push(0xDB); // ERASE command
        cmd.extend_from_slice(partition.as_bytes());
        cmd.push(0x00);

        self.write_with_retry(&cmd)?;

        let status = self.read_with_retry(2)?;
        if status[0] != 0x00 {
            return Err(format!(
                "Erase operation failed for '{}': status {:02X}",
                partition, status[0]
            ));
        }

        log::info!("[MTK DA] Successfully erased partition '{}'", partition);
        Ok(())
    }

    // ─── USB I/O with retry ────────────────────────────────────────────────

    /// Write with single retry on transient USB errors
    fn write_with_retry(&self, data: &[u8]) -> Result<(), String> {
        match self.write(data) {
            Ok(()) => Ok(()),
            Err(e) => {
                log::warn!("[MTK DA] Write failed ({}), retrying once...", e);
                std::thread::sleep(Duration::from_millis(50));
                self.write(data)
            }
        }
    }

    /// Read with single retry on transient USB errors
    fn read_with_retry(&self, len: usize) -> Result<Vec<u8>, String> {
        match self.read(len) {
            Ok(data) => Ok(data),
            Err(e) => {
                log::warn!("[MTK DA] Read failed ({}), retrying once...", e);
                std::thread::sleep(Duration::from_millis(50));
                self.read(len)
            }
        }
    }

    /// Write raw data to USB
    fn write(&self, data: &[u8]) -> Result<(), String> {
        let written = self
            .handle
            .write_bulk(self.ep_out, data, Self::TIMEOUT)
            .map_err(|e| format!("USB write failed: {}", e))?;

        if written != data.len() {
            return Err(format!(
                "Short write: expected {} bytes, wrote {}",
                data.len(),
                written
            ));
        }

        Ok(())
    }

    /// Read raw data from USB
    fn read(&self, len: usize) -> Result<Vec<u8>, String> {
        let mut buf = vec![0u8; len];
        let read = self
            .handle
            .read_bulk(self.ep_in, &mut buf, Self::TIMEOUT)
            .map_err(|e| format!("USB read failed: {}", e))?;

        if read == 0 {
            return Err("Device returned no data".into());
        }

        Ok(buf[..read].to_vec())
    }
}

impl Drop for DaSession {
    fn drop(&mut self) {
        // Release USB interface on drop
        if self.interface_claimed {
            if let Err(e) = self.handle.release_interface(self.interface_num) {
                log::warn!("[MTK DA] Failed to release interface on drop: {}", e);
            } else {
                log::debug!("[MTK DA] USB interface {} released", self.interface_num);
            }
        }
    }
}
