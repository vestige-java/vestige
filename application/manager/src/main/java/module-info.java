module fr.gaellalire.vestige.application.manager {
        
    requires slf4j.api;
    
    requires fr.gaellalire.vestige.utils;

    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.system;

    requires fr.gaellalire.vestige.spi.resolver;

    exports fr.gaellalire.vestige.application.manager;

}
