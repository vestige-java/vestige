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

package fr.gaellalire.vestige.application.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.job.Job;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobHelper;
import fr.gaellalire.vestige.job.JobListener;
import fr.gaellalire.vestige.job.JobManager;
import fr.gaellalire.vestige.job.TaskHelper;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.platform.system.PrivateVestigePolicy;
import fr.gaellalire.vestige.platform.system.PrivateVestigeSecurityManager;
import fr.gaellalire.vestige.platform.system.PublicVestigeSystem;
import fr.gaellalire.vestige.utils.FileUtils;

/**
 * @author Gael Lalire
 */
public class DefaultApplicationManager implements ApplicationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApplicationManager.class);

    private VestigePlatform vestigePlatform;

    private PublicVestigeSystem rootVestigeSystem;

    private PublicVestigeSystem managerVestigeSystem;

    private ApplicationRepositoryManager applicationDescriptorFactory;

    private File appBaseFile;

    private File appDataFile;

    private DefaultApplicationManagerState state;

    private VestigeSecureExecutor vestigeSecureExecutor;

    private File resolverFile;

    private File nextResolverFile;

    private List<ApplicationManagerStateListener> applicationManagerStateListeners;

    private ApplicationManagerState lastState;

    private Thread stateListenerThread;

    public DefaultApplicationManager(final JobManager actionManager, final File appBaseFile, final File appDataFile, final VestigePlatform vestigePlatform,
            final PublicVestigeSystem vestigeSystem, final PublicVestigeSystem managerVestigeSystem, final PrivateVestigeSecurityManager vestigeSecurityManager,
            final ApplicationRepositoryManager applicationDescriptorFactory, final File resolverFile, final File nextResolverFile) {
        this.actionManager = actionManager;
        this.appBaseFile = appBaseFile;
        this.appDataFile = appDataFile;

        AllPermission allPermission = new AllPermission();
        PermissionCollection allPermissionCollection = allPermission.newPermissionCollection();
        allPermissionCollection.add(allPermission);

        PrivateVestigePolicy vestigePolicy = new PrivateVestigePolicy(allPermissionCollection);
        vestigeSystem.setPolicy(vestigePolicy);
        this.rootVestigeSystem = vestigeSystem;
        this.managerVestigeSystem = managerVestigeSystem;
        this.vestigeSecureExecutor = new VestigeSecureExecutor(vestigeSecurityManager, vestigePolicy);
        this.vestigePlatform = vestigePlatform;
        this.applicationDescriptorFactory = applicationDescriptorFactory;
        this.resolverFile = resolverFile;
        this.nextResolverFile = nextResolverFile;
        state = new DefaultApplicationManagerState();
        applicationManagerStateListeners = new ArrayList<ApplicationManagerStateListener>();
    }

    private DefaultApplicationManagerState readDefaultApplicationManagerState(final File file) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file)) {
            protected java.lang.Class<?> resolveClass(final java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
            }
        };
        try {
            return (DefaultApplicationManagerState) objectInputStream.readObject();
        } finally {
            objectInputStream.close();
        }
    }

    public void restoreState() throws ApplicationException {
        try {
            if (nextResolverFile.isFile()) {
                try {
                    state = readDefaultApplicationManagerState(nextResolverFile);
                    resolverFile.delete();
                    nextResolverFile.renameTo(resolverFile);
                    return;
                } catch (Exception e) {
                    LOGGER.warn("Next resolver file invalid", e);
                }
            }
            if (resolverFile.isFile()) {
                state = readDefaultApplicationManagerState(resolverFile);
            }
        } catch (Exception e) {
            throw new ApplicationException("Unable to restore old state", e);
        }
    }

    public void saveState() throws ApplicationException {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(nextResolverFile));
            try {
                objectOutputStream.writeObject(state);
            } finally {
                objectOutputStream.close();
            }
            resolverFile.delete();
            nextResolverFile.renameTo(resolverFile);
        } catch (IOException e) {
            throw new ApplicationException("Unable to save new state", e);
        }
        synchronized (applicationManagerStateListeners) {
            if (applicationManagerStateListeners.size() != 0) {
                lastState = state.copy();
                applicationManagerStateListeners.notify();
            }
        }
    }

    public ApplicationContext createApplicationContext(final String repoName, final String appName, final List<Integer> version, final String installName, final JobHelper jobHelper)
            throws ApplicationException {
        URL context = state.getRepositoryURL(repoName);
        if (context == null) {
            throw new ApplicationException("Repository not found");
        }

        ApplicationDescriptor applicationDescriptor = applicationDescriptorFactory.createApplicationDescriptor(context, repoName, appName, version, jobHelper);

        File basefile = new File(appBaseFile, installName);
        File dataFile = new File(appDataFile, installName);

        Set<List<Integer>> supportedMigrationVersion = applicationDescriptor.getSupportedMigrationVersions();
        Set<List<Integer>> uninterruptedMigrationVersion = applicationDescriptor.getUninterruptedMigrationVersions();
        if (!supportedMigrationVersion.containsAll(uninterruptedMigrationVersion)) {
            throw new ApplicationException("Some migration are uninterrupted but not supported");
        }

        ApplicationContext applicationContext = new ApplicationContext();

        applicationContext.setRepoName(repoName);
        applicationContext.setRepoApplicationName(appName);
        applicationContext.setRepoApplicationVersion(version);
        applicationContext.setInstallerClassName(applicationDescriptor.getInstallerClassName());
        applicationContext.setInstallerResolve(applicationDescriptor.getInstallerClassLoaderConfiguration(installName + " installer"));
        applicationContext.setClassName(applicationDescriptor.getLauncherClassName());
        applicationContext.setInstallerPrivateSystem(applicationDescriptor.isInstallerPrivateSystem());
        applicationContext.setPrivateSystem(applicationDescriptor.isLauncherPrivateSystem());
        applicationContext.setResolve(applicationDescriptor.getLauncherClassLoaderConfiguration(installName + " launcher"));
        applicationContext.setPermissions(applicationDescriptor.getPermissions());
        applicationContext.setInstallerPermissions(applicationDescriptor.getInstallerPermissions());

        applicationContext.setBase(basefile);
        applicationContext.setData(dataFile);
        applicationContext.setSupportedMigrationVersion(supportedMigrationVersion);
        applicationContext.setUninterruptedMigrationVersion(uninterruptedMigrationVersion);
        applicationContext.setName(installName);

        return applicationContext;
    }

    private Set<String> lockedInstallNames = new HashSet<String>();

    private JobManager actionManager;

    public JobController install(final String repoName, final String appName, final List<Integer> version, final String pinstallName, final JobListener jobListener)
            throws ApplicationException {
        String installName = pinstallName;
        if (installName == null || installName.length() == 0) {
            installName = appName;
        }
        synchronized (state) {
            if (state.hasContext(installName)) {
                throw new ApplicationException("application already installed");
            }
            if (!lockedInstallNames.add(installName)) {
                throw new ApplicationException("application already installing");
            }
        }
        return actionManager.submitJob("install", "Installing " + installName, new InstallAction(repoName, appName, version, installName), jobListener);
    }

    /**
     * @author Gael Lalire
     */
    private class InstallAction implements Job {

        private String repoName;

        private String appName;

        private List<Integer> version;

        private String installName;

        public InstallAction(final String repoName, final String appName, final List<Integer> version, final String installName) {
            this.repoName = repoName;
            this.appName = appName;
            this.version = version;
            this.installName = installName;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            ApplicationContext applicationContext = null;

            boolean successful = false;
            try {
                applicationContext = createApplicationContext(repoName, appName, version, installName, jobHelper);
                final File base = applicationContext.getBase();
                if (base.exists()) {
                    try {
                        FileUtils.forceDelete(base);
                    } catch (IOException e) {
                        throw new ApplicationException("Base directory already exists and cannot be deleted", e);
                    }
                }
                base.mkdirs();
                final File data = applicationContext.getData();
                if (data.exists()) {
                    try {
                        FileUtils.forceDelete(data);
                    } catch (IOException e) {
                        throw new ApplicationException("Data directory already exists and cannot be deleted", e);
                    }
                }
                data.mkdirs();

                ClassLoaderConfiguration installerResolve = applicationContext.getInstallerResolve();
                if (installerResolve != null) {
                    TaskHelper task = jobHelper.addTask("Attaching installer classLoader");
                    int installerAttach = vestigePlatform.attach(installerResolve);
                    task.setDone();
                    try {
                        final VestigeClassLoader<?> installerClassLoader = vestigePlatform.getClassLoader(installerAttach);

                        Set<Permission> additionnalPermissions = new HashSet<Permission>();
                        additionnalPermissions.addAll(installerResolve.getPermissions());
                        additionnalPermissions.add(new FilePermission(base.getPath(), "read,write"));
                        additionnalPermissions.add(new FilePermission(base.getPath() + File.separator + "-", "read,write,delete"));
                        additionnalPermissions.add(new FilePermission(data.getPath(), "read,write"));
                        additionnalPermissions.add(new FilePermission(data.getPath() + File.separator + "-", "read,write,delete"));
                        additionnalPermissions.addAll(applicationContext.getInstallerPermissions());

                        final PublicVestigeSystem vestigeSystem;
                        if (applicationContext.isInstallerPrivateSystem()) {
                            vestigeSystem = rootVestigeSystem.createSubSystem();
                        } else {
                            vestigeSystem = rootVestigeSystem;
                        }
                        final String installerClassName = applicationContext.getInstallerClassName();
                        VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader, additionnalPermissions, null, applicationContext.getName() + "-installer", vestigeSystem,
                                new Callable<Void>() {

                                    @Override
                                    public Void call() throws Exception {
                                        ApplicationInstaller applicationInstaller = new ApplicationInstallerInvoker(callConstructor(installerClassLoader,
                                                installerClassLoader.loadClass(installerClassName), base, data, vestigeSystem));
                                        applicationInstaller.install();
                                        return null;
                                    }

                                }, managerVestigeSystem, null);
                        task = jobHelper.addTask("Calling install method");
                        vestigeSecureExecution.start();
                        try {
                            vestigeSecureExecution.get();
                        } finally {
                            task.setDone();
                        }
                    } finally {
                        vestigePlatform.detach(installerAttach);
                    }
                }
                successful = true;
            } catch (Exception e) {
                throw new ApplicationException("fail to install", e);
            } finally {
                synchronized (state) {
                    lockedInstallNames.remove(installName);
                    if (successful) {
                        state.install(installName, applicationContext);
                        saveState();
                    }
                }
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Application {} version {} on repository {} installed", new Object[] {appName, VersionUtils.toString(version), repoName});
            }
        }
    }

    public static final List<Integer> LOCKED_VERSION = Collections.emptyList();

    public JobController uninstall(final String installName, final JobListener jobListener) throws ApplicationException {
        final ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getApplication(installName);
            if (applicationContext.isStarted()) {
                throw new ApplicationException("Application is started");
            }
            applicationContext.setMigrationRepoApplicationVersion(LOCKED_VERSION);
        }
        return actionManager.submitJob("uninstall", "Uninstalling " + installName, new UninstallAction(applicationContext, installName), jobListener);
    }

    /**
     * @author Gael Lalire
     */
    private class UninstallAction implements Job {

        private ApplicationContext applicationContext;

        private String installName;

        public UninstallAction(final ApplicationContext applicationContext, final String installName) {
            this.applicationContext = applicationContext;
            this.installName = installName;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            try {
                final File base = applicationContext.getBase();
                final File data = applicationContext.getData();
                try {
                    ClassLoaderConfiguration installerResolve = applicationContext.getInstallerResolve();
                    if (installerResolve != null) {
                        TaskHelper task = jobHelper.addTask("Attaching installer classLoader");
                        int installerAttach = vestigePlatform.attach(installerResolve);
                        task.setDone();
                        try {
                            final VestigeClassLoader<?> installerClassLoader = vestigePlatform.getClassLoader(installerAttach);

                            Set<Permission> additionnalPermissions = new HashSet<Permission>();
                            additionnalPermissions.addAll(installerResolve.getPermissions());
                            additionnalPermissions.add(new FilePermission(base.getPath(), "read,write"));
                            additionnalPermissions.add(new FilePermission(base.getPath() + File.separator + "-", "read,write,delete"));
                            additionnalPermissions.add(new FilePermission(data.getPath(), "read,write"));
                            additionnalPermissions.add(new FilePermission(data.getPath() + File.separator + "-", "read,write,delete"));
                            additionnalPermissions.addAll(applicationContext.getInstallerPermissions());
                            final PublicVestigeSystem vestigeSystem;
                            if (applicationContext.isInstallerPrivateSystem()) {
                                vestigeSystem = rootVestigeSystem.createSubSystem();
                            } else {
                                vestigeSystem = rootVestigeSystem;
                            }
                            VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader, additionnalPermissions, null, applicationContext.getName() + "-installer", vestigeSystem,
                                    new Callable<Void>() {

                                        @Override
                                        public Void call() throws Exception {
                                            ApplicationInstaller applicationInstaller = new ApplicationInstallerInvoker(callConstructor(installerClassLoader,
                                                    installerClassLoader.loadClass(applicationContext.getInstallerClassName()), base, data, vestigeSystem));
                                            applicationInstaller.uninstall();
                                            return null;
                                        }

                                    }, managerVestigeSystem, null);
                            task = jobHelper.addTask("Calling uninstall method");
                            vestigeSecureExecution.start();
                            try {
                                vestigeSecureExecution.get();
                            } finally {
                                task.setDone();
                            }
                        } finally {
                            vestigePlatform.detach(installerAttach);
                        }
                    }
                } finally {
                    if (base.exists()) {
                        try {
                            FileUtils.forceDelete(base);
                        } catch (IOException e) {
                            LOGGER.error("Unable to remove base application directory", e);
                        }
                    }
                    if (data.exists()) {
                        try {
                            FileUtils.forceDelete(data);
                        } catch (IOException e) {
                            LOGGER.error("Unable to remove data application directory", e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("fail to uninstall properly", e);
            } finally {
                synchronized (state) {
                    state.uninstall(installName);
                    saveState();
                }
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Application {} uninstalled", installName);
            }
        }
    }

    public ApplicationContext findMigratorApplicationContext(final List<Integer> fromVersion, final ApplicationContext fromApplicationContext, final List<Integer> toVersion,
            final ApplicationContext toApplicationContext, final boolean ignoreIfUnsupported) throws ApplicationException {
        Integer compare = VersionUtils.compare(fromVersion, toVersion);
        if (compare == null) {
            if (fromApplicationContext.getSupportedMigrationVersion().contains(toVersion)) {
                // use fromVersion to perform the migration
                return fromApplicationContext;
            } else {
                if (!toApplicationContext.getSupportedMigrationVersion().contains(fromVersion)) {
                    if (ignoreIfUnsupported) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("None of " + VersionUtils.toString(fromVersion) + " and " + VersionUtils.toString(toVersion) + " can migrate the other");
                        }
                        return null;
                    }
                    throw new ApplicationException("None of " + VersionUtils.toString(fromVersion) + " and " + VersionUtils.toString(toVersion) + " can migrate the other");
                }
                // use toVersion to perform the migration
                return toApplicationContext;
            }
        } else {
            if (compare.intValue() < 0) {
                // fromVersion before toVersion
                if (toApplicationContext.getSupportedMigrationVersion().contains(fromVersion)) {
                    // use toVersion to perform the migration
                    return toApplicationContext;
                } else {
                    if (ignoreIfUnsupported) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(VersionUtils.toString(toVersion) + " does not support migrate from " + VersionUtils.toString(fromVersion));
                        }
                        return null;
                    }
                    throw new ApplicationException(VersionUtils.toString(toVersion) + " does not support migrate from " + VersionUtils.toString(fromVersion));
                }
            } else {
                // toVersion before fromVersion
                if (fromApplicationContext.getSupportedMigrationVersion().contains(toVersion)) {
                    // use fromVersion to perform the migration
                    return fromApplicationContext;
                } else {
                    if (ignoreIfUnsupported) {
                        LOGGER.debug(VersionUtils.toString(fromVersion) + " does not support migrate from " + VersionUtils.toString(toVersion));
                        return null;
                    }
                    throw new ApplicationException(VersionUtils.toString(fromVersion) + " does not support migrate from " + VersionUtils.toString(toVersion));
                }
            }
        }
    }

    public JobController migrate(final String installName, final List<Integer> toVersion, final JobListener jobListener) throws ApplicationException {
        final ApplicationContext fromApplicationContext;
        synchronized (state) {
            fromApplicationContext = state.getApplication(installName);
            fromApplicationContext.setMigrationRepoApplicationVersion(toVersion);
        }
        return actionManager.submitJob("migrate", "Migrating " + installName, new MigrateAction(fromApplicationContext, installName, toVersion, false, false), jobListener);
    }

    /**
     * @author Gael Lalire
     */
    private class MigrateAction implements Job {

        private ApplicationContext fromApplicationContext;

        private String installName;

        private List<Integer> toVersion;

        private boolean ignoreIfUnsupported;

        // private boolean stopIfMigrationPossible;

        public MigrateAction(final ApplicationContext fromApplicationContext, final String installName, final List<Integer> toVersion, final boolean ignoreIfUnsupported,
                final boolean stopIfMigrationPossible) {
            this.fromApplicationContext = fromApplicationContext;
            this.installName = installName;
            this.toVersion = toVersion;
            this.ignoreIfUnsupported = ignoreIfUnsupported;
            // this.stopIfMigrationPossible = stopIfMigrationPossible;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Migrating {} to version {} ", installName, VersionUtils.toString(toVersion));
            }
            try {
                ApplicationContext toApplicationContext = createApplicationContext(fromApplicationContext.getRepoName(), fromApplicationContext.getRepoApplicationName(),
                        toVersion, installName, jobHelper);

                int level = fromApplicationContext.getAutoMigrateLevel();
                // migration target inherits autoMigrateLevel
                toApplicationContext.setAutoMigrateLevel(level);

                final List<Integer> fromVersion = fromApplicationContext.getRepoApplicationVersion();
                final ApplicationContext migratorApplicationContext = findMigratorApplicationContext(fromVersion, fromApplicationContext, toVersion, toApplicationContext,
                        ignoreIfUnsupported);
                if (migratorApplicationContext == null) {
                    return;
                }
                ClassLoaderConfiguration installerResolve = migratorApplicationContext.getInstallerResolve();

                final RuntimeApplicationContext runtimeApplicationContext = fromApplicationContext.getRuntimeApplicationContext();
                // && runtimeApplicationContext != null should be redundant
                if (fromApplicationContext.isStarted() && runtimeApplicationContext != null) {
                    List<Integer> supportedVersion;
                    if (migratorApplicationContext == fromApplicationContext) {
                        supportedVersion = toVersion;
                    } else {
                        supportedVersion = fromVersion;
                    }
                    if (!migratorApplicationContext.getUninterruptedMigrationVersion().contains(supportedVersion)) {
                        if (ignoreIfUnsupported) {
                            LOGGER.info("Uninterrupted migration not supported and application running");
                            return;
                        } else {
                            throw new ApplicationException("Uninterrupted migration not supported and application running");
                        }
                    }

                    try {
                        final Object runMutex = new Object();
                        RuntimeApplicationContext toRuntimeApplicationContext = toApplicationContext.getRuntimeApplicationContext();
                        Object constructorMutex = null;
                        if (toRuntimeApplicationContext == null) {
                            constructorMutex = new Object();
                        } else {
                            toRuntimeApplicationContext.setRunAllowed(false);
                        }
                        start(toApplicationContext, runMutex, constructorMutex);
                        if (constructorMutex != null) {
                            synchronized (constructorMutex) {
                                toRuntimeApplicationContext = toApplicationContext.getRuntimeApplicationContext();
                                while (toRuntimeApplicationContext == null && toApplicationContext.getVestigeSecureExecution() != null) {
                                    constructorMutex.wait();
                                    toRuntimeApplicationContext = toApplicationContext.getRuntimeApplicationContext();
                                }
                            }
                        }
                        if (toRuntimeApplicationContext == null) {
                            // fail on object construction
                            throw new ApplicationException("Cannot create application instance");
                        }

                        final RuntimeApplicationContext notNullToRuntimeApplicationContext = toRuntimeApplicationContext;

                        final Runnable notifyRunMutex = new Runnable() {

                            @Override
                            public void run() {
                                synchronized (runMutex) {
                                    notNullToRuntimeApplicationContext.setRunAllowed(true);
                                    runMutex.notify();
                                }
                            }
                        };
                        try {
                            TaskHelper task;
                            if (migratorApplicationContext == fromApplicationContext) {
                                task = jobHelper.addTask("Attaching installer classLoader of version " + VersionUtils.toString(fromVersion));
                            } else {
                                task = jobHelper.addTask("Attaching installer classLoader of version " + VersionUtils.toString(toVersion));
                            }
                            int installerAttach = vestigePlatform.attach(installerResolve);
                            task.setDone();
                            try {
                                final VestigeClassLoader<?> installerClassLoader = vestigePlatform.getClassLoader(installerAttach);

                                Set<Permission> additionnalPermissions = new HashSet<Permission>();
                                additionnalPermissions.addAll(installerResolve.getPermissions());
                                final File base = fromApplicationContext.getBase();
                                final File data = fromApplicationContext.getData();
                                additionnalPermissions.add(new FilePermission(base.getPath(), "read,write"));
                                additionnalPermissions.add(new FilePermission(base.getPath() + File.separator + "-", "read,write,delete"));
                                additionnalPermissions.add(new FilePermission(data.getPath(), "read,write"));
                                additionnalPermissions.add(new FilePermission(data.getPath() + File.separator + "-", "read,write,delete"));
                                additionnalPermissions.addAll(fromApplicationContext.getResolve().getPermissions());
                                additionnalPermissions.addAll(toApplicationContext.getResolve().getPermissions());
                                additionnalPermissions.addAll(migratorApplicationContext.getInstallerPermissions());
                                final PublicVestigeSystem vestigeSystem;
                                if (migratorApplicationContext.isInstallerPrivateSystem()) {
                                    vestigeSystem = rootVestigeSystem.createSubSystem();
                                } else {
                                    vestigeSystem = rootVestigeSystem;
                                }
                                VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader, additionnalPermissions,
                                        Arrays.asList(fromApplicationContext.getVestigeSecureExecution().getThreadGroup(), toApplicationContext.getVestigeSecureExecution().getThreadGroup()),
                                        migratorApplicationContext.getName() + "-installer", vestigeSystem, new Callable<Void>() {

                                            @Override
                                            public Void call() throws Exception {
                                                ApplicationInstaller applicationInstaller = new ApplicationInstallerInvoker(callConstructor(installerClassLoader,
                                                        installerClassLoader.loadClass(fromApplicationContext.getInstallerClassName()), base, data, vestigeSystem));
                                                if (migratorApplicationContext == fromApplicationContext) {
                                                    applicationInstaller.uninterruptedMigrateTo(runtimeApplicationContext.getRunnable(), toVersion,
                                                            notNullToRuntimeApplicationContext.getRunnable(), notifyRunMutex);
                                                } else {
                                                    applicationInstaller.uninterruptedMigrateFrom(fromVersion, runtimeApplicationContext.getRunnable(),
                                                            notNullToRuntimeApplicationContext.getRunnable(), notifyRunMutex);
                                                }
                                                return null;
                                            }

                                        }, managerVestigeSystem, null);
                                if (migratorApplicationContext == fromApplicationContext) {
                                    task = jobHelper.addTask("Calling uninterruptedMigrateTo method");
                                } else {
                                    task = jobHelper.addTask("Calling uninterruptedMigrateFrom method");
                                }
                                vestigeSecureExecution.start();
                                try {
                                    vestigeSecureExecution.get();
                                } finally {
                                    task.setDone();
                                }
                            } finally {
                                vestigePlatform.detach(installerAttach);
                            }
                        } finally {
                            notifyRunMutex.run();
                        }

                        stop(fromApplicationContext);

                    } catch (Exception e) {
                        throw new ApplicationException("fail to uninterrupted migrate", e);
                    }
                } else {
                    try {
                        TaskHelper task;
                        if (migratorApplicationContext == fromApplicationContext) {
                            task = jobHelper.addTask("Attaching installer classLoader of version " + VersionUtils.toString(fromVersion));
                        } else {
                            task = jobHelper.addTask("Attaching installer classLoader of version " + VersionUtils.toString(toVersion));
                        }
                        int installerAttach = vestigePlatform.attach(installerResolve);
                        task.setDone();
                        try {
                            final VestigeClassLoader<?> installerClassLoader = vestigePlatform.getClassLoader(installerAttach);

                            Set<Permission> additionnalPermissions = new HashSet<Permission>();
                            additionnalPermissions.addAll(installerResolve.getPermissions());
                            final File base = fromApplicationContext.getBase();
                            final File data = fromApplicationContext.getData();
                            additionnalPermissions.add(new FilePermission(base.getPath(), "read,write"));
                            additionnalPermissions.add(new FilePermission(base.getPath() + File.separator + "-", "read,write,delete"));
                            additionnalPermissions.add(new FilePermission(data.getPath(), "read,write"));
                            additionnalPermissions.add(new FilePermission(data.getPath() + File.separator + "-", "read,write,delete"));
                            additionnalPermissions.addAll(migratorApplicationContext.getInstallerPermissions());
                            final PublicVestigeSystem vestigeSystem;
                            if (migratorApplicationContext.isInstallerPrivateSystem()) {
                                vestigeSystem = rootVestigeSystem.createSubSystem();
                            } else {
                                vestigeSystem = rootVestigeSystem;
                            }
                            VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader, additionnalPermissions, null, migratorApplicationContext.getName() + "-installer", vestigeSystem,
                                    new Callable<Void>() {

                                        @Override
                                        public Void call() throws Exception {
                                            ApplicationInstaller applicationInstaller = new ApplicationInstallerInvoker(callConstructor(installerClassLoader,
                                                    installerClassLoader.loadClass(fromApplicationContext.getInstallerClassName()), base, data, vestigeSystem));
                                            if (migratorApplicationContext == fromApplicationContext) {
                                                applicationInstaller.migrateTo(toVersion);
                                            } else {
                                                applicationInstaller.migrateFrom(fromVersion);
                                            }
                                            return null;
                                        }

                                    }, managerVestigeSystem, null);
                            if (migratorApplicationContext == fromApplicationContext) {
                                task = jobHelper.addTask("Calling migrateTo method");
                            } else {
                                task = jobHelper.addTask("Calling migrateFrom method");
                            }
                            vestigeSecureExecution.start();
                            try {
                                vestigeSecureExecution.get();
                            } finally {
                                task.setDone();
                            }
                        } finally {
                            vestigePlatform.detach(installerAttach);
                        }
                    } catch (Exception e) {
                        throw new ApplicationException("fail to migrate", e);
                    }
                }
                synchronized (state) {
                    state.install(installName, toApplicationContext);
                    saveState();
                }
            } finally {
                synchronized (state) {
                    fromApplicationContext.setMigrationRepoApplicationVersion(null);
                }
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Application {} migrated to version {}", installName, VersionUtils.toString(toVersion));
            }
        }
    }

    public void start(final String installName) throws ApplicationException {
        synchronized (state) {
            final ApplicationContext applicationContext = state.getApplication(installName);
            if (applicationContext.isStarted()) {
                throw new ApplicationException("already started");
            }
            start(applicationContext);
        }
        LOGGER.info("Application {} started", installName);
    }

    public void stop(final String installName) throws ApplicationException {
        ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getApplication(installName);
        }
        stop(applicationContext);
        LOGGER.info("Application {} stopped", installName);
    }

    public void start(final ApplicationContext applicationContext) throws ApplicationException {
        if (applicationContext.getVestigeSecureExecution() != null) {
            // already started
            return;
        }
        start(applicationContext, null, null);
    }

    public Object callConstructor(final ClassLoader classLoader, final Class<?> loadClass, final File home, final File data, final PublicVestigeSystem vestigeSystem)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, ApplicationException {
        // level : constructor
        // 0 : ()
        // 1 : (File)
        // 2 : (File, File)
        // 3 : (File, File, VestigeSystem)
        // The first file is base directory, the optional second file is data
        // directory

        Class<?> bestItfType = null;
        Constructor<?> bestConstructor = null;
        int level = -1;
        for (Constructor<?> constructor : loadClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            switch (parameterTypes.length) {
            case 0:
                if (level < 0) {
                    level = 0;
                    bestConstructor = constructor;
                }
                break;
            case 1:
                if (level < 1) {
                    if (!parameterTypes[0].equals(File.class)) {
                        break;
                    }
                    level = 1;
                    bestConstructor = constructor;
                }
                break;
            case 2:
                if (level < 1) {
                    if (!parameterTypes[0].equals(File.class)) {
                        break;
                    }
                    if (!parameterTypes[1].equals(File.class)) {
                        break;
                    }
                    level = 2;
                    bestConstructor = constructor;
                }
                break;
            case 3:
                if (vestigeSystem == null) {
                    break;
                }
                if (!parameterTypes[0].equals(File.class)) {
                    break;
                }
                if (!parameterTypes[1].equals(File.class)) {
                    break;
                }
                if (!parameterTypes[2].isInterface()) {
                    break;
                }
                if (level == 3) {
                    LOGGER.warn("Two constructors with two args are available", level);
                    break;
                }
                level = 3;
                bestConstructor = constructor;
                bestItfType = parameterTypes[2];
                break;
            default:
            }
        }
        switch (level) {
        case 0:
            return bestConstructor.newInstance();
        case 1:
            return bestConstructor.newInstance(home);
        case 2:
            return bestConstructor.newInstance(home, data);
        case 3:
            return bestConstructor
                    .newInstance(home, data, VestigeSystemInvocationHandler.createProxy(classLoader, bestItfType, vestigeSystem, System.getSecurityManager() != null));
        default:
            throw new ApplicationException("No constructor found");
        }
    }

    public void start(final ApplicationContext applicationContext, final Object runMutex, final Object constructorMutex) throws ApplicationException {
        try {
            final int attach;
            final PublicVestigeSystem vestigeSystem;
            final VestigeClassLoader<AttachedVestigeClassLoader> classLoader;
            RuntimeApplicationContext previousRuntimeApplicationContext = applicationContext.getRuntimeApplicationContext();
            final ClassLoaderConfiguration resolve = applicationContext.getResolve();
            final RuntimeApplicationContext finalPreviousRuntimeApplicationContext;
            if (resolve.isAttachmentScoped() || previousRuntimeApplicationContext == null) {
                previousRuntimeApplicationContext = null;
                attach = vestigePlatform.attach(resolve);
                classLoader = vestigePlatform.getClassLoader(attach);
                if (applicationContext.isPrivateSystem()) {
                    vestigeSystem = rootVestigeSystem.createSubSystem();
                } else {
                    vestigeSystem = rootVestigeSystem;
                }
                finalPreviousRuntimeApplicationContext = null;
            } else {
                // reattach to platform
                attach = vestigePlatform.attach(previousRuntimeApplicationContext.getClassLoader());
                vestigeSystem = previousRuntimeApplicationContext.getVestigeSystem();
                classLoader = previousRuntimeApplicationContext.getClassLoader();
                finalPreviousRuntimeApplicationContext = previousRuntimeApplicationContext;
            }

            Set<Permission> additionnalPermissions = new HashSet<Permission>();
            additionnalPermissions.addAll(resolve.getPermissions());
            additionnalPermissions.add(new FilePermission(applicationContext.getBase().getPath(), "read,write"));
            additionnalPermissions.add(new FilePermission(applicationContext.getBase().getPath() + File.separator + "-", "read,write,delete"));
            additionnalPermissions.addAll(applicationContext.getPermissions());
            additionnalPermissions.add(new FilePermission(applicationContext.getData().getPath(), "read,write"));
            additionnalPermissions.add(new FilePermission(applicationContext.getData().getPath() + File.separator + "-", "read,write,delete"));
            VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(classLoader, additionnalPermissions, null, applicationContext.getName(), vestigeSystem, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    final Runnable runnable;
                    RuntimeApplicationContext runtimeApplicationContext;
                    if (finalPreviousRuntimeApplicationContext == null) {
                        runnable = (Runnable) callConstructor(classLoader, classLoader.loadClass(applicationContext.getClassName()), applicationContext.getBase(),
                                applicationContext.getData(), vestigeSystem);
                        runtimeApplicationContext = new RuntimeApplicationContext(classLoader, runnable, vestigeSystem, runMutex == null);
                        if (constructorMutex != null) {
                            synchronized (constructorMutex) {
                                applicationContext.setRuntimeApplicationContext(runtimeApplicationContext);
                                constructorMutex.notify();
                            }
                        } else {
                            applicationContext.setRuntimeApplicationContext(runtimeApplicationContext);
                        }
                        classLoader.getData().addObject(new SoftReference<RuntimeApplicationContext>(runtimeApplicationContext));
                    } else {
                        runtimeApplicationContext = finalPreviousRuntimeApplicationContext;
                        runnable = runtimeApplicationContext.getRunnable();
                    }
                    if (runMutex != null) {
                        synchronized (runMutex) {
                            while (!runtimeApplicationContext.isRunAllowed()) {
                                runMutex.wait();
                            }
                        }
                    }
                    runnable.run();
                    return null;
                }
            }, managerVestigeSystem, new FutureDoneHandler<Void>() {

                @Override
                public void futureDone(final Future<Void> future) {
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        LOGGER.error("Unexpected InterruptedException", e);
                    } catch (ExecutionException e) {
                        LOGGER.error("Application ended with exception", e.getCause());
                    }
                    vestigePlatform.detach(attach);
                    // allow inner start to run
                    if (constructorMutex != null) {
                        synchronized (constructorMutex) {
                            applicationContext.setVestigeSecureExecution(null);
                            constructorMutex.notify();
                        }
                    } else {
                        applicationContext.setVestigeSecureExecution(null);
                    }
                    // allow external start to run
                    applicationContext.setStarted(false);
                    // notify after stop
                    synchronized (applicationManagerStateListeners) {
                        if (applicationManagerStateListeners.size() != 0) {
                            synchronized (state) {
                                lastState = state.copy();
                            }
                            applicationManagerStateListeners.notify();
                        }
                    }
                }
            });
            applicationContext.setVestigeSecureExecution(vestigeSecureExecution);
            applicationContext.setStarted(true);
            // notify after start
            synchronized (applicationManagerStateListeners) {
                if (applicationManagerStateListeners.size() != 0) {
                    synchronized (state) {
                        lastState = state.copy();
                    }
                    applicationManagerStateListeners.notify();
                }
            }
            vestigeSecureExecution.start();
        } catch (Exception e) {
            throw new ApplicationException("Unable to start", e);
        }
    }

    public void stop(final ApplicationContext applicationContext) throws ApplicationException {
        VestigeSecureExecution<Void> vestigeSecureExecution = applicationContext.getVestigeSecureExecution();
        if (vestigeSecureExecution == null) {
            // already stopped
            return;
        }
        vestigeSecureExecution.interrupt();
        try {
            vestigeSecureExecution.join();
        } catch (InterruptedException e) {
            throw new ApplicationException("Unable to stop", e);
        }
    }

    /**
     * Stop all applications without modifing its states.
     */
    public void stopAll() {
        for (ApplicationContext applicationContext : state.getApplicationContexts()) {
            if (applicationContext.isStarted()) {
                try {
                    stop(applicationContext);
                } catch (ApplicationException e) {
                    LOGGER.error("Unable to stop", e);
                }
            }
        }
        this.vestigeSecureExecutor.stop();
    }

    public void autoStart() {
        List<ApplicationContext> autoStartApplicationContext = new ArrayList<ApplicationContext>();

        for (ApplicationContext applicationContext : state.getApplicationContexts()) {
            applicationContext.setStarted(false);
            if (applicationContext.isAutoStarted()) {
                autoStartApplicationContext.add(applicationContext);
            }
        }
        for (ApplicationContext applicationContext : autoStartApplicationContext) {
            try {
                start(applicationContext);
            } catch (ApplicationException e) {
                LOGGER.error("Unable to start", e);
            }
        }
    }

    /**
     * @author Gael Lalire
     */
    private class AutoMigrateJob implements Job {

        @Override
        public void run(final JobHelper jobHelper) throws Exception {
            final List<String> failedMigration = Collections.synchronizedList(new ArrayList<String>());
            Set<String> applicationNames;
            synchronized (state) {
                // create a new set because migration cause concurrent
                // modification
                applicationNames = new TreeSet<String>(state.getApplicationsName());
            }
            List<Thread> threads = new ArrayList<Thread>(applicationNames.size());
            for (final String applicationName : applicationNames) {
                Thread thread = new Thread("automigrate-" + applicationName) {
                    public void run() {
                        try {
                            autoMigrate(new JobHelper() {

                                @Override
                                public TaskHelper addTask(final String taskDescription) {
                                    return jobHelper.addTask("[" + applicationName + "] " + taskDescription);
                                }
                            }, applicationName);
                        } catch (ApplicationException e) {
                            failedMigration.add(applicationName);
                            LOGGER.warn("Unable to auto-migrate " + applicationName, e);
                        }
                    }
                };
                threads.add(thread);
                thread.start();
            }
            try {
                for (Thread thread : threads) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted auto-migrate ", e);
                for (Thread thread : threads) {
                    thread.interrupt();
                }
                return;
            }
            if (failedMigration.size() != 0) {
                throw new ApplicationException("Following migration failed " + failedMigration);
            }
        }

    }

    public JobController autoMigrate(final JobListener jobListener) throws ApplicationException {
        return actionManager.submitJob("automigrate", "Automatic migration", new AutoMigrateJob(), jobListener);
    }

    public JobController autoMigrate(final String installName, final JobListener jobListener) throws ApplicationException {
        return actionManager.submitJob("automigrate", "Automatic migration", new Job() {

            @Override
            public void run(final JobHelper jobHelper) throws Exception {
                autoMigrate(jobHelper, installName);
            }
        }, jobListener);
    }

    public void autoMigrate(final JobHelper jobHelper, final String installName) throws ApplicationException {
        ApplicationContext applicationContext;
        URL context;
        String repoName;
        synchronized (state) {
            applicationContext = state.getApplication(installName);
            repoName = applicationContext.getRepoName();
            applicationContext.setMigrationRepoApplicationVersion(LOCKED_VERSION);
            context = state.getRepositoryURL(repoName);
        }
        String appName = applicationContext.getRepoApplicationName();
        List<Integer> version = applicationContext.getRepoApplicationVersion();
        int autoMigrateLevel = applicationContext.getAutoMigrateLevel();
        int majorVersion = version.get(0);
        int minorVersion = version.get(1);
        int bugfixVersion = version.get(2);
        List<Integer> newerVersion = Arrays.asList(majorVersion, minorVersion, bugfixVersion);
        switch (autoMigrateLevel) {
        case 3:
            newerVersion.set(0, majorVersion + 1);
            newerVersion.set(1, 0);
            newerVersion.set(2, 0);
            if (applicationDescriptorFactory.hasApplicationDescriptor(context, repoName, appName, newerVersion)) {
                minorVersion = 0;
                bugfixVersion = 0;
                do {
                    majorVersion++;
                    newerVersion.set(0, majorVersion + 1);
                } while (applicationDescriptorFactory.hasApplicationDescriptor(context, repoName, appName, newerVersion));
            } else {
                newerVersion.set(2, bugfixVersion);
            }
            newerVersion.set(0, majorVersion);
        case 2:
            newerVersion.set(1, minorVersion + 1);
            newerVersion.set(2, 0);
            if (applicationDescriptorFactory.hasApplicationDescriptor(context, repoName, appName, newerVersion)) {
                bugfixVersion = 0;
                do {
                    minorVersion++;
                    newerVersion.set(1, minorVersion + 1);
                } while (applicationDescriptorFactory.hasApplicationDescriptor(context, repoName, appName, newerVersion));
            }
            newerVersion.set(1, minorVersion);
        case 1:
            newerVersion.set(2, bugfixVersion + 1);
            if (applicationDescriptorFactory.hasApplicationDescriptor(context, repoName, appName, newerVersion)) {
                do {
                    bugfixVersion++;
                    newerVersion.set(2, bugfixVersion + 1);
                } while (applicationDescriptorFactory.hasApplicationDescriptor(context, repoName, appName, newerVersion));
            }
            newerVersion.set(2, bugfixVersion);
        case 0:
            break;
        default:
            throw new ApplicationException("Unexpected autoMigrateLevel" + autoMigrateLevel);
        }
        if (!newerVersion.equals(version)) {
            new MigrateAction(applicationContext, applicationContext.getName(), newerVersion, true, true).run(jobHelper);
        } else {
            synchronized (state) {
                applicationContext.setMigrationRepoApplicationVersion(null);
            }
        }
    }

    public void setAutoMigrateLevel(final String installName, final int level) throws ApplicationException {
        if (level < 0 || level > 3) {
            throw new ApplicationException("Level must be an integer between 0 and 3");
        }
        synchronized (state) {
            ApplicationContext applicationContext = state.getApplication(installName);
            applicationContext.setAutoMigrateLevel(level);
            saveState();
        }
        LOGGER.info("Application {} auto-migrate-level set to {}", installName, level);
    }

    @Override
    public void setAutoStarted(final String installName, final boolean autostart) throws ApplicationException {
        synchronized (state) {
            final ApplicationContext applicationContext = state.getApplication(installName);
            applicationContext.setAutoStarted(autostart);
            saveState();
        }
        if (autostart) {
            LOGGER.info("Application {} auto start enabled", installName);
        } else {
            LOGGER.info("Application {} auto start disabled", installName);
        }
    }

    public void createRepository(final String name, final URL url) throws ApplicationException {
        synchronized (state) {
            state.createRepository(name, url);
            saveState();
        }
        LOGGER.info("Repository {} with url {} created", name, url);
    }

    public void removeRepository(final String name) throws ApplicationException {
        synchronized (state) {
            state.removeRepository(name);
            saveState();
        }
        LOGGER.info("Repository {} removed", name);
    }

    @Override
    public ApplicationManagerState copyState() throws ApplicationException {
        synchronized (state) {
            return state.copy();
        }
    }

    @Override
    public Set<String> getApplicationsName() throws ApplicationException {
        synchronized (state) {
            return state.getApplicationsName();
        }
    }

    @Override
    public boolean isStarted(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.isStarted(installName);
        }
    }

    @Override
    public ClassLoaderConfiguration getClassLoaders(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getClassLoaders(installName);
        }
    }

    @Override
    public String getRepositoryName(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getRepositoryName(installName);
        }
    }

    @Override
    public List<Integer> getMigrationRepositoryApplicationVersion(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getMigrationRepositoryApplicationVersion(installName);
        }
    }

    @Override
    public int getAutoMigrateLevel(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getAutoMigrateLevel(installName);
        }
    }

    @Override
    public boolean isAutoStarted(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.isAutoStarted(installName);
        }
    }

    @Override
    public String getRepositoryApplicationName(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getRepositoryApplicationName(installName);
        }
    }

    @Override
    public List<Integer> getRepositoryApplicationVersion(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getRepositoryApplicationVersion(installName);
        }
    }

    public URL getRepositoryURL(final String repoName) {
        synchronized (state) {
            return state.getRepositoryURL(repoName);
        }
    }

    public Set<String> getRepositoriesName() {
        synchronized (state) {
            return state.getRepositoriesName();
        }
    }

    @Override
    public ApplicationRepositoryMetadata getRepositoryMetadata(final String repoName) {
        return applicationDescriptorFactory.getMetadata(getRepositoryURL(repoName));
    }

    public void startStateListenerThread() {
        stateListenerThread = new Thread("DefaultApplicationManager-stateListener") {
            @Override
            public void run() {
                try {
                    while (true) {
                        ApplicationManagerState state;
                        synchronized (applicationManagerStateListeners) {
                            while (lastState == null) {
                                applicationManagerStateListeners.wait();
                            }
                            state = lastState;
                            lastState = null;
                        }
                        for (ApplicationManagerStateListener applicationManagerStateListener : applicationManagerStateListeners) {
                            applicationManagerStateListener.stateChanged(state);
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.trace("State listener interrupted", e);
                }
            }
        };
        stateListenerThread.start();
    }

    public void stopStateListenerThread() throws InterruptedException {
        stateListenerThread.interrupt();
        stateListenerThread.join();
        stateListenerThread = null;
    }

    @Override
    public void addStateListener(final ApplicationManagerStateListener listener) {
        synchronized (state) {
            applicationManagerStateListeners.add(listener);
        }
    }

    @Override
    public void removeStateListener(final ApplicationManagerStateListener listener) {
        synchronized (state) {
            applicationManagerStateListeners.remove(listener);
        }
    }

    @Override
    public void open(final File file) {
        // TODO Auto-generated method stub

    }

    @Override
    public void open(final URL url) {
        // TODO Auto-generated method stub

    }

}
