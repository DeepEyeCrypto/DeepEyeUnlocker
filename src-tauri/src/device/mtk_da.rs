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
}

impl DaSession {
    const DA_EP_IN: u8 = 0x81;
    const DA_EP_OUT: u8 = 0x01;
    const TIMEOUT: Duration = Duration::from_secs(30);
    const CHUNK_SIZE: usize = 0x8000; // 32KB chunks

    /// Create new DA session from existing USB handle
    pub fn new(handle: DeviceHandle<GlobalContext>) -> Self {
        Self { handle }
    }

    /// Flash partition with progress callbacks
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
        self.write(&cmd)?;

        // Verify command accepted
        let resp = self.read(2)?;
        if resp[0] != 0x00 {
            return Err(format!("Flash command rejected: {:02X}", resp[0]));
        }

        // Transfer data in chunks
        let mut written = 0usize;
        for chunk in data.chunks(Self::CHUNK_SIZE) {
            self.write(chunk)?;
            written += chunk.len();

            let percent = if total > 0 {
                ((written as f64 / total as f64) * 100.0) as u8
            } else {
                0
            };

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
        let status = self.read(2)?;
        if status[0] != 0x00 {
            return Err(format!(
                "Flash operation failed with status: {:02X}",
                status[0]
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
        self.write(&cmd)?;

        // Verify command accepted
        let resp = self.read(2)?;
        if resp[0] != 0x00 {
            return Err(format!("Read command rejected: {:02X}", resp[0]));
        }

        // Read data in chunks
        let mut data = Vec::with_capacity(size as usize);
        let mut remaining = size as usize;

        while remaining > 0 {
            let chunk_size = remaining.min(Self::CHUNK_SIZE);
            let chunk = self.read(chunk_size)?;
            data.extend_from_slice(&chunk);
            remaining -= chunk.len();

            log::debug!(
                "[MTK DA] Read progress: {}/{} bytes",
                data.len(),
                size
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

        self.write(&cmd)?;

        let status = self.read(2)?;
        if status[0] != 0x00 {
            return Err(format!(
                "Erase operation failed with status: {:02X}",
                status[0]
            ));
        }

        log::info!("[MTK DA] Successfully erased partition '{}'", partition);
        Ok(())
    }

    /// Write raw data to USB
    fn write(&self, data: &[u8]) -> Result<(), String> {
        let written = self
            .handle
            .write_bulk(Self::DA_EP_OUT, data, Self::TIMEOUT)
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
            .read_bulk(Self::DA_EP_IN, &mut buf, Self::TIMEOUT)
            .map_err(|e| format!("USB read failed: {}", e))?;

        if read == 0 {
            return Err("Device returned no data".into());
        }

        Ok(buf[..read].to_vec())
    }
}
