
DESTDIR=/
PREFIX=/usr
bindir=$(PREFIX)/bin
datadir=$(PREFIX)/share

build:
	find . -name '._*' -delete

install:
	mkdir -p $(DESTDIR)/$(datadir)/vestige
	mkdir -p $(DESTDIR)/etc/vestige
	cp vestige $(DESTDIR)/$(datadir)/vestige/
	cp vestigessh $(DESTDIR)/$(datadir)/vestige/
	cp vestigeweb $(DESTDIR)/$(datadir)/vestige/
	cp jvm_enhancer.properties $(DESTDIR)/$(datadir)/vestige/
	cp -r m2 $(DESTDIR)/$(datadir)/vestige/
	cp unix-classpath.txt $(DESTDIR)/$(datadir)/vestige/
	cp -r lib $(DESTDIR)/$(datadir)/vestige
	cp -r repository $(DESTDIR)/$(datadir)/vestige
	cp -r . $(DESTDIR)/etc/vestige/
	$(RM) -r $(DESTDIR)/etc/vestige/jvm_enhancer.properties $(DESTDIR)/etc/vestige/README $(DESTDIR)/etc/vestige/m2 $(DESTDIR)/etc/vestige/unix-classpath.txt $(DESTDIR)/etc/vestige/Makefile $(DESTDIR)/etc/vestige/vestige $(DESTDIR)/etc/vestige/vestigessh $(DESTDIR)/etc/vestige/vestigeweb $(DESTDIR)/etc/vestige/lib $(DESTDIR)/etc/vestige/repository

uninstall:
	$(RM) $(DESTDIR)/$(bindir)/vestige
	$(RM) -r $(DESTDIR)/etc/vestige
	$(RM) -r $(DESTDIR)/$(datadir)/vestige
	