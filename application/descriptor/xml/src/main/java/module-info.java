module fr.gaellalire.vestige.application.descriptor.xml {
        
    requires slf4j.api;
    
    requires org.apache.maven.resolver;

    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.platform;

    requires fr.gaellalire.vestige.application.manager;

    requires fr.gaellalire.vestige.resolver.maven;

    requires fr.gaellalire.vestige.utils;

    requires java.xml.bind;

    exports fr.gaellalire.vestige.application.descriptor.xml;

}
