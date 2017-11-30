module fr.gaellalire.vestige.resolver.url_list {
   
    requires fr.gaellalire.vestige.resolver.common;

    requires fr.gaellalire.vestige.spi.job;

    requires transitive fr.gaellalire.vestige.spi.resolver.url_list;

    exports fr.gaellalire.vestige.resolver.url_list;

}
