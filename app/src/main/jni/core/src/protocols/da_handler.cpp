#include "../../include/da_handler.h"
#include <cstring>

namespace DeepEye {
namespace Protocols {

bool DaHandler::ValidateDA(const uint8_t *buffer, size_t size) {
  if (size < sizeof(DaHeader))
    return false;

  DaHeader header;
  memcpy(&header, buffer, sizeof(DaHeader));
  // Magic: "MTK_DA" (reversed byte order in some versions)
  return (header.magic == 0x4D544B5F || header.magic == 0x5F4B544D);
}

std::vector<DaSection> DaHandler::ParseSections(const uint8_t *buffer,
                                                size_t size) {
  std::vector<DaSection> sections;
  if (!ValidateDA(buffer, size))
    return sections;

  DaHeader header;
  memcpy(&header, buffer, sizeof(DaHeader));

  const uint8_t *current = buffer + sizeof(DaHeader);
  for (uint32_t i = 0; i < header.da_count; ++i) {
    if ((current + sizeof(DaSection)) > (buffer + size))
      break;

    DaSection sec;
    memcpy(&sec, current, sizeof(DaSection));
    sections.push_back(sec);
    current += sizeof(DaSection);
  }

  return sections;
}

} // namespace Protocols
} // namespace DeepEye
