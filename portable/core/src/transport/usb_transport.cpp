#include "../../include/deepeye_core.h"
#include <iostream>
#include <libusb.h>

namespace DeepEye {
namespace Core {

class LibUsbTransport : public ITransport {
public:
  LibUsbTransport() : _ctx(nullptr), _handle(nullptr), _fd(-1) {
    libusb_init(&_ctx);
  }

  ~LibUsbTransport() {
    Close();
    if (_ctx)
      libusb_exit(_ctx);
  }

  bool Open(int fd) override {
    _fd = fd;
    // On Android, we use libusb_wrap_sys_device to wrap the OS-provided FD.
    int rc = libusb_wrap_sys_device(_ctx, (intptr_t)fd, &_handle);
    if (rc != 0) {
      std::cerr << "Failed to wrap FD: " << libusb_error_name(rc) << std::endl;
      return false;
    }

    libusb_claim_interface(_handle, 0);
    return true;
  }

  void Close() override {
    if (_handle) {
      libusb_release_interface(_handle, 0);
      libusb_close(_handle);
      _handle = nullptr;
    }
  }

  int Send(const uint8_t *data, size_t length, uint32_t timeout_ms) override {
    int transferred = 0;
    libusb_bulk_transfer(_handle, 0x01, (unsigned char *)data, (int)length,
                         &transferred, timeout_ms);
    return transferred;
  }

  int Receive(uint8_t *data, size_t length, uint32_t timeout_ms) override {
    int transferred = 0;
    libusb_bulk_transfer(_handle, 0x81, data, (int)length, &transferred,
                         timeout_ms);
    return transferred;
  }

private:
  libusb_context *_ctx;
  libusb_device_handle *_handle;
  int _fd;
};

} // namespace Core
} // namespace DeepEye
