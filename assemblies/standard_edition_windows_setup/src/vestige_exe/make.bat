windres resources.rc resources.o 
gcc -m32 resources.o vestige.c -Wl,--subsystem,windows -municode -lwsock32 -lcrypt32 -o vestige-x86.exe
