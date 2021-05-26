#include <jni.h>
#include <string>
#include <iostream>
#include <stdint.h>
#include <android/log.h>


extern "C"
JNIEXPORT jstring JNICALL
Java_lu_uni_trux_getter_1proxy_1leaker_MainActivity_nativeGetImei(JNIEnv *env, jobject thiz, jobject manager) {
    jclass tmClass = (*env).GetObjectClass(manager);
    jmethodID getMethodId = (*env).GetMethodID(tmClass, "getDeviceId", "()Ljava/lang/String;");
    jstring imei = (jstring)(*env).CallObjectMethod(manager, getMethodId);
    return imei;
}

extern "C"
JNIEXPORT void JNICALL
Java_lu_uni_trux_getter_1proxy_1leaker_MainActivity_nativeLeaker(JNIEnv *env, jobject thiz, jstring s) {
    jclass jniLog = (*env).FindClass("android/util/Log");
    jmethodID logDId = (*env).GetStaticMethodID(jniLog, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
    jstring tag = (*env).NewStringUTF("Test");
    (*env).CallStaticIntMethod(jniLog, logDId, tag, s);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_lu_uni_trux_getter_1proxy_1leaker_MainActivity_nativeProxy(JNIEnv *env, jobject thiz, jstring s) {
    return s;
}