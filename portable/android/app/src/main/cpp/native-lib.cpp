#include "deepeye_core.h"
#include "usb_transport.h"
#include "brom_proto.h"
#include <jni.h>
#include <string>
#include <vector>

extern "C" JNIEXPORT jlong JNICALL Java_com_deepeye_otg_NativeBridge_initCore(
    JNIEnv *env, jobject thiz, jint fd, jint vid, jint pid) {
  (void)env;
  (void)thiz;
  (void)vid;
  (void)pid;
  
  auto transport = new DeepEye::Core::LibUsbTransport();
  if (transport->Open(fd)) {
    return reinterpret_cast<jlong>(transport);
  }
  delete transport;
  return 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_deepeye_otg_NativeBridge_identifyDevice(JNIEnv *env, jobject thiz,
                                                 jlong transportPtr) {
  (void)env;
  (void)thiz;
  
  auto transport = reinterpret_cast<DeepEye::Core::ITransport *>(transportPtr);
  if (!transport)
    return false;

  DeepEye::Core::ProtocolEngine engine(transport);
  return engine.Identify();
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_deepeye_otg_NativeBridge_getPartitions(JNIEnv *env, jobject thiz,
                                                jlong handle) {
  (void)thiz;
  
  auto transport = reinterpret_cast<DeepEye::Core::ITransport *>(handle);
  if (!transport)
    return nullptr;

  DeepEye::Core::ProtocolEngine engine(transport);
  auto partitions = engine.GetPartitions();

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
Java_com_deepeye_otg_NativeBridge_injectDa(JNIEnv *env, jobject thiz,
                                           jlong handle, jbyteArray da_data) {
  (void)thiz;
  
  auto transport = reinterpret_cast<DeepEye::Core::ITransport *>(handle);
  if (!transport)
    return JNI_FALSE;

  jsize len = env->GetArrayLength(da_data);
  jbyte *body = env->GetByteArrayElements(da_data, 0);

  std::vector<uint8_t> buffer(reinterpret_cast<uint8_t *>(body),
                              reinterpret_cast<uint8_t *>(body) + len);

  DeepEye::Protocols::BromManager brom(transport);
  bool success = brom.SendDA(buffer);

  env->ReleaseByteArrayElements(da_data, body, JNI_ABORT);
  return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_deepeye_otg_NativeBridge_closeCore(
    JNIEnv *env, jobject thiz, jlong transportPtr) {
  (void)env;
  (void)thiz;
  
  auto transport =
      reinterpret_cast<DeepEye::Core::LibUsbTransport *>(transportPtr);
  if (transport) {
    transport->Close();
    delete transport;
  }
}
