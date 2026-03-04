#ifndef DEEPEYE_ODIN_PROTO_H
#define DEEPEYE_ODIN_PROTO_H

#include "deepeye_core.h"
#include "gpt_parser.h"
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

// ═══════════════════════════════════════════════════════════════════
//  Samsung Odin Protocol — Download Mode communication
//
//  Odin uses a proprietary protocol over USB bulk transfers:
//    1. Session begin (0x64 handshake)
//    2. PIT (Partition Information Table) exchange
//    3. File transfer (TAR.MD5 or raw images)
//    4. Session end
//
//  Packet format: 4-byte type + variable payload
// ═══════════════════════════════════════════════════════════════════

// Odin packet types
enum class OdinPacketType : uint32_t {
  SessionSetup = 0x64, // Session begin/end
  PitRequest = 0x65,   // Request PIT
  FileTransfer = 0x66, // Flash file
  PitTransfer = 0x65,  // PIT data exchange
  EndSession = 0x67,   // Close session
};

// Odin session states
enum class OdinSessionState {
  Disconnected,
  Handshaking,
  SessionActive,
  PitLoaded,
  Flashing,
  Complete,
  Error
};

// PIT entry (Samsung Partition Information Table)
#pragma pack(push, 1)
struct PitEntry {
  uint32_t binary_type; // 0=AP, 1=CP, 2=CSC
  uint32_t device_type; // 0=OneNAND, 1=NAND, 2=EMMC
  uint32_t identifier;
  uint32_t attributes;
  uint32_t update_attributes;
  uint32_t block_size;
  uint32_t block_count;
  uint32_t file_offset;
  uint32_t file_size;
  char partition_name[32];
  char flash_filename[32];
  char fota_filename[32];
};
#pragma pack(pop)

// Parsed PIT partition info
struct OdinPartitionInfo {
  std::string name;
  std::string flashFilename;
  uint32_t binaryType; // 0=AP, 1=CP, 2=CSC
  uint32_t blockSize;
  uint32_t blockCount;
  uint64_t sizeInBytes;
};

class OdinManager {
public:
  OdinManager(Core::ITransport *transport);

  // ── Session Management ─────────────────────────────────────
  bool Handshake();
  void EndSession();
  OdinSessionState GetState() const;

  // ── PIT Operations ─────────────────────────────────────────
  bool RequestPit();
  std::vector<uint8_t> ReadPit();
  std::vector<OdinPartitionInfo> ParsePit(const std::vector<uint8_t> &pitData);
  std::vector<PartitionInfo> GetPartitionsAsGeneric();

  // ── Flash Operations ───────────────────────────────────────
  bool FlashPartition(const std::string &partName,
                      const std::string &imagePath);
  bool FlashRawData(const std::string &partName,
                    const std::vector<uint8_t> &data);

  // ── EFS Operations ─────────────────────────────────────────
  std::vector<uint8_t> ReadEfs();
  bool WriteEfs(const std::vector<uint8_t> &data);

  // ── Partition Read/Erase ───────────────────────────────────
  bool ReadPartition(const std::string &name, const std::string &outPath);
  bool ErasePartition(const std::string &name);

private:
  Core::ITransport *_transport;
  OdinSessionState _state;
  std::vector<OdinPartitionInfo> _pitEntries;

  // Low-level packet I/O
  bool SendPacket(OdinPacketType type, const uint8_t *data, size_t len);
  bool ReceivePacket(uint32_t &responseType, std::vector<uint8_t> &data);
  bool SendOdinCommand(uint32_t cmd, uint32_t arg1 = 0, uint32_t arg2 = 0);
  bool WaitAck();

  // PIT lookup
  const OdinPartitionInfo *FindPartition(const std::string &name) const;
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_ODIN_PROTO_H
