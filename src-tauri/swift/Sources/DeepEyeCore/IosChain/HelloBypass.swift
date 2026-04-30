import Foundation

struct HelloBypass {
    let log: Logger

    func run(device: IosDeviceInfo) {
        let chipId   = device.chipId
        let chipName = device.chipName

        // A12+ → different flow
        if !device.isCheckm8 && !device.isDfu {
            log.error(
                "A12+ chip detected. Use server bypass instead.",
                layer: "UNSUPPORTED"
            )
            return
        }

        // Step 1: Enter DFU if not already
        if !device.isDfu {
            log.progress(15, "Entering DFU mode...")
            if !enterDfu() { return }
        }

        log.emit("dfu_ok")

        // Step 2: checkm8 via gaster
        log.progress(30, "Running checkm8 on \(chipName)...")
        if !runCheckm8(chipId: chipId, chipName: chipName) { return }

        // Step 3: Ramdisk via palera1n
        log.progress(60, "Booting bypass ramdisk...")
        if !bootRadisk() { return }

        // Step 4: Activation patch
        log.progress(85, "Patching activation record...")
        patchActivation()

        log.progress(100, "Hello screen bypass complete ✓")
        log.emit("complete", [
            "notes": [
                "WiFi: Active",
                "Signal: Requires full signal bypass",
                "Tethered: Re-run after power cycle",
            ]
        ])
    }

    // ── DFU Entry ─────────────────────────────────────────────

    private func enterDfu() -> Bool {
        let r = ProcessRunner.run("palera1n",
                                  args: ["--dfuhelper"],
                                  timeout: 60)
        if r.success || r.stdout.uppercased().contains("DFU") {
            log.emit("dfu_ok")
            return true
        }
        log.error("DFU entry failed: \(r.stderr.suffix(200))",
                  layer: "DFU", retryable: true)
        return false
    }

    // ── checkm8 via gaster ────────────────────────────────────

    private func runCheckm8(chipId: Int, chipName: String) -> Bool {
        // Try 1
        var r = ProcessRunner.run("gaster", args: ["pwn"], timeout: 30)

        if isPwned(r) {
            log.emit("checkm8_ok",
                     ["msg": "PWND:[checkm8] \(chipName)"])
            return true
        }

        // Retry once — checkm8 not 100% first attempt
        log.emit("retry", ["msg": "Retrying checkm8..."])
        Thread.sleep(forTimeInterval: 1.0)

        r = ProcessRunner.run("gaster", args: ["pwn"], timeout: 30)
        if isPwned(r) {
            log.emit("checkm8_ok",
                     ["msg": "PWND:[checkm8] \(chipName) (retry)"])
            return true
        }

        log.error(
            "checkm8 failed. Re-enter DFU mode and retry.",
            layer: "EXPLOIT", retryable: true
        )
        return false
    }

    private func isPwned(_ r: ProcessResult) -> Bool {
        r.success ||
        r.stdout.contains("PWND") ||
        r.stdout.lowercased().contains("pwned")
    }

    // ── Ramdisk via palera1n ──────────────────────────────────

    private func bootRadisk() -> Bool {
        let r = ProcessRunner.run("palera1n",
                                  args: [
                                      "--no-colors",
                                      "-e", "rootdev=md0",
                                      "--skip-fakefs",
                                  ],
                                  timeout: 120)

        let success = r.success ||
                      r.stdout.lowercased().contains("done") ||
                      r.stdout.lowercased().contains("success")

        if success {
            log.emit("ramdisk_ok")
            return true
        }

        log.error("Ramdisk boot failed: \(r.stderr.suffix(200))",
                  layer: "RAMDISK")
        return false
    }

    // ── Activation patch ──────────────────────────────────────

    private func patchActivation() {
        // Method A: ideviceactivation
        var r = ProcessRunner.run("ideviceactivation",
                                  args: ["activate"], timeout: 60)
        if r.success {
            log.emit("activation_ok", ["method": "ideviceactivation"])
            return
        }

        // Method B: with debug flag
        r = ProcessRunner.run("ideviceactivation",
                              args: ["-d", "activate"], timeout: 60)
        if r.success {
            log.emit("activation_ok", ["method": "ideviceactivation_debug"])
            return
        }

        // Partial success — WiFi still works
        log.emit("activation_partial", [
            "msg": "Activation methods failed. WiFi bypass may still work.",
            "wifi": true,
        ])
    }
}
