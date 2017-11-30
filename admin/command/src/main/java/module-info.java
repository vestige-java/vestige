module fr.gaellalire.vestige.admin.command {
    
    requires slf4j.api;

    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.application.manager;
    
    requires fr.gaellalire.vestige.spi.resolver;

    requires static fr.gaellalire.vestige.platform;

    exports fr.gaellalire.vestige.admin.command;

    exports fr.gaellalire.vestige.admin.command.argument;
}
