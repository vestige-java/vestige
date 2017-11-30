module fr.gaellalire.vestige.edition.standard {

    requires slf4j.api;

    requires fr.gaellalire.vestige.admin.ssh;

    requires fr.gaellalire.vestige.admin.web;

    requires fr.gaellalire.vestige.system;

    requires fr.gaellalire.vestige.admin.command;

    requires fr.gaellalire.vestige.application.manager;

    requires fr.gaellalire.vestige.core;

    requires fr.gaellalire.vestige.jpms;

    requires fr.gaellalire.vestige.platform;

    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.utils;

    requires fr.gaellalire.vestige.resolver.maven;

    requires fr.gaellalire.vestige.resolver.url_list;

    requires fr.gaellalire.vestige.application.descriptor.xml;

    requires java.xml.bind;
    
    requires java.xml;

    requires java.management;

    // Shame part : filename-based automodules

    requires logback.classic;

    requires logback.core;

    requires jetty.server;

    requires jetty.servlet;

    requires httpclient;

    requires httpcore;

    // End of shame part

    exports fr.gaellalire.vestige.edition.standard;

    opens fr.gaellalire.vestige.edition.standard.schema to java.xml.bind;
}
