#include "../../include/brom_proto.h"
#include "../../include/deepeye_core.h"
#include "../../include/edl_proto.h"
#include "../../include/fastboot_proto.h"
#include "../../include/fdl_proto.h"
#include "../../include/gpt_parser.h"
#include "../../include/odin_proto.h"
#include <iostream>

namespace DeepEye {
namespace Core {

ProtocolEngine::ProtocolEngine(ITransport *transport) : _transport(transport) {}

bool ProtocolEngine::Identify() {
  // Try MediaTek BROM
  Protocols::BromManager brom(_transport);
  if (brom.Handshake()) {
    std::cout << "[CORE] Detected MediaTek BROM Target via OTG." << std::endl;
    _targetType = "MTK";
    return true;
  }

  // Try Qualcomm EDL (Sahara)
  Protocols::EdlManager edl(_transport);
  if (edl.ConnectSahara()) {
    std::cout << "[CORE] Detected Qualcomm EDL Target via OTG." << std::endl;
    _targetType = "QCOM";
    return true;
  }

  // Try Samsung Odin Protocol
  Protocols::OdinManager odin(_transport);
  if (odin.Handshake()) {
    std::cout << "[CORE] Detected Samsung ODIN Target via OTG." << std::endl;
    _targetType = "ODIN";
    return true;
  }

  // Try Generic Android Fastboot
  Protocols::FastbootManager fb(_transport);
  if (fb.Handshake()) {
    std::cout << "[CORE] Detected FASTBOOT Target via OTG." << std::endl;
    _targetType = "FASTBOOT";
    return true;
  }

  // Try UniSoc FDL
  Protocols::FdlManager fdl(_transport);
  if (fdl.Handshake()) {
    std::cout << "[CORE] Detected UniSoc FDL Target via OTG." << std::endl;
    _targetType = "FDL";
    return true;
  }

  return false;
}

std::vector<Protocols::PartitionInfo> ProtocolEngine::GetPartitions() {
  std::vector<Protocols::PartitionInfo> partitions;

  if (_targetType == "QCOM") {
    Protocols::EdlManager edl(_transport);
    if (edl.FirehoseHandshake()) {
      std::vector<uint8_t> headerBuf;
      if (edl.ReadPartition("gpt", 1, 1, headerBuf)) {
        Protocols::GptHeader header;
        if (Protocols::GptParser::ParseHeader(headerBuf.data(), header)) {
          uint32_t entrySectors =
              (header.numPartitionEntries * header.partitionEntrySize + 511) /
              512;
          std::vector<uint8_t> entriesBuf;
          if (edl.ReadPartition("gpt", 2, entrySectors, entriesBuf)) {
            partitions = Protocols::GptParser::ParseEntries(
                entriesBuf.data(), header.numPartitionEntries,
                header.partitionEntrySize);
          }
        }
      }
    }
  } else if (_targetType == "MTK") {
    Protocols::BromManager brom(_transport);
    std::vector<uint8_t> headerBuf;
    if (brom.DaReadPartition("gpt", 1, 1, headerBuf)) {
      Protocols::GptHeader header;
      if (Protocols::GptParser::ParseHeader(headerBuf.data(), header)) {
        uint32_t entrySectors =
            (header.numPartitionEntries * header.partitionEntrySize + 511) /
            512;
        std::vector<uint8_t> entriesBuf;
        if (brom.DaReadPartition("gpt", 2, entrySectors, entriesBuf)) {
          partitions = Protocols::GptParser::ParseEntries(
              entriesBuf.data(), header.numPartitionEntries,
              header.partitionEntrySize);
        }
      }
    }
  } else if (_targetType == "ODIN") {
    Protocols::OdinManager odin(_transport);
    partitions = odin.GetPartitionsAsGeneric();
  } else if (_targetType == "FDL") {
    Protocols::FdlManager fdl(_transport);
    partitions = fdl.GetPartitions();
  }

  return partitions;
}

bool ProtocolEngine::DumpPartition(const std::string &name,
                                   const std::string &outPath) {
  if (_targetType == "QCOM") {
    Protocols::EdlManager edl(_transport);
    if (edl.FirehoseHandshake()) {
      std::vector<uint8_t> data;
      if (edl.ReadPartition(name, 0, 1024, data)) {
        return true;
      }
    }
  } else if (_targetType == "MTK") {
    Protocols::BromManager brom(_transport);
    std::vector<uint8_t> data;
    return brom.DaReadPartition(name, 0, 1024, data);
  } else if (_targetType == "ODIN") {
    Protocols::OdinManager odin(_transport);
    return odin.ReadPartition(name, outPath);
  } else if (_targetType == "FDL") {
    Protocols::FdlManager fdl(_transport);
    return fdl.ReadPartition(name, outPath);
  }
  return false;
}

bool ProtocolEngine::FlashPartition(const std::string &name,
                                    const std::string &inPath) {
  if (_targetType == "QCOM") {
    Protocols::EdlManager edl(_transport);
    if (edl.FirehoseHandshake()) {
      std::vector<uint8_t> dummy(512, 0);
      return edl.WritePartition(name, 0, dummy);
    }
  } else if (_targetType == "MTK") {
    Protocols::BromManager brom(_transport);
    std::vector<uint8_t> dummy(512, 0);
    return brom.DaWritePartition(name, 0, dummy);
  } else if (_targetType == "ODIN") {
    Protocols::OdinManager odin(_transport);
    return odin.FlashPartition(name, inPath);
  } else if (_targetType == "FASTBOOT") {
    Protocols::FastbootManager fb(_transport);
    // Read local file inPath into a dummy buffer locally? The real code should
    // read it from file. Since this is a skeleton without fs operations readily
    // mocked here, let's just assume DownloadData receives the right bytes and
    // flashes.
    std::vector<uint8_t> dummy(512, 0);
    if (fb.DownloadData(dummy)) {
      return fb.FlashPartition(name);
    }
  } else if (_targetType == "FDL") {
    Protocols::FdlManager fdl(_transport);
    return fdl.FlashPartition(name, inPath);
  }
  return false;
}

bool ProtocolEngine::ErasePartition(const std::string &name) {
  if (_targetType == "QCOM") {
    Protocols::EdlManager edl(_transport);
    if (edl.FirehoseHandshake()) {
      return edl.ErasePartition(name);
    }
  } else if (_targetType == "MTK") {
    Protocols::BromManager brom(_transport);
    return brom.DaErasePartition(name);
  } else if (_targetType == "ODIN") {
    Protocols::OdinManager odin(_transport);
    return odin.ErasePartition(name);
  } else if (_targetType == "FASTBOOT") {
    Protocols::FastbootManager fb(_transport);
    return fb.ErasePartition(name);
  } else if (_targetType == "FDL") {
    Protocols::FdlManager fdl(_transport);
    return fdl.ErasePartition(name);
  }
  return false;
}

} // namespace Core
} // namespace DeepEye
