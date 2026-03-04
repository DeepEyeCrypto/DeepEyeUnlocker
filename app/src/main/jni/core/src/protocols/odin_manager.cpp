#include "../../include/odin_proto.h"
#include <cstring>
#include <fstream>
#include <iostream>

namespace DeepEye {
namespace Protocols {

// ═══════════════════════════════════════════════════════════════════
//  Samsung Odin Protocol Implementation
//
//  Reference: Heimdall open-source Odin implementation
//  Packet wire format: [uint32_t type][payload...]
//  Ack format: [0x00, 0x00, 0x00, 0x00] from device
// ═══════════════════════════════════════════════════════════════════

static constexpr uint8_t ODIN_HANDSHAKE_MSG[] = "ODIN";
static constexpr uint8_t ODIN_HANDSHAKE_ACK[] = "LOKE";
static constexpr uint32_t ODIN_TRANSFER_SIZE = 131072; // 128KB chunks
static constexpr uint32_t PIT_ENTRY_SIZE = sizeof(PitEntry);

OdinManager::OdinManager(Core::ITransport *transport)
    : _transport(transport), _state(OdinSessionState::Disconnected) {}

// ── Session Management ────────────────────────────────────────────

bool OdinManager::Handshake() {
  std::cout << "[ODIN] Initiating session handshake..." << std::endl;
  _state = OdinSessionState::Handshaking;

  // Step 1: Send "ODIN" magic
  if (_transport->Send(ODIN_HANDSHAKE_MSG, 4, 1000) != 4) {
    std::cerr << "[ODIN] Failed to send handshake" << std::endl;
    _state = OdinSessionState::Error;
    return false;
  }

  // Step 2: Expect "LOKE" response
  uint8_t response[4] = {};
  if (_transport->Receive(response, 4, 3000) != 4) {
    std::cerr << "[ODIN] No handshake response" << std::endl;
    _state = OdinSessionState::Error;
    return false;
  }

  if (memcmp(response, ODIN_HANDSHAKE_ACK, 4) != 0) {
    std::cerr << "[ODIN] Unexpected response: "
              << std::string((char *)response, 4) << std::endl;
    _state = OdinSessionState::Error;
    return false;
  }

  // Step 3: Begin session (send session setup packet)
  if (!SendOdinCommand(static_cast<uint32_t>(OdinPacketType::SessionSetup), 0,
                       0)) {
    _state = OdinSessionState::Error;
    return false;
  }

  if (!WaitAck()) {
    _state = OdinSessionState::Error;
    return false;
  }

  _state = OdinSessionState::SessionActive;
  std::cout << "[ODIN] Session established" << std::endl;
  return true;
}

void OdinManager::EndSession() {
  std::cout << "[ODIN] Ending session..." << std::endl;
  SendOdinCommand(static_cast<uint32_t>(OdinPacketType::EndSession), 0, 0);
  WaitAck();
  _state = OdinSessionState::Disconnected;
}

OdinSessionState OdinManager::GetState() const { return _state; }

// ── PIT Operations ────────────────────────────────────────────────

bool OdinManager::RequestPit() {
  std::cout << "[ODIN] Requesting PIT..." << std::endl;

  // Send PIT request command
  if (!SendOdinCommand(static_cast<uint32_t>(OdinPacketType::PitRequest), 1,
                       0)) {
    return false;
  }

  return WaitAck();
}

std::vector<uint8_t> OdinManager::ReadPit() {
  std::cout << "[ODIN] Reading PIT data..." << std::endl;

  if (!RequestPit()) {
    std::cerr << "[ODIN] PIT request failed" << std::endl;
    return {};
  }

  // Send PIT dump command
  if (!SendOdinCommand(static_cast<uint32_t>(OdinPacketType::PitTransfer), 2,
                       0)) {
    return {};
  }

  // Receive PIT size (first 4 bytes of response)
  uint8_t sizeBuf[4] = {};
  if (_transport->Receive(sizeBuf, 4, 3000) != 4) {
    return {};
  }
  uint32_t pitSize = 0;
  memcpy(&pitSize, sizeBuf, 4);

  if (pitSize == 0 || pitSize > 1024 * 1024) { // Max 1MB PIT
    std::cerr << "[ODIN] Invalid PIT size: " << pitSize << std::endl;
    return {};
  }

  // Receive PIT data in chunks
  std::vector<uint8_t> pitData(pitSize);
  size_t received = 0;
  while (received < pitSize) {
    size_t chunk = std::min((size_t)ODIN_TRANSFER_SIZE, pitSize - received);
    int got = _transport->Receive(pitData.data() + received, chunk, 5000);
    if (got <= 0)
      break;
    received += got;
  }

  if (received != pitSize) {
    std::cerr << "[ODIN] PIT read incomplete: " << received << "/" << pitSize
              << std::endl;
    return {};
  }

  // End PIT transfer
  SendOdinCommand(static_cast<uint32_t>(OdinPacketType::PitTransfer), 3, 0);
  WaitAck();

  std::cout << "[ODIN] PIT read complete: " << pitSize << " bytes" << std::endl;
  _state = OdinSessionState::PitLoaded;
  return pitData;
}

std::vector<OdinPartitionInfo>
OdinManager::ParsePit(const std::vector<uint8_t> &pitData) {
  std::vector<OdinPartitionInfo> entries;

  if (pitData.size() < 28)
    return entries; // PIT header is 28 bytes

  // PIT header: magic(4) + count(4) + reserved(20)
  uint32_t entryCount = 0;
  memcpy(&entryCount, pitData.data() + 4, 4);

  std::cout << "[ODIN] Parsing PIT: " << entryCount << " entries" << std::endl;

  size_t offset = 28; // After PIT header
  for (uint32_t i = 0;
       i < entryCount && offset + PIT_ENTRY_SIZE <= pitData.size(); i++) {
    PitEntry raw;
    memcpy(&raw, pitData.data() + offset, PIT_ENTRY_SIZE);

    OdinPartitionInfo info;
    info.name =
        std::string(raw.partition_name, strnlen(raw.partition_name, 32));
    info.flashFilename =
        std::string(raw.flash_filename, strnlen(raw.flash_filename, 32));
    info.binaryType = raw.binary_type;
    info.blockSize = raw.block_size;
    info.blockCount = raw.block_count;
    info.sizeInBytes = (uint64_t)raw.block_size * raw.block_count;

    entries.push_back(info);
    offset += PIT_ENTRY_SIZE;
  }

  _pitEntries = entries;
  std::cout << "[ODIN] Parsed " << entries.size() << " partitions" << std::endl;
  return entries;
}

std::vector<PartitionInfo> OdinManager::GetPartitionsAsGeneric() {
  std::vector<PartitionInfo> result;

  if (_pitEntries.empty()) {
    auto pitData = ReadPit();
    if (!pitData.empty()) {
      ParsePit(pitData);
    }
  }

  for (const auto &entry : _pitEntries) {
    PartitionInfo p;
    p.name = entry.name;
    p.startLba = 0; // PIT doesn't use LBA directly
    p.endLba = 0;
    p.sizeInBytes = entry.sizeInBytes;
    result.push_back(p);
  }

  return result;
}

// ── Flash Operations ──────────────────────────────────────────────

bool OdinManager::FlashPartition(const std::string &partName,
                                 const std::string &imagePath) {
  std::cout << "[ODIN] Flashing " << partName << " from " << imagePath
            << std::endl;
  _state = OdinSessionState::Flashing;

  // Read image file
  std::ifstream file(imagePath, std::ios::binary | std::ios::ate);
  if (!file.is_open()) {
    std::cerr << "[ODIN] Cannot open: " << imagePath << std::endl;
    return false;
  }

  size_t fileSize = file.tellg();
  file.seekg(0, std::ios::beg);

  std::vector<uint8_t> data(fileSize);
  if (!file.read(reinterpret_cast<char *>(data.data()), fileSize)) {
    std::cerr << "[ODIN] Cannot read: " << imagePath << std::endl;
    return false;
  }

  return FlashRawData(partName, data);
}

bool OdinManager::FlashRawData(const std::string &partName,
                               const std::vector<uint8_t> &data) {
  const OdinPartitionInfo *part = FindPartition(partName);
  if (!part) {
    std::cerr << "[ODIN] Partition not found: " << partName << std::endl;
    return false;
  }

  std::cout << "[ODIN] Flash: " << partName << " (" << data.size() << " bytes)"
            << std::endl;

  // Send file transfer begin
  uint32_t totalSize = data.size();
  if (!SendOdinCommand(static_cast<uint32_t>(OdinPacketType::FileTransfer), 0,
                       totalSize)) {
    return false;
  }
  if (!WaitAck())
    return false;

  // Send data in chunks
  size_t sent = 0;
  uint32_t sequence = 0;
  while (sent < data.size()) {
    size_t chunk = std::min((size_t)ODIN_TRANSFER_SIZE, data.size() - sent);

    // Sequence header: [sequence(4)][size(4)][data...]
    std::vector<uint8_t> packet(8 + chunk);
    memcpy(packet.data(), &sequence, 4);
    uint32_t chunkSize = chunk;
    memcpy(packet.data() + 4, &chunkSize, 4);
    memcpy(packet.data() + 8, data.data() + sent, chunk);

    if (_transport->Send(packet.data(), packet.size(), 10000) !=
        (int)packet.size()) {
      std::cerr << "[ODIN] Flash chunk " << sequence << " send failed"
                << std::endl;
      return false;
    }

    // Wait for per-chunk ack on some devices
    if (sequence % 8 == 7) {
      WaitAck();
    }

    sent += chunk;
    sequence++;
  }

  // Send file transfer end
  SendOdinCommand(static_cast<uint32_t>(OdinPacketType::FileTransfer), 3, 0);
  if (!WaitAck())
    return false;

  _state = OdinSessionState::Complete;
  std::cout << "[ODIN] Flash complete: " << partName << std::endl;
  return true;
}

// ── EFS Operations ────────────────────────────────────────────────

std::vector<uint8_t> OdinManager::ReadEfs() {
  std::cout << "[ODIN] Reading EFS (nv_data)..." << std::endl;

  // EFS is stored in efs partition — read via PIT
  const OdinPartitionInfo *efsPart = FindPartition("efs");
  if (!efsPart && !(efsPart = FindPartition("EFS"))) {
    std::cerr << "[ODIN] EFS partition not found in PIT" << std::endl;
    return {};
  }

  // Send read request for efs partition
  // Protocol: request partition dump by index
  uint32_t partIdx = 0;
  for (size_t i = 0; i < _pitEntries.size(); i++) {
    if (_pitEntries[i].name == efsPart->name) {
      partIdx = i;
      break;
    }
  }

  // Send PIT-based read command
  if (!SendOdinCommand(static_cast<uint32_t>(OdinPacketType::FileTransfer), 1,
                       partIdx)) {
    return {};
  }

  // Receive size
  uint8_t sizeBuf[4] = {};
  if (_transport->Receive(sizeBuf, 4, 3000) != 4)
    return {};
  uint32_t efsSize = 0;
  memcpy(&efsSize, sizeBuf, 4);

  if (efsSize == 0 || efsSize > 64 * 1024 * 1024)
    return {};

  // Receive data
  std::vector<uint8_t> data(efsSize);
  size_t received = 0;
  while (received < efsSize) {
    size_t chunk = std::min((size_t)ODIN_TRANSFER_SIZE, efsSize - received);
    int got = _transport->Receive(data.data() + received, chunk, 10000);
    if (got <= 0)
      break;
    received += got;
  }

  if (received != efsSize) {
    std::cerr << "[ODIN] EFS read incomplete" << std::endl;
    return {};
  }

  WaitAck();
  std::cout << "[ODIN] EFS read complete: " << efsSize << " bytes" << std::endl;
  return data;
}

bool OdinManager::WriteEfs(const std::vector<uint8_t> &data) {
  std::cout << "[ODIN] Writing EFS (" << data.size() << " bytes)..."
            << std::endl;
  return FlashRawData("efs", data);
}

// ── Partition Read/Erase ──────────────────────────────────────────

bool OdinManager::ReadPartition(const std::string &name,
                                const std::string &outPath) {
  std::cout << "[ODIN] Reading partition " << name << " to " << outPath
            << std::endl;

  // Similar to ReadEfs but for arbitrary partition
  const OdinPartitionInfo *part = FindPartition(name);
  if (!part) {
    std::cerr << "[ODIN] Partition not found: " << name << std::endl;
    return false;
  }

  // Find partition index
  uint32_t partIdx = 0;
  for (size_t i = 0; i < _pitEntries.size(); i++) {
    if (_pitEntries[i].name == name) {
      partIdx = i;
      break;
    }
  }

  if (!SendOdinCommand(static_cast<uint32_t>(OdinPacketType::FileTransfer), 1,
                       partIdx)) {
    return false;
  }

  // Receive size
  uint8_t sizeBuf[4] = {};
  if (_transport->Receive(sizeBuf, 4, 3000) != 4)
    return false;
  uint32_t partSize = 0;
  memcpy(&partSize, sizeBuf, 4);

  if (partSize == 0 || partSize > 4ULL * 1024 * 1024 * 1024)
    return false;

  // Stream to file
  std::ofstream outFile(outPath, std::ios::binary);
  if (!outFile.is_open())
    return false;

  size_t received = 0;
  uint8_t chunkBuf[ODIN_TRANSFER_SIZE];
  while (received < partSize) {
    size_t chunk = std::min((size_t)ODIN_TRANSFER_SIZE, partSize - received);
    int got = _transport->Receive(chunkBuf, chunk, 10000);
    if (got <= 0)
      break;
    outFile.write(reinterpret_cast<char *>(chunkBuf), got);
    received += got;
  }

  outFile.close();
  WaitAck();

  std::cout << "[ODIN] Partition read: " << received << " bytes" << std::endl;
  return received == partSize;
}

bool OdinManager::ErasePartition(const std::string &name) {
  std::cout << "[ODIN] Erasing partition: " << name << std::endl;

  const OdinPartitionInfo *part = FindPartition(name);
  if (!part) {
    std::cerr << "[ODIN] Partition not found for erase: " << name << std::endl;
    return false;
  }

  // Send erase command (writing empty data effectively erases)
  std::vector<uint8_t> zeros(part->blockSize > 0 ? part->blockSize : 4096, 0);
  return FlashRawData(name, zeros);
}

// ── Low-Level Packet I/O ──────────────────────────────────────────

bool OdinManager::SendPacket(OdinPacketType type, const uint8_t *data,
                             size_t len) {
  std::vector<uint8_t> packet(4 + len);
  uint32_t typeVal = static_cast<uint32_t>(type);
  memcpy(packet.data(), &typeVal, 4);
  if (len > 0 && data) {
    memcpy(packet.data() + 4, data, len);
  }
  return _transport->Send(packet.data(), packet.size(), 5000) ==
         (int)packet.size();
}

bool OdinManager::ReceivePacket(uint32_t &responseType,
                                std::vector<uint8_t> &data) {
  uint8_t header[4] = {};
  if (_transport->Receive(header, 4, 5000) != 4)
    return false;
  memcpy(&responseType, header, 4);

  // For most Odin responses, the response type is followed by data
  // Read up to ODIN_TRANSFER_SIZE
  data.resize(ODIN_TRANSFER_SIZE);
  int received = _transport->Receive(data.data(), data.size(), 5000);
  if (received > 0) {
    data.resize(received);
  } else {
    data.clear();
  }
  return true;
}

bool OdinManager::SendOdinCommand(uint32_t cmd, uint32_t arg1, uint32_t arg2) {
  uint8_t payload[8];
  memcpy(payload, &arg1, 4);
  memcpy(payload + 4, &arg2, 4);
  return SendPacket(static_cast<OdinPacketType>(cmd), payload, 8);
}

bool OdinManager::WaitAck() {
  uint8_t ack[4] = {};
  int received = _transport->Receive(ack, 4, 5000);
  if (received != 4)
    return false;

  // Odin ACK: first byte should be 0x00 for success
  return ack[0] == 0x00;
}

const OdinPartitionInfo *
OdinManager::FindPartition(const std::string &name) const {
  for (const auto &entry : _pitEntries) {
    if (entry.name == name)
      return &entry;
  }
  // Case-insensitive fallback
  for (const auto &entry : _pitEntries) {
    if (strcasecmp(entry.name.c_str(), name.c_str()) == 0)
      return &entry;
  }
  return nullptr;
}

} // namespace Protocols
} // namespace DeepEye
