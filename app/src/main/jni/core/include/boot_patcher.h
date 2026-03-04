#ifndef DEEPEYE_BOOT_PATCHER_H
#define DEEPEYE_BOOT_PATCHER_H

#include <string>
#include <vector>

namespace DeepEye {
namespace Core {

enum class PatchMethod { Magisk, KernelSU, Custom };

class BootImagePatcher {
public:
  BootImagePatcher(const std::string &workDir);

  /**
   * Patches an Android boot.img using the specified method.
   * This assumes the patcher binaries (magiskboot/ksu) are present in the
   * workDir.
   */
  bool Patch(const std::string &inputPath, const std::string &outputPath,
             PatchMethod method);

  bool ExtractBoot(const std::string &inputPath);
  bool RepackBoot(const std::string &outputPath);

private:
  std::string _workDir;
  bool RunCommand(const std::string &cmd);
};

} // namespace Core
} // namespace DeepEye

#endif // DEEPEYE_BOOT_PATCHER_H
