use super::protocol::{AdbProtocol, A_AUTH, A_CLSE, A_CNXN, A_OKAY, A_OPEN, A_WRTE};
use anyhow::{anyhow, Result};
use nusb::transfer::RequestBuffer;

pub struct AdbSession {
    interface: nusb::Interface,
    read_endpoint: u8,
    write_endpoint: u8,
    #[allow(dead_code)]
    _max_payload: u32,
}

impl AdbSession {
    pub fn new(device: nusb::Device) -> Result<Self> {
        let configs = device.configurations();

        for config in configs {
            for interface_desc in config.interfaces() {
                for alt in interface_desc.alt_settings() {
                    // ADB class=255, subclass=66, protocol=1
                    if alt.class() == 255 && alt.subclass() == 66 && alt.protocol() == 1 {
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

                        let interface = device.claim_interface(interface_num)?;

                        return Ok(Self {
                            interface,
                            read_endpoint,
                            write_endpoint,
                            _max_payload: 4096,
                        });
                    }
                }
            }
        }

        Err(anyhow!("No ADB interface found on device"))
    }

    pub async fn connect(&mut self) -> Result<()> {
        let connect_payload = b"host::\0";
        let connect_pkt = AdbProtocol::build_packet(A_CNXN, 0x01000000, 4096, connect_payload);

        // Write CNXN
        let res = self
            .interface
            .bulk_out(self.write_endpoint, connect_pkt)
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("ADB Write Error: {:?}", e));
        }

        // Read Response (should be CNXN or AUTH)
        let res = self
            .interface
            .bulk_in(self.read_endpoint, RequestBuffer::new(24))
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("ADB Read Error: {:?}", e));
        }
        let header = res.data;

        let (cmd, _arg0, arg1, data_len, _, _) = AdbProtocol::parse_header(&header)?;

        if data_len > 0 {
            let res = self
                .interface
                .bulk_in(self.read_endpoint, RequestBuffer::new(data_len as usize))
                .await;
            if let Err(e) = res.status {
                return Err(anyhow!("ADB Read Data Error: {:?}", e));
            }
            if cmd == A_AUTH {
                return Err(anyhow!("ADB Authentication required (Check device screen)"));
            }
        }

        if cmd == A_CNXN {
            self._max_payload = arg1;
            Ok(())
        } else {
            Err(anyhow!("Unexpected ADB response: {:08X}", cmd))
        }
    }

    pub async fn shell_exec(&self, command: &str) -> Result<String> {
        let local_id = 1;
        let open_payload = format!("shell:{}\0", command);
        let open_pkt = AdbProtocol::build_packet(A_OPEN, local_id, 0, open_payload.as_bytes());

        let res = self.interface.bulk_out(self.write_endpoint, open_pkt).await;
        if let Err(e) = res.status {
            return Err(anyhow!("ADB Open Error: {:?}", e));
        }

        // Read OKAY
        let res = self
            .interface
            .bulk_in(self.read_endpoint, RequestBuffer::new(24))
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("ADB Read OKAY Error: {:?}", e));
        }
        let (cmd, remote_id, _, _, _, _) = AdbProtocol::parse_header(&res.data)?;

        if cmd != A_OKAY {
            return Err(anyhow!("Failed to open shell: response {:08X}", cmd));
        }

        let mut output = Vec::new();
        loop {
            let res = self
                .interface
                .bulk_in(self.read_endpoint, RequestBuffer::new(24))
                .await;
            if let Err(e) = res.status {
                return Err(anyhow!("ADB Poll Error: {:?}", e));
            }
            let (cmd, _, _, data_len, _, _) = AdbProtocol::parse_header(&res.data)?;

            if data_len > 0 {
                let res = self
                    .interface
                    .bulk_in(self.read_endpoint, RequestBuffer::new(data_len as usize))
                    .await;
                if let Err(e) = res.status {
                    return Err(anyhow!("ADB Data Stream Error: {:?}", e));
                }
                if cmd == A_WRTE {
                    output.extend_from_slice(&res.data);
                    // Send OKAY
                    let okay_pkt = AdbProtocol::build_packet(A_OKAY, local_id, remote_id, &[]);
                    let _ = self.interface.bulk_out(self.write_endpoint, okay_pkt).await;
                }
            }

            if cmd == A_CLSE {
                break;
            }
        }

        Ok(String::from_utf8_lossy(&output).into_owned())
    }

    pub async fn push_file(&self, local_path: &str, remote_path: &str) -> Result<()> {
        let file_data = std::fs::read(local_path)?;
        let local_id = 2;

        // Use 'sh -c "cat > ..."' trick for surgical file transfer without full SYNC protocol complexity
        let open_payload = format!("shell:sh -c \"cat > {}\"\0", remote_path);
        let open_pkt = AdbProtocol::build_packet(A_OPEN, local_id, 0, open_payload.as_bytes());

        let res = self.interface.bulk_out(self.write_endpoint, open_pkt).await;
        if let Err(e) = res.status {
            return Err(anyhow!("ADB Open Write Error: {:?}", e));
        }

        // Read OKAY
        let res = self
            .interface
            .bulk_in(self.read_endpoint, RequestBuffer::new(24))
            .await;
        if let Err(e) = res.status {
            return Err(anyhow!("ADB Write Header Error: {:?}", e));
        }
        let (cmd, remote_id, _, _, _, _) = AdbProtocol::parse_header(&res.data)?;
        if cmd != A_OKAY {
            return Err(anyhow!(
                "Failed to open remote file for writing: {:08X}",
                cmd
            ));
        }

        // Write data in chunks
        for chunk in file_data.chunks(4096) {
            let wrte_pkt = AdbProtocol::build_packet(A_WRTE, local_id, remote_id, chunk);
            let _ = self.interface.bulk_out(self.write_endpoint, wrte_pkt).await;

            // Wait for OKAY (Ack)
            let ack_res = self
                .interface
                .bulk_in(self.read_endpoint, RequestBuffer::new(24))
                .await;
            if let Err(e) = ack_res.status {
                return Err(anyhow!("ADB Write Ack Error: {:?}", e));
            }
            let (ack_cmd, _, _, _, _, _) = AdbProtocol::parse_header(&ack_res.data)?;
            if ack_cmd != A_OKAY {
                return Err(anyhow!("Write failed during transfer: {:08X}", ack_cmd));
            }
        }

        // Send CLSE
        let clse_pkt = AdbProtocol::build_packet(A_CLSE, local_id, remote_id, &[]);
        let _ = self.interface.bulk_out(self.write_endpoint, clse_pkt).await;

        Ok(())
    }
}
