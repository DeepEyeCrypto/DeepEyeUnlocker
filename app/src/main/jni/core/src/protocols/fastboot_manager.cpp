#include "../../include/fastboot_proto.h"
#include <iomanip>
#include <iostream>
#include <sstream>

namespace DeepEye {
namespace Protocols {

FastbootManager::FastbootManager(Core::ITransport *transport)
    : _transport(transport) {}

bool FastbootManager::Handshake() {
  std::string response;
  // Send simple getvar command to test connection
  std::string res = SendCommand("getvar:product", nullptr);
  return !res.empty();
}

std::string FastbootManager::SendCommand(const std::string &cmd,
                                         std::string *outFailReason) {
  if (!_transport || !_transport->IsOpen())
    return "";

  if (_transport->Send(reinterpret_cast<const uint8_t *>(cmd.c_str()),
                       cmd.size(), 1000) != static_cast<int>(cmd.size())) {
    if (outFailReason)
      *outFailReason = "Write timeout / disconnect";
    return "";
  }

  std::string dataBuf;
  if (!AwaitResponse(dataBuf, outFailReason)) {
    return "";
  }

  return dataBuf;
}

bool FastbootManager::AwaitResponse(std::string &outData,
                                    std::string *outFailReason) {
  outData.clear();
  if (outFailReason)
    outFailReason->clear();

  uint8_t buf[256];
  while (true) {
    int r = _transport->Receive(buf, sizeof(buf) - 1, 3000);
    if (r <= 0) {
      if (outFailReason)
        *outFailReason = "Timeout waiting for response";
      return false;
    }
    buf[r] = '\0';
    std::string resHeader(reinterpret_cast<char *>(buf), r);

    if (resHeader.substr(0, 4) == "INFO") {
      std::cout << "[FASTBOOT INFO] " << resHeader.substr(4) << std::endl;
      // continue waiting for OKAY or FAIL
    } else if (resHeader.substr(0, 4) == "OKAY") {
      outData += resHeader.substr(4);
      return true;
    } else if (resHeader.substr(0, 4) == "FAIL") {
      if (outFailReason)
        *outFailReason = resHeader.substr(4);
      std::cerr << "[FASTBOOT FAIL] " << resHeader.substr(4) << std::endl;
      return false;
    } else if (resHeader.substr(0, 4) == "DATA") {
      // Data requests are handled specifically in DownloadData!
      // In a normal command await, seeing DATA happens but isn't part of normal
      // getvar/flash command responses.
      outData += resHeader;
      return true;
    } else {
      // Append any chunking text (some OEMs violate spec)
      outData += resHeader;
    }
  }
}

bool FastbootManager::DownloadData(const std::vector<uint8_t> &data) {
  std::stringstream ss;
  ss << "download:" << std::setw(8) << std::setfill('0') << std::hex
     << data.size();
  std::string cmd = ss.str();

  if (_transport->Send(reinterpret_cast<const uint8_t *>(cmd.c_str()),
                       cmd.size(), 1000) != static_cast<int>(cmd.size())) {
    return false;
  }

  uint8_t buf[256];
  int r = _transport->Receive(buf, sizeof(buf) - 1, 3000);
  if (r <= 0)
    return false;
  buf[r] = '\0';

  std::string resHeader(reinterpret_cast<char *>(buf), r);
  if (resHeader.substr(0, 4) != "DATA") {
    std::cerr << "[FASTBOOT] Expected DATA response, got: " << resHeader
              << std::endl;
    return false;
  }

  // Send actual data payload using bulk transfer
  if (!_transport->WriteBulk(data.data(), data.size(), 15000)) {
    std::cerr << "[FASTBOOT] Raw payload upload failed" << std::endl;
    return false;
  }

  // Check OKAY
  std::string throwaway;
  return AwaitResponse(throwaway, nullptr);
}

bool FastbootManager::FlashPartition(const std::string &partition) {
  std::string fail;
  std::string res = SendCommand("flash:" + partition, &fail);
  return fail.empty();
}

bool FastbootManager::ErasePartition(const std::string &partition) {
  std::string fail;
  std::string res = SendCommand("erase:" + partition, &fail);
  return fail.empty();
}

bool FastbootManager::Boot(const std::vector<uint8_t> &bootImage) {
  if (!DownloadData(bootImage))
    return false;
  std::string fail;
  SendCommand("boot", &fail);
  return fail.empty();
}

bool FastbootManager::OemUnlock() {
  std::string fail;
  SendCommand("flashing unlock", &fail);
  if (!fail.empty()) {
    fail.clear();
    SendCommand("oem unlock", &fail);
  }
  return fail.empty();
}

bool FastbootManager::Reboot(const std::string &target) {
  std::string fail;
  if (target.empty() || target == "normal") {
    SendCommand("reboot", &fail);
  } else {
    SendCommand("reboot-" + target, &fail); // ex: reboot-bootloader
  }
  return fail.empty();
}

} // namespace Protocols
} // namespace DeepEye
