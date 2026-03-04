#include "brom_proto.h"
#include "deepeye_core.h"
#include "edl_proto.h"
#include "fdl_proto.h"
#include "odin_proto.h"
#include "usb_transport.h"
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>

#define DTAG "DeepEye-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, DTAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, DTAG, __VA_ARGS__)

// ═══════════════════════════════════════════════════════════════════
//  Helpers
// ═══════════════════════════════════════════════════════════════════

static DeepEye::Core::ITransport *asTransport(jlong handle) {
  return reinterpret_cast<DeepEye::Core::ITransport *>(handle);
}

static std::string jstringToStd(JNIEnv *env, jstring jstr) {
  if (!jstr)
    return "";
  const char *utf = env->GetStringUTFChars(jstr, nullptr);
  std::string result(utf);
  env->ReleaseStringUTFChars(jstr, utf);
  return result;
}

static jbyteArray vecToJbyteArray(JNIEnv *env,
                                  const std::vector<uint8_t> &data) {
  jbyteArray arr = env->NewByteArray(data.size());
  if (arr && !data.empty()) {
    env->SetByteArrayRegion(arr, 0, data.size(),
                            reinterpret_cast<const jbyte *>(data.data()));
  }
  return arr;
}

static std::vector<uint8_t> jbyteArrayToVec(JNIEnv *env, jbyteArray arr) {
  if (!arr)
    return {};
  jsize len = env->GetArrayLength(arr);
  jbyte *body = env->GetByteArrayElements(arr, nullptr);
  std::vector<uint8_t> result(reinterpret_cast<uint8_t *>(body),
                              reinterpret_cast<uint8_t *>(body) + len);
  env->ReleaseByteArrayElements(arr, body, JNI_ABORT);
  return result;
}

// ═══════════════════════════════════════════════════════════════════
//  Lifecycle
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jlong JNICALL Java_com_deepeye_otg_NativeBridge_initCore(
    JNIEnv *env, jobject thiz, jint fd, jint vid, jint pid) {
  (void)env;
  (void)thiz;
  LOGI("initCore: fd=%d vid=0x%04X pid=0x%04X", fd, vid, pid);

  auto transport = new DeepEye::Core::LibUsbTransport();
  if (transport->Open(fd)) {
    LOGI("initCore: transport opened OK");
    return reinterpret_cast<jlong>(transport);
  }
  LOGE("initCore: transport->Open() failed");
  delete transport;
  return 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_identifyDevice(JNIEnv *env, jobject thiz,
                                                 jlong handle) {
  (void)env;
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  DeepEye::Core::ProtocolEngine engine(transport);
  bool ok = engine.Identify();
  LOGI("identifyDevice: %s", ok ? "OK" : "FAILED");
  return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_deepeye_otg_NativeBridge_closeCore(
    JNIEnv *env, jobject thiz, jlong handle) {
  (void)env;
  (void)thiz;
  auto transport = reinterpret_cast<DeepEye::Core::LibUsbTransport *>(handle);
  if (transport) {
    LOGI("closeCore: closing transport");
    transport->Close();
    delete transport;
  }
}

// ═══════════════════════════════════════════════════════════════════
//  Partition Operations
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_deepeye_otg_NativeBridge_getPartitions(JNIEnv *env, jobject thiz,
                                                jlong handle) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return nullptr;

  DeepEye::Core::ProtocolEngine engine(transport);
  auto partitions = engine.GetPartitions();
  LOGI("getPartitions: found %zu", partitions.size());

  jclass stringClass = env->FindClass("java/lang/String");
  jobjectArray result =
      env->NewObjectArray(partitions.size(), stringClass, nullptr);
  for (size_t i = 0; i < partitions.size(); ++i) {
    std::string info = partitions[i].name + " (" +
                       std::to_string(partitions[i].sizeInBytes / 1024 / 1024) +
                       " MB)";
    env->SetObjectArrayElement(result, i, env->NewStringUTF(info.c_str()));
  }
  return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_readPartition(JNIEnv *env, jobject thiz,
                                                jlong handle, jstring name,
                                                jstring outPath) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string partName = jstringToStd(env, name);
  std::string path = jstringToStd(env, outPath);
  LOGI("readPartition: %s -> %s", partName.c_str(), path.c_str());

  DeepEye::Core::ProtocolEngine engine(transport);
  return engine.DumpPartition(partName, path) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_writePartition(JNIEnv *env, jobject thiz,
                                                 jlong handle, jstring name,
                                                 jstring inPath) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string partName = jstringToStd(env, name);
  std::string path = jstringToStd(env, inPath);
  LOGI("writePartition: %s <- %s", partName.c_str(), path.c_str());

  DeepEye::Core::ProtocolEngine engine(transport);
  return engine.FlashPartition(partName, path) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_erasePartition(JNIEnv *env, jobject thiz,
                                                 jlong handle, jstring name) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string partName = jstringToStd(env, name);
  LOGI("erasePartition: %s", partName.c_str());

  DeepEye::Core::ProtocolEngine engine(transport);
  return engine.ErasePartition(partName) ? JNI_TRUE : JNI_FALSE;
}

// ═══════════════════════════════════════════════════════════════════
//  MTK-Specific
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_injectDa(JNIEnv *env, jobject thiz,
                                           jlong handle, jbyteArray daData) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  auto buffer = jbyteArrayToVec(env, daData);
  LOGI("injectDa: %zu bytes", buffer.size());

  DeepEye::Protocols::BromManager brom(transport);
  return brom.SendDA(buffer) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_deepeye_otg_NativeBridge_readNvram(JNIEnv *env, jobject thiz,
                                            jlong handle, jint item) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewByteArray(0);

  LOGI("readNvram: item=%d", item);
  DeepEye::Protocols::BromManager brom(transport);
  auto data = brom.ReadNvramItem(item);
  return vecToJbyteArray(env, data);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_writeNvram(JNIEnv *env, jobject thiz,
                                             jlong handle, jint item,
                                             jbyteArray data) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  auto buffer = jbyteArrayToVec(env, data);
  LOGI("writeNvram: item=%d, %zu bytes", item, buffer.size());

  DeepEye::Protocols::BromManager brom(transport);
  return brom.WriteNvramItem(item, buffer) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_enterMetaMode(JNIEnv *env, jobject thiz,
                                                jlong handle) {
  (void)env;
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  LOGI("enterMetaMode");
  DeepEye::Protocols::BromManager brom(transport);
  return brom.EnterMetaMode() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_deepeye_otg_NativeBridge_readSeccfg(JNIEnv *env, jobject thiz,
                                             jlong handle) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewByteArray(0);

  LOGI("readSeccfg");
  DeepEye::Protocols::BromManager brom(transport);
  auto data = brom.ReadSeccfg();
  return vecToJbyteArray(env, data);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_writeSeccfg(JNIEnv *env, jobject thiz,
                                              jlong handle, jbyteArray data) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  auto buffer = jbyteArrayToVec(env, data);
  LOGI("writeSeccfg: %zu bytes", buffer.size());

  DeepEye::Protocols::BromManager brom(transport);
  return brom.WriteSeccfg(buffer) ? JNI_TRUE : JNI_FALSE;
}

// ═══════════════════════════════════════════════════════════════════
//  Qualcomm-Specific
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_saharaHandshake(JNIEnv *env, jobject thiz,
                                                  jlong handle,
                                                  jstring programmerPath) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string path = jstringToStd(env, programmerPath);
  LOGI("saharaHandshake: programmer=%s", path.c_str());

  DeepEye::Protocols::EdlManager edl(transport);
  return edl.SaharaHandshake(path) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_NativeBridge_firehoseCommand(JNIEnv *env, jobject thiz,
                                                  jlong handle,
                                                  jstring xmlCommand) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewStringUTF("");

  std::string cmd = jstringToStd(env, xmlCommand);
  LOGI("firehoseCommand: %s", cmd.substr(0, 80).c_str());

  DeepEye::Protocols::EdlManager edl(transport);
  std::string response = edl.FirehoseXml(cmd);
  return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_deepeye_otg_NativeBridge_readQcNv(JNIEnv *env, jobject thiz,
                                           jlong handle, jint nvItem) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewByteArray(0);

  LOGI("readQcNv: item=%d", nvItem);
  DeepEye::Protocols::EdlManager edl(transport);
  auto data = edl.ReadNvItem(nvItem);
  return vecToJbyteArray(env, data);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_writeQcNv(JNIEnv *env, jobject thiz,
                                            jlong handle, jint nvItem,
                                            jbyteArray data) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  auto buffer = jbyteArrayToVec(env, data);
  LOGI("writeQcNv: item=%d, %zu bytes", nvItem, buffer.size());

  DeepEye::Protocols::EdlManager edl(transport);
  return edl.WriteNvItem(nvItem, buffer) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_deepeye_otg_NativeBridge_diagCommand(JNIEnv *env, jobject thiz,
                                              jlong handle, jbyteArray cmd) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewByteArray(0);

  auto cmdBuf = jbyteArrayToVec(env, cmd);
  LOGI("diagCommand: %zu bytes", cmdBuf.size());

  DeepEye::Protocols::EdlManager edl(transport);
  auto response = edl.DiagCommand(cmdBuf);
  return vecToJbyteArray(env, response);
}

// ═══════════════════════════════════════════════════════════════════
//  Samsung-Specific
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_odinHandshake(JNIEnv *env, jobject thiz,
                                                jlong handle) {
  (void)env;
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  LOGI("odinHandshake");
  DeepEye::Protocols::OdinManager odin(transport);
  return odin.Handshake() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_deepeye_otg_NativeBridge_readPit(JNIEnv *env, jobject thiz,
                                          jlong handle) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewByteArray(0);

  LOGI("readPit");
  DeepEye::Protocols::OdinManager odin(transport);
  auto pitData = odin.ReadPit();
  return vecToJbyteArray(env, pitData);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_odinFlash(JNIEnv *env, jobject thiz,
                                            jlong handle, jstring partName,
                                            jstring imagePath) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string name = jstringToStd(env, partName);
  std::string path = jstringToStd(env, imagePath);
  LOGI("odinFlash: %s <- %s", name.c_str(), path.c_str());

  DeepEye::Protocols::OdinManager odin(transport);
  if (!odin.Handshake())
    return JNI_FALSE;
  return odin.FlashPartition(name, path) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_deepeye_otg_NativeBridge_readEfs(JNIEnv *env, jobject thiz,
                                          jlong handle) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewByteArray(0);

  LOGI("readEfs");
  DeepEye::Protocols::OdinManager odin(transport);
  if (!odin.Handshake())
    return env->NewByteArray(0);
  auto data = odin.ReadEfs();
  return vecToJbyteArray(env, data);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_writeEfs(JNIEnv *env, jobject thiz,
                                           jlong handle, jbyteArray data) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  auto buffer = jbyteArrayToVec(env, data);
  LOGI("writeEfs: %zu bytes", buffer.size());

  DeepEye::Protocols::OdinManager odin(transport);
  if (!odin.Handshake())
    return JNI_FALSE;
  return odin.WriteEfs(buffer) ? JNI_TRUE : JNI_FALSE;
}

// ═══════════════════════════════════════════════════════════════════
//  UniSoc-Specific
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_fdlHandshake(JNIEnv *env, jobject thiz,
                                               jlong handle) {
  (void)env;
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  LOGI("fdlHandshake");
  DeepEye::Protocols::FdlManager fdl(transport);
  return fdl.Handshake() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_fdlFlash(JNIEnv *env, jobject thiz,
                                           jlong handle, jstring pacPath) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string path = jstringToStd(env, pacPath);
  LOGI("fdlFlash: %s", path.c_str());

  DeepEye::Protocols::FdlManager fdl(transport);
  if (!fdl.Handshake())
    return JNI_FALSE;
  return fdl.FlashPac(path) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_deepeye_otg_NativeBridge_readUnisocNv(JNIEnv *env, jobject thiz,
                                               jlong handle, jint nvId) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewByteArray(0);

  LOGI("readUnisocNv: id=%d", nvId);
  DeepEye::Protocols::FdlManager fdl(transport);
  auto data = fdl.ReadNv(nvId);
  return vecToJbyteArray(env, data);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_writeUnisocNv(JNIEnv *env, jobject thiz,
                                                jlong handle, jint nvId,
                                                jbyteArray data) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  auto buffer = jbyteArrayToVec(env, data);
  LOGI("writeUnisocNv: id=%d, %zu bytes", nvId, buffer.size());

  DeepEye::Protocols::FdlManager fdl(transport);
  return fdl.WriteNv(nvId, buffer) ? JNI_TRUE : JNI_FALSE;
}

// ═══════════════════════════════════════════════════════════════════
//  Device Info
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_NativeBridge_getDeviceInfo(JNIEnv *env, jobject thiz,
                                                jlong handle) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewStringUTF("{}");

  LOGI("getDeviceInfo");
  // Build JSON from engine info
  DeepEye::Core::ProtocolEngine engine(transport);
  bool identified = engine.Identify();

  std::string json = "{";
  json += "\"identified\":" + std::string(identified ? "true" : "false");
  json += ",\"transport\":\"active\"";

  // Read partitions for additional context
  if (identified) {
    auto parts = engine.GetPartitions();
    json += ",\"partition_count\":" + std::to_string(parts.size());
  }

  json += "}";
  return env->NewStringUTF(json.c_str());
}
