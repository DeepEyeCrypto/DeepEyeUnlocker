#include "../../include/usb_transport.h"
#include <algorithm>
#include <iostream>
#include <limits>
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

bool LibUsbTransport::Open() {
  if (_fd < 0) {
    return false;
  }
  return Open(_fd);
}

bool LibUsbTransport::Open(int fd) {
#ifdef HAS_LIBUSB
  if (fd < 0) {
    std::cerr << "Invalid FD passed to transport" << std::endl;
    return false;
  }

  // Re-open safety: drop any previous handle first.
  if (_handle) {
    Close();
  }

  _fd = fd;
  _ep_in = 0;
  _ep_out = 0;

  if (!_ctx) {
    std::cerr << "LibUSB context is null" << std::endl;
    return false;
  }

  // On Android, we use libusb_wrap_sys_device to wrap the OS-provided FD.
  int rc = libusb_wrap_sys_device(
      reinterpret_cast<libusb_context *>(_ctx), (intptr_t)fd,
      reinterpret_cast<libusb_device_handle **>(&_handle));
  if (rc != 0) {
    std::cerr << "Failed to wrap FD: " << libusb_error_name(rc) << std::endl;
    return false;
  }

  libusb_device_handle *handle = reinterpret_cast<libusb_device_handle *>(_handle);
  rc = libusb_claim_interface(handle, 0);
  if (rc != 0) {
    std::cerr << "Failed to claim USB interface 0: " << libusb_error_name(rc)
              << std::endl;
    Close();
    return false;
  }

  // Dynamic Endpoint Discovery
  libusb_device *dev = libusb_get_device(handle);
  libusb_config_descriptor *config;
  if (dev && libusb_get_active_config_descriptor(dev, &config) == 0) {
      for (int i = 0; i < config->bNumInterfaces; i++) {
          const libusb_interface *inter = &config->interface[i];
          if (!inter || inter->num_altsetting <= 0) {
              continue;
          }

          for (int a = 0; a < inter->num_altsetting; a++) {
              const libusb_interface_descriptor *interdesc = &inter->altsetting[a];
              if (!interdesc) {
                  continue;
              }

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

bool LibUsbTransport::IsOpen() const {
  return _handle != nullptr;
}

bool LibUsbTransport::Write(const std::vector<uint8_t> &data) {
  if (data.empty()) {
    return true;
  }
  return WriteBulk(data.data(), data.size(), 5000);
}

bool LibUsbTransport::Read(std::vector<uint8_t> &out, size_t length, int timeoutMs) {
  if (length > static_cast<size_t>(std::numeric_limits<int>::max())) {
    out.clear();
    return false;
  }
  out.assign(length, 0);
  if (length == 0) {
    return true;
  }
  const int received = ReadBulk(out.data(), length, timeoutMs);
  if (received < 0) {
    out.clear();
    return false;
  }
  out.resize(static_cast<size_t>(received));
  return static_cast<size_t>(received) == length;
}

bool LibUsbTransport::WriteBulk(const uint8_t *buf, size_t len, int timeoutMs) {
  if (!buf && len > 0) {
    return false;
  }
  if (len > static_cast<size_t>(std::numeric_limits<int>::max())) {
    return false;
  }
  return Send(buf, len, static_cast<uint32_t>(timeoutMs)) == static_cast<int>(len);
}

int LibUsbTransport::ReadBulk(uint8_t *buf, size_t maxLen, int timeoutMs) {
  if (!buf && maxLen > 0) {
    return -1;
  }
  if (maxLen > static_cast<size_t>(std::numeric_limits<int>::max())) {
    return -1;
  }
  return Receive(buf, maxLen, static_cast<uint32_t>(timeoutMs));
}

std::string LibUsbTransport::GetDeviceName() const {
  return "libusb-wrapped-fd";
}

int LibUsbTransport::GetFileDescriptor() const {
  return _fd;
}

int LibUsbTransport::Send(const uint8_t *data, size_t length,
                          uint32_t timeout_ms) {
#ifdef HAS_LIBUSB
  if (!_handle) {
    return -1;
  }
  if (length == 0) {
    return 0;
  }
  if (!data) {
    return -1;
  }
  if (length > static_cast<size_t>(std::numeric_limits<int>::max())) {
    return -1;
  }

  size_t totalTransferred = 0;
  constexpr size_t CHUNK_SIZE = 16 * 1024; // 16KB Chunks for OTG stability

  while (totalTransferred < length) {
    const size_t remaining = length - totalTransferred;
    int toTransfer = static_cast<int>(std::min(remaining, CHUNK_SIZE));
    int transferred = 0;
    int rc =
        libusb_bulk_transfer(reinterpret_cast<libusb_device_handle *>(_handle),
                             _ep_out, (unsigned char *)(data + totalTransferred),
                             toTransfer, &transferred, timeout_ms);

    if (rc != 0 && rc != LIBUSB_ERROR_TIMEOUT)
      break;
    if (transferred < 0) {
      break;
    }
    totalTransferred += static_cast<size_t>(transferred);
    if (transferred < toTransfer)
      break; // Partial transfer
  }
  return static_cast<int>(totalTransferred);
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
  if (!_handle) {
    return -1;
  }
  if (length == 0) {
    return 0;
  }
  if (!data) {
    return -1;
  }
  if (length > static_cast<size_t>(std::numeric_limits<int>::max())) {
    return -1;
  }

  size_t totalTransferred = 0;
  constexpr size_t CHUNK_SIZE = 16 * 1024;

  while (totalTransferred < length) {
    const size_t remaining = length - totalTransferred;
    int toTransfer = static_cast<int>(std::min(remaining, CHUNK_SIZE));
    int transferred = 0;
    int rc = libusb_bulk_transfer(
        reinterpret_cast<libusb_device_handle *>(_handle), _ep_in,
        (data + totalTransferred), toTransfer, &transferred, timeout_ms);

    if (rc != 0 && rc != LIBUSB_ERROR_TIMEOUT)
      break;
    if (transferred < 0) {
      break;
    }
    totalTransferred += static_cast<size_t>(transferred);
    if (transferred < toTransfer)
      break; // Partial read
  }
  return static_cast<int>(totalTransferred);
#else
  (void)data;
  (void)length;
  (void)timeout_ms;
  return -1;
#endif
}

} // namespace Core
} // namespace DeepEye
