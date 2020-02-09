/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.gaellalire.vestige.edition.single_application_launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.ProxySelector;
import java.net.URL;
import java.security.AllPermission;
import java.security.KeyStore;
import java.security.PermissionCollection;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import fr.gaellalire.vestige.application.descriptor.xml.XMLApplicationRepositoryManager;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationRepositoryManager;
import fr.gaellalire.vestige.application.manager.DefaultApplicationManager;
import fr.gaellalire.vestige.application.manager.URLOpener;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.job.DefaultJobManager;
import fr.gaellalire.vestige.job.JobManager;
import fr.gaellalire.vestige.jpms.JPMSAccessor;
import fr.gaellalire.vestige.jpms.JPMSAccessorLoader;
import fr.gaellalire.vestige.jpms.JPMSModuleAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerRepository;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.secure.SecureVestigeMavenResolver;
import fr.gaellalire.vestige.resolver.url_list.DefaultVestigeURLListResolver;
import fr.gaellalire.vestige.spi.resolver.VestigeResolver;
import fr.gaellalire.vestige.spi.resolver.maven.VestigeMavenResolver;
import fr.gaellalire.vestige.spi.resolver.url_list.VestigeURLListResolver;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.spi.system.VestigeSystemCache;
import fr.gaellalire.vestige.system.JVMVestigeSystemActionExecutor;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;
import fr.gaellalire.vestige.system.PrivateVestigeSecurityManager;
import fr.gaellalire.vestige.system.PrivateWhiteListVestigePolicy;
import fr.gaellalire.vestige.system.SecureProxySelector;
import fr.gaellalire.vestige.system.VestigeSystemAction;

/**
 * @author Gael Lalire
 */
public class SingleApplicationLauncherEditionVestige implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleApplicationLauncherEditionVestige.class);

    private VestigePlatform vestigePlatform;

    private DefaultApplicationManager defaultApplicationManager;

    private VestigeExecutor vestigeExecutor;

    private Thread workerThread;

    // private ApplicationDescriptorFactory applicationDescriptorFactory;

    private File configFile;

    private File dataFile;

    private File cacheFile;

    private long startTimeMillis;

    private VestigeSystem vestigeSystem;

    private List<? extends ClassLoader> privilegedClassloaders;

    private VestigeMavenResolver vestigeMavenResolver;

    private VestigeURLListResolver vestigeURLListResolver;

    private File appFile;

    public void setVestigeExecutor(final VestigeExecutor vestigeExecutor) {
        this.vestigeExecutor = vestigeExecutor;
    }

    public void setVestigePlatform(final VestigePlatform vestigePlatform) {
        this.vestigePlatform = vestigePlatform;
    }

    public void setPrivilegedClassloaders(final List<? extends ClassLoader> privilegedClassloaders) {
        this.privilegedClassloaders = privilegedClassloaders;
    }

    public void setAppFile(final File appFile) {
        this.appFile = appFile;
    }

    public void setVestigeSystem(final VestigeSystem vestigeSystem) {
        this.vestigeSystem = vestigeSystem;
    }

    /**
     * This constructor should not have its parameter modified. You can add setter to give more information.
     */
    public SingleApplicationLauncherEditionVestige(final File configFile, final File dataFile, final File cacheFile) {
        this.configFile = configFile;
        this.dataFile = dataFile;
        this.cacheFile = cacheFile;
    }

    @Override
    public void run() {

        // FIXME maybe vestigeExecutor is useless now that detach will not run anything
        if (vestigeExecutor == null) {
            vestigeExecutor = new VestigeExecutor();
        }
        if (vestigeURLListResolver == null || vestigeMavenResolver == null) {
            if (vestigePlatform == null) {
                JPMSAccessor jpmsAccessor = JPMSAccessorLoader.INSTANCE;
                JPMSModuleLayerRepository moduleLayerRepository = null;
                if (jpmsAccessor != null) {
                    moduleLayerRepository = jpmsAccessor.createModuleLayerRepository();
                }
                vestigePlatform = new DefaultVestigePlatform(vestigeExecutor, moduleLayerRepository);
            }
        }

        if (LOGGER.isInfoEnabled()) {
            startTimeMillis = System.currentTimeMillis();
            Properties vestigeProperties = new Properties();
            try {
                InputStream vestigeStream = SingleApplicationLauncherEditionVestige.class.getResourceAsStream("vestige.properties");
                if (vestigeStream != null) {
                    vestigeProperties.load(vestigeStream);
                }
            } catch (IOException e) {
                LOGGER.debug("Cannot load vestige.properties", e);
            }
            String version = vestigeProperties.getProperty("version");
            if (version == null) {
                LOGGER.info("Starting Vestige SAL");
            } else {
                LOGGER.info("Starting Vestige SAL version {}", version);
            }
        } else {
            startTimeMillis = 0;
        }

        boolean securityEnabled = System.getSecurityManager() != null;

        // Vestige dependencies can modify system, so we run isolated
        // vestigeSystem.setName("rootVestigeSystem");
        VestigeSystem singleApplicationLauncherEditionVestigeSystem = vestigeSystem.createSubSystem("singleApplicationLauncherEditionVestigeSystem");
        VestigeSystem applicationsVestigeSystem = vestigeSystem.createSubSystem("applicationsVestigeSystem");
        singleApplicationLauncherEditionVestigeSystem.setCurrentSystem();
        // new threads are in subsystem
        PrivateVestigeSecurityManager vestigeSecurityManager = null;

        if (securityEnabled) {

            PrivateWhiteListVestigePolicy whiteListVestigePolicy = new PrivateWhiteListVestigePolicy();
            // So AccessController.doPrivileged will work in JVM and privileged classes
            whiteListVestigePolicy.addSafeClassLoader(ClassLoader.getSystemClassLoader());
            if (privilegedClassloaders != null) {
                for (ClassLoader privilegedClassloader : privilegedClassloaders) {
                    whiteListVestigePolicy.addSafeClassLoader(privilegedClassloader);
                }
            }

            vestigeSystem.setWhiteListPolicy(whiteListVestigePolicy);

            ProxySelector defaultProxySelector = vestigeSystem.getDefaultProxySelector();
            if (defaultProxySelector != null) {
                // standardEditionVestigeSystem has not yet a security manager
                // standardEditionVestigeSystem.setDefaultProxySelector(new SecureProxySelector(vestigeSystem, defaultProxySelector));
                applicationsVestigeSystem.setDefaultProxySelector(new SecureProxySelector(vestigeSystem, defaultProxySelector));
            }

            vestigeSecurityManager = new PrivateVestigeSecurityManager();
            applicationsVestigeSystem.setSecurityManager(vestigeSecurityManager);
        }

        File appConfigFile = new File(configFile, "app");
        if (!appConfigFile.exists()) {
            appConfigFile.mkdir();
        }
        File appDataFile = new File(dataFile, "app");
        if (!appDataFile.exists()) {
            appDataFile.mkdir();
        }
        File cacheDataFile = new File(cacheFile, "app");
        if (!cacheDataFile.exists()) {
            cacheDataFile.mkdir();
        }

        File resolverFile = new File(dataFile, "application-manager.ser");
        File nextResolverFile = new File(dataFile, "application-manager-tmp.ser");

        // $VESTIGE_BASE/m2/settings.xml overrides $home/.m2/settings.xml
        // if none exists then no config file is used
        File mavenSettingsFile = new File(new File(configFile, "m2"), "settings.xml");
        if (!mavenSettingsFile.exists()) {
            LOGGER.debug("No vestige Maven settings file found at {}", mavenSettingsFile);
            mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
            if (!mavenSettingsFile.exists()) {
                LOGGER.debug("No user Maven settings file found at {}", mavenSettingsFile);
                mavenSettingsFile = null;
            }
        }

        if (LOGGER.isInfoEnabled()) {
            if (mavenSettingsFile == null) {
                LOGGER.info("No Maven settings file found");
            } else {
                LOGGER.info("Use {} for Maven settings file", mavenSettingsFile);
            }
        }

        if (vestigeURLListResolver == null) {
            vestigeURLListResolver = new DefaultVestigeURLListResolver(vestigePlatform);
        }

        final SSLContext sslContext;
        try {
            KeyStore trustStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);

            trustStore.load(new FileInputStream(new File(configFile, "cacerts.p12")), "changeit".toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", BouncyCastleJsseProvider.PROVIDER_NAME);
            trustManagerFactory.init(trustStore);
            sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        } catch (Exception e) {
            LOGGER.error("SSLContext creation failed", e);
            return;
        }

        if (vestigeMavenResolver == null) {
            try {
                vestigeMavenResolver = new MavenArtifactResolver(vestigePlatform, mavenSettingsFile, sslContext);
            } catch (NoLocalRepositoryManagerException e) {
                LOGGER.error("NoLocalRepositoryManagerException", e);
                return;
            }
        }

        URLOpener opener = new URLOpener() {

            @Override
            public InputStream openURL(final URL url) throws IOException {
                return new FileInputStream(appFile);
            }
        };

        List<VestigeResolver> vestigeResolvers = Arrays.asList(vestigeURLListResolver, vestigeMavenResolver);
        ApplicationRepositoryManager applicationDescriptorFactory = new XMLApplicationRepositoryManager(vestigeURLListResolver, 0, vestigeMavenResolver, 1, opener);

        JobManager actionManager = new DefaultJobManager();

        AllPermission allPermission = new AllPermission();
        PermissionCollection allPermissionCollection = allPermission.newPermissionCollection();
        allPermissionCollection.add(allPermission);

        PrivateVestigePolicy vestigePolicy = new PrivateVestigePolicy(allPermissionCollection);
        applicationsVestigeSystem.setPolicy(vestigePolicy);

        Map<String, Object> injectableByClassName = new HashMap<String, Object>();
        injectableByClassName.put(VestigeMavenResolver.class.getName(),
                new SecureVestigeMavenResolver(singleApplicationLauncherEditionVestigeSystem, vestigePolicy, vestigeMavenResolver));
        // injectableByClassName.put(VestigeURLListResolver.class.getName(), vestigeURLListResolver);

        defaultApplicationManager = new DefaultApplicationManager(actionManager, appConfigFile, appDataFile, cacheDataFile, applicationsVestigeSystem,
                singleApplicationLauncherEditionVestigeSystem, vestigePolicy, vestigeSecurityManager, applicationDescriptorFactory, resolverFile, nextResolverFile,
                vestigeResolvers, injectableByClassName);
        try {
            defaultApplicationManager.restoreState();
        } catch (ApplicationException e) {
            LOGGER.warn("Unable to restore application manager state", e);
        }

        try {
            defaultApplicationManager.install(null, "local", Arrays.<Integer> asList(0, 0, 0), "app", null);

            defaultApplicationManager.start("app");
        } catch (ApplicationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        boolean started = false;
        try {
            start(actionManager);
            started = true;
        } catch (Exception e) {
            LOGGER.error("Unable to start Vestige SAL", e);
        }

        if (started) {
            if (LOGGER.isInfoEnabled() && startTimeMillis != 0) {
                long currentTimeMillis = System.currentTimeMillis();
                long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
                LOGGER.info("Vestige SAL started in {} ms ({} ms since JVM started)", currentTimeMillis - startTimeMillis, currentTimeMillis - jvmStartTime);
            }
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    LOGGER.trace("Vestige SAL interrupted", e);
                }
            }
        }
        try {
            stop();
            if (vestigePlatform != null) {
                for (Integer id : vestigePlatform.getAttachments()) {
                    vestigePlatform.detach(id);
                }
            }
            LOGGER.info("Vestige SAL stopped");
        } catch (Exception e) {
            LOGGER.error("Unable to stop Vestige SAL", e);
        }

    }

    public void addAll(final AttachedVestigeClassLoader attachedVestigeClassLoader, final PrivateWhiteListVestigePolicy whiteListVestigePolicy) {
        whiteListVestigePolicy.addSafeClassLoader(attachedVestigeClassLoader.getVestigeClassLoader());
        for (AttachedVestigeClassLoader child : attachedVestigeClassLoader.getDependencies()) {
            addAll(child, whiteListVestigePolicy);
        }
    }

    public void start(final JobManager jobManager) throws Exception {
        if (workerThread != null) {
            return;
        }
        workerThread = vestigeExecutor.createWorker("se-worker", true, 0);
        defaultApplicationManager.startStateListenerThread();
        defaultApplicationManager.autoStart();
    }

    public void stop() throws Exception {
        if (workerThread == null) {
            return;
        }
        defaultApplicationManager.stopAll();
        defaultApplicationManager.stopStateListenerThread();
        workerThread.interrupt();
        workerThread.join();
        workerThread = null;
    }

    public static void giveDirectStreamAccessToLogback() {
        try {
            Field streamField = ConsoleTarget.class.getDeclaredField("stream");
            streamField.setAccessible(true);
            streamField.set(ConsoleTarget.SystemOut, System.out);
            streamField.set(ConsoleTarget.SystemErr, System.err);
            streamField.setAccessible(false);

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ContextInitializer ci = new ContextInitializer(loggerContext);
            URL url = ci.findURLOfDefaultConfigurationFile(true);
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(loggerContext);
                loggerContext.reset();
                configurator.doConfigure(url);
            } catch (JoranException je) {
                // StatusPrinter will handle this
            }
            StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);

            // Bug of logback
            Field copyOnThreadLocalField = LogbackMDCAdapter.class.getDeclaredField("copyOnThreadLocal");
            copyOnThreadLocalField.setAccessible(true);
            copyOnThreadLocalField.set(MDC.getMDCAdapter(), new InheritableThreadLocal<Map<String, String>>());
        } catch (NoSuchFieldException e) {
            LOGGER.error("Logback appender changes", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Logback appender changes", e);
        }
    }

    public static void vestigeMain(final VestigeExecutor vestigeExecutor, final VestigePlatform vestigePlatform, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders,
            final WeakReference<Object> bootstrapObject, final String[] args) {
        try {
            if (args.length != 4) {
                throw new IllegalArgumentException("expected 4 arguments (vestige base, vestige data, security, app file) got " + args.length);
            }
            final Thread currentThread = Thread.currentThread();

            // logback can use system stream directly
            try {
                giveDirectStreamAccessToLogback();
            } catch (Exception e) {
                // logback may not be initialized so use stderr
                e.printStackTrace();
                return;
            }

            Thread seShutdownThread = new Thread("se-shutdown") {
                @Override
                public void run() {
                    currentThread.interrupt();
                    try {
                        currentThread.join();
                    } catch (InterruptedException e) {
                        LOGGER.error("Shutdown thread interrupted", e);
                    }
                }
            };
            if (addShutdownHook == null) {
                Runtime.getRuntime().addShutdownHook(seShutdownThread);
            } else {
                addShutdownHook.apply(seShutdownThread);
            }

            String property = Security.getProperty("securerandom.source");
            if (property != null) {
                try {
                    new URL(property).openStream().close();
                } catch (IOException ie) {
                    try {
                        Field propsField = Security.class.getDeclaredField("props");
                        propsField.setAccessible(true);
                        try {
                            Properties props = (Properties) propsField.get(null);
                            props.remove("securerandom.source");
                        } finally {
                            propsField.setAccessible(false);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("BC may failed {}", e);
                    }
                }
            }

            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            int bcpos = Security.addProvider(new BouncyCastleProvider());
            LOGGER.debug("BC position is {}", bcpos);
            Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
            int bcjssepos = Security.addProvider(new BouncyCastleJsseProvider(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)));
            LOGGER.debug("BCJSSE position is {}", bcjssepos);

            String config = System.getenv("VESTIGE_CONFIG");
            if (config == null) {
                LOGGER.error("VESTIGE_CONFIG must be defined");
                return;
            }
            String data = System.getenv("VESTIGE_DATA");
            if (data == null) {
                data = config;
            }
            String cache = System.getenv("VESTIGE_CACHE");
            if (cache == null) {
                cache = data;
            }
            String vestigeSecurity = System.getenv("VESTIGE_SECURITY");
            boolean securityEnabled = true;
            if (vestigeSecurity != null) {
                securityEnabled = Boolean.parseBoolean(vestigeSecurity);
            }

            int argIndex = 0;
            String app = args[argIndex++];

            final File configFile = new File(config).getCanonicalFile();
            final File dataFile = new File(data).getCanonicalFile();
            final File cacheFile = new File(cache).getCanonicalFile();
            final File appFile = new File(app).getCanonicalFile();
            if (!configFile.isDirectory()) {
                if (!configFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige base");
                }
            }
            if (!dataFile.isDirectory()) {
                if (!dataFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige data");
                }
            }
            new JVMVestigeSystemActionExecutor(securityEnabled).execute(new VestigeSystemAction() {

                @Override
                public void vestigeSystemRun(final VestigeSystem vestigeSystem) {
                    final SingleApplicationLauncherEditionVestige singleApplicationLauncherVestige = new SingleApplicationLauncherEditionVestige(configFile, dataFile, cacheFile);
                    singleApplicationLauncherVestige.setPrivilegedClassloaders(privilegedClassloaders);
                    singleApplicationLauncherVestige.setVestigeExecutor(vestigeExecutor);
                    singleApplicationLauncherVestige.setVestigePlatform(vestigePlatform);
                    singleApplicationLauncherVestige.setVestigeSystem(vestigeSystem);
                    singleApplicationLauncherVestige.setAppFile(appFile);
                    VestigeSystemCache vestigeSystemCache = vestigeSystem.pushVestigeSystemCache();
                    try {
                        singleApplicationLauncherVestige.run();
                    } finally {
                        vestigeSystemCache.clearCache();
                    }
                }
            });

        } catch (Throwable e) {
            LOGGER.error("Uncatched throwable", e);
        }
    }

    public static void vestigeEnhancedCoreMain(final VestigeExecutor vestigeExecutor, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final String[] args) {
        JPMSAccessor jpmsAccessor = JPMSAccessorLoader.INSTANCE;
        JPMSModuleLayerRepository moduleLayerRepository = null;
        if (jpmsAccessor != null) {
            JPMSModuleLayerAccessor bootLayer = jpmsAccessor.bootLayer();
            JPMSModuleAccessor javaLogging = bootLayer.findModule("java.logging");
            javaLogging.addOpens("java.util.logging", JVMVestigeSystemActionExecutor.class);
            JPMSModuleAccessor javaBaseModule = bootLayer.findModule("java.base");
            javaBaseModule.addOpens("java.security", JVMVestigeSystemActionExecutor.class);
            javaBaseModule.addOpens("sun.security.jca", JVMVestigeSystemActionExecutor.class);
            javaBaseModule.addOpens("java.lang.reflect", JVMVestigeSystemActionExecutor.class);
            javaBaseModule.addOpens("java.net", JVMVestigeSystemActionExecutor.class);
            JPMSModuleAccessor findModule = bootLayer.findModule("java.sql");
            if (findModule != null) {
                findModule.addOpens("java.sql", JVMVestigeSystemActionExecutor.class);
            }
            moduleLayerRepository = jpmsAccessor.createModuleLayerRepository();
        }
        VestigePlatform vestigePlatform = new DefaultVestigePlatform(vestigeExecutor, moduleLayerRepository);
        vestigeMain(vestigeExecutor, vestigePlatform, addShutdownHook, removeShutdownHook, null, null, args);
    }

    public static void vestigeCoreMain(final VestigeExecutor vestigeExecutor, final String[] args) {
        vestigeEnhancedCoreMain(vestigeExecutor, null, null, args);
    }

    public static void main(final String[] args) throws Exception {
        VestigeExecutor vestigeExecutor = new VestigeExecutor();
        vestigeCoreMain(vestigeExecutor, args);
    }

}
