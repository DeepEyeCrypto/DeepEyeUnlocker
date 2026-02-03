#ifndef DEEPEYE_BROM_PROTO_H
#define DEEPEYE_BROM_PROTO_H

#include "deepeye_core.h"
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

class BromManager {
public:
  BromManager(Core::ITransport *transport);

  bool Handshake();
  bool SendDA(const std::vector<uint8_t> &daData);
  bool JumpDA(uint32_t addr);

  // BROM Commands
  bool ReadReg32(uint32_t addr, uint32_t &val);
  bool WriteReg32(uint32_t addr, uint32_t val);

  // DA Protocol (Active after JumpDA)
  bool DaReadPartition(const std::string &name, uint64_t offset, uint64_t count,
                       std::vector<uint8_t> &out);
  bool DaWritePartition(const std::string &name, uint64_t offset,
                        const std::vector<uint8_t> &data);
  bool DaErasePartition(const std::string &name);

private:
  Core::ITransport *_transport;
  bool EchoCmd(uint8_t cmd);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_BROM_PROTO_H
