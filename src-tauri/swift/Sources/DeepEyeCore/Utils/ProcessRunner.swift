import Foundation

struct ProcessResult {
    let exitCode: Int32
    let stdout:   String
    let stderr:   String
    var success:  Bool { exitCode == 0 }
}

struct ProcessRunner {

    static let toolsDir: URL = {
        // In bundled app: Contents/Resources/tools/
        // In development: src-tauri/resources/tools/
        let bundle = Bundle.main.resourceURL?
            .appendingPathComponent("tools")
        return bundle ?? URL(fileURLWithPath: "resources/tools")
    }()

    /// Run a subprocess and return result
    static func run(
        _ executable: String,
        args:    [String]  = [],
        env:     [String: String] = [:],
        timeout: TimeInterval = 30.0
    ) -> ProcessResult {
        let proc = Process()

        // Resolve tool path
        let toolPath = toolsDir.appendingPathComponent(executable)
        if FileManager.default.fileExists(atPath: toolPath.path) {
            proc.executableURL = toolPath
        } else if let systemPath = which(executable) {
            proc.executableURL = URL(fileURLWithPath: systemPath)
        } else {
            return ProcessResult(
                exitCode: -1, stdout: "",
                stderr: "\(executable) not found in tools/ or PATH"
            )
        }

        proc.arguments = args

        // Merge environment
        var environment = ProcessInfo.processInfo.environment
        env.forEach { environment[$0] = $1 }
        proc.environment = environment

        let stdoutPipe = Pipe()
        let stderrPipe = Pipe()
        proc.standardOutput = stdoutPipe
        proc.standardError  = stderrPipe

        do {
            try proc.run()
        } catch {
            return ProcessResult(exitCode: -1, stdout: "",
                                 stderr: "Launch failed: \(error)")
        }

        // Timeout handling
        let deadline = DispatchTime.now() + timeout
        let result   = DispatchSemaphore(value: 0)
        proc.terminationHandler = { _ in result.signal() }

        if result.wait(timeout: deadline) == .timedOut {
            proc.terminate()
            return ProcessResult(exitCode: -1, stdout: "",
                                 stderr: "Timeout after \(timeout)s")
        }

        let out = String(
            data: stdoutPipe.fileHandleForReading.readDataToEndOfFile(),
            encoding: .utf8
        ) ?? ""
        let err = String(
            data: stderrPipe.fileHandleForReading.readDataToEndOfFile(),
            encoding: .utf8
        ) ?? ""

        return ProcessResult(exitCode: proc.terminationStatus,
                             stdout: out, stderr: err)
    }

    /// Find binary in PATH
    private static func which(_ name: String) -> String? {
        let r = run("/usr/bin/which", args: [name], timeout: 3)
        let path = r.stdout.trimmingCharacters(in: .whitespacesAndNewlines)
        return path.isEmpty ? nil : path
    }
}
