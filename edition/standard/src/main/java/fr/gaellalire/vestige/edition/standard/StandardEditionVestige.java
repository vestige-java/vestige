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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.List;
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

import org.apache.commons.io.IOUtils;
import org.apache.sshd.SshServer;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import fr.gaellalire.vestige.application.ApplicationDescriptorFactory;
import fr.gaellalire.vestige.application.DefaultApplicationManager;
import fr.gaellalire.vestige.application.SynchronizedApplicationManager;
import fr.gaellalire.vestige.application.descriptor.xml.XMLApplicationDescriptorFactory;
import fr.gaellalire.vestige.core.VestigeExecutor;
import fr.gaellalire.vestige.edition.standard.schema.Admin;
import fr.gaellalire.vestige.edition.standard.schema.Bind;
import fr.gaellalire.vestige.edition.standard.schema.ObjectFactory;
import fr.gaellalire.vestige.edition.standard.schema.SSH;
import fr.gaellalire.vestige.edition.standard.schema.Settings;
import fr.gaellalire.vestige.edition.standard.schema.Web;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.platform.system.JVMVestigeSystemActionExecutor;
import fr.gaellalire.vestige.platform.system.PrivateVestigePolicy;
import fr.gaellalire.vestige.platform.system.PrivateVestigeSecurityManager;
import fr.gaellalire.vestige.platform.system.PrivateWhiteListVestigePolicy;
import fr.gaellalire.vestige.platform.system.PublicVestigeSystem;
import fr.gaellalire.vestige.platform.system.SecureProxySelector;
import fr.gaellalire.vestige.platform.system.VestigeSystemAction;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;

/**
 * @author Gael Lalire
 */
public class StandardEditionVestige implements VestigeSystemAction, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardEditionVestige.class);

    private VestigePlatform vestigePlatform;

    private DefaultApplicationManager defaultApplicationManager;

    private SshServer sshServer;

    private Server webServer;

    private VestigeExecutor vestigeExecutor;

    private Thread workerThread;

    private ApplicationDescriptorFactory applicationDescriptorFactory;

    private File resolverFile;

    private boolean securityEnabled;

    private File baseFile;

    private File dataFile;

    private Settings settings;

    private long startTimeMillis;

    private VestigeStateListener vestigeStateListener;

    public StandardEditionVestige(final File baseFile, final File dataFile, final VestigeExecutor vestigeExecutor, final VestigePlatform vestigePlatform,
            final VestigeStateListener vestigeStateListener) throws Exception {
        this.vestigeExecutor = vestigeExecutor;
        this.vestigePlatform = vestigePlatform;
        this.baseFile = baseFile;
        this.dataFile = dataFile;
        this.vestigeStateListener = vestigeStateListener;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
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
                IOUtils.copy(StandardEditionVestige.class.getResourceAsStream("settings.xml"), new FileOutputStream(settingsFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Settings file does not exists", e);
            } catch (IOException e) {
                throw new RuntimeException("Unable to copy settings file", e);
            }
        }
        LOGGER.info("Use {} for vestige settings file", settingsFile);

        Unmarshaller unMarshaller = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            unMarshaller = jc.createUnmarshaller();

            URL xsdURL = StandardEditionVestige.class.getResource("settings.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(xsdURL);
            unMarshaller.setSchema(schema);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize settings parser", e);
        }

        try {
            settings = ((JAXBElement<Settings>) unMarshaller.unmarshal(settingsFile)).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("unable to unmarshall settings.xml", e);
        }
        securityEnabled = settings.getSecurity().isEnabled();
        new JVMVestigeSystemActionExecutor(securityEnabled).execute(this);
    }

    public void addAll(final AttachedVestigeClassLoader attachedVestigeClassLoader, final PrivateWhiteListVestigePolicy whiteListVestigePolicy) {
        whiteListVestigePolicy.addSafeClassLoader(attachedVestigeClassLoader.getVestigeClassLoader());
        for (AttachedVestigeClassLoader child : attachedVestigeClassLoader.getDependencies()) {
            addAll(child, whiteListVestigePolicy);
        }
    }

    @Override
    public void vestigeSystemRun(final PublicVestigeSystem vestigeSystem) {
        // Vestige dependencies can modify system, so we run isolated
        PublicVestigeSystem standardEditionVestigeSystem = vestigeSystem.createSubSystem();
        standardEditionVestigeSystem.setCurrentSystem();
        // new threads are in subsystem
        ExecutorService executorService = Executors.newCachedThreadPool();

        File appFile = new File(baseFile, "app");
        if (!appFile.exists()) {
            appFile.mkdir();
        }

        try {
            resolverFile = new File(dataFile, "application-manager.ser");
            if (resolverFile.isFile()) {
                LOGGER.trace("Restoring application-manager.ser");
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(resolverFile));
                try {
                    defaultApplicationManager = (DefaultApplicationManager) objectInputStream.readObject();
                } finally {
                    objectInputStream.close();
                }
                LOGGER.trace("application-manager.ser restored");
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to restore application manager", e);
        }

        File appHomeFile = new File(appFile, "home");
        if (!appHomeFile.exists()) {
            appHomeFile.mkdir();
        }
        if (defaultApplicationManager == null) {
            defaultApplicationManager = new DefaultApplicationManager(appHomeFile);
        }
        SynchronizedApplicationManager synchronizedApplicationManager = new SynchronizedApplicationManager(defaultApplicationManager);

        // $VESTIGE_BASE/m2/settings.xml overrides $home/.m2/settings.xml
        // if none exists then no config file is used
        File mavenSettingsFile = new File(new File(baseFile, "m2"), "settings.xml");
        if (!mavenSettingsFile.exists()) {
            mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
            if (!mavenSettingsFile.exists()) {
                mavenSettingsFile = null;
            }
        }

        LOGGER.info("Use {} for maven settings file", mavenSettingsFile);

        try {
            applicationDescriptorFactory = new XMLApplicationDescriptorFactory(new MavenArtifactResolver(mavenSettingsFile));
        } catch (NoLocalRepositoryManagerException e) {
            LOGGER.error("NoLocalRepositoryManagerException", e);
            return;
        }

        Admin admin = settings.getAdmin();
        SSH ssh = admin.getSsh();

        Future<SshServer> futureSshServer = null;
        if (ssh.isEnabled()) {
            File sshBase = new File(baseFile, "ssh");
            futureSshServer = executorService.submit(new SSHServerFactory(sshBase, ssh, appHomeFile, synchronizedApplicationManager, vestigePlatform));
        }

        Future<Server> futureServer = null;
        Web web = admin.getWeb();
        if (web.isEnabled()) {
            futureServer = executorService.submit(new WebServerFactory(web, synchronizedApplicationManager, appHomeFile));
        }

        PrivateVestigePolicy vestigePolicy = null;
        PrivateVestigeSecurityManager vestigeSecurityManager = null;

        if (securityEnabled) {
            vestigePolicy = new PrivateVestigePolicy();
            vestigeSystem.setPolicy(vestigePolicy);

            PrivateWhiteListVestigePolicy whiteListVestigePolicy = new PrivateWhiteListVestigePolicy();

            // all classloader already loaded are considered safe (to be
            // verified later)
            for (Integer id : vestigePlatform.getAttachments()) {
                addAll(vestigePlatform.getAttachedVestigeClassLoader(id), whiteListVestigePolicy);
            }

            vestigeSystem.setWhiteListPolicy(whiteListVestigePolicy);

            ProxySelector defaultProxySelector = vestigeSystem.getDefaultProxySelector();
            if (defaultProxySelector != null) {
                vestigeSystem.setDefaultProxySelector(new SecureProxySelector(defaultProxySelector));
            }

            vestigeSecurityManager = new PrivateVestigeSecurityManager();
            vestigeSystem.setSecurityManager(vestigeSecurityManager);
        }

        boolean interrupted = false;
        try {
            if (futureSshServer != null) {
                try {
                    sshServer = futureSshServer.get();
                } catch (ExecutionException e) {
                    LOGGER.error("Unable to start SSH access", e);
                }
            }
            if (futureServer != null) {
                try {
                    webServer = futureServer.get();
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
                                LOGGER.error("Invalid host", e);
                                continue;
                            }
                        }
                        vestigeStateListener.webAdminAvailable(host + ":" + bind.getPort());
                        break;
                    }
                } catch (ExecutionException e) {
                    LOGGER.error("Unable to start web access", e);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("Vestige SE stopped before start finished");
            interrupted = true;
        }

        boolean started = false;
        if (!interrupted) {
            try {
                start(vestigeSystem, standardEditionVestigeSystem, vestigeSecurityManager, vestigePolicy);
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
            Thread workerThread = vestigeExecutor.createWorker("se-shutdown-worker", true, 0);
            for (Integer id : vestigePlatform.getAttachments()) {
                vestigePlatform.detach(id);
            }
            workerThread.interrupt();
            workerThread.join();
            LOGGER.info("Vestige SE stopped");
            vestigeStateListener.stopped();
        } catch (Exception e) {
            LOGGER.error("Unable to stop Vestige SE", e);
        }
    }

    public void start(final PublicVestigeSystem vestigeSystem, final PublicVestigeSystem standardEditionVestigeSystem, final PrivateVestigeSecurityManager vestigeSecurityManager,
            final PrivateVestigePolicy vestigePolicy) throws Exception {
        if (workerThread != null) {
            return;
        }
        workerThread = vestigeExecutor.createWorker("se-worker", true, 0);
        defaultApplicationManager.powerOn(vestigePlatform, vestigeSystem, standardEditionVestigeSystem, vestigeSecurityManager, vestigePolicy, applicationDescriptorFactory);
        if (sshServer != null) {
            sshServer.start();
        }
        if (webServer != null) {
            webServer.start();
        }
    }

    public void stop() throws Exception {
        if (workerThread == null) {
            return;
        }
        if (webServer != null) {
            webServer.stop();
        }
        if (sshServer != null) {
            sshServer.stop();
        }
        defaultApplicationManager.shutdown();
        try {
            File parentFile = resolverFile.getParentFile();
            if (!parentFile.isDirectory()) {
                parentFile.mkdirs();
            }
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(resolverFile));
            try {
                objectOutputStream.writeObject(defaultApplicationManager);
            } finally {
                objectOutputStream.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to save application manager", e);
        }
        workerThread.interrupt();
        workerThread.join();
        workerThread = null;
    }

    public static void vestigeMain(final VestigeExecutor vestigeExecutor, final VestigePlatform vestigePlatform, final String[] args) {
        try {
            if (args.length != 3) {
                throw new IllegalArgumentException("expected 3 arguments (vestige base, vestige data, listener port) got " + args.length);
            }
            final Thread currentThread = Thread.currentThread();

            File logbackConfigurationFile = new File(System.getProperty("logback.configurationFile"));
            if (!logbackConfigurationFile.exists()) {
                try {
                    File logbackConfigurationParentFile = logbackConfigurationFile.getParentFile();
                    if (!logbackConfigurationParentFile.isDirectory()) {
                        if (!logbackConfigurationParentFile.mkdirs()) {
                            throw new RuntimeException("Unable to create logback.xml parent directory");
                        }
                    }
                    IOUtils.copy(StandardEditionVestige.class.getResourceAsStream("logback.xml"), new FileOutputStream(logbackConfigurationFile));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("logback.configurationFile does not exists", e);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to copy logback.configurationFile file", e);
                }
            }
            // logback can use system stream directly
            try {
                giveDirectStreamAccessToLogback();
            } catch (Exception e) {
                // logback may not be initialized so use stderr
                e.printStackTrace();
                return;
            }

            Runtime.getRuntime().addShutdownHook(new Thread("se-shutdown") {
                @Override
                public void run() {
                    currentThread.interrupt();
                    try {
                        currentThread.join();
                    } catch (InterruptedException e) {
                        LOGGER.error("Shutdown thread interrupted", e);
                    }
                }
            });

            int argIndex = 0;
            String base = args[argIndex++];
            String data = args[argIndex++];
            int listenerPort = Integer.parseInt(args[argIndex++]);

            VestigeStateListener vestigeStateListener;
            if (listenerPort != 0) {
                SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", listenerPort));
                vestigeStateListener = new PrintWriterVestigeStateListener(new PrintWriter(socketChannel.socket().getOutputStream(), true));
                vestigeStateListener.starting();
            } else {
                vestigeStateListener = new NoopVestigeStateListener();
            }

            File baseFile = new File(base).getCanonicalFile();
            File dataFile = new File(data).getCanonicalFile();
            if (!baseFile.isDirectory()) {
                if (!baseFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige base");
                }
            }
            if (!dataFile.isDirectory()) {
                if (!dataFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige data");
                }
            }
            final StandardEditionVestige standardEditionVestige = new StandardEditionVestige(baseFile, dataFile, vestigeExecutor, vestigePlatform, vestigeStateListener);
            standardEditionVestige.run();

        } catch (Throwable e) {
            LOGGER.error("Uncatched throwable", e);
        }
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
        } catch (NoSuchFieldException e) {
            LOGGER.error("Logback appender changes", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Logback appender changes", e);
        }
    }

    public static void main(final String[] args) throws Exception {
        VestigeExecutor vestigeExecutor = new VestigeExecutor();
        VestigePlatform vestigePlatform = new DefaultVestigePlatform(vestigeExecutor);
        vestigeMain(vestigeExecutor, vestigePlatform, args);
    }

}
