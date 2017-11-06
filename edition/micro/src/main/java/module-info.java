module fr.gaellalire.vestige.edition.micro {
        
    requires slf4j.api;

    requires fr.gaellalire.vestige.admin.telnet;

    requires fr.gaellalire.vestige.system;
    
    requires fr.gaellalire.vestige.admin.command;
    
    requires fr.gaellalire.vestige.application.manager;
    
    requires fr.gaellalire.vestige.core;
    
    requires fr.gaellalire.vestige.platform;
    
    requires fr.gaellalire.vestige.application.descriptor.properties;

    exports fr.gaellalire.vestige.edition.micro;

}
