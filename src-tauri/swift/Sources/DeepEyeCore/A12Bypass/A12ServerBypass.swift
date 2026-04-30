import Foundation

// Environment variable for server URLs
// Set at build time or runtime
private let servers: [String] = {
    let primary   = ProcessInfo.processInfo.environment["BYPASS_SERVER_URL"]
                    ?? ""
    let secondary = ProcessInfo.processInfo.environment["BYPASS_SERVER_FALLBACK"]
                    ?? ""
    return [primary, secondary].filter { !$0.isEmpty }
}()

struct A12ServerBypass {
    let log:     Logger
    let network: NetworkClient
    let cache:   TokenCache

    // ── Full Signal Bypass ────────────────────────────────────

    func fullSignal(
        ecid:    String,
        imei:    String,
        imei2:   String  = "",
        serial:  String  = "",
        ios:     String  = "",
        model:   String  = ""
    ) {
        log.progress(10, "Validating IMEI...")

        guard luhnCheck(imei) else {
            log.error("IMEI failed Luhn validation", layer: "VALIDATION")
            return
        }

        log.progress(20, "Checking device eligibility...")

        let eligResp = network.post(
            servers:  servers,
            endpoint: "/v2/device/check",
            body:     ["ecid": ecid, "model": model,
                       "ios_version": ios, "session_id": log.sessionId]
        )

        if eligResp?["eligible"] as? Bool == false {
            log.error(
                eligResp?["reason"] as? String ?? "Device not eligible",
                layer: "ELIGIBILITY"
            )
            return
        }

        log.progress(35, "Getting activation token...")

        // Use cached token if valid
        let token: String
        if let cached = cache.get(ecid) {
            token = cached
            log.emit("cache_hit", ["msg": "Using cached token"])
        } else {
            guard let resp = network.post(
                servers:  servers,
                endpoint: "/v2/bypass/full-signal",
                body:     [
                    "ecid": ecid, "imei": imei, "imei2": imei2,
                    "serial": serial, "ios_version": ios,
                    "model": model, "bypass_type": "full_signal",
                    "session_id": log.sessionId,
                ]
            ), let t = resp["token"] as? String else {
                log.error(
                    "Token request failed — server unreachable",
                    layer: "SERVER", retryable: true
                )
                return
            }
            token = t
            cache.set(ecid, token: token)
        }

        log.progress(55, "Registering IMEI for signal...")

        let imeiResp = network.post(
            servers:  servers,
            endpoint: "/v2/imei/register-signal",
            body:     [
                "ecid": ecid, "imei": imei, "imei2": imei2,
                "token": token, "session_id": log.sessionId,
            ]
        )

        if imeiResp?["success"] as? Bool != true {
            log.error(
                imeiResp?["error"] as? String ?? "IMEI registration failed",
                layer: "IMEI_REG"
            )
            return
        }

        log.progress(80, "Applying activation record...")

        network.post(
            servers:  servers,
            endpoint: "/v2/activation/apply",
            body:     ["ecid": ecid, "token": token,
                       "session_id": log.sessionId]
        )

        log.progress(100, "Full signal bypass complete ✓")
        log.emit("bypass_complete", [
            "method":        "SERVER_FULL_SIGNAL_SWIFT",
            "signalEnabled": true,
            "untethered":    true,
        ])
    }

    // ── Fake Erase Bypass ────────────────────────────────────

    func fakeErase(
        ecid:   String,
        imei:   String  = "",
        serial: String  = "",
        ios:    String  = "",
        model:  String  = ""
    ) {
        log.progress(15, "Requesting fake erase token...")

        guard let resp = network.post(
            servers:  servers,
            endpoint: "/v2/bypass/fake-erase",
            body:     [
                "ecid": ecid, "imei": imei, "serial": serial,
                "ios_version": ios, "model": model,
                "bypass_type": "fake_erase",
                "session_id": log.sessionId,
            ]
        ), let token = resp["token"] as? String else {
            log.error("Fake Erase token failed", layer: "SERVER")
            return
        }

        log.progress(60, "Applying fake erase record...")

        network.post(
            servers:  servers,
            endpoint: "/v2/activation/apply",
            body:     ["ecid": ecid, "token": token,
                       "erase_type": "fake", "session_id": log.sessionId]
        )

        log.progress(100, "Fake Erase complete — data preserved ✓")
        log.emit("bypass_complete", [
            "method":        "SERVER_FAKE_ERASE_SWIFT",
            "signalEnabled": false,
            "untethered":    true,
            "dataLoss":      false,
        ])
    }

    // ── WiFi Only ────────────────────────────────────────────

    func wifiOnly(ecid: String, serial: String = "", ios: String = "") {
        log.progress(20, "Requesting WiFi bypass token...")

        guard let resp = network.post(
            servers:  servers,
            endpoint: "/v2/bypass/token",
            body:     ["ecid": ecid, "serial": serial,
                       "ios_version": ios, "bypass_type": "wifi_only",
                       "session_id": log.sessionId]
        ), let token = resp["token"] as? String else {
            log.error("WiFi bypass failed", layer: "SERVER")
            return
        }

        log.progress(70, "Applying WiFi bypass...")
        network.post(
            servers:  servers,
            endpoint: "/v2/activation/apply",
            body:     ["ecid": ecid, "token": token,
                       "session_id": log.sessionId]
        )

        log.progress(100, "WiFi bypass complete ✓")
        log.emit("bypass_complete", [
            "method":        "SERVER_WIFI_SWIFT",
            "signalEnabled": false,
            "untethered":    true,
        ])
    }

    // ── Luhn check ───────────────────────────────────────────

    private func luhnCheck(_ imei: String) -> Bool {
        guard imei.count == 15,
              imei.allSatisfy({ $0.isNumber })
        else { return false }
        var sum = 0
        for (i, ch) in imei.reversed().enumerated() {
            var d = ch.wholeNumberValue!
            if i % 2 == 1 { d *= 2; if d > 9 { d -= 9 } }
            sum += d
        }
        return sum % 10 == 0
    }
}
