#include "../../include/brom_proto.h"
#include <cstring>
#include <iostream>

namespace DeepEye {
namespace Protocols {

BromManager::BromManager(Core::ITransport *transport) : _transport(transport) {}

bool BromManager::Handshake() {
  std::cout << "[BROM] Initiating Start sequence..." << std::endl;

  // MTK BROM standard START_CMD sequence: 0xA0
  uint8_t startCmd = static_cast<uint8_t>(BromCommand::START);

  // Device expects START_CMD and replies with its bitwise inverse
  if (_transport->Send(&startCmd, 1, 100) != 1)
    return false;

  uint8_t echo = 0;
  if (_transport->Receive(&echo, 1, 100) != 1)
    return false;

  if (echo != static_cast<uint8_t>(~startCmd)) {
    std::cerr << "[BROM] Handshake mismatch, expected 0x5F, got " << std::hex
              << (int)echo << std::endl;
    return false;
  }

  // Additional typical START sequence bytes (0x0A, 0x50, 0x05)
  uint8_t syncSeq[] = {0x0A, 0x50, 0x05};
  for (uint8_t b : syncSeq) {
    if (_transport->Send(&b, 1, 100) != 1)
      return false;
    if (_transport->Receive(&echo, 1, 100) != 1 ||
        echo != static_cast<uint8_t>(~b))
      return false;
  }

  std::cout << "[BROM] Handshake verified!" << std::endl;
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

// ── NVRAM Operations ─────────────────────────────────────────

std::vector<uint8_t> BromManager::ReadNvramItem(int item) {
  std::cout << "[DA] Reading NVRAM item " << item << "..." << std::endl;

  // DA command: read NVRAM by item ID
  uint8_t cmd[8] = {0xBD, 0x10}; // DA NVRAM Read
  memcpy(cmd + 2, &item, 4);
  uint16_t maxLen = 512;
  memcpy(cmd + 6, &maxLen, 2);

  if (_transport->Send(cmd, 8, 1000) != 8)
    return {};

  // First read: 4 bytes = length
  uint32_t dataLen = 0;
  if (_transport->Receive((uint8_t *)&dataLen, 4, 2000) != 4)
    return {};

  if (dataLen == 0 || dataLen > 65536)
    return {};

  // Read actual NVRAM data
  std::vector<uint8_t> data(dataLen);
  if (_transport->Receive(data.data(), dataLen, 5000) != (int)dataLen)
    return {};

  return data;
}

bool BromManager::WriteNvramItem(int item, const std::vector<uint8_t> &data) {
  std::cout << "[DA] Writing NVRAM item " << item << " (" << data.size()
            << " bytes)..." << std::endl;

  uint8_t cmd[8] = {0xBD, 0x11}; // DA NVRAM Write
  memcpy(cmd + 2, &item, 4);
  uint16_t len = (uint16_t)data.size();
  memcpy(cmd + 6, &len, 2);

  if (_transport->Send(cmd, 8, 1000) != 8)
    return false;

  if (_transport->Send(data.data(), data.size(), 5000) != (int)data.size())
    return false;

  uint8_t status = 0;
  return _transport->Receive(&status, 1, 2000) == 1 && status == 0x5A;
}

// ── MetaMode / seccfg ────────────────────────────────────────

bool BromManager::EnterMetaMode() {
  std::cout << "[BROM] Entering MetaMode..." << std::endl;

  // MTK MetaMode entry: send META_CONNECT command
  uint8_t metaCmd[] = {0xFE, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
  if (_transport->Send(metaCmd, 8, 1000) != 8)
    return false;

  // Wait for ACK (device may re-enumerate)
  uint8_t ack = 0;
  if (_transport->Receive(&ack, 1, 3000) != 1)
    return false;

  return ack == 0x5A || ack == 0xFE;
}

std::vector<uint8_t> BromManager::ReadSeccfg() {
  std::cout << "[DA] Reading seccfg partition..." << std::endl;

  // seccfg is typically at a known partition name
  std::vector<uint8_t> out;
  // Read full seccfg (typically 32KB or smaller)
  if (DaReadPartition("seccfg", 0, 64, out)) { // 64 sectors = 32KB
    return out;
  }
  return {};
}

bool BromManager::WriteSeccfg(const std::vector<uint8_t> &data) {
  std::cout << "[DA] Writing seccfg (" << data.size() << " bytes)..."
            << std::endl;
  return DaWritePartition("seccfg", 0, data);
}

bool BromManager::EchoCmd(uint8_t cmd) {
  if (_transport->Send(&cmd, 1, 100) != 1)
    return false;
  uint8_t echo = 0;
  return _transport->Receive(&echo, 1, 100) == 1 && echo == cmd;
}

} // namespace Protocols
} // namespace DeepEye
