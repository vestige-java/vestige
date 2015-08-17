windres resources.rc resources.o 
gcc resources.o vestige.c -Wl,--subsystem,windows -lwsock32 -o vestige.exe