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

package com.googlecode.vestige.edition.micro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.vestige.admin.command.VestigeCommandExecutor;
import com.googlecode.vestige.admin.telnet.TelnetServer;
import com.googlecode.vestige.application.ApplicationDescriptorFactory;
import com.googlecode.vestige.application.DefaultApplicationManager;
import com.googlecode.vestige.application.descriptor.xml.PropertiesApplicationDescriptorFactory;
import com.googlecode.vestige.core.VestigeExecutor;
import com.googlecode.vestige.core.logger.VestigeLoggerFactory;
import com.googlecode.vestige.platform.DefaultVestigePlatform;
import com.googlecode.vestige.platform.VestigePlatform;
import com.googlecode.vestige.platform.logger.SLF4JLoggerFactoryAdapter;
import com.googlecode.vestige.platform.logger.SLF4JPrintStream;
import com.googlecode.vestige.platform.system.interceptor.VestigeProperties;

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

    private Thread workerThread;

    private ApplicationDescriptorFactory applicationDescriptorFactory;

    public MicroEditionVestige(final File homeFile, final VestigeExecutor vestigeExecutor, final VestigePlatform vestigePlatform) throws IOException {
        this.vestigeExecutor = vestigeExecutor;
        this.vestigePlatform = vestigePlatform;

        File appHome = new File(homeFile, "app");

        try {
            resolverFile = new File(appHome, "application-manager.ser");
            if (resolverFile.isFile()) {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(resolverFile));
                try {
                    defaultApplicationManager = (DefaultApplicationManager) objectInputStream.readObject();
                } finally {
                    objectInputStream.close();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to restore application manager", e);
        }

        if (defaultApplicationManager == null) {
            defaultApplicationManager = new DefaultApplicationManager(new File(appHome, "home"));
        }

        applicationDescriptorFactory = new PropertiesApplicationDescriptorFactory();

        final VestigeCommandExecutor vestigeCommandExecutor = new VestigeCommandExecutor(defaultApplicationManager, vestigePlatform);
        server = new TelnetServer(vestigeCommandExecutor, 1984);
    }

    public void start() throws Exception {
        if (workerThread != null) {
            throw new Exception("Vestige ME already started");
        }
        workerThread = vestigeExecutor.createWorker("me-worker", true, 0);
        defaultApplicationManager.powerOn(vestigePlatform, null, null, null, null, applicationDescriptorFactory);
        server.start();
    }

    public void stop() throws Exception {
        if (workerThread == null) {
            throw new Exception("Vestige ME is not started");
        }
        server.stop();
        defaultApplicationManager.shutdown();
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
                SLF4JPrintStream out = new SLF4JPrintStream(true, System.out);
                System.setOut(out);
                SLF4JPrintStream err = new SLF4JPrintStream(false, System.err);
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
            VestigePlatform vestigePlatform = new DefaultVestigePlatform(vestigeExecutor);
            String home = args[0];
            File homeFile = new File(home);
            if (!homeFile.isDirectory()) {
                if (!homeFile.mkdirs()) {
                    LOGGER.error("Unable to create vestige home");
                }
            }
            LOGGER.info("Use {} for home file", homeFile);
            final MicroEditionVestige microEditionVestige = new MicroEditionVestige(homeFile, vestigeExecutor, vestigePlatform);
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
