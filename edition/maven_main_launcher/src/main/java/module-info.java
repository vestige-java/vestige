module fr.gaellalire.vestige.edition.maven_main_launcher {
    
    requires fr.gaellalire.vestige.resolver.maven;

    requires fr.gaellalire.vestige.platform;

    requires fr.gaellalire.vestige.core;

    requires fr.gaellalire.vestige.jpms;

    requires fr.gaellalire.vestige.utils;

    requires slf4j.api;

    requires static java.desktop;

    requires java.xml.bind;

    requires org.bouncycastle.provider;
    
    requires org.bouncycastle.tls;
    
    requires org.bouncycastle.pkix;

    exports fr.gaellalire.vestige.edition.maven_main_launcher;
    
    opens fr.gaellalire.vestige.edition.maven_main_launcher.schema to java.xml.bind;

}
