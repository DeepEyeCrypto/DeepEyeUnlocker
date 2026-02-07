#include "../../include/usb_transport.h"
#include <iostream>
#ifdef HAS_LIBUSB
#include <libusb.h>
#endif

namespace DeepEye {
namespace Core {

LibUsbTransport::LibUsbTransport() : _ctx(nullptr), _handle(nullptr), _fd(-1) {
#ifdef HAS_LIBUSB
  libusb_init(reinterpret_cast<libusb_context **>(&_ctx));
#endif
}

LibUsbTransport::~LibUsbTransport() {
  Close();
#ifdef HAS_LIBUSB
  if (_ctx)
    libusb_exit(reinterpret_cast<libusb_context *>(_ctx));
#endif
}

bool LibUsbTransport::Open(int fd) {
#ifdef HAS_LIBUSB
  _fd = fd;
  // On Android, we use libusb_wrap_sys_device to wrap the OS-provided FD.
  int rc = libusb_wrap_sys_device(
      reinterpret_cast<libusb_context *>(_ctx), (intptr_t)fd,
      reinterpret_cast<libusb_device_handle **>(&_handle));
  if (rc != 0) {
    std::cerr << "Failed to wrap FD: " << libusb_error_name(rc) << std::endl;
    return false;
  }

  libusb_claim_interface(reinterpret_cast<libusb_device_handle *>(_handle), 0);
  return true;
#else
  (void)fd;
  std::cerr << "USB Transport compiled without libusb support." << std::endl;
  return false;
#endif
}

void LibUsbTransport::Close() {
#ifdef HAS_LIBUSB
  if (_handle) {
    libusb_release_interface(reinterpret_cast<libusb_device_handle *>(_handle),
                             0);
    libusb_close(reinterpret_cast<libusb_device_handle *>(_handle));
    _handle = nullptr;
  }
#endif
}

int LibUsbTransport::Send(const uint8_t *data, size_t length,
                          uint32_t timeout_ms) {
#ifdef HAS_LIBUSB
  int totalTransferred = 0;
  const size_t CHUNK_SIZE = 16 * 1024; // 16KB Chunks for OTG stability

  while (totalTransferred < (int)length) {
    int toTransfer =
        std::min((int)(length - totalTransferred), (int)CHUNK_SIZE);
    int transferred = 0;
    int rc =
        libusb_bulk_transfer(reinterpret_cast<libusb_device_handle *>(_handle),
                             0x01, (unsigned char *)(data + totalTransferred),
                             toTransfer, &transferred, timeout_ms);

    if (rc != 0 && rc != LIBUSB_ERROR_TIMEOUT)
      break;
    totalTransferred += transferred;
    if (transferred < toTransfer)
      break; // Partial transfer
  }
  return totalTransferred;
#else
  (void)data;
  (void)length;
  (void)timeout_ms;
  return -1;
#endif
}

int LibUsbTransport::Receive(uint8_t *data, size_t length,
                             uint32_t timeout_ms) {
#ifdef HAS_LIBUSB
  int totalTransferred = 0;
  const size_t CHUNK_SIZE = 16 * 1024;

  while (totalTransferred < (int)length) {
    int toTransfer =
        std::min((int)(length - totalTransferred), (int)CHUNK_SIZE);
    int transferred = 0;
    int rc = libusb_bulk_transfer(
        reinterpret_cast<libusb_device_handle *>(_handle), 0x81,
        (data + totalTransferred), toTransfer, &transferred, timeout_ms);

    if (rc != 0 && rc != LIBUSB_ERROR_TIMEOUT)
      break;
    totalTransferred += transferred;
    if (transferred < toTransfer)
      break; // Partial read
  }
  return totalTransferred;
#else
  (void)data;
  (void)length;
  (void)timeout_ms;
  return -1;
#endif
}

} // namespace Core
} // namespace DeepEye
