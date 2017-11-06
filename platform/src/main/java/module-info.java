module fr.gaellalire.vestige.platform {

    exports fr.gaellalire.vestige.platform;
    
    requires fr.gaellalire.vestige.core;

    requires fr.gaellalire.vestige.jpms;

    requires slf4j.api;
    
    // needed to migrate from one classloader to another with reflection
    opens fr.gaellalire.vestige.platform;
}
