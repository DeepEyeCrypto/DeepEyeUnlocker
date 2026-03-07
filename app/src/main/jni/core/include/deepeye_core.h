#ifndef DEEPEYE_CORE_H
#define DEEPEYE_CORE_H

#include "gpt_parser.h"
#include <stdint.h>
#include <string>
#include <vector>

#include "itransport.h"

namespace DeepEye {
namespace Core {

enum class ProtocolType {
  Qualcomm_EDL,
  MediaTek_BROM,
  Samsung_Odin,
  UniSoc_FDL,
  Fastboot,
  Unknown
};

struct DeviceInfo {
  int fd;
  uint16_t vid;
  uint16_t pid;
  std::string serial;
  ProtocolType type;
};

class ProtocolEngine {
public:
  ProtocolEngine(ITransport *transport);
  bool Identify();
  std::string GetTargetType() const { return _targetType; }
  std::vector<Protocols::PartitionInfo> GetPartitions();
  bool DumpPartition(const std::string &name, const std::string &outPath);
  bool FlashPartition(const std::string &name, const std::string &inPath);
  bool ErasePartition(const std::string &name);

private:
  ITransport *_transport;
  std::string _targetType;
};

} // namespace Core
} // namespace DeepEye

#endif // DEEPEYE_CORE_H
