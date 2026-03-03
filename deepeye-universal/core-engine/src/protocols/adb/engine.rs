use super::protocol::AdbProtocol;
use super::session::AdbSession;
use anyhow::Result;
use std::collections::HashMap;

pub struct AdbEngine {
    pub session: Option<AdbSession>,
}

impl AdbEngine {
    pub fn new() -> Self {
        Self { session: None }
    }

    pub fn with_session(session: AdbSession) -> Self {
        Self {
            session: Some(session),
        }
    }

    pub fn build_message(command: u32, arg0: u32, arg1: u32, data: &[u8]) -> Vec<u8> {
        AdbProtocol::build_packet(command, arg0, arg1, data)
    }

    pub async fn get_device_properties(&self) -> Result<HashMap<String, String>> {
        if let Some(session) = &self.session {
            let output = session.shell_exec("getprop").await?;
            let mut props = HashMap::new();
            for line in output.lines() {
                if let Some((key, value)) = line.split_once(':') {
                    let k = key.trim().trim_matches('[').trim_matches(']');
                    let v = value.trim().trim_matches('[').trim_matches(']');
                    props.insert(k.to_string(), v.to_string());
                }
            }
            return Ok(props);
        }

        // Sim fallback
        let mut props = HashMap::new();
        props.insert("ro.product.model".to_string(), "SM-S928B".to_string());
        props.insert("ro.product.brand".to_string(), "Samsung".to_string());
        props.insert("ro.build.version.release".to_string(), "14".to_string());
        props.insert("ro.serialno".to_string(), "R5CW20PXXXX".to_string());
        props.insert("ro.crypto.state".to_string(), "encrypted".to_string());
        props.insert("ro.secure".to_string(), "1".to_string());
        props.insert("sys.usb.state".to_string(), "adb,acm,mtp".to_string());

        Ok(props)
    }

    pub async fn execute_shell_command(&self, command: &str) -> Result<String> {
        if let Some(session) = &self.session {
            return session.shell_exec(command).await;
        }
        // Placeholder for future RAW ADB interaction logic
        println!("[DeepEye Core] ADB SHELL EXEC: {}", command);
        Ok(format!("Success executing: {}", command))
    }

    pub async fn list_packages(&self) -> Result<Vec<String>> {
        if let Some(session) = &self.session {
            let output = session.shell_exec("pm list packages").await?;
            return Ok(output
                .lines()
                .filter_map(|l| l.strip_prefix("package:"))
                .map(|s| s.to_string())
                .collect());
        }
        // Simulated output for 'pm list packages'
        Ok(vec![
            "com.android.chrome".into(),
            "com.google.android.youtube".into(),
            "com.samsung.android.messaging".into(),
            "com.deepeye.unlocker.helper".into(),
            "com.facebook.katana".into(),
            "com.whatsapp".into(),
        ])
    }

    pub async fn install_apk(&self, path: &str) -> Result<String> {
        if let Some(session) = &self.session {
            let remote_path = "/data/local/tmp/deepeye_helper.apk";
            println!("[DeepEye Core] Pushing APK to device...");
            session.push_file(path, remote_path).await?;

            println!("[DeepEye Core] Invoking Package Manager...");
            let install_res = session
                .shell_exec(&format!("pm install -r {}", remote_path))
                .await?;

            // Cleanup
            let _ = session.shell_exec(&format!("rm {}", remote_path)).await;

            return Ok(install_res);
        }

        // Simulated output for 'pm install'
        println!("[DeepEye Core] ADB INSTALLING APK FROM (SIM): {}", path);
        Ok(format!("Success: Package installed from {}", path))
    }
}
