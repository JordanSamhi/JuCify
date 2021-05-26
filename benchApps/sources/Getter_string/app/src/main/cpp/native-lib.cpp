#include <jni.h>
#include <string>
#include <iostream>
#include <stdint.h>
#include <android/log.h>


extern "C"
JNIEXPORT jstring JNICALL
Java_lu_uni_trux_getter_1imei_MainActivity_nativeGetImei(JNIEnv *env, jobject thiz, jobject manager) {
    jclass tmClass = (*env).GetObjectClass(manager);
    jmethodID getMethodId = (*env).GetMethodID(tmClass, "getDeviceId", "()Ljava/lang/String;");
    jstring imei = (jstring)(*env).CallObjectMethod(manager, getMethodId);
    char *buf = (char*)malloc(7);
    strcpy(buf, "string") 
    jstring jstrBuf = (*env).NewStringUTF(buf);
    return jstrBuf;
}
