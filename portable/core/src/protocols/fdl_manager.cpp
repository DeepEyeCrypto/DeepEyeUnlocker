#include "../../include/fdl_proto.h"
#include <cstring>
#include <fstream>
#include <iostream>

namespace DeepEye {
namespace Protocols {

// ═══════════════════════════════════════════════════════════════════
//  UniSoc FDL Protocol Implementation
//
//  Wire format (HDLC-like):
//    [0x7E][cmd_hi][cmd_lo][len_hi][len_lo][payload...][crc_hi][crc_lo][0x7E]
//
//  Escape: 0x7E in payload → 0x7D 0x5E  |  0x7D → 0x7D 0x5D
// ═══════════════════════════════════════════════════════════════════

static constexpr uint8_t HDLC_FLAG = 0x7E;
static constexpr uint8_t HDLC_ESCAPE = 0x7D;
static constexpr uint8_t HDLC_ESCAPE_MASK = 0x20;
static constexpr uint32_t FDL_TRANSFER_SIZE = 65536; // 64KB chunks

FdlManager::FdlManager(Core::ITransport *transport)
    : _transport(transport), _state(FdlSessionState::Disconnected) {}

// ── Session Management ────────────────────────────────────────────

bool FdlManager::Handshake() {
  std::cout << "[FDL] Initiating FDL1 handshake..." << std::endl;
  _state = FdlSessionState::Disconnected;

  // Step 1: Send connect command
  if (!SendFdlPacket(FdlCommand::Connect, nullptr, 0)) {
    std::cerr << "[FDL] Connect send failed" << std::endl;
    _state = FdlSessionState::Error;
    return false;
  }

  // Step 2: Wait for ConnectAck
  if (!WaitResponse(FdlCommand::ConnectAck)) {
    std::cerr << "[FDL] No ConnectAck received" << std::endl;
    _state = FdlSessionState::Error;
    return false;
  }

  _state = FdlSessionState::FDL1Connected;
  std::cout << "[FDL] FDL1 connected" << std::endl;
  return true;
}

bool FdlManager::LoadFdl2(const std::vector<uint8_t> &fdl2Data) {
  std::cout << "[FDL] Loading FDL2 (" << fdl2Data.size() << " bytes)..."
            << std::endl;

  if (_state != FdlSessionState::FDL1Connected) {
    std::cerr << "[FDL] Must handshake first" << std::endl;
    return false;
  }

  // Send DataStart with load address
  uint32_t loadAddr = 0x30000000; // Typical FDL2 load address
  uint32_t totalSize = fdl2Data.size();
  uint8_t startPayload[8];
  memcpy(startPayload, &loadAddr, 4);
  memcpy(startPayload + 4, &totalSize, 4);

  if (!SendFdlPacket(FdlCommand::DataStart, startPayload, 8)) {
    return false;
  }
  if (!WaitResponse(FdlCommand::DataAck))
    return false;

  // Send data in chunks
  size_t sent = 0;
  while (sent < fdl2Data.size()) {
    size_t chunk = std::min((size_t)FDL_TRANSFER_SIZE, fdl2Data.size() - sent);
    if (!SendFdlPacket(FdlCommand::DataMid, fdl2Data.data() + sent, chunk)) {
      return false;
    }
    if (!WaitResponse(FdlCommand::DataAck))
      return false;
    sent += chunk;
  }

  // Send DataEnd
  if (!SendFdlPacket(FdlCommand::DataEnd, nullptr, 0))
    return false;
  if (!WaitResponse(FdlCommand::DataAck))
    return false;

  // Execute FDL2
  if (!SendFdlPacket(FdlCommand::Exec, nullptr, 0))
    return false;

  // Wait for FDL2 to start and re-handshake
  // FDL2 sends its own ConnectAck after boot
  if (!WaitResponse(FdlCommand::ConnectAck)) {
    std::cerr << "[FDL] FDL2 did not start" << std::endl;
    return false;
  }

  _state = FdlSessionState::FDL2Active;
  std::cout << "[FDL] FDL2 active" << std::endl;
  return true;
}

void FdlManager::Reset() {
  std::cout << "[FDL] Sending reset..." << std::endl;
  SendFdlPacket(FdlCommand::Reset, nullptr, 0);
  _state = FdlSessionState::Disconnected;
}

FdlSessionState FdlManager::GetState() const { return _state; }

// ── Flash Operations ──────────────────────────────────────────────

bool FdlManager::FlashPac(const std::string &pacPath) {
  std::cout << "[FDL] Flashing PAC: " << pacPath << std::endl;

  std::ifstream file(pacPath, std::ios::binary | std::ios::ate);
  if (!file.is_open()) {
    std::cerr << "[FDL] Cannot open PAC: " << pacPath << std::endl;
    return false;
  }

  size_t fileSize = file.tellg();
  file.seekg(0, std::ios::beg);

  // PAC format: header + multiple image blobs
  // For now: treat as single blob flash
  std::vector<uint8_t> data(fileSize);
  if (!file.read(reinterpret_cast<char *>(data.data()), fileSize)) {
    return false;
  }

  return FlashRawData("system", data);
}

bool FdlManager::FlashPartition(const std::string &name,
                                const std::string &imagePath) {
  std::ifstream file(imagePath, std::ios::binary | std::ios::ate);
  if (!file.is_open())
    return false;

  size_t fileSize = file.tellg();
  file.seekg(0, std::ios::beg);

  std::vector<uint8_t> data(fileSize);
  if (!file.read(reinterpret_cast<char *>(data.data()), fileSize)) {
    return false;
  }

  return FlashRawData(name, data);
}

bool FdlManager::FlashRawData(const std::string &name,
                              const std::vector<uint8_t> &data) {
  std::cout << "[FDL] Flash: " << name << " (" << data.size() << " bytes)"
            << std::endl;
  _state = FdlSessionState::Flashing;

  // DataStart: partition name (null-terminated) + size
  std::vector<uint8_t> startPayload(name.size() + 1 + 4);
  memcpy(startPayload.data(), name.c_str(), name.size() + 1);
  uint32_t totalSize = data.size();
  memcpy(startPayload.data() + name.size() + 1, &totalSize, 4);

  if (!SendFdlPacket(FdlCommand::DataStart, startPayload.data(),
                     startPayload.size())) {
    return false;
  }
  if (!WaitResponse(FdlCommand::DataAck))
    return false;

  // Send data chunks
  size_t sent = 0;
  while (sent < data.size()) {
    size_t chunk = std::min((size_t)FDL_TRANSFER_SIZE, data.size() - sent);
    if (!SendFdlPacket(FdlCommand::DataMid, data.data() + sent, chunk)) {
      return false;
    }
    if (!WaitResponse(FdlCommand::DataAck))
      return false;
    sent += chunk;
  }

  // DataEnd
  if (!SendFdlPacket(FdlCommand::DataEnd, nullptr, 0))
    return false;
  if (!WaitResponse(FdlCommand::DataAck))
    return false;

  _state = FdlSessionState::Complete;
  std::cout << "[FDL] Flash complete: " << name << std::endl;
  return true;
}

// ── Partition Operations ──────────────────────────────────────────

std::vector<PartitionInfo> FdlManager::GetPartitions() {
  std::cout << "[FDL] Reading partition table..." << std::endl;

  // Read GPT from device
  uint8_t gptBuf[34 * 512]; // LBA 0-33
  uint32_t readSize = sizeof(gptBuf);

  uint8_t readPayload[8] = {};
  uint32_t readOffset = 0;
  memcpy(readPayload, &readOffset, 4);
  memcpy(readPayload + 4, &readSize, 4);

  if (!SendFdlPacket(FdlCommand::ReadFlash, readPayload, 8)) {
    return {};
  }

  FdlCommand respCmd;
  std::vector<uint8_t> respData;
  if (!ReceiveFdlPacket(respCmd, respData)) {
    return {};
  }

  if (respCmd != FdlCommand::ReadAck || respData.size() < sizeof(gptBuf)) {
    return {};
  }

  // Parse GPT
  GptHeader header;
  if (!GptParser::ParseHeader(respData.data() + 512, header)) {
    std::cerr << "[FDL] GPT parse failed" << std::endl;
    return {};
  }

  return GptParser::ParseEntries(
      respData.data() + header.partitionEntryLba * 512,
      header.numPartitionEntries, header.partitionEntrySize);
}

bool FdlManager::ReadPartition(const std::string &name,
                               const std::string &outPath) {
  std::cout << "[FDL] Reading partition " << name << " to " << outPath
            << std::endl;

  // Send read command with partition name
  std::vector<uint8_t> payload(name.size() + 1);
  memcpy(payload.data(), name.c_str(), name.size() + 1);

  if (!SendFdlPacket(FdlCommand::ReadFlash, payload.data(), payload.size())) {
    return false;
  }

  FdlCommand respCmd;
  std::vector<uint8_t> respData;
  if (!ReceiveFdlPacket(respCmd, respData))
    return false;

  if (respCmd != FdlCommand::ReadAck || respData.empty())
    return false;

  std::ofstream outFile(outPath, std::ios::binary);
  if (!outFile.is_open())
    return false;
  outFile.write(reinterpret_cast<char *>(respData.data()), respData.size());
  outFile.close();

  std::cout << "[FDL] Partition read: " << respData.size() << " bytes"
            << std::endl;
  return true;
}

bool FdlManager::ErasePartition(const std::string &name) {
  std::cout << "[FDL] Erasing partition: " << name << std::endl;

  std::vector<uint8_t> payload(name.size() + 1);
  memcpy(payload.data(), name.c_str(), name.size() + 1);

  if (!SendFdlPacket(FdlCommand::EraseFlash, payload.data(), payload.size())) {
    return false;
  }

  return WaitResponse(FdlCommand::EraseAck);
}

// ── NV Operations ─────────────────────────────────────────────────

std::vector<uint8_t> FdlManager::ReadNv(int nvId) {
  std::cout << "[FDL] ReadNv: id=" << nvId << std::endl;

  uint8_t payload[4];
  memcpy(payload, &nvId, 4);

  if (!SendFdlPacket(FdlCommand::ReadNv, payload, 4)) {
    return {};
  }

  FdlCommand respCmd;
  std::vector<uint8_t> data;
  if (!ReceiveFdlPacket(respCmd, data))
    return {};

  if (respCmd != FdlCommand::ReadNvAck)
    return {};
  return data;
}

bool FdlManager::WriteNv(int nvId, const std::vector<uint8_t> &data) {
  std::cout << "[FDL] WriteNv: id=" << nvId << " (" << data.size() << " bytes)"
            << std::endl;

  std::vector<uint8_t> payload(4 + data.size());
  memcpy(payload.data(), &nvId, 4);
  memcpy(payload.data() + 4, data.data(), data.size());

  if (!SendFdlPacket(FdlCommand::WriteNv, payload.data(), payload.size())) {
    return false;
  }

  return WaitResponse(FdlCommand::WriteNvAck);
}

// ── Device Info ───────────────────────────────────────────────────

std::string FdlManager::GetDeviceInfo() {
  std::cout << "[FDL] Requesting device info..." << std::endl;

  if (!SendFdlPacket(FdlCommand::DeviceInfo, nullptr, 0)) {
    return "{}";
  }

  FdlCommand respCmd;
  std::vector<uint8_t> data;
  if (!ReceiveFdlPacket(respCmd, data))
    return "{}";

  if (respCmd != FdlCommand::DeviceInfoAck || data.empty())
    return "{}";

  return std::string(data.begin(), data.end());
}

// ── HDLC Framing ──────────────────────────────────────────────────

bool FdlManager::SendFdlPacket(FdlCommand cmd, const uint8_t *data,
                               size_t len) {
  // Build raw packet: [cmd_hi][cmd_lo][len_hi][len_lo][payload...]
  std::vector<uint8_t> raw(4 + len);
  uint16_t cmdVal = static_cast<uint16_t>(cmd);
  uint16_t sizeVal = static_cast<uint16_t>(len);
  raw[0] = (cmdVal >> 8) & 0xFF;
  raw[1] = cmdVal & 0xFF;
  raw[2] = (sizeVal >> 8) & 0xFF;
  raw[3] = sizeVal & 0xFF;
  if (len > 0 && data) {
    memcpy(raw.data() + 4, data, len);
  }

  // Add checksum
  uint16_t crc = CalcChecksum(raw.data(), raw.size());
  raw.push_back((crc >> 8) & 0xFF);
  raw.push_back(crc & 0xFF);

  // HDLC encode
  auto encoded = HdlcEncode(raw);

  return _transport->Send(encoded.data(), encoded.size(), 5000) ==
         (int)encoded.size();
}

bool FdlManager::ReceiveFdlPacket(FdlCommand &cmd, std::vector<uint8_t> &data) {
  uint8_t buffer[FDL_TRANSFER_SIZE + 256];
  int received = _transport->Receive(buffer, sizeof(buffer), 5000);
  if (received < 6)
    return false; // Minimum: flag + cmd(2) + len(2) + flag

  auto decoded = HdlcDecode(buffer, received);
  if (decoded.size() < 4)
    return false;

  uint16_t cmdVal = (decoded[0] << 8) | decoded[1];
  uint16_t dataLen = (decoded[2] << 8) | decoded[3];
  cmd = static_cast<FdlCommand>(cmdVal);

  if (decoded.size() >= 4 + dataLen) {
    data.assign(decoded.begin() + 4, decoded.begin() + 4 + dataLen);
  } else {
    data.clear();
  }

  return true;
}

bool FdlManager::WaitResponse(FdlCommand expectedAck) {
  FdlCommand cmd;
  std::vector<uint8_t> data;
  if (!ReceiveFdlPacket(cmd, data))
    return false;
  return cmd == expectedAck;
}

std::vector<uint8_t> FdlManager::HdlcEncode(const std::vector<uint8_t> &raw) {
  std::vector<uint8_t> encoded;
  encoded.reserve(raw.size() * 2 + 2);
  encoded.push_back(HDLC_FLAG);

  for (uint8_t b : raw) {
    if (b == HDLC_FLAG || b == HDLC_ESCAPE) {
      encoded.push_back(HDLC_ESCAPE);
      encoded.push_back(b ^ HDLC_ESCAPE_MASK);
    } else {
      encoded.push_back(b);
    }
  }

  encoded.push_back(HDLC_FLAG);
  return encoded;
}

std::vector<uint8_t> FdlManager::HdlcDecode(const uint8_t *data, size_t len) {
  std::vector<uint8_t> decoded;
  bool escaped = false;
  bool started = false;

  for (size_t i = 0; i < len; i++) {
    if (data[i] == HDLC_FLAG) {
      if (started)
        break; // End of frame
      started = true;
      continue;
    }
    if (!started)
      continue;

    if (escaped) {
      decoded.push_back(data[i] ^ HDLC_ESCAPE_MASK);
      escaped = false;
    } else if (data[i] == HDLC_ESCAPE) {
      escaped = true;
    } else {
      decoded.push_back(data[i]);
    }
  }

  // Remove trailing CRC (2 bytes) if present
  if (decoded.size() >= 2) {
    decoded.resize(decoded.size() - 2);
  }

  return decoded;
}

uint16_t FdlManager::CalcChecksum(const uint8_t *data, size_t len) {
  uint16_t crc = 0;
  for (size_t i = 0; i < len; i++) {
    crc += data[i];
  }
  return crc;
}

} // namespace Protocols
} // namespace DeepEye
