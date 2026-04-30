import Foundation

// Thread-safe in-memory token cache
// ecid → (token, expiry timestamp)
// TTL: 10 minutes

final class TokenCache {
    private var store: [String: (String, Date)] = [:]
    private let lock  = NSLock()
    private let ttl:  TimeInterval = 600  // 10 minutes

    func get(_ ecid: String) -> String? {
        lock.lock(); defer { lock.unlock() }
        guard let (token, expiry) = store[ecid],
              expiry > Date()
        else {
            store.removeValue(forKey: ecid)
            return nil
        }
        return token
    }

    func set(_ ecid: String, token: String) {
        lock.lock(); defer { lock.unlock() }
        store[ecid] = (token, Date().addingTimeInterval(ttl))
    }

    func invalidate(_ ecid: String) {
        lock.lock(); defer { lock.unlock() }
        store.removeValue(forKey: ecid)
    }
}
