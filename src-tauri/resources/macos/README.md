# macOS installer resources

This directory is bundled into the macOS application via `resources/macos/**/*` in `src-tauri/tauri.conf.json`.

## PKG installer support

- `pkg/postinstall` runs after the `.pkg` payload is installed.
- The post-install script normalizes permissions on `/Applications/DeepEyeUnlocker.app`.
- It creates `/Library/Application Support/DeepEyeUnlocker` and `/Library/Logs/DeepEyeUnlocker`.
- If `com.deepeye.unlocker.usb-monitor.plist` is bundled inside the app resources later, the installer reloads it as a system LaunchDaemon.

The `.pkg` artifact itself is produced by `bash ./scripts/build_macos_pkg.sh`, which wraps the signed `.app` bundle created by Tauri.
