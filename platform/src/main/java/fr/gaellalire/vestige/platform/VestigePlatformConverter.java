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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.resource.JarFileResourceLocator;
import fr.gaellalire.vestige.jpms.JPMSInRepositoryModuleLayerAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerRepository;

/**
 * @author Gael Lalire
 */
public final class VestigePlatformConverter {

    private VestigePlatformConverter() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static AttachedVestigeClassLoader convertAttachedVestigeClassLoader(final Object oldAttachedVestigeClassLoader, final Object oldVestigePlatform,
            final VestigePlatform vestigePlatform, final Map<Object, JPMSInRepositoryModuleLayerAccessor> loadedModuleLayers) throws Exception {
        Class<?> oldAttachedVestigeClassLoaderClass = oldAttachedVestigeClassLoader.getClass();

        VestigeClassLoader uncheckedVestigeClassLoader = (VestigeClassLoader<?>) oldAttachedVestigeClassLoaderClass.getMethod("getVestigeClassLoader")
                .invoke(oldAttachedVestigeClassLoader);
        uncheckedVestigeClassLoader.setDataProtector(oldVestigePlatform, vestigePlatform);
        Object data = uncheckedVestigeClassLoader.getData(vestigePlatform);
        if (data != oldAttachedVestigeClassLoader) {
            // already converted
            return (AttachedVestigeClassLoader) data;
        }

        List<Object> dependencies = (List<Object>) oldAttachedVestigeClassLoaderClass.getMethod("getDependencies").invoke(oldAttachedVestigeClassLoader);
        List<AttachedVestigeClassLoader> list = new ArrayList<AttachedVestigeClassLoader>(dependencies.size());
        for (Object dependency : dependencies) {
            list.add(convertAttachedVestigeClassLoader(dependency, oldVestigePlatform, vestigePlatform, loadedModuleLayers));
        }
        AttachedVestigeClassLoader attachedVestigeClassLoader = new AttachedVestigeClassLoader(
                (Serializable) oldAttachedVestigeClassLoaderClass.getMethod("getKey").invoke(oldAttachedVestigeClassLoader), uncheckedVestigeClassLoader, list,
                (String) oldAttachedVestigeClassLoaderClass.getMethod("getName").invoke(oldAttachedVestigeClassLoader),
                (Boolean) oldAttachedVestigeClassLoaderClass.getMethod("isAttachmentScoped").invoke(oldAttachedVestigeClassLoader),
                (JarFileResourceLocator[]) oldAttachedVestigeClassLoaderClass.getMethod("getCache").invoke(oldAttachedVestigeClassLoader),
                loadedModuleLayers.get(oldAttachedVestigeClassLoaderClass.getMethod("getModuleLayer").invoke(oldAttachedVestigeClassLoader)),
                (Boolean) oldAttachedVestigeClassLoaderClass.getMethod("isJPMSActivated").invoke(oldAttachedVestigeClassLoader));

        attachedVestigeClassLoader.setAttachments((Integer) oldAttachedVestigeClassLoaderClass.getMethod("getAttachments").invoke(oldAttachedVestigeClassLoader));

        uncheckedVestigeClassLoader.setData(vestigePlatform, attachedVestigeClassLoader);
        return attachedVestigeClassLoader;
    }

    public static JPMSInRepositoryModuleLayerAccessor convertLayerAccessor(final Object oldLayerAccessor, final JPMSModuleLayerRepository moduleLayerRepository,
            final Map<Object, JPMSInRepositoryModuleLayerAccessor> convertedLayerAccessorByLayerAccessor, final Method addMethod) throws Exception {
        JPMSInRepositoryModuleLayerAccessor convertedLayerAccessor = convertedLayerAccessorByLayerAccessor.get(oldLayerAccessor);
        if (convertedLayerAccessor != null) {
            return convertedLayerAccessor;
        }
        Class<?> oldJPMSInRepositoryModuleLayerAccessorClass = oldLayerAccessor.getClass();

        List<JPMSInRepositoryModuleLayerAccessor> loadedParents = new ArrayList<JPMSInRepositoryModuleLayerAccessor>();
        for (Object parent : (List<Object>) oldJPMSInRepositoryModuleLayerAccessorClass.getMethod("parents").invoke(oldLayerAccessor)) {
            loadedParents.add(convertLayerAccessor(parent, moduleLayerRepository, convertedLayerAccessorByLayerAccessor, addMethod));
        }
        Class<?> oldLayerAccessorClass = oldLayerAccessor.getClass();
        Object controllerProxy = oldLayerAccessorClass.getMethod("getController").invoke(oldLayerAccessor);
        Object controller = controllerProxy.getClass().getMethod("getController").invoke(controllerProxy);

        convertedLayerAccessor = (JPMSInRepositoryModuleLayerAccessor) addMethod.invoke(moduleLayerRepository, loadedParents, controller);
        convertedLayerAccessorByLayerAccessor.put(oldLayerAccessor, convertedLayerAccessor);
        return convertedLayerAccessor;
    }

    public static JPMSModuleLayerRepository convertModuleRepository(final Object oldModuleLayerRepository,
            final Map<Object, JPMSInRepositoryModuleLayerAccessor> loadedModuleLayers) throws Exception {
        if (oldModuleLayerRepository == null) {
            return null;
        }

        Class<?> oldJPMSModuleLayerRepositoryClass = oldModuleLayerRepository.getClass();

        // create new instance by reflection because of Java9 dependency
        Class<?> vestigeRepositoryClass = DefaultVestigePlatform.class.getClassLoader().loadClass("fr.gaellalire.vestige.jpms.Java9JPMSModuleLayerRepository");
        JPMSModuleLayerRepository moduleLayerRepository = (JPMSModuleLayerRepository) vestigeRepositoryClass.getConstructor().newInstance();

        // public JPMSInRepositoryModuleLayerAccessor add(final List<Java9JPMSInRepositoryModuleLayerAccessor> parents, final Controller controller) {
        Method addMethod = moduleLayerRepository.getClass().getMethod("add", List.class, Class.forName("java.lang.ModuleLayer$Controller"));

        int size = (Integer) oldJPMSModuleLayerRepositoryClass.getMethod("size").invoke(oldModuleLayerRepository);

        Method getMethod = oldJPMSModuleLayerRepositoryClass.getMethod("get", int.class);
        Object oldBootLayer = getMethod.invoke(oldModuleLayerRepository, JPMSModuleLayerRepository.BOOT_LAYER_INDEX);

        JPMSInRepositoryModuleLayerAccessor bootLayer = moduleLayerRepository.get(JPMSModuleLayerRepository.BOOT_LAYER_INDEX);

        loadedModuleLayers.put(oldBootLayer, bootLayer);
        for (int i = 0; i < size; i++) {
            Object layerAccessor = getMethod.invoke(oldModuleLayerRepository, i);
            if (layerAccessor != null) {
                convertLayerAccessor(layerAccessor, moduleLayerRepository, loadedModuleLayers, addMethod);
            }
        }

        return moduleLayerRepository;
    }

    public static VestigePlatform convert(final Object oldVestigePlatform) throws Exception {

        Map<Object, JPMSInRepositoryModuleLayerAccessor> loadedModuleLayers = new IdentityHashMap<Object, JPMSInRepositoryModuleLayerAccessor>();

        Object oldModuleLayerRepository = oldVestigePlatform.getClass().getMethod("getModuleLayerRepository").invoke(oldVestigePlatform);
        JPMSModuleLayerRepository moduleLayerRepository = convertModuleRepository(oldModuleLayerRepository, loadedModuleLayers);

        DefaultVestigePlatform loadedVestigePlatform = new DefaultVestigePlatform(moduleLayerRepository);

        // fetch fields
        List<AttachedVestigeClassLoader> attached = loadedVestigePlatform.getAttached();
        List<WeakReference<AttachedVestigeClassLoader>> unattached = loadedVestigePlatform.getAttachmentScopedUnattachedVestigeClassLoaders();
        List<List<WeakReference<AttachedVestigeClassLoader>>> attachedClassLoaders = loadedVestigePlatform.getAttachmentScopedAttachedClassLoaders();
        Map<Serializable, WeakReference<AttachedVestigeClassLoader>> map = loadedVestigePlatform.getMap();

        Class<?> oldVestigePlatformClass = oldVestigePlatform.getClass();

        // fill fields
        Set<Integer> attachments = (Set<Integer>) oldVestigePlatformClass.getMethod("getAttachments").invoke(oldVestigePlatform);
        for (Integer id : attachments) {
            attached.add(convertAttachedVestigeClassLoader(oldVestigePlatformClass.getMethod("getAttachedVestigeClassLoader", int.class).invoke(oldVestigePlatform, id),
                    oldVestigePlatform, loadedVestigePlatform, loadedModuleLayers));
        }

        List<WeakReference<?>> unattachedVestigeClassLoaders = (List<WeakReference<?>>) oldVestigePlatformClass.getMethod("getAttachmentScopedUnattachedVestigeClassLoaders")
                .invoke(oldVestigePlatform);
        // vestigePlatform.getAttachmentScopedUnattachedVestigeClassLoaders();
        for (WeakReference<?> weakReference : unattachedVestigeClassLoaders) {
            Object unattachedVestigeClassLoader = weakReference.get();
            if (unattachedVestigeClassLoader != null) {
                unattached.add(new WeakReference<AttachedVestigeClassLoader>(
                        convertAttachedVestigeClassLoader(unattachedVestigeClassLoader, oldVestigePlatform, loadedVestigePlatform, loadedModuleLayers)));
            }
        }

        List<List<WeakReference<?>>> attachmentScopedAttachedClassLoaders = (List<List<WeakReference<?>>>) oldVestigePlatformClass
                .getMethod("getAttachmentScopedAttachedClassLoaders").invoke(oldVestigePlatform);
        for (List<WeakReference<?>> list : attachmentScopedAttachedClassLoaders) {
            if (list == null) {
                attachedClassLoaders.add(null);
                continue;
            }
            List<WeakReference<AttachedVestigeClassLoader>> destList = new ArrayList<WeakReference<AttachedVestigeClassLoader>>(list.size());
            for (WeakReference<?> weakReference : list) {
                Object attachedVestigeClassLoader = weakReference.get();
                if (attachedVestigeClassLoader != null) {
                    destList.add(new WeakReference<AttachedVestigeClassLoader>(
                            convertAttachedVestigeClassLoader(attachedVestigeClassLoader, oldVestigePlatform, loadedVestigePlatform, loadedModuleLayers)));
                }
            }
            attachedClassLoaders.add(destList);
        }

        Method method = oldVestigePlatformClass.getMethod("getAttachedVestigeClassLoaderByKey", Serializable.class);
        List<Serializable> loadedArtifact = (List<Serializable>) oldVestigePlatformClass.getMethod("getClassLoaderKeys").invoke(oldVestigePlatform);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        List<AttachedVestigeClassLoader> attachedVestigeClassLoaders = new ArrayList<AttachedVestigeClassLoader>();
        for (Serializable mavenArtifact : loadedArtifact) {
            Object attachedVestigeClassLoader = method.invoke(oldVestigePlatform, mavenArtifact);
            if (attachedVestigeClassLoader != null) {
                attachedVestigeClassLoaders.add(convertAttachedVestigeClassLoader(attachedVestigeClassLoader, oldVestigePlatform, loadedVestigePlatform, loadedModuleLayers));
                objectOutputStream.writeObject(mavenArtifact);
            }
        }

        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            @Override
            protected Class<?> resolveClass(final java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
            }
        };

        for (AttachedVestigeClassLoader classLoader : attachedVestigeClassLoaders) {
            Serializable readObject = (Serializable) objectInputStream.readObject();
            map.put(readObject, new WeakReference<AttachedVestigeClassLoader>(classLoader));
        }

        return loadedVestigePlatform;
    }

}
