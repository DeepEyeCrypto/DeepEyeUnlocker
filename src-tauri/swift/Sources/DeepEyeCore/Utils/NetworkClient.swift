import Foundation

struct NetworkClient {
    let sessionId:    String
    let maxRetries:   Int    = 3
    let timeoutSecs:  Double = 90.0

    private let session: URLSession = {
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest  = 90
        cfg.timeoutIntervalForResource = 120
        return URLSession(configuration: cfg)
    }()

    /// POST JSON with retry + failover across multiple servers
    func post(
        servers:  [String],
        endpoint: String,
        body:     [String: Any]
    ) -> [String: Any]? {
        var lastErr: Error?

        for server in servers.filter({ !$0.isEmpty }) {
            let url = "\(server)\(endpoint)"

            for attempt in 1...maxRetries {
                do {
                    let result = try postOnce(url: url, body: body)
                    return result
                } catch {
                    lastErr = error
                    if attempt < maxRetries {
                        Thread.sleep(forTimeInterval: Double(attempt))
                    }
                }
            }
        }
        return nil
    }

    private func postOnce(url: String, body: [String: Any]) throws -> [String: Any] {
        guard let url = URL(string: url) else {
            throw URLError(.badURL)
        }

        var req = URLRequest(url: url)
        req.httpMethod  = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue(sessionId,          forHTTPHeaderField: "X-Session-Id")
        req.setValue("swift-native",     forHTTPHeaderField: "X-Client")
        req.setValue("true",             forHTTPHeaderField: "X-Open-Source")
        req.httpBody = try JSONSerialization.data(withJSONObject: body)

        var responseData: Data?
        var responseErr:  Error?
        let sema = DispatchSemaphore(value: 0)

        session.dataTask(with: req) { data, _, error in
            responseData = data
            responseErr  = error
            sema.signal()
        }.resume()

        sema.wait()

        if let err = responseErr { throw err }
        guard let data = responseData else { throw URLError(.zeroByteResource) }
        guard let json = try JSONSerialization.jsonObject(
            with: data
        ) as? [String: Any] else {
            throw URLError(.cannotDecodeContentData)
        }

        return json
    }
}
