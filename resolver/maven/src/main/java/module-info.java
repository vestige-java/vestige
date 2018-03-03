module fr.gaellalire.vestige.resolver.maven {

    requires org.apache.maven.resolver;

    requires fr.gaellalire.vestige.platform;

    requires fr.gaellalire.vestige.core;

    requires fr.gaellalire.vestige.job;

    requires org.apache.maven.resolver.impl;

    requires org.apache.maven.resolver.transport.file;

    requires org.apache.maven.resolver.transport.http;

    requires org.apache.maven.resolver.connector.basic;

    requires org.apache.maven.resolver.spi;

    requires org.apache.maven.resolver.util;

    requires fr.gaellalire.vestige.maven;

    requires fr.gaellalire.vestige.jpms;

    requires transitive fr.gaellalire.vestige.spi.system;

    requires slf4j.api;

    requires httpclient;

    requires fr.gaellalire.vestige.resolver.common;

    requires transitive fr.gaellalire.vestige.spi.resolver.maven;

    requires transitive fr.gaellalire.vestige.system;

    exports fr.gaellalire.vestige.resolver.maven;

    exports fr.gaellalire.vestige.resolver.maven.secure;
}
