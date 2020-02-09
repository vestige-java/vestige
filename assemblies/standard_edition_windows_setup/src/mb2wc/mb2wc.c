#define _WIN32_WINNT _WIN32_WINNT_WINXP

#include <TCHAR.H>
#include <windows.h>

#include <stdio.h>

#define BUFFER_SIZE 1024

int missingCR(WCHAR * wchBuf, int len, int * pcrRead) {
    for (int i=0; i < len; i++) {
        WCHAR c = wchBuf[i];
        if (c == L'\r') {
            *pcrRead = 1;
        } else if (c == L'\n') {
            if (*pcrRead == 0) {
                *pcrRead = 0;
                return i;
            }
            *pcrRead = 0;
        } else {
            *pcrRead = 0;
        }
    }
    return -1;
}

void fullWrite(HANDLE h, BYTE * buff, int length) {
    DWORD nOut = 0;
    while (length != 0) {
        if (!WriteFile(h, buff, length, &nOut, NULL)) {
            ExitProcess(0);
        }
        length -= nOut;
        buff += nOut;
    }
    FlushFileBuffers(h);
}

int main(int argc, char *argv[]) {
    HANDLE hIn = GetStdHandle(STD_INPUT_HANDLE);
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    
    int codePage = CP_ACP;
    
    if (argc > 1) {
        codePage = atoi(argv[1]);
    } else {
        LANGID langID;
        LCID localeID;
        TCHAR strCodePage[7];
        
        langID = LANGIDFROMLCID(GetUserDefaultLCID());
        localeID = MAKELCID(langID, SORT_DEFAULT);
        if (GetLocaleInfo(localeID, LOCALE_IDEFAULTANSICODEPAGE,
                          strCodePage, sizeof(strCodePage)/sizeof(TCHAR)) > 0 ) {
            codePage = atoi(strCodePage);
        }
    }
    
    BYTE chBuf[BUFFER_SIZE];
    // worst case 1 byte -> 2 char (4 bytes) UTF-16
    WCHAR wchBuf[BUFFER_SIZE * 2];
    DWORD dwRead;
    int crRead = 0;
    WCHAR * rnBuffer = L"\r\n";
    
    int alreadyRead = 0;
    
    while (ReadFile(hIn, chBuf + alreadyRead, sizeof(chBuf) - alreadyRead, &dwRead, NULL)) {
        int mbRet = MultiByteToWideChar(codePage, MB_ERR_INVALID_CHARS, &chBuf[0], dwRead + alreadyRead, &wchBuf[0], BUFFER_SIZE * 2);
        if (mbRet != 0) {
            alreadyRead = 0;
        } else if (GetLastError() != ERROR_NO_UNICODE_TRANSLATION) {
            ExitProcess(1);
        } else do {
            for (int i = 0; i < 4; i++) {
                if (dwRead - i <= 0) {
                    break;
                }
                mbRet = MultiByteToWideChar(codePage, MB_ERR_INVALID_CHARS, &chBuf[0], dwRead + alreadyRead - i, &wchBuf[0], BUFFER_SIZE * 2);
                if (mbRet != 0) {
                    for (int j = 0; j < i; j++) {
                        chBuf[j] = chBuf[dwRead - i + j];
                    }
                    alreadyRead = i;
                    break;
                }
            }
            
            if (mbRet == 0) {
                if (alreadyRead + dwRead < 4) {
                    // wait for more
                    alreadyRead += dwRead;
                    if (ReadFile(hIn, chBuf + alreadyRead, sizeof(chBuf) - alreadyRead, &dwRead, NULL)) {
                        mbRet = MultiByteToWideChar(codePage, MB_ERR_INVALID_CHARS, &chBuf[0], dwRead + alreadyRead, &wchBuf[0], BUFFER_SIZE * 2);
                        if (mbRet != 0) {
                            alreadyRead = 0;
                        }
                    } else {
                        // invalid char : don't care
                        mbRet = MultiByteToWideChar(codePage, 0, &chBuf[0], dwRead + alreadyRead, &wchBuf[0], BUFFER_SIZE * 2);
                        alreadyRead = 0;
                    }
                } else {
                    // invalid char : don't care
                    mbRet = MultiByteToWideChar(codePage, 0, &chBuf[0], dwRead + alreadyRead, &wchBuf[0], BUFFER_SIZE * 2);
                    alreadyRead = 0;
                }
            }
            
        } while (mbRet == 0);
        
        DWORD nOut;
        WCHAR * currentWchBuf = wchBuf;
        int pos = missingCR(currentWchBuf, mbRet, &crRead);
        while (pos != -1) {
            if (pos == 0) {
                fullWrite(hOut, (BYTE *) rnBuffer, 2 * sizeof(WCHAR));
                mbRet--;
                currentWchBuf++;
            } else {
                fullWrite(hOut, (BYTE *) currentWchBuf, pos * sizeof(WCHAR));
                fullWrite(hOut, (BYTE *) rnBuffer, 2 * sizeof(WCHAR));
                mbRet -= pos + 1;
                currentWchBuf += pos + 1;
            }
            pos = missingCR(currentWchBuf, mbRet, &crRead);
        }
        
        fullWrite(hOut, (BYTE *) currentWchBuf, mbRet * sizeof(WCHAR));
    }
    
    ExitProcess(0);
    return 0;
}
