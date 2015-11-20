#define LOG_TAG "xSocks"

#include "jni.h"
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <cpu-features.h>

#include <sys/un.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <ancillary.h>


#define LOGI(...) do { __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__); } while(0)
#define LOGW(...) do { __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__); } while(0)
#define LOGE(...) do { __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__); } while(0)


static jstring
getABI(JNIEnv *env, jobject thiz) {
    AndroidCpuFamily family = android_getCpuFamily();
    uint64_t features = android_getCpuFeatures();
    const char *abi;

    if (family == ANDROID_CPU_FAMILY_X86) {
        abi = "x86";
    } else if (family == ANDROID_CPU_FAMILY_MIPS) {
        abi = "mips";
    } else if (family == ANDROID_CPU_FAMILY_ARM) {
        abi = "armeabi-v7a";
    }
    return env->NewStringUTF(abi);
}

static void
exec(JNIEnv *env, jobject thiz, jstring cmd) {
    const char *str = env->GetStringUTFChars(cmd, 0);
    system(str);
    env->ReleaseStringUTFChars(cmd, str);
}

static void
jniclose(JNIEnv *env, jobject thiz, jint fd) {
    close(fd);
}

static jint
sendfd(JNIEnv *env, jobject thiz, jint tun_fd) {
    int fd;
    struct sockaddr_un addr;

    if ( (fd = socket(AF_UNIX, SOCK_STREAM, 0)) == -1) {
        LOGE("socket() failed: %s (socket fd = %d)\n", strerror(errno), fd);
        return (jint)-1;
    }

    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, "/data/data/io.github.xSocks/sock_path", sizeof(addr.sun_path)-1);

    if (connect(fd, (struct sockaddr*)&addr, sizeof(addr)) == -1) {
        LOGE("connect() failed: %s (fd = %d)\n", strerror(errno), fd);
        close(fd);
        return (jint)-1;
    }

    if (ancil_send_fd(fd, tun_fd)) {
        LOGE("ancil_send_fd: %s", strerror(errno));
        close(fd);
        return (jint)-1;
    }

    close(fd);

    return 0;
}

static const char *classPathName = "io/github/xSocks/System";
static JNINativeMethod method_table[] = {
    { "jniclose", "(I)V",
        (void*) jniclose },
    { "sendfd", "(I)I",
        (void*) sendfd },
    { "exec", "(Ljava/lang/String;)V",
        (void*) exec },
    { "getABI", "()Ljava/lang/String;",
        (void*) getABI }
};

static int
registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int
registerNatives(JNIEnv* env) {
    if (!registerNativeMethods(env, classPathName, method_table,
          sizeof(method_table) / sizeof(method_table[0]))) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint
JNI_OnLoad(JavaVM* vm, void* reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto fail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto fail;
    }

    result = JNI_VERSION_1_4;

fail:
    return result;
}
