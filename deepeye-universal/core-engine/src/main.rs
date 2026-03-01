use deepeyecore::connection::usb::UsbManager;

fn main() -> anyhow::Result<()> {
    deepeyecore::init();
    tracing::info!("Starting DeepEye Universal CLI Tester");

    let usb_mgr = UsbManager::new();
    let devices = usb_mgr.scan_devices()?;

    for dev in devices {
        println!("DETECTED Target Device: {}", dev);
    }

    Ok(())
}
