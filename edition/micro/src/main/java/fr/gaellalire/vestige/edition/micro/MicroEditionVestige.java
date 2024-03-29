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

package fr.gaellalire.vestige.edition.micro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.admin.telnet.TelnetServer;
import fr.gaellalire.vestige.application.descriptor.properties.PropertiesApplicationDescriptorFactory;
import fr.gaellalire.vestige.application.manager.ApplicationRepositoryManager;
import fr.gaellalire.vestige.application.manager.DefaultApplicationManager;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.core.logger.VestigeLoggerFactory;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.system.interceptor.VestigeProperties;
import fr.gaellalire.vestige.system.logger.SLF4JLoggerFactoryAdapter;
import fr.gaellalire.vestige.system.logger.SLF4JPrintStream;

/**
 * @author Gael Lalire
 */
public class MicroEditionVestige {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroEditionVestige.class);

    private VestigePlatform vestigePlatform;

    private DefaultApplicationManager defaultApplicationManager;

    private TelnetServer server;

    private File resolverFile;

    private VestigeExecutor vestigeExecutor;

    private VestigeWorker workerThread;

    private ApplicationRepositoryManager applicationDescriptorFactory;

    public MicroEditionVestige(final File baseFile, final File dataFile, final VestigeExecutor vestigeExecutor, final VestigePlatform vestigePlatform) throws IOException {
        this.vestigeExecutor = vestigeExecutor;
        this.vestigePlatform = vestigePlatform;

        // File appBaseFile = new File(baseFile, "app");
        // File appDataFile = new File(dataFile, "app");
        // try {
        // resolverFile = new File(dataFile, "application-manager.ser");
        // if (resolverFile.isFile()) {
        // ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(resolverFile));
        // try {
        // defaultApplicationManager = (DefaultApplicationManager) objectInputStream.readObject();
        // } finally {
        // objectInputStream.close();
        // }
        // }
        // } catch (Exception e) {
        // LOGGER.warn("Unable to restore application manager", e);
        // }
        //
        // if (defaultApplicationManager == null) {
        // defaultApplicationManager = new DefaultApplicationManager(appBaseFile, appDataFile);
        // }

        applicationDescriptorFactory = new PropertiesApplicationDescriptorFactory();

        final VestigeCommandExecutor vestigeCommandExecutor = new VestigeCommandExecutor(null, defaultApplicationManager);
        server = new TelnetServer(vestigeCommandExecutor, 8423);
    }

    public void start() throws Exception {
        if (workerThread != null) {
            throw new Exception("Vestige ME already started");
        }
        workerThread = vestigeExecutor.createWorker("me-worker", true, 0);
        defaultApplicationManager.autoStart();
        server.start();
    }

    public void stop() throws Exception {
        if (workerThread == null) {
            throw new Exception("Vestige ME is not started");
        }
        server.stop();
        defaultApplicationManager.stopAll();
        try {
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

    public static void main(final String[] args) throws Exception {
        try {
            if (args.length != 1) {
                throw new IllegalArgumentException("expected one argument (vestige home)");
            }
            final Thread currentThread = Thread.currentThread();
            long currentTimeMillis = 0;
            if (LOGGER.isInfoEnabled()) {
                currentTimeMillis = System.currentTimeMillis();
                LOGGER.info("Starting vestige ME");
            }

            synchronized (VestigeLoggerFactory.class) {
                SLF4JLoggerFactoryAdapter factory = new SLF4JLoggerFactoryAdapter();
                factory.setNextHandler(VestigeLoggerFactory.getVestigeLoggerFactory());
                VestigeLoggerFactory.setVestigeLoggerFactory(factory);
            }
            // FIXME simple logger must not be intercepted
            // avoid direct log
            synchronized (System.class) {
                SLF4JPrintStream out = new SLF4JPrintStream(null, true, System.out);
                System.setOut(out);
                SLF4JPrintStream err = new SLF4JPrintStream(null, false, System.err);
                System.setErr(err);
            }

            Properties properties = System.getProperties();
            VestigeProperties vestigeProperties = new VestigeProperties(System.getProperties()) {
                private static final long serialVersionUID = 5629777990474462598L;

                @Override
                public Properties getProperties() {
                    return null;
                }
            };
            System.setProperties(vestigeProperties);

            VestigeExecutor vestigeExecutor = new VestigeExecutor();
            VestigePlatform vestigePlatform = new DefaultVestigePlatform(null, null);
            File baseFile = new File(args[0]);
            File dataFile = new File(args[1]);
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
            LOGGER.info("Use {} for base file", baseFile);
            LOGGER.debug("Use {} for data file", dataFile);
            final MicroEditionVestige microEditionVestige = new MicroEditionVestige(baseFile, dataFile, vestigeExecutor, vestigePlatform);
            Runtime.getRuntime().addShutdownHook(new Thread("me-shutdown") {
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
            microEditionVestige.start();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Vestige ME started in {} ms", System.currentTimeMillis() - currentTimeMillis);
            }
            synchronized (microEditionVestige) {
                try {
                    microEditionVestige.wait();
                } catch (InterruptedException e) {
                    LOGGER.trace("Vestige ME interrupted", e);
                }
            }
            try {
                microEditionVestige.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop micro vestige edition", e);
            }
            System.setProperties(properties);
            LOGGER.info("Vestige ME stopped");
        } catch (Throwable e) {
            LOGGER.error("Unable to start vestige ME", e);
        }
    }

}
