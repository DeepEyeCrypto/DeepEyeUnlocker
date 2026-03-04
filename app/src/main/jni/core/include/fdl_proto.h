#ifndef DEEPEYE_FDL_PROTO_H
#define DEEPEYE_FDL_PROTO_H

#include "deepeye_core.h"
#include "gpt_parser.h"
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

// ═══════════════════════════════════════════════════════════════════
//  UniSoc (Spreadtrum) FDL Protocol
//
//  UniSoc devices use FDL (Firmware DownLoader) protocol:
//    1. FDL1 handshake (boot ROM stage)
//    2. FDL2 load (download agent)
//    3. PAC firmware flash (proprietary package format)
//    4. NV (calibration/IMEI) read/write
//
//  Packet format: [0x7E][type(2)][size(2)][payload][checksum(2)][0x7E]
// ═══════════════════════════════════════════════════════════════════

// FDL command types
enum class FdlCommand : uint16_t {
  Connect = 0x0000,
  ConnectAck = 0x0080,
  DataStart = 0x0001,
  DataMid = 0x0002,
  DataEnd = 0x0003,
  DataAck = 0x0081,
  Exec = 0x0004,
  ReadFlash = 0x0005,
  ReadAck = 0x0085,
  EraseFlash = 0x0006,
  EraseAck = 0x0086,
  ReadNv = 0x0007,
  ReadNvAck = 0x0087,
  WriteNv = 0x0008,
  WriteNvAck = 0x0088,
  Repartition = 0x0009,
  DeviceInfo = 0x000A,
  DeviceInfoAck = 0x008A,
  Reset = 0x000F
};

// FDL session states
enum class FdlSessionState {
  Disconnected,
  FDL1Connected,
  FDL2Active,
  Flashing,
  Complete,
  Error
};

class FdlManager {
public:
  FdlManager(Core::ITransport *transport);

  // ── Session Management ─────────────────────────────────────
  bool Handshake();
  bool LoadFdl2(const std::vector<uint8_t> &fdl2Data);
  void Reset();
  FdlSessionState GetState() const;

  // ── Flash Operations ───────────────────────────────────────
  bool FlashPac(const std::string &pacPath);
  bool FlashPartition(const std::string &name, const std::string &imagePath);
  bool FlashRawData(const std::string &name, const std::vector<uint8_t> &data);

  // ── Partition Operations ───────────────────────────────────
  std::vector<PartitionInfo> GetPartitions();
  bool ReadPartition(const std::string &name, const std::string &outPath);
  bool ErasePartition(const std::string &name);

  // ── NV Operations ──────────────────────────────────────────
  std::vector<uint8_t> ReadNv(int nvId);
  bool WriteNv(int nvId, const std::vector<uint8_t> &data);

  // ── Device Info ────────────────────────────────────────────
  std::string GetDeviceInfo();

private:
  Core::ITransport *_transport;
  FdlSessionState _state;

  // HDLC-framed packet I/O
  bool SendFdlPacket(FdlCommand cmd, const uint8_t *data, size_t len);
  bool ReceiveFdlPacket(FdlCommand &cmd, std::vector<uint8_t> &data);
  bool WaitResponse(FdlCommand expectedAck);

  // HDLC framing
  std::vector<uint8_t> HdlcEncode(const std::vector<uint8_t> &raw);
  std::vector<uint8_t> HdlcDecode(const uint8_t *data, size_t len);
  uint16_t CalcChecksum(const uint8_t *data, size_t len);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_FDL_PROTO_H
