use anyhow::Result;
use tracing::{error, info};

pub const ODIN_HELLO_REQ: &[u8] = b"ODIN";
pub const ODIN_HELLO_ACK: &[u8] = b"ACK";

#[derive(Debug, Default, serde::Serialize)]
pub struct PitEntry {
    pub name: String,
    pub size: u64,
    pub offset: u64,
}

#[derive(Debug, Default)]
pub struct OdinHandshakeInfo {
    pub device_name: String,
    pub chip_id: u32,
    pub pit_size: u32,
}

pub struct OdinProtocol;

impl OdinProtocol {
    pub fn new() -> Self {
        Self
    }

    pub fn parse_pit(&self, data: &[u8]) -> Result<Vec<PitEntry>> {
        info!("Parsing PIT data ({} bytes)...", data.len());

        // Header: PIT\0 (4 bytes) + counts + metadata
        if data.len() < 28 || &data[0..4] != b"PIT\0" {
            return Err(anyhow::anyhow!("Invalid PIT header signature."));
        }

        let entry_count = u32::from_le_bytes(data[4..8].try_into()?) as usize;
        let mut entries = Vec::with_capacity(entry_count);

        // Standard entry size is 132 bytes usually in modern PITs
        // Offset 28 is where entries often start
        let mut cursor = 28;
        for _ in 0..entry_count {
            if cursor + 132 > data.len() {
                break;
            }

            let entry_slice = &data[cursor..cursor + 132];

            // Name is at beginning of entry block, 32 bytes max
            let mut name_end = 0;
            while name_end < 32 && entry_slice[name_end] != 0 {
                name_end += 1;
            }
            let name = String::from_utf8_lossy(&entry_slice[0..name_end]).to_string();

            // Simplified offsets for simulation (in reality specific indices)
            let size = u64::from_le_bytes(entry_slice[32..40].try_into()?);
            let offset = u64::from_le_bytes(entry_slice[40..48].try_into()?);

            if !name.is_empty() {
                entries.push(PitEntry { name, size, offset });
            }
            cursor += 132;
        }

        info!("Successfully parsed {} PIT partitions.", entries.len());
        Ok(entries)
    }

    pub fn execute_handshake(&self, rx_buffer: &[u8]) -> Result<OdinHandshakeInfo> {
        info!("Executing Odin handshake from buffer...");

        // Simplified Odin response parsing (mocking standard Loke/Odin format)
        if rx_buffer.len() < 32 {
            error!("Buffer too small for Odin Handshake.");
            return Err(anyhow::anyhow!("odin packet bounds check failed."));
        }

        // Check LOKE signature
        if &rx_buffer[0..4] != b"LOKE" {
            return Err(anyhow::anyhow!("Invalid Odin response signature."));
        }

        let chip_id = u32::from_le_bytes(rx_buffer[4..8].try_into()?);
        let pit_size = u32::from_le_bytes(rx_buffer[8..12].try_into()?);

        let mut name_end = 12;
        while name_end < 32 && rx_buffer[name_end] != 0 {
            name_end += 1;
        }
        let device_name = String::from_utf8_lossy(&rx_buffer[12..name_end])
            .trim()
            .to_string();

        info!("Odin Device: {}", device_name);
        info!("Odin Chip ID: 0x{:08X}", chip_id);
        info!("Odin PIT Size: {} bytes", pit_size);

        Ok(OdinHandshakeInfo {
            device_name,
            chip_id,
            pit_size,
        })
    }
}
