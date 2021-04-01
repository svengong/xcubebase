//
// Created by sven on 2021/1/14.
//

#include "HelloFrida.h"
#include <fcntl.h>
#include <string.h>
#include <unistd.h>
#include <jni.h>
#include <android/log.h>

#define LOGTAG "Xcube_gumjshook"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOGTAG , __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , LOGTAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , LOGTAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , LOGTAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR , LOGTAG, __VA_ARGS__)


extern "C" JNIEXPORT jstring JNICALL
Java_org_xtgo_xcube_base_XcubeBase_stringFromJNI2(
        JNIEnv* env,
        jobject /* this */) {
    LOGD ("[*] stringFromJNI2");
    //  close (open ("/etc/hosts", O_RDONLY));
    // close (open ("/etc/fstab", O_RDONLY));
    //close (open ("/etc/fstab", O_RDONLY));
    const char* hello = "Hello from C++ 2";
    return env->NewStringUTF(hello);
}

extern "C" JNIEXPORT jstring JNICALL Java_org_xtgo_xcube_base_XcubeBase_stringFromJNI3(
        JNIEnv* env,
        jobject /* this */) {
//    gumHook();
//  funcForTest();
    LOGD ("[*] stringFromJNI3");
    const char* hello = "Hello from C++";
    return env->NewStringUTF(hello);
}

static jint JNICALL cAdd(JNIEnv *env, jobject jobj, jint x, jint y){
    LOGI("cAdd x is :%d  y is :%d", x, y);
    return x + y;
}

static jstring JNICALL cSayHi(JNIEnv *env, jobject jobj, jint x, jint y){
    LOGD ("[*] stringFromJNI4");
    return env->NewStringUTF("hello from cSayHi");
}

/**
   第一个参数：javaAdd 是java中的方法名称
   第二个参数：(II)I  是java中方法的签名，可以通过javap -s -p 类名.class 查看
   第三个参数： (jstring *)cSayHi  （返回值类型）映射到native的方法名称

*/
static const JNINativeMethod gMethods[] = {
        {"stringFromJNI4","()Ljava/lang/String;",(jstring *)cSayHi}
};


static jclass myClass;
// 这里是java调用C的存在Native方法的类路径
static const char* className="org/xtgo/xcube/base/XcubeBase";
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved){
    LOGI("jni onload called");
    JNIEnv* env = NULL; //注册时在JNIEnv中实现的，所以必须首先获取它
    jint result = -1;
    if(vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) { //从JavaVM获取JNIEnv，一般使用1.4的版本
        return -1;
    }
    // 获取映射的java类
    myClass = env->FindClass(className);
    if(myClass == NULL)
    {
        return -1;
    }
    // 通过RegisterNatives方法动态注册
    if(env->RegisterNatives(myClass, gMethods, sizeof(gMethods)/sizeof(gMethods[0])) < 0)
    {
        return -1;
    }
    LOGI("jni onload called end...");
    return JNI_VERSION_1_4; //这里很重要，必须返回版本，否则加载会失败。
}