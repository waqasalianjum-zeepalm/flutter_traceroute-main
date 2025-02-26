#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <malloc.h>
#include "traceroute.h"
#include <pthread.h>

#define OUTPUT_LENGTH  10000

#define TAG "traceroute-jni" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

JavaVM *g_jvm;
int exec_status = -3;
static jobject j_java_cls = NULL;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6)) {
        LOGE("Could not get JNIEnv*");
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

void JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = (*env)->FindClass(env, name);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
    }
    (*env)->DeleteLocalRef(env, cls);
}

JNIEnv *JNU_GetEnv() {
    JNIEnv *env;
    (*g_jvm)->GetEnv(g_jvm, (void **) &env, JNI_VERSION_1_6);
    return env;
}


void call_java_append_result(const char *text) {
    JNIEnv *jniEnv = JNU_GetEnv();
    jclass j_cls = (*jniEnv)->GetObjectClass(jniEnv, j_java_cls);
    jmethodID j_append_result = (*jniEnv)->GetMethodID(jniEnv,
                                                       j_cls,
                                                       "appendResult",
                                                       "(Ljava/lang/String;)V");
    if (j_append_result == NULL) {
        LOGE("can't find appendResult() method. do you add proguard to proguard-rules.pro");
        return;
    }
    jstring message = (*jniEnv)->NewStringUTF(jniEnv, text);
    (*jniEnv)->CallVoidMethod(jniEnv, j_java_cls, j_append_result, message);
    (*jniEnv)->DeleteLocalRef(jniEnv, message);
}

int printf(const char *fmt, ...) {
    va_list argptr;
    int cnt;
    va_start(argptr, fmt);
    char *buffer = (char *) malloc(OUTPUT_LENGTH);
    memset(buffer, 0, OUTPUT_LENGTH);
    cnt = vsnprintf(buffer, OUTPUT_LENGTH, fmt, argptr);
    buffer[cnt] = '\0';
    call_java_append_result(buffer);
    free(buffer);
    va_end(argptr);
    return 1;
}

int fprintf(FILE *fp, const char *fmt, ...) {
    va_list argptr;
    int cnt;
    va_start(argptr, fmt);
    char *buffer = (char *) malloc(OUTPUT_LENGTH);
    memset(buffer, 0, OUTPUT_LENGTH);
    cnt = vsnprintf(buffer, OUTPUT_LENGTH, fmt, argptr);
    buffer[cnt] = '\0';
    LOGE("traceroute error message(fprintf): %s", buffer);
    free(buffer);
    va_end(argptr);
    return 1;
}

int vfprintf(FILE *fp, const char *fmt, va_list args) {
    int cnt;
    char *buffer = (char *) malloc(OUTPUT_LENGTH);
    memset(buffer, 0, OUTPUT_LENGTH);
    cnt = vsnprintf(buffer, OUTPUT_LENGTH, fmt, args);
    buffer[cnt] = '\0';
    LOGE("traceroute error message(vfprintf): %s", buffer);
    free(buffer);
    return 1;
}

void perror(const char *msg) {
    LOGE("traceroute error message(perror): %s", msg);
}

void exit(int status) {
    if (status != 0) {
        printf("#end, %ld, %d", pthread_self(), exec_status);
    }
    // avoid some device crash. eg: vivo x7
    (*g_jvm)->DetachCurrentThread(g_jvm);
    exec_status = -3;
    LOGE("traceroute error to exit program, status:%d", status);
    pthread_exit(0);
}

void *do_execute(void *args) {
    jobjectArray *j_argvs = (jobjectArray *)args;

    JNIEnv *jniEnv;
    jint status = (*g_jvm)->AttachCurrentThread(g_jvm, (void **) &jniEnv, NULL);
    if (status != 0) {
        LOGE("AttachCurrentThread failed");
        return NULL;
    }
    jniEnv = JNU_GetEnv();

    jint size = (*jniEnv)->GetArrayLength(jniEnv, j_argvs);
    LOGD("command size:%d", size);

    char *argv[size];
    int i;
    for (i = 0; i < size; i++) {
        jstring command = (jstring) ((*jniEnv)->GetObjectArrayElement(jniEnv, j_argvs, i));
        argv[i] = (char *) ((*jniEnv)->GetStringUTFChars(jniEnv, command, 0));
        LOGD("command %d = %s", i, argv[i]);
    }

    exec_status = exec(size, argv);
    LOGD("execute command result:%d", exec_status);
    (*g_jvm)->DetachCurrentThread(g_jvm);
    return NULL;
}

static int active = 0;

JNIEXPORT jint JNICALL Java_com_qualoo_flutter_1traceroute_TraceRoute_execute
        (JNIEnv *env, jobject jthis, jobjectArray jarray) {
    // The C code is full of static definitions so we can only run it once
    if (active != 0) {
        LOGD("traceroute already running");
        return -1;
    }
    LOGD("start traceroute. active=%d", ++active);
    pthread_t trace_thread;
    if (!j_java_cls) {
        j_java_cls = (*env)->NewGlobalRef(env, jthis);
    }
    jobjectArray j_argvs = (*env)->NewGlobalRef(env, jarray);

    pthread_create(&trace_thread, NULL, do_execute, (void*)j_argvs);
    pthread_join(trace_thread, NULL);

    (*env)->DeleteGlobalRef(env, j_argvs);

    LOGD("finish traceroute, status:%d. active=%d", exec_status, --active);
    return exec_status;
}
