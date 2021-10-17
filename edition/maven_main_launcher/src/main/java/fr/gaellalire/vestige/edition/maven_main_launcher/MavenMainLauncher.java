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

package fr.gaellalire.vestige.edition.maven_main_launcher;

import java.beans.Introspector;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.Vestige;
import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.VestigeCoreContext;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.core.executor.callable.InvokeMethod;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandler;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.ActivateNamedModules;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.AddDependency;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.AddExports;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.AddOpens;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.AdditionalRepository;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.ExceptIn;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.FileAdditionalRepository;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.MavenAttachType;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.MavenClassType;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.MavenConfig;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.MavenLauncher;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.Mode;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.ModifyDependency;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.ObjectFactory;
import fr.gaellalire.vestige.edition.maven_main_launcher.schema.ReplaceDependency;
import fr.gaellalire.vestige.jpms.JPMSAccessorLoader;
import fr.gaellalire.vestige.jpms.JPMSInRepositoryModuleLayerAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerRepository;
import fr.gaellalire.vestige.logback_enhancer.LogbackEnhancer;
import fr.gaellalire.vestige.platform.AddAccessibility;
import fr.gaellalire.vestige.platform.AddReads;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.AttachmentVerificationMetadata;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.platform.VestigePlatformConverter;
import fr.gaellalire.vestige.platform.VestigeURLStreamHandlerFactory;
import fr.gaellalire.vestige.resolver.maven.CreateClassLoaderConfigurationParameters;
import fr.gaellalire.vestige.resolver.maven.DefaultDependencyModifier;
import fr.gaellalire.vestige.resolver.maven.DefaultJPMSConfiguration;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.MavenRepository;
import fr.gaellalire.vestige.resolver.maven.ResolveParameters;
import fr.gaellalire.vestige.resolver.maven.SSLContextAccessor;
import fr.gaellalire.vestige.spi.job.DummyJobHelper;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;
import fr.gaellalire.vestige.utils.SimpleValueGetter;
import fr.gaellalire.vestige.utils.UtilsSchema;

/**
 * @author Gael Lalire
 */
public final class MavenMainLauncher {

    private MavenMainLauncher() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenMainLauncher.class);

    /**
     * Sun JDK serialization keep {@link java.lang.ref.SoftReference SoftReference} of serialized classes. Those reference are hard to GC (you must allocate all available memory),
     * so we try to remove them.
     */
    @SuppressWarnings("rawtypes")
    public static void clearSerializationCache() {
        try {
            Class<?> forName = Class.forName("java.io.ObjectStreamClass$Caches");
            // clear localDescs
            Field declaredField = forName.getDeclaredField("localDescs");
            declaredField.setAccessible(true);
            ConcurrentHashMap m = (ConcurrentHashMap) declaredField.get(null);
            declaredField.setAccessible(false);
            m.clear();
            // clear reflectors
            declaredField = forName.getDeclaredField("reflectors");
            declaredField.setAccessible(true);
            m = (ConcurrentHashMap) declaredField.get(null);
            declaredField.setAccessible(false);
            m.clear();
        } catch (Exception e) {
            // it is just a JVM specific try
        }
    }

    public static ResolveMode convertMode(final Mode mode) {
        switch (mode) {
        case CLASSPATH:
            return ResolveMode.CLASSPATH;
        case FIXED_DEPENDENCIES:
            return ResolveMode.FIXED_DEPENDENCIES;
        default:
            return ResolveMode.FIXED_DEPENDENCIES;
        }
    }

    public static Scope convertScope(final fr.gaellalire.vestige.edition.maven_main_launcher.schema.Scope scope) {
        switch (scope) {
        case ATTACHMENT:
            return Scope.ATTACHMENT;
        case APPLICATION:
            return Scope.CLASS_LOADER_CONFIGURATION;
        case PLATFORM:
            return Scope.PLATFORM;
        default:
            return Scope.PLATFORM;
        }
    }

    public static List<ModuleConfiguration> toModuleConfigurations(final List<fr.gaellalire.vestige.edition.maven_main_launcher.schema.ModulePackageName> addExports,
            final List<fr.gaellalire.vestige.edition.maven_main_launcher.schema.ModulePackageName> addOpens) {
        List<ModuleConfiguration> moduleConfigurations = new ArrayList<ModuleConfiguration>(addExports.size() + addOpens.size());
        for (fr.gaellalire.vestige.edition.maven_main_launcher.schema.ModulePackageName modulePackageName : addExports) {
            moduleConfigurations.add(new ModuleConfiguration(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()),
                    Collections.singleton(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage())), Collections.<String> emptySet(), null));
        }
        for (fr.gaellalire.vestige.edition.maven_main_launcher.schema.ModulePackageName modulePackageName : addOpens) {
            moduleConfigurations.add(new ModuleConfiguration(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getModule()), Collections.<String> emptySet(),
                    Collections.singleton(SimpleValueGetter.INSTANCE.getValue(modulePackageName.getPackage())), null));
        }
        return moduleConfigurations;
    }

    public static JPMSNamedModulesConfiguration convertActivateNamedModule(final ActivateNamedModules activateNamedModules) {
        if (activateNamedModules == null) {
            return null;
        }
        List<fr.gaellalire.vestige.edition.maven_main_launcher.schema.AddReads> addReadsXMLList = activateNamedModules.getAddReads();
        Set<AddReads> addReadsList = new HashSet<AddReads>(addReadsXMLList.size());
        for (AddReads addReads : addReadsList) {
            addReadsList.add(new AddReads(addReads.getSource(), addReads.getTarget()));
        }
        List<AddExports> addExportsXMLList = activateNamedModules.getAddExports();
        Set<AddAccessibility> addExportsList = new HashSet<AddAccessibility>(addExportsXMLList.size());
        for (AddExports addExports : addExportsXMLList) {
            addExportsList.add(new AddAccessibility(SimpleValueGetter.INSTANCE.getValue(addExports.getSource()), SimpleValueGetter.INSTANCE.getValue(addExports.getPn()),
                    SimpleValueGetter.INSTANCE.getValue(addExports.getTarget())));
        }
        List<AddOpens> addOpensXMLList = activateNamedModules.getAddOpens();
        Set<AddAccessibility> addOpensList = new HashSet<AddAccessibility>(addOpensXMLList.size());
        for (AddOpens addExports : addOpensXMLList) {
            addOpensList.add(new AddAccessibility(SimpleValueGetter.INSTANCE.getValue(addExports.getSource()), SimpleValueGetter.INSTANCE.getValue(addExports.getPn()),
                    SimpleValueGetter.INSTANCE.getValue(addExports.getTarget())));
        }

        return new JPMSNamedModulesConfiguration(addReadsList, addExportsList, addOpensList);
    }

    @SuppressWarnings("unchecked")
    public static Object runVestigeMain(final VestigeCoreContext vestigeCoreContext, final File mavenLauncherFile, final File mavenSettingsFile, final File mavenCacertsFile,
            final File mavenResolverCacheFile, final Function<Thread, Void, RuntimeException> addShutdownHook, final Function<Thread, Void, RuntimeException> removeShutdownHook,
            final List<? extends ClassLoader> privilegedClassloaders, final JPMSModuleLayerRepository repository, final String[] dargs) throws Exception {
        VestigeExecutor vestigeExecutor = vestigeCoreContext.getVestigeExecutor();
        VestigeWorker vestigeWorker = vestigeExecutor.createWorker("resolver-maven-worker", true, 0);
        VestigePlatform vestigePlatform = new DefaultVestigePlatform(vestigeCoreContext.getVestigeReaper(), repository);
        // if (JPMSAccessorLoader.INSTANCE != null) {
        // JPMSAccessorLoader.INSTANCE.getModule(VestigePlatform.class).addOpens("fr.gaellalire.vestige.platform", MavenMainLauncher.class);
        // }

        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(vestigePlatform, new VestigeWorker[] {vestigeWorker}, mavenSettingsFile, null);
        VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory = new VestigeURLStreamHandlerFactory();
        MavenArtifactResolver.replaceMavenURLStreamHandler(mavenArtifactResolver.getBaseDir(), vestigeURLStreamHandlerFactory);
        DelegateURLStreamHandlerFactory streamHandlerFactory = vestigeCoreContext.getStreamHandlerFactory();
        streamHandlerFactory.setDelegate(vestigeURLStreamHandlerFactory);

        MavenResolverCache mavenResolverCache = null;
        try {
            if (mavenResolverCacheFile.isFile()) {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(mavenResolverCacheFile));
                try {
                    mavenResolverCache = (MavenResolverCache) objectInputStream.readObject();
                } finally {
                    objectInputStream.close();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to restore main resolver", e);
        }

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        int bcpos = Security.addProvider(new BouncyCastleProvider());
        try {
            LOGGER.debug("BC position is {}", bcpos);
            Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
            int bcjssepos = Security.addProvider(new BouncyCastleJsseProvider(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)));
            try {
                LOGGER.debug("BCJSSE position is {}", bcjssepos);

                long lastModified = mavenLauncherFile.lastModified();
                if (mavenResolverCache == null || lastModified != mavenResolverCache.getLastModified() || !mavenResolverCache.verify()) {
                    JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
                    Unmarshaller unMarshaller = jc.createUnmarshaller();

                    URL xsdURL = MavenMainLauncher.class.getResource("mavenLauncher.xsd");
                    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
                    Schema schema = schemaFactory.newSchema(new Source[] {new StreamSource(UtilsSchema.getURL().toExternalForm()), new StreamSource(xsdURL.toExternalForm())});
                    unMarshaller.setSchema(schema);
                    MavenLauncher mavenLauncher = ((JAXBElement<MavenLauncher>) unMarshaller.unmarshal(mavenLauncherFile)).getValue();
                    DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
                    DefaultJPMSConfiguration defaultJPMSConfiguration = new DefaultJPMSConfiguration();
                    MavenConfig mavenConfig = mavenLauncher.getConfig();
                    List<MavenRepository> additionalRepositories = new ArrayList<MavenRepository>();
                    boolean pomRepositoriesIgnored = false;
                    boolean superPomRepositoriesIgnored = true;
                    if (mavenConfig != null) {
                        for (Object object : mavenConfig.getModifyDependencyOrReplaceDependencyOrAdditionalRepository()) {
                            if (object instanceof ModifyDependency) {
                                ModifyDependency modifyDependency = (ModifyDependency) object;
                                List<AddDependency> addDependencies = modifyDependency.getAddDependency();
                                List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                                for (AddDependency addDependency : addDependencies) {
                                    dependencies.add(new Dependency(new DefaultArtifact(SimpleValueGetter.INSTANCE.getValue(addDependency.getGroupId()),
                                            SimpleValueGetter.INSTANCE.getValue(addDependency.getArtifactId()), "jar",
                                            SimpleValueGetter.INSTANCE.getValue(addDependency.getVersion())), "runtime"));
                                }
                                defaultJPMSConfiguration.addModuleConfiguration(SimpleValueGetter.INSTANCE.getValue(modifyDependency.getGroupId()),
                                        SimpleValueGetter.INSTANCE.getValue(modifyDependency.getArtifactId()),
                                        toModuleConfigurations(modifyDependency.getAddExports(), modifyDependency.getAddOpens()));
                                defaultDependencyModifier.add(SimpleValueGetter.INSTANCE.getValue(modifyDependency.getGroupId()),
                                        SimpleValueGetter.INSTANCE.getValue(modifyDependency.getArtifactId()), dependencies);
                                if (modifyDependency.getAddBeforeParent() != null) {
                                    defaultDependencyModifier.addBeforeParent(SimpleValueGetter.INSTANCE.getValue(modifyDependency.getGroupId()),
                                            SimpleValueGetter.INSTANCE.getValue(modifyDependency.getArtifactId()));
                                }
                            } else if (object instanceof ReplaceDependency) {
                                ReplaceDependency replaceDependency = (ReplaceDependency) object;
                                List<AddDependency> addDependencies = replaceDependency.getAddDependency();
                                List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                                for (AddDependency addDependency : addDependencies) {
                                    dependencies.add(new Dependency(new DefaultArtifact(SimpleValueGetter.INSTANCE.getValue(addDependency.getGroupId()),
                                            SimpleValueGetter.INSTANCE.getValue(addDependency.getArtifactId()), "jar",
                                            SimpleValueGetter.INSTANCE.getValue(addDependency.getVersion())), "runtime"));
                                }
                                Map<String, Set<String>> exceptsMap = null;
                                List<ExceptIn> excepts = replaceDependency.getExceptIn();
                                if (excepts != null) {
                                    exceptsMap = new HashMap<String, Set<String>>();
                                    for (ExceptIn except : excepts) {
                                        Set<String> set = exceptsMap.get(SimpleValueGetter.INSTANCE.getValue(except.getGroupId()));
                                        if (set == null) {
                                            set = new HashSet<String>();
                                            exceptsMap.put(SimpleValueGetter.INSTANCE.getValue(except.getGroupId()), set);
                                        }
                                        set.add(SimpleValueGetter.INSTANCE.getValue(except.getArtifactId()));
                                    }
                                }
                                defaultDependencyModifier.replace(SimpleValueGetter.INSTANCE.getValue(replaceDependency.getGroupId()),
                                        SimpleValueGetter.INSTANCE.getValue(replaceDependency.getArtifactId()), dependencies, exceptsMap);
                            } else if (object instanceof AdditionalRepository) {
                                AdditionalRepository additionalRepository = (AdditionalRepository) object;
                                additionalRepositories.add(new MavenRepository(SimpleValueGetter.INSTANCE.getValue(additionalRepository.getId()),
                                        SimpleValueGetter.INSTANCE.getValue(additionalRepository.getLayout()), SimpleValueGetter.INSTANCE.getValue(additionalRepository.getUrl())));
                            } else if (object instanceof FileAdditionalRepository) {
                                FileAdditionalRepository additionalRepository = (FileAdditionalRepository) object;
                                additionalRepositories.add(new MavenRepository(SimpleValueGetter.INSTANCE.getValue(additionalRepository.getId()),
                                        SimpleValueGetter.INSTANCE.getValue(additionalRepository.getLayout()),
                                        new File(SimpleValueGetter.INSTANCE.getValue(additionalRepository.getPath())).toURI().toURL().toString()));
                            }
                        }
                        pomRepositoriesIgnored = SimpleValueGetter.INSTANCE.getValue(mavenConfig.getPomRepositoriesIgnored());
                        superPomRepositoriesIgnored = SimpleValueGetter.INSTANCE.getValue(mavenConfig.getSuperPomRepositoriesIgnored());
                    }

                    SSLContextAccessor lazySSLContextAccessor = new SSLContextAccessor() {

                        private SSLContext sslContext;

                        private Object mutex = new Object();

                        @Override
                        public SSLContext getSSLContext() {
                            synchronized (mutex) {
                                if (sslContext == null) {
                                    try {
                                        KeyStore trustStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);

                                        TrustManager[] trustManagers = null;
                                        if (mavenCacertsFile != null) {
                                            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", BouncyCastleJsseProvider.PROVIDER_NAME);
                                            FileInputStream stream = new FileInputStream(mavenCacertsFile);
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
                                    } catch (Exception e) {
                                        throw new Error("SSLContext creation failed", e);
                                    }
                                }
                                return sslContext;
                            }
                        }
                    };

                    mavenArtifactResolver = new MavenArtifactResolver(vestigePlatform, new VestigeWorker[] {vestigeWorker}, mavenSettingsFile, lazySSLContextAccessor);

                    List<VerifiedClassLoaderConfiguration> launchCaches = new ArrayList<VerifiedClassLoaderConfiguration>();
                    int attachCount = 0;
                    for (MavenAttachType mavenClassType : mavenLauncher.getAttach()) {
                        ResolveMode resolveMode = convertMode(mavenClassType.getMode());
                        Scope mavenScope = convertScope(mavenClassType.getScope());

                        ResolveParameters resolveRequest = new ResolveParameters();
                        resolveRequest.setGroupId(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getGroupId()));
                        resolveRequest.setArtifactId(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getArtifactId()));
                        resolveRequest.setVersion(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getVersion()));
                        resolveRequest.setExtension("jar");
                        resolveRequest.setAdditionalRepositories(additionalRepositories);
                        resolveRequest.setDependencyModifier(defaultDependencyModifier);
                        resolveRequest.setSuperPomRepositoriesIgnored(superPomRepositoriesIgnored);
                        resolveRequest.setPomRepositoriesIgnored(pomRepositoriesIgnored);
                        resolveRequest.setChecksumVerified(true);

                        CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters = new CreateClassLoaderConfigurationParameters();

                        createClassLoaderConfigurationParameters.setAppName("vestige-attach-" + attachCount);
                        createClassLoaderConfigurationParameters.setJpmsConfiguration(defaultJPMSConfiguration);
                        createClassLoaderConfigurationParameters.setManyLoaders(resolveMode == ResolveMode.FIXED_DEPENDENCIES);
                        createClassLoaderConfigurationParameters.setScope(mavenScope);

                        ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                                .createClassLoaderConfiguration(createClassLoaderConfigurationParameters, null);

                        launchCaches
                                .add(new VerifiedClassLoaderConfiguration(classLoaderConfiguration, SimpleValueGetter.INSTANCE.getValue(mavenClassType.getVerificationMetadata())));
                        attachCount++;
                    }

                    MavenClassType mavenClassType = mavenLauncher.getLaunch();
                    JPMSNamedModulesConfiguration namedModulesConfiguration = convertActivateNamedModule(mavenClassType.getActivateNamedModules());
                    ResolveMode resolveMode = convertMode(mavenClassType.getMode());
                    Scope mavenScope = convertScope(mavenClassType.getScope());

                    ResolveParameters resolveRequest = new ResolveParameters();
                    resolveRequest.setGroupId(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getGroupId()));
                    resolveRequest.setArtifactId(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getArtifactId()));
                    resolveRequest.setVersion(SimpleValueGetter.INSTANCE.getValue(mavenClassType.getVersion()));
                    resolveRequest.setExtension("jar");
                    resolveRequest.setAdditionalRepositories(additionalRepositories);
                    resolveRequest.setDependencyModifier(defaultDependencyModifier);
                    resolveRequest.setSuperPomRepositoriesIgnored(superPomRepositoriesIgnored);
                    resolveRequest.setPomRepositoriesIgnored(pomRepositoriesIgnored);
                    resolveRequest.setChecksumVerified(true);

                    CreateClassLoaderConfigurationParameters createClassLoaderConfigurationParameters = new CreateClassLoaderConfigurationParameters();
                    createClassLoaderConfigurationParameters.setAppName("vestige");
                    createClassLoaderConfigurationParameters.setJpmsConfiguration(defaultJPMSConfiguration);
                    createClassLoaderConfigurationParameters.setJpmsNamedModulesConfiguration(namedModulesConfiguration);
                    createClassLoaderConfigurationParameters.setManyLoaders(resolveMode == ResolveMode.FIXED_DEPENDENCIES);
                    createClassLoaderConfigurationParameters.setScope(mavenScope);

                    ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve(resolveRequest, DummyJobHelper.INSTANCE)
                            .createClassLoaderConfiguration(createClassLoaderConfigurationParameters, null);

                    mavenResolverCache = new MavenResolverCache(launchCaches, SimpleValueGetter.INSTANCE.getValue(mavenClassType.getClazz()),
                            new VerifiedClassLoaderConfiguration(classLoaderConfiguration, SimpleValueGetter.INSTANCE.getValue(mavenClassType.getVerificationMetadata())),
                            lastModified);
                    try {
                        File parentFile = mavenResolverCacheFile.getParentFile();
                        if (!parentFile.isDirectory()) {
                            parentFile.mkdirs();
                        }
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(mavenResolverCacheFile));
                        try {
                            objectOutputStream.writeObject(mavenResolverCache);
                        } finally {
                            objectOutputStream.close();
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Unable to save main resolver", e);
                    }
                    if (!mavenResolverCache.verify()) {
                        throw new IOException("Unable to fix maven repository, aborting vestige run");
                    }
                } else {
                    LOGGER.info("Maven launcher file not modified, use resolver cache");
                }

                for (VerifiedClassLoaderConfiguration classLoaderConfiguration : mavenResolverCache.getClassLoaderConfigurations()) {
                    LOGGER.debug("Attach:\n{}", classLoaderConfiguration);
                    // int attach =
                    vestigePlatform.attach(classLoaderConfiguration.getClassLoaderConfiguration(),
                            AttachmentVerificationMetadata.fromString(classLoaderConfiguration.getVerificationMetadata()), vestigeWorker, null);
                    // vestigePlatform.start(attach);
                }

                VerifiedClassLoaderConfiguration verifiedClassLoaderConfiguration = mavenResolverCache.getClassLoaderConfiguration();
                LOGGER.debug("Attach and run vestigeMain:\n{}", verifiedClassLoaderConfiguration);
                int load = vestigePlatform.attach(verifiedClassLoaderConfiguration.getClassLoaderConfiguration(),
                        AttachmentVerificationMetadata.fromString(verifiedClassLoaderConfiguration.getVerificationMetadata()), vestigeWorker, null);
                // vestigePlatform.start(load);

                final VestigeClassLoader<AttachedVestigeClassLoader> mavenResolverClassLoader = vestigePlatform.getClassLoader(load);
                JPMSInRepositoryModuleLayerAccessor moduleLayer = mavenResolverClassLoader.getData(vestigePlatform).getModuleLayer();
                if (moduleLayer != null) {
                    moduleLayer.findModule("fr.gaellalire.vestige.platform").addOpens("fr.gaellalire.vestige.platform", MavenMainLauncher.class);
                }

                Thread currentThread = Thread.currentThread();
                ClassLoader contextClassLoader = currentThread.getContextClassLoader();
                currentThread.setContextClassLoader(mavenResolverClassLoader);
                Object loadedVestigePlatform;
                final Method vestigeMain;
                URLStreamHandlerFactory loadedURLStreamHandlerFactory = null;
                try {

                    try {
                        Class<?> logbackEnhancerClass = Class.forName(LogbackEnhancer.class.getName(), false, mavenResolverClassLoader);
                        vestigeWorker.invoke(mavenResolverClassLoader, logbackEnhancerClass.getMethod("enhance", VestigeCoreContext.class), null, vestigeCoreContext);
                    } catch (ClassNotFoundException e) {
                        // ignore
                    }

                    String className = mavenResolverCache.getClassName();

                    Class<?> vestigeMainClass = Class.forName(className, false, mavenResolverClassLoader);
                    Class<?> vestigeURLStreamHandlerFactoryClass = Class.forName(VestigeURLStreamHandlerFactory.class.getName(), false, mavenResolverClassLoader);
                    vestigeMain = vestigeMainClass.getMethod("vestigeMain", VestigeCoreContext.class, vestigeURLStreamHandlerFactoryClass,
                            Class.forName(VestigePlatform.class.getName(), false, mavenResolverClassLoader), Function.class, Function.class, List.class, WeakReference.class,
                            String[].class);

                    Class<?> vestigePlatformConverterClass = vestigeWorker.classForName(mavenResolverClassLoader, VestigePlatformConverter.class.getName());
                    LOGGER.trace("Start converting vestige platform");
                    loadedVestigePlatform = vestigeWorker.invoke(mavenResolverClassLoader, vestigePlatformConverterClass.getMethod("convert", Object.class), null, vestigePlatform);
                    LOGGER.trace("Vestige platform converted");

                    loadedURLStreamHandlerFactory = installConvertedVestigeURLStreamHandlerFactory(mavenResolverClassLoader, vestigeURLStreamHandlerFactoryClass,
                            streamHandlerFactory, vestigeURLStreamHandlerFactory, mavenArtifactResolver.getBaseDir());
                } finally {
                    currentThread.setContextClassLoader(contextClassLoader);
                }

                vestigeWorker.interrupt();
                vestigeWorker.join();
                return new InvokeMethod(mavenResolverClassLoader, vestigeMain, null, new Object[] {vestigeCoreContext, loadedURLStreamHandlerFactory, loadedVestigePlatform,
                        addShutdownHook, removeShutdownHook, privilegedClassloaders, new WeakReference<ClassLoader>(MavenMainLauncher.class.getClassLoader()), dargs});
            } finally {
                Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
            }
        } finally {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        }
    }

    private static URLStreamHandlerFactory installConvertedVestigeURLStreamHandlerFactory(final VestigeClassLoader<AttachedVestigeClassLoader> mavenResolverClassLoader,
            final Class<?> vestigeURLStreamHandlerFactoryClass, final DelegateURLStreamHandlerFactory streamHandlerFactory,
            final VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory, final File baseDir) throws Exception {
        Map<String, DelegateURLStreamHandler> copyMap = vestigeURLStreamHandlerFactory.copyMap();
        URLStreamHandlerFactory newInstance = (URLStreamHandlerFactory) vestigeURLStreamHandlerFactoryClass.getConstructor(Map.class).newInstance(copyMap);

        Class<?> mavenArtifactResolverClass = Class.forName(MavenArtifactResolver.class.getName(), false, mavenResolverClassLoader);
        mavenArtifactResolverClass.getMethod("replaceMavenURLStreamHandler", File.class, vestigeURLStreamHandlerFactoryClass).invoke(null, baseDir, newInstance);

        streamHandlerFactory.setDelegate(newInstance);
        return newInstance;
    }

    public static Object vestigeEnhancedCoreMain(final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final String[] args) {
        try {
            long currentTimeMillis = 0;
            if (LOGGER.isInfoEnabled()) {
                currentTimeMillis = System.currentTimeMillis();
                LOGGER.info("Running on JVM {} ({})", System.getProperty("java.specification.version"), System.getProperty("java.home"));
            }
            JPMSModuleLayerRepository repository = null;
            if (JPMSAccessorLoader.INSTANCE != null) {
                repository = JPMSAccessorLoader.INSTANCE.createModuleLayerRepository();
                JPMSModuleAccessor findModule = JPMSAccessorLoader.INSTANCE.bootLayer().findModule("java.base");
                findModule.addOpens("java.io", MavenMainLauncher.class);
                findModule.addOpens("java.lang", Class.forName("com.sun.xml.bind.v2.runtime.reflect.opt.Injector", false, Thread.currentThread().getContextClassLoader()));
            }

            String launcher = System.getenv("MAVEN_LAUNCHER_FILE");
            if (launcher == null) {
                LOGGER.error("MAVEN_LAUNCHER_FILE must be defined");
                return null;
            }
            String settings = System.getenv("MAVEN_SETTINGS_FILE");
            if (settings == null) {
                LOGGER.error("MAVEN_SETTINGS_FILE must be defined");
                return null;
            }
            String mavenCacerts = System.getenv("MAVEN_CACERTS");

            String cache = System.getenv("MAVEN_RESOLVER_CACHE_FILE");
            if (cache == null) {
                LOGGER.error("MAVEN_RESOLVER_CACHE_FILE must be defined");
                return null;
            }

            File mavenLauncherFile = new File(launcher).getCanonicalFile();
            LOGGER.info("Starting a Maven application with {}", mavenLauncherFile);

            File mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
            if (!mavenSettingsFile.isFile()) {
                mavenSettingsFile = new File(settings).getCanonicalFile();
            }
            LOGGER.info("Use {} for Maven settings file", mavenSettingsFile);

            File mavenCacertsFile = null;
            if (mavenCacerts != null) {
                mavenCacertsFile = new File(mavenCacerts).getCanonicalFile();
                LOGGER.debug("Use {} for Maven CA Certs store", mavenCacertsFile);
            }

            File mavenResolverCacheFile = new File(cache).getCanonicalFile();
            LOGGER.debug("Use {} for Maven resolver cache file", mavenResolverCacheFile);

            Object result = runVestigeMain(vestigeCoreContext, mavenLauncherFile, mavenSettingsFile, mavenCacertsFile, mavenResolverCacheFile, addShutdownHook, removeShutdownHook,
                    privilegedClassloaders, repository, args);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Maven application started in {} ms", System.currentTimeMillis() - currentTimeMillis);
            }
            return result;
        } catch (Throwable e) {
            LOGGER.error("Unable to start Maven application", e);
            return null;
        } finally {
            // logback use introspector cache
            Introspector.flushCaches();
            clearSerializationCache();
            Closeable closeable = vestigeCoreContext.getCloseable();
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    LOGGER.trace("Unable to close bootstrap resources", e);
                }
                vestigeCoreContext.setCloseable(null);
            }
        }
    }

    public static Object vestigeCoreMain(final VestigeCoreContext vestigeCoreContext, final String[] args) throws Exception {
        return vestigeEnhancedCoreMain(vestigeCoreContext, null, null, null, args);
    }

    public static void main(final String[] args) throws Exception {
        VestigeCoreContext vestigeCoreContext = VestigeCoreContext.buildDefaultInstance();
        URL.setURLStreamHandlerFactory(vestigeCoreContext.getStreamHandlerFactory());
        Vestige.runCallableLoop(vestigeCoreMain(vestigeCoreContext, args));
    }

}
