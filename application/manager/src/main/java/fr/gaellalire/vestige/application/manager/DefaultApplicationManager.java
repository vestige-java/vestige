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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.application.manager.proxy.ProxyInvocationHandler;
import fr.gaellalire.vestige.job.Job;
import fr.gaellalire.vestige.job.JobController;
import fr.gaellalire.vestige.job.JobListener;
import fr.gaellalire.vestige.job.JobManager;
import fr.gaellalire.vestige.spi.job.AbstractJobHelper;
import fr.gaellalire.vestige.spi.job.JobHelper;
import fr.gaellalire.vestige.spi.job.TaskHelper;
import fr.gaellalire.vestige.spi.resolver.AttachableClassLoader;
import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.VestigeResolver;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.spi.trust.PGPPrivatePart;
import fr.gaellalire.vestige.spi.trust.PGPTrustSystem;
import fr.gaellalire.vestige.spi.trust.Signature;
import fr.gaellalire.vestige.spi.trust.TrustException;
import fr.gaellalire.vestige.spi.trust.TrustSystemAccessor;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;
import fr.gaellalire.vestige.system.PrivateVestigeSecurityManager;
import fr.gaellalire.vestige.system.VestigeSystemJarURLConnection;
import fr.gaellalire.vestige.utils.FileUtils;

/**
 * @author Gael Lalire
 */
public class DefaultApplicationManager implements ApplicationManager, CompatibilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultApplicationManager.class);

    static {
        // ensure classes used in application context are already loaded
        ProxyInvocationHandler.init();
        VestigeSystemJarURLConnection.init();
        ApplicationInstallerInvoker.class.getName();
    }

    private VestigeSystem rootVestigeSystem;

    private ApplicationRepositoryManager applicationDescriptorFactory;

    private File appConfigFile;

    private File appDataFile;

    private File appCacheFile;

    private DefaultApplicationManagerState state;

    private VestigeSecureExecutor vestigeSecureExecutor;

    private File resolverFile;

    private File nextResolverFile;

    private boolean applicationManagerStateListenersChanged;

    private List<ApplicationManagerStateListener> applicationManagerStateListeners;

    private ApplicationManagerState lastState;

    private Thread stateListenerThread;

    private int currentJavaSpecificationVersion;

    private List<VestigeResolver> vestigeResolvers;

    private Map<String, Object> injectableByClassName;

    private TrustSystemAccessor trustSystemAccessor;

    public DefaultApplicationManager(final JobManager actionManager, final File appConfigFile, final File appDataFile, final File appCacheFile, final VestigeSystem vestigeSystem,
            final VestigeSystem managerVestigeSystem, final PrivateVestigePolicy vestigePolicy, final PrivateVestigeSecurityManager vestigeSecurityManager,
            final ApplicationRepositoryManager applicationDescriptorFactory, final File resolverFile, final File nextResolverFile, final List<VestigeResolver> vestigeResolvers,
            final Map<String, Object> injectableByClassName, final TrustSystemAccessor trustSystemAccessor) {
        this.jobManager = actionManager;
        this.appConfigFile = appConfigFile;
        this.appDataFile = appDataFile;
        this.appCacheFile = appCacheFile;
        this.vestigeResolvers = vestigeResolvers;
        this.injectableByClassName = injectableByClassName;

        this.rootVestigeSystem = vestigeSystem;
        this.vestigeSecureExecutor = new VestigeSecureExecutor(vestigeSecurityManager, vestigePolicy, managerVestigeSystem);
        this.applicationDescriptorFactory = applicationDescriptorFactory;
        this.resolverFile = resolverFile;
        this.nextResolverFile = nextResolverFile;
        state = new DefaultApplicationManagerState();
        applicationManagerStateListeners = new ArrayList<ApplicationManagerStateListener>();
        String javaSpecificationVersion = System.getProperty("java.specification.version");
        if (javaSpecificationVersion == null) {
            LOGGER.warn("Property java.specification.version is not set, will accept all version");
        } else {
            try {
                if (javaSpecificationVersion.startsWith("1.")) {
                    currentJavaSpecificationVersion = Integer.parseInt(javaSpecificationVersion.substring("1.".length()));
                } else {
                    currentJavaSpecificationVersion = Integer.parseInt(javaSpecificationVersion);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Could not parse java.specification.version, will accept all version");
            }
        }
        this.trustSystemAccessor = trustSystemAccessor;
    }

    public AttachedClassLoader attach(final boolean trustedOnly, final AttachmentContext<?> attachmentContext)
            throws ApplicationException, ResolverException, InterruptedException {
        ResolvedClassLoaderConfiguration resolve = attachmentContext.getResolve();
        VerificationMetadata verificationMetadata = attachmentContext.getVerificationMetadata();
        String text = null;
        if (verificationMetadata != null) {
            text = verificationMetadata.getText();
        }
        if (text != null) {
            text = text.trim();
            if (trustedOnly) {
                String pgpSignature = verificationMetadata.getPgpSignature();
                if (pgpSignature != null) {
                    try {
                        Signature loadSignature = trustSystemAccessor.getPGPTrustSystem().loadSignature(new ByteArrayInputStream(Base64.decodeBase64(pgpSignature)));
                        if (!loadSignature.verify(new ByteArrayInputStream(text.getBytes("UTF-8")))) {
                            throw new ApplicationException("Unable to verify signature");
                        }
                        if (!loadSignature.getPublicPart().isTrusted()) {
                            throw new ApplicationException("Signature verified but not trusted");
                        }
                    } catch (TrustException e) {
                        throw new ApplicationException("Trust exception", e);
                    } catch (UnsupportedEncodingException e) {
                        // unpossible
                    }
                } else {
                    throw new ApplicationException("No signature found");
                }
            }
            return resolve.verifiedAttach(text);
        } else if (trustedOnly) {
            throw new ApplicationException("No verification metadata found");
        } else {
            return resolve.attach();
        }
    }

    private ApplicationResolvedClassLoaderConfiguration readResolvedClassLoaderConfiguration(final ObjectInputStream objectInputStream) throws IOException {
        int resolverIndex = objectInputStream.readInt();
        if (resolverIndex == -1) {
            return null;
        }
        ResolvedClassLoaderConfiguration restoreSavedResolvedClassLoaderConfiguration = vestigeResolvers.get(resolverIndex)
                .restoreSavedResolvedClassLoaderConfiguration(objectInputStream);
        return new ApplicationResolvedClassLoaderConfiguration(restoreSavedResolvedClassLoaderConfiguration, resolverIndex);
    }

    private DefaultApplicationManagerState readDefaultApplicationManagerState(final File file) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file)) {
            protected java.lang.Class<?> resolveClass(final java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
            }
        };
        try {
            DefaultApplicationManagerState defaultApplicationManagerState = (DefaultApplicationManagerState) objectInputStream.readObject();
            for (ApplicationContext applicationContext : defaultApplicationManagerState.getApplicationContexts()) {
                ApplicationContext.setResolve(applicationContext.getInstallerAttachmentContext(), readResolvedClassLoaderConfiguration(objectInputStream));
                ApplicationContext.setResolve(applicationContext.getLauncherAttachmentContext(), readResolvedClassLoaderConfiguration(objectInputStream));
            }
            return defaultApplicationManagerState;
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
            } else {
                state.createRepository("gls", new URL("https://gaellalire.fr/vestige/repository/"));
            }
        } catch (Exception e) {
            throw new ApplicationException("Unable to restore old state", e);
        }
    }

    public void saveApplicationResolvedClassLoaderConfiguration(final ObjectOutputStream objectOutputStream,
            final ApplicationResolvedClassLoaderConfiguration applicationResolvedClassLoaderConfiguration) throws IOException {
        if (applicationResolvedClassLoaderConfiguration == null) {
            objectOutputStream.writeInt(-1);
            return;
        }
        objectOutputStream.writeInt(applicationResolvedClassLoaderConfiguration.getResolverIndex());
        applicationResolvedClassLoaderConfiguration.save(objectOutputStream);
    }

    public void saveState() throws ApplicationException {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(nextResolverFile));
            try {
                objectOutputStream.writeObject(state);
                for (ApplicationContext applicationContext : state.getApplicationContexts()) {
                    saveApplicationResolvedClassLoaderConfiguration(objectOutputStream, ApplicationContext.getResolve(applicationContext.getInstallerAttachmentContext()));
                    saveApplicationResolvedClassLoaderConfiguration(objectOutputStream, ApplicationContext.getResolve(applicationContext.getLauncherAttachmentContext()));
                }
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

    public ApplicationContext createApplicationContext(final RepositoryOverride repositoryOverride, final URL repoURL, final String appName, final List<Integer> version,
            final String installName, final boolean trusted, final JobHelper jobHelper) throws ApplicationException {
        ApplicationDescriptor applicationDescriptor = applicationDescriptorFactory.createApplicationDescriptor(repositoryOverride, repoURL, appName, version, jobHelper);

        String javaSpecificationVersion = applicationDescriptor.getJavaSpecificationVersion();
        if (!isJavaSpecificationVersionCompatible(javaSpecificationVersion)) {
            throw new ApplicationException("This JVM does not support java " + javaSpecificationVersion);
        }

        File configFile = new File(appConfigFile, installName);
        File dataFile = new File(appDataFile, installName);
        File cacheFile = new File(appCacheFile, installName);

        InstallerAttachmentDescriptor installerAttachmentDescriptor = applicationDescriptor.getInstallerAttachmentDescriptor();

        Set<List<Integer>> supportedMigrationVersion;
        Set<List<Integer>> uninterruptedMigrationVersion;
        if (installerAttachmentDescriptor != null) {
            supportedMigrationVersion = installerAttachmentDescriptor.getSupportedMigrationVersions();
            uninterruptedMigrationVersion = installerAttachmentDescriptor.getUninterruptedMigrationVersions();
            if (!supportedMigrationVersion.containsAll(uninterruptedMigrationVersion)) {
                throw new ApplicationException("Some migration are uninterrupted but not supported");
            }
        } else {
            supportedMigrationVersion = Collections.emptySet();
            uninterruptedMigrationVersion = Collections.emptySet();
        }

        ApplicationContext applicationContext = new ApplicationContext();

        LauncherAttachmentDescriptor launcherAttachmentDescriptor = applicationDescriptor.getLauncherAttachmentDescriptor();
        AttachmentContext<RuntimeApplicationContext> launcherAttachmentContext = createAttachmentContext(launcherAttachmentDescriptor, installName, " launcher",
                RuntimeApplicationContext.class);
        launcherAttachmentContext.setClassName(launcherAttachmentDescriptor.getClassName());
        launcherAttachmentContext.setPrivateSystem(launcherAttachmentDescriptor.isPrivateSystem());

        applicationContext.setLauncherAttachmentContext(launcherAttachmentContext);
        AttachmentContext<RuntimeApplicationInstallerContext> installerAttachmentContext = createAttachmentContext(installerAttachmentDescriptor, installName, " installer",
                RuntimeApplicationInstallerContext.class);
        if (installerAttachmentDescriptor != null) {
            installerAttachmentContext.setClassName(installerAttachmentDescriptor.getClassName());
            installerAttachmentContext.setPrivateSystem(installerAttachmentDescriptor.isPrivateSystem());
        }
        applicationContext.setInstallerAttachmentContext(installerAttachmentContext);

        applicationContext.setTrusted(trusted);
        applicationContext.setRepoURL(repoURL);
        applicationContext.setRepoApplicationName(appName);
        applicationContext.setRepoApplicationVersion(version);

        applicationContext.setConfig(configFile);
        applicationContext.setData(dataFile);
        applicationContext.setCache(cacheFile);
        applicationContext.setSupportedMigrationVersion(supportedMigrationVersion);
        applicationContext.setUninterruptedMigrationVersion(uninterruptedMigrationVersion);
        applicationContext.setName(installName);

        applicationContext.setOverrideURL(repositoryOverride);

        return applicationContext;
    }

    public <RuntimeContext> AttachmentContext<RuntimeContext> createAttachmentContext(final AttachmentDescriptor attachmentDescriptor, final String installName,
            final String attachmentSuffix, final Class<RuntimeContext> clazz) throws ApplicationException {
        if (attachmentDescriptor == null) {
            return null;
        }
        AttachmentContext<RuntimeContext> attachmentContext = new AttachmentContext<RuntimeContext>();

        attachmentContext.setResolve(attachmentDescriptor.getClassLoaderConfiguration(installName + " installer"));
        attachmentContext.setPermissions(attachmentDescriptor.getPermissions());
        attachmentContext.setAddInjects(attachmentDescriptor.getAddInjects());
        attachmentContext.setVerificationMetadata(attachmentDescriptor.getVerificationMetadata());

        return attachmentContext;
    }

    private Set<String> lockedInstallNames = new HashSet<String>();

    private JobManager jobManager;

    public JobController install(final RepositoryOverride repositoryOverride, final URL repoURL, final String appName, final List<Integer> version, final String pinstallName,
            final boolean trusted, final JobListener jobListener) throws ApplicationException {
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
        return jobManager.submitJob("install", "Installing " + installName, new InstallAction(repositoryOverride, repoURL, appName, version, installName, trusted), jobListener);
    }

    /**
     * @author Gael Lalire
     */
    private class InstallAction implements Job {

        private RepositoryOverride repositoryOverride;

        private URL repoURL;

        private String appName;

        private List<Integer> version;

        private String installName;

        private boolean trusted;

        InstallAction(final RepositoryOverride repositoryOverride, final URL repoURL, final String appName, final List<Integer> version, final String installName,
                final boolean trusted) {
            this.repositoryOverride = repositoryOverride;
            this.repoURL = repoURL;
            this.appName = appName;
            this.version = version;
            this.installName = installName;
            this.trusted = trusted;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            ApplicationContext applicationContext = null;

            boolean successful = false;
            try {
                applicationContext = createApplicationContext(repositoryOverride, repoURL, appName, version, installName, trusted, jobHelper);
                final File config = applicationContext.getConfig();
                if (config.exists()) {
                    try {
                        FileUtils.forceDelete(config);
                    } catch (IOException e) {
                        throw new ApplicationException("Config directory already exists and cannot be deleted", e);
                    }
                }
                config.mkdirs();
                final File data = applicationContext.getData();
                if (data.exists()) {
                    try {
                        FileUtils.forceDelete(data);
                    } catch (IOException e) {
                        throw new ApplicationException("Data directory already exists and cannot be deleted", e);
                    }
                }
                data.mkdirs();
                final File cache = applicationContext.getCache();
                if (cache.exists()) {
                    try {
                        FileUtils.forceDelete(cache);
                    } catch (IOException e) {
                        throw new ApplicationException("Data directory already exists and cannot be deleted", e);
                    }
                }
                cache.mkdirs();

                final RuntimeApplicationInstallerContext finalRuntimeApplicationInstallerContext;
                TaskHelper task = jobHelper.addTask("Attaching installer classLoader");
                AttachmentContext<RuntimeApplicationInstallerContext> installerAttachmentContext = applicationContext.getInstallerAttachmentContext();
                AttachedClassLoader installerAttach;
                try {
                    if (installerAttachmentContext == null) {
                        successful = true;
                        finalRuntimeApplicationInstallerContext = null;
                        installerAttach = null;
                    } else {
                        // attach
                        installerAttach = attach(trusted, installerAttachmentContext);

                        finalRuntimeApplicationInstallerContext = createRuntimeApplicationInstallerContext(applicationContext,
                                installerAttachmentContext.getResolve().getPermissions(), installerAttach.getAttachableClassLoader(), installName);

                        installerAttach.getAttachableClassLoader().addSoftReferenceObject(finalRuntimeApplicationInstallerContext);
                    }
                } finally {
                    task.setDone();
                }
                if (!successful) {
                    try {
                        final ClassLoader installerClassLoader = installerAttach.getAttachableClassLoader().getClassLoader();

                        final VestigeSystem vestigeSystem = finalRuntimeApplicationInstallerContext.getVestigeSystem();

                        final String installerClassName = installerAttachmentContext.getClassName();
                        final List<AddInject> addInjects = installerAttachmentContext.getAddInjects();
                        VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader,
                                finalRuntimeApplicationInstallerContext.getInstallerAdditionnalPermissions(), null, applicationContext.getName() + "-installer", vestigeSystem,
                                new VestigeSecureCallable<Void>() {

                                    @Override
                                    public Void call(final PrivilegedExceptionActionExecutor privilegedExecutor) throws Exception {
                                        ApplicationInstaller applicationInstaller = new ApplicationInstallerInvoker(callConstructorAndInject(installerClassLoader,
                                                installerClassLoader.loadClass(installerClassName), config, data, cache, addInjects, vestigeSystem, privilegedExecutor));
                                        applicationInstaller.install();
                                        return null;
                                    }

                                }, null);
                        task = jobHelper.addTask("Calling install method");
                        vestigeSecureExecution.start();
                        try {
                            vestigeSecureExecution.get();
                        } finally {
                            task.setDone();
                        }
                    } finally {
                        installerAttach.detach();
                    }
                    successful = true;
                }
            } catch (Exception e) {
                throw new ApplicationException("Fail to install", e);
            } finally {
                synchronized (state) {
                    lockedInstallNames.remove(installName);
                    if (successful) {
                        state.install(installName, applicationContext);
                        saveState();
                    }
                }
            }

            LOGGER.info("Application {} installed", installName);
        }
    }

    // public static final List<Integer> LOCKED_VERSION = Collections.emptyList();

    @Override
    public JobController reloadDescriptor(final String installName, final JobListener jobListener) throws ApplicationException {
        final ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getUnlockedApplicationContext(installName);
            if (applicationContext.isStarted()) {
                throw new ApplicationException("Application is started");
            }
            applicationContext.setLocked(true);
        }
        return jobManager.submitJob("reloadDescriptor", "Reloading descriptor of " + installName, new ReloadDescriptorAction(applicationContext, installName), jobListener);
    }

    /**
     * @author Gael Lalire
     */
    private class ReloadDescriptorAction implements Job {

        private ApplicationContext applicationContext;

        private String installName;

        ReloadDescriptorAction(final ApplicationContext applicationContext, final String installName) {
            this.applicationContext = applicationContext;
            this.installName = installName;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            ApplicationContext reloadedApplicationContext = null;
            try {
                reloadedApplicationContext = createApplicationContext(applicationContext.getOverrideURL(), applicationContext.getRepoURL(),
                        applicationContext.getRepoApplicationName(), applicationContext.getRepoApplicationVersion(), installName, applicationContext.isTrusted(), jobHelper);
            } finally {
                if (reloadedApplicationContext != null) {
                    synchronized (state) {
                        state.install(installName, reloadedApplicationContext);
                        saveState();
                    }
                } else {
                    applicationContext.setLocked(false);
                }
            }
        }
    }

    public JobController uninstall(final String installName, final JobListener jobListener) throws ApplicationException {
        final ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getUnlockedApplicationContext(installName);
            if (applicationContext.isStarted()) {
                throw new ApplicationException("Application is started");
            }
            applicationContext.setLocked(true);
        }
        return jobManager.submitJob("uninstall", "Uninstalling " + installName, new UninstallAction(applicationContext, installName), jobListener);
    }

    /**
     * @author Gael Lalire
     */
    private class UninstallAction implements Job {

        private ApplicationContext applicationContext;

        private String installName;

        UninstallAction(final ApplicationContext applicationContext, final String installName) {
            this.applicationContext = applicationContext;
            this.installName = installName;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            try {
                final File config = applicationContext.getConfig();
                final File data = applicationContext.getData();
                final File cache = applicationContext.getCache();
                try {
                    RuntimeApplicationInstallerContext runtimeApplicationInstallerContext;
                    TaskHelper task = jobHelper.addTask("Attaching installer classLoader");
                    final AttachmentContext<RuntimeApplicationInstallerContext> installerAttachmentContext = applicationContext.getInstallerAttachmentContext();
                    AttachedClassLoader installerAttach;
                    try {
                        if (installerAttachmentContext == null) {
                            installerAttach = null;
                            runtimeApplicationInstallerContext = null;
                        } else {
                            runtimeApplicationInstallerContext = installerAttachmentContext.getRuntimeApplicationContext();
                            if (runtimeApplicationInstallerContext == null) {
                                // attach
                                installerAttach = attach(applicationContext.isTrusted(), installerAttachmentContext);

                                runtimeApplicationInstallerContext = createRuntimeApplicationInstallerContext(applicationContext,
                                        installerAttachmentContext.getResolve().getPermissions(), installerAttach.getAttachableClassLoader(), installName);
                                installerAttach.getAttachableClassLoader().addSoftReferenceObject(runtimeApplicationInstallerContext);
                            } else {
                                // reattach
                                installerAttach = runtimeApplicationInstallerContext.getClassLoader().attach();
                            }
                        }
                    } finally {
                        task.setDone();
                    }
                    if (installerAttachmentContext != null) {
                        try {
                            final ClassLoader installerClassLoader = installerAttach.getAttachableClassLoader().getClassLoader();
                            final VestigeSystem vestigeSystem = runtimeApplicationInstallerContext.getVestigeSystem();
                            final List<AddInject> addInjects = installerAttachmentContext.getAddInjects();
                            VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader,
                                    runtimeApplicationInstallerContext.getInstallerAdditionnalPermissions(), null, applicationContext.getName() + "-installer", vestigeSystem,
                                    new VestigeSecureCallable<Void>() {

                                        @Override
                                        public Void call(final PrivilegedExceptionActionExecutor privilegedExecutor) throws Exception {
                                            ApplicationInstaller applicationInstaller = new ApplicationInstallerInvoker(
                                                    callConstructorAndInject(installerClassLoader, installerClassLoader.loadClass(installerAttachmentContext.getClassName()),
                                                            config, data, cache, addInjects, vestigeSystem, privilegedExecutor));
                                            applicationInstaller.uninstall();
                                            return null;
                                        }

                                    }, null);
                            task = jobHelper.addTask("Calling uninstall method");
                            vestigeSecureExecution.start();
                            try {
                                vestigeSecureExecution.get();
                            } finally {
                                task.setDone();
                            }
                        } finally {
                            installerAttach.detach();
                        }
                    }
                } finally {
                    if (config.exists()) {
                        try {
                            FileUtils.forceDelete(config);
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
                throw new ApplicationException("Fail to uninstall properly", e);
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

    public ApplicationContext findMigratorApplicationContext(final ApplicationContext fromApplicationContext, final ApplicationContext toApplicationContext,
            final boolean ignoreIfUnsupported) throws ApplicationException {
        List<Integer> fromVersion = fromApplicationContext.getRepoApplicationVersion();
        List<Integer> toVersion = toApplicationContext.getRepoApplicationVersion();

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
            fromApplicationContext = state.getUnlockedApplicationContext(installName);
            fromApplicationContext.setLocked(true);
        }
        return jobManager.submitJob("migrate", "Migrating " + installName, new MigrateAction(fromApplicationContext, installName, toVersion, false), jobListener);
    }

    public RuntimeApplicationInstallerContext createRuntimeApplicationInstallerContext(final ApplicationContext applicationContext,
            final Collection<Permission> installerResolvePermission, final AttachableClassLoader installerClassLoader, final String installName) {
        Set<Permission> additionnalPermissions = new HashSet<Permission>();
        additionnalPermissions.addAll(installerResolvePermission);
        final File config = applicationContext.getConfig();
        final File data = applicationContext.getData();
        final File cache = applicationContext.getCache();
        AttachmentContext<RuntimeApplicationInstallerContext> installerAttachmentContext = applicationContext.getInstallerAttachmentContext();

        additionnalPermissions.add(new FilePermission(config.getPath(), "read,write"));
        additionnalPermissions.add(new FilePermission(config.getPath() + File.separator + "-", "read,write,delete"));
        additionnalPermissions.add(new FilePermission(data.getPath(), "read,write"));
        additionnalPermissions.add(new FilePermission(data.getPath() + File.separator + "-", "read,write,delete"));
        additionnalPermissions.add(new FilePermission(cache.getPath(), "read,write"));
        additionnalPermissions.add(new FilePermission(cache.getPath() + File.separator + "-", "read,write,delete"));
        additionnalPermissions.addAll(installerAttachmentContext.getResolve().getPermissions());
        additionnalPermissions.addAll(installerAttachmentContext.getPermissions().createPermissionSet());
        final VestigeSystem vestigeSystem;
        if (installerAttachmentContext.isPrivateSystem()) {
            vestigeSystem = rootVestigeSystem.createSubSystem("app-" + installName + "-installer");
        } else {
            vestigeSystem = rootVestigeSystem;
        }

        return new RuntimeApplicationInstallerContext(installerClassLoader, additionnalPermissions, vestigeSystem);
    }

    /**
     * @author Gael Lalire
     */
    private class RecoverMigrationAction implements Job {

        private ApplicationContext savedApplicationContext;

        private String installName;

        RecoverMigrationAction(final ApplicationContext savedApplicationContext, final String installName) {
            this.savedApplicationContext = savedApplicationContext;
            this.installName = installName;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            final ApplicationContext fromApplicationContext;
            final ApplicationContext toApplicationContext;
            final boolean uncommitted = savedApplicationContext.isUncommitted();
            if (uncommitted) {
                fromApplicationContext = savedApplicationContext.getMigrationApplicationContext();
                toApplicationContext = savedApplicationContext;
            } else {
                fromApplicationContext = savedApplicationContext;
                toApplicationContext = savedApplicationContext.getMigrationApplicationContext();
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Recovering migration of {} to version {} ", installName, VersionUtils.toString(toApplicationContext.getRepoApplicationVersion()));
            }
            final ApplicationContext migratedApplicationContext;
            final ApplicationContext migratorApplicationContext = findMigratorApplicationContext(fromApplicationContext, toApplicationContext, false);
            if (migratorApplicationContext == fromApplicationContext) {
                migratedApplicationContext = toApplicationContext;
            } else {
                migratedApplicationContext = fromApplicationContext;
            }

            try {
                final AttachmentContext<RuntimeApplicationInstallerContext> migratorInstallerAttachmentContext = migratorApplicationContext.getInstallerAttachmentContext();
                final RuntimeApplicationInstallerContext finalRuntimeApplicationInstallerContext;
                TaskHelper task = jobHelper.addTask("Attaching installer classLoader of version " + VersionUtils.toString(migratorApplicationContext.getRepoApplicationVersion()));
                AttachedClassLoader installerAttach;
                try {
                    RuntimeApplicationInstallerContext runtimeApplicationInstallerContext = migratorInstallerAttachmentContext.getRuntimeApplicationContext();
                    if (runtimeApplicationInstallerContext == null) {
                        // attach
                        installerAttach = attach(fromApplicationContext.isTrusted(), migratorInstallerAttachmentContext);

                        finalRuntimeApplicationInstallerContext = createRuntimeApplicationInstallerContext(migratorApplicationContext,
                                migratorInstallerAttachmentContext.getResolve().getPermissions(), installerAttach.getAttachableClassLoader(), installName);
                        installerAttach.getAttachableClassLoader().addSoftReferenceObject(finalRuntimeApplicationInstallerContext);
                    } else {
                        // reattach
                        installerAttach = runtimeApplicationInstallerContext.getClassLoader().attach();
                        finalRuntimeApplicationInstallerContext = runtimeApplicationInstallerContext;
                    }
                } finally {
                    task.setDone();
                }
                try {
                    final ClassLoader installerClassLoader = installerAttach.getAttachableClassLoader().getClassLoader();

                    Set<Permission> additionnalPermissions = new HashSet<Permission>();
                    finalRuntimeApplicationInstallerContext.addInstallerAdditionnalPermissions(additionnalPermissions);
                    additionnalPermissions.addAll(ApplicationContext.getResolve(migratedApplicationContext.getLauncherAttachmentContext()).getPermissions());

                    final VestigeSystem vestigeSystem = finalRuntimeApplicationInstallerContext.getVestigeSystem();
                    VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader, additionnalPermissions, null,
                            migratorApplicationContext.getName() + "-installer", vestigeSystem, new VestigeSecureCallable<Void>() {

                                @Override
                                public Void call(final PrivilegedExceptionActionExecutor privilegedExecutor) throws Exception {
                                    ApplicationInstaller applicationInstaller = finalRuntimeApplicationInstallerContext.getApplicationInstaller();
                                    if (applicationInstaller == null) {
                                        applicationInstaller = new ApplicationInstallerInvoker(
                                                callConstructorAndInject(installerClassLoader, installerClassLoader.loadClass(migratorInstallerAttachmentContext.getClassName()),
                                                        migratorApplicationContext.getConfig(), migratorApplicationContext.getData(), migratorApplicationContext.getCache(),
                                                        migratorInstallerAttachmentContext.getAddInjects(), vestigeSystem, privilegedExecutor));
                                        finalRuntimeApplicationInstallerContext.setApplicationInstaller(applicationInstaller);
                                    }
                                    try {
                                        TaskHelper task;
                                        if (uncommitted) {
                                            task = jobHelper.addTask("Calling commitMigration method");
                                            try {
                                                applicationInstaller.commitMigration();
                                            } finally {
                                                task.setDone();
                                            }
                                        } else {
                                            task = jobHelper.addTask("Calling cleanMigration method");
                                            try {
                                                applicationInstaller.cleanMigration();
                                            } finally {
                                                task.setDone();
                                            }
                                        }
                                    } catch (Exception e) {
                                        // uninstall because we can't decide between from and to version, this uninstall does not remove files
                                        // so user can backup them, reinstall the app in the correct version and copy/correct the backup
                                        privilegedExecutor.setPrivileged();
                                        try {
                                            synchronized (state) {
                                                state.uninstall(installName);
                                                saveState();
                                            }
                                        } finally {
                                            privilegedExecutor.unsetPrivileged();
                                        }
                                        throw e;
                                    }
                                    return null;
                                }

                            }, null);
                    vestigeSecureExecution.start();
                    vestigeSecureExecution.get();
                } finally {
                    installerAttach.detach();
                }
            } catch (Exception e) {
                throw new ApplicationException("Fail to recover migration", e);
            }
            synchronized (state) {
                if (uncommitted) {
                    toApplicationContext.setLocked(false);
                    toApplicationContext.setUncommitted(false);
                    toApplicationContext.setMigrationApplicationContext(null);
                } else {
                    fromApplicationContext.setMigrationApplicationContext(null);
                    fromApplicationContext.setLocked(false);
                }
                saveState();
            }
            if (LOGGER.isInfoEnabled()) {
                if (uncommitted) {
                    LOGGER.info("Application {} migrated to version {}", installName, VersionUtils.toString(toApplicationContext.getRepoApplicationVersion()));
                } else {
                    LOGGER.info("Application {} restored to version {}", installName, VersionUtils.toString(fromApplicationContext.getRepoApplicationVersion()));
                }
            }
        }
    }

    /**
     * @author Gael Lalire
     */
    private class MigrateAction implements Job {

        private ApplicationContext fromApplicationContext;

        private String installName;

        private List<Integer> toVersion;

        private boolean ignoreIfUnsupported;

        MigrateAction(final ApplicationContext fromApplicationContext, final String installName, final List<Integer> toVersion, final boolean ignoreIfUnsupported) {
            this.fromApplicationContext = fromApplicationContext;
            this.installName = installName;
            this.toVersion = toVersion;
            this.ignoreIfUnsupported = ignoreIfUnsupported;
        }

        @Override
        public void run(final JobHelper jobHelper) throws ApplicationException {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Migrating {} to version {} ", installName, VersionUtils.toString(toVersion));
            }
            try {
                final ApplicationContext toApplicationContext = createApplicationContext(fromApplicationContext.getOverrideURL(), fromApplicationContext.getRepoURL(),
                        fromApplicationContext.getRepoApplicationName(), toVersion, installName, fromApplicationContext.isTrusted(), jobHelper);

                int level = fromApplicationContext.getAutoMigrateLevel();
                // migration target inherits autoMigrateLevel
                toApplicationContext.setAutoMigrateLevel(level);
                toApplicationContext.setLocked(true);
                toApplicationContext.setUncommitted(true);
                fromApplicationContext.setMigrationApplicationContext(toApplicationContext);

                synchronized (state) {
                    // crash here: migration never happens
                    saveState();
                    // crash after: cleanMigration or commitMigration will be called
                }

                final ApplicationContext migratedApplicationContext;
                final ApplicationContext migratorApplicationContext = findMigratorApplicationContext(fromApplicationContext, toApplicationContext, ignoreIfUnsupported);
                if (migratorApplicationContext == null) {
                    return;
                } else if (migratorApplicationContext == fromApplicationContext) {
                    migratedApplicationContext = toApplicationContext;
                } else {
                    migratedApplicationContext = fromApplicationContext;
                }

                final RuntimeApplicationContext runtimeApplicationContext = fromApplicationContext.getLauncherAttachmentContext().getRuntimeApplicationContext();
                // && runtimeApplicationContext != null should be redundant
                if (fromApplicationContext.isStarted() && runtimeApplicationContext != null) {
                    if (!migratorApplicationContext.getUninterruptedMigrationVersion().contains(migratedApplicationContext.getRepoApplicationVersion())) {
                        if (ignoreIfUnsupported) {
                            LOGGER.info("Uninterrupted migration not supported and application running");
                            return;
                        } else {
                            throw new ApplicationException("Uninterrupted migration not supported and application running");
                        }
                    }

                    try {
                        final Object runMutex = new Object();
                        RuntimeApplicationContext toRuntimeApplicationContext = toApplicationContext.getLauncherAttachmentContext().getRuntimeApplicationContext();
                        Object constructorMutex = null;
                        if (toRuntimeApplicationContext == null) {
                            constructorMutex = new Object();
                        } else {
                            toRuntimeApplicationContext.setRunAllowed(false);
                        }
                        start(toApplicationContext, runMutex, constructorMutex, installName, false, false);
                        if (constructorMutex != null) {
                            synchronized (constructorMutex) {
                                toRuntimeApplicationContext = toApplicationContext.getLauncherAttachmentContext().getRuntimeApplicationContext();
                                while (toRuntimeApplicationContext == null && toApplicationContext.getVestigeSecureExecution() != null) {
                                    constructorMutex.wait();
                                    toRuntimeApplicationContext = toApplicationContext.getLauncherAttachmentContext().getRuntimeApplicationContext();
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

                        boolean successfulMigration = false;
                        VestigeSecureExecution<Void> fromVestigeSecureExecution = fromApplicationContext.getVestigeSecureExecution();
                        try {
                            final RuntimeApplicationInstallerContext finalRuntimeApplicationInstallerContext;
                            TaskHelper task = jobHelper
                                    .addTask("Attaching installer classLoader of version " + VersionUtils.toString(migratorApplicationContext.getRepoApplicationVersion()));
                            final AttachmentContext<RuntimeApplicationInstallerContext> migratorInstallerAttachmentContext = migratorApplicationContext
                                    .getInstallerAttachmentContext();
                            AttachedClassLoader installerAttach;
                            try {
                                RuntimeApplicationInstallerContext runtimeApplicationInstallerContext = migratorInstallerAttachmentContext.getRuntimeApplicationContext();
                                if (runtimeApplicationInstallerContext == null) {
                                    // attach
                                    installerAttach = attach(fromApplicationContext.isTrusted(), migratorInstallerAttachmentContext);

                                    finalRuntimeApplicationInstallerContext = createRuntimeApplicationInstallerContext(migratorApplicationContext,
                                            migratorInstallerAttachmentContext.getResolve().getPermissions(), installerAttach.getAttachableClassLoader(), installName);
                                    installerAttach.getAttachableClassLoader().addSoftReferenceObject(finalRuntimeApplicationInstallerContext);
                                } else {
                                    // reattach
                                    installerAttach = runtimeApplicationInstallerContext.getClassLoader().attach();
                                    finalRuntimeApplicationInstallerContext = runtimeApplicationInstallerContext;
                                }
                            } finally {
                                task.setDone();
                            }
                            try {
                                final ClassLoader installerClassLoader = installerAttach.getAttachableClassLoader().getClassLoader();

                                Set<Permission> additionnalPermissions = new HashSet<Permission>();
                                finalRuntimeApplicationInstallerContext.addInstallerAdditionnalPermissions(additionnalPermissions);
                                additionnalPermissions.addAll(ApplicationContext.getResolve(migratedApplicationContext.getLauncherAttachmentContext()).getPermissions());

                                final VestigeSystem vestigeSystem = finalRuntimeApplicationInstallerContext.getVestigeSystem();
                                VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader, additionnalPermissions,
                                        Arrays.asList(fromVestigeSecureExecution.getThreadGroup(), toApplicationContext.getVestigeSecureExecution().getThreadGroup()),
                                        migratorApplicationContext.getName() + "-installer", vestigeSystem, new VestigeSecureCallable<Void>() {

                                            @Override
                                            public Void call(final PrivilegedExceptionActionExecutor privilegedExecutor) throws Exception {
                                                ApplicationInstaller applicationInstaller = finalRuntimeApplicationInstallerContext.getApplicationInstaller();
                                                if (applicationInstaller == null) {
                                                    applicationInstaller = new ApplicationInstallerInvoker(callConstructorAndInject(installerClassLoader,
                                                            installerClassLoader.loadClass(migratorInstallerAttachmentContext.getClassName()),
                                                            migratorApplicationContext.getConfig(), migratorApplicationContext.getData(), migratorApplicationContext.getCache(),
                                                            migratorInstallerAttachmentContext.getAddInjects(), vestigeSystem, privilegedExecutor));
                                                    finalRuntimeApplicationInstallerContext.setApplicationInstaller(applicationInstaller);
                                                }
                                                Exception migrateException = null;
                                                TaskHelper task;
                                                try {
                                                    if (migratorApplicationContext == fromApplicationContext) {
                                                        task = jobHelper.addTask("Calling prepareUninterruptedMigrateTo method");
                                                        try {
                                                            applicationInstaller.prepareUninterruptedMigrateTo(runtimeApplicationContext.getApplicationCallable(), toVersion,
                                                                    notNullToRuntimeApplicationContext.getApplicationCallable(), notifyRunMutex);
                                                        } finally {
                                                            task.setDone();
                                                        }
                                                    } else {
                                                        task = jobHelper.addTask("Calling prepareUninterruptedMigrateFrom method");
                                                        try {
                                                            applicationInstaller.prepareUninterruptedMigrateFrom(fromApplicationContext.getRepoApplicationVersion(),
                                                                    runtimeApplicationContext.getApplicationCallable(), notNullToRuntimeApplicationContext.getApplicationCallable(),
                                                                    notifyRunMutex);
                                                        } finally {
                                                            task.setDone();
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    migrateException = e;
                                                }
                                                try {
                                                    if (migrateException == null) {
                                                        privilegedExecutor.setPrivileged();
                                                        try {
                                                            synchronized (state) {
                                                                fromApplicationContext.setMigrationApplicationContext(null);
                                                                toApplicationContext.setMigrationApplicationContext(fromApplicationContext);
                                                                state.install(installName, toApplicationContext);
                                                                saveState();
                                                            }
                                                        } finally {
                                                            privilegedExecutor.unsetPrivileged();
                                                        }
                                                        task = jobHelper.addTask("Calling commitMigration method");
                                                        try {
                                                            applicationInstaller.commitMigration();
                                                        } finally {
                                                            task.setDone();
                                                        }
                                                    } else {
                                                        task = jobHelper.addTask("Calling cleanMigration method");
                                                        try {
                                                            applicationInstaller.cleanMigration();
                                                        } finally {
                                                            task.setDone();
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    // uninstall because we can't decide between from and to version, this uninstall does not remove files
                                                    // so user can backup them, reinstall the app in the correct version and copy/correct the backup
                                                    privilegedExecutor.setPrivileged();
                                                    try {
                                                        synchronized (state) {
                                                            state.uninstall(installName);
                                                            saveState();
                                                        }
                                                    } finally {
                                                        privilegedExecutor.unsetPrivileged();
                                                    }
                                                    throw e;
                                                }
                                                if (migrateException != null) {
                                                    throw migrateException;
                                                }
                                                return null;
                                            }

                                        }, null);
                                vestigeSecureExecution.start();
                                vestigeSecureExecution.get();
                                notifyRunMutex.run();
                                fromVestigeSecureExecution.interrupt();
                                successfulMigration = true;
                            } finally {
                                installerAttach.detach();
                            }
                        } finally {
                            if (!successfulMigration) {
                                VestigeSecureExecution<Void> vestigeSecureExecution = toApplicationContext.getVestigeSecureExecution();
                                if (vestigeSecureExecution != null) {
                                    vestigeSecureExecution.interrupt();
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new ApplicationException("Fail to uninterrupted migrate", e);
                    }
                } else {
                    try {
                        final RuntimeApplicationInstallerContext finalRuntimeApplicationInstallerContext;
                        TaskHelper task = jobHelper
                                .addTask("Attaching installer classLoader of version " + VersionUtils.toString(migratorApplicationContext.getRepoApplicationVersion()));
                        final AttachmentContext<RuntimeApplicationInstallerContext> migratorInstallerAttachmentContext = migratorApplicationContext.getInstallerAttachmentContext();
                        AttachedClassLoader installerAttach;
                        try {
                            RuntimeApplicationInstallerContext runtimeApplicationInstallerContext = migratorInstallerAttachmentContext.getRuntimeApplicationContext();
                            if (runtimeApplicationInstallerContext == null) {
                                // attach
                                installerAttach = attach(fromApplicationContext.isTrusted(), migratorInstallerAttachmentContext);
                                finalRuntimeApplicationInstallerContext = createRuntimeApplicationInstallerContext(migratorApplicationContext,
                                        migratorInstallerAttachmentContext.getResolve().getPermissions(), installerAttach.getAttachableClassLoader(), installName);
                                installerAttach.getAttachableClassLoader()
                                        .addSoftReferenceObject(new SoftReference<RuntimeApplicationInstallerContext>(finalRuntimeApplicationInstallerContext));
                            } else {
                                // reattach
                                installerAttach = runtimeApplicationInstallerContext.getClassLoader().attach();
                                finalRuntimeApplicationInstallerContext = runtimeApplicationInstallerContext;
                            }
                        } finally {
                            task.setDone();
                        }
                        try {
                            final ClassLoader installerClassLoader = installerAttach.getAttachableClassLoader().getClassLoader();

                            Set<Permission> additionnalPermissions = new HashSet<Permission>();
                            finalRuntimeApplicationInstallerContext.addInstallerAdditionnalPermissions(additionnalPermissions);
                            additionnalPermissions.addAll(ApplicationContext.getResolve(migratedApplicationContext.getLauncherAttachmentContext()).getPermissions());

                            final VestigeSystem vestigeSystem = finalRuntimeApplicationInstallerContext.getVestigeSystem();
                            VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(installerClassLoader, additionnalPermissions, null,
                                    migratorApplicationContext.getName() + "-installer", vestigeSystem, new VestigeSecureCallable<Void>() {

                                        @Override
                                        public Void call(final PrivilegedExceptionActionExecutor privilegedExecutor) throws Exception {
                                            ApplicationInstaller applicationInstaller = finalRuntimeApplicationInstallerContext.getApplicationInstaller();
                                            if (applicationInstaller == null) {
                                                applicationInstaller = new ApplicationInstallerInvoker(callConstructorAndInject(installerClassLoader,
                                                        installerClassLoader.loadClass(migratorInstallerAttachmentContext.getClassName()), migratorApplicationContext.getConfig(),
                                                        migratorApplicationContext.getData(), migratorApplicationContext.getCache(),
                                                        migratorInstallerAttachmentContext.getAddInjects(), vestigeSystem, privilegedExecutor));
                                                finalRuntimeApplicationInstallerContext.setApplicationInstaller(applicationInstaller);
                                            }
                                            Exception migrateException = null;
                                            TaskHelper task;
                                            try {
                                                if (migratorApplicationContext == fromApplicationContext) {
                                                    task = jobHelper.addTask("Calling prepareMigrateTo method");
                                                    try {
                                                        applicationInstaller.prepareMigrateTo(toVersion);
                                                    } finally {
                                                        task.setDone();
                                                    }
                                                } else {
                                                    task = jobHelper.addTask("Calling prepareMigrateFrom method");
                                                    try {
                                                        applicationInstaller.prepareMigrateFrom(fromApplicationContext.getRepoApplicationVersion());
                                                    } finally {
                                                        task.setDone();
                                                    }
                                                }
                                            } catch (Exception e) {
                                                migrateException = e;
                                            }
                                            try {
                                                if (migrateException == null) {
                                                    privilegedExecutor.setPrivileged();
                                                    try {
                                                        synchronized (state) {
                                                            fromApplicationContext.setMigrationApplicationContext(null);
                                                            toApplicationContext.setMigrationApplicationContext(fromApplicationContext);
                                                            state.install(installName, toApplicationContext);
                                                            saveState();
                                                        }
                                                    } finally {
                                                        privilegedExecutor.unsetPrivileged();
                                                    }
                                                    task = jobHelper.addTask("Calling commitMigration method");
                                                    try {
                                                        applicationInstaller.commitMigration();
                                                    } finally {
                                                        task.setDone();
                                                    }
                                                } else {
                                                    task = jobHelper.addTask("Calling cleanMigration method");
                                                    try {
                                                        applicationInstaller.cleanMigration();
                                                    } finally {
                                                        task.setDone();
                                                    }
                                                }
                                            } catch (Exception e) {
                                                // uninstall because we can't decide between from and to version, this uninstall does not remove files
                                                // so user can backup them, reinstall the app in the correct version and copy/correct the backup
                                                privilegedExecutor.setPrivileged();
                                                try {
                                                    synchronized (state) {
                                                        state.uninstall(installName);
                                                        saveState();
                                                    }
                                                } finally {
                                                    privilegedExecutor.unsetPrivileged();
                                                }
                                                throw e;
                                            }
                                            if (migrateException != null) {
                                                throw migrateException;
                                            }
                                            return null;
                                        }

                                    }, null);
                            vestigeSecureExecution.start();
                            vestigeSecureExecution.get();
                        } finally {
                            installerAttach.detach();
                        }
                    } catch (Exception e) {
                        throw new ApplicationException("Fail to migrate", e);
                    }
                }
                synchronized (state) {
                    toApplicationContext.setLocked(false);
                    toApplicationContext.setUncommitted(false);
                    toApplicationContext.setMigrationApplicationContext(null);
                }
            } finally {
                synchronized (state) {
                    fromApplicationContext.setMigrationApplicationContext(null);
                    fromApplicationContext.setLocked(false);
                    saveState();
                }
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Application {} migrated to version {}", installName, VersionUtils.toString(toVersion));
            }
        }
    }

    public void start(final String installName, final boolean interrupted) throws ApplicationException {
        synchronized (state) {
            final ApplicationContext applicationContext = state.getUnlockedApplicationContext(installName);
            if (applicationContext.isStarted()) {
                throw new ApplicationException("already started");
            }
            start(applicationContext, installName, false, interrupted);
        }
        LOGGER.info("Application {} started", installName);
    }

    @Override
    public void run(final String installName) throws ApplicationException {
        synchronized (state) {
            final ApplicationContext applicationContext = state.getUnlockedApplicationContext(installName);
            if (applicationContext.isStarted()) {
                throw new ApplicationException("Already started");
            }
            LOGGER.info("Application {} started", installName);
            start(applicationContext, installName, true, false);
        }
    }

    @Override
    public void discard(final String installName) throws ApplicationException {
        ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getUnlockedApplicationContext(installName);
            if (applicationContext.isStarted()) {
                throw new ApplicationException("Cannot discard started application state");
            }
            ApplicationContext.setRuntimeApplicationContext(applicationContext.getLauncherAttachmentContext(), null);
            ApplicationContext.setRuntimeApplicationContext(applicationContext.getInstallerAttachmentContext(), null);
        }
    }

    public String base64Sign(final PGPPrivatePart privatePart, final String metadata) throws TrustException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            privatePart.sign(new ByteArrayInputStream(metadata.getBytes("UTF-8")), os);
        } catch (UnsupportedEncodingException e) {
            // not possible
        }
        return Base64.encodeBase64String(os.toByteArray());
    }

    @Override
    public ApplicationVerificationMetadata generateApplicationVerificationMetadata(final String installName) throws ApplicationException {
        ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getApplicationContext(installName);
        }
        try {
            AttachmentContext<?> installerAttachmentContext = applicationContext.getInstallerAttachmentContext();

            String installerVerificationMetadata = null;
            if (installerAttachmentContext != null) {
                installerVerificationMetadata = installerAttachmentContext.getResolve().createVerificationMetadata();
            }
            String launcherVerificationMetadata = applicationContext.getLauncherAttachmentContext().getResolve().createVerificationMetadata();
            return new ApplicationVerificationMetadata(launcherVerificationMetadata, installerVerificationMetadata);
        } catch (ResolverException e) {
            throw new ApplicationException("Unable to generate verification metadata", e);
        }
    }

    @Override
    public ApplicationVerificationMetadataPGPSigned pgpSign(final String installName, final String key) throws ApplicationException {
        ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getApplicationContext(installName);
        }
        try {
            PGPTrustSystem pgpTrustSystem = trustSystemAccessor.getPGPTrustSystem();
            PGPPrivatePart privatePart;
            if (key == null) {
                privatePart = pgpTrustSystem.getDefaultPrivatePart();
            } else {
                privatePart = pgpTrustSystem.getPrivatePart(key);
            }
            if (privatePart == null) {
                throw new ApplicationException("Private part not found");
            }

            AttachmentContext<?> installerAttachmentContext = applicationContext.getInstallerAttachmentContext();

            String installerVerificationMetadata = null;
            String installerBase64Signature = null;
            if (installerAttachmentContext != null) {
                installerVerificationMetadata = installerAttachmentContext.getResolve().createVerificationMetadata();
                installerBase64Signature = base64Sign(privatePart, installerVerificationMetadata);
            }
            String launcherVerificationMetadata = applicationContext.getLauncherAttachmentContext().getResolve().createVerificationMetadata();
            String launcherBase64Signature = base64Sign(privatePart, launcherVerificationMetadata);
            return new ApplicationVerificationMetadataPGPSigned(launcherVerificationMetadata, launcherBase64Signature, installerVerificationMetadata, installerBase64Signature);
        } catch (TrustException e) {
            throw new ApplicationException("Unable to sign", e);
        } catch (ResolverException e) {
            throw new ApplicationException("Unable to sign", e);
        }

    }

    public JobController stop(final String installName, final JobListener jobListener) throws ApplicationException {
        ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getUnlockedApplicationContext(installName);
        }
        final VestigeSecureExecution<Void> vestigeSecureExecution = applicationContext.getVestigeSecureExecution();
        if (vestigeSecureExecution == null) {
            // already stopped
            return null;
        }
        vestigeSecureExecution.interrupt();
        return jobManager.submitJob("stop", "Stop application", new Job() {

            @Override
            public void run(final JobHelper jobHelper) throws Exception {
                TaskHelper task = jobHelper.addTask("Waiting for application to stop");
                try {
                    vestigeSecureExecution.join();
                } catch (InterruptedException e) {
                    throw new ApplicationException("Unable to stop", e);
                } finally {
                    task.setDone();
                }
                LOGGER.info("Application {} stopped", installName);
            }
        }, jobListener);
    }

    @Override
    public JobController wait(final String installName, final JobListener jobListener) throws ApplicationException {
        ApplicationContext applicationContext;
        synchronized (state) {
            applicationContext = state.getUnlockedApplicationContext(installName);
        }
        final VestigeSecureExecution<Void> vestigeSecureExecution = applicationContext.getVestigeSecureExecution();
        if (vestigeSecureExecution == null) {
            // already stopped
            return null;
        }
        return jobManager.submitJob("stop", "Wait application", new Job() {

            @Override
            public void run(final JobHelper jobHelper) throws Exception {
                TaskHelper task = jobHelper.addTask("Waiting for application to end");
                try {
                    vestigeSecureExecution.join();
                } catch (InterruptedException e) {
                    throw new ApplicationException("Unable to wait", e);
                } finally {
                    task.setDone();
                }
                LOGGER.info("Application {} ended", installName);
            }
        }, jobListener);
    }

    public void start(final ApplicationContext applicationContext, final String installName, final boolean selfThread, final boolean interrupted) throws ApplicationException {
        if (applicationContext.getVestigeSecureExecution() != null) {
            // already started
            return;
        }
        start(applicationContext, null, null, installName, selfThread, interrupted);
    }

    public static Class<?> superSearch(final String className, final Class<?> cl) {
        for (Class<?> itf : cl.getInterfaces()) {
            if (className.equals(itf.getName())) {
                return itf;
            }
        }
        Class<?> superclass = cl.getSuperclass();
        if (superclass != null) {
            return superSearch(className, superclass);
        }
        return null;
    }

    public Object callConstructorAndInject(final ClassLoader classLoader, final Class<?> loadClass, final File config, final File data, final File cache,
            final List<AddInject> addInjects, final VestigeSystem vestigeSystem, final PrivilegedExceptionActionExecutor privilegedExecutor)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, ApplicationException {
        // level : constructor
        // 0 : ()
        // 1 : (File)
        // 2 : (File, File)
        // 3 : (File, File, File)
        // The first file is base directory, the optional second file is data directory, the optional third file is cache directory

        Object applicationObject;
        Constructor<?> constructor = null;
        try {
            constructor = loadClass.getConstructor(File.class, File.class, File.class);
            applicationObject = constructor.newInstance(config, data, cache);
        } catch (NoSuchMethodException e1) {
            try {
                constructor = loadClass.getConstructor(File.class, File.class);
                applicationObject = constructor.newInstance(config, data);
            } catch (NoSuchMethodException e2) {
                try {
                    constructor = loadClass.getConstructor(File.class);
                    applicationObject = constructor.newInstance(config);
                } catch (NoSuchMethodException e3) {
                    try {
                        constructor = loadClass.getConstructor();
                        applicationObject = constructor.newInstance();
                    } catch (NoSuchMethodException e4) {
                        throw new ApplicationException("No constructor found");
                    }
                }
            }
        }

        if (addInjects != null) {
            for (AddInject addInject : addInjects) {
                String serviceClassName = addInject.getServiceClassName();

                Object object;
                if (VestigeSystem.class.getName().equals(serviceClassName)) {
                    if (vestigeSystem == rootVestigeSystem) {
                        LOGGER.error("Cannot inject vestige system if it is not private");
                        continue;
                    }
                    object = vestigeSystem;
                } else {
                    object = injectableByClassName.get(serviceClassName);
                    if (object == null) {
                        LOGGER.error("Cannot inject " + serviceClassName);
                        continue;
                    }
                }
                Class<?> rcl;
                Method method;
                try {
                    rcl = classLoader.loadClass(addInject.getTargetServiceClassName());
                    method = loadClass.getMethod(addInject.getSetterName(), rcl);
                } catch (Exception e) {
                    LOGGER.error("Cannot inject " + serviceClassName, e);
                    continue;
                }
                if (!rcl.isInstance(object)) {
                    Class<?> cl = superSearch(serviceClassName, object.getClass());
                    object = ProxyInvocationHandler.createProxy(classLoader, cl, rcl, object, privilegedExecutor);
                }
                method.invoke(applicationObject, object);
            }
        }

        return applicationObject;
    }

    /**
     * Just to ensure that the class is loaded.
     */
    @SuppressWarnings("unused")
    private static final RunnableProxy UNUSED = new RunnableProxy(null);

    /**
     * @author Gael Lalire
     */
    private static class RunnableProxy implements Callable<Void> {

        private Runnable runnable;

        RunnableProxy(final Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Void call() throws Exception {
            runnable.run();
            return null;
        }

    }

    public void start(final ApplicationContext applicationContext, final Object runMutex, final Object constructorMutex, final String installName, final boolean selfThread,
            final boolean interrupted) throws ApplicationException {
        try {
            final AttachedClassLoader attach;
            final VestigeSystem vestigeSystem;
            final AttachableClassLoader attachableClassLoader;
            final ClassLoader classLoader;

            final AttachmentContext<RuntimeApplicationContext> launcherAttachmentContext = applicationContext.getLauncherAttachmentContext();

            RuntimeApplicationContext previousRuntimeApplicationContext = launcherAttachmentContext.getRuntimeApplicationContext();
            final ResolvedClassLoaderConfiguration resolve = launcherAttachmentContext.getResolve();
            final RuntimeApplicationContext finalPreviousRuntimeApplicationContext;
            if (resolve.isAttachmentScoped() || previousRuntimeApplicationContext == null) {
                previousRuntimeApplicationContext = null;

                attach = attach(applicationContext.isTrusted(), launcherAttachmentContext);

                attachableClassLoader = attach.getAttachableClassLoader();
                classLoader = attachableClassLoader.getClassLoader();
                if (launcherAttachmentContext.isPrivateSystem()) {
                    vestigeSystem = rootVestigeSystem.createSubSystem("app" + installName);
                } else {
                    vestigeSystem = rootVestigeSystem;
                }
                finalPreviousRuntimeApplicationContext = null;
            } else {
                // reattach to platform
                attachableClassLoader = previousRuntimeApplicationContext.getClassLoader();
                attach = attachableClassLoader.attach();
                vestigeSystem = previousRuntimeApplicationContext.getVestigeSystem();
                classLoader = attachableClassLoader.getClassLoader();
                finalPreviousRuntimeApplicationContext = previousRuntimeApplicationContext;
            }

            Set<Permission> additionnalPermissions = new HashSet<Permission>();
            additionnalPermissions.addAll(resolve.getPermissions());
            additionnalPermissions.add(new FilePermission(applicationContext.getConfig().getPath(), "read,write"));
            additionnalPermissions.add(new FilePermission(applicationContext.getConfig().getPath() + File.separator + "-", "read,write,delete"));
            additionnalPermissions.addAll(launcherAttachmentContext.getPermissions().createPermissionSet());
            additionnalPermissions.add(new FilePermission(applicationContext.getData().getPath(), "read,write"));
            additionnalPermissions.add(new FilePermission(applicationContext.getData().getPath() + File.separator + "-", "read,write,delete"));
            additionnalPermissions.add(new FilePermission(applicationContext.getCache().getPath(), "read,write"));
            additionnalPermissions.add(new FilePermission(applicationContext.getCache().getPath() + File.separator + "-", "read,write,delete"));
            VestigeSecureExecution<Void> vestigeSecureExecution = vestigeSecureExecutor.execute(classLoader, additionnalPermissions, null, applicationContext.getName(),
                    vestigeSystem, new VestigeSecureCallable<Void>() {
                        @Override
                        public Void call(final PrivilegedExceptionActionExecutor privilegedExecutor) throws Exception {
                            final Callable<?> applicationCallable;
                            RuntimeApplicationContext runtimeApplicationContext;
                            if (finalPreviousRuntimeApplicationContext == null) {
                                Class<?> cl = classLoader.loadClass(launcherAttachmentContext.getClassName());
                                Object applicationObject = callConstructorAndInject(classLoader, cl, applicationContext.getConfig(), applicationContext.getData(),
                                        applicationContext.getCache(), launcherAttachmentContext.getAddInjects(), vestigeSystem, privilegedExecutor);
                                if (applicationObject instanceof Callable<?>) {
                                    applicationCallable = (Callable<?>) applicationObject;
                                } else {
                                    final Runnable runnable = (Runnable) applicationObject;
                                    applicationCallable = new RunnableProxy(runnable);
                                }

                                runtimeApplicationContext = new RuntimeApplicationContext(attachableClassLoader, applicationCallable, vestigeSystem, runMutex == null);
                                if (constructorMutex != null) {
                                    synchronized (constructorMutex) {
                                        launcherAttachmentContext.setRuntimeApplicationContext(runtimeApplicationContext);
                                        constructorMutex.notify();
                                    }
                                } else {
                                    launcherAttachmentContext.setRuntimeApplicationContext(runtimeApplicationContext);
                                }
                                attachableClassLoader.addSoftReferenceObject(runtimeApplicationContext);
                            } else {
                                runtimeApplicationContext = finalPreviousRuntimeApplicationContext;
                                applicationCallable = runtimeApplicationContext.getApplicationCallable();
                            }
                            if (runMutex != null) {
                                synchronized (runMutex) {
                                    while (!runtimeApplicationContext.isRunAllowed()) {
                                        runMutex.wait();
                                    }
                                }
                            }
                            applicationCallable.call();
                            return null;
                        }
                    }, new FutureDoneHandler<Void>() {

                        @Override
                        public void futureDone(final Future<Void> future) {
                            try {
                                future.get();
                            } catch (InterruptedException e) {
                                LOGGER.error("Unexpected InterruptedException", e);
                            } catch (ExecutionException e) {
                                Throwable cause = e.getCause();
                                if (cause instanceof Exception) {
                                    applicationContext.setException((Exception) cause);
                                } else {
                                    applicationContext.setException(e);
                                }
                                LOGGER.error("Application ended with exception", e.getCause());
                            }
                            attach.detach();
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
                            synchronized (state) {
                                if (applicationManagerStateListeners.size() != 0) {
                                    lastState = state.copy();
                                    synchronized (applicationManagerStateListeners) {
                                        applicationManagerStateListeners.notify();
                                    }
                                }
                            }
                        }
                    }, selfThread, interrupted);
            applicationContext.setVestigeSecureExecution(vestigeSecureExecution);
            applicationContext.setStarted(true);
            applicationContext.setException(null);
            // notify after start
            synchronized (state) {
                if (applicationManagerStateListeners.size() != 0) {
                    lastState = state.copy();
                    synchronized (applicationManagerStateListeners) {
                        applicationManagerStateListeners.notify();
                    }
                }
            }
            vestigeSecureExecution.start();
        } catch (Exception e) {
            throw new ApplicationException("Unable to start", e);
        }
    }

    /**
     * Stop all applications without modifying its states.
     */
    public void stopAll() {
        List<VestigeSecureExecution<Void>> vestigeSecureExecutions = new ArrayList<VestigeSecureExecution<Void>>();
        for (ApplicationContext applicationContext : state.getApplicationContexts()) {
            if (applicationContext.isStarted()) {
                VestigeSecureExecution<Void> vestigeSecureExecution = applicationContext.getVestigeSecureExecution();
                vestigeSecureExecution.interrupt();
                vestigeSecureExecutions.add(vestigeSecureExecution);
            }
        }
        for (VestigeSecureExecution<Void> vestigeSecureExecution : vestigeSecureExecutions) {
            try {
                vestigeSecureExecution.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Stop all interrupted", e);
                return;
            }
        }
    }

    public void autoStart() {
        List<Entry<String, ApplicationContext>> autoStartApplicationContext = new ArrayList<Entry<String, ApplicationContext>>();
        autoStartApplicationContext.addAll(state.applicationContextByInstallNameEntrySet());

        for (Entry<String, ApplicationContext> entry : autoStartApplicationContext) {
            ApplicationContext applicationContext = entry.getValue();
            String installName = entry.getKey();

            if (applicationContext.getMigrationApplicationContext() != null) {
                applicationContext.setLocked(true);
                jobManager.submitJob("recover", "Recover " + installName + " migration", new RecoverMigrationAction(applicationContext, entry.getKey()), null);
            }
            if (applicationContext.isAutoStarted()) {
                try {
                    start(applicationContext, installName, false, false);
                } catch (ApplicationException e) {
                    LOGGER.error("Unable to start", e);
                }
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
                            autoMigrate(new AbstractJobHelper() {

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
            Iterator<Thread> threadIterator = threads.iterator();
            while (threadIterator.hasNext()) {
                Thread thread = threadIterator.next();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    LOGGER.warn("Interrupted auto-migrate", e);
                    List<Thread> interrupted = new ArrayList<Thread>();
                    thread.interrupt();
                    interrupted.add(thread);
                    while (threadIterator.hasNext()) {
                        thread = threadIterator.next();
                        thread.interrupt();
                        interrupted.add(thread);
                    }
                    threadIterator = interrupted.iterator();
                }
            }
            if (failedMigration.size() != 0) {
                throw new ApplicationException("Following migration failed " + failedMigration);
            }
        }

    }

    public JobController autoMigrate(final JobListener jobListener) throws ApplicationException {
        return jobManager.submitJob("automigrate", "Automatic migration", new AutoMigrateJob(), jobListener);
    }

    public JobController autoMigrate(final String installName, final JobListener jobListener) throws ApplicationException {
        return jobManager.submitJob("automigrate", "Automatic migration", new Job() {

            @Override
            public void run(final JobHelper jobHelper) throws Exception {
                autoMigrate(jobHelper, installName);
            }
        }, jobListener);
    }

    public void autoMigrate(final JobHelper jobHelper, final String installName) throws ApplicationException {
        ApplicationContext applicationContext;
        URL context;
        synchronized (state) {
            applicationContext = state.getUnlockedApplicationContext(installName);
            context = applicationContext.getRepoURL();
            applicationContext.setLocked(true);
        }
        boolean migrating = false;
        try {
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
                if (applicationDescriptorFactory.hasApplicationDescriptor(context, appName, newerVersion, this)) {
                    minorVersion = 0;
                    bugfixVersion = 0;
                    do {
                        majorVersion++;
                        newerVersion.set(0, majorVersion + 1);
                    } while (applicationDescriptorFactory.hasApplicationDescriptor(context, appName, newerVersion, this));
                } else {
                    newerVersion.set(2, bugfixVersion);
                }
                newerVersion.set(0, majorVersion);
            case 2:
                newerVersion.set(1, minorVersion + 1);
                newerVersion.set(2, 0);
                if (applicationDescriptorFactory.hasApplicationDescriptor(context, appName, newerVersion, this)) {
                    bugfixVersion = 0;
                    do {
                        minorVersion++;
                        newerVersion.set(1, minorVersion + 1);
                    } while (applicationDescriptorFactory.hasApplicationDescriptor(context, appName, newerVersion, this));
                }
                newerVersion.set(1, minorVersion);
            case 1:
                newerVersion.set(2, bugfixVersion + 1);
                if (applicationDescriptorFactory.hasApplicationDescriptor(context, appName, newerVersion, this)) {
                    do {
                        bugfixVersion++;
                        newerVersion.set(2, bugfixVersion + 1);
                    } while (applicationDescriptorFactory.hasApplicationDescriptor(context, appName, newerVersion, this));
                }
                newerVersion.set(2, bugfixVersion);
            case 0:
                break;
            default:
                throw new ApplicationException("Unexpected autoMigrateLevel" + autoMigrateLevel);
            }
            if (!newerVersion.equals(version)) {
                migrating = true;
                new MigrateAction(applicationContext, applicationContext.getName(), newerVersion, true).run(jobHelper);
            }
        } finally {
            if (!migrating) {
                synchronized (state) {
                    applicationContext.setLocked(false);
                }
            }
        }
    }

    public void setAutoMigrateLevel(final String installName, final int level) throws ApplicationException {
        if (level < 0 || level > 3) {
            throw new ApplicationException("Level must be an integer between 0 and 3");
        }
        synchronized (state) {
            ApplicationContext applicationContext = state.getUnlockedApplicationContext(installName);
            applicationContext.setAutoMigrateLevel(level);
            saveState();
        }
        LOGGER.info("Application {} auto-migrate-level set to {}", installName, level);
    }

    @Override
    public void setAutoStarted(final String installName, final boolean autostart) throws ApplicationException {
        synchronized (state) {
            final ApplicationContext applicationContext = state.getUnlockedApplicationContext(installName);
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
    public String getExceptionStackTrace(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getExceptionStackTrace(installName);
        }
    }

    @Override
    public ResolvedClassLoaderConfiguration getClassLoaders(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getClassLoaders(installName);
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
    public URL getApplicationRepositoryURL(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getApplicationRepositoryURL(installName);
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

    @Override
    public List<Integer> getMigrationRepositoryApplicationVersion(final String installName) throws ApplicationException {
        synchronized (state) {
            return state.getMigrationRepositoryApplicationVersion(installName);
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
    public ApplicationRepositoryMetadata getRepositoryMetadata(final URL repoURL) {
        return applicationDescriptorFactory.getMetadata(repoURL);
    }

    public void startService() {
        vestigeSecureExecutor.startService();

        stateListenerThread = new Thread("default-application-manager-state-listener") {
            @Override
            public void run() {
                try {
                    List<ApplicationManagerStateListener> applicationManagerStateListenersCopy = new ArrayList<ApplicationManagerStateListener>();
                    while (true) {
                        ApplicationManagerState state;
                        synchronized (applicationManagerStateListeners) {
                            while (lastState == null) {
                                applicationManagerStateListeners.wait();
                            }
                            state = lastState;
                            lastState = null;
                            if (applicationManagerStateListenersChanged) {
                                applicationManagerStateListenersCopy.clear();
                                applicationManagerStateListenersCopy.addAll(applicationManagerStateListeners);
                                applicationManagerStateListenersChanged = false;
                            }
                        }
                        for (ApplicationManagerStateListener applicationManagerStateListener : applicationManagerStateListenersCopy) {
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

    public void stopService() throws InterruptedException {
        stateListenerThread.interrupt();
        stateListenerThread.join();
        stateListenerThread = null;
        this.vestigeSecureExecutor.stopService();
    }

    @Override
    public void addStateListener(final ApplicationManagerStateListener listener) {
        synchronized (state) {
            applicationManagerStateListeners.add(listener);
            applicationManagerStateListenersChanged = true;
        }
    }

    @Override
    public void removeStateListener(final ApplicationManagerStateListener listener) {
        synchronized (state) {
            applicationManagerStateListeners.remove(listener);
            applicationManagerStateListenersChanged = true;
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

    @Override
    public boolean isJavaSpecificationVersionCompatible(final String javaSpecificationVersion) {
        if (currentJavaSpecificationVersion == 0) {
            return true;
        }
        int version;
        try {
            if (javaSpecificationVersion.startsWith("1.")) {
                version = Integer.parseInt(javaSpecificationVersion.substring("1.".length()));
            } else {
                version = Integer.parseInt(javaSpecificationVersion);
            }
        } catch (NumberFormatException e) {
            // unknown: accept
            return true;
        }
        if (currentJavaSpecificationVersion >= version) {
            return true;
        }
        return false;
    }

}
