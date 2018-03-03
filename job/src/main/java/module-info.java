module fr.gaellalire.vestige.job {
    
    requires transitive fr.gaellalire.vestige.spi.job;

    requires transitive fr.gaellalire.vestige.spi.system;

    exports fr.gaellalire.vestige.job;
    
    exports fr.gaellalire.vestige.job.secure;
}
