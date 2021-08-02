#include <jni.h>
#include <string>
#include <iostream>
#include <stdint.h>
#include <android/log.h>

jstring f(JNIEnv *env, jobject thiz, jobject manager){
    jclass tmClass = (*env).GetObjectClass(manager);
    jmethodID getMethodId = (*env).GetMethodID(tmClass, "getDeviceId", "()Ljava/lang/String;");
    jstring imei = (jstring)(*env).CallObjectMethod(manager, getMethodId);
    return imei;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_lu_uni_trux_getter_1imei_1deep_MainActivity_nativeGetImei(JNIEnv *env, jobject thiz, jobject manager) {
    return f(env, thiz, manager);
}