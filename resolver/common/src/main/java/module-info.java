module fr.gaellalire.vestige.resolver.common {
        
    requires transitive fr.gaellalire.vestige.platform;

    requires transitive fr.gaellalire.vestige.spi.system;

    requires transitive fr.gaellalire.vestige.spi.resolver;

    requires transitive fr.gaellalire.vestige.core;
    
    requires fr.gaellalire.vestige.jpms;

    exports fr.gaellalire.vestige.resolver.common;

    exports fr.gaellalire.vestige.resolver.common.secure;
}
