#ifndef DEEPEYE_GPT_PARSER_H
#define DEEPEYE_GPT_PARSER_H

#include <stdint.h>
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

#pragma pack(push, 1)
struct GptHeader {
  uint64_t signature; // "EFI PART"
  uint32_t revision;
  uint32_t headerSize;
  uint32_t headerCrc32;
  uint32_t reserved;
  uint64_t currentLba;
  uint64_t backupLba;
  uint64_t firstUsableLba;
  uint64_t lastUsableLba;
  uint8_t diskGuid[16];
  uint64_t partitionEntryLba;
  uint32_t numPartitionEntries;
  uint32_t partitionEntrySize;
  uint32_t partitionEntriesCrc32;
};

struct GptEntry {
  uint8_t partitionTypeGuid[16];
  uint8_t uniquePartitionGuid[16];
  uint64_t startingLba;
  uint64_t endingLba;
  uint64_t attributes;
  uint16_t partitionName[36]; // UTF-16
};
#pragma pack(pop)

struct PartitionInfo {
  std::string name;
  uint64_t startLba;
  uint64_t endLba;
  uint64_t sizeInBytes;
};

class GptParser {
public:
  static bool ParseHeader(const uint8_t *buffer, GptHeader &header);
  static std::vector<PartitionInfo> ParseEntries(const uint8_t *buffer,
                                                 uint32_t count, uint32_t size,
                                                 uint32_t sectorSize = 512);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_GPT_PARSER_H
