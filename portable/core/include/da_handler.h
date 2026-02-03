#ifndef DEEPEYE_DA_HANDLER_H
#define DEEPEYE_DA_HANDLER_H

#include <stdint.h>
#include <vector>

namespace DeepEye {
namespace Protocols {

#pragma pack(push, 1)
struct DaHeader {
  uint32_t magic; // 0x4D544B5F
  uint32_t version;
  uint32_t da_count;
};

struct DaSection {
  uint32_t da_index;
  uint32_t da_offset;
  uint32_t da_size;
  uint32_t da_address;
  uint32_t sig_offset;
  uint32_t sig_size;
};
#pragma pack(pop)

class DaHandler {
public:
  static bool ValidateDA(const uint8_t *buffer, size_t size);
  static std::vector<DaSection> ParseSections(const uint8_t *buffer,
                                              size_t size);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_DA_HANDLER_H
