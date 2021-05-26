#include <jni.h>
#include <string>
#include <iostream>
#include <stdint.h>
#include <android/log.h>

extern "C"
JNIEXPORT void JNICALL
Java_lu_uni_trux_delegation_1imei_MainActivity_nativeDelegation(JNIEnv *env, jobject thiz) {
    jclass context = (*env).FindClass( "android/content/Context");
    jmethodID getSystemServiceMID = (*env).GetMethodID(context, "getSystemService", "(Ljava/lang/String;)Ljava/lang/Object;");
    jfieldID fid = (*env).GetStaticFieldID(context, "TELEPHONY_SERVICE", "Ljava/lang/String;");
    jstring contextTelephonyService = (jstring)(*env).GetStaticObjectField(context, fid);
    jobject telephonyManagerObject = (*env).CallObjectMethod(thiz, getSystemServiceMID, contextTelephonyService);

    jclass telephonyManagerClass = (*env).FindClass("android/telephony/TelephonyManager");
    jmethodID getImeiMid = (*env).GetMethodID(telephonyManagerClass, "getDeviceId", "()Ljava/lang/String;");
    jstring imei =  (jstring)(*env).CallObjectMethod(telephonyManagerObject, getImeiMid);

    jclass jniLog = (*env).FindClass("android/util/Log");
    jmethodID logDId = (*env).GetStaticMethodID(jniLog, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
    jstring tag = (*env).NewStringUTF("Test");
    (*env).CallStaticIntMethod(jniLog, logDId, tag, imei);
}
