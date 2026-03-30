# Windows bundled tools

Place universal Windows executables used by DeepEyeUnlocker runtime commands in this directory.

Expected filenames:

- `adb.exe`
- `irecovery.exe`
- `ideviceinfo.exe`
- `idevicepair.exe`
- `idevicerestore.exe`
- `iproxy.exe` (optional)

These files are bundled by Tauri via `resources/windows/*` in `tauri.conf.json`.
