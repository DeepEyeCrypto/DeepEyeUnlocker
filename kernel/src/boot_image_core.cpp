#include "../include/boot_image.h"
#include <algorithm>
#include <cstring>
#include <fstream>
#include <iostream>

namespace deepeye {

static size_t align_to_page(size_t size, uint32_t page_size) {
  return ((size + page_size - 1) / page_size) * page_size;
}

bool BootImage::load(const std::string &path) {
  std::ifstream file(path, std::ios::binary);
  if (!file)
    return false;

  // Read magic and version first
  uint8_t magic[8];
  file.read(reinterpret_cast<char *>(magic), 8);
  if (memcmp(magic, BOOT_MAGIC, 8) != 0)
    return false;

  // Get header version (offset 40 in v0-v2, offset 36 in v3-v4)
  file.seekg(40);
  file.read(reinterpret_cast<char *>(&version), 4);

  // Safety check for v3/v4 version location
  if (version > 4) {
    file.seekg(36);
    file.read(reinterpret_cast<char *>(&version), 4);
  }

  file.seekg(0);
  if (version <= 2) {
    boot_img_hdr_v2 hdr;
    file.read(reinterpret_cast<char *>(&hdr), sizeof(hdr));

    uint32_t page_size = hdr.page_size;

    // Kernel
    kernel.resize(hdr.kernel_size);
    file.seekg(page_size);
    file.read(reinterpret_cast<char *>(kernel.data()), hdr.kernel_size);

    // Ramdisk
    size_t ramdisk_offset =
        align_to_page(page_size + hdr.kernel_size, page_size);
    ramdisk.resize(hdr.ramdisk_size);
    file.seekg(ramdisk_offset);
    file.read(reinterpret_cast<char *>(ramdisk.data()), hdr.ramdisk_size);

    // DTB (v2 only)
    if (version == 2 && hdr.dtb_size > 0) {
      size_t dtb_offset =
          align_to_page(ramdisk_offset + hdr.ramdisk_size, page_size);
      dtb.resize(hdr.dtb_size);
      file.seekg(dtb_offset);
      file.read(reinterpret_cast<char *>(dtb.data()), hdr.dtb_size);
    }

    cmdline = reinterpret_cast<char *>(hdr.cmdline);
  } else {
    // V3/V4 Implementation
    boot_img_hdr_v4 hdr;
    file.read(reinterpret_cast<char *>(&hdr), sizeof(hdr));

    uint32_t page_size = 4096; // Fixed in v3+

    kernel.resize(hdr.kernel_size);
    file.seekg(page_size);
    file.read(reinterpret_cast<char *>(kernel.data()), hdr.kernel_size);

    size_t ramdisk_offset =
        align_to_page(page_size + hdr.kernel_size, page_size);
    ramdisk.resize(hdr.ramdisk_size);
    file.seekg(ramdisk_offset);
    file.read(reinterpret_cast<char *>(ramdisk.data()), hdr.ramdisk_size);

    cmdline = reinterpret_cast<char *>(hdr.cmdline);
  }

  return true;
}

bool BootImage::save(const std::string &path) {
  std::ofstream file(path, std::ios::binary);
  if (!file)
    return false;

  uint32_t page_size = (version <= 2) ? 2048 : 4096;

  if (version <= 2) {
    boot_img_hdr_v2 hdr;
    memset(&hdr, 0, sizeof(hdr));
    memcpy(hdr.magic, BOOT_MAGIC, 8);
    hdr.kernel_size = kernel.size();
    hdr.ramdisk_size = ramdisk.size();
    hdr.dtb_size = dtb.size();
    hdr.page_size = page_size;
    hdr.header_version = version;
    strncpy(reinterpret_cast<char *>(hdr.cmdline), cmdline.c_str(),
            BOOT_ARGS_SIZE - 1);

    file.write(reinterpret_cast<char *>(&hdr),
               page_size); // Header padded to page
    file.write(reinterpret_cast<char *>(kernel.data()), kernel.size());

    // Padding
    size_t k_pad = align_to_page(kernel.size(), page_size) - kernel.size();
    if (k_pad > 0) {
      std::vector<uint8_t> pad(k_pad, 0);
      file.write(reinterpret_cast<char *>(pad.data()), k_pad);
    }

    file.write(reinterpret_cast<char *>(ramdisk.data()), ramdisk.size());
  }

  return true;
}

} // namespace deepeye
