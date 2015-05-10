
VESTIGE_VERSION=1.0.0

Vestige.pkg:
	mkdir target
	bunzip2 -kc src/wc.dmg.bz2 > target/wc.dmg
	hdiutil attach target/wc.dmg -noautoopen -quiet -mountpoint target/wc
	cp -r src/Vestige.app target/wc/
	unzip -d target/wc/Vestige.app/Contents/Resources/vestige_home ../standard_edition_standalone/target/vestige.assemblies.standard_edition_standalone-$(VESTIGE_VERSION)-unix.zip
	sed -i~ -e 's/-Djava.util.logging.manager/-Dapple.awt.UIElement=true -Djava.util.logging.manager/' -e 's/VESTIGE_BASE=.*/VESTIGE_BASE="$$HOME\/Vestige"/' -e 's/VESTIGE_DATA=.*/VESTIGE_DATA="$$HOME\/Library\/Application Support\/Vestige"/' target/wc/Vestige.app/Contents/Resources/vestige_home/vestige
	rm target/wc/Vestige.app/Contents/Resources/vestige_home/vestige~
	-find target -name .svn -exec rm -rf {} \;
	hdiutil detach `hdiutil info | grep "target/wc"  | grep "Apple_HFS" | cut -d$$'\t' -f 1` -quiet -force
	hdiutil convert target/wc.dmg -quiet -format UDZO -imagekey zlib-level=9 -o target/vestige-$(VESTIGE_VERSION).dmg
	
test:
	unzip -u -o -d /Applications/Vestige.app/Contents/Resources/vestige_home ../standard_edition_standalone/target/vestige.assemblies.standard_edition_standalone-$(VESTIGE_VERSION)-unix.zip
	sed -i~ -e 's/-Djava.util.logging.manager/-Dapple.awt.UIElement=true -Djava.util.logging.manager/' -e 's/VESTIGE_BASE=.*/VESTIGE_BASE="$$HOME\/Library\/Application Support\/Vestige"/' /Applications/Vestige.app/Contents/Resources/vestige_home/vestige
	rm /Applications/Vestige.app/Contents/Resources/vestige_home/vestige~

clean:
	$(RM) -r target
