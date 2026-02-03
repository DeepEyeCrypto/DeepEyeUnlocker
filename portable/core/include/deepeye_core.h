#ifndef DEEPEYE_CORE_H
#define DEEPEYE_CORE_H

#include "gpt_parser.h"
#include <stdint.h>
#include <string>
#include <vector>

namespace DeepEye {
namespace Core {

enum class ProtocolType { Qualcomm_EDL, MediaTek_BROM, Fastboot, Unknown };

struct DeviceInfo {
  int fd;
  uint16_t vid;
  uint16_t pid;
  std::string serial;
  ProtocolType type;
};

class ITransport {
public:
  virtual ~ITransport() = default;
  virtual bool Open(int fd) = 0;
  virtual void Close() = 0;
  virtual int Send(const uint8_t *data, size_t length, uint32_t timeout_ms) = 0;
  virtual int Receive(uint8_t *data, size_t length, uint32_t timeout_ms) = 0;
};

class ProtocolEngine {
public:
  ProtocolEngine(ITransport *transport);
  bool Identify();
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
