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

package com.googlecode.vestige.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.vestige.core.VestigeClassLoader;
import com.googlecode.vestige.core.VestigeExecutor;
import com.googlecode.vestige.core.parser.ClassesStringParser;
import com.googlecode.vestige.core.parser.NoStateStringParser;
import com.googlecode.vestige.core.parser.StringParser;

/**
 * @author Gael Lalire
 */
public class DefaultVestigePlatform implements VestigePlatform {

    private static final List<List<VestigeClassLoader<?>>> NO_DEPENDENCY_LIST = Collections.singletonList(Collections.<VestigeClassLoader<?>> singletonList(null));

    public static final String STARTSTOP_CLASS = "StartStop-Class";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVestigePlatform.class);

    private List<AttachedVestigeClassLoader> attached = new ArrayList<AttachedVestigeClassLoader>();

    private List<Boolean> started = new ArrayList<Boolean>();

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
        if (id == size) {
            started.add(Boolean.FALSE);
            attached.add(load);
        } else {
            started.set(id, Boolean.FALSE);
            attached.set(id, load);
        }
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
            started.add(Boolean.FALSE);
            attached.add(load);
        } else {
            started.set(id, Boolean.FALSE);
            attached.set(id, load);
        }
        clean();
        return id;
    }


    public void detach(final int id) {
        stop(id);
        int last = attached.size() - 1;
        if (id == last) {
            attached.remove(last);
            started.remove(last);
            last--;
            while (last >= 0 && attached.get(last) == null) {
                attached.remove(last);
                started.remove(last);
                last--;
            }
        } else {
            attached.set(id, null);
            started.set(id, null);
        }
        clean();
    }

    public boolean isStarted(final int id) {
        return started.get(id);
    }

    public void start(final int id) {
        AttachedVestigeClassLoader vestigeClassLoaders = attached.get(id);
        if (!started.get(id)) {
            load(vestigeClassLoaders);
            started.set(id, Boolean.TRUE);
        }
    }

    public void stop(final int id) {
        AttachedVestigeClassLoader vestigeClassLoaders = attached.get(id);
        if (started.get(id)) {
            unload(vestigeClassLoaders);
            started.set(id, Boolean.FALSE);
        }
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

    private void unload(final AttachedVestigeClassLoader attachedVestigeClassLoader) {
        VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
        if (removeAttachment(vestigeClassLoader)) {
            // stop method
            LOGGER.debug("Stop {} ", vestigeClassLoader);
            for (String startStopClass : vestigeClassLoader.getData().getStartStopClasses()) {
                LOGGER.trace("Invoke stop of {}", startStopClass);
                try {
                    try {
                        Class<?> loadClass = vestigeClassLoader.loadClass(startStopClass);
                        Method method = loadClass.getDeclaredMethod("stop");
                        vestigeExecutor.invoke(vestigeClassLoader, method, null);
                    } catch (NoSuchMethodException e) {
                        LOGGER.debug("No stop method found", e);
                    } catch (Exception e) {
                        LOGGER.error("Issue when invoking stop method", e);
                    }
                } finally {
                    LOGGER.trace("Stop of {} invoked", startStopClass);
                }
            }
        }
        for (AttachedVestigeClassLoader dep : attachedVestigeClassLoader.getDependencies()) {
            unload(dep);
        }
    }

    private void load(final AttachedVestigeClassLoader attachedVestigeClassLoader) {
        for (AttachedVestigeClassLoader dep : attachedVestigeClassLoader.getDependencies()) {
            load(dep);
        }
        VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
        if (addAttachment(vestigeClassLoader)) {
            // start method
            LOGGER.debug("Start {} ", vestigeClassLoader);
            for (String startStopClass : vestigeClassLoader.getData().getStartStopClasses()) {
                LOGGER.trace("Invoke start of {}", startStopClass);
                try {
                    try {
                        Class<?> loadClass = vestigeClassLoader.loadClass(startStopClass);
                        Method method = loadClass.getDeclaredMethod("start");
                        vestigeExecutor.invoke(vestigeClassLoader, method, null);
                    } catch (NoSuchMethodException e) {
                        LOGGER.debug("No start method found", e);
                    } catch (Exception e) {
                        LOGGER.error("Issue when invoking start method", e);
                    }
                } finally {
                    LOGGER.trace("Start of {} invoked", startStopClass);
                }
            }
        }
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
        if (pathsData == null) {
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

            vestigeClassLoader = vestigeExecutor.createVestigeClassLoader(ClassLoader.getSystemClassLoader(), convert(attachedVestigeClassLoader, classLoaderConfiguration),
                    classStringParser, resourceStringParser, urls);

            List<String> classes = new ArrayList<String>();
            for (URL url : urls) {
                try {
                    InputStream openStream = null;
                    Manifest manifest = null;
                    String file = url.getFile();
                    if (file != null && file.endsWith("/")) {
                        if (!"file".equals(url.getProtocol())) {
                            throw new IOException("protocol " + url.getProtocol() + " is not supported");
                        }
                        openStream = new URL(url, "META-INF/MANIFEST.MF").openStream();
                        try {
                            manifest = new Manifest(openStream);
                        } finally {
                            openStream.close();
                        }
                    } else {
                        openStream = url.openStream();
                        try {
                            JarInputStream jarInputStream = new JarInputStream(openStream);
                            try {
                                manifest = jarInputStream.getManifest();
                            } finally {
                                jarInputStream.close();
                            }
                        } finally {
                            openStream.close();
                        }
                    }
                    if (manifest != null) {
                        Attributes mainAttributes = manifest.getMainAttributes();
                        if (mainAttributes != null) {
                            String value = mainAttributes.getValue(STARTSTOP_CLASS);
                            if (value != null) {
                                classes.add(value);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("META-INF/MANIFEST.MF issue", e);
                }
            }
            if (classLoaderConfiguration.isAttachmentScoped()) {
                name = name + " @ " + Integer.toHexString(vestigeClassLoader.hashCode());
            }
            attachedVestigeClassLoader = new AttachedVestigeClassLoader(vestigeClassLoader, classLoaderDependencies, Arrays.toString(urls), classes, name);
            vestigeClassLoader.setData(attachedVestigeClassLoader);
            if (classLoaderConfiguration.isAttachmentScoped()) {
                attachmentMap.put(key, vestigeClassLoader);
            } else {
                map.put(key, new WeakReference<AttachedVestigeClassLoader>(attachedVestigeClassLoader));
            }
        }
        return vestigeClassLoader.getData();
    }

    public boolean addAttachment(final VestigeClassLoader<AttachedVestigeClassLoader> classLoader) {
        int attachment = classLoader.getData().getAttachments();
        classLoader.getData().setAttachments(attachment + 1);
        if (attachment == 0) {
            return true;
        }
        return false;
    }

    public boolean removeAttachment(final VestigeClassLoader<AttachedVestigeClassLoader> classLoader) {
        int attachment = classLoader.getData().getAttachments();
        classLoader.getData().setAttachments(attachment - 1);
        if (attachment == 1) {
            return true;
        }
        return false;
    }

}
