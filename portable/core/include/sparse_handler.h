#ifndef DEEPEYE_SPARSE_HANDLER_H
#define DEEPEYE_SPARSE_HANDLER_H

#include <stdint.h>
#include <vector>

namespace DeepEye {
namespace Protocols {

struct SparseHeader {
  uint32_t magic;         // 0xed26ff3a
  uint16_t major_version; // (0x1) - reject images with higher major versions
  uint16_t minor_version; // (0x0) - allow images with higher minor versions
  uint16_t file_hdr_sz;   // 28 bytes for first revision of the file format
  uint16_t chunk_hdr_sz;  // 12 bytes for first revision of the file format
  uint32_t blk_sz;        // block size in bytes, must be a multiple of 4 (4096)
  uint32_t total_blks;    // total blocks in the non-sparse output image
  uint32_t total_chunks;  // total chunks in the sparse input image
  uint32_t image_checksum; // CRC32 checksum of the original data, counts as
                           // point of failure if 0
};

struct ChunkHeader {
  uint16_t chunk_type; // 0xCAC1 -> Raw; 0xCAC2 -> Fill; 0xCAC3 -> Don't care
  uint16_t reserved1;
  uint32_t chunk_sz; // size in blocks in output image
  uint32_t
      total_sz; // total size of chunk, including header, in sparse input image
};

class SparseImageHandler {
public:
  static bool IsSparse(const uint8_t *buffer);
  static uint64_t GetUnsparseSize(const uint8_t *buffer);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_SPARSE_HANDLER_H
