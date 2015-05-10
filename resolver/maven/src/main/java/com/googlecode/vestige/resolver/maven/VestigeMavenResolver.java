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

package com.googlecode.vestige.resolver.maven;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

import com.googlecode.vestige.core.VestigeClassLoader;
import com.googlecode.vestige.core.VestigeExecutor;
import com.googlecode.vestige.core.callable.InvokeMethod;
import com.googlecode.vestige.platform.AttachedVestigeClassLoader;
import com.googlecode.vestige.platform.ClassLoaderConfiguration;
import com.googlecode.vestige.platform.DefaultVestigePlatform;
import com.googlecode.vestige.platform.VestigePlatform;
import com.googlecode.vestige.resolver.maven.schema.AddDependency;
import com.googlecode.vestige.resolver.maven.schema.AdditionalRepository;
import com.googlecode.vestige.resolver.maven.schema.ExceptIn;
import com.googlecode.vestige.resolver.maven.schema.FileAdditionalRepository;
import com.googlecode.vestige.resolver.maven.schema.MavenAttachType;
import com.googlecode.vestige.resolver.maven.schema.MavenClassType;
import com.googlecode.vestige.resolver.maven.schema.MavenConfig;
import com.googlecode.vestige.resolver.maven.schema.MavenLauncher;
import com.googlecode.vestige.resolver.maven.schema.ModifyDependency;
import com.googlecode.vestige.resolver.maven.schema.ObjectFactory;
import com.googlecode.vestige.resolver.maven.schema.ReplaceDependency;

/**
 * @author Gael Lalire
 */
public final class VestigeMavenResolver {

    private VestigeMavenResolver() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeMavenResolver.class);

    @SuppressWarnings("unchecked")
    public static void runVestigeMain(final File mavenLauncherFile, final File mavenSettingsFile, final File mavenResolverCacheFile, final String[] dargs) throws Exception {
        final VestigeExecutor vestigeExecutor = new VestigeExecutor();
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
        if (mavenResolverCache == null || lastModified != mavenResolverCache.getLastModified()) {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            Unmarshaller unMarshaller = jc.createUnmarshaller();

            URL xsdURL = VestigeMavenResolver.class.getResource("mavenLauncher.xsd");
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(xsdURL);
            unMarshaller.setSchema(schema);
            MavenLauncher mavenLauncher = ((JAXBElement<MavenLauncher>) unMarshaller.unmarshal(mavenLauncherFile)).getValue();
            DefaultDependencyModifier defaultDependencyModifier = new DefaultDependencyModifier();
            MavenConfig mavenConfig = mavenLauncher.getConfig();
            List<MavenRepository> additionalRepositories = new ArrayList<MavenRepository>();
            if (mavenConfig != null) {
                for (Object object : mavenConfig.getModifyDependencyOrReplaceDependencyOrAdditionalRepository()) {
                    if (object instanceof ModifyDependency) {
                        ModifyDependency modifyDependency = (ModifyDependency) object;
                        List<AddDependency> addDependencies = modifyDependency.getAddDependency();
                        List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                        for (AddDependency addDependency : addDependencies) {
                            dependencies.add(new Dependency(new DefaultArtifact(addDependency.getGroupId(), addDependency.getArtifactId(), "jar", addDependency.getVersion()),
                                    "runtime"));
                        }
                        defaultDependencyModifier.add(modifyDependency.getGroupId(), modifyDependency.getArtifactId(), dependencies);
                    } else if (object instanceof ReplaceDependency) {
                        ReplaceDependency replaceDependency = (ReplaceDependency) object;
                        List<AddDependency> addDependencies = replaceDependency.getAddDependency();
                        List<Dependency> dependencies = new ArrayList<Dependency>(addDependencies.size());
                        for (AddDependency addDependency : addDependencies) {
                            dependencies.add(new Dependency(new DefaultArtifact(addDependency.getGroupId(), addDependency.getArtifactId(), "jar", addDependency.getVersion()),
                                    "runtime"));
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
                        additionalRepositories.add(new MavenRepository(additionalRepository.getId(), additionalRepository.getLayout(), new File(additionalRepository.getPath())
                                .toURI().toURL().toString()));
                    }
                }
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
                com.googlecode.vestige.resolver.maven.schema.Scope scope = mavenClassType.getScope();
                switch (scope) {
                case ATTACHMENT:
                    mavenScope = Scope.ATTACHMENT;
                    break;
                case APPLICATION:
                    mavenScope = Scope.APPLICATION;
                    break;
                case PLATFORM:
                    mavenScope = Scope.PLATFORM;
                    break;
                default:
                    mavenScope = Scope.PLATFORM;
                    break;
                }

                ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve("vestige-attach-" + attachCount, mavenClassType.getGroupId(),
                        mavenClassType.getArtifactId(), mavenClassType.getVersion(), additionalRepositories, defaultDependencyModifier, resolveMode, mavenScope);
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
            com.googlecode.vestige.resolver.maven.schema.Scope scope = mavenClassType.getScope();
            switch (scope) {
            case ATTACHMENT:
                mavenScope = Scope.ATTACHMENT;
                break;
            case APPLICATION:
                mavenScope = Scope.APPLICATION;
                break;
            case PLATFORM:
                mavenScope = Scope.PLATFORM;
                break;
            default:
                mavenScope = Scope.PLATFORM;
                break;
            }
            ClassLoaderConfiguration classLoaderConfiguration = mavenArtifactResolver.resolve("vestige", mavenClassType.getGroupId(), mavenClassType.getArtifactId(),
                    mavenClassType.getVersion(), additionalRepositories, defaultDependencyModifier, resolveMode, mavenScope);

            LOGGER.trace("classLoaderConfiguration : {}", classLoaderConfiguration);

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
            int attach = vestigePlatform.attach(classLoaderConfiguration);
            vestigePlatform.start(attach);
        }

        LOGGER.debug("Attach and run vestigeMain:\n{}", mavenResolverCache.getClassLoaderConfiguration());
        int load = vestigePlatform.attach(mavenResolverCache.getClassLoaderConfiguration());
        vestigePlatform.start(load);

        final VestigeClassLoader<?> mavenResolverClassLoader = vestigePlatform.getClassLoader(load);

        String className = mavenResolverCache.getClassName();
        Class<?> vestigeMainClass = Class.forName(className, true, mavenResolverClassLoader);
        final Method vestigeMain = vestigeMainClass.getMethod("vestigeMain", VestigeExecutor.class, Class.forName(VestigePlatform.class.getName(), true, mavenResolverClassLoader),
                String[].class);

        // convert
        Object loadedVestigePlatform = convertVestigePlatform(mavenResolverClassLoader, vestigePlatform, vestigeExecutor);

        thread.interrupt();
        thread.join();

        // start a new thread to allow this classloader to be GC even if the
        // vestigeMain method does not return
        vestigeExecutor.createWorker("resolver-maven-main", false, 1);
        vestigeExecutor.submit(new InvokeMethod(mavenResolverClassLoader, vestigeMain, null, new Object[] {vestigeExecutor, loadedVestigePlatform, dargs}));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object convertAttachedVestigeClassLoader(final Constructor<?> attachedVestigeClassLoaderConstructor, final Field attachedVestigeClassLoaderAttachment,
            final AttachedVestigeClassLoader attachedVestigeClassLoader) throws Exception {
        VestigeClassLoader uncheckedVestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
        Object data = uncheckedVestigeClassLoader.getData();
        if (data != attachedVestigeClassLoader) {
            return data;
        }

        List<AttachedVestigeClassLoader> dependencies = attachedVestigeClassLoader.getDependencies();
        List<Object> list = new ArrayList<Object>(dependencies.size());
        for (AttachedVestigeClassLoader dependency : dependencies) {
            list.add(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, attachedVestigeClassLoaderAttachment, dependency));
        }
        Object convertedAttachedVestigeClassLoader = attachedVestigeClassLoaderConstructor.newInstance(attachedVestigeClassLoader.getVestigeClassLoader(), list,
                attachedVestigeClassLoader.getUrls(), attachedVestigeClassLoader.getStartStopClasses(), attachedVestigeClassLoader.getName());
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

        Field startedField = vestigePlatformClass.getDeclaredField("started");
        startedField.setAccessible(true);
        List<Boolean> started = (List<Boolean>) startedField.get(loadedVestigePlatform);
        startedField.setAccessible(false);

        Field mapField = vestigePlatformClass.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<Object, WeakReference<Object>> map = (Map<Object, WeakReference<Object>>) mapField.get(loadedVestigePlatform);
        mapField.setAccessible(false);

        Class<?> attachedVestigeClassLoaderClass = Class.forName(AttachedVestigeClassLoader.class.getName(), false, mavenResolverClassLoader);
        Constructor<?> attachedVestigeClassLoaderConstructor = attachedVestigeClassLoaderClass.getConstructor(VestigeClassLoader.class, List.class, String.class, List.class, String.class);
        Field attachedVestigeClassLoaderAttachment = attachedVestigeClassLoaderClass.getDeclaredField("attachments");
        attachedVestigeClassLoaderAttachment.setAccessible(true);

        // fill fields
        Set<Integer> attachments = vestigePlatform.getAttachments();
        for (Integer id : attachments) {
            attached.add(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, attachedVestigeClassLoaderAttachment,
                    vestigePlatform.getAttachedVestigeClassLoader(id.intValue())));
            if (vestigePlatform.isStarted(id.intValue())) {
                started.add(Boolean.TRUE);
            } else {
                started.add(Boolean.FALSE);
            }
        }

        List<Serializable> loadedArtifact = vestigePlatform.getClassLoaderKeys();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        List<Object> attachedVestigeClassLoaders = new ArrayList<Object>();
        for (Serializable mavenArtifact : loadedArtifact) {
            AttachedVestigeClassLoader attachedVestigeClassLoader = vestigePlatform.getAttachedVestigeClassLoaderByKey(mavenArtifact);
            if (attachedVestigeClassLoader != null) {
                attachedVestigeClassLoaders.add(convertAttachedVestigeClassLoader(attachedVestigeClassLoaderConstructor, attachedVestigeClassLoaderAttachment,
                        attachedVestigeClassLoader));
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
        try {
            if (args.length < 3) {
                throw new IllegalArgumentException("expected at least 3 arguments (maven launcher, maven settings, maven resolver cache)");
            }
            long currentTimeMillis = 0;
            if (LOGGER.isInfoEnabled()) {
                currentTimeMillis = System.currentTimeMillis();
                LOGGER.info("Starting a maven application");
            }

            File mavenLauncherFile = new File(args[0]).getCanonicalFile();
            LOGGER.info("Use {} for maven launcher file", mavenLauncherFile);

            File mavenSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
            if (!mavenSettingsFile.isFile()) {
                mavenSettingsFile = new File(args[1]).getCanonicalFile();
            }
            LOGGER.info("Use {} for maven settings file", mavenSettingsFile);

            File mavenResolverCacheFile = new File(args[2]).getCanonicalFile();
            LOGGER.info("Use {} for maven resolver cache file", mavenResolverCacheFile);

            final String[] dargs = new String[args.length - 3];
            System.arraycopy(args, 3, dargs, 0, dargs.length);

            runVestigeMain(mavenLauncherFile, mavenSettingsFile, mavenResolverCacheFile, dargs);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Maven application started in {} ms", System.currentTimeMillis() - currentTimeMillis);
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to start maven application", e);
        } finally {
            // logback use introspector cache
            Introspector.flushCaches();
            clearSerializationCache();
        }
    }

    /**
     * Sun JDK serialization keep {@link java.lang.ref.SoftReference SoftReference} of serialized classes.
     * Those reference are hard to GC (you must allocate all available memory),
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
