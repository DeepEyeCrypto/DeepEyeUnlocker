#ifndef DEEPEYE_EXPORTS_H
#define DEEPEYE_EXPORTS_H

#include <stdint.h>

#ifdef _WIN32
#define DEEPEYE_API extern "C" __declspec(dllexport)
#else
#define DEEPEYE_API extern "C" __attribute__((visibility("default")))
#endif

DEEPEYE_API void *DeepEye_CreateTransport();
DEEPEYE_API void DeepEye_DestroyTransport(void *transport);
DEEPEYE_API bool DeepEye_TransportOpen(void *transport, int fd);
DEEPEYE_API void DeepEye_TransportClose(void *transport);

DEEPEYE_API void *DeepEye_CreateEngine(void *transport);
DEEPEYE_API void DeepEye_DestroyEngine(void *engine);
DEEPEYE_API bool DeepEye_EngineIdentify(void *engine);
DEEPEYE_API bool DeepEye_EngineDumpPartition(void *engine, const char *name,
                                             const char *outPath);
DEEPEYE_API bool DeepEye_EngineFlashPartition(void *engine, const char *name,
                                              const char *inPath);
DEEPEYE_API bool DeepEye_EngineErasePartition(void *engine, const char *name);
DEEPEYE_API int DeepEye_EngineGetPartitions(void *engine, char *outBuffer,
                                            int bufferSize);

#endif // DEEPEYE_EXPORTS_H
