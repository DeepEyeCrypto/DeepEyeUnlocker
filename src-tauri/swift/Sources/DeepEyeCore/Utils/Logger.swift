import Foundation

// JSON events to stdout — consumed by Tauri Rust command
// Same protocol as previous Python emitter
// Each line = one JSON event

struct Logger {
    let sessionId: String

    func emit(_ event: String, _ extra: [String: Any] = [:]) {
        var payload: [String: Any] = ["event": event, "sessionId": sessionId]
        payload.merge(extra) { _, new in new }

        guard let data = try? JSONSerialization.data(
            withJSONObject: payload, options: .sortedKeys
        ),
        let line = String(data: data, encoding: .utf8) else { return }

        print(line)           // stdout → Tauri reads this
        fflush(stdout)        // flush immediately (critical for streaming)
    }

    func progress(_ pct: Int, _ phase: String) {
        emit("progress", ["pct": pct, "phase": phase])
    }

    func error(_ reason: String, layer: String = "UNKNOWN",
               retryable: Bool = false) {
        emit("error", [
            "reason":    reason,
            "layer":     layer,
            "retryable": retryable,
        ])
    }

    func success(_ msg: String, extra: [String: Any] = [:]) {
        var d = extra; d["msg"] = msg
        emit("success", d)
    }
}
