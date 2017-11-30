module fr.gaellalire.vestige.application.descriptor.xml {
        
    requires slf4j.api;
    
    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.application.manager;

    requires fr.gaellalire.vestige.spi.resolver.maven;

    requires fr.gaellalire.vestige.spi.resolver.url_list;

    requires fr.gaellalire.vestige.utils;
    
    requires java.xml;
    
    requires java.xml.bind;

    exports fr.gaellalire.vestige.application.descriptor.xml;

    opens fr.gaellalire.vestige.application.descriptor.xml.schema.application to java.xml.bind;

    opens fr.gaellalire.vestige.application.descriptor.xml.schema.repository to java.xml.bind;
}
