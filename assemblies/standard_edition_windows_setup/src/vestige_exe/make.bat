windres resources.rc resources.o 
gcc resources.o vestige.c -Wl,--subsystem,windows -o vestige.exe