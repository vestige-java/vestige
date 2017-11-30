module fr.gaellalire.vestige.system {

    requires slf4j.api;

    requires fr.gaellalire.vestige.core;

    requires static java.logging;

    requires static java.sql;
    
    requires transitive fr.gaellalire.vestige.spi.system;

    exports fr.gaellalire.vestige.system;
    
    exports fr.gaellalire.vestige.system.logger;

    exports fr.gaellalire.vestige.system.interceptor;

}
