#include "../../include/sparse_handler.h"
#include <cstring>

namespace DeepEye {
namespace Protocols {

bool SparseImageHandler::IsSparse(const uint8_t *buffer) {
  uint32_t magic;
  memcpy(&magic, buffer, 4);
  return (magic == 0xed26ff3a);
}

uint64_t SparseImageHandler::GetUnsparseSize(const uint8_t *buffer) {
  if (!IsSparse(buffer))
    return 0;

  SparseHeader header;
  memcpy(&header, buffer, sizeof(SparseHeader));

  return (uint64_t)header.blk_sz * header.total_blks;
}

} // namespace Protocols
} // namespace DeepEye
