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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
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

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.VestigeCoreContext;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.executor.callable.InvokeMethod;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.core.resource.JarFileResourceLocator;
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
import fr.gaellalire.vestige.jpms.JPMSModuleLayerAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerRepository;
import fr.gaellalire.vestige.platform.AddAccessibility;
import fr.gaellalire.vestige.platform.AddReads;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.JPMSNamedModulesConfiguration;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
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
    public static void runVestigeMain(final VestigeCoreContext vestigeCoreContext, final File mavenLauncherFile, final File mavenSettingsFile, final File mavenCacertsFile,
            final File mavenResolverCacheFile, final Function<Thread, Void, RuntimeException> addShutdownHook, final Function<Thread, Void, RuntimeException> removeShutdownHook,
            final List<? extends ClassLoader> privilegedClassloaders, final JPMSModuleLayerRepository repository, final String[] dargs) throws Exception {
        VestigeExecutor vestigeExecutor = vestigeCoreContext.getVestigeExecutor();
        Thread thread = vestigeExecutor.createWorker("resolver-maven-worker", true, 0);
        VestigePlatform vestigePlatform = new DefaultVestigePlatform(vestigeExecutor, repository);
        // if (JPMSAccessorLoader.INSTANCE != null) {
        // JPMSAccessorLoader.INSTANCE.getModule(VestigePlatform.class).addOpens("fr.gaellalire.vestige.platform", MavenMainLauncher.class);
        // }

        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(vestigePlatform, mavenSettingsFile, null);
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
                                    SimpleValueGetter.INSTANCE.getValue(addDependency.getArtifactId()), "jar", SimpleValueGetter.INSTANCE.getValue(addDependency.getVersion())),
                                    "runtime"));
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
                                    SimpleValueGetter.INSTANCE.getValue(addDependency.getArtifactId()), "jar", SimpleValueGetter.INSTANCE.getValue(addDependency.getVersion())),
                                    "runtime"));
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
                            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                            int bcpos = Security.addProvider(new BouncyCastleProvider());
                            LOGGER.debug("BC position is {}", bcpos);
                            Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
                            int bcjssepos = Security.addProvider(new BouncyCastleJsseProvider(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)));
                            LOGGER.debug("BCJSSE position is {}", bcjssepos);

                            try {
                                KeyStore trustStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);

                                trustStore.load(new FileInputStream(mavenCacertsFile), "changeit".toCharArray());

                                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", BouncyCastleJsseProvider.PROVIDER_NAME);
                                trustManagerFactory.init(trustStore);
                                sslContext = SSLContext.getInstance("TLS", BouncyCastleJsseProvider.PROVIDER_NAME);
                                sslContext.init(null, trustManagerFactory.getTrustManagers(), SecureRandom.getInstance("DEFAULT", BouncyCastleProvider.PROVIDER_NAME));
                            } catch (Exception e) {
                                throw new Error("SSLContext creation failed", e);
                            }
                        }
                        return sslContext;
                    }
                }
            };

            mavenArtifactResolver = new MavenArtifactResolver(vestigePlatform, mavenSettingsFile, lazySSLContextAccessor);

            List<ClassLoaderConfiguration> launchCaches = new ArrayList<ClassLoaderConfiguration>();
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
                        .createClassLoaderConfiguration(createClassLoaderConfigurationParameters);

                launchCaches.add(classLoaderConfiguration);
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
                    .createClassLoaderConfiguration(createClassLoaderConfigurationParameters);

            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);

            mavenResolverCache = new MavenResolverCache(launchCaches, SimpleValueGetter.INSTANCE.getValue(mavenClassType.getClazz()), classLoaderConfiguration, lastModified);
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
                LOGGER.error("Unable to fix maven repository, aborting vestige run");
                return;
            }
        } else {
            LOGGER.info("Maven launcher file not modified, use resolver cache");
        }

        for (ClassLoaderConfiguration classLoaderConfiguration : mavenResolverCache.getClassLoaderConfigurations()) {
            LOGGER.debug("Attach:\n{}", classLoaderConfiguration);
            // int attach =
            vestigePlatform.attach(classLoaderConfiguration);
            // vestigePlatform.start(attach);
        }

        LOGGER.debug("Attach and run vestigeMain:\n{}", mavenResolverCache.getClassLoaderConfiguration());
        int load = vestigePlatform.attach(mavenResolverCache.getClassLoaderConfiguration());
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
        try {
            String className = mavenResolverCache.getClassName();
            Class<?> vestigeMainClass = Class.forName(className, false, mavenResolverClassLoader);
            vestigeMain = vestigeMainClass.getMethod("vestigeMain", VestigeExecutor.class, Class.forName(VestigePlatform.class.getName(), false, mavenResolverClassLoader),
                    Function.class, Function.class, List.class, WeakReference.class, String[].class);

            Map<JPMSInRepositoryModuleLayerAccessor, Object> loadedModuleLayers = new IdentityHashMap<JPMSInRepositoryModuleLayerAccessor, Object>();
            // convert, this will initialize some classes in mavenResolverClassLoader so we must set the contextClassLoader
            loadedVestigePlatform = convertVestigePlatform(mavenResolverClassLoader, vestigePlatform, vestigeExecutor,
                    convertModuleRepository(mavenResolverClassLoader, repository, loadedModuleLayers), loadedModuleLayers);

            installConvertedVestigeURLStreamHandlerFactory(mavenResolverClassLoader, streamHandlerFactory, vestigeURLStreamHandlerFactory, mavenArtifactResolver.getBaseDir());

        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }

        thread.interrupt();
        thread.join();

        // start a new thread to allow this classloader to be GC even if the
        // vestigeMain method does not return
        vestigeExecutor.createWorker("resolver-maven-main", false, 1);
        vestigeExecutor.submit(new InvokeMethod(mavenResolverClassLoader, vestigeMain, null, new Object[] {vestigeExecutor, loadedVestigePlatform, addShutdownHook,
                removeShutdownHook, privilegedClassloaders, new WeakReference<ClassLoader>(MavenMainLauncher.class.getClassLoader()), dargs}));
    }

    private static void installConvertedVestigeURLStreamHandlerFactory(final VestigeClassLoader<AttachedVestigeClassLoader> mavenResolverClassLoader,
            final DelegateURLStreamHandlerFactory streamHandlerFactory, final VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory, final File baseDir) throws Exception {
        Class<?> vestigeURLStreamHandlerFactoryClass = Class.forName(VestigeURLStreamHandlerFactory.class.getName(), false, mavenResolverClassLoader);
        Map<String, DelegateURLStreamHandler> copyMap = vestigeURLStreamHandlerFactory.copyMap();
        URLStreamHandlerFactory newInstance = (URLStreamHandlerFactory) vestigeURLStreamHandlerFactoryClass.getConstructor(Map.class).newInstance(copyMap);

        Class<?> mavenArtifactResolverClass = Class.forName(MavenArtifactResolver.class.getName(), false, mavenResolverClassLoader);
        mavenArtifactResolverClass.getMethod("replaceMavenURLStreamHandler", File.class, vestigeURLStreamHandlerFactoryClass).invoke(null, baseDir, newInstance);

        streamHandlerFactory.setDelegate(newInstance);
    }

    public static Object convertLayerAccessor(final VestigeClassLoader<?> mavenResolverClassLoader, final Object loadedRepo, final Method addMethod,
            final Map<JPMSInRepositoryModuleLayerAccessor, Object> convertedLayerAccessorByLayerAccessor, final JPMSInRepositoryModuleLayerAccessor layerAccessor)
            throws Exception {
        Object convertedLayerAccessor = convertedLayerAccessorByLayerAccessor.get(layerAccessor);
        if (convertedLayerAccessor != null) {
            return convertedLayerAccessor;
        }
        List<Object> loadedParents = new ArrayList<Object>();
        for (JPMSInRepositoryModuleLayerAccessor parent : layerAccessor.parents()) {
            loadedParents.add(convertLayerAccessor(mavenResolverClassLoader, loadedRepo, addMethod, convertedLayerAccessorByLayerAccessor, parent));
        }
        Class<? extends JPMSModuleLayerAccessor> layerAccessorClass = layerAccessor.getClass();
        Object controllerProxy = layerAccessorClass.getMethod("getController").invoke(layerAccessor);
        Object controller = controllerProxy.getClass().getMethod("getController").invoke(controllerProxy);

        convertedLayerAccessor = addMethod.invoke(loadedRepo, loadedParents, controller);
        convertedLayerAccessorByLayerAccessor.put(layerAccessor, convertedLayerAccessor);
        return convertedLayerAccessor;
    }

    public static Object convertModuleRepository(final VestigeClassLoader<?> mavenResolverClassLoader, final JPMSModuleLayerRepository repo,
            final Map<JPMSInRepositoryModuleLayerAccessor, Object> loadedModuleLayers) throws Exception {
        if (repo == null) {
            return null;
        }
        // create new instance
        Class<?> vestigeRepositoryClass = Class.forName("fr.gaellalire.vestige.jpms.Java9JPMSModuleLayerRepository", false, mavenResolverClassLoader);
        Object loadedRepo = vestigeRepositoryClass.getConstructor().newInstance();

        // public JPMSInRepositoryModuleLayerAccessor add(final List<Java9JPMSInRepositoryModuleLayerAccessor> parents, final Controller controller) {
        Method addMethod = vestigeRepositoryClass.getMethod("add", List.class, Class.forName("java.lang.ModuleLayer$Controller"));

        int size = repo.size();
        JPMSInRepositoryModuleLayerAccessor bootLayer = repo.get(JPMSModuleLayerRepository.BOOT_LAYER_INDEX);
        Object loadedBootLayer = vestigeRepositoryClass.getMethod("get", int.class).invoke(loadedRepo, JPMSModuleLayerRepository.BOOT_LAYER_INDEX);
        loadedModuleLayers.put(bootLayer, loadedBootLayer);
        for (int i = 0; i < size; i++) {
            JPMSInRepositoryModuleLayerAccessor layerAccessor = repo.get(i);
            if (layerAccessor != null) {
                convertLayerAccessor(mavenResolverClassLoader, loadedRepo, addMethod, loadedModuleLayers, layerAccessor);
            }
        }

        return loadedRepo;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object convertAttachedVestigeClassLoader(final VestigePlatform vestigePlatform, final Object loadedVestigePlatform,
            final Constructor<?> attachedVestigeClassLoaderConstructor, final Field attachedVestigeClassLoaderAttachment,
            final AttachedVestigeClassLoader attachedVestigeClassLoader, final Map<JPMSInRepositoryModuleLayerAccessor, Object> loadedModuleLayers) throws Exception {
        VestigeClassLoader uncheckedVestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
        uncheckedVestigeClassLoader.setDataProtector(vestigePlatform, loadedVestigePlatform);
        Object data = uncheckedVestigeClassLoader.getData(loadedVestigePlatform);
        if (data != attachedVestigeClassLoader) {
            return data;
        }

        List<AttachedVestigeClassLoader> dependencies = attachedVestigeClassLoader.getDependencies();
        List<Object> list = new ArrayList<Object>(dependencies.size());
        for (AttachedVestigeClassLoader dependency : dependencies) {
            list.add(convertAttachedVestigeClassLoader(vestigePlatform, loadedVestigePlatform, attachedVestigeClassLoaderConstructor, attachedVestigeClassLoaderAttachment,
                    dependency, loadedModuleLayers));
        }
        JarFileResourceLocator[] cache = attachedVestigeClassLoader.getCache();
        Object convertedAttachedVestigeClassLoader = attachedVestigeClassLoaderConstructor.newInstance(attachedVestigeClassLoader.getVestigeClassLoader(), list,
                attachedVestigeClassLoader.getName(), attachedVestigeClassLoader.isAttachmentScoped(), cache, loadedModuleLayers.get(attachedVestigeClassLoader.getModuleLayer()),
                attachedVestigeClassLoader.isJPMSActivated());
        attachedVestigeClassLoaderAttachment.set(convertedAttachedVestigeClassLoader, attachedVestigeClassLoader.getAttachments());

        uncheckedVestigeClassLoader.setData(loadedVestigePlatform, convertedAttachedVestigeClassLoader);
        return convertedAttachedVestigeClassLoader;
    }

    @SuppressWarnings("unchecked")
    public static Object convertVestigePlatform(final VestigeClassLoader<?> mavenResolverClassLoader, final VestigePlatform vestigePlatform, final VestigeExecutor vestigeExecutor,
            final Object convertedModuleLayerRepository, final Map<JPMSInRepositoryModuleLayerAccessor, Object> loadedModuleLayers) throws Exception {
        // create new instance
        Class<?> vestigePlatformClass = Class.forName(DefaultVestigePlatform.class.getName(), false, mavenResolverClassLoader);

        final Object loadedVestigePlatform = vestigePlatformClass
                .getConstructor(VestigeExecutor.class, Class.forName(JPMSModuleLayerRepository.class.getName(), false, mavenResolverClassLoader))
                .newInstance(vestigeExecutor, convertedModuleLayerRepository);

        // fetch fields
        Field attachedField = vestigePlatformClass.getDeclaredField("attached");
        attachedField.setAccessible(true);
        List<Object> attached = (List<Object>) attachedField.get(loadedVestigePlatform);
        attachedField.setAccessible(false);

        Field unattachedField = vestigePlatformClass.getDeclaredField("unattached");
        unattachedField.setAccessible(true);
        List<Object> unattached = (List<Object>) unattachedField.get(loadedVestigePlatform);
        unattachedField.setAccessible(false);

        Field attachedClassLoadersField = vestigePlatformClass.getDeclaredField("attachedClassLoaders");
        attachedClassLoadersField.setAccessible(true);
        List<List<WeakReference<Object>>> attachedClassLoaders = (List<List<WeakReference<Object>>>) attachedClassLoadersField.get(loadedVestigePlatform);
        attachedClassLoadersField.setAccessible(false);

        Field mapField = vestigePlatformClass.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<Object, WeakReference<Object>> map = (Map<Object, WeakReference<Object>>) mapField.get(loadedVestigePlatform);
        mapField.setAccessible(false);

        Class<?> attachedVestigeClassLoaderClass = Class.forName(AttachedVestigeClassLoader.class.getName(), false, mavenResolverClassLoader);
        Constructor<?> attachedVestigeClassLoaderConstructor = attachedVestigeClassLoaderClass.getConstructor(VestigeClassLoader.class, List.class, String.class, boolean.class,
                JarFileResourceLocator[].class, Class.forName(JPMSInRepositoryModuleLayerAccessor.class.getName(), false, mavenResolverClassLoader), boolean.class);
        Field attachedVestigeClassLoaderAttachment = attachedVestigeClassLoaderClass.getDeclaredField("attachments");
        attachedVestigeClassLoaderAttachment.setAccessible(true);

        // fill fields
        Set<Integer> attachments = vestigePlatform.getAttachments();
        for (Integer id : attachments) {
            attached.add(convertAttachedVestigeClassLoader(vestigePlatform, loadedVestigePlatform, attachedVestigeClassLoaderConstructor, attachedVestigeClassLoaderAttachment,
                    vestigePlatform.getAttachedVestigeClassLoader(id.intValue()), loadedModuleLayers));
        }

        List<WeakReference<AttachedVestigeClassLoader>> unattachedVestigeClassLoaders = vestigePlatform.getAttachmentScopedUnattachedVestigeClassLoaders();
        for (WeakReference<AttachedVestigeClassLoader> weakReference : unattachedVestigeClassLoaders) {
            AttachedVestigeClassLoader unattachedVestigeClassLoader = weakReference.get();
            if (unattachedVestigeClassLoader != null) {
                unattached.add(new WeakReference<Object>(convertAttachedVestigeClassLoader(vestigePlatform, loadedVestigePlatform, attachedVestigeClassLoaderConstructor,
                        attachedVestigeClassLoaderAttachment, unattachedVestigeClassLoader, loadedModuleLayers)));
            }
        }

        List<List<WeakReference<AttachedVestigeClassLoader>>> attachmentScopedAttachedClassLoaders = vestigePlatform.getAttachmentScopedAttachedClassLoaders();
        for (List<WeakReference<AttachedVestigeClassLoader>> list : attachmentScopedAttachedClassLoaders) {
            if (list == null) {
                attachedClassLoaders.add(null);
                continue;
            }
            List<WeakReference<Object>> destList = new ArrayList<WeakReference<Object>>(list.size());
            for (WeakReference<AttachedVestigeClassLoader> weakReference : list) {
                AttachedVestigeClassLoader attachedVestigeClassLoader = weakReference.get();
                if (attachedVestigeClassLoader != null) {
                    destList.add(new WeakReference<Object>(convertAttachedVestigeClassLoader(vestigePlatform, loadedVestigePlatform, attachedVestigeClassLoaderConstructor,
                            attachedVestigeClassLoaderAttachment, attachedVestigeClassLoader, loadedModuleLayers)));
                }
            }
            attachedClassLoaders.add(destList);
        }

        List<Serializable> loadedArtifact = vestigePlatform.getClassLoaderKeys();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        List<Object> attachedVestigeClassLoaders = new ArrayList<Object>();
        for (Serializable mavenArtifact : loadedArtifact) {
            AttachedVestigeClassLoader attachedVestigeClassLoader = vestigePlatform.getAttachedVestigeClassLoaderByKey(mavenArtifact);
            if (attachedVestigeClassLoader != null) {
                attachedVestigeClassLoaders.add(convertAttachedVestigeClassLoader(vestigePlatform, loadedVestigePlatform, attachedVestigeClassLoaderConstructor,
                        attachedVestigeClassLoaderAttachment, attachedVestigeClassLoader, loadedModuleLayers));
                objectOutputStream.writeObject(mavenArtifact);
            }
        }
        attachedVestigeClassLoaderAttachment.setAccessible(false);
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            @Override
            protected Class<?> resolveClass(final java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, mavenResolverClassLoader);
            }
        };
        for (Object classLoader : attachedVestigeClassLoaders) {
            Object readObject = objectInputStream.readObject();
            map.put(readObject, new WeakReference<Object>(classLoader));
        }

        return loadedVestigePlatform;
    }

    public static void vestigeEnhancedCoreMain(final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
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
                return;
            }
            String settings = System.getenv("MAVEN_SETTINGS_FILE");
            if (settings == null) {
                LOGGER.error("MAVEN_SETTINGS_FILE must be defined");
                return;
            }
            String mavenCacerts = System.getenv("MAVEN_CACERTS");
            if (mavenCacerts == null) {
                LOGGER.error("MAVEN_CACERTS must be defined");
                return;
            }

            String cache = System.getenv("MAVEN_RESOLVER_CACHE_FILE");
            if (cache == null) {
                LOGGER.error("MAVEN_RESOLVER_CACHE_FILE must be defined");
                return;
            }

            File mavenLauncherFile = new File(launcher).getCanonicalFile();
            LOGGER.info("Starting a Maven application with {}", mavenLauncherFile);

            File mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
            if (!mavenSettingsFile.isFile()) {
                mavenSettingsFile = new File(settings).getCanonicalFile();
            }
            LOGGER.info("Use {} for Maven settings file", mavenSettingsFile);

            File mavenCacertsFile = new File(mavenCacerts).getCanonicalFile();
            LOGGER.debug("Use {} for Maven CA Certs store", mavenCacertsFile);

            File mavenResolverCacheFile = new File(cache).getCanonicalFile();
            LOGGER.debug("Use {} for Maven resolver cache file", mavenResolverCacheFile);

            runVestigeMain(vestigeCoreContext, mavenLauncherFile, mavenSettingsFile, mavenCacertsFile, mavenResolverCacheFile, addShutdownHook, removeShutdownHook,
                    privilegedClassloaders, repository, args);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Maven application started in {} ms", System.currentTimeMillis() - currentTimeMillis);
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to start Maven application", e);
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
            }
        }
    }

    public static void vestigeCoreMain(final VestigeCoreContext vestigeCoreContext, final String[] args) throws Exception {
        vestigeEnhancedCoreMain(vestigeCoreContext, null, null, null, args);
    }

    public static void main(final String[] args) throws Exception {
        DelegateURLStreamHandlerFactory streamHandlerFactory = new DelegateURLStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(streamHandlerFactory);
        vestigeCoreMain(new VestigeCoreContext(streamHandlerFactory, new VestigeExecutor()), args);
    }

}
