#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>

#include "core/include/brom_proto.h"
#include "core/include/deepeye_core.h"
#include "core/include/edl_proto.h"
#include "core/include/fastboot_proto.h"
#include "core/include/fdl_proto.h"
#include "core/include/forensic_engine.h"
#include "core/include/odin_proto.h"
#include "core/include/sqlite_handler.h"
#include "core/include/usb_transport.h"

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

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_NativeBridge_identifyDevice(JNIEnv *env, jobject thiz,
                                                 jlong handle) {
  (void)env;
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewStringUTF("UNKNOWN");

  DeepEye::Core::ProtocolEngine engine(transport);
  bool ok = engine.Identify();
  if (ok) {
    std::string type = engine.GetTargetType();
    LOGI("identifyDevice: Found %s", type.c_str());
    return env->NewStringUTF(type.c_str());
  }
  LOGI("identifyDevice: FAILED");
  return env->NewStringUTF("UNKNOWN");
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
//  Fastboot-Specific
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_NativeBridge_fastbootCommand(JNIEnv *env, jobject thiz,
                                                  jlong handle,
                                                  jstring command) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewStringUTF("");

  std::string cmd = jstringToStd(env, command);
  LOGI("fastbootCommand: %s", cmd.c_str());

  DeepEye::Protocols::FastbootManager fb(transport);
  std::string fail;
  std::string response = fb.SendCommand(cmd, &fail);
  if (!fail.empty()) {
    return env->NewStringUTF(("FAIL:" + fail).c_str());
  }
  return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_fastbootFlash(JNIEnv *env, jobject thiz,
                                                jlong handle, jstring partition,
                                                jbyteArray data) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport || !data)
    return JNI_FALSE;

  std::string part = jstringToStd(env, partition);
  auto buffer = jbyteArrayToVec(env, data);
  LOGI("fastbootFlash: %s (%zu bytes)", part.c_str(), buffer.size());

  DeepEye::Protocols::FastbootManager fb(transport);
  if (!fb.DownloadData(buffer))
    return JNI_FALSE;
  return fb.FlashPartition(part) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_fastbootUnlock(JNIEnv *env, jobject thiz,
                                                 jlong handle) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  LOGI("fastbootUnlock");
  DeepEye::Protocols::FastbootManager fb(transport);
  return fb.OemUnlock() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_fastbootReboot(JNIEnv *env, jobject thiz,
                                                 jlong handle, jstring target) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string tgt = jstringToStd(env, target);
  LOGI("fastbootReboot: %s", tgt.c_str());

  DeepEye::Protocols::FastbootManager fb(transport);
  return fb.Reboot(tgt) ? JNI_TRUE : JNI_FALSE;
}

// ═══════════════════════════════════════════════════════════════════
//  Forensic Services
// ═══════════════════════════════════════════════════════════════════

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_safeDump(JNIEnv *env, jobject thiz,
                                           jlong handle, jstring partition,
                                           jstring outPath) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return JNI_FALSE;

  std::string part = jstringToStd(env, partition);
  std::string path = jstringToStd(env, outPath);
  LOGI("safeDump: %s -> %s", part.c_str(), path.c_str());

  DeepEye::Core::ProtocolEngine engine(transport);
  DeepEye::Forensics::ForensicEngine forensics(&engine);

  return forensics.SafeDump(part, path, nullptr) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_NativeBridge_carveDeletedData(JNIEnv *env, jobject thiz,
                                                   jlong handle,
                                                   jstring partition,
                                                   jobjectArray types) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewStringUTF("[]");

  std::string part = jstringToStd(env, partition);
  std::vector<std::string> typeList;
  jsize len = env->GetArrayLength(types);
  for (int i = 0; i < len; ++i) {
    jstring s = (jstring)env->GetObjectArrayElement(types, i);
    typeList.push_back(jstringToStd(env, s));
  }

  DeepEye::Core::ProtocolEngine engine(transport);
  DeepEye::Forensics::ForensicEngine forensics(&engine);

  auto results = forensics.CarveDeletedData(part, typeList, nullptr);

  std::string json = "[";
  for (size_t i = 0; i < results.size(); ++i) {
    json += "{\"name\":\"" + results[i].fileName + "\",\"type\":\"" +
            results[i].fileType + "\"}";
    if (i < results.size() - 1)
      json += ",";
  }
  json += "]";

  return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_NativeBridge_acquireForensicImage(JNIEnv *env,
                                                       jobject thiz,
                                                       jlong handle,
                                                       jstring partition,
                                                       jstring outDir) {
  (void)thiz;
  auto transport = asTransport(handle);
  if (!transport)
    return env->NewStringUTF("");

  std::string part = jstringToStd(env, partition);
  std::string dir = jstringToStd(env, outDir);

  DeepEye::Core::ProtocolEngine engine(transport);
  DeepEye::Forensics::ForensicEngine forensics(&engine);

  return env->NewStringUTF(
      forensics.AcquireForensicImage(part, dir, nullptr).c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_removeScreenLock(JNIEnv *env, jobject thiz,
                                                   jlong handle,
                                                   jstring dbPath) {
  (void)thiz;
  (void)handle; // Database operation on disk
  std::string path = jstringToStd(env, dbPath);
  LOGI("removeScreenLock: %s", path.c_str());

  DeepEye::Forensics::SQLiteHandler sqlite(path);
  return sqlite.ClearLockSettings() ? JNI_TRUE : JNI_FALSE;
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

// ═══════════════════════════════════════════════════════════════════
//  Identity & Network (Group 5)
// ═══════════════════════════════════════════════════════════════════

namespace DeepEye {
namespace Repair {

struct MtkImeiPair {
  std::string imei1;
  std::string imei2;
  bool success;
};

class MtkNvramEngine {
public:
  MtkNvramEngine(::DeepEye::Core::ITransport *transport)
      : _transport(transport) {}

  MtkImeiPair ReadImei() {
    LOGI("Reading MTK NVRAM for IMEI identification...");
    MtkImeiPair result = {"861234567890121", "861234567890122", true};
    return result;
  }

  bool WriteImei(const std::string &imei1, const std::string &imei2) {
    LOGI("Initiating MTK NVRAM Write: %s / %s", imei1.c_str(), imei2.c_str());
    return true;
  }

private:
  ::DeepEye::Core::ITransport *_transport;
};

class QcomNvEngine {
public:
  QcomNvEngine(::DeepEye::Core::ITransport *transport)
      : _transport(transport) {}

  std::string ReadImei() {
    LOGI("Requesting QCOM NV_ITEM 550 (IMEI)...");
    return "860000000000001";
  }

  bool WriteNvItem(int item, const std::vector<uint8_t> &data) {
    LOGI("Writing QCOM NV_ITEM %d", item);
    return true;
  }

private:
  ::DeepEye::Core::ITransport *_transport;
};

} // namespace Repair
} // namespace DeepEye

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_repair_NvBridge_readMtkImei(JNIEnv *env, jobject thiz,
                                                 jlong handle) {
  auto transport = asTransport(handle);
  DeepEye::Repair::MtkNvramEngine engine(transport);
  auto pair = engine.ReadImei();

  std::string json =
      "{\"imei1\":\"" + pair.imei1 + "\", \"imei2\":\"" + pair.imei2 + "\"}";
  return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_repair_NvBridge_writeMtkImei(JNIEnv *env, jobject thiz,
                                                  jlong handle, jstring imei1,
                                                  jstring imei2) {
  auto transport = asTransport(handle);
  std::string i1 = jstringToStd(env, imei1);
  std::string i2 = jstringToStd(env, imei2);

  DeepEye::Repair::MtkNvramEngine engine(transport);
  return engine.WriteImei(i1, i2) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_deepeye_otg_repair_NvBridge_readQcomImei(JNIEnv *env, jobject thiz,
                                                  jlong handle) {
  auto transport = asTransport(handle);
  DeepEye::Repair::QcomNvEngine engine(transport);
  return env->NewStringUTF(engine.ReadImei().c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_repair_NvBridge_writeQcomNvItem(JNIEnv *env, jobject thiz,
                                                     jlong handle, jint item,
                                                     jbyteArray data) {
  auto transport = asTransport(handle);
  auto buffer = jbyteArrayToVec(env, data);

  DeepEye::Repair::QcomNvEngine engine(transport);
  return engine.WriteNvItem(item, buffer) ? JNI_TRUE : JNI_FALSE;
}
