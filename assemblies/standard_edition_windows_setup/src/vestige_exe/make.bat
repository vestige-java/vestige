windres resources.rc resources.o 
gcc resources.o vestige.c -Wl,--subsystem,windows -municode -lwsock32 -o vestige.exe
