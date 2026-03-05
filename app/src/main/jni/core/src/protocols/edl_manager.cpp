#include "../../include/edl_proto.h"
#include <cstring>
#include <iostream>

namespace DeepEye {
namespace Protocols {

EdlManager::EdlManager(Core::ITransport *transport) : _transport(transport) {}

bool EdlManager::ConnectSahara() {
    if (!_transport) return false;
    // TODO: implement real Sahara Hello/HelloResponse exchange
    std::cout << "[EDL] ConnectSahara stub" << std::endl;
    return false;
}

bool EdlManager::SendProgrammer(const std::vector<uint8_t> &data) {
    (void)data;
    std::cout << "[EDL] SendProgrammer stub" << std::endl;
    return false;
}

bool EdlManager::FirehoseHandshake() {
    std::cout << "[EDL] FirehoseHandshake stub" << std::endl;
    return false;
}

bool EdlManager::SendXmlCommand(const std::string &xml) {
    (void)xml;
    return false;
}

std::string EdlManager::ReceiveXmlResponse() {
    return "";
}

bool EdlManager::SaharaHandshake(const std::string &programmerPath) {
    (void)programmerPath;
    std::cout << "[EDL] SaharaHandshake stub" << std::endl;
    return false;
}

std::string EdlManager::FirehoseXml(const std::string &xmlCommand) {
    (void)xmlCommand;
    return "";
}

std::vector<uint8_t> EdlManager::ReadNvItem(int nvItem) {
    (void)nvItem;
    return {};
}

bool EdlManager::WriteNvItem(int nvItem, const std::vector<uint8_t> &data) {
    (void)nvItem;
    (void)data;
    return false;
}

std::vector<uint8_t> EdlManager::DiagCommand(const std::vector<uint8_t> &cmd) {
    (void)cmd;
    return {};
}

bool EdlManager::ReadPartition(const std::string &name, uint64_t offset,
                                uint64_t count, std::vector<uint8_t> &out) {
    (void)name; (void)offset; (void)count; (void)out;
    return false;
}

bool EdlManager::WritePartition(const std::string &name, uint64_t offset,
                                 const std::vector<uint8_t> &data) {
    (void)name; (void)offset; (void)data;
    return false;
}

bool EdlManager::ErasePartition(const std::string &name) {
    (void)name;
    return false;
}

bool EdlManager::SendSaharaPacket(SaharaCommand cmd, const uint8_t *data,
                                   size_t len) {
    (void)cmd; (void)data; (void)len;
    return false;
}

bool EdlManager::ReceiveSaharaPacket(SaharaCommand &cmd,
                                      std::vector<uint8_t> &data) {
    (void)cmd; (void)data;
    return false;
}

} // namespace Protocols
} // namespace DeepEye
