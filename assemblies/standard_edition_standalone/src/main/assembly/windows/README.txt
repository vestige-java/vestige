---
Vestige is a java application manager.
You can control lifecycle (install / start / stop / uninstall) of applications from vestige console.
Moreover if the application support it you can migrate from a version to another, and
this migration can be automatically performed with automigrate-level.

---
PROXY

Vestige try to use system proxy settings.
If it does not work it tries to use %VESTIGE_CONFIG%\m2\settings.xml
If the file does not exist then it uses %HOME%\.m2\settings.xml

---
STARTING

Double-click on vestige.exe to start vestige.
Right click on status icon and click on open web administration.

---
SSH

Vestige console is any SSH client.
On windows you can use PuTTY (http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html)
You have to generate an rsa key pair (public / private).
Put the public key in ssh\authorized_keys and configure PuTTY to use the private key and port defined in %VESTIGE_CONFIG%\settings.xml (default to 0 meaning dynamically allocated port whose value will be in %VESTIGE_DATA%\ssh\port.txt) and admin username.
Do not distribute the private key and do not add unknown public key in ssh\authorized_keys because an access to vestige console allow to install any application on your computer (including virus).

---
STOPPING

Right click on status icon, click on quit

---
BATCH file

You can open a cmd and run vestige.bat instead of vestige.exe
To have correct encoding you can either use UTF-8
  > chcp 65001
  > set VESTIGE_CONSOLE_ENCODING=UTF-8
or use your default encoding (example with Cp1252)
  > chcp 1252
  > set VESTIGE_CONSOLE_ENCODING=Cp1252
