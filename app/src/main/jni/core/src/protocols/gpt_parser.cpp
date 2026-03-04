#include "../../include/gpt_parser.h"
#include <codecvt>
#include <cstring>
#include <locale>

namespace DeepEye {
namespace Protocols {

bool GptParser::ParseHeader(const uint8_t *buffer, GptHeader &header) {
  memcpy(&header, buffer, sizeof(GptHeader));
  // Signature check: "EFI PART"
  return header.signature == 0x5452415020494645;
}

std::string Utf16ToUtf8(const uint16_t *utf16, size_t maxLen) {
  std::string result;
  for (size_t i = 0; i < maxLen && utf16[i] != 0; ++i) {
    uint16_t c = utf16[i];
    if (c < 0x80)
      result += (char)c;
    else if (c < 0x800) {
      result += (char)(0xC0 | (c >> 6));
      result += (char)(0x80 | (c & 0x3F));
    }
  }
  return result;
}

std::vector<PartitionInfo> GptParser::ParseEntries(const uint8_t *buffer,
                                                   uint32_t count,
                                                   uint32_t size,
                                                   uint32_t sectorSize) {
  std::vector<PartitionInfo> partitions;

  for (uint32_t i = 0; i < count; ++i) {
    const GptEntry *entry = (const GptEntry *)(buffer + (i * size));

    bool isEmpty = true;
    for (int b = 0; b < 16; ++b) {
      if (entry->partitionTypeGuid[b] != 0) {
        isEmpty = false;
        break;
      }
    }

    if (isEmpty)
      continue;

    PartitionInfo info;
    info.name = Utf16ToUtf8(entry->partitionName, 36);
    info.startLba = entry->startingLba;
    info.endLba = entry->endingLba;
    info.sizeInBytes = (entry->endingLba - entry->startingLba + 1) * sectorSize;

    partitions.push_back(info);
  }

  return partitions;
}

} // namespace Protocols
} // namespace DeepEye
