---
Vestige is a java application manager.
You can control lifecycle (install / start / stop / uninstall) of applications from vestige console.
Moreover if the application support it you can migrate from a version to another, and
this migration can be automatically performed with automigrate-level.

---
PROXY

Vestige try to use system proxy settings.
If it does not work it tries to use %VESTIGE_BASE%\m2\settings.xml
If the file does not exist then it uses %HOME%\.m2\settings.xml

---
STARTING

Double-click on vestige.bat to start vestige.
Once started you can access to vestige console.

---
SSH

Vestige console is any SSH client.
On windows you can use PuTTY (http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html)
You have to generate an rsa key pair (public / private).
Put the public key in ssh\authorized_keys and configure PuTTY to use the private key and port 8422 (instead of 22) and admin username.
Do not distribute the private key and do not add unknown public key in ssh\authorized_keys because an access to vestige console allow to install any application on your computer (including virus).

---
STOPPING

CTRL-C inside bat window
Avoid closing the window opened with bat which may prevents vestige to shutdown properly.
