#ifndef USB_TRANSPORT_H
#define USB_TRANSPORT_H

#include "itransport.h"
#include <vector>

namespace DeepEye {
namespace Core {

class LibUsbTransport : public ITransport {
public:
  LibUsbTransport();
  ~LibUsbTransport();

  bool Open() override;
  bool Open(int fd) override;
  void Close() override;
  bool IsOpen() const override;

  bool Write(const std::vector<uint8_t> &data) override;
  bool Read(std::vector<uint8_t> &out, size_t length, int timeoutMs = 3000) override;

  bool WriteBulk(const uint8_t *buf, size_t len, int timeoutMs = 5000) override;
  int ReadBulk(uint8_t *buf, size_t maxLen, int timeoutMs = 5000) override;

  std::string GetDeviceName() const override;
  int GetFileDescriptor() const override;

  // Legacy API compatibility (used by protocol managers)
  int Send(const uint8_t *data, size_t length, uint32_t timeout_ms) override;
  int Receive(uint8_t *data, size_t length, uint32_t timeout_ms) override;

private:
  void *_ctx;
  void *_handle;
  int _fd;
  unsigned char _ep_in;
  unsigned char _ep_out;
};

} // namespace Core
} // namespace DeepEye

#endif // USB_TRANSPORT_H
