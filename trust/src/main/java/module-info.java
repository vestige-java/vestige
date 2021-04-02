module fr.gaellalire.vestige.trust {

    requires slf4j.api;

    requires org.bouncycastle.pg;

    requires org.bouncycastle.provider;

    requires fr.gaellalire.vestige.spi.trust;
    
    requires fr.gaellalire.vestige.spi.system;

    exports fr.gaellalire.vestige.trust;

    exports fr.gaellalire.vestige.trust.secure;

}
