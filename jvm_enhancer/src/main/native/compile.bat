gcc -Wl,--add-stdcall-alias -I"C:\Program Files\Java\jdk1.8.0_73\include" -I"C:\Program Files\Java\jdk1.8.0_73\include\win32" WindowsShutdownHook.c -m32 -shared -o WindowsShutdownHook_w32.dll
gcc -Wl,--add-stdcall-alias -I"C:\Program Files\Java\jdk1.8.0_73\include" -I"C:\Program Files\Java\jdk1.8.0_73\include\win32" WindowsShutdownHook.c -m64 -shared -o WindowsShutdownHook_amd64.dll
