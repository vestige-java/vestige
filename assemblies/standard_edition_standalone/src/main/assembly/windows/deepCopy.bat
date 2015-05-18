@echo off
for /f "tokens=*" %%f in ('dir /A:-D /B "%~1"') do (
  copy /Y "%~1\%%f" "%~2\%%f" > nul
) 2> nul
for /f "tokens=*" %%d in ('dir /A:D /B "%~1"') do (
  md "%~2\%%d" 2> nul
  call %0 "%~1\%%d" "%~2\%%d"
) 2> nul
