#include "../include/deepeye_exports.h"
#include "../include/deepeye_core.h"
// For simplicity in this build, we assume LibUsbTransport is the primary
// implementation
#include "transport/usb_transport.cpp"

using namespace DeepEye::Core;

DEEPEYE_API void *DeepEye_CreateTransport() { return new LibUsbTransport(); }

DEEPEYE_API void DeepEye_DestroyTransport(void *transport) {
  delete static_cast<ITransport *>(transport);
}

DEEPEYE_API bool DeepEye_TransportOpen(void *transport, int fd) {
  return static_cast<ITransport *>(transport)->Open(fd);
}

DEEPEYE_API void DeepEye_TransportClose(void *transport) {
  static_cast<ITransport *>(transport)->Close();
}

DEEPEYE_API void *DeepEye_CreateEngine(void *transport) {
  return new ProtocolEngine(static_cast<ITransport *>(transport));
}

DEEPEYE_API void DeepEye_DestroyEngine(void *engine) {
  delete static_cast<ProtocolEngine *>(engine);
}

DEEPEYE_API bool DeepEye_EngineIdentify(void *engine) {
  return static_cast<ProtocolEngine *>(engine)->Identify();
}

DEEPEYE_API bool DeepEye_EngineDumpPartition(void *engine, const char *name,
                                             const char *outPath) {
  return static_cast<ProtocolEngine *>(engine)->DumpPartition(name, outPath);
}

DEEPEYE_API bool DeepEye_EngineFlashPartition(void *engine, const char *name,
                                              const char *inPath) {
  return static_cast<ProtocolEngine *>(engine)->FlashPartition(name, inPath);
}

DEEPEYE_API int DeepEye_EngineGetPartitions(void *engine, char *outBuffer,
                                            int bufferSize) {
  auto partitions = static_cast<ProtocolEngine *>(engine)->GetPartitions();
  std::string result;
  for (const auto &p : partitions) {
    result += p.name + "|" + std::to_string(p.sizeInBytes) + "\n";
  }

  if (result.length() < (size_t)bufferSize) {
    strncpy(outBuffer, result.c_str(), bufferSize);
    return (int)result.length();
  }
  return -1;
}
