To run ./build you must be on OS X

to add env variable like 

launchctl setenv VESTIGE_DEBUG 1

launchctl unsetenv VESTIGE_DEBUG

if you want persistent (after a reboot) modification edit file

/etc/launchd.conf
setenv VESTIGE_OPTS -Xmx4096m

Starting from OSX 10.10 (Yosemite) file /etc/launchd.conf is not read anymore you can restore the behavior with the following commands

sudo tee /usr/local/bin/launchd.conf.sh > /dev/null << EOF
echo '#!/bin/sh

while read line || [[ -n "$line" ]] ; do [[ -n "$line" ]] && eval launchctl $line; done < /etc/launchd.conf;
EOF

sudo chmod +x /usr/local/bin/launchd.conf.sh

sudo tee /Library/LaunchAgents/launchd.conf.plist > /dev/null << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>launchd.conf</string>
  <key>ProgramArguments</key>
  <array>
    <string>sh</string>
    <string>-c</string>
    <string>/usr/local/bin/launchd.conf.sh</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
</dict>
</plist>
EOF

launchctl load -w  /Library/LaunchAgents/launchd.conf.plist
