#ifndef DEEPEYE_EDL_PROTO_H
#define DEEPEYE_EDL_PROTO_H

#include "deepeye_core.h"
#include "gpt_parser.h"
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

// Sahara Protocol (Initial Handshake)
enum class SaharaCommand {
  Hello = 0x01,
  HelloResponse = 0x02,
  Read = 0x03,
  Write = 0x04,
  Done = 0x05,
  Reset = 0x07
};

struct SaharaHeader {
  uint32_t command;
  uint32_t length;
};

class EdlManager {
public:
  EdlManager(Core::ITransport *transport);

  bool ConnectSahara();
  bool SendProgrammer(const std::vector<uint8_t> &data);
  bool FirehoseHandshake();

  // Firehose Operations (XML based)
  bool SendXmlCommand(const std::string &xml);
  std::string ReceiveXmlResponse();

  bool ReadPartition(const std::string &name, uint64_t offset, uint64_t count,
                     std::vector<uint8_t> &out);
  bool WritePartition(const std::string &name, uint64_t offset,
                      const std::vector<uint8_t> &data);
  bool ErasePartition(const std::string &name);

private:
  Core::ITransport *_transport;
  bool SendSaharaPacket(SaharaCommand cmd, const uint8_t *data, size_t len);
  bool ReceiveSaharaPacket(SaharaCommand &cmd, std::vector<uint8_t> &data);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_EDL_PROTO_H
