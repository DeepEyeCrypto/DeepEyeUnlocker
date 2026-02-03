#include "../../include/brom_proto.h"
#include <cstring>
#include <iostream>

namespace DeepEye {
namespace Protocols {

BromManager::BromManager(Core::ITransport *transport) : _transport(transport) {}

bool BromManager::Handshake() {
  uint8_t handshake[] = {0xA1, 0xA2, 0xA3, 0xA4};
  for (uint8_t b : handshake) {
    if (_transport->Send(&b, 1, 100) != 1)
      return false;
    uint8_t echo = 0;
    if (_transport->Receive(&echo, 1, 100) != 1 || echo != (uint8_t)~b)
      return false;
  }
  return true;
}

bool BromManager::SendDA(const std::vector<uint8_t> &daData) {
  std::cout << "[BROM] Injecting Download Agent (" << daData.size()
            << " bytes)..." << std::endl;

  if (!EchoCmd(0xD7))
    return false; // Write DA command

  uint32_t addr = 0x40000000; // Common DA load address
  uint32_t size = daData.size();

  _transport->Send((uint8_t *)&addr, 4, 1000);
  _transport->Send((uint8_t *)&size, 4, 1000);
  _transport->Send((uint8_t *)&size, 4, 1000); // Sig size or secondary size

  return _transport->Send(daData.data(), daData.size(), 5000) ==
         (int)daData.size();
}

bool BromManager::JumpDA(uint32_t addr) {
  if (!EchoCmd(0xD5))
    return false; // Jump command
  return _transport->Send((uint8_t *)&addr, 4, 1000) == 4;
}

bool BromManager::ReadReg32(uint32_t addr, uint32_t &val) {
  if (!EchoCmd(0xD1))
    return false;
  _transport->Send((uint8_t *)&addr, 4, 1000);
  return _transport->Receive((uint8_t *)&val, 4, 1000) == 4;
}

bool BromManager::WriteReg32(uint32_t addr, uint32_t val) {
  if (!EchoCmd(0xD4))
    return false;
  _transport->Send((uint8_t *)&addr, 4, 1000);
  return _transport->Send((uint8_t *)&val, 4, 1000) == 4;
}

bool BromManager::DaReadPartition(const std::string &name, uint64_t offset,
                                  uint64_t count, std::vector<uint8_t> &out) {
  std::cout << "[DA] Reading " << name << " sector " << offset << "..."
            << std::endl;
  // MTK DA-specific protocol would go here (Cmd 0x??)
  uint8_t readCmd[16] = {0xBD, 0x01}; // Mock DA Read
  memcpy(readCmd + 2, &offset, 8);
  memcpy(readCmd + 10, &count, 4);

  _transport->Send(readCmd, 16, 1000);
  out.resize(count * 512);
  return _transport->Receive(out.data(), out.size(), 5000) == (int)out.size();
}

bool BromManager::DaWritePartition(const std::string &name, uint64_t offset,
                                   const std::vector<uint8_t> &data) {
  std::cout << "[DA] Writing to " << name << " at sector " << offset << "..."
            << std::endl;
  uint8_t writeCmd[16] = {0xD0, 0x02}; // Mock DA Write
  uint32_t count = data.size() / 512;
  memcpy(writeCmd + 2, &offset, 8);
  memcpy(writeCmd + 10, &count, 4);

  _transport->Send(writeCmd, 16, 1000);
  return _transport->Send(data.data(), data.size(), 10000) == (int)data.size();
}

bool BromManager::DaErasePartition(const std::string &name) {
  std::cout << "[DA] Erasing MediaTek partition: " << name << "..."
            << std::endl;
  uint8_t eraseCmd[16] = {0xBD, 0x03}; // Mock DA Erase
  // Length/Offset would normally be needed for partial erase,
  // but here we assume full partition erase by name.
  _transport->Send(eraseCmd, 16, 1000);

  uint8_t status = 0;
  return _transport->Receive(&status, 1, 5000) == 1 &&
         status == 0x5A; // 0x5A = DA_ACK
}

bool BromManager::EchoCmd(uint8_t cmd) {
  if (_transport->Send(&cmd, 1, 100) != 1)
    return false;
  uint8_t echo = 0;
  return _transport->Receive(&echo, 1, 100) == 1 && echo == cmd;
}

} // namespace Protocols
} // namespace DeepEye
