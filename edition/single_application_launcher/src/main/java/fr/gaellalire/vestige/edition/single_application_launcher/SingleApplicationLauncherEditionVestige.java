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
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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
import fr.gaellalire.vestige.core.VestigeCoreContext;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.job.DefaultJobManager;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobListener;
import fr.gaellalire.vestige.job.JobManager;
import fr.gaellalire.vestige.job.TaskListener;
import fr.gaellalire.vestige.jpms.JPMSAccessor;
import fr.gaellalire.vestige.jpms.JPMSAccessorLoader;
import fr.gaellalire.vestige.jpms.JPMSModuleAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerRepository;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.platform.VestigeURLStreamHandlerFactory;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.SSLContextAccessor;
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

    private VestigeWorker[] vestigeWorker = new VestigeWorker[1];

    private File configFile;

    private File dataFile;

    private File cacheFile;

    private long startTimeMillis;

    private VestigeSystem vestigeSystem;

    private List<? extends ClassLoader> privilegedClassloaders;

    private VestigeMavenResolver vestigeMavenResolver;

    private VestigeURLListResolver vestigeURLListResolver;

    private URL appURL;

    private String appName;

    public void setVestigeExecutor(final VestigeExecutor vestigeExecutor) {
        this.vestigeExecutor = vestigeExecutor;
    }

    public void setVestigePlatform(final VestigePlatform vestigePlatform) {
        this.vestigePlatform = vestigePlatform;
    }

    public void setPrivilegedClassloaders(final List<? extends ClassLoader> privilegedClassloaders) {
        this.privilegedClassloaders = privilegedClassloaders;
    }

    public void setAppURL(final URL appURL) {
        this.appURL = appURL;
    }

    public void setAppName(final String appName) {
        this.appName = appName;
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
        if (appName == null) {
            appName = "myapp";
        }
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
                vestigePlatform = new DefaultVestigePlatform(moduleLayerRepository);
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
        File appCacheFile = new File(cacheFile, "app");
        if (!appCacheFile.exists()) {
            appCacheFile.mkdir();
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
            vestigeURLListResolver = new DefaultVestigeURLListResolver(vestigePlatform, vestigeWorker);
        }

        final SSLContextAccessor lazySSLContextAccessor = new SSLContextAccessor() {

            private SSLContext sslContext;

            private volatile SSLContext volatileSSLContext;

            private Object mutex = new Object();

            @Override
            public SSLContext getSSLContext() {
                if (sslContext == null) {
                    if (volatileSSLContext == null) {
                        synchronized (mutex) {
                            if (volatileSSLContext == null) {
                                SSLContext sslContext;
                                try {
                                    KeyStore trustStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);

                                    File caCerts = new File(configFile, "cacerts.p12");
                                    TrustManager[] trustManagers = null;
                                    if (caCerts.isFile()) {
                                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", BouncyCastleJsseProvider.PROVIDER_NAME);
                                        FileInputStream stream = new FileInputStream(caCerts);
                                        try {
                                            trustStore.load(stream, "changeit".toCharArray());
                                        } finally {
                                            stream.close();
                                        }
                                        trustManagerFactory.init(trustStore);
                                        trustManagers = trustManagerFactory.getTrustManagers();
                                    }

                                    sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
                                    sslContext.init(null, trustManagers, SecureRandom.getInstance("DEFAULT", BouncyCastleProvider.PROVIDER_NAME));
                                    volatileSSLContext = sslContext;
                                } catch (Exception e) {
                                    throw new Error("SSLContext creation failed", e);
                                }
                            }
                        }
                    }
                    sslContext = volatileSSLContext;
                }
                return sslContext;
            }
        };

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                lazySSLContextAccessor.getSSLContext();
            }
        }, "sal-ssl-context-creator");
        thread.setDaemon(true);
        thread.start();

        if (vestigeMavenResolver == null) {
            try {
                vestigeMavenResolver = new MavenArtifactResolver(vestigePlatform, vestigeWorker, mavenSettingsFile, lazySSLContextAccessor);
            } catch (NoLocalRepositoryManagerException e) {
                LOGGER.error("NoLocalRepositoryManagerException", e);
                return;
            }
        }

        URLOpener opener = new URLOpener() {

            @Override
            public InputStream openURL(final URL url) throws IOException {
                return url.openStream();
            }
        };

        List<VestigeResolver> vestigeResolvers = Arrays.asList(vestigeURLListResolver, vestigeMavenResolver);
        ApplicationRepositoryManager applicationDescriptorFactory = new XMLApplicationRepositoryManager(vestigeURLListResolver, 0, vestigeMavenResolver, null, 1, opener);

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

        defaultApplicationManager = new DefaultApplicationManager(actionManager, appConfigFile, appDataFile, appCacheFile, applicationsVestigeSystem,
                singleApplicationLauncherEditionVestigeSystem, vestigePolicy, vestigeSecurityManager, applicationDescriptorFactory, resolverFile, nextResolverFile,
                vestigeResolvers, injectableByClassName);

        boolean started = false;
        try {
            startService(actionManager);
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
                    defaultApplicationManager.run(appName);
                } catch (ApplicationException e) {
                    LOGGER.trace("Vestige SAL exception", e);
                }
            }
        }
        try {
            stopService();
            if (vestigePlatform != null) {
                for (Integer id : vestigePlatform.getAttachments()) {
                    vestigePlatform.detach(id);
                }
            }
            LOGGER.info("Vestige SAL stopped");
        } catch (Exception e) {
            LOGGER.error("Unable to stop Vestige SAL", e);
        }
        // accelerate the stop process, run in separate thread to avoid thread lock
        new Thread() {
            @Override
            public void run() {
                System.exit(0);
            }
        }.start();
    }

    public void addAll(final AttachedVestigeClassLoader attachedVestigeClassLoader, final PrivateWhiteListVestigePolicy whiteListVestigePolicy) {
        whiteListVestigePolicy.addSafeClassLoader(attachedVestigeClassLoader.getVestigeClassLoader());
        for (AttachedVestigeClassLoader child : attachedVestigeClassLoader.getDependencies()) {
            addAll(child, whiteListVestigePolicy);
        }
    }

    public void startService(final JobManager jobManager) throws Exception {
        if (vestigeWorker[0] != null) {
            return;
        }
        vestigeWorker[0] = vestigeExecutor.createWorker("sal-worker", true, 0);
        try {
            defaultApplicationManager.restoreState();
        } catch (ApplicationException e) {
            LOGGER.warn("Unable to restore application manager state", e);
        }

        try {
            if (!defaultApplicationManager.getApplicationsName().contains(appName)) {
                final boolean[] installDone = new boolean[] {false};
                JobController jobController = defaultApplicationManager.install(appURL, null, appName, Arrays.<Integer> asList(0, 0, 0), appName, new JobListener() {

                    @Override
                    public TaskListener taskAdded(final String description) {
                        return null;
                    }

                    @Override
                    public void jobDone() {
                        synchronized (installDone) {
                            installDone[0] = true;
                            installDone.notifyAll();
                        }
                    }
                });
                synchronized (installDone) {
                    while (!installDone[0]) {
                        installDone.wait();
                    }
                }
                Exception exception = jobController.getException();
                if (exception != null) {
                    LOGGER.error("Got an installing exception", exception);
                }
            }
        } catch (ApplicationException e) {
            LOGGER.error("Got an exception", e);
        }

    }

    public void stopService() throws Exception {
        if (vestigeWorker[0] == null) {
            return;
        }
        vestigeWorker[0].interrupt();
        vestigeWorker[0].join();
        vestigeWorker = null;
    }

    public static void configureLogback() {
        try {
            Field streamField = ConsoleTarget.class.getDeclaredField("stream");
            streamField.setAccessible(true);
            streamField.set(ConsoleTarget.SystemOut, System.out);
            streamField.set(ConsoleTarget.SystemErr, System.err);
            streamField.setAccessible(false);

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ContextInitializer ci = new ContextInitializer(loggerContext);
            URL url = ci.findURLOfDefaultConfigurationFile(true);
            if (url != null) {
                try {
                    JoranConfigurator configurator = new JoranConfigurator();
                    configurator.setContext(loggerContext);
                    loggerContext.reset();
                    configurator.doConfigure(url);
                } catch (JoranException je) {
                    // StatusPrinter will handle this
                }
                StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
            }

            // Bug of logback
            Field copyOnThreadLocalField = LogbackMDCAdapter.class.getDeclaredField("copyOnThreadLocal");
            copyOnThreadLocalField.setAccessible(true);
            try {
                copyOnThreadLocalField.set(MDC.getMDCAdapter(), new InheritableThreadLocal<Map<String, String>>());
            } finally {
                copyOnThreadLocalField.setAccessible(false);
            }
        } catch (NoSuchFieldException e) {
            LOGGER.error("Logback appender changes", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Logback appender changes", e);
        }
    }

    public static void vestigeMain(final VestigeCoreContext vestigeCoreContext, final VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory,
            final VestigePlatform vestigePlatform, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders,
            final WeakReference<Object> bootstrapObject, final String[] args) {
        try {
            final Thread currentThread = Thread.currentThread();

            // logback can use system stream directly
            try {
                configureLogback();
            } catch (Exception e) {
                // logback may not be initialized so use stderr
                e.printStackTrace();
                return;
            }

            Thread salShutdownThread = new Thread("sal-shutdown") {
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
                Runtime.getRuntime().addShutdownHook(salShutdownThread);
            } else {
                addShutdownHook.apply(salShutdownThread);
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

            final URL appURL;
            String app = System.getenv("VESTIGE_SAL_APP_URL");
            if (app == null) {
                app = System.getenv("VESTIGE_SAL_APP_FILE");
                appURL = new File(app).toURI().toURL();
            } else {
                appURL = new URL(app);
            }

            final String appName = System.getenv("VESTIGE_SAL_APP_LOCAL_NAME");

            final File configFile = new File(config).getCanonicalFile();
            final File dataFile = new File(data).getCanonicalFile();
            final File cacheFile = new File(cache).getCanonicalFile();
            if (!configFile.isDirectory()) {
                if (!configFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige config");
                }
            }
            if (!dataFile.isDirectory()) {
                if (!dataFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige data");
                }
            }
            if (!cacheFile.isDirectory()) {
                if (!cacheFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige cache");
                }
            }
            new JVMVestigeSystemActionExecutor(securityEnabled).execute(new VestigeSystemAction() {

                @Override
                public void vestigeSystemRun(final VestigeSystem vestigeSystem) {
                    final SingleApplicationLauncherEditionVestige singleApplicationLauncherVestige = new SingleApplicationLauncherEditionVestige(configFile, dataFile, cacheFile);
                    singleApplicationLauncherVestige.setPrivilegedClassloaders(privilegedClassloaders);
                    singleApplicationLauncherVestige.setVestigeExecutor(vestigeCoreContext.getVestigeExecutor());
                    singleApplicationLauncherVestige.setVestigePlatform(vestigePlatform);
                    singleApplicationLauncherVestige.setVestigeSystem(vestigeSystem);
                    singleApplicationLauncherVestige.setAppURL(appURL);
                    singleApplicationLauncherVestige.setAppName(appName);
                    VestigeSystemCache vestigeSystemCache = vestigeSystem.pushVestigeSystemCache();
                    try {
                        singleApplicationLauncherVestige.run();
                    } finally {
                        vestigeSystemCache.clearCache();
                    }
                }
            });
            try {
                if (removeShutdownHook == null) {
                    Runtime.getRuntime().removeShutdownHook(salShutdownThread);
                } else {
                    removeShutdownHook.apply(salShutdownThread);
                }
            } catch (IllegalStateException e) {
                // ok, shutdown in progress
            }
            vestigePlatform.close();
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
            vestigeCoreContext.getStreamHandlerFactory().setDelegate(null);
        } catch (Throwable e) {
            LOGGER.error("Uncatched throwable", e);
        }
    }

    public static void vestigeEnhancedCoreMain(final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final String[] args) throws Exception {
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

        VestigePlatform vestigePlatform = new DefaultVestigePlatform(moduleLayerRepository);

        File mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        if (!mavenSettingsFile.isFile()) {
            mavenSettingsFile = new File(args[0]).getAbsoluteFile();
        }
        LOGGER.info("Use {} for Maven settings file", mavenSettingsFile);

        // we don't need vestigePlatform or vestigeWorker because we will never create a classloader, we only want the baseDir
        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(null, null, mavenSettingsFile, null);
        VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory = new VestigeURLStreamHandlerFactory();
        MavenArtifactResolver.replaceMavenURLStreamHandler(mavenArtifactResolver.getBaseDir(), vestigeURLStreamHandlerFactory);
        DelegateURLStreamHandlerFactory streamHandlerFactory = vestigeCoreContext.getStreamHandlerFactory();
        streamHandlerFactory.setDelegate(vestigeURLStreamHandlerFactory);

        vestigeMain(vestigeCoreContext, vestigeURLStreamHandlerFactory, vestigePlatform, addShutdownHook, removeShutdownHook, privilegedClassloaders, null, args);
    }

    public static void vestigeCoreMain(final VestigeCoreContext vestigeCoreContext, final String[] args) throws Exception {
        vestigeEnhancedCoreMain(vestigeCoreContext, null, null, null, args);
    }

    public static void main(final String[] args) throws Exception {
        VestigeCoreContext vestigeCoreContext = VestigeCoreContext.buildDefaultInstance();
        URL.setURLStreamHandlerFactory(vestigeCoreContext.getStreamHandlerFactory());
        vestigeCoreMain(vestigeCoreContext, args);
    }

}
