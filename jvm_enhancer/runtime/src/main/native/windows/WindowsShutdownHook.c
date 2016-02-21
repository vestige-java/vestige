#include "WindowsShutdownHook.h"
#include "windows.h"

jclass wshClass;
JavaVM *jvm;

BOOL WINAPI HandlerRoutine(DWORD dwCtrlType) {
    if (dwCtrlType == CTRL_CLOSE_EVENT || dwCtrlType == CTRL_LOGOFF_EVENT || dwCtrlType == CTRL_SHUTDOWN_EVENT) {
        JNIEnv *env;
        jint res = (*jvm)->AttachCurrentThread(jvm, (void **) &env, 0);
        jmethodID mid = (*env)->GetStaticMethodID(env, wshClass, "runHooks", "()V");
        (*env)->CallStaticVoidMethod(env, wshClass, mid);
        (*jvm)->DetachCurrentThread(jvm);
        ExitProcess(0);
    }
    return FALSE;
}

JNIEXPORT void JNICALL Java_fr_gaellalire_vestige_jvm_1enhancer_runtime_windows_WindowsShutdownHook_nativeRegister
(JNIEnv * env, jclass cls) {
    wshClass = (*env)->NewGlobalRef(env, cls);
    (*env)->GetJavaVM(env, &jvm);
    SetConsoleCtrlHandler(&HandlerRoutine, TRUE);
}

