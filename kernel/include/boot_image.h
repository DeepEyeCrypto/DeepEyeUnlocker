#ifndef BOOT_IMAGE_H
#define BOOT_IMAGE_H

#include <cstdint>
#include <string>
#include <vector>

namespace deepeye {

/**
 * Android Boot Image Header Structures (v0-v4)
 * Reference:
 * https://android.googlesource.com/platform/system/tools/mkbootimg/+/refs/heads/master/include/bootimg/bootimg.h
 */

#define BOOT_MAGIC "ANDROID!"
#define BOOT_MAGIC_SIZE 8
#define BOOT_NAME_SIZE 16
#define BOOT_ARGS_SIZE 512
#define BOOT_EXTRA_ARGS_SIZE 1024

struct boot_img_hdr_v0 {
  uint8_t magic[BOOT_MAGIC_SIZE];
  uint32_t kernel_size;
  uint32_t kernel_addr;
  uint32_t ramdisk_size;
  uint32_t ramdisk_addr;
  uint32_t second_size;
  uint32_t second_addr;
  uint32_t tags_addr;
  uint32_t page_size;
  uint32_t header_version;
  uint32_t os_version;
  uint8_t name[BOOT_NAME_SIZE];
  uint8_t cmdline[BOOT_ARGS_SIZE];
  uint32_t id[8];
  uint8_t extra_cmdline[BOOT_EXTRA_ARGS_SIZE];
};

struct boot_img_hdr_v1 : public boot_img_hdr_v0 {
  uint32_t recovery_dtbo_size;
  uint64_t recovery_dtbo_offset;
  uint32_t header_size;
};

struct boot_img_hdr_v2 : public boot_img_hdr_v1 {
  uint32_t dtb_size;
  uint64_t dtb_addr;
};

// V3 and V4 are significantly different (no more load addresses)
struct boot_img_hdr_v3 {
  uint8_t magic[BOOT_MAGIC_SIZE];
  uint32_t kernel_size;
  uint32_t ramdisk_size;
  uint32_t os_version;
  uint32_t header_size;
  uint32_t reserved[4];
  uint32_t header_version;
  uint8_t cmdline[BOOT_ARGS_SIZE + BOOT_EXTRA_ARGS_SIZE];
};

struct boot_img_hdr_v4 : public boot_img_hdr_v3 {
  uint32_t signature_size;
};

class BootImage {
public:
  uint32_t version;
  std::vector<uint8_t> kernel;
  std::vector<uint8_t> ramdisk;
  std::vector<uint8_t> dtb;
  std::string cmdline;

  BootImage() : version(0) {}

  bool load(const std::string &path);
  bool save(const std::string &path);

  // Unpack components to directory
  bool unpack(const std::string &out_dir);

  // Repack from components
  bool repack(const std::string &in_dir);
};

} // namespace deepeye

#endif // BOOT_IMAGE_H
