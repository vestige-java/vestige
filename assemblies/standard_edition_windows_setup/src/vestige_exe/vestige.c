#define _WIN32_WINNT 0x05010000

#include <windows.h>
#include <winsock2.h>
#include "resources.h"
#include <stdio.h>

#define MY_WM_NOTIFYICON WM_USER+1
#define WM_SOCKET WM_USER+2
#define VESTIGE_CLASSNAME "fr.gaellalire.vestige"
#define VESTIGE_VALUE_NAME "Vestige"

#define C_KEY 0x43

NOTIFYICONDATA TrayIcon;
HINSTANCE hinst;
HWND hWnd;
HWND hEditIn;
SOCKET ssocket, csocket;
HMENU hmenu;
int procState;
HANDLE g_hChildStd_OUT_Rd;
HANDLE vestigeProc;
DWORD vestigePid;
BOOL consoleWinShown;
TCHAR szPath[MAX_PATH];
TCHAR szPathDirectory[MAX_PATH];
TCHAR szPathBat[MAX_PATH];
HKEY hKey;
int atLoginStarted;
int forceQuit;
HANDLE hjob;

char * url, *base;

LRESULT CALLBACK MainWndProc( HWND, UINT, WPARAM, LPARAM);

BOOL CALLBACK TerminateAppEnum(HWND hWnd, LPARAM lParam) {
    DWORD dwID;

    GetWindowThreadProcessId(hWnd, &dwID);

    if (dwID == (DWORD) lParam) {
        if (SetForegroundWindow(hWnd)) {
            keybd_event(VK_LCONTROL, 0, 0, 0);
            keybd_event(C_KEY, 0, 0, 0);
            keybd_event(C_KEY, 0, KEYEVENTF_KEYUP, 0);
            keybd_event(VK_LCONTROL, 0, KEYEVENTF_KEYUP, 0);
        }
    }

    return TRUE;
}

DWORD WINAPI WaitForBatCommand(void * param) {
    char chBuf[1024];
    DWORD dwRead;

    while (ReadFile(g_hChildStd_OUT_Rd, chBuf, sizeof(chBuf) - 1, &dwRead, NULL)) {
        chBuf[dwRead] = 0;
        int ndx = GetWindowTextLength(hEditIn);
        SetFocus(hEditIn);
        SendMessage(hEditIn, EM_SETSEL, ndx, ndx);

        SendMessage(hEditIn, EM_REPLACESEL, 0, (LPARAM) & chBuf[0]);
    }
    WaitForSingleObject(vestigeProc, INFINITE);
    if (consoleWinShown || procState < 2) {
        // user show console or starting failed
        procState = 5;
        Shell_NotifyIcon(NIM_DELETE, &TrayIcon);
        ShowWindow(hWnd, SW_SHOW);
    } else {
        // quit
        procState = 5;
        PostMessage(hWnd, WM_CLOSE, 0, 0);
    }
}

HANDLE launchVestige(HANDLE g_hChildStd_OUT_Wr) {
    hjob = CreateJobObject(NULL, NULL);

    STARTUPINFO siStartInfo;

    ZeroMemory(&siStartInfo, sizeof(STARTUPINFO));
    siStartInfo.cb = sizeof(STARTUPINFO);
    siStartInfo.hStdError = g_hChildStd_OUT_Wr;
    siStartInfo.hStdOutput = g_hChildStd_OUT_Wr;
    siStartInfo.dwFlags |= STARTF_USESTDHANDLES | STARTF_USESHOWWINDOW;
    siStartInfo.wShowWindow = SW_HIDE;

    PROCESS_INFORMATION pi;
    ZeroMemory(&pi, sizeof(PROCESS_INFORMATION));

    CreateProcess(NULL, szPathBat, NULL, NULL, TRUE, CREATE_SUSPENDED | CREATE_BREAKAWAY_FROM_JOB, NULL, NULL, &siStartInfo, &pi);
    AssignProcessToJobObject(hjob, pi.hProcess);
    ResumeThread(pi.hThread);
    vestigePid = pi.dwProcessId;
    return pi.hProcess;
}

void toggleStartAtLogin() {
    if (atLoginStarted) {
        RegDeleteValue(hKey, VESTIGE_VALUE_NAME);
        atLoginStarted = 0;
        CheckMenuItem(hmenu, IDM_START_LOGIN, MF_UNCHECKED);
    } else {
        RegSetValueEx(hKey, VESTIGE_VALUE_NAME, 0, REG_SZ, (LPBYTE) szPath, sizeof(szPath));
        atLoginStarted = 1;
        CheckMenuItem(hmenu, IDM_START_LOGIN, MF_CHECKED);
    }
}

int WINAPI WinMain(HINSTANCE hinstance, HINSTANCE hPrevInstance,
        LPSTR lpCmdLine, int nCmdShow) {
    MSG msg;
    WNDCLASS wc;
    DWORD pid;
    HANDLE g_hChildStd_OUT_Wr;

    SECURITY_ATTRIBUTES saAttr;

    saAttr.nLength = sizeof(SECURITY_ATTRIBUTES);
    saAttr.bInheritHandle = TRUE;
    saAttr.lpSecurityDescriptor = NULL;

    hWnd = FindWindowEx(NULL, NULL, VESTIGE_CLASSNAME, NULL);
    if (hWnd) {
        SetForegroundWindow(hWnd);
        // notify other
        return 0;
    }

    forceQuit = 0;
    GetModuleFileName(NULL, szPath, MAX_PATH);
    GetModuleFileName(NULL, szPathDirectory, MAX_PATH);
    char * lastSep = strrchr(szPathDirectory, '\\');
    *lastSep = 0;
    snprintf(szPathBat, MAX_PATH, "cmd /c \"%s\\vestige.bat\" < nul", szPathDirectory);
    RegOpenKey(HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Run", &hKey);
    if (RegQueryValueEx(hKey, VESTIGE_VALUE_NAME, NULL, NULL, NULL, NULL) == ERROR_SUCCESS) {
        atLoginStarted = 1;
    } else {
        atLoginStarted = 0;
    }

    consoleWinShown = FALSE;
    procState = 0;
    hinst = hinstance;
    wc.style = 0;
    wc.lpfnWndProc = MainWndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    wc.hInstance = hinstance;
    wc.hIcon = (HICON) LoadImage(hinstance, MAKEINTRESOURCE(IDI_APPICON), IMAGE_ICON, 0, 0, LR_SHARED);
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(1 + COLOR_BTNFACE);
    wc.lpszMenuName = NULL;
    wc.lpszClassName = VESTIGE_CLASSNAME;

    if(!RegisterClass(&wc)) return FALSE;

    hWnd = CreateWindow(wc.lpszClassName, "Vestige: command line output", WS_OVERLAPPEDWINDOW,
            CW_USEDEFAULT, CW_USEDEFAULT, 700, 500,
            NULL, NULL, hinstance, NULL);

    hmenu = LoadMenu(hinst, "VESTIGE_MENU");

    WSADATA WsaDat;
    WSAStartup(MAKEWORD(2,2),&WsaDat);

    SOCKADDR_IN SockAddr;
    SockAddr.sin_port=htons(0);
    SockAddr.sin_family=AF_INET;
    SockAddr.sin_addr.s_addr=inet_addr("127.0.0.1");
    int SockAddrSize = sizeof(SockAddr);

    ssocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    WSAAsyncSelect(ssocket, hWnd, WM_SOCKET, FD_ACCEPT);
    bind(ssocket,(LPSOCKADDR)&SockAddr, SockAddrSize);
    getsockname(ssocket, (SOCKADDR *)&SockAddr, &SockAddrSize);
    listen(ssocket, 1);

    char portString[10];
    sprintf(portString, "%d", ntohs(SockAddr.sin_port));

    SetEnvironmentVariable(TEXT("VESTIGE_LISTENER_PORT"), portString);

    CreatePipe(&g_hChildStd_OUT_Rd, &g_hChildStd_OUT_Wr, &saAttr, 0);
    SetHandleInformation(g_hChildStd_OUT_Rd, HANDLE_FLAG_INHERIT, 0);

    // launch
    vestigeProc = launchVestige(g_hChildStd_OUT_Wr);
    CloseHandle(g_hChildStd_OUT_Wr);

    RECT rc;

    GetWindowRect ( hWnd, &rc );

    int xPos = (GetSystemMetrics(SM_CXSCREEN) - rc.right)/2;
    int yPos = (GetSystemMetrics(SM_CYSCREEN) - rc.bottom)/2;

    SetWindowPos( hWnd, 0, xPos, yPos, 0, 0, SWP_NOZORDER | SWP_NOSIZE );

    GetClientRect(hWnd, &rc);

    hEditIn=CreateWindowEx(WS_EX_CLIENTEDGE,
            "EDIT",
            "",
            WS_CHILD|WS_VISIBLE|ES_MULTILINE|
            ES_AUTOVSCROLL|ES_AUTOHSCROLL |
            WS_HSCROLL | WS_VSCROLL | ES_READONLY,
            rc.left,
            rc.top,
            rc.right - rc.left,
            rc.bottom - rc.top,
            hWnd,
            NULL,
            GetModuleHandle(NULL),
            NULL);

    if (!hWnd) return FALSE;

    ShowWindow(hWnd, SW_HIDE);

    TrayIcon.cbSize = sizeof( NOTIFYICONDATA );
    TrayIcon.hWnd = hWnd;
    TrayIcon.uID = 0;
    TrayIcon.hIcon = (HICON) LoadImage(hinstance, MAKEINTRESOURCE(IDI_APPICON), IMAGE_ICON, 0, 0, LR_SHARED);
    TrayIcon.uCallbackMessage = MY_WM_NOTIFYICON;
    TrayIcon.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP;
    strcpy(TrayIcon.szTip, "Vestige");

    if (atLoginStarted) {
        CheckMenuItem(hmenu, IDM_START_LOGIN, MF_CHECKED);
    }

    Shell_NotifyIcon(NIM_ADD, &TrayIcon);

    UpdateWindow(hWnd);

    DWORD dwThreadID;
    CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE) WaitForBatCommand, 0, // no thread parameters
            0,// default startup flags
            &dwThreadID);

    while (GetMessage(&msg, NULL, 0, 0))
    {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    EnumWindows((WNDENUMPROC)TerminateAppEnum, (LPARAM) vestigePid);
    WaitForSingleObject(vestigeProc, INFINITE);
    CloseHandle(vestigeProc);

    return msg.wParam;
}

/******************************************************************************/

LRESULT CALLBACK MainWndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
    RECT rc;
    char cbuf[1024];
    switch (uMsg) {
    case WM_CREATE:
    case WM_SIZE:
    case WM_SIZING:
        GetClientRect(hWnd, &rc);
        MoveWindow(hEditIn, rc.left, rc.top, rc.right - rc.left, rc.bottom - rc.top, TRUE);
        return 0;
    case WM_CLOSE:
        if (procState == 5) {
            DestroyWindow(hWnd);
        } else {
            consoleWinShown = FALSE;
            ShowWindow(hWnd, SW_HIDE);
        }
        return 0;

    case MY_WM_NOTIFYICON:
        if (lParam == WM_LBUTTONUP || lParam == WM_RBUTTONUP) {
            HMENU hpopup;
            POINT pos;
            GetCursorPos(&pos);
            hpopup = GetSubMenu(hmenu, 0);
            SetForegroundWindow(hWnd);
            TrackPopupMenuEx(hpopup, 0, pos.x, pos.y, hWnd, NULL);
        }
        return 0;

    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case IDM_QUIT:
            if (forceQuit) {
                TerminateJobObject(hjob, 0);
            } else {
                EnumWindows((WNDENUMPROC) TerminateAppEnum, (LPARAM) vestigePid);
                MENUITEMINFO info;
                info.cbSize = sizeof(MENUITEMINFO);
                info.fMask = MIIM_ID;
                HMENU hpopup = GetSubMenu(hmenu, 0);
                GetMenuItemInfo(hpopup, 5, TRUE, &info);
                ModifyMenu(hpopup, info.wID, MF_BYCOMMAND | MF_STRING, info.wID, "Force quit");
                forceQuit = 1;
            }
            break;
        case IDM_OPEN_WEB:
            sprintf(cbuf, "url.dll,FileProtocolHandler %s", url);
            ShellExecute(NULL, "open", "rundll32.exe", cbuf, NULL, SW_SHOWNORMAL);
            break;
        case IDM_OPEN_BASE:
            ShellExecute(NULL, "open", base, NULL, NULL, SW_SHOWNORMAL);
            break;
        case IDM_SHOW_CONSOLE:
            consoleWinShown = TRUE;
            ShowWindow(hWnd, SW_SHOWNORMAL);
            SetForegroundWindow(hWnd);
            break;
        case IDM_START_LOGIN:
            toggleStartAtLogin();
            break;
        }
        return 0;

    case WM_DESTROY:
        Shell_NotifyIcon(NIM_DELETE, &TrayIcon);
        PostQuitMessage(0);
        return 0;

    case WM_SOCKET: {
        switch (WSAGETSELECTEVENT(lParam)) {
        case FD_READ: {
            int i;
            char szIncoming[1024];
            ZeroMemory(szIncoming, sizeof(szIncoming));

            int len = recv(csocket, (char*) szIncoming, sizeof(szIncoming) / sizeof(szIncoming[0]), 0);

            char * command = (char *) &szIncoming[0];
            for (i = 0; i < len; i++) {
                if (szIncoming[i] == '\r') {
                    szIncoming[i] = 0;
                    int webLen = strlen("Web ");
                    int baseLen = strlen("Base ");
                    if (strlen(command) > webLen && strncmp(command, "Web ", webLen) == 0) {
                        url = (char *) malloc(strlen(&command[webLen]) + 1);
                        strcpy(url, &command[webLen]);
                        EnableMenuItem(hmenu, IDM_OPEN_WEB, MF_ENABLED);
                    } else if (strlen(command) > baseLen && strncmp(command, "Base ", baseLen) == 0) {
                        base = (char *) malloc(strlen(&command[baseLen]) + 1);
                        strcpy(base, &command[baseLen]);
                        EnableMenuItem(hmenu, IDM_OPEN_BASE, MF_ENABLED);
                    } else if (strcmp(command, "Starting") == 0) {
                        procState = 1;
                    } else if (strcmp(command, "Started") == 0) {
                        procState = 2;
                    } else if (strcmp(command, "Stopping") == 0) {
                        procState = 3;
                    } else if (strcmp(command, "Stopped") == 0) {
                        procState = 4;
                    }
                    command = (char *) &szIncoming[i + 2];
                }
            }
        }
            break;
        case FD_ACCEPT: {
            csocket = accept(ssocket, 0, 0);
            WSAAsyncSelect(csocket, hWnd, WM_SOCKET, FD_READ);
        }
            break;
        }
    }

    default:
        return DefWindowProc(hWnd, uMsg, wParam, lParam);
    }
}
