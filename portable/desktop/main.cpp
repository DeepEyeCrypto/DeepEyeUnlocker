#include "../core/include/deepeye_core.h"
#include <iostream>
#include <string>
#include <vector>

// Mock Transport for Desktop CLI (would be replaced by LibUsbTransport in
// production)
class DesktopTransport : public DeepEye::Core::ITransport {
public:
  bool Open(int fd) override { return true; }
  void Close() override {}
  int Send(const uint8_t *data, size_t length, uint32_t timeout_ms) override {
    return (int)length;
  }
  int Receive(uint8_t *data, size_t length, uint32_t timeout_ms) override {
    return (int)length;
  }
};

int main(int argc, char *argv[]) {
  std::cout << "========================================" << std::endl;
  std::cout << "   DeepEye Unlocker - Portable Desktop v1.4.0" << std::endl;
  std::cout << "   Technician Command Line Interface" << std::endl;
  std::cout << "========================================" << std::endl;

  if (argc < 2) {
    std::cout << "Usage: deepeye_cli [identify|partitions|dump|flash]"
              << std::endl;
    std::cout << "  identify   - Detect connected device chipset" << std::endl;
    std::cout << "  partitions - List partition table via OTG stream"
              << std::endl;
    std::cout << "  dump [p]   - Read partition 'p' to local file" << std::endl;
    std::cout << "  flash [p] [f] - Write file 'f' to partition 'p'"
              << std::endl;
    return 1;
  }

  std::string cmd = argv[1];
  DesktopTransport transport;
  DeepEye::Core::ProtocolEngine engine(&transport);

  if (cmd == "identify") {
    if (engine.Identify()) {
      std::cout << "[SUCCESS] Device identified." << std::endl;
    } else {
      std::cout << "[ERROR] No compatible device found in BROM/EDL mode."
                << std::endl;
    }
  } else if (cmd == "partitions") {
    if (engine.Identify()) {
      auto parts = engine.GetPartitions();
      std::cout << "Found " << parts.size() << " partitions:" << std::endl;
      for (const auto &p : parts) {
        std::cout << " - " << p.name << " (" << p.sizeInBytes / 1024 << " KB)"
                  << std::endl;
      }
    }
  } else if (cmd == "dump") {
    if (argc < 3) {
      std::cout << "Specify partition name." << std::endl;
      return 1;
    }
    std::string partName = argv[2];
    if (engine.DumpPartition(partName, partName + ".bin")) {
      std::cout << "[SUCCESS] Dumped " << partName << " to " << partName
                << ".bin" << std::endl;
    }
  } else if (cmd == "flash") {
    if (argc < 4) {
      std::cout << "Specify partition and input file." << std::endl;
      return 1;
    }
    std::string partName = argv[2];
    if (engine.FlashPartition(partName, argv[3])) {
      std::cout << "[SUCCESS] Flashed " << partName << " successfully."
                << std::endl;
    }
  } else {
    std::cout << "Unknown command: " << cmd << std::endl;
  }

  return 0;
}
