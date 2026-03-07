#ifndef DEEPEYE_BROM_PROTO_H
#define DEEPEYE_BROM_PROTO_H

#include "deepeye_core.h"
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

class BromManager {
public:
  enum class BromCommand : uint8_t {
    START = 0xA0,
    READ16 = 0xA2,
    READ32 = 0xAA,
    WRITE16 = 0xA4,
    WRITE32 = 0xAE,
    JUMP_DA = 0xD5,
    SEND_DA = 0xD7,
    GET_HW_SW_VER = 0xFC,
    GET_TARGET_CONFIG = 0xD8
  };

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

  // NVRAM Operations
  std::vector<uint8_t> ReadNvramItem(int item);
  bool WriteNvramItem(int item, const std::vector<uint8_t> &data);

  // MetaMode / seccfg
  bool EnterMetaMode();
  std::vector<uint8_t> ReadSeccfg();
  bool WriteSeccfg(const std::vector<uint8_t> &data);

private:
  Core::ITransport *_transport;
  bool EchoCmd(uint8_t cmd);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_BROM_PROTO_H
