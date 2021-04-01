/*
 * Compile with:
 *
 * clang -fPIC -DANDROID -ffunction-sections -fdata-sections -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -Os -pipe -g3 -mthumb frida-gumjs-example.c -o frida-gumjs-example -L. -lfrida-gumjs -llog -ldl -lm -pthread -Wl,--gc-sections,-z,noexecstack,-z,relro,-z,now -fuse-ld=gold -fuse-ld=gold -Wl,--icf=all,--fix-cortex-a8
 *
 * Visit https://frida.re to learn more about Frida.
 */

#include "frida-gumjs.h"
#include <fcntl.h>
#include <unistd.h>
#include <jni.h>
#include <android/log.h>
#include <sys/stat.h>
#include <sys/system_properties.h>//获取系统属性，不过这里没用到

const char *confpath = "/data/local/tmp/pkg.conf";
static pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
static GumScriptBackend *backend;
static GCancellable *cancellable = NULL;
static GError *error = NULL;
static GumScript *script;
static GMainContext *context;
static GMainLoop *loop;


bool checkConfguared(const char *packagename) {
    FILE *file = fopen(confpath, "r");
    if (file == NULL) {
        LOGE("file open failed %s",packagename);
        return false;
    }
    char *packages = readfile(confpath);
    int ret = 0;
    char *item = NULL;
    char *delims = "\r\n";
    item = strtok(packages, delims);
    while (item != NULL) {
        if (strcmp(item, packagename) == 0) {
            ret = 1;
            break;
        } else {
            ret = 0;
        }
        item = strtok(NULL, delims);
    }
    return ret;
}

char *readfile(const char *filepath) {
    FILE *file = fopen(filepath, "r");
    if (file == NULL) {
        LOGE("file open failed : %s " ,filepath);
        return NULL;
    }

    struct stat statbuf{};
    stat(filepath, &statbuf);
    int filesize = statbuf.st_size;

    void *buffer = malloc(filesize + 1);
    memset(buffer, 0, filesize + 1);
    int count = 0;
    int total = 0;
    while ((count = fread((char *) buffer + total, sizeof(char), 1024, file)) != 0) {
        total += count;
    }
    if (file != NULL) {
        fclose(file);
    }
    return (char *) buffer;
}

int hookFunc(const char *scriptpath) {
    LOGD ("[*] gumjsHook()");
    gum_init_embedded();
    backend = gum_script_backend_obtain_qjs();
    char *js = readfile(scriptpath);
    if (!js) {
        return 1;
    }

    script = gum_script_backend_create_sync(backend, "example", js, cancellable, &error);
    g_assert (error == NULL);
    gum_script_set_message_handler(script, on_message, NULL, NULL);
    gum_script_load_sync(script, cancellable);
    //下面这段代码会执行一下已有的事件
    context = g_main_context_get_thread_default();
    while (g_main_context_pending(context))
        g_main_context_iteration(context, FALSE);
    //到这里说明脚本已经加载完成，通知主线程继续执行
    pthread_mutex_lock(&mtx);
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mtx);

    loop = g_main_loop_new(g_main_context_get_thread_default(), FALSE);
    g_main_loop_run(loop);//block here

    return 0;
}

int gumjsHook(const char *scriptpath) {
    pthread_t pthread;
    int result = pthread_create(&pthread, NULL, (void *(*)(void *)) (hookFunc),
                                (void *) scriptpath);
    struct timeval now;
    struct timespec outtime;
    pthread_mutex_lock(&mtx);
    gettimeofday(&now, NULL);
    outtime.tv_sec = now.tv_sec + 5;
    outtime.tv_nsec = now.tv_usec * 1000;
    pthread_cond_timedwait(&cond, &mtx, &outtime);
    pthread_mutex_unlock(&mtx);
    if (result != 0) {
        LOGD("create thread failed");
    } else {
        LOGD("create thread success");
    }
    return result;
}

static void
on_message(GumScript *script, const gchar *message,
           GBytes *data, gpointer user_data) {
    JsonParser *parser;
    JsonObject *root;
    const gchar *type;

    parser = json_parser_new();
    json_parser_load_from_data(parser, message, -1, NULL);
    root = json_node_get_object(json_parser_get_root(parser));

    type = json_object_get_string_member(root, "type");
    if (strcmp(type, "log") == 0) {
        const gchar *log_message;
        log_message = json_object_get_string_member(root, "payload");
        LOGD ("[*] log : %s ", log_message);
    } else {
        LOGD ("[*] %s ", message);
    }

    g_object_unref(parser);
}

