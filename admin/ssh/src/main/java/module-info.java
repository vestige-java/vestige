module fr.gaellalire.vestige.admin.ssh {
    
    requires slf4j.api;

    requires fr.gaellalire.vestige.job;

    requires fr.gaellalire.vestige.admin.command;

    // Shame part : filename-based automodules

    requires jline;

    requires sshd.core;

    requires mina.core;

    // End of shame part

    exports fr.gaellalire.vestige.admin.ssh;
    
}
