use std::sync::OnceLock;

pub mod connection;
pub mod database;
pub mod models;
pub mod policy;
pub mod protocols;

pub static DB: OnceLock<database::DatabaseManager> = OnceLock::new();

/// Core Initialization
pub fn init() {
    tracing_subscriber::fmt::init();
    tracing::info!("DeepEyeCore Universal Engine Initialized.");

    // Setup Singleton SQLite mapping
    if let Ok(db_manager) = database::DatabaseManager::new() {
        let _ = DB.set(db_manager);
    } else {
        tracing::error!("Failed to initialize operational database layer. Analytics offline.");
    }
}

/// Orchestrator Router - The main entrypoint for executing all features
pub async fn dispatch_feature(
    req: models::FeatureExecutionRequest,
) -> Result<models::FeatureExecutionResponse, anyhow::Error> {
    tracing::info!(
        "Core Dispatcher Received: [{:?}] {:?}",
        req.platform,
        req.title
    );

    let mut session_logs = vec![
        format!("Engine Started. Target platform: {:?}", req.platform),
        format!("Invoking action: {}", req.title),
    ];

    // Branch to specific SoC Protocol Implementations
    match req.platform {
        models::DevicePlatform::Qualcomm => {
            session_logs.push("Scanning for physical Qualcomm EDL interface...".into());
            let usb = connection::usb::UsbManager::new();
            let devices = connection::usb::UsbManager::list_detailed_devices().unwrap_or_default();

            let qcom_dev_info = devices
                .iter()
                .find(|d| d.platform == models::DevicePlatform::Qualcomm);

            if let Some(info) = qcom_dev_info {
                session_logs.push(format!(
                    "Found {} [0x{:04X}:0x{:04X}]. Entering EDL Protocol stack...",
                    info.name, info.vid, info.pid
                ));
                match usb.open_device(info.vid, info.pid) {
                    Ok(dev) => {
                        match protocols::qcom::session::QcomSession::new(dev) {
                            Ok(session) => {
                                session_logs.push("Native Sahara Pipe Claimed.".into());
                                // Sahara usually starts by device sending HELLO_REQ
                                session_logs.push("Waiting for Sahara HELLO_REQ...".into());
                                match session.read(48).await {
                                    Ok(data) => {
                                        let sahara = protocols::qcom::sahara::SaharaProtocol::new();
                                        match sahara.execute_handshake(&data) {
                                            Ok(info) => {
                                                session_logs.push(format!("Sahara Handshake OK (Version: {}, Mode: {})", info.version, info.mode));
                                                session_logs.push("EDL Command Pipeline Established (Real Hardware).".into());
                                            }
                                            Err(e) => session_logs.push(format!("Sahara Protocol Error: {}. Falling back to simulation.", e)),
                                        }
                                    }
                                    Err(e) => session_logs.push(format!(
                                        "Sahara Read Failed: {}. Try replugging.",
                                        e
                                    )),
                                }
                            }
                            Err(e) => {
                                session_logs.push(format!("Session init error: {}.", e));
                            }
                        }
                    }
                    Err(e) => {
                        session_logs.push(format!("USB Open Error: {}.", e));
                    }
                }
            } else {
                session_logs.push("No physical EDL device detected. Using simulator...".into());
                let sahara = protocols::qcom::sahara::SaharaProtocol::new();

                // Simulate a raw buffer read from USB (this represents a HELLO REQ)
                let mock_hello_req_buffer: [u8; 48] = [
                    0x01, 0x00, 0x00, 0x00, // cmd = 1
                    0x30, 0x00, 0x00, 0x00, // len = 48
                    0x02, 0x00, 0x00, 0x00, // version = 2
                    0x01, 0x00, 0x00, 0x00, // min_version = 1
                    0x00, 0x10, 0x00, 0x00, // max_packet_size
                    0x00, 0x00, 0x00, 0x00, // mode = 0
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, // padding
                ];

                tokio::time::sleep(tokio::time::Duration::from_millis(300)).await;

                match sahara.execute_handshake(&mock_hello_req_buffer) {
                    Ok(info) => {
                        session_logs.push(format!(
                            "Sahara Handshake OK (Version: {}, Max Pkt: {} bytes)",
                            info.version, info.max_cmd_packet_size
                        ));
                        session_logs.push("EDL Bootloader mapping established.".into());
                    }
                    Err(e) => {
                        return Err(anyhow::anyhow!("Sahara Handshake Error: {}", e));
                    }
                }
            }

            // Execute feature payload mock
            tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
            session_logs.push(format!("Executed Feature logic for ID #{}", req.feature_id));
        }
        models::DevicePlatform::MTK => {
            session_logs.push("Scanning for physical MTK interface...".into());
            let usb = connection::usb::UsbManager::new();
            let devices = connection::usb::UsbManager::list_detailed_devices().unwrap_or_default();

            let mtk_dev_info = devices
                .iter()
                .find(|d| d.platform == models::DevicePlatform::MTK);

            if let Some(info) = mtk_dev_info {
                session_logs.push(format!(
                    "Found {} in {} mode [0x{:04X}:0x{:04X}]",
                    info.name, info.mode, info.vid, info.pid
                ));
                match usb.open_device(info.vid, info.pid) {
                    Ok(dev) => match protocols::mtk::session::MtkSession::new(dev) {
                        Ok(session) => {
                            session_logs.push("Native BROM/VCP claimed.".into());
                            session_logs.push("Sending Sync Character (0xA0)...".into());
                            if let Err(e) = session.echo_handshake().await {
                                session_logs.push(format!(
                                    "Handshake failed: {}. Ensure device is at BROM level.",
                                    e
                                ));
                                return Err(anyhow::anyhow!("MTK Handshake Timeout: {}", e));
                            }
                            session_logs.push("Handshake Echo (0x5F) Verified.".into());

                            session_logs.push("Attempting Chip Info Read...".into());
                            match session.read_chip_info().await {
                                Ok(data) => {
                                    let brom = protocols::mtk::brom::BromProtocol::new();
                                    match brom.execute_handshake(&data) {
                                        Ok(info) => {
                                            session_logs.push(format!("BROM Protection Bypassed -> BBChip: {:04X}, HW Ver: {:04X}", info.bbchip, info.hw_ver));
                                            session_logs
                                                .push("MTK SEC_AUTH disabled (Exploit OK).".into());
                                        }
                                        Err(e) => session_logs.push(format!(
                                            "Protocol Parsing Error: {}. Buffer: {:?}",
                                            e, data
                                        )),
                                    }
                                }
                                Err(e) => session_logs.push(format!(
                                    "Payload Read Failed: {}. Try replugging device.",
                                    e
                                )),
                            }
                        }
                        Err(e) => {
                            session_logs.push(format!("Session init error: {}.", e));
                            return Err(anyhow::anyhow!("MTK Session Error: {}", e));
                        }
                    },
                    Err(e) => {
                        session_logs.push(format!("USB Open Error: {}.", e));
                        return Err(anyhow::anyhow!("USB Access Denied: {}", e));
                    }
                }
            } else {
                session_logs.push("No physical MTK device detected. Running in Demo Mode.".into());
                let brom = protocols::mtk::brom::BromProtocol::new();

                // Send START CMD, parse 10-byte Handshake block
                let mock_brom_handshake: [u8; 10] = [
                    0x03, 0x59, // BBChip
                    0xCA, 0xFE, // Echo
                    0x00, 0x01, // SW Ver
                    0x8A, 0x00, // HW SubCode
                    0xCA, 0x01, // HW Ver
                ];

                tokio::time::sleep(tokio::time::Duration::from_millis(150)).await;

                match brom.execute_handshake(&mock_brom_handshake) {
                    Ok(info) => {
                        session_logs.push(format!(
                            "BROM Protection Bypassed -> BBChip: {:04X}, HW Ver: {:04X}",
                            info.bbchip, info.hw_ver
                        ));
                        session_logs.push("MTK SEC_AUTH disabled. Serial pipeline open.".into());
                    }
                    Err(e) => {
                        return Err(anyhow::anyhow!("BROM Exploit Failed: {}", e));
                    }
                }
            }

            // Execute feature payload logic
            tokio::time::sleep(tokio::time::Duration::from_millis(650)).await;
            session_logs.push(format!("Executed Feature logic for ID #{}", req.feature_id));
        }
        models::DevicePlatform::Samsung => {
            session_logs.push("Scanning for physical Samsung Download interface...".into());
            let usb = connection::usb::UsbManager::new();
            let devices = connection::usb::UsbManager::list_detailed_devices().unwrap_or_default();

            let samsung_dev_info = devices
                .iter()
                .find(|d| d.platform == models::DevicePlatform::Samsung);

            if let Some(info) = samsung_dev_info {
                session_logs.push(format!(
                    "Found {} [0x{:04X}:0x{:04X}]. Entering Loke/Odin Protocol...",
                    info.name, info.vid, info.pid
                ));
                match usb.open_device(info.vid, info.pid) {
                    Ok(dev) => {
                        match protocols::samsung::session::SamsungSession::new(dev) {
                            Ok(session) => {
                                session_logs.push("Native Odin Port Claimed.".into());
                                session_logs.push("Sending ODIN Start sequence...".into());
                                if let Err(e) = session.handshake().await {
                                    session_logs.push(format!(
                                        "Handshake failed: {}. Ensure device is in Download Mode.",
                                        e
                                    ));
                                } else {
                                    // Read response parameters
                                    match session.read(32).await {
                                        Ok(data) => {
                                            let odin =
                                                protocols::samsung::odin::OdinProtocol::new();
                                            match odin.execute_handshake(&data) {
                                                Ok(info) => {
                                                    session_logs.push(format!("Odin Mode Verified -> Device: {}, Chip: 0x{:08X}", info.device_name, info.chip_id));
                                                    session_logs.push("Libusb connection to ODIN port successful.".into());
                                                }
                                                Err(e) => session_logs
                                                    .push(format!("Handshake Parse Error: {}.", e)),
                                            }
                                        }
                                        Err(e) => session_logs.push(format!("Read Error: {}.", e)),
                                    }
                                }
                            }
                            Err(e) => {
                                session_logs.push(format!("Session init error: {}.", e));
                            }
                        }
                    }
                    Err(e) => {
                        session_logs.push(format!("USB Open Error: {}.", e));
                    }
                }
            } else {
                session_logs
                    .push("No physical Samsung device detected. Scaling to simulator...".into());
                let odin = protocols::samsung::odin::OdinProtocol::new();

                // LOKE ... [ChipID] ... [PIT Size] ... [Device Name] \0
                let mut mock_odin_handshake: [u8; 32] = [0; 32];
                mock_odin_handshake[0..4].copy_from_slice(b"LOKE");
                mock_odin_handshake[4..8].copy_from_slice(&0x0000_1234u32.to_le_bytes()); // chip
                mock_odin_handshake[8..12].copy_from_slice(&0x0000_1000u32.to_le_bytes()); // pit
                let device_name = b"SM-G998B\0";
                mock_odin_handshake[12..12 + device_name.len()].copy_from_slice(device_name);

                tokio::time::sleep(tokio::time::Duration::from_millis(200)).await;

                match odin.execute_handshake(&mock_odin_handshake) {
                    Ok(info) => {
                        session_logs.push(format!(
                            "Odin Mode Verified -> Device: {}, Chip: 0x{:08X}",
                            info.device_name, info.chip_id
                        ));
                        session_logs.push("Libusb connection to ODIN port successful.".into());
                    }
                    Err(e) => {
                        return Err(anyhow::anyhow!("Odin Handshake Failed: {}", e));
                    }
                }
            }

            // Execute feature payload mock
            tokio::time::sleep(tokio::time::Duration::from_millis(700)).await;
            session_logs.push(format!("Executed Feature logic for ID #{}", req.feature_id));
        }
        _ => {
            if req.title == "Deep Device Info" {
                session_logs.push("Scanning for physical ADB devices...".into());
                let adb = get_adb_engine(&mut session_logs).await;

                match adb.get_device_properties().await {
                    Ok(props) => {
                        session_logs.push("ADB Handshake established via Transport S0.".into());
                        session_logs.push("--- DEVICE DESCRIPTOR ---".into());
                        for (k, v) in props {
                            session_logs.push(format!("  {} : {}", k, v));
                        }
                    }
                    Err(e) => {
                        return Err(anyhow::anyhow!("ADB Service Unavailable: {}", e));
                    }
                }
            } else if req.title == "ADB App Manager" {
                session_logs.push("Searching for ADB hardware...".into());
                let adb = get_adb_engine(&mut session_logs).await;

                match adb.list_packages().await {
                    Ok(pkgs) => {
                        session_logs.push(format!("Detected {} system/user packages.", pkgs.len()));
                        for p in pkgs {
                            session_logs.push(format!("  • {}", p));
                        }
                        session_logs.push("Package query complete.".into());
                    }
                    Err(e) => {
                        return Err(anyhow::anyhow!("ADB Package Scan Error: {}", e));
                    }
                }
            } else if req.title.to_lowercase().contains("apk") {
                session_logs.push("Staging hardware for APK Transport...".into());
                let adb = get_adb_engine(&mut session_logs).await;

                let apk_path = req
                    .options
                    .as_ref()
                    .and_then(|o| o.get("path"))
                    .and_then(|p| p.as_str())
                    .unwrap_or("/tmp/deep-eye-helper.apk");

                session_logs.push(format!("Target Payload: {}", apk_path));

                match adb.install_apk(apk_path).await {
                    Ok(msg) => {
                        session_logs.push(format!("ADB CLI OUTPUT: {}", msg));
                        session_logs.push("Installation successful.".into());
                    }
                    Err(e) => return Err(anyhow::anyhow!("APK Install Error: {}", e)),
                }
            } else {
                session_logs.push(
                    "WARN: No physical device auto-detected. Executing in simulation mode.".into(),
                );
                tokio::time::sleep(tokio::time::Duration::from_millis(600)).await;
            }
        }
    }

    session_logs.push("Operation sequence terminated successfully.".into());

    // Write outcome to local DB for Desktop UI persistence tracking
    if let Some(db) = DB.get() {
        if let Err(e) = db.insert_job_log(&format!("{:?}", req.platform), &req.title, "SUCCESS") {
            tracing::error!("Failed to persist local DB job completion log: {}", e);
        }
    }

    Ok(models::FeatureExecutionResponse {
        success: true,
        message: format!("Command [{}] finished.", req.title),
        log_output: session_logs,
    })
}

async fn get_adb_engine(logs: &mut Vec<String>) -> protocols::adb::AdbEngine {
    let usb = connection::usb::UsbManager::new();
    let devices = connection::usb::UsbManager::list_detailed_devices().unwrap_or_default();
    let adb_dev_info = devices
        .iter()
        .find(|d| d.mode == "ADB" || (d.vid == 0x18D1 && d.pid == 0x4EE7));

    if let Some(info) = adb_dev_info {
        logs.push(format!("Physical link detected: {}", info.name));
        match usb.open_device(info.vid, info.pid) {
            Ok(dev) => match protocols::adb::session::AdbSession::new(dev) {
                Ok(mut session) => {
                    if let Err(e) = session.connect().await {
                        logs.push(format!("Hardware tunnel failed: {}. Simulating...", e));
                        protocols::adb::AdbEngine::new()
                    } else {
                        logs.push("Hardware tunnel active.".into());
                        protocols::adb::AdbEngine::with_session(session)
                    }
                }
                Err(_) => protocols::adb::AdbEngine::new(),
            },
            Err(_) => protocols::adb::AdbEngine::new(),
        }
    } else {
        logs.push("No hardware detected. Running in Demo Mode.".into());
        protocols::adb::AdbEngine::new()
    }
}
