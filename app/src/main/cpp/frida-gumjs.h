//
// Created by sven on 2021/1/23.
//

#ifndef XPOSEDDEMO_FRIDA_GUMJS_H
#define XPOSEDDEMO_FRIDA_GUMJS_H
#if defined(__arm__)
#include "includes/armeabi-v7a/frida-gumjs.h"
#elif defined(__aarch64__)

#include "includes/arm64-v8a/frida-gumjs.h"

#endif
#define LOGTAG "Xcube_gumjshook"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOGTAG , __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , LOGTAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , LOGTAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , LOGTAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR , LOGTAG, __VA_ARGS__)


//https://stackoverflow.com/questions/19355783/getting-os-version-with-ndk-in-c
static void on_message(GumScript *script, const gchar *message, GBytes *data, gpointer user_data);

int gumjsHook(const char *scriptpath);

char *readfile(const char *filepath);

int hookFunc(const char *scriptpath);

bool checkConfguared(const char *packagename);

#endif //XPOSEDDEMO_FRIDA_GUMJS_H
