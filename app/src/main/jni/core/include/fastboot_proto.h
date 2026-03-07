#ifndef DEEPEYE_FASTBOOT_PROTO_H
#define DEEPEYE_FASTBOOT_PROTO_H

#include "deepeye_core.h"
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

class FastbootManager {
public:
  FastbootManager(Core::ITransport *transport);

  // Initialize and check fastboot
  bool Handshake();

  // Generic command (e.g., "getvar:product")
  // Returns the response payload, omitting OKAY/INFO status unless requested
  std::string SendCommand(const std::string &cmd,
                          std::string *outFailReason = nullptr);

  // Download data to device buffer
  bool DownloadData(const std::vector<uint8_t> &data);

  // Flash downloaded data
  bool FlashPartition(const std::string &partition);

  // Erase partition
  bool ErasePartition(const std::string &partition);

  // Specialized routines
  bool Boot(const std::vector<uint8_t> &bootImage);
  bool OemUnlock();
  bool Reboot(const std::string &target = ""); // "bootloader", "recovery", etc.

private:
  Core::ITransport *_transport;

  // Handles reading the streamed responses (INFO, DATA, OKAY, FAIL)
  bool AwaitResponse(std::string &outData,
                     std::string *outFailReason = nullptr);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_FASTBOOT_PROTO_H
