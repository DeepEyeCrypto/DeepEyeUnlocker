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
  EndTransfer = 0x04,
  Done = 0x05,
  DoneResponse = 0x06,
  Reset = 0x07,
  Execute = 0x08,
  ExecuteResponse = 0x09,
  CommandSwitchMode = 0x0C,
  CommandExecute = 0x0D,
  CommandExecuteResponse = 0x0E
};

#pragma pack(push, 1)

struct SaharaHeader {
  uint32_t command;
  uint32_t length;
};

struct SaharaHello {
  uint32_t version;
  uint32_t version_min;
  uint32_t max_cmd_len;
  uint32_t mode;
  uint32_t reserved[6];
};

struct SaharaHelloResponse {
  uint32_t version;
  uint32_t version_min;
  uint32_t status;
  uint32_t mode;
  uint32_t reserved[6];
};

struct SaharaReadData {
  uint32_t image_id;
  uint32_t data_offset;
  uint32_t data_length;
};

struct SaharaEndTransfer {
  uint32_t image_id;
  uint32_t status;
};

struct SaharaDoneResponse {
  uint32_t status;
};

#pragma pack(pop)

class EdlManager {
public:
  EdlManager(Core::ITransport *transport);

  bool ConnectSahara();
  bool SendProgrammer(const std::vector<uint8_t> &data);
  bool FirehoseHandshake();

  // Firehose Operations (XML based)
  bool SendXmlCommand(const std::string &xml);
  std::string ReceiveXmlResponse();

  // Sahara with programmer file path
  bool SaharaHandshake(const std::string &programmerPath);

  // Firehose XML command → response
  std::string FirehoseXml(const std::string &xmlCommand);

  // NV item read/write (via diag or Firehose)
  std::vector<uint8_t> ReadNvItem(int nvItem);
  bool WriteNvItem(int nvItem, const std::vector<uint8_t> &data);

  // Raw diag command
  std::vector<uint8_t> DiagCommand(const std::vector<uint8_t> &cmd);

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
