#ifndef USB_TRANSPORT_H
#define USB_TRANSPORT_H

#include "deepeye_core.h"

namespace DeepEye {
namespace Core {

class LibUsbTransport : public ITransport {
public:
  LibUsbTransport();
  ~LibUsbTransport();

  bool Open(int fd) override;
  void Close() override;
  int Send(const uint8_t *data, size_t length, uint32_t timeout_ms) override;
  int Receive(uint8_t *data, size_t length, uint32_t timeout_ms) override;

private:
  void *_ctx;
  void *_handle;
  int _fd;
};

} // namespace Core
} // namespace DeepEye

#endif // USB_TRANSPORT_H
