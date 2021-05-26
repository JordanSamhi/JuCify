#include <jni.h>
#include <string>
#include <iostream>
#include <stdint.h>
#include <android/log.h>

jstring proxy(jstring s){
    return s;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_lu_uni_trux_proxy_1double_MainActivity_nativeProxy(JNIEnv *env, jobject thiz, jstring s) {
    return proxy(s);
}