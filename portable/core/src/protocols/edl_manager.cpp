#include "../../include/edl_proto.h"
#include "../../include/firehose.h"
#include <cstring>
#include <iostream>

namespace DeepEye {
namespace Protocols {

EdlManager::EdlManager(Core::ITransport *transport) : _transport(transport) {}

bool EdlManager::ConnectSahara() {
  std::cout << "[EDL] Initiating Sahara Handshake..." << std::endl;

  SaharaCommand cmd;
  std::vector<uint8_t> data;

  if (!ReceiveSaharaPacket(cmd, data)) {
    std::cerr << "[EDL] Failed to receive HELLO from target." << std::endl;
    return false;
  }

  if (cmd != SaharaCommand::Hello) {
    std::cerr << "[EDL] Unexpected command: " << (int)cmd << std::endl;
    return false;
  }

  // Send Hello Response
  uint32_t resp[] = {0x01, 0x30, 0x02, 0x01, 0, 0, 0, 0}; // Simplified response
  return SendSaharaPacket(SaharaCommand::HelloResponse, (uint8_t *)resp,
                          sizeof(resp));
}

bool EdlManager::SendProgrammer(const std::vector<uint8_t> &data) {
  std::cout << "[EDL] Streaming programmer payload (" << data.size()
            << " bytes)..." << std::endl;
  // Sahara Write logic would go here
  return true;
}

bool EdlManager::FirehoseHandshake() {
  std::string config = FirehoseClient::CreateConfigureXml();
  if (!SendXmlCommand(config))
    return false;

  std::string resp = ReceiveXmlResponse();
  auto parsed = FirehoseClient::ParseResponse(resp);
  return parsed.success;
}

bool EdlManager::SendXmlCommand(const std::string &xml) {
  return _transport->Send((const uint8_t *)xml.c_str(), xml.length(), 2000) > 0;
}

std::string EdlManager::ReceiveXmlResponse() {
  uint8_t buffer[4096];
  memset(buffer, 0, sizeof(buffer));
  int read = _transport->Receive(buffer, sizeof(buffer), 5000);
  return (read > 0) ? std::string((char *)buffer, read) : "";
}

bool EdlManager::ReadPartition(const std::string &name, uint64_t offset,
                               uint64_t count, std::vector<uint8_t> &out) {
  std::string cmd = FirehoseClient::CreateReadXml(name, offset, count);
  if (!SendXmlCommand(cmd))
    return false;

  size_t expectedBytes = count * 512;
  out.resize(expectedBytes);

  int received = _transport->Receive(out.data(), expectedBytes, 10000);
  if (received != (int)expectedBytes)
    return false;

  std::string finalResp = ReceiveXmlResponse();
  return FirehoseClient::ParseResponse(finalResp).success;
}

bool EdlManager::WritePartition(const std::string &name, uint64_t offset,
                                const std::vector<uint8_t> &data) {
  uint64_t count = data.size() / 512;
  std::string cmd = FirehoseClient::CreateWriteXml(name, offset, count);
  if (!SendXmlCommand(cmd))
    return false;

  int sent = _transport->Send(data.data(), data.size(), 10000);
  if (sent != (int)data.size())
    return false;

  std::string finalResp = ReceiveXmlResponse();
  return FirehoseClient::ParseResponse(finalResp).success;
}

bool EdlManager::ErasePartition(const std::string &name) {
  std::cout << "[EDL] Erasing partition: " << name << "..." << std::endl;
  std::string cmd = FirehoseClient::CreateEraseXml(name);
  if (!SendXmlCommand(cmd))
    return false;

  std::string finalResp = ReceiveXmlResponse();
  return FirehoseClient::ParseResponse(finalResp).success;
}

// Internal Helpers
bool EdlManager::SendSaharaPacket(SaharaCommand cmd, const uint8_t *data,
                                  size_t len) {
  SaharaHeader header = {(uint32_t)cmd, (uint32_t)(sizeof(SaharaHeader) + len)};
  std::vector<uint8_t> pkt(sizeof(SaharaHeader) + len);
  memcpy(pkt.data(), &header, sizeof(header));
  if (len > 0)
    memcpy(pkt.data() + sizeof(header), data, len);

  return _transport->Send(pkt.data(), pkt.size(), 1000) > 0;
}

bool EdlManager::ReceiveSaharaPacket(SaharaCommand &cmd,
                                     std::vector<uint8_t> &data) {
  uint8_t buffer[1024];
  int read = _transport->Receive(buffer, sizeof(buffer), 2000);
  if (read < (int)sizeof(SaharaHeader))
    return false;

  SaharaHeader *header = (SaharaHeader *)buffer;
  cmd = (SaharaCommand)header->command;

  size_t dataLen = header->length - sizeof(SaharaHeader);
  if (dataLen > 0 && dataLen < 1024) {
    data.assign(buffer + sizeof(SaharaHeader), buffer + header->length);
  }
  return true;
}

} // namespace Protocols
} // namespace DeepEye
