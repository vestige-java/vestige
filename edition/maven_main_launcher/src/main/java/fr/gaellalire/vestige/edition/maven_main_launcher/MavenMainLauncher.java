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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

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
import fr.gaellalire.vestige.resolver.maven.DefaultDependencyModifier;
import fr.gaellalire.vestige.resolver.maven.DefaultJPMSConfiguration;
import fr.gaellalire.vestige.resolver.maven.MavenArtifactResolver;
import fr.gaellalire.vestige.resolver.maven.MavenRepository;
import fr.gaellalire.vestige.spi.job.DummyJobHelper;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.ResolveMode;

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
            moduleConfigurations
                    .add(new ModuleConfiguration(modulePackageName.getModule(), Collections.singleton(modulePackageName.getPackage()), Collections.<String> emptySet(), null));
        }
        for (fr.gaellalire.vestige.edition.maven_main_launcher.schema.ModulePackageName modulePackageName : addOpens) {
            moduleConfigurations
                    .add(new ModuleConfiguration(modulePackageName.getModule(), Collections.<String> emptySet(), Collections.singleton(modulePackageName.getPackage()), null));
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
            addExportsList.add(new AddAccessibility(addExports.getSource(), addExports.getPn(), addExports.getTarget()));
        }
        List<AddOpens> addOpensXMLList = activateNamedModules.getAddOpens();
        Set<AddAccessibility> addOpensList = new HashSet<AddAccessibility>(addOpensXMLList.size());
        for (AddOpens addExports : addOpensXMLList) {
            addOpensList.add(new AddAccessibility(addExports.getSource(), addExports.getPn(), addExports.getTarget()));
        }

        return new JPMSNamedModulesConfiguration(addReadsList, addExportsList, addOpensList);
    }

    @SuppressWarnings("unchecked")
    public static void runVestigeMain(final VestigeCoreContext vestigeCoreContext, final File mavenLauncherFile, final File mavenSettingsFile, final File mavenResolverCacheFile,
            final Function<Thread, Void, RuntimeException> addShutdownHook, final Function<Thread, Void, RuntimeException> removeShutdownHook,
            final List<? extends ClassLoader> privilegedClassloaders, final JPMSModuleLayerRepository repository, final String[] dargs) throws Exception {
        VestigeExecutor vestigeExecutor = vestigeCoreContext.getVestigeExecutor();
        Thread thread = vestigeExecutor.createWorker("resolver-maven-worker", true, 0);
        VestigePlatform vestigePlatform = new DefaultVestigePlatform(vestigeExecutor, repository);
        // if (JPMSAccessorLoader.INSTANCE != null) {
        // JPMSAccessorLoader.INSTANCE.getModule(VestigePlatform.class).addOpens("fr.gaellalire.vestige.platform", MavenMainLauncher.class);
        // }

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
        if (mavenResolverCache == null || lastModified != mavenResolverCache.getLastModified() || !mavenResolverCache.areAllURLConnectable()) {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            Unmarshaller unMarshaller = jc.createUnmarshaller();

            URL xsdURL = MavenMainLauncher.class.getResource("mavenLauncher-1.0.0.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(xsdURL);
            unMarshaller.setSchema(schema);
            MavenLauncher mavenLauncher = ((JAXBElement<MavenLauncher>) unMarshaller.unmarshal(mavenLauncherFile)).getValue();
            DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
            DefaultJPMSConfiguration defaultJPMSConfiguration = new DefaultJPMSConfiguration();
            MavenConfig mavenConfig = mavenLauncher.getConfig();
            List<MavenRepository> additionalRepositories = new ArrayList<MavenRepository>();
            boolean pomRepositoriesIgnored = false;
            boolean superPomRepositoriesUsed = true;
            if (mavenConfig != null) {
                for (Object object : mavenConfig.getModifyDependencyOrReplaceDependencyOrAdditionalRepository()) {
                    if (object instanceof ModifyDependency) {
                        ModifyDependency modifyDependency = (ModifyDependency) object;
                        List<AddDependency> addDependencies = modifyDependency.getAddDependency();
                        List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                        for (AddDependency addDependency : addDependencies) {
                            dependencies.add(
                                    new Dependency(new DefaultArtifact(addDependency.getGroupId(), addDependency.getArtifactId(), "jar", addDependency.getVersion()), "runtime"));
                        }
                        defaultJPMSConfiguration.addModuleConfiguration(modifyDependency.getGroupId(), modifyDependency.getArtifactId(),
                                toModuleConfigurations(modifyDependency.getAddExports(), modifyDependency.getAddOpens()));
                        defaultDependencyModifier.add(modifyDependency.getGroupId(), modifyDependency.getArtifactId(), dependencies);
                        if (modifyDependency.getAddBeforeParent() != null) {
                            defaultDependencyModifier.addBeforeParent(modifyDependency.getGroupId(), modifyDependency.getArtifactId());
                        }
                    } else if (object instanceof ReplaceDependency) {
                        ReplaceDependency replaceDependency = (ReplaceDependency) object;
                        List<AddDependency> addDependencies = replaceDependency.getAddDependency();
                        List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                        for (AddDependency addDependency : addDependencies) {
                            dependencies.add(
                                    new Dependency(new DefaultArtifact(addDependency.getGroupId(), addDependency.getArtifactId(), "jar", addDependency.getVersion()), "runtime"));
                        }
                        Map<String, Set<String>> exceptsMap = null;
                        List<ExceptIn> excepts = replaceDependency.getExceptIn();
                        if (excepts != null) {
                            exceptsMap = new HashMap<String, Set<String>>();
                            for (ExceptIn except : excepts) {
                                Set<String> set = exceptsMap.get(except.getGroupId());
                                if (set == null) {
                                    set = new HashSet<String>();
                                    exceptsMap.put(except.getGroupId(), set);
                                }
                                set.add(except.getArtifactId());
                            }
                        }
                        defaultDependencyModifier.replace(replaceDependency.getGroupId(), replaceDependency.getArtifactId(), dependencies, exceptsMap);
                    } else if (object instanceof AdditionalRepository) {
                        AdditionalRepository additionalRepository = (AdditionalRepository) object;
                        additionalRepositories.add(new MavenRepository(additionalRepository.getId(), additionalRepository.getLayout(), additionalRepository.getUrl()));
                    } else if (object instanceof FileAdditionalRepository) {
                        FileAdditionalRepository additionalRepository = (FileAdditionalRepository) object;
                        additionalRepositories.add(new MavenRepository(additionalRepository.getId(), additionalRepository.getLayout(),
                                new File(additionalRepository.getPath()).toURI().toURL().toString()));
                    }
                }
                pomRepositoriesIgnored = mavenConfig.isPomRepositoriesIgnored();
                superPomRepositoriesUsed = mavenConfig.isSuperPomRepositoriesUsed();
            }
            MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(vestigePlatform, mavenSettingsFile);
            List<ClassLoaderConfiguration> launchCaches = new ArrayList<ClassLoaderConfiguration>();
            int attachCount = 0;
            for (MavenAttachType mavenClassType : mavenLauncher.getAttach()) {
                ResolveMode resolveMode = convertMode(mavenClassType.getMode());
                Scope mavenScope = convertScope(mavenClassType.getScope());

                ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve("vestige-attach-" + attachCount, mavenClassType.getGroupId(),
                        mavenClassType.getArtifactId(), mavenClassType.getVersion(), additionalRepositories, defaultDependencyModifier, defaultJPMSConfiguration, null,
                        resolveMode == ResolveMode.FIXED_DEPENDENCIES, mavenScope, null, superPomRepositoriesUsed, pomRepositoriesIgnored, DummyJobHelper.INSTANCE);
                launchCaches.add(classLoaderConfiguration);
                attachCount++;
            }

            MavenClassType mavenClassType = mavenLauncher.getLaunch();
            JPMSNamedModulesConfiguration namedModulesConfiguration = convertActivateNamedModule(mavenClassType.getActivateNamedModules());
            ResolveMode resolveMode = convertMode(mavenClassType.getMode());
            Scope mavenScope = convertScope(mavenClassType.getScope());

            ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve("vestige", mavenClassType.getGroupId(), mavenClassType.getArtifactId(),
                    mavenClassType.getVersion(), additionalRepositories, defaultDependencyModifier, defaultJPMSConfiguration, namedModulesConfiguration,
                    resolveMode == ResolveMode.FIXED_DEPENDENCIES, mavenScope, null, superPomRepositoriesUsed, pomRepositoriesIgnored, DummyJobHelper.INSTANCE);

            mavenResolverCache = new MavenResolverCache(launchCaches, mavenClassType.getClazz(), classLoaderConfiguration, lastModified);
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
            if (args.length < 3) {
                throw new IllegalArgumentException("expected at least 3 arguments (Maven launcher, Maven settings, Maven resolver cache)");
            }
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

            File mavenLauncherFile = new File(args[0]).getCanonicalFile();
            LOGGER.info("Starting a Maven application with {}", mavenLauncherFile);

            File mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
            if (!mavenSettingsFile.isFile()) {
                mavenSettingsFile = new File(args[1]).getCanonicalFile();
            }
            LOGGER.info("Use {} for Maven settings file", mavenSettingsFile);

            File mavenResolverCacheFile = new File(args[2]).getCanonicalFile();
            LOGGER.debug("Use {} for Maven resolver cache file", mavenResolverCacheFile);

            final String[] dargs = new String[args.length - 3];
            System.arraycopy(args, 3, dargs, 0, dargs.length);

            runVestigeMain(vestigeCoreContext, mavenLauncherFile, mavenSettingsFile, mavenResolverCacheFile, addShutdownHook, removeShutdownHook, privilegedClassloaders,
                    repository, dargs);
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
