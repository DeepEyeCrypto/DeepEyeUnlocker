use anyhow::{anyhow, Result};
use nusb::transfer::RequestBuffer;

pub struct QcomSession {
    interface: nusb::Interface,
    read_endpoint: u8,
    write_endpoint: u8,
}

impl QcomSession {
    pub fn new(device: nusb::Device) -> Result<Self> {
        let configs = device.configurations();

        for config in configs {
            for interface_desc in config.interfaces() {
                for alt in interface_desc.alt_settings() {
                    // QCOM EDL usually class 0xFF
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

        Err(anyhow!("No Qualcomm EDL interface found"))
    }

    pub async fn write(&self, data: &[u8]) -> Result<()> {
        let res = self
            .interface
            .bulk_out(self.write_endpoint, data.to_vec())
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("Sahara Write Error: {:?}", e));
        }
        Ok(())
    }

    pub async fn read(&self, len: usize) -> Result<Vec<u8>> {
        let res = self
            .interface
            .bulk_in(self.read_endpoint, RequestBuffer::new(len))
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("Sahara Read Error: {:?}", e));
        }
        Ok(res.data)
    }
}
