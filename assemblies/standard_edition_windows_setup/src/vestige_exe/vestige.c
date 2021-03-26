#define _WIN32_WINNT _WIN32_WINNT_WINXP

#ifdef UNICODE
#ifndef _UNICODE
#define _UNICODE
#endif
#endif

#include <TCHAR.H>
#include <winsock2.h>
#include <windows.h>
#include <wincrypt.h>
#include "resources.h"
#include <stdio.h>
#include <string.h>
#include <uxtheme.h>

#define USER_DEFAULT_SCREEN_DPI 96
#define WM_DPICHANGED                   0x02E0
#define WM_DWMCOMPOSITIONCHANGED        0x031E
#define TMT_MSGBOXFONT	805


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

enum PreferredAppMode
{
	Default,
	AllowDark,
	ForceDark,
	ForceLight,
	Max
};

enum WINDOWCOMPOSITIONATTRIB {
	WCA_UNDEFINED = 0,
	WCA_NCRENDERING_ENABLED = 1,
	WCA_NCRENDERING_POLICY = 2,
	WCA_TRANSITIONS_FORCEDISABLED = 3,
	WCA_ALLOW_NCPAINT = 4,
	WCA_CAPTION_BUTTON_BOUNDS = 5,
	WCA_NONCLIENT_RTL_LAYOUT = 6,
	WCA_FORCE_ICONIC_REPRESENTATION = 7,
	WCA_EXTENDED_FRAME_BOUNDS = 8,
	WCA_HAS_ICONIC_BITMAP = 9,
	WCA_THEME_ATTRIBUTES = 10,
	WCA_NCRENDERING_EXILED = 11,
	WCA_NCADORNMENTINFO = 12,
	WCA_EXCLUDED_FROM_LIVEPREVIEW = 13,
	WCA_VIDEO_OVERLAY_ACTIVE = 14,
	WCA_FORCE_ACTIVEWINDOW_APPEARANCE = 15,
	WCA_DISALLOW_PEEK = 16,
	WCA_CLOAK = 17,
	WCA_CLOAKED = 18,
	WCA_ACCENT_POLICY = 19,
	WCA_FREEZE_REPRESENTATION = 20,
	WCA_EVER_UNCLOAKED = 21,
	WCA_VISUAL_OWNER = 22,
	WCA_HOLOGRAPHIC = 23,
	WCA_EXCLUDED_FROM_DDA = 24,
	WCA_PASSIVEUPDATEMODE = 25,
	WCA_USEDARKMODECOLORS = 26,
	WCA_LAST = 27
};



struct WINDOWCOMPOSITIONATTRIBDATA
{
	enum WINDOWCOMPOSITIONATTRIB Attrib;
	PVOID pvData;
	SIZE_T cbData;
};

HRESULT (WINAPI * fnSetWindowTheme)(HWND ,LPCWSTR, LPCWSTR) = 0;
COLORREF(WINAPI * fnSetTextColor)(HDC, COLORREF) = 0;
COLORREF(WINAPI * fnSetBkColor)(HDC, COLORREF) = 0;
HGDIOBJ (WINAPI * fnGetStockObject)(int) = 0;
HFONT (WINAPI *fnCreateFontIndirect)(const LOGFONT *) = 0;
int (WINAPI *fnGetDeviceCaps)(HDC , int ) = 0;
BOOL (WINAPI *fnDeleteObject)(HGDIOBJ) = 0;
int (WINAPI *fnGetObject)(HANDLE,int,LPVOID pv) = 0;


void (WINAPI * fnRtlGetNtVersionNumbers)(LPDWORD, LPDWORD, LPDWORD) = 0;
BOOL (WINAPI * fnSetWindowCompositionAttribute)(HWND, struct WINDOWCOMPOSITIONATTRIBDATA *) = 0;
// 1809 17763
BOOL (WINAPI *fnShouldAppsUseDarkMode)() = 0; // ordinal 132
BOOL (WINAPI *fnAllowDarkModeForWindow)(HWND, BOOL) = 0; // ordinal 133
BOOL (WINAPI *fnAllowDarkModeForApp)(BOOL) = 0; // ordinal 135, in 1809
void (WINAPI *fnRefreshImmersiveColorPolicyState)() = 0; // ordinal 104
BOOL (WINAPI *fnIsDarkModeAllowedForWindow)(HWND) = 0; // ordinal 137

enum PreferredAppMode (WINAPI *fnSetPreferredAppMode)(enum PreferredAppMode) = 0; // ordinal 135, in 1903
BOOL (WINAPI *fnIsDarkModeAllowedForApp)() = 0; // ordinal 139

DWORD g_buildNumber = 0;

BOOL g_darkModeSupported = FALSE;
BOOL g_darkModeEnabled = FALSE;
BOOL g_highDPISupported = FALSE;


typedef enum PROCESS_DPI_AWARENESS {
	PROCESS_DPI_UNAWARE,
	PROCESS_SYSTEM_DPI_AWARE,
	PROCESS_PER_MONITOR_DPI_AWARE
} PROCESS_DPI_AWARENESS;

DECLARE_HANDLE(DPI_AWARENESS_CONTEXT);

typedef enum DPI_AWARENESS {
	DPI_AWARENESS_INVALID = -1,
	DPI_AWARENESS_UNAWARE = 0,
	DPI_AWARENESS_SYSTEM_AWARE = 1,
	DPI_AWARENESS_PER_MONITOR_AWARE = 2
} DPI_AWARENESS;

#define DPI_AWARENESS_CONTEXT_UNAWARE               ((DPI_AWARENESS_CONTEXT)-1)
#define DPI_AWARENESS_CONTEXT_SYSTEM_AWARE          ((DPI_AWARENESS_CONTEXT)-2)
#define DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE     ((DPI_AWARENESS_CONTEXT)-3)
#define DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2  ((DPI_AWARENESS_CONTEXT)-4)
#define DPI_AWARENESS_CONTEXT_UNAWARE_GDISCALED     ((DPI_AWARENESS_CONTEXT)-5)



BOOL (WINAPI *fnSetProcessDPIAware)() = 0;
BOOL (WINAPI *fnSetProcessDpiAwareness)(PROCESS_DPI_AWARENESS value) = 0;
BOOL (WINAPI *fnSetProcessDpiAwarenessContext)(DPI_AWARENESS_CONTEXT value) = 0;

int (WINAPI * fnGetSystemMetricsForDpi) (int, UINT) = NULL;

UINT (WINAPI * fnGetDpiForSystem)() = NULL;
UINT (WINAPI * fnGetDpiForWindow)(HWND) = NULL;

HTHEME (WINAPI * fnOpenThemeData)(HWND, LPCWSTR) = NULL;
HRESULT (WINAPI *fnGetThemeSysFont)(HTHEME ,int ,LOGFONTW *) = NULL;


enum IconSize {
	SmallIconSize = 0,
	StartIconSize,
	LargeIconSize,
	ShellIconSize,
	JumboIconSize,
	IconSizesCount
};

int metrics[SM_CMETRICS] = { 0 };

UINT dpiWindow;

HICON icons[IconSizesCount] = { NULL };

HFONT hFont = 0;

DWORD major, minor;



void InitHighDPI() {

	HMODULE hUxtheme = LoadLibraryEx(TEXT("uxtheme.dll"), 0, 0);

	if (hUxtheme)
	{
		fnOpenThemeData = (HTHEME(WINAPI *)(HWND, LPCWSTR)) GetProcAddress(hUxtheme, "OpenThemeData");
#ifdef UNICODE
		fnGetThemeSysFont = (HRESULT(WINAPI*)(HTHEME, int, LOGFONTW *))GetProcAddress(hUxtheme, "GetThemeSysFont");
#endif
	}

	HMODULE gdi = LoadLibraryEx(TEXT("gdi32.dll"), 0, 0);
	if (gdi) {
		fnGetStockObject = (HGDIOBJ(WINAPI *)(int)) GetProcAddress(gdi, "GetStockObject");
#ifdef UNICODE
		fnCreateFontIndirect = (HFONT(WINAPI*)(const LOGFONT *)) GetProcAddress(gdi, "CreateFontIndirectW");
		fnGetObject = (int(WINAPI*)(HANDLE, int, LPVOID pv)) GetProcAddress(gdi, "GetObjectW");
#else
		fnCreateFontIndirect = (HFONT(WINAPI*)(const LOGFONT *)) GetProcAddress(gdi, "CreateFontIndirectA");
		fnGetObject = (int(WINAPI*)(HANDLE, int, LPVOID pv)) GetProcAddress(gdi, "GetObjectA");
#endif
		fnDeleteObject = (BOOL(WINAPI*)(HGDIOBJ)) GetProcAddress(gdi, "DeleteObject");
		fnGetDeviceCaps = (int(WINAPI*)(HDC, int)) GetProcAddress(gdi, "GetDeviceCaps");
	}


	HMODULE user32 = GetModuleHandle(TEXT("user32.dll"));

	fnGetDpiForSystem = (UINT(WINAPI *)()) GetProcAddress(user32, "GetDpiForSystem");
	fnGetDpiForWindow = (UINT(WINAPI *)(HWND)) GetProcAddress(user32, "GetDpiForWindow");
	fnGetSystemMetricsForDpi = (int (WINAPI *) (int, UINT)) GetProcAddress(user32, "GetSystemMetricsForDpi");

	fnSetProcessDpiAwarenessContext = (BOOL(WINAPI *)()) GetProcAddress(user32, "SetProcessDpiAwarenessContext");

	if (fnOpenThemeData &&
		fnGetDeviceCaps &&
		fnGetObject && fnDeleteObject && fnCreateFontIndirect && fnGetStockObject) {
		g_highDPISupported = TRUE;
	}

	if (fnSetProcessDpiAwarenessContext) {
		(*fnSetProcessDpiAwarenessContext)(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
		return;
	}

	fnSetProcessDpiAwareness = (BOOL(WINAPI *)()) GetProcAddress(user32, "SetProcessDpiAwareness");
	if (fnSetProcessDpiAwareness) {
		(*fnSetProcessDpiAwareness)(PROCESS_PER_MONITOR_DPI_AWARE);
		return;
	}

	fnSetProcessDPIAware = (BOOL(WINAPI *)()) GetProcAddress(user32, "SetProcessDPIAware");
	if (fnSetProcessDPIAware) {
		(*fnSetProcessDPIAware)();
		return;
	}
}



UINT GetDPI(HWND hWnd) {
	if (hWnd != NULL) {
		if (fnGetDpiForWindow)
			return (*fnGetDpiForWindow)(hWnd);
	}
	else {
		if (fnGetDpiForSystem)
			return (*fnGetDpiForSystem)();
	}
	HDC hDC;
	if (hDC = GetDC(hWnd)) {
		int dpi = (*fnGetDeviceCaps)(hDC, LOGPIXELSX);
		ReleaseDC(hWnd, hDC);
		return dpi;
	}
	else
		return USER_DEFAULT_SCREEN_DPI;
}

LRESULT RefreshVisualMetrics(UINT dpiSystem) {
	if (fnGetSystemMetricsForDpi) {
		for (int i = 0; i != sizeof metrics / sizeof metrics[0]; ++i) {
			metrics[i] = (*fnGetSystemMetricsForDpi)(i, dpiWindow);
		}
	}
	else {
		for (int i = 0; i != sizeof metrics / sizeof metrics[0]; ++i) {
			metrics[i] = dpiWindow * GetSystemMetrics(i) / dpiSystem;
		}
	}
	return 0;
}

HICON LoadBestIcon(SIZE size) {
	HICON hNewIcon = NULL;
	if (size.cx > 256) size.cx = 256;
	if (size.cy > 256) size.cy = 256;

	return (HICON)LoadImage(hinst, MAKEINTRESOURCE(IDI_APPICON), IMAGE_ICON, size.cx, size.cy, LR_DEFAULTCOLOR);
}


SIZE GetIconMetrics(enum IconSize size, UINT dpiSystem) {
	switch (size) {
	case SmallIconSize:
		return  (SIZE) { metrics[SM_CXSMICON], metrics[SM_CYSMICON] };
	case StartIconSize:
		return (SIZE) {
			(metrics[SM_CXICON] + metrics[SM_CXSMICON]) / 2,
				(metrics[SM_CYICON] + metrics[SM_CYSMICON]) / 2
		};
	case LargeIconSize:
	default:
		return (SIZE) { metrics[SM_CXICON], metrics[SM_CYICON] };

	case ShellIconSize:
	case JumboIconSize:
        // TODO maybe improve
		switch (size) {
		default:
		case ShellIconSize: return (SIZE) { (long)(48 * dpiWindow / dpiSystem), (long)(48 * dpiWindow / dpiSystem) };
		case JumboIconSize: return (SIZE) { (long)(256 * dpiWindow / 96), (long)(256 * dpiWindow / 96) };
		}
	}
}



LRESULT OnVisualEnvironmentChange(HWND hWnd) {
	UINT dpiSystem = GetDPI(NULL);
	RefreshVisualMetrics(dpiSystem);
	HTHEME hTheme = (*fnOpenThemeData)(hWnd, L"TEXTSTYLE");

	LOGFONT lf;

	int height;

#ifdef UNICODE
	if (fnGetThemeSysFont && (*fnGetThemeSysFont)(hTheme, TMT_MSGBOXFONT, &lf) == S_OK) {
#else
	if (FALSE) {
#endif
		lf.lfHeight = MulDiv(lf.lfHeight, dpiWindow, dpiSystem);
		if (lf.lfHeight > 0) {
			height = lf.lfHeight;
		}
		else {
			height = 96 * -lf.lfHeight / 72;
		}
		HFONT hNewFont;
		if (hNewFont = (*fnCreateFontIndirect)(&lf)) {
			if (hFont != NULL) {
				(*fnDeleteObject)(hFont);
			}
			hFont = hNewFont;
		}
		else {
			if (hFont == NULL) {
				hFont = (HFONT)(*fnGetStockObject)(DEFAULT_GUI_FONT);
			}
		}

	}
	else {
		if ((*fnGetObject)((*fnGetStockObject)(DEFAULT_GUI_FONT), sizeof lf, &lf)) {
			lf.lfHeight = MulDiv(lf.lfHeight, dpiWindow, dpiSystem);
		}
	}

	SendMessage(hEditIn, WM_SETFONT, (WPARAM) hFont, 1);

	for (int i = 0u; i != IconSizesCount; ++i) {
		HICON icon;
		if (icon = LoadBestIcon(GetIconMetrics((enum IconSize)i, dpiSystem))) {
			if (icons[i]) {
				DestroyIcon(icons[i]);
			}
			icons[i] = icon;
		}
	}


	SendMessage(hWnd, WM_SETICON, ICON_SMALL, (LPARAM)icons[SmallIconSize]);
	if (fnRtlGetNtVersionNumbers && major >= 10) {
		SendMessage(hWnd, WM_SETICON, ICON_BIG, (LPARAM)icons[StartIconSize]);
	}
	else {
		SendMessage(hWnd, WM_SETICON, ICON_BIG, (LPARAM)icons[LargeIconSize]);
	}

}


LRESULT OnDpiChange(HWND hWnd, WPARAM dpi, const RECT * r) {
	dpiWindow = LOWORD(dpi);

	OnVisualEnvironmentChange(hWnd);
	SetWindowPos(hWnd, NULL, r->left, r->top, r->right - r->left, r->bottom - r->top, 0);
	return 0;
}


void InitVersion() {
	fnRtlGetNtVersionNumbers = (void (WINAPI *)(LPDWORD, LPDWORD, LPDWORD)) GetProcAddress(GetModuleHandle(TEXT("ntdll.dll")), "RtlGetNtVersionNumbers");
	if (fnRtlGetNtVersionNumbers)
	{
		(*fnRtlGetNtVersionNumbers)(&major, &minor, &g_buildNumber);
		g_buildNumber &= ~0xF0000000;
	}
}


void InitDarkMode()
{
	if (fnRtlGetNtVersionNumbers)
	{
		if (major == 10 && ((minor == 0 && g_buildNumber >= 17763) || minor > 0))
		{
			HMODULE hUxtheme = LoadLibraryEx(TEXT("uxtheme.dll"), 0, 0);
			
			if (hUxtheme)
			{
				fnSetWindowTheme = (HRESULT(WINAPI *)(HWND, LPCWSTR, LPCWSTR)) GetProcAddress(hUxtheme, "SetWindowTheme");
				fnRefreshImmersiveColorPolicyState = (void (WINAPI *)()) GetProcAddress(hUxtheme, MAKEINTRESOURCEA(104));
				fnShouldAppsUseDarkMode = (BOOL(WINAPI *)()) GetProcAddress(hUxtheme, MAKEINTRESOURCEA(132));
				fnAllowDarkModeForWindow = (BOOL(WINAPI *)(HWND, BOOL)) GetProcAddress(hUxtheme, MAKEINTRESOURCEA(133));

				void * ord135 = GetProcAddress(hUxtheme, MAKEINTRESOURCEA(135));
				if (g_buildNumber < 18362)
					fnAllowDarkModeForApp = ord135;
				else {
					fnSetPreferredAppMode = ord135;
				}

				fnIsDarkModeAllowedForWindow = (BOOL(WINAPI *)(HWND)) GetProcAddress(hUxtheme, MAKEINTRESOURCEA(137));

				fnSetWindowCompositionAttribute = (BOOL(WINAPI *)(HWND, struct WINDOWCOMPOSITIONATTRIBDATA *)) GetProcAddress(GetModuleHandle(TEXT("user32.dll")), "SetWindowCompositionAttribute");
				HMODULE gdi = LoadLibraryEx(TEXT("gdi32.dll"), 0, 0);
				if (gdi) {
					fnSetTextColor = (COLORREF(WINAPI *)(HDC, COLORREF)) GetProcAddress(gdi, "SetTextColor");
					fnSetBkColor = (COLORREF(WINAPI *)(HDC, COLORREF)) GetProcAddress(gdi, "SetBkColor");
					fnGetStockObject = (HGDIOBJ(WINAPI *)(int)) GetProcAddress(gdi, "GetStockObject");
				}

				if (fnRefreshImmersiveColorPolicyState &&
					fnShouldAppsUseDarkMode &&
					fnAllowDarkModeForWindow &&
					(fnAllowDarkModeForApp || fnSetPreferredAppMode) &&
					fnIsDarkModeAllowedForWindow && fnSetTextColor && fnSetBkColor && fnGetStockObject)
				{
					g_darkModeSupported = TRUE;

					if (fnAllowDarkModeForApp) {
						(*fnAllowDarkModeForApp)(TRUE);
					} else {
						(*fnSetPreferredAppMode)(AllowDark);
					}
					(*fnRefreshImmersiveColorPolicyState)();

					g_darkModeEnabled = (*fnShouldAppsUseDarkMode)();
				}
			}
		}
	}
}

BOOL editDark = FALSE;

void RefreshEditThemeColor(HWND hWnd)
{
	BOOL dark = FALSE;
	BOOL allowed = (*fnIsDarkModeAllowedForWindow)(hWnd) & 0xFF;
	BOOL appsDark = (*fnShouldAppsUseDarkMode)() & 0xFF;
	if (allowed && appsDark)
	{
		if (editDark) {
			return;
		}
		(*fnSetWindowTheme)(hEditIn, L"DarkMode_Explorer", 0);
		editDark = TRUE;
	}
	else {
		(*fnSetWindowTheme)(hEditIn, L"Explorer", 0);
		if (!editDark) {
			return;
		}
		editDark = FALSE;
	}
	InvalidateRect(hWnd, NULL, TRUE);
}

BOOL titleDark = FALSE;

void RefreshTitleBarThemeColor(HWND hWnd)
{
	BOOL dark = FALSE;
	BOOL allowed = (*fnIsDarkModeAllowedForWindow)(hWnd) & 0xFF;
	BOOL appsDark = (*fnShouldAppsUseDarkMode)() & 0xFF;
	if (allowed && appsDark)
	{
		if (titleDark) {
			return;
		}
		titleDark = TRUE;
		dark = TRUE;
	}
	else {
		if (!titleDark) {
			return;
		}
		titleDark = FALSE;
	}
	if (g_buildNumber < 18362)
		SetProp(hWnd, TEXT("UseImmersiveDarkModeColors"), &dark);
	else if (fnSetWindowCompositionAttribute)
	{
		struct WINDOWCOMPOSITIONATTRIBDATA data = { WCA_USEDARKMODECOLORS, &dark, sizeof(dark) };
		(*fnSetWindowCompositionAttribute)(hWnd, &data);
	}

}





int loadFile(TCHAR * path, BYTE ** bufferPointer) {
    FILE *fIn;
    long flength;

    fIn = _tfopen(path, TEXT("rb"));
    if (fIn == NULL)
        return -1;

    if (fseek(fIn, 0, SEEK_END)) {
        fclose(fIn);
        return -1;
    }

    flength = ftell(fIn);
    if (flength == -1) {
        fclose(fIn);
        return -1;
    }

    if (fseek(fIn, 0, SEEK_SET)) {
        fclose(fIn);
        return -1;
    }

    *bufferPointer = (BYTE *) malloc(flength);
    if (!*bufferPointer) {
        fclose(fIn);
        return -1;
    }

    int nread = fread(*bufferPointer, 1, flength, fIn);
    if (nread != flength) {
        fclose(fIn);
        free(*bufferPointer);
        return -1;
    }

    fclose(fIn);

    return flength;
}

void addCA(TCHAR * path) {
    HCERTSTORE caStore = CertOpenSystemStore(0, TEXT("ROOT"));
    BYTE * data;
    long dlength = loadFile(path, &data);
    if (dlength > 0) {
        BOOL res = CertAddEncodedCertificateToStore(
                                                    caStore,
                                                    X509_ASN_ENCODING,
                                                    data,
                                                    dlength,
                                                    CERT_STORE_ADD_NEW,
                                                    NULL
                                                    );
        if (!res) {
            // try PEM
            DWORD dwSkip;
            DWORD dwFlags;
            DWORD dwDataLen = dlength;
            BYTE * bdata = (BYTE *) malloc(dlength);
            TCHAR * tdata;

#ifdef UNICODE
            tdata = (TCHAR *) malloc(dlength * sizeof(TCHAR));
            mbstowcs(tdata, data, dlength);
#else
            tdata = data;
#endif

            if (CryptStringToBinary(tdata,
                                    dlength,
                                    CRYPT_STRING_BASE64HEADER,
                                    bdata,
                                    &dwDataLen,
                                    &dwSkip,
                                    &dwFlags ) ) {
                CertAddEncodedCertificateToStore(
                                                       caStore,
                                                       X509_ASN_ENCODING,
                                                       bdata,
                                                       dwDataLen,
                                                       CERT_STORE_ADD_NEW,
                                                       NULL
                                                       );
            }
#ifdef UNICODE
            free(tdata);
#endif
        }
        free(data);
    }
    CertCloseStore(caStore, 0);
}

void addP12(TCHAR * path) {
    HCERTSTORE myStore = CertOpenSystemStore(0, TEXT("MY"));
    CRYPT_DATA_BLOB blob;
    blob.cbData = loadFile(path, &blob.pbData);
    DWORD cbSize;
    if (blob.cbData > 0) {
        WCHAR * pass = L"changeit";
        HCERTSTORE hPfxStore = PFXImportCertStore(&blob, pass, CRYPT_EXPORTABLE);
        free(blob.pbData);

        PCCERT_CONTEXT pCertContext = NULL;
        while ((pCertContext = CertEnumCertificatesInStore(hPfxStore, pCertContext))) {
            CertAddCertificateContextToStore(
                                                  myStore,
                                                   pCertContext,
                                                  CERT_STORE_ADD_NEW,
                                                  NULL
                                                  );
        }

        CertCloseStore(hPfxStore, 0);

    }
    CertCloseStore(myStore, 0);
}


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
    DWORD dwRead;
    DWORD nextBytes = 0;
    TCHAR previous = 0;

    while (ReadFile(g_hChildStd_OUT_Rd, ((BYTE *) chBuf) + nextBytes, sizeof(chBuf) - sizeof(TCHAR) - nextBytes, &dwRead, NULL)) {
        nextBytes = dwRead % sizeof(TCHAR);
        if (nextBytes != 0) {
            previous = chBuf[dwRead / sizeof(TCHAR)];
        }
        chBuf[dwRead / sizeof(TCHAR)] = 0;

        int ndx = GetWindowTextLength(hEditIn);
        SetFocus(hEditIn);
        SendMessage(hEditIn, EM_SETSEL, ndx, ndx);

        SendMessage(hEditIn, EM_REPLACESEL, 0, (LPARAM) & chBuf[0]);
        chBuf[0] = previous;
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





LRESULT CALLBACK MainWndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
	RECT rc;
	TCHAR cbuf[1024];
	switch (uMsg) {
	case WM_NCCREATE:
		if (g_highDPISupported) {
			dpiWindow = GetDPI(hWnd);
			RefreshVisualMetrics(GetDPI(NULL));
		}
		break;
	case WM_CREATE:
		if (g_darkModeSupported) {
			(*fnAllowDarkModeForWindow)(hWnd, TRUE);
			RefreshTitleBarThemeColor(hWnd);
		}
		return 0;
		break;
	case WM_SIZE:
	case WM_SIZING:
		GetClientRect(hWnd, &rc);
		MoveWindow(hEditIn, rc.left, rc.top, rc.right - rc.left, rc.bottom - rc.top, TRUE);
		break;
		
	case WM_CLOSE:
		if (procState == 5) {
			DestroyWindow(hWnd);
		} else {
			consoleWinShown = FALSE;
			ShowWindow(hWnd, SW_HIDE);
		}
		return 0;

	case WM_DPICHANGED:
		if (g_highDPISupported) {
			return OnDpiChange(hWnd, wParam, (const RECT *)(lParam));
		}
		return 0;

	case WM_SETTINGCHANGE:
		if (g_darkModeSupported) {
			g_darkModeEnabled = (*fnShouldAppsUseDarkMode)();
			RefreshTitleBarThemeColor(hWnd);
			RefreshEditThemeColor(hWnd);
		}
		// no break here
	case WM_THEMECHANGED:
	case WM_DWMCOMPOSITIONCHANGED:
		if (g_highDPISupported) {
			OnVisualEnvironmentChange(hWnd);
		}
		InvalidateRect(hWnd, NULL, TRUE);
		break;

	case WM_CTLCOLORSTATIC: {
		HDC hdcStatic = (HDC)wParam;
		if (g_darkModeEnabled) {
			// TODO maybe extract the color from a Theme
			(*fnSetTextColor)(hdcStatic, RGB(255, 255, 255));
			(*fnSetBkColor)(hdcStatic, RGB(0, 0, 0));
		    return (LRESULT) (*fnGetStockObject)(BLACK_BRUSH);
		}
		break;
	}
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
				PJOBOBJECT_BASIC_PROCESS_ID_LIST procList = (PJOBOBJECT_BASIC_PROCESS_ID_LIST)LocalAlloc(LPTR,
					sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR));
				while (QueryInformationJobObject(hjob, JobObjectBasicProcessIdList, procList, sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR), NULL) == 0) {
					numProcs = procList->NumberOfAssignedProcesses;
					LocalFree(procList);
					procList = (PJOBOBJECT_BASIC_PROCESS_ID_LIST)LocalAlloc(LPTR, sizeof(JOBOBJECT_BASIC_PROCESS_ID_LIST) + numProcs * sizeof(ULONG_PTR));
				}
				EnumWindows((WNDENUMPROC)PostCloseEnum, (LPARAM)procList);

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
			SendMessage(hEditIn, EM_SETSEL, 0, -1);
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
				bufferSizeBytes += recv(csocket, ((char *)&bufferSize) + bufferSizeBytes, 4 - bufferSizeBytes, 0);
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
			TCHAR * command = (TCHAR *)malloc(bufferSize * 2);
			int mbRet = MultiByteToWideChar(CP_UTF8, 0, &buffer[0], bufferSize, command, 1024);
			command[mbRet] = 0;
#else
			TCHAR * command = (TCHAR  *)&buffer[0];
			command[bufferSize] = 0;
#endif

			int webLen = _tcslen(TEXT("Web "));
			int baseLen = _tcslen(TEXT("Config "));
			int caLen = _tcslen(TEXT("CA "));
			int clientP12Len = _tcslen(TEXT("ClientP12 "));
			if (_tcslen(command) > webLen && _tcsncmp(command, TEXT("Web "), webLen) == 0) {
				url = (TCHAR *)malloc((_tcslen(&command[webLen]) + 1) * sizeof(TCHAR));
				_tcscpy(url, &command[webLen]);
				EnableMenuItem(hmenu, IDM_OPEN_WEB, MF_ENABLED);
			}
			else if (_tcslen(command) > baseLen && _tcsncmp(command, TEXT("Config "), baseLen) == 0) {
				base = (TCHAR *)malloc((_tcslen(&command[baseLen]) + 1) * sizeof(TCHAR));
				_tcscpy(base, &command[baseLen]);
				EnableMenuItem(hmenu, IDM_OPEN_BASE, MF_ENABLED);
			}
			else if (_tcscmp(command, TEXT("Starting")) == 0) {
				procState = 1;
			}
			else if (_tcscmp(command, TEXT("Started")) == 0) {
				procState = 2;
			}
			else if (_tcscmp(command, TEXT("Stopping")) == 0) {
				procState = 3;
			}
			else if (_tcscmp(command, TEXT("Stopped")) == 0) {
				procState = 4;
			}
			else if (_tcslen(command) > caLen && _tcsncmp(command, TEXT("CA "), caLen) == 0) {
				addCA(&command[caLen]);
			}
			else if (_tcslen(command) > clientP12Len && _tcsncmp(command, TEXT("ClientP12 "), clientP12Len) == 0) {
				addP12(&command[clientP12Len]);
			}

#ifdef UNICODE
			free(command);
#endif
			return 0;
			}
			break;
		case FD_ACCEPT: {
			csocket = accept(ssocket, 0, 0);
			WSAAsyncSelect(csocket, hWnd, WM_SOCKET, FD_READ);
			closesocket(ssocket);
			}
			break;
		}
	}

	default:
		break;
	}
	return DefWindowProc(hWnd, uMsg, wParam, lParam);
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

	InitVersion();

	InitHighDPI();
	InitDarkMode();

    forceStop = 0;
    GetModuleFileName(NULL, szPath, MAX_PATH);
    GetModuleFileName(NULL, szPathDirectory, MAX_PATH);
    TCHAR * lastSep = _tcsrchr(szPathDirectory, '\\');
    *lastSep = 0;
#ifdef UNICODE
    _sntprintf(szPathBat, MAX_PATH, TEXT("cmd /u /c \"\"%s\\vestige.bat\"\" < nul"), szPathDirectory);
#else
    _sntprintf(szPathBat, MAX_PATH, TEXT("cmd /a /c \"\"%s\\vestige.bat\"\" < nul"), szPathDirectory);
#endif
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

	wc.hIcon = (HICON)LoadImage(hinstance, MAKEINTRESOURCE(IDI_APPICON), IMAGE_ICON, 0, 0, LR_SHARED);
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(1 + COLOR_BTNFACE);
    wc.lpszMenuName = NULL;
    wc.lpszClassName = VESTIGE_CLASSNAME;

    if(!RegisterClass(&wc)) return FALSE;

    hWnd = CreateWindow(wc.lpszClassName, TEXT("Vestige: command line output"), WS_OVERLAPPEDWINDOW,
            CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
            NULL, NULL, hinstance, NULL);

	if (!hWnd) return FALSE;

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
#ifdef UNICODE
    SetEnvironmentVariable(TEXT("VESTIGE_CONSOLE_ENCODING"), TEXT("UTF-16LE"));
#else
    SetEnvironmentVariable(TEXT("VESTIGE_CONSOLE_ENCODING"), TEXT("US-ASCII"));
#endif

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
            WS_CHILD|WS_VISIBLE |  ES_MULTILINE  |
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

	if (!hEditIn) return FALSE;

	if (g_darkModeSupported) {
		RefreshEditThemeColor(hWnd);
	}

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

	if (g_highDPISupported) {
		OnVisualEnvironmentChange(hWnd);
	}

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

