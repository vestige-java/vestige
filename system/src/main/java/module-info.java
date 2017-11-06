module fr.gaellalire.vestige.system {

    requires slf4j.api;

    requires fr.gaellalire.vestige.core;

    requires java.logging;

    requires java.sql;

    exports fr.gaellalire.vestige.system;
    
    exports fr.gaellalire.vestige.system.logger;

    exports fr.gaellalire.vestige.system.interceptor;

}
