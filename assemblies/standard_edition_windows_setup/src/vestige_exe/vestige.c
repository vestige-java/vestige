#define _WIN32_WINNT 0x05010000

#ifdef UNICODE
#define _UNICODE
#endif

#include <TCHAR.H>
#include <winsock2.h>
#include <windows.h>
#include "resources.h"
#include <stdio.h>
#include <string.h>

#define MY_WM_NOTIFYICON WM_USER+1
#define WM_SOCKET WM_USER+2
#define VESTIGE_CLASSNAME TEXT("fr.gaellalire.vestige")
#define VESTIGE_VALUE_NAME TEXT("Vestige")

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
int forceStop;
HANDLE hjob;

char buffer[32767];
INT32 bufferRemain;
INT32 bufferSize;
int bufferSizeBytes;


TCHAR * url, *base;

LRESULT CALLBACK MainWndProc( HWND, UINT, WPARAM, LPARAM);

BOOL CALLBACK PostCloseEnum(HWND hWnd, LPARAM lParam) {
    DWORD dwID = 0;
    int i;
    PJOBOBJECT_BASIC_PROCESS_ID_LIST procList = (PJOBOBJECT_BASIC_PROCESS_ID_LIST) lParam;

    GetWindowThreadProcessId(hWnd, &dwID);

    for (i = 0; i < procList->NumberOfProcessIdsInList; ++i) {
        // close windows but not bat window
        if (dwID == procList->ProcessIdList[i] && dwID != vestigePid) {
            PostMessage(hWnd, WM_CLOSE, 0, 0);
        }
    }

    return TRUE;
}

DWORD WINAPI WaitForBatCommand(void * param) {
    TCHAR chBuf[1024];
#ifdef UNICODE
    TCHAR wchBuf[1024 * 2];
#endif
    DWORD dwRead;

    while (ReadFile(g_hChildStd_OUT_Rd, chBuf, sizeof(chBuf) - 1, &dwRead, NULL)) {
#ifdef UNICODE
        int mbRet = MultiByteToWideChar(CP_UTF8, 0, (char *) & chBuf[0], dwRead, wchBuf, 1024 * 2);
        wchBuf[mbRet] = 0;
#else
        chBuf[dwRead] = 0;
#endif

        int ndx = GetWindowTextLength(hEditIn);
        SetFocus(hEditIn);
        SendMessage(hEditIn, EM_SETSEL, ndx, ndx);

#ifdef UNICODE
        SendMessage(hEditIn, EM_REPLACESEL, 0, (LPARAM) & wchBuf[0]);
#else
        SendMessage(hEditIn, EM_REPLACESEL, 0, (LPARAM) & chBuf[0]);
#endif
    }
    WaitForSingleObject(vestigeProc, INFINITE);
    if (consoleWinShown || procState < 2) {
        // user show console or starting failed
        procState = 5;
        Shell_NotifyIcon(NIM_DELETE, &TrayIcon);
        ShowWindow(hWnd, SW_SHOW);
    } else {
        // stop
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

int APIENTRY _tWinMain(HINSTANCE hinstance, HINSTANCE hPrevInstance,
        LPTSTR lpCmdLine, int nCmdShow) {
    MSG msg;
    WNDCLASS wc;
    DWORD pid;
    HANDLE g_hChildStd_OUT_Wr;
    HACCEL hAccel;

    SECURITY_ATTRIBUTES saAttr;

    saAttr.nLength = sizeof(SECURITY_ATTRIBUTES);
    saAttr.bInheritHandle = TRUE;
    saAttr.lpSecurityDescriptor = NULL;

    if (CreateMutex (NULL, FALSE, TEXT("VestigeMutex")) == NULL || GetLastError() == ERROR_ALREADY_EXISTS) {
        hWnd = FindWindowEx(NULL, NULL, VESTIGE_CLASSNAME, NULL);
        if (hWnd) {
            // notify other
            SetForegroundWindow(hWnd);
        }
        return 0;
    }

    forceStop = 0;
    GetModuleFileName(NULL, szPath, MAX_PATH);
    GetModuleFileName(NULL, szPathDirectory, MAX_PATH);
    TCHAR * lastSep = _tcsrchr(szPathDirectory, '\\');
    *lastSep = 0;
    _sntprintf(szPathBat, MAX_PATH, TEXT("cmd /c \"%s\\vestige.bat\" < nul"), szPathDirectory);
    RegOpenKey(HKEY_CURRENT_USER, TEXT("Software\\Microsoft\\Windows\\CurrentVersion\\Run"), &hKey);
    if (RegQueryValueEx(hKey, VESTIGE_VALUE_NAME, NULL, NULL, NULL, NULL) == ERROR_SUCCESS) {
        atLoginStarted = 1;
    } else {
        atLoginStarted = 0;
    }

    consoleWinShown = FALSE;
    procState = 0;
    hinst = hinstance;
    bufferSizeBytes = 0;
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

    hWnd = CreateWindow(wc.lpszClassName, TEXT("Vestige: command line output"), WS_OVERLAPPEDWINDOW,
            CW_USEDEFAULT, CW_USEDEFAULT, 700, 500,
            NULL, NULL, hinstance, NULL);

    hmenu = LoadMenu(hinst, TEXT("VESTIGE_MENU"));

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

    TCHAR portString[10];
    _sntprintf(portString, 10, TEXT("%d"), ntohs(SockAddr.sin_port));

    SetEnvironmentVariable(TEXT("VESTIGE_LISTENER_PORT"), portString);
    SetEnvironmentVariable(TEXT("VESTIGE_CONSOLE_ENCODING"), TEXT("UTF-8"));

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
            TEXT("EDIT"),
            TEXT(""),
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
    _tcscpy(TrayIcon.szTip, TEXT("Vestige"));

    if (atLoginStarted) {
        CheckMenuItem(hmenu, IDM_START_LOGIN, MF_CHECKED);
    }

    Shell_NotifyIcon(NIM_ADD, &TrayIcon);

    UpdateWindow(hWnd);

    DWORD dwThreadID;
    CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE) WaitForBatCommand, 0, // no thread parameters
            0,// default startup flags
            &dwThreadID);

    hAccel = LoadAccelerators(hinstance, TEXT("VESTIGE_ACCELERATORS")) ;

    while (GetMessage(&msg, NULL, 0, 0)) {
      if (!TranslateAccelerator (hWnd, hAccel, &msg)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
      }
    }

    int numProcs = 10;
    PJOBOBJECT_BASIC_PROCESS_ID_LIST procList = (PJOBOBJECT_BASIC_PROCESS_ID_LIST) LocalAlloc(LPTR,
            sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR));
    while (QueryInformationJobObject(hjob, JobObjectBasicProcessIdList, procList, sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR), NULL) == 0) {
        numProcs = procList->NumberOfAssignedProcesses;
        LocalFree(procList);
        procList = (PJOBOBJECT_BASIC_PROCESS_ID_LIST) LocalAlloc(LPTR, sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR));
    }
    EnumWindows((WNDENUMPROC) PostCloseEnum, (LPARAM) procList);

    WaitForSingleObject(vestigeProc, INFINITE);
    CloseHandle(vestigeProc);

    return msg.wParam;
}

/******************************************************************************/

LRESULT CALLBACK MainWndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
    RECT rc;
    TCHAR cbuf[1024];
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
        case IDM_STOP:
            if (forceStop) {
                TerminateJobObject(hjob, 0);
            } else {
                int numProcs = 10;
                PJOBOBJECT_BASIC_PROCESS_ID_LIST procList = (PJOBOBJECT_BASIC_PROCESS_ID_LIST) LocalAlloc(LPTR,
                        sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR));
                while (QueryInformationJobObject(hjob, JobObjectBasicProcessIdList, procList, sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR), NULL) == 0) {
                    numProcs = procList->NumberOfAssignedProcesses;
                    LocalFree(procList);
                    procList = (PJOBOBJECT_BASIC_PROCESS_ID_LIST) LocalAlloc(LPTR, sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR));
                }
                EnumWindows((WNDENUMPROC) PostCloseEnum, (LPARAM) procList);

                MENUITEMINFO info;
                info.cbSize = sizeof(MENUITEMINFO);
                info.fMask = MIIM_ID;
                HMENU hpopup = GetSubMenu(hmenu, 0);
                GetMenuItemInfo(hpopup, 5, TRUE, &info);
                ModifyMenu(hpopup, info.wID, MF_BYCOMMAND | MF_STRING, info.wID, TEXT("Force stop"));
                forceStop = 1;
            }
            break;
        case IDM_OPEN_WEB:
            _sntprintf(cbuf, 1024, TEXT("url.dll,FileProtocolHandler %s"), url);
            ShellExecute(NULL, TEXT("open"), TEXT("rundll32.exe"), cbuf, NULL, SW_SHOWNORMAL);
            break;
        case IDM_OPEN_BASE:
            ShellExecute(NULL, TEXT("open"), base, NULL, NULL, SW_SHOWNORMAL);
            break;
        case IDM_SHOW_CONSOLE:
            consoleWinShown = TRUE;
            ShowWindow(hWnd, SW_SHOWNORMAL);
            SetForegroundWindow(hWnd);
            break;
        case IDM_START_LOGIN:
            toggleStartAtLogin();
            break;
        case IDM_SELECT_ALL:
            SetFocus(hEditIn);
            SendMessage(hEditIn, EM_SETSEL, 0, -1) ;
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

            if (bufferSizeBytes != 4) {
                bufferSizeBytes += recv(csocket, ((char *)&bufferSize)+bufferSizeBytes, 4 - bufferSizeBytes, 0);
                if (bufferSizeBytes != 4) {
                    return 0;
                }
                bufferSize = ntohl(bufferSize);
                bufferRemain = bufferSize;
                return 0;
            }

            bufferRemain -= recv(csocket, buffer + (bufferSize - bufferRemain), bufferRemain, 0);
            if (bufferRemain != 0) {
                return 0;
            }
            bufferSizeBytes = 0;

#ifdef UNICODE
            TCHAR command[bufferSize * 2];
            int mbRet = MultiByteToWideChar(CP_UTF8, 0, &buffer[0], bufferSize, command, 1024);
            command[mbRet] = 0;
#else
            TCHAR * command = (TCHAR  *) &buffer[0];
            command[bufferSize] = 0;
#endif

            int webLen = _tcslen(TEXT("Web "));
            int baseLen = _tcslen(TEXT("Base "));
            if (_tcslen(command) > webLen && _tcsncmp(command, TEXT("Web "), webLen) == 0) {
                url = (TCHAR *) malloc((_tcslen(&command[webLen]) + 1) * sizeof(TCHAR));
                _tcscpy(url, &command[webLen]);
                EnableMenuItem(hmenu, IDM_OPEN_WEB, MF_ENABLED);
            } else if (_tcslen(command) > baseLen && _tcsncmp(command, TEXT("Base "), baseLen) == 0) {
                base = (TCHAR *) malloc((_tcslen(&command[baseLen]) + 1) * sizeof(TCHAR));
                _tcscpy(base, &command[baseLen]);
                EnableMenuItem(hmenu, IDM_OPEN_BASE, MF_ENABLED);
            } else if (_tcscmp(command, TEXT("Starting")) == 0) {
                procState = 1;
            } else if (_tcscmp(command, TEXT("Started")) == 0) {
                procState = 2;
            } else if (_tcscmp(command, TEXT("Stopping")) == 0) {
                procState = 3;
            } else if (_tcscmp(command, TEXT("Stopped")) == 0) {
                procState = 4;
            }
            return 0;

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
