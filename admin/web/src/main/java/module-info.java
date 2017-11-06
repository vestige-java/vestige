module fr.gaellalire.vestige.admin.web {
    
    requires slf4j.api;

    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.admin.command;

    requires fr.gaellalire.vestige.application.manager;

    // Shame part : filename-based automodules

    requires json.smart;

    requires javax.servlet;

    requires jetty.websocket;

    // End of shame part

    exports fr.gaellalire.vestige.admin.web;
    
}
