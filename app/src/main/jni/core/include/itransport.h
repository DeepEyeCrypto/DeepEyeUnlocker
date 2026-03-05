#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace DeepEye {
namespace Core {

class ITransport {
public:
    virtual ~ITransport() = default;

    virtual bool Open() = 0;
    virtual bool Open(int fd) = 0;
    virtual void Close() = 0;
    virtual bool IsOpen() const = 0;

    virtual bool Write(const std::vector<uint8_t>& data) = 0;
    virtual bool Read(std::vector<uint8_t>& out,
                      size_t length,
                      int timeoutMs = 3000) = 0;

    virtual bool WriteBulk(const uint8_t* buf,
                           size_t len,
                           int timeoutMs = 5000) = 0;
    virtual int ReadBulk(uint8_t* buf,
                         size_t maxLen,
                         int timeoutMs = 5000) = 0;

    virtual std::string GetDeviceName() const = 0;
    virtual int GetFileDescriptor() const = 0;

    // Legacy compatibility shim for existing protocol implementations.
    virtual int Send(const uint8_t* data, size_t length, uint32_t timeout_ms) {
        if (!WriteBulk(data, length, static_cast<int>(timeout_ms))) {
            return -1;
        }
        return static_cast<int>(length);
    }

    virtual int Receive(uint8_t* data, size_t length, uint32_t timeout_ms) {
        return ReadBulk(data, length, static_cast<int>(timeout_ms));
    }
};

} // namespace Core
} // namespace DeepEye
