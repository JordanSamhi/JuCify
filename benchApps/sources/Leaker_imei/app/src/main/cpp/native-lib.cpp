#include <jni.h>
#include <string>
#include <iostream>
#include <stdint.h>
#include <android/log.h>

extern "C"
JNIEXPORT void JNICALL
Java_lu_uni_trux_leaker_1imei_MainActivity_nativeLeaker(JNIEnv *env, jobject thiz, jstring s) {
    jclass jniLog = (*env).FindClass("android/util/Log");
    jmethodID logDId = (*env).GetStaticMethodID(jniLog, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
    jstring tag = (*env).NewStringUTF("Test");
    (*env).CallStaticIntMethod(jniLog, logDId, tag, s);
}