#include <jni.h>
#include <android/log.h>
#define LOG_TAG "DeepEyeCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_deepeye_otg_NativeBridge_getVersion(
            JNIEnv* env, jobject /* this */) {
        return env->NewStringUTF("DeepEye Core v1.0 - Android");
    }
}
