#include "../../include/firehose.h"
#include <iomanip>
#include <sstream>

namespace DeepEye {
namespace Protocols {

std::string FirehoseClient::CreateConfigureXml(uint32_t sectorSize,
                                               const std::string &storageType) {
  std::stringstream ss;
  ss << "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
  ss << "<data>\n";
  ss << "  <configure verbose=\"0\" AlwaysValidate=\"0\" "
        "MaxPayloadSizeToTargetInBytes=\"1048576\" ";
  ss << "MemoryName=\"" << storageType << "\" TargetName=\"MSM8998\" />\n";
  ss << "</data>";
  return ss.str();
}

std::string FirehoseClient::CreateReadXml(const std::string &partitionName,
                                          uint64_t sectorOffset,
                                          uint64_t sectorCount) {
  std::stringstream ss;
  ss << "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
  ss << "<data>\n";
  ss << "  <read SECTOR_SIZE_IN_BYTES=\"512\" num_partition_sectors=\""
     << sectorCount << "\" ";
  ss << "physical_partition_number=\"0\" start_sector=\"" << sectorOffset
     << "\" />\n";
  ss << "</data>";
  return ss.str();
}

std::string FirehoseClient::CreateWriteXml(const std::string &partitionName,
                                           uint64_t sectorOffset,
                                           uint64_t sectorCount) {
  std::stringstream ss;
  ss << "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
  ss << "<data>\n";
  ss << "  <program SECTOR_SIZE_IN_BYTES=\"512\" num_partition_sectors=\""
     << sectorCount << "\" ";
  ss << "physical_partition_number=\"0\" start_sector=\"" << sectorOffset
     << "\" filename=\"" << partitionName << ".img\" />\n";
  ss << "</data>";
  return ss.str();
}

std::string FirehoseClient::CreateEraseXml(const std::string &partitionName) {
  std::stringstream ss;
  ss << "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
  ss << "<data>\n";
  ss << "  <erase physical_partition_number=\"0\" partition_name=\""
     << partitionName << "\" />\n";
  ss << "</data>";
  return ss.str();
}

FirehoseClient::Response FirehoseClient::ParseResponse(const std::string &xml) {
  Response resp;
  resp.raw = xml;
  resp.success = (xml.find("value=\"ACK\"") != std::string::npos ||
                  xml.find("value=\"ack\"") != std::string::npos);

  size_t pos = 0;
  while ((pos = xml.find(" ", pos)) != std::string::npos) {
    size_t eq = xml.find("=\"", pos);
    if (eq != std::string::npos) {
      std::string key = xml.substr(pos + 1, eq - pos - 1);
      size_t end = xml.find("\"", eq + 2);
      if (end != std::string::npos) {
        std::string val = xml.substr(eq + 2, end - eq - 2);
        resp.attributes[key] = val;
        pos = end;
      } else
        break;
    } else
      break;
  }

  return resp;
}

} // namespace Protocols
} // namespace DeepEye
