#include <windows.h>
#include "resources.h"

#define MY_WM_NOTIFYICON WM_USER+1

NOTIFYICONDATA TrayIcon;
HINSTANCE hinst;

LRESULT CALLBACK MainWndProc(HWND, UINT, WPARAM, LPARAM);

typedef struct _WaitForBatCommandParam {
	HWND hwnd;
	HANDLE hProcess;
} WaitForBatCommandParam;

typedef struct _smPROCESS_BASIC_INFORMATION {
    LONG ExitStatus;
    void * PebBaseAddress;
    ULONG_PTR AffinityMask;
    LONG BasePriority;
    ULONG_PTR UniqueProcessId;
    ULONG_PTR InheritedFromUniqueProcessId;
} smPROCESS_BASIC_INFORMATION, *smPPROCESS_BASIC_INFORMATION;

typedef LONG (WINAPI* pfnQueryInformationProcess)(
													  HANDLE ProcessHandle,
													  LONG ProcessInformationClass,
													  PVOID ProcessInformation,
													  ULONG ProcessInformationLength,
													  PULONG ReturnLength);

BOOL CALLBACK TerminateAppEnum( HWND hwnd, LPARAM lParam )
{
	DWORD dwID ;
	
	GetWindowThreadProcessId(hwnd, &dwID) ;
	
	if(dwID == (DWORD)lParam)
	{
		PostMessage(hwnd, WM_CLOSE, 0, 0) ;
	}
	
	return TRUE ;
   }

DWORD WINAPI WaitForBatCommand(WaitForBatCommandParam * param) {
	WaitForSingleObject(param->hProcess, INFINITE);
	PostMessage(param->hwnd, WM_COMMAND, IDM_QUIT, 0) ;	
}

int WINAPI WinMain(HINSTANCE hinstance, HINSTANCE hPrevInstance,
				   LPSTR lpCmdLine, int nCmdShow)
{
    HWND hwnd;
    MSG msg;
    WNDCLASS wc;
	DWORD pid;
	WaitForBatCommandParam param;

	char * appName = "vestige";
        char * batfile = "vestige.bat";
	
	SHELLEXECUTEINFO shExecInfo;
	
	shExecInfo.cbSize = sizeof(SHELLEXECUTEINFO);
	shExecInfo.fMask = SEE_MASK_NOCLOSEPROCESS;
	shExecInfo.hwnd = NULL;
	shExecInfo.lpVerb = NULL;
	shExecInfo.lpFile = batfile;
	shExecInfo.lpParameters = NULL;
	shExecInfo.lpDirectory = NULL;
	shExecInfo.nShow = SW_HIDE;
	shExecInfo.hInstApp = NULL;
	
	if (!ShellExecuteEx(&shExecInfo)) {
		return;
	}
	
	pfnQueryInformationProcess ntQIP = (pfnQueryInformationProcess) GetProcAddress(GetModuleHandle("NTDLL.DLL"),"NtQueryInformationProcess");
	smPROCESS_BASIC_INFORMATION info;
	ULONG returnSize;
	ntQIP(shExecInfo.hProcess, 0, &info, sizeof(info), &returnSize);
	pid = info.UniqueProcessId;
	
    hinst = hinstance;	
    wc.style = 0;
    wc.lpfnWndProc = MainWndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    wc.hInstance = hinstance;
    wc.hIcon = LoadIcon(NULL, IDI_APPLICATION);
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(1 + COLOR_BTNFACE);
    wc.lpszMenuName =  NULL;
    wc.lpszClassName = "MaWinClass";
	
    if(!RegisterClass(&wc)) return FALSE;
	
    hwnd = CreateWindow(wc.lpszClassName, appName, WS_OVERLAPPEDWINDOW,
						CW_USEDEFAULT, CW_USEDEFAULT, 0, 0,
						NULL, NULL, hinstance, NULL);
	
    if (!hwnd) return FALSE;

	TrayIcon.cbSize = sizeof( NOTIFYICONDATA );
	TrayIcon.hWnd = hwnd;
	TrayIcon.uID = 0;
	TrayIcon.hIcon = (HICON) LoadImage(hinstance, MAKEINTRESOURCE(IDI_APPICON), IMAGE_ICON, 0, 0, LR_SHARED);
	TrayIcon.uCallbackMessage = MY_WM_NOTIFYICON;
	TrayIcon.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP;
	strcpy(TrayIcon.szTip, appName);
	Shell_NotifyIcon(NIM_ADD,&TrayIcon);
	
    UpdateWindow(hwnd);	
	
	param.hProcess = shExecInfo.hProcess;
	param.hwnd = hwnd;
	DWORD dwThreadID;
	CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE) WaitForBatCommand, &param,              // no thread parameters
								0,                 // default startup flags
								&dwThreadID); 

	
    while (GetMessage(&msg, NULL, 0, 0))
    {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
	
	EnumWindows((WNDENUMPROC)TerminateAppEnum, (LPARAM) pid) ;	
	WaitForSingleObject(shExecInfo.hProcess, INFINITE);
	CloseHandle(shExecInfo.hProcess);
	free(batfile);
	free(appName);
	
    return msg.wParam;
}
/******************************************************************************/

LRESULT CALLBACK MainWndProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
    switch (uMsg)
    {
        case WM_CREATE:
			
            return 0;
			
        case WM_CLOSE:
            return 0;
			
        case MY_WM_NOTIFYICON :
            if(lParam == WM_LBUTTONUP || lParam == WM_RBUTTONUP)
            {
				HMENU hmenu;
				HMENU hpopup;
				POINT pos;
				GetCursorPos(&pos);
				hmenu = LoadMenu(hinst,"LEMENU");
				hpopup = GetSubMenu(hmenu, 0);
				SetForegroundWindow(hwnd);
				TrackPopupMenuEx(hpopup, 0, pos.x, pos.y, hwnd, NULL);              
				DestroyMenu(hmenu);
			 }
            return 0;
			
        case WM_COMMAND:
            if(LOWORD(wParam) == IDM_QUIT) DestroyWindow(hwnd);
            return 0;   
			
        case WM_DESTROY:
            Shell_NotifyIcon(NIM_DELETE,&TrayIcon);
            PostQuitMessage(0);
            return 0;

        default:
            return DefWindowProc(hwnd, uMsg, wParam, lParam);
    }
}
