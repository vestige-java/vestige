
DESTDIR=/
PREFIX=/usr
bindir=$(PREFIX)/bin
datadir=$(PREFIX)/share

build:
	find . -name '._*' -delete

install:
	mkdir -p $(DESTDIR)/$(bindir)
	mkdir -p $(DESTDIR)/$(datadir)/vestige
	mkdir -p $(DESTDIR)/$(datadir)/applications
	mkdir -p $(DESTDIR)/$(datadir)/icons
	mkdir -p $(DESTDIR)/etc/vestige
	cp vestige $(DESTDIR)/$(datadir)/vestige/
	cp jvm_enhancer.properties $(DESTDIR)/$(datadir)/vestige/
	cp -r m2 $(DESTDIR)/$(datadir)/vestige/
	cp unix-classpath.txt $(DESTDIR)/$(datadir)/vestige/
	cp vestige.desktop $(DESTDIR)/$(datadir)/applications/
	cp vestige_gtk_launcher.py $(DESTDIR)/$(bindir)/vestige
	cp -r hicolor $(DESTDIR)/$(datadir)/icons/
	cp -r lib $(DESTDIR)/$(datadir)/vestige
	cp -r modulepatch $(DESTDIR)/$(datadir)/vestige
	cp -r repository $(DESTDIR)/$(datadir)/vestige
	cp -r . $(DESTDIR)/etc/vestige/
	$(RM) -r $(DESTDIR)/etc/vestige/jvm_enhancer.properties $(DESTDIR)/etc/vestige/README $(DESTDIR)/etc/vestige/m2 $(DESTDIR)/etc/vestige/unix-classpath.txt $(DESTDIR)/etc/vestige/Makefile $(DESTDIR)/etc/vestige/vestige $(DESTDIR)/etc/vestige/vestige.desktop $(DESTDIR)/etc/vestige/vestige_gtk_launcher.py $(DESTDIR)/etc/vestige/lib $(DESTDIR)/etc/vestige/modulepatch $(DESTDIR)/etc/vestige/repository $(DESTDIR)/etc/vestige/hicolor

uninstall:
	$(RM) $(DESTDIR)/$(bindir)/vestige
	$(RM) -r $(DESTDIR)/etc/vestige
	$(RM) -r $(DESTDIR)/$(datadir)/vestige
	