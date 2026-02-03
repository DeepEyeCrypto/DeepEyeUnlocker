#ifndef DEEPEYE_FIREHOSE_H
#define DEEPEYE_FIREHOSE_H

#include <map>
#include <string>
#include <vector>

namespace DeepEye {
namespace Protocols {

class FirehoseClient {
public:
  static std::string
  CreateConfigureXml(uint32_t sectorSize = 512,
                     const std::string &storageType = "emmc");
  static std::string CreateReadXml(const std::string &partitionName,
                                   uint64_t sectorOffset, uint64_t sectorCount);
  static std::string CreateWriteXml(const std::string &partitionName,
                                    uint64_t sectorOffset,
                                    uint64_t sectorCount);
  static std::string CreateEraseXml(const std::string &partitionName);
  static std::string CreateGetGptXml();

  struct Response {
    bool success;
    std::string raw;
    std::map<std::string, std::string> attributes;
  };

  static Response ParseResponse(const std::string &xml);
};

} // namespace Protocols
} // namespace DeepEye

#endif // DEEPEYE_FIREHOSE_H
