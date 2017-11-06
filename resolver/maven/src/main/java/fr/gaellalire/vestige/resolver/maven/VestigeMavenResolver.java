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

package fr.gaellalire.vestige.resolver.maven;

import java.beans.Introspector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

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
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.executor.callable.InvokeMethod;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandler;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.job.DummyJobHelper;
import fr.gaellalire.vestige.jpms.JPMSAccessorLoader;
import fr.gaellalire.vestige.platform.AttachedVestigeClassLoader;
import fr.gaellalire.vestige.platform.ClassLoaderConfiguration;
import fr.gaellalire.vestige.platform.DefaultVestigePlatform;
import fr.gaellalire.vestige.platform.ModuleConfiguration;
import fr.gaellalire.vestige.platform.VestigePlatform;
import fr.gaellalire.vestige.resolver.maven.schema.AddDependency;
import fr.gaellalire.vestige.resolver.maven.schema.AdditionalRepository;
import fr.gaellalire.vestige.resolver.maven.schema.ExceptIn;
import fr.gaellalire.vestige.resolver.maven.schema.FileAdditionalRepository;
import fr.gaellalire.vestige.resolver.maven.schema.MavenAttachType;
import fr.gaellalire.vestige.resolver.maven.schema.MavenClassType;
import fr.gaellalire.vestige.resolver.maven.schema.MavenConfig;
import fr.gaellalire.vestige.resolver.maven.schema.MavenLauncher;
import fr.gaellalire.vestige.resolver.maven.schema.ModifyDependency;
import fr.gaellalire.vestige.resolver.maven.schema.ObjectFactory;
import fr.gaellalire.vestige.resolver.maven.schema.ReplaceDependency;

/**
 * @author Gael Lalire
 */
public final class VestigeMavenResolver {

    private VestigeMavenResolver() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeMavenResolver.class);

    public static List<ModuleConfiguration> toModuleConfigurations(final List<fr.gaellalire.vestige.resolver.maven.schema.ModulePackageName> addExports,
            final List<fr.gaellalire.vestige.resolver.maven.schema.ModulePackageName> addOpens) {
        List<ModuleConfiguration> moduleConfigurations = new ArrayList<ModuleConfiguration>(addExports.size() + addOpens.size());
        for (fr.gaellalire.vestige.resolver.maven.schema.ModulePackageName modulePackageName : addExports) {
            moduleConfigurations
                    .add(new ModuleConfiguration(modulePackageName.getModule(), Collections.singleton(modulePackageName.getPackage()), Collections.<String> emptySet()));
        }
        for (fr.gaellalire.vestige.resolver.maven.schema.ModulePackageName modulePackageName : addOpens) {
            moduleConfigurations
                    .add(new ModuleConfiguration(modulePackageName.getModule(), Collections.<String> emptySet(), Collections.singleton(modulePackageName.getPackage())));
        }
        return moduleConfigurations;
    }

    @SuppressWarnings("unchecked")
    public static void runVestigeMain(final VestigeExecutor vestigeExecutor, final File mavenLauncherFile, final File mavenSettingsFile, final File mavenResolverCacheFile,
            final Function<Thread, Void, RuntimeException> addShutdownHook, final Function<Thread, Void, RuntimeException> removeShutdownHook,
            final List<? extends ClassLoader> privilegedClassloaders, final String[] dargs) throws Exception {
        Thread thread = vestigeExecutor.createWorker("resolver-maven-worker", true, 0);
        VestigePlatform vestigePlatform = new DefaultVestigePlatform(vestigeExecutor);

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

            URL xsdURL = VestigeMavenResolver.class.getResource("mavenLauncher-1.0.0.xsd");
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
            MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver(mavenSettingsFile);
            List<ClassLoaderConfiguration> launchCaches = new ArrayList<ClassLoaderConfiguration>();
            int attachCount = 0;
            for (MavenAttachType mavenClassType : mavenLauncher.getAttach()) {
                ResolveMode resolveMode;
                switch (mavenClassType.getMode()) {
                case CLASSPATH:
                    resolveMode = ResolveMode.CLASSPATH;
                    break;
                case FIXED_DEPENDENCIES:
                    resolveMode = ResolveMode.FIXED_DEPENDENCIES;
                    break;
                default:
                    resolveMode = ResolveMode.FIXED_DEPENDENCIES;
                    break;
                }

                Scope mavenScope;
                fr.gaellalire.vestige.resolver.maven.schema.Scope scope = mavenClassType.getScope();
                switch (scope) {
                case ATTACHMENT:
                    mavenScope = Scope.ATTACHMENT;
                    break;
                case APPLICATION:
                    mavenScope = Scope.INSTALLATION;
                    break;
                case PLATFORM:
                    mavenScope = Scope.PLATFORM;
                    break;
                default:
                    mavenScope = Scope.PLATFORM;
                    break;
                }

                ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve("vestige-attach-" + attachCount, mavenClassType.getGroupId(),
                        mavenClassType.getArtifactId(), mavenClassType.getVersion(), additionalRepositories, defaultDependencyModifier, defaultJPMSConfiguration, resolveMode,
                        mavenScope, null, superPomRepositoriesUsed, pomRepositoriesIgnored, DummyJobHelper.INSTANCE);
                launchCaches.add(classLoaderConfiguration);
                attachCount++;
            }

            MavenClassType mavenClassType = mavenLauncher.getLaunch();
            ResolveMode resolveMode;
            switch (mavenClassType.getMode()) {
            case CLASSPATH:
                resolveMode = ResolveMode.CLASSPATH;
                break;
            case FIXED_DEPENDENCIES:
                resolveMode = ResolveMode.FIXED_DEPENDENCIES;
                break;
            default:
                resolveMode = ResolveMode.FIXED_DEPENDENCIES;
                break;
            }
            Scope mavenScope;
            fr.gaellalire.vestige.resolver.maven.schema.Scope scope = mavenClassType.getScope();
            switch (scope) {
            case ATTACHMENT:
                mavenScope = Scope.ATTACHMENT;
                break;
            case APPLICATION:
                mavenScope = Scope.INSTALLATION;
                break;
            case PLATFORM:
                mavenScope = Scope.PLATFORM;
                break;
            default:
                mavenScope = Scope.PLATFORM;
                break;
            }
            ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve("vestige", mavenClassType.getGroupId(), mavenClassType.getArtifactId(),
                    mavenClassType.getVersion(), additionalRepositories, defaultDependencyModifier, defaultJPMSConfiguration, resolveMode, mavenScope, null,
                    superPomRepositoriesUsed, pomRepositoriesIgnored, DummyJobHelper.INSTANCE);

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

        final VestigeClassLoader<?> mavenResolverClassLoader = vestigePlatform.getClassLoader(load);

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

            // convert, this will initialize some classes in mavenResolverClassLoader so we must set the contextClassLoader
            loadedVestigePlatform = convertVestigePlatform(mavenResolverClassLoader, vestigePlatform, vestigeExecutor);
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }

        thread.interrupt();
        thread.join();

        // start a new thread to allow this classloader to be GC even if the
        // vestigeMain method does not return
        vestigeExecutor.createWorker("resolver-maven-main", false, 1);
        vestigeExecutor.submit(new InvokeMethod(mavenResolverClassLoader, vestigeMain, null, new Object[] {vestigeExecutor, loadedVestigePlatform, addShutdownHook,
                removeShutdownHook, privilegedClassloaders, new WeakReference<ClassLoader>(VestigeMavenResolver.class.getClassLoader()), dargs}));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object convertAttachedVestigeClassLoader(final Constructor<?> attachedVestigeClassLoaderConstructor, final Method setURLStreamHandlerFactoryDelegateMethod,
            final Field attachedVestigeClassLoaderAttachment, final AttachedVestigeClassLoader attachedVestigeClassLoader) throws Exception {
        VestigeClassLoader uncheckedVestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
        Object data = uncheckedVestigeClassLoader.getData();
        if (data != attachedVestigeClassLoader) {
            return data;
        }

        List<AttachedVestigeClassLoader> dependencies = attachedVestigeClassLoader.getDependencies();
        List<Object> list = new ArrayList<Object>(dependencies.size());
        for (AttachedVestigeClassLoader dependency : dependencies) {
            list.add(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, setURLStreamHandlerFactoryDelegateMethod, attachedVestigeClassLoaderAttachment,
                    dependency));
        }
        DelegateURLStreamHandlerFactory delegateURLStreamHandlerFactory = attachedVestigeClassLoader.getDelegateURLStreamHandlerFactory();
        Map<File, JarFile> cache = null;
        DelegateURLStreamHandler delegateURLStreamHandler = null;
        if (delegateURLStreamHandlerFactory != null) {
            delegateURLStreamHandler = attachedVestigeClassLoader.getDelegateURLStreamHandler();
            cache = attachedVestigeClassLoader.getCache();
            setURLStreamHandlerFactoryDelegateMethod.invoke(null, delegateURLStreamHandlerFactory, delegateURLStreamHandler, cache);
        }
        Object convertedAttachedVestigeClassLoader = attachedVestigeClassLoaderConstructor.newInstance(attachedVestigeClassLoader.getVestigeClassLoader(), list,
                attachedVestigeClassLoader.getUrls(), attachedVestigeClassLoader.getName(), attachedVestigeClassLoader.isAttachmentScoped(), cache, delegateURLStreamHandlerFactory,
                delegateURLStreamHandler);
        attachedVestigeClassLoaderAttachment.set(convertedAttachedVestigeClassLoader, attachedVestigeClassLoader.getAttachments());

        uncheckedVestigeClassLoader.setData(convertedAttachedVestigeClassLoader);
        return convertedAttachedVestigeClassLoader;
    }

    @SuppressWarnings("unchecked")
    public static Object convertVestigePlatform(final VestigeClassLoader<?> mavenResolverClassLoader, final VestigePlatform vestigePlatform, final VestigeExecutor vestigeExecutor)
            throws Exception {
        // create new instance
        Class<?> vestigePlatformClass = Class.forName(DefaultVestigePlatform.class.getName(), false, mavenResolverClassLoader);
        final Object loadedVestigePlatform = vestigePlatformClass.getConstructor(VestigeExecutor.class).newInstance(vestigeExecutor);

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
        Constructor<?> attachedVestigeClassLoaderConstructor = attachedVestigeClassLoaderClass.getConstructor(VestigeClassLoader.class, List.class, String.class, String.class,
                boolean.class, Map.class, DelegateURLStreamHandlerFactory.class, DelegateURLStreamHandler.class);
        Method setURLStreamHandlerFactoryDelegateMethod = vestigePlatformClass.getMethod("setURLStreamHandlerFactoryDelegate", DelegateURLStreamHandlerFactory.class,
                DelegateURLStreamHandler.class, Map.class);
        Field attachedVestigeClassLoaderAttachment = attachedVestigeClassLoaderClass.getDeclaredField("attachments");
        attachedVestigeClassLoaderAttachment.setAccessible(true);

        // fill fields
        Set<Integer> attachments = vestigePlatform.getAttachments();
        for (Integer id : attachments) {
            attached.add(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, setURLStreamHandlerFactoryDelegateMethod, attachedVestigeClassLoaderAttachment,
                    vestigePlatform.getAttachedVestigeClassLoader(id.intValue())));
        }

        List<WeakReference<AttachedVestigeClassLoader>> unattachedVestigeClassLoaders = vestigePlatform.getAttachmentScopedUnattachedVestigeClassLoaders();
        for (WeakReference<AttachedVestigeClassLoader> weakReference : unattachedVestigeClassLoaders) {
            AttachedVestigeClassLoader unattachedVestigeClassLoader = weakReference.get();
            if (unattachedVestigeClassLoader != null) {
                unattached.add(new WeakReference<Object>(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, setURLStreamHandlerFactoryDelegateMethod,
                        attachedVestigeClassLoaderAttachment, unattachedVestigeClassLoader)));
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
                    destList.add(new WeakReference<Object>(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, setURLStreamHandlerFactoryDelegateMethod,
                            attachedVestigeClassLoaderAttachment, attachedVestigeClassLoader)));
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
                attachedVestigeClassLoaders.add(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, setURLStreamHandlerFactoryDelegateMethod,
                        attachedVestigeClassLoaderAttachment, attachedVestigeClassLoader));
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

    public static void main(final String[] args) throws Exception {
        vestigeCoreMain(new VestigeExecutor(), args);
    }

    public static void vestigeCoreMain(final VestigeExecutor vestigeExecutor, final String[] args) throws Exception {
        vestigeEnhancedCoreMain(vestigeExecutor, null, null, null, args);
    }

    public static void vestigeEnhancedCoreMain(final VestigeExecutor vestigeExecutor, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final String[] args) {
        try {
            if (args.length < 3) {
                throw new IllegalArgumentException("expected at least 3 arguments (Maven launcher, Maven settings, Maven resolver cache)");
            }
            long currentTimeMillis = 0;
            if (LOGGER.isInfoEnabled()) {
                currentTimeMillis = System.currentTimeMillis();
                LOGGER.info("Running on JVM {} ({})", System.getProperty("java.specification.version"), System.getProperty("java.home"));
                LOGGER.info("Starting a Maven application");
            }

            File mavenLauncherFile = new File(args[0]).getCanonicalFile();
            LOGGER.debug("Use {} for Maven launcher file", mavenLauncherFile);

            File mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
            if (!mavenSettingsFile.isFile()) {
                mavenSettingsFile = new File(args[1]).getCanonicalFile();
            }
            LOGGER.info("Use {} for Maven settings file", mavenSettingsFile);

            File mavenResolverCacheFile = new File(args[2]).getCanonicalFile();
            LOGGER.debug("Use {} for Maven resolver cache file", mavenResolverCacheFile);

            final String[] dargs = new String[args.length - 3];
            System.arraycopy(args, 3, dargs, 0, dargs.length);

            runVestigeMain(vestigeExecutor, mavenLauncherFile, mavenSettingsFile, mavenResolverCacheFile, addShutdownHook, removeShutdownHook, privilegedClassloaders, dargs);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Maven application started in {} ms", System.currentTimeMillis() - currentTimeMillis);
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to start Maven application", e);
        } finally {
            if (JPMSAccessorLoader.INSTANCE != null) {
                JPMSAccessorLoader.INSTANCE.findBootModule("java.base").addOpens("java.io", VestigeMavenResolver.class);
            }
            // logback use introspector cache
            Introspector.flushCaches();
            clearSerializationCache();
            try {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                // from java 7 we can close
                if (contextClassLoader instanceof URLClassLoader) {
                    URLClassLoader.class.getMethod("close").invoke(contextClassLoader);
                }
            } catch (Exception e) {
                LOGGER.trace("Unable to close classLoader", e);
            }
        }
    }

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

}
