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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SocketChannel;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
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
import fr.gaellalire.vestige.admin.command.CheckBootstrap;
import fr.gaellalire.vestige.admin.command.DiscardUnattached;
import fr.gaellalire.vestige.admin.command.Platform;
import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
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
import fr.gaellalire.vestige.edition.standard.schema.Admin;
import fr.gaellalire.vestige.edition.standard.schema.ObjectFactory;
import fr.gaellalire.vestige.edition.standard.schema.SSH;
import fr.gaellalire.vestige.edition.standard.schema.Settings;
import fr.gaellalire.vestige.edition.standard.schema.Web;
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
import fr.gaellalire.vestige.utils.SimpleValueGetter;
import fr.gaellalire.vestige.utils.UtilsSchema;

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

    private VestigeWorker[] vestigeWorker = new VestigeWorker[1];

    private File systemConfigFile;

    private File configFile;

    private File dataFile;

    private File cacheFile;

    private long startTimeMillis;

    private VestigeStateListener vestigeStateListener;

    private VestigeSystem vestigeSystem;

    private List<? extends ClassLoader> privilegedClassloaders;

    private WeakReference<Object> bootstrapObject;

    private VestigeMavenResolver vestigeMavenResolver;

    private VestigeURLListResolver vestigeURLListResolver;

    public void setSystemConfigFile(final File systemConfigFile) {
        this.systemConfigFile = systemConfigFile;
    }

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
    public StandardEditionVestige(final File configFile, final File dataFile, final File cacheFile) {
        this.configFile = configFile;
        this.dataFile = dataFile;
        this.cacheFile = cacheFile;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        boolean vestigeExecutorCreated = false;
        boolean vestigePlatformCreated = false;
        if (vestigeExecutor == null) {
            vestigeExecutorCreated = true;
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
                vestigePlatformCreated = true;
            }
        }

        if (systemConfigFile == null) {
            String systemConfig = System.getenv("VESTIGE_SYSTEM_CONFIG");
            if (systemConfig != null) {
                systemConfigFile = new File(systemConfig).getAbsoluteFile();
            }
        }

        if (vestigeStateListener == null) {
            vestigeStateListener = new NoopVestigeStateListener();
        }
        if (LOGGER.isInfoEnabled()) {
            startTimeMillis = System.currentTimeMillis();
            Properties vestigeProperties = new Properties();
            try {
                InputStream vestigeStream = StandardEditionVestige.class.getResourceAsStream("vestige.properties");
                if (vestigeStream != null) {
                    vestigeProperties.load(vestigeStream);
                }
            } catch (IOException e) {
                LOGGER.debug("Cannot load vestige.properties", e);
            }
            String version = vestigeProperties.getProperty("version");
            if (version == null) {
                LOGGER.info("Starting Vestige SE");
            } else {
                LOGGER.info("Starting Vestige SE version {}", version);
            }
        } else {
            startTimeMillis = 0;
        }
        File settingsFile = new File(configFile, "settings.xml");
        if (!settingsFile.exists()) {
            try {
                ConfFileUtils.copy(StandardEditionVestige.class.getResourceAsStream("settings.xml"), settingsFile, "UTF-8");
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

            URL xsdURL = StandardEditionVestige.class.getResource("settings.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(new Source[] {new StreamSource(UtilsSchema.getURL().toExternalForm()), new StreamSource(xsdURL.toExternalForm())});
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
        }, "se-ssl-context-creator");
        thread.setDaemon(true);
        thread.start();

        if (vestigeMavenResolver == null) {
            try {
                vestigeMavenResolver = new MavenArtifactResolver(vestigePlatform, vestigeWorker, mavenSettingsFile, lazySSLContextAccessor);
                // the mvn protocol of the launching code may not be the same as ours
                // vestigeSystem.setURLStreamHandlerForProtocol(MavenArtifactResolver.MVN_PROTOCOL, MavenArtifactResolver.URL_STREAM_HANDLER);
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
                httpClientBuilder.setSSLContext(lazySSLContextAccessor.getSSLContext());
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
        final String installM2Repo = System.getenv("VESTIGE_INSTALL_M2_REPO");
        MavenArtifactResolver installVestigeMavenResolver = null;
        if (installM2Repo != null) {
            File installM2RepoFile = new File(installM2Repo);
            if (installM2RepoFile.mkdirs()) {
                try {
                    installVestigeMavenResolver = new MavenArtifactResolver(vestigePlatform, vestigeWorker, mavenSettingsFile, lazySSLContextAccessor, "installation",
                            installM2RepoFile);
                    // the mvn protocol of the launching code may not be the same as ours
                    // vestigeSystem.setURLStreamHandlerForProtocol(MavenArtifactResolver.MVN_PROTOCOL, MavenArtifactResolver.URL_STREAM_HANDLER);
                } catch (NoLocalRepositoryManagerException e) {
                    LOGGER.error("NoLocalRepositoryManagerException", e);
                    return;
                }
            }
        }
        ApplicationRepositoryManager applicationDescriptorFactory = new XMLApplicationRepositoryManager(vestigeURLListResolver, 0, vestigeMavenResolver,
                installVestigeMavenResolver, 1, opener);

        JobManager actionManager = new DefaultJobManager();

        AllPermission allPermission = new AllPermission();
        PermissionCollection allPermissionCollection = allPermission.newPermissionCollection();
        allPermissionCollection.add(allPermission);

        PrivateVestigePolicy vestigePolicy = new PrivateVestigePolicy(allPermissionCollection);
        applicationsVestigeSystem.setPolicy(vestigePolicy);

        Map<String, Object> injectableByClassName = new HashMap<String, Object>();
        injectableByClassName.put(VestigeMavenResolver.class.getName(), new SecureVestigeMavenResolver(standardEditionVestigeSystem, vestigePolicy, vestigeMavenResolver));
        // injectableByClassName.put(VestigeURLListResolver.class.getName(), vestigeURLListResolver);

        defaultApplicationManager = new DefaultApplicationManager(actionManager, appConfigFile, appDataFile, appCacheFile, applicationsVestigeSystem, standardEditionVestigeSystem,
                vestigePolicy, vestigeSecurityManager, applicationDescriptorFactory, resolverFile, nextResolverFile, vestigeResolvers, injectableByClassName);

        Admin admin = settings.getAdmin();
        SSH ssh = admin.getSsh();

        VestigeCommandExecutor vestigeCommandExecutor = new VestigeCommandExecutor(actionManager, defaultApplicationManager);
        if (bootstrapObject != null) {
            vestigeCommandExecutor.addCommand(new CheckBootstrap(bootstrapObject));
        }
        if (vestigePlatform != null) {
            vestigeCommandExecutor.addCommand(new Platform(vestigePlatform));
            vestigeCommandExecutor.addCommand(new DiscardUnattached(vestigePlatform));
        }

        Web web = admin.getWeb();

        File commandHistory = new File(cacheFile, "history.txt");

        boolean interrupted = false;
        ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {

            private AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, "se-executor-" + count.incrementAndGet());
            }
        });
        try {

            Future<VestigeServer> futureSshServer = null;
            if (SimpleValueGetter.INSTANCE.getValue(ssh.getEnabled())) {
                File sshConfig = new File(configFile, "ssh");
                File sshData = new File(dataFile, "ssh");
                futureSshServer = executorService.submit(new SSHServerFactory(sshConfig, sshData, commandHistory, ssh, configFile, vestigeCommandExecutor));
            }

            Future<VestigeServer> futureWebServer = null;
            if (SimpleValueGetter.INSTANCE.getValue(web.getEnabled())) {
                File webConfig = new File(configFile, "web");
                File webData = new File(dataFile, "web");
                // TODO share commandHistory with ssh
                futureWebServer = executorService
                        .submit(new WebServerFactory(webConfig, webData, web, defaultApplicationManager, configFile, vestigeCommandExecutor, vestigeStateListener));
            }

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
        } finally {
            executorService.shutdownNow();
        }

        boolean started = false;
        if (!interrupted) {
            try {
                startService(actionManager);
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
            URL vestigeInstallURL = null;
            String vestigeInstallURLString = System.getenv("VESTIGE_INSTALL_URL");
            try {
                if (vestigeInstallURLString == null) {
                    String vestigeInstallFileString = System.getenv("VESTIGE_INSTALL_FILE");
                    if (vestigeInstallFileString != null) {
                        File vestigeInstallFile = new File(vestigeInstallFileString);
                        if (vestigeInstallFile.isFile()) {
                            vestigeInstallURL = vestigeInstallFile.toURI().toURL();
                        }
                    }
                } else {
                    vestigeInstallURL = new URL(vestigeInstallURLString);
                }
            } catch (MalformedURLException e) {
                LOGGER.trace("Vestige SE auto installing invalid URL", e);
            }
            if (vestigeInstallURL != null) {
                try {
                    final String installLocalName = System.getenv("VESTIGE_INSTALL_LOCAL_NAME");
                    if (installLocalName != null) {
                        final JobController jobController = defaultApplicationManager.install(vestigeInstallURL, null, null, null, installLocalName, new JobListener() {

                            @Override
                            public TaskListener taskAdded(final String description) {
                                return new TaskListener() {

                                    @Override
                                    public void taskDone() {
                                    }

                                    @Override
                                    public void progressChanged(final float progress) {
                                    }
                                };
                            }

                            @Override
                            public void jobDone() {
                                synchronized (StandardEditionVestige.this) {
                                    StandardEditionVestige.this.notify();
                                }
                            }
                        });
                        synchronized (this) {
                            try {
                                while (!jobController.isDone()) {
                                    wait();
                                }
                                defaultApplicationManager.setAutoStarted(installLocalName, true);
                            } catch (InterruptedException e) {
                                LOGGER.trace("Vestige SE interrupted while auto installing", e);
                                jobController.interrupt();
                                interrupted = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Vestige SE auto installing failed", e);
                }
            }
            if (!interrupted) {
                String stopAfterStart = System.getenv("VESTIGE_STOP_AFTER_START");
                if (stopAfterStart == null || !Boolean.parseBoolean(stopAfterStart)) {
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            LOGGER.trace("Vestige SE interrupted", e);
                        }
                    }
                } else {
                    LOGGER.trace("Stop according to VESTIGE_STOP_AFTER_START");
                }
            }
        }
        try {
            vestigeStateListener.stopping();
            stopService();
            if (vestigePlatform != null) {
                for (Integer id : vestigePlatform.getAttachments()) {
                    vestigePlatform.detach(id);
                }
            }
            if (vestigePlatformCreated) {
                vestigePlatform.close();
            }
            if (vestigeExecutorCreated) {
                vestigeExecutor.getThreadReaperHelper().reap();
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

    public void startService(final JobManager jobManager) throws Exception {
        if (vestigeWorker[0] != null) {
            return;
        }
        vestigeWorker[0] = vestigeExecutor.createWorker("se-worker", true, 0);
        try {
            defaultApplicationManager.restoreState();
        } catch (ApplicationException e) {
            LOGGER.warn("Unable to restore application manager state", e);
        }

        defaultApplicationManager.startService();
        // try {
        // defaultApplicationManager.autoMigrate();
        // } catch (ApplicationException e) {
        // LOGGER.error("Automigration failed", e);
        // }
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
                vestigeStateListener.webAdminAvailable("https://" + webServer.getLocalHost() + ":" + webServer.getLocalPort());
            } catch (Exception e) {
                LOGGER.error("Unable to start web access", e);
            }
        }
    }

    public void stopService() throws Exception {
        if (vestigeWorker[0] == null) {
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
        defaultApplicationManager.stopService();
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
            String vestigeListenerPort = System.getenv("VESTIGE_LISTENER_PORT");
            int listenerPort = 0;
            if (vestigeListenerPort != null) {
                listenerPort = Integer.parseInt(vestigeListenerPort);
            }

            final VestigeStateListener vestigeStateListener;
            SocketChannel socketChannel = null;
            try {
                if (listenerPort != 0) {
                    LOGGER.debug("Connect to listener at port {}", listenerPort);
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress("127.0.0.1", listenerPort));
                    socketChannel.configureBlocking(false);
                    vestigeStateListener = new NIOWriterVestigeStateListener(socketChannel);
                    vestigeStateListener.starting();
                    new VestigeStateListenerWatcher(socketChannel).start();
                } else {
                    vestigeStateListener = new NoopVestigeStateListener();
                }

                final File configFile = new File(config).getCanonicalFile();
                final File dataFile = new File(data).getCanonicalFile();
                final File cacheFile = new File(cache).getCanonicalFile();
                if (!configFile.isDirectory()) {
                    if (!configFile.mkdirs()) {
                        LOGGER.error("Unable to create vestige config");
                    }
                }
                vestigeStateListener.config(configFile);
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
                        final StandardEditionVestige standardEditionVestige = new StandardEditionVestige(configFile, dataFile, cacheFile);
                        standardEditionVestige.setPrivilegedClassloaders(privilegedClassloaders);
                        standardEditionVestige.setBootstrapObject(bootstrapObject);
                        standardEditionVestige.setVestigeExecutor(vestigeCoreContext.getVestigeExecutor());
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
                String stopAfterStart = System.getenv("VESTIGE_STOP_AFTER_START");
                if (stopAfterStart != null && Boolean.parseBoolean(stopAfterStart)) {
                    // accelerate the stop process, run in separate thread to avoid thread lock
                    new Thread() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    }.start();
                }
                try {
                    if (removeShutdownHook == null) {
                        Runtime.getRuntime().removeShutdownHook(seShutdownThread);
                    } else {
                        removeShutdownHook.apply(seShutdownThread);
                    }
                } catch (IllegalStateException e) {
                    // ok, shutdown in progress
                }
                vestigePlatform.close();
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
                vestigeCoreContext.getStreamHandlerFactory().setDelegate(null);
            } finally {
                if (socketChannel != null) {
                    socketChannel.close();
                }
            }

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
