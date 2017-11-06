module fr.gaellalire.vestige.resolver.maven {
    
    requires org.apache.maven.resolver;
    
    requires fr.gaellalire.vestige.platform;

    requires fr.gaellalire.vestige.core;
    
    requires fr.gaellalire.vestige.job;
    
    requires fr.gaellalire.vestige.utils;
    
    requires org.apache.maven.resolver.impl;
    
    requires org.apache.maven.resolver.transport.file;

    requires org.apache.maven.resolver.transport.http;
    
    requires org.apache.maven.resolver.connector.basic;

    requires org.apache.maven.resolver.spi;

    requires org.apache.maven.resolver.util;

    requires fr.gaellalire.vestige.maven;

    requires fr.gaellalire.vestige.jpms;

    requires java.desktop;

    requires java.xml.bind;
    
    requires slf4j.api;
    
    exports fr.gaellalire.vestige.resolver.maven;

    opens fr.gaellalire.vestige.resolver.maven.schema to java.xml.bind;

}
