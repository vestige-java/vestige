
VESTIGE_VERSION=1.0.0

Vestige.pkg:
	mkdir -p target/vestige_deb/DEBIAN
	mkdir -p target/vestige_deb/etc/vestige
	mkdir -p target/vestige_deb/usr/bin
	mkdir -p target/vestige_deb/usr/share/icons
	mkdir -p target/vestige_deb/usr/share/applications
	unzip -d target/vestige ../standard_edition_standalone/target/vestige.assemblies.standard_edition_standalone-$(VESTIGE_VERSION)-unix.zip
	sed -i~ -e 's/VESTIGE_BASE=.*/VESTIGE_BASE="$$HOME\/.vestige"/' -e 's/LIBDIR=.*/LIBDIR=\/usr\/share\/vestige/' -e 's/CONFDIR=.*/CONFDIR=\/etc\/vestige\//' target/vestige/vestige
	rm target/vestige/vestige~
	mv target/vestige/lib target/vestige_deb/usr/share/vestige
	mv target/vestige/vestige target/vestige_deb/usr/bin/vestige
	mv target/vestige/* target/vestige_deb/etc/vestige/
	sed -e 's/Version: .*/Version: $(VESTIGE_VERSION)/' src/control > target/vestige_deb/DEBIAN/control
	cp src/conffiles target/vestige_deb/DEBIAN/
	cp src/vestige.desktop target/vestige_deb/usr/share/applications/
	cp src/vestige_gtk_launcher.py target/vestige_deb/usr/bin/vestige_gtk_launcher.py
	cp -r src/hicolor target/vestige_deb/usr/share/icons/
	chmod +x target/vestige_deb/usr/bin/*
	-find target \( -name .svn -o -name .DS_Store \) -exec rm -rf {} \;
	-ssh debbuilder rm -rf '~/target'
	scp -r target debbuilder:.
	ssh debbuilder dpkg -b target/vestige_deb/ target/vestige_$(VESTIGE_VERSION)_all.deb
	scp debbuilder:target/vestige_$(VESTIGE_VERSION)_all.deb target/vestige_$(VESTIGE_VERSION)_all.deb 

clean:
	$(RM) -r target
