use anyhow::{anyhow, Result};
use nusb::transfer::RequestBuffer;
use std::time::Duration;

pub struct MtkSession {
    interface: nusb::Interface,
    read_endpoint: u8,
    write_endpoint: u8,
}

impl MtkSession {
    pub fn new(device: nusb::Device) -> Result<Self> {
        let configs = device.configurations();

        for config in configs {
            for interface_desc in config.interfaces() {
                for alt in interface_desc.alt_settings() {
                    // MTK BROM usually class 0xFF or 0x02 (CDC)
                    // We look for specific VID/PID typically (0x0E8D:0x0003)
                    let interface_num = interface_desc.interface_number();

                    let mut read_endpoint = 0;
                    let mut write_endpoint = 0;

                    for ep in alt.endpoints() {
                        if ep.direction() == nusb::transfer::Direction::In {
                            read_endpoint = ep.address();
                        } else {
                            write_endpoint = ep.address();
                        }
                    }

                    if read_endpoint != 0 && write_endpoint != 0 {
                        let interface = device.claim_interface(interface_num)?;
                        return Ok(Self {
                            interface,
                            read_endpoint,
                            write_endpoint,
                        });
                    }
                }
            }
        }

        Err(anyhow!("No compatible MTK interface found on device"))
    }

    pub async fn write(&self, data: &[u8]) -> Result<()> {
        let res = self
            .interface
            .bulk_out(self.write_endpoint, data.to_vec())
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("MTK Write Error: {:?}", e));
        }
        Ok(())
    }

    pub async fn read(&self, len: usize) -> Result<Vec<u8>> {
        let res = self
            .interface
            .bulk_in(self.read_endpoint, RequestBuffer::new(len))
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("MTK Read Error: {:?}", e));
        }
        Ok(res.data)
    }

    /// Step 1: Echo Handshake
    pub async fn echo_handshake(&self) -> Result<()> {
        // Send 0xA0 until device responds with 0x5F
        for _ in 0..10 {
            self.write(&[0xA0]).await?;
            let resp = self.read(1).await?;
            if !resp.is_empty() && resp[0] == 0x5F {
                return Ok(());
            }
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
        Err(anyhow!("MTK BROM Handshake Timeout (No Echo)"))
    }

    /// Step 2: Get Chip Info (Placeholder for SEC_AUTH bypass)
    pub async fn read_chip_info(&self) -> Result<Vec<u8>> {
        // Command to read target info
        self.write(&[0xD1]).await?;
        self.read(16).await
    }
}
