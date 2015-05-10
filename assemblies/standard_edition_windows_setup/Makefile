
VESTIGE_VERSION=1.0.0

vestige-setup:
	mkdir -p target/vestige
	unzip -d target/vestige ../standard_edition_standalone/target/vestige.assemblies.standard_edition_standalone-$(VESTIGE_VERSION)-windows.zip
	sed -i~ -e 's/VESTIGE_BASE=.*/VESTIGE_BASE=%userprofile%\\Vestige/' -e 's/VESTIGE_DATA=.*/VESTIGE_DATA=%appdata%\\Vestige/' target/vestige/vestige.bat
	rm target/vestige/vestige.bat~
	sed -e 's/MyAppVersion .*/MyAppVersion "$(VESTIGE_VERSION)"/' src/vestige.iss > target/vestige.iss
	cp src/CheckJava.class target
	cp src/checkJava.bat target
	-find target \( -name .svn -o -name .DS_Store \) -exec rm -rf {} \;
	-ssh winbuilder 'rm -Rf /cygdrive/c/target'
	scp -r target/ winbuilder:/cygdrive/c
	ssh winbuilder '"/cygdrive/c/Program Files/Inno Setup 5/ISCC.exe" "C:\target\vestige.iss"'
	scp winbuilder:/cygdrive/c/target/Output/vestige-setup.exe target/vestige-$(VESTIGE_VERSION)-setup.exe
clean:
	$(RM) -r target
