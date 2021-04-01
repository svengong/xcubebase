//
// Created by sven on 2021/1/23.
//

#include "NativeEntry.h"
#include <jni.h>


const char *scriptpath;

extern "C"
JNIEXPORT void JNICALL Java_org_xtgo_xcube_base_XcubeBase_gumjsHook(
        JNIEnv *env, jclass clazz, jstring script) {
    const char *scriptpath = env->GetStringUTFChars(script, 0);
    gumjsHook(scriptpath);
    env->ReleaseStringUTFChars(script, scriptpath);
}

extern "C"
JNIEXPORT jboolean JNICALL
        Java_org_xtgo_xcube_base_XcubeBase_checkConfigured(JNIEnv *env, jclass thiz,
jstring jpackagename) {
const char *packagename = env->GetStringUTFChars(jpackagename, 0);
bool ret = checkConfguared(packagename);
env->ReleaseStringUTFChars(jpackagename, packagename);
return ret;
}