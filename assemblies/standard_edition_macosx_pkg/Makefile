
Vestige.pkg:
	mkdir target
	cp -r src/root target/
	unzip -d target/root/opt/vestige ../standalone/target/vestige.distrib.standalone-*-unix.zip
	cp ../../../vestige.jvm_enhancer/target/vestige.jvm_enhancer-*-jar-with-dependencies.jar target/root/opt/vestige/lib
	sed -i~ 's/jar-with-dependencies.jar"/jar-with-dependencies.jar:$$DIRNAME\/lib\/vestige.jvm_enhancer-0.0.1-SNAPSHOT-jar-with-dependencies.jar" com.googlecode.vestige.jvm_enhancer.JVMEnhancer/' target/root/opt/vestige/vestige
	rm target/root/opt/vestige/vestige~
	mv target/root/opt/vestige/m2/vestige-se.xml target/root/opt/vestige/m2/vestige-se.tmp
	cat target/root/opt/vestige/m2/vestige-se.tmp | sed '1q' >> target/root/opt/vestige/m2/vestige-se.xml
	cat src/vestige-se.snippet >> target/root/opt/vestige/m2/vestige-se.xml
	cat target/root/opt/vestige/m2/vestige-se.tmp | sed '2,$$!d' >> target/root/opt/vestige/m2/vestige-se.xml
	rm target/root/opt/vestige/m2/vestige-se.tmp
#	/Developer/usr/bin/packagemaker --doc src/Vestige.pmdoc --out target/Vestige.pkg 

clean:
	$(RM) -r target
