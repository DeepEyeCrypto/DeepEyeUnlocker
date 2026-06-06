import Foundation

// CLI: deepeye-core <command> <args...>
// Commands:
//   detect <session_id>
//   hello-bypass <session_id>
//   full-signal <ecid> <imei> [imei2] [serial] [ios] [model] <session_id>
//   fake-erase  <ecid> <imei> [serial] [ios] [model] <session_id>
//   wifi-bypass <ecid> [serial] [ios] <session_id>

let args = CommandLine.arguments
guard args.count >= 3 else {
    print("""
Usage: deepeye-core <command> [args] <session_id>
Commands: detect | hello-bypass | full-signal | fake-erase | wifi-bypass
""")
    exit(1)
}

let command   = args[1]
let sessionId = args.last ?? "unknown"
let log       = Logger(sessionId: sessionId)
let cache     = TokenCache()
let network   = NetworkClient(sessionId: sessionId)
let bypass    = A12ServerBypass(log: log, network: network, cache: cache)
let detector  = DeviceDetector(log: log)
let helloBP   = HelloBypass(log: log)

switch command {

case "detect":
    if let device = detector.detect() {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        if let data = try? encoder.encode(device),
           let json = String(data: data, encoding: .utf8) {
            print(json)
        }
    }

case "hello-bypass":
    if let device = detector.detect() {
        helloBP.run(device: device)
    }

case "full-signal":
    guard args.count >= 4 else { exit(1) }
    bypass.fullSignal(
        ecid:   args[2],
        imei:   args[3],
        imei2:  args.count > 5 ? args[4] : "",
        serial: args.count > 6 ? args[5] : "",
        ios:    args.count > 7 ? args[6] : "",
        model:  args.count > 8 ? args[7] : ""
    )

case "fake-erase":
    guard args.count >= 4 else { exit(1) }
    bypass.fakeErase(
        ecid:   args[2],
        imei:   args[3],
        serial: args.count > 5 ? args[4] : "",
        ios:    args.count > 6 ? args[5] : "",
        model:  args.count > 7 ? args[6] : ""
    )

case "wifi-bypass":
    guard args.count >= 3 else { exit(1) }
    bypass.wifiOnly(
        ecid:   args[2],
        serial: args.count > 4 ? args[3] : "",
        ios:    args.count > 5 ? args[4] : ""
    )

case "f3arrain":
    let engine = F3arRa1nEngine(resourcesPath: ".", log: log)
    await engine.runFullChain(sessionId: sessionId)

case "f3arrain-detect":
    let engine = F3arRa1nEngine(resourcesPath: ".", log: log)
    do {
        let device = try await engine.detectDevice(sessionId: sessionId)
        let enc    = JSONEncoder()
        enc.keyEncodingStrategy = .convertToSnakeCase
        if let data = try? enc.encode(device),
           let json = String(data: data, encoding: .utf8) {
            print(json)
        }
    } catch {
        log.error(error.localizedDescription, layer: "DETECT")
    }

case "f3arrain-checkm8":
    // Just checkm8 step — for testing
    let engine = F3arRa1nEngine(resourcesPath: ".", log: log)
    do {
        let device = try await engine.detectDevice(sessionId: sessionId)
        if !device.isDfu {
            try await engine.enterDfu(cpid: device.cpid, sessionId: sessionId)
        }
        try await engine.runCheckm8(cpid: device.cpid, sessionId: sessionId)
    } catch {
        log.error(error.localizedDescription, layer: "CHECKM8",
                  retryable: true)
    }

default:
    log.error("Unknown command: \(command)", layer: "CLI")
    exit(1)
}
