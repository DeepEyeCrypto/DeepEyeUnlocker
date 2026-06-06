import Foundation

extension ProcessRunner {
    init(resourcesPath: String) {}

    func run(_ executable: String, args: [String] = [], timeout: TimeInterval = 30.0) async throws -> ProcessResult {
        return await Task.detached {
            return ProcessRunner.run(executable, args: args, timeout: timeout)
        }.value
    }
    
    func stream(_ executable: String, args: [String] = [], timeout: TimeInterval = 30.0, onLine: @escaping (String) -> Void) async throws -> ProcessResult {
        // Since we don't have a real streaming ProcessRunner, we'll run it synchronously inside detached task
        // But for stream, we can implement a custom process launch or just fake the stream for now.
        // Actually, we can just run it and send the whole output line by line at the end, or implement real stream.
        return await Task.detached {
            let proc = Process()
            
            let toolPath = ProcessRunner.toolsDir.appendingPathComponent(executable)
            if FileManager.default.fileExists(atPath: toolPath.path) {
                proc.executableURL = toolPath
            } else {
                let which = ProcessRunner.run("/usr/bin/which", args: [executable], timeout: 3)
                let path = which.stdout.trimmingCharacters(in: .whitespacesAndNewlines)
                if path.isEmpty {
                    return ProcessResult(exitCode: -1, stdout: "", stderr: "\(executable) not found")
                }
                proc.executableURL = URL(fileURLWithPath: path)
            }
            
            proc.arguments = args
            let stdoutPipe = Pipe()
            let stderrPipe = Pipe()
            proc.standardOutput = stdoutPipe
            proc.standardError = stderrPipe
            
            let outHandle = stdoutPipe.fileHandleForReading
            outHandle.readabilityHandler = { handle in
                if let line = String(data: handle.availableData, encoding: .utf8), !line.isEmpty {
                    onLine(line)
                }
            }
            
            do {
                try proc.run()
            } catch {
                return ProcessResult(exitCode: -1, stdout: "", stderr: "Launch failed: \(error)")
            }
            
            proc.waitUntilExit()
            let result = ProcessResult(exitCode: proc.terminationStatus, stdout: "done", stderr: "")
            outHandle.readabilityHandler = nil
            return result
        }.value
    }
}

extension ProcessResult {
    var succeeded: Bool { success }
}
