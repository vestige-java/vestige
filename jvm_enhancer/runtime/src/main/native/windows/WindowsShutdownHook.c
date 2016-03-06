#include "WindowsShutdownHook.h"
#include "windows.h"
#define CLASSNAME "SHUTDOWN_HOOK_CLASS"

jclass wshClass;
JavaVM *jvm;

void runHooks() {
    JNIEnv *env;
    jint res = (*jvm)->AttachCurrentThread(jvm, (void **) &env, 0);
    jmethodID mid = (*env)->GetStaticMethodID(env, wshClass, "runHooks", "()V");
    (*env)->CallStaticVoidMethod(env, wshClass, mid);
    (*jvm)->DetachCurrentThread(jvm);
    ExitProcess(0);
}

BOOL WINAPI HandlerRoutine(DWORD dwCtrlType) {
    if (dwCtrlType == CTRL_CLOSE_EVENT || dwCtrlType == CTRL_LOGOFF_EVENT || dwCtrlType == CTRL_SHUTDOWN_EVENT) {
        runHooks();
    }
    return FALSE;
}

LRESULT __stdcall WndProc(HWND handle, UINT umsg, WPARAM wparam, LPARAM lparam) {
    if (umsg == WM_QUERYENDSESSION)
        return TRUE;
    else if (umsg == WM_ENDSESSION || umsg == WM_CLOSE) {
        runHooks();
    } else
        return DefWindowProc(handle, umsg, wparam, lparam);
}

DWORD WINAPI WindowThread(void * param) {
    HINSTANCE instance = GetModuleHandle(NULL);

    WNDCLASSEX wx;
    memset(&wx, 0, sizeof(WNDCLASSEX));
    wx.cbSize = sizeof(WNDCLASSEX);
    wx.lpfnWndProc = WndProc;
    wx.hInstance = instance;
    wx.lpszClassName = CLASSNAME;
    RegisterClassEx(&wx);
    CreateWindowEx(0, CLASSNAME, "SHUTDOWN_HOOK", 0, 0, 0, 0, 0, NULL, NULL, NULL, NULL);
    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
}

JNIEXPORT void JNICALL Java_fr_gaellalire_vestige_jvm_1enhancer_runtime_windows_WindowsShutdownHook_nativeRegister
(JNIEnv * env, jclass cls) {
    wshClass = (*env)->NewGlobalRef(env, cls);
    (*env)->GetJavaVM(env, &jvm);
    SetConsoleCtrlHandler(&HandlerRoutine, TRUE);

    CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)WindowThread, 0, 0, NULL);
}

