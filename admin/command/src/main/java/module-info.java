module fr.gaellalire.vestige.admin.command {
    
    requires slf4j.api;

    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.platform;

    requires fr.gaellalire.vestige.application.manager;

    exports fr.gaellalire.vestige.admin.command;

    exports fr.gaellalire.vestige.admin.command.argument;
}
