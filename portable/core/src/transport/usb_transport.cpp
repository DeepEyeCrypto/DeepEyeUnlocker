#include "../../include/deepeye_core.h"
#include <iostream>
#ifdef HAS_LIBUSB
#include <libusb.h>
#endif

namespace DeepEye {
namespace Core {

class LibUsbTransport : public ITransport {
public:
  LibUsbTransport() : _ctx(nullptr), _handle(nullptr), _fd(-1) {
#ifdef HAS_LIBUSB
    libusb_init(&_ctx);
#endif
  }

  ~LibUsbTransport() {
    Close();
#ifdef HAS_LIBUSB
    if (_ctx)
      libusb_exit(_ctx);
#endif
  }

  bool Open(int fd) override {
#ifdef HAS_LIBUSB
    _fd = fd;
    // On Android, we use libusb_wrap_sys_device to wrap the OS-provided FD.
    int rc = libusb_wrap_sys_device(_ctx, (intptr_t)fd, &_handle);
    if (rc != 0) {
      std::cerr << "Failed to wrap FD: " << libusb_error_name(rc) << std::endl;
      return false;
    }

    libusb_claim_interface(_handle, 0);
    return true;
#else
    (void)fd;
    std::cerr << "USB Transport compiled without libusb support." << std::endl;
    return false;
#endif
  }

  void Close() override {
#ifdef HAS_LIBUSB
    if (_handle) {
      libusb_release_interface(_handle, 0);
      libusb_close(_handle);
      _handle = nullptr;
    }
#endif
  }

  int Send(const uint8_t *data, size_t length, uint32_t timeout_ms) override {
#ifdef HAS_LIBUSB
    int transferred = 0;
    libusb_bulk_transfer(_handle, 0x01, (unsigned char *)data, (int)length,
                         &transferred, timeout_ms);
    return transferred;
#else
    (void)data;
    (void)length;
    (void)timeout_ms;
    return -1;
#endif
  }

  int Receive(uint8_t *data, size_t length, uint32_t timeout_ms) override {
#ifdef HAS_LIBUSB
    int transferred = 0;
    libusb_bulk_transfer(_handle, 0x81, data, (int)length, &transferred,
                         timeout_ms);
    return transferred;
#else
    (void)data;
    (void)length;
    (void)timeout_ms;
    return -1;
#endif
  }

private:
#ifdef HAS_LIBUSB
  libusb_context *_ctx;
  libusb_device_handle *_handle;
#else
  void *_ctx;
  void *_handle;
#endif
  int _fd;
};

} // namespace Core
} // namespace DeepEye
