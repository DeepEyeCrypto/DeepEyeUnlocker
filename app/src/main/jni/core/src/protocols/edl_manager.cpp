#include "../../include/edl_proto.h"
#include "../../include/firehose.h"
#include <cstring>
#include <fstream>
#include <iostream>

namespace DeepEye {
namespace Protocols {

namespace {

bool IsAckResponse(const std::string &resp) {
    return resp.find("ACK") != std::string::npos ||
           resp.find("ack") != std::string::npos;
}

}

EdlManager::EdlManager(Core::ITransport *transport) : _transport(transport) {}

bool EdlManager::ConnectSahara() {
    if (!_transport || !_transport->IsOpen()) {
        std::cerr << "[EDL] ConnectSahara: transport not ready" << std::endl;
        return false;
    }

    SaharaCommand cmd{};
    std::vector<uint8_t> payload;
    if (!ReceiveSaharaPacket(cmd, payload)) {
        std::cerr << "[EDL] ConnectSahara: no Sahara HELLO received" << std::endl;
        return false;
    }

    if (cmd != SaharaCommand::Hello) {
        std::cerr << "[EDL] ConnectSahara: unexpected Sahara command "
                  << static_cast<uint32_t>(cmd) << std::endl;
        return false;
    }

    std::cout << "[EDL] ConnectSahara: HELLO received (" << payload.size()
              << " bytes payload)" << std::endl;
    return true;
}

bool EdlManager::SendProgrammer(const std::vector<uint8_t> &data) {
    if (!_transport || !_transport->IsOpen()) {
        return false;
    }
    if (data.empty()) {
        std::cerr << "[EDL] SendProgrammer: empty programmer image" << std::endl;
        return false;
    }

    const bool ok = _transport->Write(data);
    if (!ok) {
        std::cerr << "[EDL] SendProgrammer: write failed" << std::endl;
    }
    return ok;
}

bool EdlManager::FirehoseHandshake() {
    if (!_transport || !_transport->IsOpen()) {
        std::cerr << "[EDL] FirehoseHandshake: transport not ready" << std::endl;
        return false;
    }

    const auto cfg = FirehoseClient::CreateConfigureXml();
    if (!SendXmlCommand(cfg)) {
        return false;
    }
    const auto resp = ReceiveXmlResponse();
    return IsAckResponse(resp);
}

bool EdlManager::SendXmlCommand(const std::string &xml) {
    if (!_transport || !_transport->IsOpen()) {
        return false;
    }
    if (xml.empty()) {
        return false;
    }

    const auto *raw = reinterpret_cast<const uint8_t *>(xml.data());
    const bool ok = _transport->WriteBulk(raw, xml.size(), 5000);
    if (!ok) {
        std::cerr << "[EDL] SendXmlCommand: bulk write failed" << std::endl;
    }
    return ok;
}

std::string EdlManager::ReceiveXmlResponse() {
    if (!_transport || !_transport->IsOpen()) {
        return {};
    }

    // Firehose typically returns short xml ack/nak packets.
    std::vector<uint8_t> buf(4096, 0);
    const int n = _transport->ReadBulk(buf.data(), buf.size(), 5000);
    if (n <= 0) {
        return {};
    }
    return std::string(reinterpret_cast<const char *>(buf.data()), static_cast<size_t>(n));
}

bool EdlManager::SaharaHandshake(const std::string &programmerPath) {
    if (!ConnectSahara()) {
        return false;
    }

    if (programmerPath.empty()) {
        std::cerr << "[EDL] SaharaHandshake: programmer path empty" << std::endl;
        return false;
    }

    std::ifstream file(programmerPath, std::ios::binary | std::ios::ate);
    if (!file.is_open()) {
        std::cerr << "[EDL] SaharaHandshake: cannot open programmer file "
                  << programmerPath << std::endl;
        return false;
    }

    const auto endPos = file.tellg();
    if (endPos <= 0) {
        std::cerr << "[EDL] SaharaHandshake: programmer file empty" << std::endl;
        return false;
    }

    std::vector<uint8_t> data(static_cast<size_t>(endPos));
    file.seekg(0, std::ios::beg);
    if (!file.read(reinterpret_cast<char *>(data.data()), static_cast<std::streamsize>(data.size()))) {
        std::cerr << "[EDL] SaharaHandshake: failed to read programmer data" << std::endl;
        return false;
    }

    return SendProgrammer(data);
}

std::string EdlManager::FirehoseXml(const std::string &xmlCommand) {
    if (!SendXmlCommand(xmlCommand)) {
        return {};
    }
    return ReceiveXmlResponse();
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
    out.clear();
    if (!_transport || !_transport->IsOpen()) {
        return false;
    }

    const auto xml = FirehoseClient::CreateReadXml(name, offset, count);
    if (!SendXmlCommand(xml)) {
        return false;
    }

    const auto resp = ReceiveXmlResponse();
    if (!IsAckResponse(resp)) {
        return false;
    }

    const size_t bytesToRead = static_cast<size_t>(count * 512ULL);
    if (bytesToRead == 0) {
        return true;
    }

    out.resize(bytesToRead);
    const int n = _transport->ReadBulk(out.data(), out.size(), 10000);
    if (n <= 0) {
        out.clear();
        return false;
    }
    out.resize(static_cast<size_t>(n));
    return true;
}

bool EdlManager::WritePartition(const std::string &name, uint64_t offset,
                                 const std::vector<uint8_t> &data) {
    if (!_transport || !_transport->IsOpen()) {
        return false;
    }
    if (data.empty()) {
        return false;
    }

    const uint64_t sectors = (data.size() + 511ULL) / 512ULL;
    const auto xml = FirehoseClient::CreateWriteXml(name, offset, sectors);
    if (!SendXmlCommand(xml)) {
        return false;
    }

    auto resp = ReceiveXmlResponse();
    if (!IsAckResponse(resp)) {
        return false;
    }

    if (!_transport->WriteBulk(data.data(), data.size(), 15000)) {
        return false;
    }

    resp = ReceiveXmlResponse();
    return IsAckResponse(resp);
}

bool EdlManager::ErasePartition(const std::string &name) {
    if (!_transport || !_transport->IsOpen()) {
        return false;
    }

    const auto xml = FirehoseClient::CreateEraseXml(name);
    if (!SendXmlCommand(xml)) {
        return false;
    }
    const auto resp = ReceiveXmlResponse();
    return IsAckResponse(resp);
}

bool EdlManager::SendSaharaPacket(SaharaCommand cmd, const uint8_t *data,
                                   size_t len) {
    if (!_transport || !_transport->IsOpen()) {
        return false;
    }

    SaharaHeader header{};
    header.command = static_cast<uint32_t>(cmd);
    header.length = static_cast<uint32_t>(sizeof(SaharaHeader) + len);

    if (_transport->Send(reinterpret_cast<const uint8_t *>(&header), sizeof(header), 3000)
        != static_cast<int>(sizeof(header))) {
        return false;
    }

    if (len > 0) {
        return _transport->Send(data, len, 5000) == static_cast<int>(len);
    }
    return true;
}

bool EdlManager::ReceiveSaharaPacket(SaharaCommand &cmd,
                                      std::vector<uint8_t> &data) {
    if (!_transport || !_transport->IsOpen()) {
        return false;
    }

    SaharaHeader header{};
    const int h = _transport->Receive(reinterpret_cast<uint8_t *>(&header), sizeof(header), 5000);
    if (h != static_cast<int>(sizeof(header))) {
        return false;
    }

    cmd = static_cast<SaharaCommand>(header.command);
    const uint32_t totalLen = header.length;
    if (totalLen < sizeof(SaharaHeader)) {
        return false;
    }

    const size_t payloadLen = totalLen - sizeof(SaharaHeader);
    data.resize(payloadLen);
    if (payloadLen == 0) {
        return true;
    }

    const int r = _transport->Receive(data.data(), payloadLen, 5000);
    if (r != static_cast<int>(payloadLen)) {
        data.clear();
        return false;
    }
    return true;
}

} // namespace Protocols
} // namespace DeepEye
