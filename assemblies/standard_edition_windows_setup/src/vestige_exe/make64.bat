windres resources.rc resources.o 
gcc -m64 resources.o vestige.c -Wl,--subsystem,windows -municode -lwsock32 -lcrypt32 -o vestige-amd64.exe
