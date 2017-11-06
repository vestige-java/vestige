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

package fr.gaellalire.vestige.platform;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.parser.ClassesStringParser;
import fr.gaellalire.vestige.core.parser.NoStateStringParser;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandler;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.jpms.JPMSAccessorLoader;
import fr.gaellalire.vestige.jpms.JPMSModuleAccessor;

/**
 * @author Gael Lalire
 */
public class DefaultVestigePlatform implements VestigePlatform {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVestigePlatform.class);

    private static final List<List<VestigeClassLoader<?>>> NO_DEPENDENCY_LIST = Collections.singletonList(Collections.<VestigeClassLoader<?>> singletonList(null));

    private List<AttachedVestigeClassLoader> attached = new ArrayList<AttachedVestigeClassLoader>();

    private List<List<WeakReference<AttachedVestigeClassLoader>>> attachedClassLoaders = new ArrayList<List<WeakReference<AttachedVestigeClassLoader>>>();

    private List<WeakReference<AttachedVestigeClassLoader>> unattached = new ArrayList<WeakReference<AttachedVestigeClassLoader>>();

    private Map<Serializable, WeakReference<AttachedVestigeClassLoader>> map = new HashMap<Serializable, WeakReference<AttachedVestigeClassLoader>>();

    private VestigeExecutor vestigeExecutor;

    public DefaultVestigePlatform(final VestigeExecutor vestigeExecutor) {
        this.vestigeExecutor = vestigeExecutor;
    }

    public void clean() {
        Iterator<WeakReference<AttachedVestigeClassLoader>> iterator = map.values().iterator();
        while (iterator.hasNext()) {
            WeakReference<AttachedVestigeClassLoader> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
        iterator = unattached.iterator();
        while (iterator.hasNext()) {
            WeakReference<AttachedVestigeClassLoader> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
    }

    public int attach(final ClassLoaderConfiguration classLoaderConfiguration) throws InterruptedException {
        int size = attached.size();
        int id = 0;
        while (id < size) {
            if (attached.get(id) == null) {
                break;
            }
            id++;
        }
        Map<Serializable, VestigeClassLoader<AttachedVestigeClassLoader>> attachmentMap = new HashMap<Serializable, VestigeClassLoader<AttachedVestigeClassLoader>>();
        AttachedVestigeClassLoader load = attachDependencies(attachmentMap, classLoaderConfiguration);
        Collection<VestigeClassLoader<AttachedVestigeClassLoader>> values = attachmentMap.values();
        int valueSize = values.size();
        List<WeakReference<AttachedVestigeClassLoader>> list = null;
        if (valueSize != 0) {
            list = new ArrayList<WeakReference<AttachedVestigeClassLoader>>(valueSize);
            for (VestigeClassLoader<AttachedVestigeClassLoader> classLoader : values) {
                list.add(new WeakReference<AttachedVestigeClassLoader>(classLoader.getData()));
            }
        }
        if (id == size) {
            attachedClassLoaders.add(list);
            attached.add(load);
        } else {
            attachedClassLoaders.set(id, list);
            attached.set(id, load);
        }
        addAttachment(load);
        clean();
        return id;
    }

    public int attach(final VestigeClassLoader<AttachedVestigeClassLoader> classLoader) {
        int size = attached.size();
        int id = 0;
        while (id < size) {
            if (attached.get(id) == null) {
                break;
            }
            id++;
        }
        AttachedVestigeClassLoader load = classLoader.getData();
        if (id == size) {
            attachedClassLoaders.add(null);
            attached.add(load);
        } else {
            attachedClassLoaders.set(id, null);
            attached.set(id, load);
        }
        addAttachment(load);
        clean();
        return id;
    }

    public void clearCache(final Map<File, JarFile> cache) {
        synchronized (cache) {
            for (Entry<File, JarFile> entry : cache.entrySet()) {
                try {
                    LOGGER.info("Closing {}", entry.getKey());
                    entry.getValue().close();
                } catch (IOException e) {
                    LOGGER.error("Unable to close", e);
                }
            }
            cache.clear();
        }
    }

    public void detach(final int id) {
        int last = attached.size() - 1;
        List<WeakReference<AttachedVestigeClassLoader>> unattachedVestigeClassLoaders;
        AttachedVestigeClassLoader unload;
        if (id == last) {
            unattachedVestigeClassLoaders = attachedClassLoaders.remove(last);
            unload = attached.remove(last);
            last--;
            while (last >= 0 && attached.get(last) == null) {
                attachedClassLoaders.remove(last);
                attached.remove(last);
                last--;
            }
        } else {
            unattachedVestigeClassLoaders = attachedClassLoaders.set(id, null);
            unload = attached.set(id, null);
        }
        if (unattachedVestigeClassLoaders != null) {
            unattached.addAll(unattachedVestigeClassLoaders);
        }
        removeAttachment(unload);
        clean();
    }

    public List<Serializable> getClassLoaderKeys() {
        clean();
        return new ArrayList<Serializable>(map.keySet());
    }

    public AttachedVestigeClassLoader getAttachedVestigeClassLoaderByKey(final Serializable key) {
        WeakReference<AttachedVestigeClassLoader> weakReference = map.get(key);
        if (weakReference == null) {
            return null;
        }
        return weakReference.get();
    }

    public Set<Integer> getAttachments() {
        Set<Integer> set = new TreeSet<Integer>();
        int id = 0;
        for (AttachedVestigeClassLoader attach : attached) {
            if (attach != null) {
                set.add(id);
            }
            id++;
        }
        return set;
    }

    public VestigeClassLoader<AttachedVestigeClassLoader> getClassLoader(final int id) {
        return attached.get(id).getVestigeClassLoader();
    }

    public AttachedVestigeClassLoader getAttachedVestigeClassLoader(final int id) {
        return attached.get(id);
    }

    public VestigeClassLoader<AttachedVestigeClassLoader> convertPath(final int path, final AttachedVestigeClassLoader attachedVestigeClassLoader,
            final ClassLoaderConfiguration conf) {
        AttachedVestigeClassLoader currentAttachedVestigeClassLoader = attachedVestigeClassLoader;
        ClassLoaderConfiguration currentConf = conf;
        int currentPath = path;
        while (currentPath != -1) {
            int dependencyIndex = currentConf.getDependencyIndex(currentPath);
            currentPath = currentConf.getDependencyPathIndex(currentPath);
            currentAttachedVestigeClassLoader = currentAttachedVestigeClassLoader.getDependencies().get(dependencyIndex);
            currentConf = currentConf.getDependencies().get(dependencyIndex);
        }
        return currentAttachedVestigeClassLoader.getVestigeClassLoader();
    }

    public List<List<VestigeClassLoader<?>>> convert(final AttachedVestigeClassLoader attachedVestigeClassLoader, final ClassLoaderConfiguration conf) {
        List<List<Integer>> pathsData = conf.getPathIdsList();
        if (pathsData == null || pathsData.size() == 0) {
            return NO_DEPENDENCY_LIST;
        }
        List<List<VestigeClassLoader<?>>> data = new ArrayList<List<VestigeClassLoader<?>>>(pathsData.size());
        for (List<Integer> paths : pathsData) {
            List<VestigeClassLoader<?>> classLoaders = null;
            if (paths != null) {
                classLoaders = new ArrayList<VestigeClassLoader<?>>(paths.size());
                for (Integer path : paths) {
                    classLoaders.add(convertPath(path, attachedVestigeClassLoader, conf));
                }
            }
            data.add(classLoaders);
        }
        return data;
    }

    private AttachedVestigeClassLoader attachDependencies(final Map<Serializable, VestigeClassLoader<AttachedVestigeClassLoader>> attachmentMap,
            final ClassLoaderConfiguration classLoaderConfiguration) throws InterruptedException {
        Serializable key = classLoaderConfiguration.getKey();

        List<ClassLoaderConfiguration> configurationDependencies = classLoaderConfiguration.getDependencies();
        List<AttachedVestigeClassLoader> classLoaderDependencies = new ArrayList<AttachedVestigeClassLoader>();
        for (ClassLoaderConfiguration configurationDependency : configurationDependencies) {
            classLoaderDependencies.add(attachDependencies(attachmentMap, configurationDependency));
        }
        VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader = null;
        if (classLoaderConfiguration.isAttachmentScoped()) {
            vestigeClassLoader = attachmentMap.get(key);
        } else {
            WeakReference<AttachedVestigeClassLoader> weakReference = map.get(key);
            if (weakReference != null) {
                AttachedVestigeClassLoader attachedVestigeClassLoader = weakReference.get();
                if (attachedVestigeClassLoader != null) {
                    vestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
                }
            }
        }

        if (vestigeClassLoader == null) {
            // search inside jar after dependencies
            // classLoaderDependencies.add(null);
            URL[] urls = classLoaderConfiguration.getUrls();
            String name = classLoaderConfiguration.getName();

            // for vestige class loader : null == current classloader
            AttachedVestigeClassLoader attachedVestigeClassLoader = new AttachedVestigeClassLoader(classLoaderDependencies);
            StringParser resourceStringParser = classLoaderConfiguration.getPathIdsPositionByResourceName();
            StringParser classStringParser;
            if (resourceStringParser == null) {
                resourceStringParser = new NoStateStringParser(0);
                classStringParser = resourceStringParser;
            } else {
                classStringParser = new ClassesStringParser(resourceStringParser);
            }

            // create classloader with executor to remove this protection domain from access control

            Map<File, JarFile> cache = new HashMap<File, JarFile>();
            DelegateURLStreamHandlerFactory delegateURLStreamHandlerFactory = new DelegateURLStreamHandlerFactory();
            DelegateURLStreamHandler delegateURLStreamHandler = new DelegateURLStreamHandler();
            setURLStreamHandlerFactoryDelegate(delegateURLStreamHandlerFactory, delegateURLStreamHandler, cache);
            vestigeClassLoader = vestigeExecutor.createVestigeClassLoader(ClassLoader.getSystemClassLoader(), convert(attachedVestigeClassLoader, classLoaderConfiguration),
                    classStringParser, resourceStringParser, delegateURLStreamHandlerFactory, urls);
            if (JPMSAccessorLoader.INSTANCE != null) {
                JPMSClassLoaderConfiguration jpmsConfiguration = classLoaderConfiguration.getModuleConfiguration();
                for (ModuleConfiguration moduleConfiguration : jpmsConfiguration.getModuleConfigurations()) {
                    String moduleName = moduleConfiguration.getModuleName();
                    JPMSModuleAccessor moduleAccessor = JPMSAccessorLoader.INSTANCE.findBootModule(moduleName);
                    if (moduleAccessor != null) {
                        for (String packageName : moduleConfiguration.getAddExports()) {
                            moduleAccessor.addExports(packageName, vestigeClassLoader);
                        }
                        for (String packageName : moduleConfiguration.getAddOpens()) {
                            moduleAccessor.addOpens(packageName, vestigeClassLoader);
                        }
                    }
                }
            }

            if (classLoaderConfiguration.isAttachmentScoped()) {
                name = name + " @ " + Integer.toHexString(System.identityHashCode(attachmentMap));
            }
            attachedVestigeClassLoader = new AttachedVestigeClassLoader(vestigeClassLoader, classLoaderDependencies, Arrays.toString(urls), name,
                    classLoaderConfiguration.isAttachmentScoped(), cache, delegateURLStreamHandlerFactory, delegateURLStreamHandler);
            vestigeClassLoader.setData(attachedVestigeClassLoader);
            if (classLoaderConfiguration.isAttachmentScoped()) {
                attachmentMap.put(key, vestigeClassLoader);
            } else {
                map.put(key, new WeakReference<AttachedVestigeClassLoader>(attachedVestigeClassLoader));
            }
        }
        return vestigeClassLoader.getData();
    }

    public static void setURLStreamHandlerFactoryDelegate(final DelegateURLStreamHandlerFactory delegateURLStreamHandlerFactory,
            final DelegateURLStreamHandler delegateURLStreamHandler, final Map<File, JarFile> cache) {
        VestigeJarURLStreamHandler vestigeJarURLStreamHandler = new VestigeJarURLStreamHandler(delegateURLStreamHandler, cache);
        delegateURLStreamHandler.setDelegate(vestigeJarURLStreamHandler);
        delegateURLStreamHandlerFactory.setDelegate(new URLStreamHandlerFactory() {

            @Override
            public URLStreamHandler createURLStreamHandler(final String protocol) {
                if ("jar".equals(protocol)) {
                    return delegateURLStreamHandler;
                }
                return null;
            }
        });
    }

    public void addAttachment(final AttachedVestigeClassLoader attachedVestigeClassLoader) {
        for (AttachedVestigeClassLoader dependency : attachedVestigeClassLoader.getDependencies()) {
            addAttachment(dependency);
        }
        int attachment = attachedVestigeClassLoader.getAttachments();
        attachedVestigeClassLoader.setAttachments(attachment + 1);
    }

    public void removeAttachment(final AttachedVestigeClassLoader attachedVestigeClassLoader) {
        for (AttachedVestigeClassLoader dependency : attachedVestigeClassLoader.getDependencies()) {
            removeAttachment(dependency);
        }
        int attachment = attachedVestigeClassLoader.getAttachments();
        attachedVestigeClassLoader.setAttachments(attachment - 1);
        if (attachment == 1) {
            clearCache(attachedVestigeClassLoader.getCache());
        }
    }

    public List<List<WeakReference<AttachedVestigeClassLoader>>> getAttachmentScopedAttachedClassLoaders() {
        return attachedClassLoaders;
    }

    @Override
    public List<WeakReference<AttachedVestigeClassLoader>> getAttachmentScopedUnattachedVestigeClassLoaders() {
        return unattached;
    }

}
