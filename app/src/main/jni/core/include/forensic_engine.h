#ifndef DEEPEYE_FORENSIC_ENGINE_H
#define DEEPEYE_FORENSIC_ENGINE_H

#include "deepeye_core.h"
#include <functional>
#include <string>
#include <vector>

namespace DeepEye {
namespace Forensics {

struct CarvedFile {
  std::string fileName;
  std::string fileType;
  size_t startOffset;
  size_t size;
  bool isDeleted;
};

using CarveProgressCallback =
    std::function<void(int progress, const std::string &message)>;

class ForensicEngine {
public:
  ForensicEngine(Core::ProtocolEngine *engine);

  /**
   * Safe Dump: Creates a full binary image of the specified partition.
   * Leverages the protocol engine's read commands.
   */
  bool SafeDump(const std::string &partitionName, const std::string &outPath,
                CarveProgressCallback callback);

  /**
   * Deleted Data Carving: Scans the specified partition for file carvings
   * based on signature matching (Magic bytes).
   */
  std::vector<CarvedFile>
  CarveDeletedData(const std::string &partitionName,
                   const std::vector<std::string> &types,
                   CarveProgressCallback callback);

  /**
   * Acquire Image: Performs a forensic acquisition with hashing (SHA-256).
   */
  std::string AcquireForensicImage(const std::string &partitionName,
                                   const std::string &outDir,
                                   CarveProgressCallback callback);

  /**
   * Decrypts the filesystem using provided master key.
   */
  bool DecryptFileSystem(const std::string &partition, const std::vector<uint8_t> &key);

  /**
   * Check if a specific volume is available in the decrypted filesystem.
   */
  bool CheckVolume(const std::string &volumeName);

  /**
   * Extracts Adoptable Storage (SD Card) key from the userdata partition.
   */
  std::vector<uint8_t> ExtractAdoptableStorageKey(const std::string &partition);

  /**
   * Lists directory contents of a decrypted filesystem.
   */
  std::string ListDirectory(const std::string &partition, const std::string &path);

  /**
   * Reads a raw file from a decrypted filesystem.
   */
  std::vector<uint8_t> ReadFile(const std::string &partition, const std::string &path);

  /**
   * Stage 600.1 — Analyze USB signal integrity.
   */
  std::string ExaminePhysicalIntegrity();

private:
  Core::ProtocolEngine *_engine;

  bool ScanSignatures(const std::string &chunk,
                      std::vector<CarvedFile> &foundFiles);
  std::string CalculateHash(const std::string &filePath);
};

} // namespace Forensics
} // namespace DeepEye

#endif // DEEPEYE_FORENSIC_ENGINE_H
