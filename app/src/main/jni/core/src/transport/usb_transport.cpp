#include "../../include/usb_transport.h"
#include <iostream>
#ifdef HAS_LIBUSB
#include <libusb.h>
#endif

namespace DeepEye {
namespace Core {

LibUsbTransport::LibUsbTransport() : _ctx(nullptr), _handle(nullptr), _fd(-1), _ep_in(0), _ep_out(0) {
#ifdef HAS_LIBUSB
  // CRITICAL ANDROID OPTIMIZATION:
  // Prevent libusb from enumerating devices (requires root/permissions we don't have).
  // We only work with the FD passed from Java.
  libusb_set_option(nullptr, LIBUSB_OPTION_NO_DEVICE_DISCOVERY);
  
  int rc = libusb_init(reinterpret_cast<libusb_context **>(&_ctx));
  if (rc != 0) {
      std::cerr << "LibUSB Init Failed: " << libusb_error_name(rc) << std::endl;
  }
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

  libusb_device_handle *handle = reinterpret_cast<libusb_device_handle *>(_handle);
  libusb_claim_interface(handle, 0);

  // Dynamic Endpoint Discovery
  libusb_device *dev = libusb_get_device(handle);
  libusb_config_descriptor *config;
  if (libusb_get_active_config_descriptor(dev, &config) == 0) {
      for (int i = 0; i < config->bNumInterfaces; i++) {
          const libusb_interface *inter = &config->interface[i];
          const libusb_interface_descriptor *interdesc = &inter->altsetting[0];
          
          for (int j = 0; j < interdesc->bNumEndpoints; j++) {
              const libusb_endpoint_descriptor *epdesc = &interdesc->endpoint[j];
              if ((epdesc->bmAttributes & LIBUSB_TRANSFER_TYPE_MASK) == LIBUSB_TRANSFER_TYPE_BULK) {
                  if ((epdesc->bEndpointAddress & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN) {
                      if (_ep_in == 0) _ep_in = epdesc->bEndpointAddress;
                  } else {
                      if (_ep_out == 0) _ep_out = epdesc->bEndpointAddress;
                  }
              }
          }
      }
      libusb_free_config_descriptor(config);
  }

  if (_ep_in == 0) _ep_in = 0x81; // Fallback
  if (_ep_out == 0) _ep_out = 0x01; // Fallback

  std::cout << "Endpoints Found: IN=" << std::hex << (int)_ep_in << " OUT=" << (int)_ep_out << std::dec << std::endl;
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
                             _ep_out, (unsigned char *)(data + totalTransferred),
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
        reinterpret_cast<libusb_device_handle *>(_handle), _ep_in,
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
