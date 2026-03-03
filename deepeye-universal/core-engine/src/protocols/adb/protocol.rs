use anyhow::{anyhow, Result};

#[derive(Debug)]
pub struct AdbPacket {
    pub command: u32,
    pub arg0: u32,
    pub arg1: u32,
    pub data: Vec<u8>,
}

pub const A_CNXN: u32 = 0x4e584e43; // "CNXN"
pub const A_AUTH: u32 = 0x48545541; // "AUTH"
pub const A_OPEN: u32 = 0x4e45504f; // "OPEN"
pub const A_OKAY: u32 = 0x59414b4f; // "OKAY"
pub const A_CLSE: u32 = 0x45534c43; // "CLSE"
pub const A_WRTE: u32 = 0x45545257; // "WRTE"

pub struct AdbProtocol;

impl AdbProtocol {
    pub fn build_packet(command: u32, arg0: u32, arg1: u32, data: &[u8]) -> Vec<u8> {
        let mut pkt = Vec::with_capacity(24 + data.len());
        let data_len = data.len() as u32;
        let mut sum: u32 = 0;
        for &b in data {
            sum = sum.wrapping_add(b as u32);
        }

        pkt.extend_from_slice(&command.to_le_bytes());
        pkt.extend_from_slice(&arg0.to_le_bytes());
        pkt.extend_from_slice(&arg1.to_le_bytes());
        pkt.extend_from_slice(&data_len.to_le_bytes());
        pkt.extend_from_slice(&sum.to_le_bytes());
        pkt.extend_from_slice(&(command ^ 0xFFFFFFFF).to_le_bytes());
        pkt.extend_from_slice(data);
        pkt
    }

    pub fn parse_header(header: &[u8]) -> Result<(u32, u32, u32, u32, u32, u32)> {
        if header.len() < 24 {
            return Err(anyhow!("Header too short"));
        }
        let command = u32::from_le_bytes(header[0..4].try_into()?);
        let arg0 = u32::from_le_bytes(header[4..8].try_into()?);
        let arg1 = u32::from_le_bytes(header[8..12].try_into()?);
        let data_len = u32::from_le_bytes(header[12..16].try_into()?);
        let data_check = u32::from_le_bytes(header[16..20].try_into()?);
        let magic = u32::from_le_bytes(header[20..24].try_into()?);

        if command ^ 0xFFFFFFFF != magic {
            return Err(anyhow!(
                "Magic mismatch: expected {:08X}, got {:08X}",
                command ^ 0xFFFFFFFF,
                magic
            ));
        }

        Ok((command, arg0, arg1, data_len, data_check, magic))
    }
}
