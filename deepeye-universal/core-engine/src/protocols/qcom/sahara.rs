use anyhow::Result;
use tracing::{error, info};

pub const SAHARA_HELLO_REQ: u32 = 0x01;
pub const SAHARA_HELLO_RESP: u32 = 0x02;
pub const SAHARA_READ_DATA: u32 = 0x03;
pub const SAHARA_END_IMAGE_TX: u32 = 0x04;
pub const SAHARA_DONE_REQ: u32 = 0x05;
pub const SAHARA_DONE_RESP: u32 = 0x06;
pub const SAHARA_RESET_REQ: u32 = 0x07;
pub const SAHARA_RESET_RESP: u32 = 0x08;

#[derive(Debug)]
pub struct SaharaHandshakeInfo {
    pub version: u32,
    pub min_version: u32,
    pub max_cmd_packet_size: u32,
    pub mode: u32,
}

pub struct SaharaProtocol;

impl SaharaProtocol {
    pub fn new() -> Self {
        Self
    }

    pub fn execute_handshake(&self, rx_buffer: &[u8]) -> Result<SaharaHandshakeInfo> {
        info!("Parsing Sahara Hello Request...");

        if rx_buffer.len() < 48 {
            error!("Buffer too small for Sahara Protocol Header.");
            return Err(anyhow::anyhow!("sahara packet bounds check failed."));
        }

        let cmd = u32::from_le_bytes(rx_buffer[0..4].try_into()?);
        let _len = u32::from_le_bytes(rx_buffer[4..8].try_into()?);

        if cmd != SAHARA_HELLO_REQ {
            return Err(anyhow::anyhow!(
                "Expected Hello Request (0x01), got 0x{:02X}",
                cmd
            ));
        }

        let version = u32::from_le_bytes(rx_buffer[8..12].try_into()?);
        let min_version = u32::from_le_bytes(rx_buffer[12..16].try_into()?);
        let max_cmd_packet_size = u32::from_le_bytes(rx_buffer[16..20].try_into()?);
        let mode = u32::from_le_bytes(rx_buffer[20..24].try_into()?);

        info!("Sahara Version: {}", version);
        info!("Sahara Min Version: {}", min_version);
        info!("Sahara Max Pkt Size: {}", max_cmd_packet_size);
        info!("Sahara Mode: {}", mode);

        Ok(SaharaHandshakeInfo {
            version,
            min_version,
            max_cmd_packet_size,
            mode,
        })
    }

    pub fn process_reset_resp(&self, rx_buffer: &[u8]) -> Result<()> {
        if rx_buffer.len() < 8 {
            return Err(anyhow::anyhow!("buffer too small for reset response"));
        }
        let cmd = u32::from_le_bytes(rx_buffer[0..4].try_into()?);
        if cmd != SAHARA_RESET_RESP {
            return Err(anyhow::anyhow!(
                "Expected Reset Response (0x08), got 0x{:02X}",
                cmd
            ));
        }
        info!("Sahara Reset Response OK.");
        Ok(())
    }

    pub fn process_done_resp(&self, rx_buffer: &[u8]) -> Result<u32> {
        if rx_buffer.len() < 12 {
            return Err(anyhow::anyhow!("buffer too small for done response"));
        }
        let cmd = u32::from_le_bytes(rx_buffer[0..4].try_into()?);
        if cmd != SAHARA_DONE_RESP {
            return Err(anyhow::anyhow!(
                "Expected Done Response (0x06), got 0x{:02X}",
                cmd
            ));
        }
        let status = u32::from_le_bytes(rx_buffer[8..12].try_into()?);
        info!("Sahara Done Status: {}", status);
        Ok(status) // 0 implies success
    }
}
