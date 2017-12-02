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

package fr.gaellalire.vestige.edition.standard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
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
import fr.gaellalire.vestige.admin.command.CheckBootstrap;
import fr.gaellalire.vestige.admin.command.Platform;
import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.application.descriptor.xml.XMLApplicationRepositoryManager;
import fr.gaellalire.vestige.application.manager.ApplicationException;
import fr.gaellalire.vestige.application.manager.ApplicationRepositoryManager;
import fr.gaellalire.vestige.application.manager.DefaultApplicationManager;
import fr.gaellalire.vestige.application.manager.URLOpener;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.edition.standard.schema.Admin;
import fr.gaellalire.vestige.edition.standard.schema.Bind;
import fr.gaellalire.vestige.edition.standard.schema.ObjectFactory;
import fr.gaellalire.vestige.edition.standard.schema.SSH;
import fr.gaellalire.vestige.edition.standard.schema.Settings;
import fr.gaellalire.vestige.edition.standard.schema.Web;
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
import fr.gaellalire.vestige.resolver.url_list.DefaultVestigeURLListResolver;
import fr.gaellalire.vestige.spi.resolver.VestigeResolver;
import fr.gaellalire.vestige.spi.resolver.maven.VestigeMavenResolver;
import fr.gaellalire.vestige.spi.resolver.url_list.VestigeURLListResolver;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.spi.system.VestigeSystemCache;
import fr.gaellalire.vestige.system.JVMVestigeSystemActionExecutor;
import fr.gaellalire.vestige.system.PrivateVestigeSecurityManager;
import fr.gaellalire.vestige.system.PrivateWhiteListVestigePolicy;
import fr.gaellalire.vestige.system.SecureProxySelector;
import fr.gaellalire.vestige.system.VestigeSystemAction;

/**
 * @author Gael Lalire
 */
public class StandardEditionVestige implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardEditionVestige.class);

    private VestigePlatform vestigePlatform;

    private DefaultApplicationManager defaultApplicationManager;

    private VestigeServer sshServer;

    private VestigeServer webServer;

    private VestigeExecutor vestigeExecutor;

    private Thread workerThread;

    // private ApplicationDescriptorFactory applicationDescriptorFactory;

    private File baseFile;

    private File dataFile;

    private long startTimeMillis;

    private VestigeStateListener vestigeStateListener;

    private VestigeSystem vestigeSystem;

    private List<? extends ClassLoader> privilegedClassloaders;

    private WeakReference<Object> bootstrapObject;

    private VestigeMavenResolver vestigeMavenResolver;

    private VestigeURLListResolver vestigeURLListResolver;

    public void setVestigeExecutor(final VestigeExecutor vestigeExecutor) {
        this.vestigeExecutor = vestigeExecutor;
    }

    public void setVestigePlatform(final VestigePlatform vestigePlatform) {
        this.vestigePlatform = vestigePlatform;
    }

    public void setVestigeStateListener(final VestigeStateListener vestigeStateListener) {
        this.vestigeStateListener = vestigeStateListener;
    }

    public void setPrivilegedClassloaders(final List<? extends ClassLoader> privilegedClassloaders) {
        this.privilegedClassloaders = privilegedClassloaders;
    }

    public void setBootstrapObject(final WeakReference<Object> bootstrapObject) {
        this.bootstrapObject = bootstrapObject;
    }

    public void setVestigeSystem(final VestigeSystem vestigeSystem) {
        this.vestigeSystem = vestigeSystem;
    }

    /**
     * This constructor should not have its parameter modified. You can add setter to give more information.
     */
    public StandardEditionVestige(final File baseFile, final File dataFile) {
        this.baseFile = baseFile;
        this.dataFile = dataFile;
    }

    @SuppressWarnings("unchecked")
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

        if (vestigeStateListener == null) {
            vestigeStateListener = new NoopVestigeStateListener();
        }
        if (LOGGER.isInfoEnabled()) {
            startTimeMillis = System.currentTimeMillis();
            String implementationVersion = null;
            Package cPackage = StandardEditionVestige.class.getPackage();
            if (cPackage != null) {
                implementationVersion = cPackage.getImplementationVersion();
            }
            if (implementationVersion == null) {
                LOGGER.info("Starting Vestige SE");
            } else {
                LOGGER.info("Starting Vestige SE version {}", implementationVersion);
            }
        } else {
            startTimeMillis = 0;
        }
        File settingsFile = new File(baseFile, "settings.xml");
        if (!settingsFile.exists()) {
            try {
                ConfFileUtils.copy(StandardEditionVestige.class.getResourceAsStream("settings.xml"), settingsFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Settings file does not exists", e);
            } catch (IOException e) {
                throw new RuntimeException("Unable to copy settings file", e);
            }
        }
        LOGGER.info("Use {} for Vestige settings file", settingsFile);

        Unmarshaller unMarshaller = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            unMarshaller = jc.createUnmarshaller();

            URL xsdURL = StandardEditionVestige.class.getResource("settings-1.0.0.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(xsdURL);
            unMarshaller.setSchema(schema);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize settings parser", e);
        }

        Settings settings;
        try {
            settings = ((JAXBElement<Settings>) unMarshaller.unmarshal(settingsFile)).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("unable to unmarshall settings.xml", e);
        }

        boolean securityEnabled = System.getSecurityManager() != null;

        // Vestige dependencies can modify system, so we run isolated
        // vestigeSystem.setName("rootVestigeSystem");
        VestigeSystem standardEditionVestigeSystem = vestigeSystem.createSubSystem("standardEditionVestigeSystem");
        VestigeSystem applicationsVestigeSystem = vestigeSystem.createSubSystem("applicationsVestigeSystem");
        standardEditionVestigeSystem.setCurrentSystem();
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

        ExecutorService executorService = Executors.newCachedThreadPool();

        File appBaseFile = new File(baseFile, "app");
        if (!appBaseFile.exists()) {
            appBaseFile.mkdir();
        }
        File appDataFile = new File(dataFile, "app");
        if (!appDataFile.exists()) {
            appDataFile.mkdir();
        }

        File resolverFile = new File(dataFile, "application-manager.ser");
        File nextResolverFile = new File(dataFile, "application-manager-tmp.ser");

        // $VESTIGE_BASE/m2/settings.xml overrides $home/.m2/settings.xml
        // if none exists then no config file is used
        File mavenSettingsFile = new File(new File(baseFile, "m2"), "settings.xml");
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
        if (vestigeMavenResolver == null) {
            try {
                vestigeMavenResolver = new MavenArtifactResolver(vestigePlatform, mavenSettingsFile);
            } catch (NoLocalRepositoryManagerException e) {
                LOGGER.error("NoLocalRepositoryManagerException", e);
                return;
            }
        }

        URLOpener opener = new URLOpener() {

            @Override
            public InputStream openURL(final URL url) throws IOException {
                // TODO how to share httpClient ?
                HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
                // httpClientBuilder.setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider());
                httpClientBuilder.setRoutePlanner(new SystemDefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE, ProxySelector.getDefault()));
                // httpClientBuilder.useSystemProperties();
                CloseableHttpClient httpClient = httpClientBuilder.build();
                String protocol = url.getProtocol();
                if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
                    try {
                        HttpGet request = new HttpGet(url.toURI());
                        HttpResponse response = httpClient.execute(request);
                        return response.getEntity().getContent();
                    } catch (URISyntaxException e) {
                        throw new IOException("URL is not valid", e);
                    }
                } else {
                    return url.openStream();
                }
            }
        };

        List<VestigeResolver> vestigeResolvers = Arrays.asList(vestigeURLListResolver, vestigeMavenResolver);
        ApplicationRepositoryManager applicationDescriptorFactory = new XMLApplicationRepositoryManager(vestigeURLListResolver, 0, vestigeMavenResolver, 1, opener);

        JobManager actionManager = new DefaultJobManager();

        defaultApplicationManager = new DefaultApplicationManager(actionManager, appBaseFile, appDataFile, applicationsVestigeSystem, standardEditionVestigeSystem,
                vestigeSecurityManager, applicationDescriptorFactory, resolverFile, nextResolverFile, vestigeResolvers);
        try {
            defaultApplicationManager.restoreState();
        } catch (ApplicationException e) {
            LOGGER.warn("Unable to restore application manager state", e);
        }

        Admin admin = settings.getAdmin();
        SSH ssh = admin.getSsh();

        VestigeCommandExecutor vestigeCommandExecutor = new VestigeCommandExecutor(actionManager, defaultApplicationManager);
        if (bootstrapObject != null) {
            vestigeCommandExecutor.addCommand(new CheckBootstrap(bootstrapObject));
        }
        if (vestigePlatform != null) {
            vestigeCommandExecutor.addCommand(new Platform(vestigePlatform));
        }

        Future<VestigeServer> futureSshServer = null;
        if (ssh.isEnabled()) {
            File sshBase = new File(baseFile, "ssh");
            File sshData = new File(dataFile, "ssh");
            futureSshServer = executorService.submit(new SSHServerFactory(sshBase, sshData, ssh, baseFile, vestigeCommandExecutor));
        }

        Future<VestigeServer> futureWebServer = null;
        Web web = admin.getWeb();
        if (web.isEnabled()) {
            futureWebServer = executorService.submit(new WebServerFactory(web, defaultApplicationManager, baseFile, vestigeCommandExecutor));
            List<Bind> binds = web.getBind();
            for (Bind bind : binds) {
                String host = bind.getHost();
                if (host == null) {
                    host = "localhost";
                } else {
                    try {
                        if (InetAddress.getByName(host).isAnyLocalAddress()) {
                            host = "localhost";
                        }
                    } catch (UnknownHostException e) {
                        LOGGER.debug("Invalid host", e);
                        continue;
                    }
                }
                webURL = "http://" + host + ":" + bind.getPort();
                break;
            }
        }

        boolean interrupted = false;
        try {
            if (futureSshServer != null) {
                try {
                    sshServer = futureSshServer.get();
                } catch (ExecutionException e) {
                    LOGGER.error("Unable to create SSH access", e);
                }
            }
            if (futureWebServer != null) {
                try {
                    webServer = futureWebServer.get();
                } catch (ExecutionException e) {
                    LOGGER.error("Unable to create web access", e);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("Vestige SE stopped before start finished");
            interrupted = true;
        }

        boolean started = false;
        if (!interrupted) {
            try {
                start(actionManager);
                started = true;
            } catch (Exception e) {
                LOGGER.error("Unable to start Vestige SE", e);
            }
        }

        if (started) {
            if (LOGGER.isInfoEnabled() && startTimeMillis != 0) {
                long currentTimeMillis = System.currentTimeMillis();
                long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
                LOGGER.info("Vestige SE started in {} ms ({} ms since JVM started)", currentTimeMillis - startTimeMillis, currentTimeMillis - jvmStartTime);
            }
            vestigeStateListener.started();
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    LOGGER.trace("Vestige SE interrupted", e);
                }
            }
        }
        try {
            vestigeStateListener.stopping();
            stop();
            if (vestigePlatform != null) {
                for (Integer id : vestigePlatform.getAttachments()) {
                    vestigePlatform.detach(id);
                }
            }
            LOGGER.info("Vestige SE stopped");
            vestigeStateListener.stopped();
        } catch (Exception e) {
            LOGGER.error("Unable to stop Vestige SE", e);
        }

    }

    public void addAll(final AttachedVestigeClassLoader attachedVestigeClassLoader, final PrivateWhiteListVestigePolicy whiteListVestigePolicy) {
        whiteListVestigePolicy.addSafeClassLoader(attachedVestigeClassLoader.getVestigeClassLoader());
        for (AttachedVestigeClassLoader child : attachedVestigeClassLoader.getDependencies()) {
            addAll(child, whiteListVestigePolicy);
        }
    }

    private String webURL;

    public void start(final JobManager jobManager) throws Exception {
        if (workerThread != null) {
            return;
        }
        workerThread = vestigeExecutor.createWorker("se-worker", true, 0);
        // try {
        // defaultApplicationManager.autoMigrate();
        // } catch (ApplicationException e) {
        // LOGGER.error("Automigration failed", e);
        // }
        defaultApplicationManager.startStateListenerThread();
        defaultApplicationManager.autoStart();
        if (sshServer != null) {
            try {
                sshServer.start();
            } catch (Exception e) {
                LOGGER.error("Unable to start SSH access", e);
            }
        }
        if (webServer != null) {
            try {
                webServer.start();
                vestigeStateListener.webAdminAvailable(webURL);
            } catch (Exception e) {
                LOGGER.error("Unable to start web access", e);
            }
        }
    }

    public void stop() throws Exception {
        if (workerThread == null) {
            return;
        }
        if (webServer != null) {
            try {
                webServer.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop web access", e);
            }
        }
        if (sshServer != null) {
            try {
                sshServer.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop SSH access", e);
            }
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
                throw new IllegalArgumentException("expected 4 arguments (vestige base, vestige data, security, listener port) got " + args.length);
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

            int argIndex = 0;
            String base = args[argIndex++];
            String data = args[argIndex++];
            boolean securityEnabled = Boolean.parseBoolean(args[argIndex++]);
            int listenerPort = Integer.parseInt(args[argIndex++]);

            final VestigeStateListener vestigeStateListener;
            SocketChannel socketChannel = null;
            try {
                if (listenerPort != 0) {
                    LOGGER.debug("Connect to listener at port {}", listenerPort);
                    socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", listenerPort));
                    vestigeStateListener = new PrintWriterVestigeStateListener(new PrintWriter(socketChannel.socket().getOutputStream(), true));
                    vestigeStateListener.starting();
                } else {
                    vestigeStateListener = new NoopVestigeStateListener();
                }

                final File baseFile = new File(base).getCanonicalFile();
                final File dataFile = new File(data).getCanonicalFile();
                if (!baseFile.isDirectory()) {
                    if (!baseFile.mkdirs()) {
                        LOGGER.error("Unable to create vestige base");
                    }
                }
                vestigeStateListener.base(baseFile);
                if (!dataFile.isDirectory()) {
                    if (!dataFile.mkdirs()) {
                        LOGGER.error("Unable to create vestige data");
                    }
                }
                new JVMVestigeSystemActionExecutor(securityEnabled).execute(new VestigeSystemAction() {

                    @Override
                    public void vestigeSystemRun(final VestigeSystem vestigeSystem) {
                        final StandardEditionVestige standardEditionVestige = new StandardEditionVestige(baseFile, dataFile);
                        standardEditionVestige.setPrivilegedClassloaders(privilegedClassloaders);
                        standardEditionVestige.setBootstrapObject(bootstrapObject);
                        standardEditionVestige.setVestigeExecutor(vestigeExecutor);
                        standardEditionVestige.setVestigePlatform(vestigePlatform);
                        standardEditionVestige.setVestigeStateListener(vestigeStateListener);
                        standardEditionVestige.setVestigeSystem(vestigeSystem);
                        VestigeSystemCache vestigeSystemCache = vestigeSystem.pushVestigeSystemCache();
                        try {
                            standardEditionVestige.run();
                        } finally {
                            vestigeSystemCache.clearCache();
                        }
                    }
                });

            } finally {
                if (socketChannel != null) {
                    socketChannel.close();
                }
            }

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
