//
// Created by sven on 2021/1/19.
//
#include <unistd.h>
#include <pthread.h>
#include <android/log.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <cstdio>
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <sys/inotify.h>

#define LOGTAG "Xcube_shellcmd"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOGTAG , __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , LOGTAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , LOGTAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , LOGTAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR , LOGTAG, __VA_ARGS__)

int watchScript();

const char *filepath = "/data/local/tmp/myscript.js";
#define EVENT_NUM 12
char *event_str[EVENT_NUM] =
        {
                "IN_ACCESS",
                "IN_MODIFY",
                "IN_ATTRIB",
                "IN_CLOSE_WRITE",
                "IN_CLOSE_NOWRITE",
                "IN_OPEN",
                "IN_MOVED_FROM",
                "IN_MOVED_TO",
                "IN_CREATE",
                "IN_DELETE",
                "IN_DELETE_SELF",
                "IN_MOVE_SELF"
        };

int mysystem(char *cmdstring, char *buf, int len) {
    int fd[2];
    pid_t pid;
    int n, count;
    memset(buf, 0, len);
    if (pipe(fd) < 0)
        return -1;
    if ((pid = fork()) < 0)
        return -1;
    else if (pid > 0) {
        close(fd[1]);
        count = 0;
        while ((n = read(fd[0], buf + count, len)) > 0 && count > len)
            count += n;
        close(fd[0]);
        if (waitpid(pid, NULL, 0) > 0)
            return -1;
    } else {
        close(fd[0]);
        if (fd[1] != STDOUT_FILENO) {
            if (dup2(fd[1], STDOUT_FILENO) != STDOUT_FILENO) {
                return -1;
            }
            close(fd[1]);
        }
        if (execl("/system/bin/sh", "sh", "-c", cmdstring, (char *) 0) == -1)
            return -1;
    }
    return 0;
}

//#include <limits.h>



static void displayInotifyEvent(struct inotify_event *i) {
    LOGD("    wd = %2d; ", i->wd);
    if (i->cookie > 0)
        LOGD("cookie = %4d; ", i->cookie);

    LOGD("mask = ");
    if (i->mask & IN_ACCESS)
        LOGD("IN_ACCESS ");
    if (i->mask & IN_ATTRIB)
        LOGD("IN_ATTRIB ");
    if (i->mask & IN_CLOSE_NOWRITE)
        LOGD("IN_CLOSE_NOWRITE ");
    if (i->mask & IN_CLOSE_WRITE)
        LOGD("IN_CLOSE_WRITE ");
    if (i->mask & IN_CREATE)
        LOGD("IN_CREATE ");
    if (i->mask & IN_DELETE)
        LOGD("IN_DELETE ");
    if (i->mask & IN_DELETE_SELF)
        LOGD("IN_DELETE_SELF ");
    if (i->mask & IN_IGNORED)
        LOGD("IN_IGNORED ");
    if (i->mask & IN_ISDIR)
        LOGD("IN_ISDIR ");
    if (i->mask & IN_MODIFY)
        LOGD("IN_MODIFY ");
    if (i->mask & IN_MOVE_SELF)
        LOGD("IN_MOVE_SELF ");
    if (i->mask & IN_MOVE_SELF)
        LOGD("IN_MOVE_SELF ");
    if (i->mask & IN_MOVED_FROM)
        LOGD("IN_MOVED_FROM ");
    if (i->mask & IN_MOVED_TO)
        LOGD("IN_MOVED_TO ");
    if (i->mask & IN_OPEN)
        LOGD("IN_OPEN ");
    if (i->mask & IN_Q_OVERFLOW)
        LOGD("IN_Q_OVERFLOW ");
    if (i->mask & IN_UNMOUNT)
        LOGD("IN_UNMOUNT ");
    LOGD("\n");

    if (i->len > 0)
        LOGD("    name = %s", i->name);
}

#define BUF_LEN (10 * (sizeof(struct inotify_event) + NAME_MAX + 1))


int rirutest() {

    const char *package_name = "org.xtgo.xcube.base";
    LOGD("package name: %s", package_name);

    char cmd_string[1024];
    const char *filepath = "/data/local/tmp/pkg.conf";
    LOGD(cmd_string, "cat %s", filepath);
    int bufsize = 1024 * 10;
    char buf[bufsize];
    mysystem(cmd_string, buf, bufsize);

    int ret = 0;
    char *item = NULL;
    char *delims = "\r\n";
    item = strtok(buf, delims);

    while (item != NULL) {
        LOGD("package item: %s", item);
        if (strcmp(item, package_name) == 0) {
            ret = 1;
            break;
        } else {
            ret = 0;
        }
        item = strtok(NULL, delims);
    }
    LOGD("rirutest ret: %d", ret);
    return ret;
}

int watchScript() {
    int fd;
    int wd;
    int len;
    int nread;
    char buf[BUFSIZ];
    struct inotify_event *event;
    int i;


    fd = inotify_init();
    if (fd < 0) {
        LOGE("inotify_init failed");
        return -1;
    }

    wd = inotify_add_watch(fd, filepath, IN_ALL_EVENTS);
    if (wd < 0) {
        LOGE("inotify_add_watch %s failed", filepath);
        return -1;
    }

    buf[sizeof(buf) - 1] = 0;
    while ((len = read(fd, buf, sizeof(buf) - 1)) > 0) {
        LOGD("entry:%d", len);
        nread = 0;
        while (len > 0) {
            event = (struct inotify_event *) &buf[nread];
            for (i = 0; i < EVENT_NUM; i++) {
                if ((event->mask >> i) & 1) {
                    if (event->len > 0)
                        LOGD("%s --- %s", event->name, event_str[i]);
                    else
                        LOGD("%s --- %s", " ", event_str[i]);
                }
            }

            nread = nread + sizeof(struct inotify_event) + event->len;
            len = len - sizeof(struct inotify_event) - event->len;
            LOGD("read:%d,%d,%d,%d", len, nread, sizeof(struct inotify_event), event->len);
        }
        memset(buf, 0, sizeof(buf) - 1);
    }

}

int
main(int argc,
     char *argv[]) {
//    watchScript();
//    pthread_t pthread2;
//    pthread_create(&pthread2, NULL,
//                   reinterpret_cast<void *(*)(void *)>(watchScript), NULL);
//    rirutest();
    printf("started");
    int inotifyFd, wd, j;
    char buf[BUF_LEN];
    ssize_t numRead;
    char *p;
    struct inotify_event *event;


    inotifyFd = inotify_init();
    if (inotifyFd == -1)
        LOGE("inotify_init");

    wd = inotify_add_watch(inotifyFd, filepath, IN_ALL_EVENTS);
    if (wd == -1)
        LOGE("inotify_add_watch");

    LOGD("Watching %s using wd %d", filepath, wd);


    for (;;) {
        numRead = read(inotifyFd, buf, BUF_LEN);
        if (numRead == 0)
            LOGE("read() from inotify fd return 0!");

        if (numRead == -1)
            LOGE("read");

        LOGD("Read %ld bytes from inotify fd", (long) numRead);

        for (p = buf; p < buf + numRead;) {
            event = (struct inotify_event *) p;
            displayInotifyEvent(event);
            p += sizeof(struct inotify_event) + event->len;
        }
    }
}