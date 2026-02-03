# Threat Model & Hardening: DeepEye Portable (OTG)

## 1. Threat Landscape

### 1.1 Host Compromises (The Host Phone)

* **Threat**: Malicious OTG Target attempts to exploit the Host's USB stack to gain root access.
* **Mitigation**: The Host app uses ONLY official `UsbManager` APIs. Native code operates on a restricted File Descriptor (FD). No raw device access is allowed.
* **Hardening**: Implement **USB Isolation Gates** in the native transport to drop non-protocol packets.

### 1.2 Binary Integrity

* **Threat**: `magiskboot` or `ksu_patcher` binaries are replaced with malformed versions to perform host-side data theft.
* **Mitigation**: All embedded binaries are signed and stored in the APK's read-only assets. On extraction, SHA-256 hashes are verified before execution.
* **Hardening**: Use **Seal-on-Extract** logic (Set `immutable` attribute via `chattr` if host supports it, though rare on non-root).

### 1.3 Privilege Escalation

* **Threat**: The app is tricked into executing commands as Root on the Host.
* **Mitigation**: THE APP NEVER ASKS FOR ROOT. By design, the architecture is host-rootless. Native code runs within the app's standard UID sandbox.

---

## 2. Hardening Recommendations

### 2.1 Native Code Security (C++)

1. **ASLR/DEP**: Ensure the `deepeye_core.so` is compiled with `-fstack-protector-all` and `-D_FORTIFY_SOURCE=2`.
2. **Buffer Safety**: Replace all `memcpy` calls in protocol parsers (EDL/BROM) with bounds-checked alternatives.
3. **Symbol Stripping**: All internal protocol functions should be marked `static` or HIDDEN to prevent reverse engineering of proprietary algorithms.

### 2.2 Android App Security (Kotlin)

1. **Native Library Root Check**: The app should refuse to load `deepeye_core.so` if the Host environment is detected as "compromised" (unexpected system hooks).
2. **Certificate Pinning**: If the app downloads GSI images, use SSL Pinning for the Google/Xiaomi servers.
3. **Task Isolation**: Run the native OTG transport in a dedicated `:otg_service` process with `android:isolatedProcess="true"` if File Descriptor passing can be maintained (requires `ContentProvider` or `Binder` FD transfer).

### 2.3 OTG-Specific Safety

1. **Safety Timer**: Implement a hardware-level communications timeout (e.g., 30 seconds). If a protocol handshake doesn't complete, the FD is closed and the USB connection is severed to prevent "Vampire" power drain or exhaustive fuzzing.
2. **VID/PID Whitelist**: Only allow communication with known Service Mode IDs (0x9008, 0x0003, etc.).
