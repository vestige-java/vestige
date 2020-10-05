Vestige should be compiled with an Oracle JDK 7.

The JDK 7 should manage TLSv1.2
In $JAVA_HOME/jre/lib/ext copy :
- bcprov-jdk15to18-1.66.jar
- bctls-jdk15to18-1.66.jar

Edit $JAVA_HOME/jre/lib/security/java.security, add following
security.provider.1=org.bouncycastle.jce.provider.BouncyCastleProvider
security.provider.2=org.bouncycastle.jsse.provider.BouncyCastleJsseProvider

and increment by 2 other security.provider.N

export JAVA_TOOL_OPTIONS='-Dhttps.protocols=TLSv1.2 -Djavax.net.ssl.trustStore=/path/to/recent/cacerts'

Launch vestige compilation with :
$ mvn clean install




When modifying module-info.java you should use an Oracle JDK 9

Launch java 9 compilation with :
$ mvn clean install -Pjava9

After this operation maven main launcher module-info.class should be modified to have a main-class :

$ pushd edition/maven_main_launcher; jar --main-class fr.gaellalire.vestige.edition.maven_main_launcher.JPMSMavenMainLauncher --update --file target/vestige.edition.maven_main_launcher*.jar; pushd src/main/resources/ && jar xf ../../../target/vestige.edition.maven_main_launcher*.jar module-info.class; popd; popd




You will need gnu-sed to create assemblies.

On Mac OS X you can install it with brew and modify your PATH :
$ brew install gnu-sed

If your editor is Eclipse, XSD validation may fail due to cache
$ rm -rf .lemminx/cache/

