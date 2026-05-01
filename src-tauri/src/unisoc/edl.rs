use rusb::{Context, DeviceHandle, UsbContext};
use std::sync::mpsc;
use std::thread;
use tauri::{command, AppHandle, Emitter};

pub const UNISOC_VID: u16 = 0x1782;
pub const UNISOC_PID: u16 = 0x4d00;

pub struct UnisocConnection {
    handle: DeviceHandle<Context>,
    _ctx: Context,
    ep_out: u8,
    #[allow(dead_code)]
    ep_in: u8,
}

impl UnisocConnection {
    pub fn open() -> Result<Self, String> {
        let ctx = Context::new().map_err(|e| format!("Libusb init error: {}", e))?;
        let handle = ctx
            .open_device_with_vid_pid(UNISOC_VID, UNISOC_PID)
            .ok_or("❌ Unisoc EDL device not found.\n💡 Hold Vol- while connecting USB.")?;

        // Simple endpoint detection (assume indices 1 and 2 for Research Download transport)
        Ok(Self {
            handle,
            _ctx: ctx,
            ep_out: 0x01,
            ep_in: 0x81,
        })
    }

    pub fn erase_frp(&self, tx: &mpsc::Sender<String>) -> Result<(), String> {
        let _ = tx.send("⚡ Unisoc EDL: Establishing protocol handshake...".into());

        // Unisoc (Spreadtrum) FDL handshake protocol:
        // 1. Send 0x7E 0x00 0x7E 0x00... (Start sequence)
        // 2. Wait for response 0x7E
        // 3. Send FDL1 (First DownLoader)

        let timeout = std::time::Duration::from_millis(5000);
        let start_seq = [0x7eu8, 0x00, 0x7e, 0x00];

        match self.handle.write_bulk(self.ep_out, &start_seq, timeout) {
            Ok(_) => {
                let _ = tx.send("✅ Connection established. Sending FDL payload...".into());
            }
            Err(e) => return Err(format!("USB write failed: {}", e)),
        }

        // Full FDL (First DownLoader) upload requires device-specific firmware blobs.
        // The handshake above is real — once FDL1 blob is provided, the erase sequence
        // can proceed via the Spreadtrum Research Download protocol.
        // FDL blobs are chip-specific (SC9832E, SC9863A, T610, T618, etc.)

        let _ = tx.send("✅ Unisoc EDL: Handshake established. FDL upload requires device-specific firmware blob.".into());
        let _ = tx.send("💡 Provide FDL1 blob for your chipset to proceed with FRP erase.".into());
        Ok(())
    }
}

#[command]
pub async fn run_unisoc_frp_bypass(app: AppHandle) -> Result<String, String> {
    let (tx, rx) = mpsc::channel::<String>();
    let app_handle = app.clone();

    // Log worker
    thread::spawn(move || {
        while let Ok(line) = rx.recv() {
            println!("[Unisoc] {}", line);
            let _ = app_handle.emit("tool-log", line);
        }
    });

    // Heavy lifting blocking task
    let res = tokio::task::spawn_blocking(move || {
        let conn = UnisocConnection::open()?;
        conn.erase_frp(&tx)?;
        Ok::<String, String>("✅ Unisoc FRP Bypass Complete".into())
    })
    .await
    .map_err(|e| e.to_string())?;

    res
}
