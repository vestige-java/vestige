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
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import fr.gaellalire.vestige.core.ModuleEncapsulationEnforcer;
import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.VestigeClassLoaderConfiguration;
import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.core.parser.ClassStringParser;
import fr.gaellalire.vestige.core.parser.NoStateStringParser;
import fr.gaellalire.vestige.core.parser.ResourceEncapsulationEnforcer;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.core.resource.JarFileResourceLocator;
import fr.gaellalire.vestige.core.resource.PatchedVestigeResourceLocator;
import fr.gaellalire.vestige.core.resource.SecureFile;
import fr.gaellalire.vestige.core.resource.SecureFile.Mode;
import fr.gaellalire.vestige.core.resource.SecureJarFileResourceLocator;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandler;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.core.weak.CloseableReaperHelper;
import fr.gaellalire.vestige.core.weak.VestigeReaper;
import fr.gaellalire.vestige.jpms.JPMSAccessorLoader;
import fr.gaellalire.vestige.jpms.JPMSInRepositoryConfiguration;
import fr.gaellalire.vestige.jpms.JPMSInRepositoryModuleLayerAccessor;
import fr.gaellalire.vestige.jpms.JPMSInRepositoryModuleLayerParentList;
import fr.gaellalire.vestige.jpms.JPMSModuleAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerAccessor;
import fr.gaellalire.vestige.jpms.JPMSModuleLayerRepository;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;

/**
 * @author Gael Lalire
 */
public class DefaultVestigePlatform implements VestigePlatform {

    private static final JPMSModuleLayerAccessor BOOT_LAYER;

    static {
        if (JPMSAccessorLoader.INSTANCE == null) {
            BOOT_LAYER = null;
        } else {
            BOOT_LAYER = JPMSAccessorLoader.INSTANCE.bootLayer();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVestigePlatform.class);

    private static final VestigeClassLoaderConfiguration[] SELF_SEARCHED = new VestigeClassLoaderConfiguration[] {VestigeClassLoaderConfiguration.THIS_PARENT_SEARCHED};

    private static final VestigeClassLoaderConfiguration[][] NO_DEPENDENCY_LIST = new VestigeClassLoaderConfiguration[][] {SELF_SEARCHED};

    private static final VestigeClassLoaderConfiguration[][] BEFORE_PARENT_NO_DEPENDENCY_LIST = new VestigeClassLoaderConfiguration[][] {
            new VestigeClassLoaderConfiguration[] {VestigeClassLoaderConfiguration.THIS_PARENT_UNSEARCHED, null}};

    public static final InheritableThreadLocal<Map<String, String>> BASIC_MDC_HOOK = new InheritableThreadLocal<Map<String, String>>() {

        private Map<String, String> map = new HashMap<String, String>() {

            private static final long serialVersionUID = -5393662769412628524L;

            @Override
            public Set<String> keySet() {
                return MDC.getCopyOfContextMap().keySet();
            }

            @Override
            public String get(final Object key) {
                return MDC.get((String) key);
            }

            @Override
            public String put(final String key, final String value) {
                MDC.put(key, value);
                return null;
            }

            @Override
            public String remove(final Object key) {
                MDC.remove((String) key);
                return null;
            }

            @Override
            public Set<java.util.Map.Entry<String, String>> entrySet() {
                return MDC.getCopyOfContextMap().entrySet();
            }

        };

        @Override
        public Map<String, String> get() {
            return map;
        }

        @Override
        public void remove() {
            MDC.clear();
        }
    };

    /**
     * Attached classloaders (all scope).
     */
    private List<AttachedVestigeClassLoader> attached = new ArrayList<AttachedVestigeClassLoader>();

    /**
     * Attachment scoped attached classloaders.
     */
    private List<List<WeakReference<AttachedVestigeClassLoader>>> attachedClassLoaders = new ArrayList<List<WeakReference<AttachedVestigeClassLoader>>>();

    /**
     * Attachment scoped unattached classloaders.
     */
    private List<WeakReference<AttachedVestigeClassLoader>> unattached = new ArrayList<WeakReference<AttachedVestigeClassLoader>>();

    /**
     * Platform scoped classloaders.
     */
    private Map<Serializable, WeakReference<AttachedVestigeClassLoader>> map = new HashMap<Serializable, WeakReference<AttachedVestigeClassLoader>>();

    private JPMSModuleLayerRepository moduleLayerRepository;

    private VestigeReaper vestigeReaper;

    public DefaultVestigePlatform(final VestigeReaper vestigeReaper, final JPMSModuleLayerRepository moduleLayerRepository) {
        this.vestigeReaper = vestigeReaper;
        this.moduleLayerRepository = moduleLayerRepository;
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

    public int attach(final ClassLoaderConfiguration classLoaderConfiguration, final AttachmentVerificationMetadata verificationMetadata, final VestigeWorker vestigeWorker,
            final AttachmentResult result) throws InterruptedException, IOException {
        int size = attached.size();
        int id = 0;
        while (id < size) {
            if (attached.get(id) == null) {
                break;
            }
            id++;
        }
        Map<Serializable, VestigeClassLoader<AttachedVestigeClassLoader>> attachmentMap = new HashMap<Serializable, VestigeClassLoader<AttachedVestigeClassLoader>>();
        VerifierContext verifierContext = null;
        if (verificationMetadata != null) {
            if (result != null) {
                verificationMetadata.prepareForUseCheck();
                verifierContext = new PartialJarVerifierContext(verificationMetadata);
            } else if (verificationMetadata != null) {
                verifierContext = new CompleteMetadataVerifierContext(verificationMetadata);
            }
        }
        AttachedVestigeClassLoader load = attachDependencies(attachmentMap, classLoaderConfiguration, vestigeWorker, verifierContext);
        if (result != null) {
            if (verificationMetadata != null) {
                AttachmentVerificationMetadata extractUsed = verificationMetadata.extractUsed();
                if (extractUsed != null) {
                    result.setUsedVerificationMetadata(extractUsed.toString());
                }
                AttachmentVerificationMetadata extractUnused = verificationMetadata.extractUnused();
                if (extractUnused != null) {
                    result.setRemainingVerificationMetadata(extractUnused.toString());
                } else {
                    result.setComplete(true);
                }
            }

            result.setAttachedVestigeClassLoader(load);
        }

        Iterator<SoftReference<?>> iterator = load.getSoftReferences().iterator();
        while (iterator.hasNext()) {
            SoftReference<?> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
        Collection<VestigeClassLoader<AttachedVestigeClassLoader>> values = attachmentMap.values();
        int valueSize = values.size();
        List<WeakReference<AttachedVestigeClassLoader>> list = null;
        if (valueSize != 0) {
            list = new ArrayList<WeakReference<AttachedVestigeClassLoader>>(valueSize);
            for (VestigeClassLoader<AttachedVestigeClassLoader> classLoader : values) {
                list.add(new WeakReference<AttachedVestigeClassLoader>(classLoader.getData(this)));
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
        AttachedVestigeClassLoader load = classLoader.getData(this);
        Iterator<SoftReference<?>> iterator = load.getSoftReferences().iterator();
        while (iterator.hasNext()) {
            SoftReference<?> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
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

    public void clearCache(final VestigeResourceLocator[] cache) {
        synchronized (cache) {
            for (VestigeResourceLocator entry : cache) {
                try {
                    LOGGER.info("Closing {}", entry);
                    entry.close();
                } catch (IOException e) {
                    LOGGER.error("Unable to close", e);
                }
            }
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
        Iterator<SoftReference<?>> iterator = unload.getSoftReferences().iterator();
        while (iterator.hasNext()) {
            SoftReference<?> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
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

    public int addConvertedPath(final int path, final AttachedVestigeClassLoader attachedVestigeClassLoader, final ClassLoaderConfiguration conf, final int lastBeforeIndex,
            final List<VestigeClassLoaderConfiguration> classLoaderConfigurations) {
        AttachedVestigeClassLoader currentAttachedVestigeClassLoader = attachedVestigeClassLoader;
        ClassLoaderConfiguration currentConf = conf;
        int currentPath = path;
        while (currentPath != -1) {
            int dependencyIndex = currentConf.getDependencyIndex(currentPath);
            currentPath = currentConf.getDependencyPathIndex(currentPath);
            currentAttachedVestigeClassLoader = currentAttachedVestigeClassLoader.getDependencies().get(dependencyIndex);
            currentConf = currentConf.getDependencies().get(dependencyIndex);
        }
        // currentAttachedVestigeClassLoader.getVestigeClassLoader == null if this is the AttachedVestigeClassLoader given in parameter because it is a temporary
        // AttachedVestigeClassLoader
        VestigeClassLoaderConfiguration vestigeClassLoaderConfiguration = currentAttachedVestigeClassLoader.getVestigeClassLoader();
        boolean beforeParent = currentConf.getBeforeUrls().size() != 0;
        if (vestigeClassLoaderConfiguration == null) {
            if (!beforeParent && classLoaderConfigurations.size() == lastBeforeIndex) {
                // first after parent
                vestigeClassLoaderConfiguration = VestigeClassLoaderConfiguration.THIS_PARENT_SEARCHED;
            } else {
                vestigeClassLoaderConfiguration = VestigeClassLoaderConfiguration.THIS_PARENT_UNSEARCHED;
            }
        } else if (!beforeParent && classLoaderConfigurations.size() == lastBeforeIndex) {
            // first after parent
            vestigeClassLoaderConfiguration = vestigeClassLoaderConfiguration.getVestigeClassLoader().getParentSeachedClassLoaderConfiguration();
        }
        if (beforeParent) {
            classLoaderConfigurations.add(lastBeforeIndex, vestigeClassLoaderConfiguration);
            return lastBeforeIndex + 1;
        }
        classLoaderConfigurations.add(vestigeClassLoaderConfiguration);
        return lastBeforeIndex;
    }

    public VestigeClassLoaderConfiguration[][] convert(final AttachedVestigeClassLoader attachedVestigeClassLoader, final ClassLoaderConfiguration conf) {
        List<List<Integer>> pathsData = conf.getPathIdsList();
        if (pathsData == null || pathsData.size() == 0) {
            if (conf.getBeforeUrls().size() == 0) {
                return NO_DEPENDENCY_LIST;
            } else {
                return BEFORE_PARENT_NO_DEPENDENCY_LIST;
            }
        }
        VestigeClassLoaderConfiguration[][] data = new VestigeClassLoaderConfiguration[pathsData.size()][];
        int i = 0;
        for (List<Integer> paths : pathsData) {
            List<VestigeClassLoaderConfiguration> classLoaderConfigurations = null;
            if (paths != null) {
                classLoaderConfigurations = new ArrayList<VestigeClassLoaderConfiguration>(paths.size());
                int lastBeforeIndex = 0;
                for (Integer path : paths) {
                    lastBeforeIndex = addConvertedPath(path, attachedVestigeClassLoader, conf, lastBeforeIndex, classLoaderConfigurations);
                }
                if (classLoaderConfigurations.size() == lastBeforeIndex) {
                    // all path are before parent, so we must add parent alone to avoid ClassNotFound on java.lang.Object
                    classLoaderConfigurations.add(null);
                }
                data[i] = classLoaderConfigurations.toArray(new VestigeClassLoaderConfiguration[classLoaderConfigurations.size()]);
            }
            i++;
        }
        return data;
    }

    public VestigeResourceLocator verifyAbstractJar(final AbstractFileWithMetadata fileWithMetadata, final VestigeJar patchVestigeJar, final VestigeJar[] vestigeJars,
            final JarVerifier jarVerifier) throws IOException {
        if (jarVerifier == null) {
            vestigeJars[0] = new DefaultVestigeJar(fileWithMetadata, null, patchVestigeJar, null, vestigeReaper);
            return new JarFileResourceLocator(fileWithMetadata.getFile(), fileWithMetadata.getCodeBase());
        }
        File file = fileWithMetadata.getFile();
        if (!file.exists()) {
            throw new IOException("Cannot verify checkum, file does not exist");
        }
        boolean closeSecureJarFile = true;
        long[] sizeHolder = new long[1];
        String sha512;
        SecureFile secureJarFile = new SecureFile(file, Mode.PRIVATE_MAP);
        try {
            InputStream inputStream = secureJarFile.getInputStream();
            try {
                try {
                    List<String> checksums = FileWithMetadata.createChecksum(inputStream, Arrays.asList("SHA-512"), sizeHolder);
                    sha512 = checksums.get(0);
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException("Cannot verify checkum", e);
                } catch (NoSuchProviderException e) {
                    throw new IOException("Cannot verify checkum", e);
                }

            } finally {
                inputStream.close();
            }
            vestigeReaper.addReapable(secureJarFile, new CloseableReaperHelper(secureJarFile.getCloseable()));
            closeSecureJarFile = false;
        } finally {
            if (closeSecureJarFile) {
                secureJarFile.close();
            }
        }

        jarVerifier.verify(sizeHolder[0], sha512);

        SecureJarFileResourceLocator secureJarFileResourceLocator = new SecureJarFileResourceLocator(secureJarFile, fileWithMetadata.getCodeBase());
        vestigeJars[0] = new DefaultVestigeJar(fileWithMetadata, secureJarFile, patchVestigeJar, secureJarFileResourceLocator, vestigeReaper);
        vestigeReaper.addReapable(vestigeJars[0], new CloseableReaperHelper(secureJarFileResourceLocator));
        return secureJarFileResourceLocator;
    }

    public VestigeResourceLocator verifyJar(final FileWithMetadata fileWithMetadata, final List<VestigeJar> vestigeJars, final VerifierContext verifierContext) throws IOException {
        VestigeJar[] vestigeJarKeeper = new VestigeJar[1];
        final PatchFileWithMetadata patch = fileWithMetadata.getPatch();
        JarVerifier standardJarVerifier;

        if (verifierContext == null) {
            standardJarVerifier = null;
        } else {
            standardJarVerifier = new JarVerifier() {

                @Override
                public void verify(final long size, final String sha512) throws IOException {
                    if (!verifierContext.verify(size, sha512)) {
                        throw new IOException("Detected corruption of file " + fileWithMetadata.getFile());
                    }
                }
            };
        }
        if (patch != null) {
            VestigeResourceLocator verifyJar = verifyAbstractJar(fileWithMetadata, null, vestigeJarKeeper, standardJarVerifier);
            JarVerifier patchJarVerifier;
            if (verifierContext == null) {
                patchJarVerifier = null;
            } else {
                if (!verifierContext.patch()) {
                    throw new IOException("Detected missing patch file " + patch.getFile());
                }
                patchJarVerifier = new JarVerifier() {

                    @Override
                    public void verify(final long size, final String sha512) throws IOException {
                        if (!verifierContext.verify(size, sha512)) {
                            throw new IOException("Detected corruption of patch file " + patch.getFile());
                        }
                    }
                };
            }
            VestigeResourceLocator verifyPatchJar = verifyAbstractJar(patch, vestigeJarKeeper[0], vestigeJarKeeper, patchJarVerifier);
            vestigeJars.add(vestigeJarKeeper[0]);
            return new PatchedVestigeResourceLocator(verifyJar, verifyPatchJar, false);
        }
        VestigeResourceLocator verifyAbstractJar = verifyAbstractJar(fileWithMetadata, null, vestigeJarKeeper, standardJarVerifier);
        vestigeJars.add(vestigeJarKeeper[0]);
        return verifyAbstractJar;
    }

    private AttachedVestigeClassLoader attachDependencies(final Map<Serializable, VestigeClassLoader<AttachedVestigeClassLoader>> attachmentMap,
            final ClassLoaderConfiguration classLoaderConfiguration, final VestigeWorker vestigeWorker, final VerifierContext verifierContext)
            throws InterruptedException, IOException {
        String presumedVerificationMetada = null;
        Serializable key = classLoaderConfiguration.getKey();
        if (verifierContext != null) {
            presumedVerificationMetada = verifierContext.getCurrentVerificationMetadata().toString();
            key = new VerifiedKey(key, presumedVerificationMetada);
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
        if (vestigeClassLoader != null) {
            // FIXME verifierContext need to manage reference (@n)
            return vestigeClassLoader.getData(this);
        }

        JPMSNamedModulesConfiguration namedModulesConfiguration = classLoaderConfiguration.getNamedModulesConfiguration();
        boolean selfNeedModuleDefine = BOOT_LAYER != null && namedModulesConfiguration != null;
        JPMSInRepositoryModuleLayerParentList moduleLayerList = null;
        if (selfNeedModuleDefine) {
            moduleLayerList = moduleLayerRepository.createModuleLayerList();
            moduleLayerList.addInRepositoryModuleLayerByIndex(JPMSModuleLayerRepository.BOOT_LAYER_INDEX);
        }

        List<ClassLoaderConfiguration> configurationDependencies = classLoaderConfiguration.getDependencies();
        List<AttachedVestigeClassLoader> classLoaderDependencies = new ArrayList<AttachedVestigeClassLoader>();

        for (ClassLoaderConfiguration configurationDependency : configurationDependencies) {
            if (verifierContext != null && !verifierContext.nextDependency()) {
                throw new IOException("Too many dependencies for " + key);
            }
            if (verifierContext != null) {
                verifierContext.pushDependency();
            }
            AttachedVestigeClassLoader attachDependency = attachDependencies(attachmentMap, configurationDependency, vestigeWorker, verifierContext);
            if (verifierContext != null) {
                verifierContext.popDependency();
            }
            classLoaderDependencies.add(attachDependency);
            if (selfNeedModuleDefine) {
                moduleLayerList.addInRepositoryModuleLayerByIndex(attachDependency.getModuleLayer().getRepositoryIndex());
            }
        }
        if (verifierContext != null && !verifierContext.endOfDependencies()) {
            throw new IOException("Not enough dependencies for " + key);
        }

        // search inside jar after dependencies
        // classLoaderDependencies.add(null);

        List<VestigeJar> vestigeJars = new ArrayList<VestigeJar>();
        List<FileWithMetadata> afterUrls = classLoaderConfiguration.getAfterUrls();
        List<FileWithMetadata> beforeUrls = classLoaderConfiguration.getBeforeUrls();
        VestigeResourceLocator[] urls = new VestigeResourceLocator[beforeUrls.size() + afterUrls.size()];
        if (verifierContext != null) {
            verifierContext.selectBeforeJars();
        }
        for (int i = 0; i < beforeUrls.size(); i++) {
            if (verifierContext != null && !verifierContext.nextJar()) {
                throw new IOException("Unexpected " + beforeUrls.get(i).getFile() + " before file");
            }
            urls[i] = verifyJar(beforeUrls.get(i), vestigeJars, verifierContext);
        }
        if (verifierContext != null && !verifierContext.endOfJars()) {
            throw new IOException("Before file of " + key + " is missing jars");
        }
        if (verifierContext != null) {
            verifierContext.selectAfterJars();
        }
        for (int i = 0; i < afterUrls.size(); i++) {
            if (verifierContext != null && !verifierContext.nextJar()) {
                throw new IOException("Unexpected " + afterUrls.get(i).getFile() + " after file");
            }
            urls[i + beforeUrls.size()] = verifyJar(afterUrls.get(i), vestigeJars, verifierContext);
        }
        if (verifierContext != null && !verifierContext.endOfJars()) {
            throw new IOException("After file of " + key + " is missing jars");
        }
        if (verifierContext != null) {
            String validatedVerificationMetada = verifierContext.getValidatedCurrentVerificationMetadata().toString();
            if (!validatedVerificationMetada.equals(presumedVerificationMetada)) {
                // try the cache again
                key = new VerifiedKey(classLoaderConfiguration.getKey(), validatedVerificationMetada);
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
                if (vestigeClassLoader != null) {
                    // FIXME verifierContext need to manage reference (@n)
                    return vestigeClassLoader.getData(this);
                }
            }
        }

        String name = classLoaderConfiguration.getName();

        // for vestige class loader : null == current classloader
        AttachedVestigeClassLoader attachedVestigeClassLoader = new AttachedVestigeClassLoader(classLoaderDependencies);
        StringParser resourceStringParser = classLoaderConfiguration.getPathIdsPositionByResourceName();
        StringParser classStringParser;
        if (resourceStringParser == null) {
            resourceStringParser = new NoStateStringParser(0);
            classStringParser = resourceStringParser;
        } else {
            classStringParser = classLoaderConfiguration.getPathIdsPositionByClassName();
            if (classStringParser == null) {
                classStringParser = new ClassStringParser(resourceStringParser);
            }
        }

        // create classloader with executor to remove this protection domain from access control

        ModuleEncapsulationEnforcer moduleEncapsulationEnforcer = null;
        JPMSInRepositoryConfiguration<VestigeClassLoader<AttachedVestigeClassLoader>> configuration = null;
        if (selfNeedModuleDefine) {
            List<File> beforeFiles = new ArrayList<File>(beforeUrls.size());
            for (FileWithMetadata secureFile : beforeUrls) {
                beforeFiles.add(secureFile.getFile());
            }
            List<File> afterFiles = new ArrayList<File>(afterUrls.size());
            for (FileWithMetadata secureFile : afterUrls) {
                afterFiles.add(secureFile.getFile());
            }
            configuration = moduleLayerList.createConfiguration(beforeFiles, afterFiles, null, this);
            moduleEncapsulationEnforcer = configuration.getModuleEncapsulationEnforcer();
            resourceStringParser = new ResourceEncapsulationEnforcer(resourceStringParser, configuration.getEncapsulatedPackageNames(), -1);
        }
        vestigeClassLoader = vestigeWorker.createVestigeClassLoader(ClassLoader.getSystemClassLoader(), convert(attachedVestigeClassLoader, classLoaderConfiguration),
                classStringParser, resourceStringParser, moduleEncapsulationEnforcer, urls);
        vestigeClassLoader.setDataProtector(null, this);

        final VestigeClassLoader<AttachedVestigeClassLoader> finalClassLoader = vestigeClassLoader;

        if (classLoaderConfiguration.isAttachmentScoped()) {
            name = name + " @ " + Integer.toHexString(System.identityHashCode(attachmentMap));
        }
        attachedVestigeClassLoader = new AttachedVestigeClassLoader(vestigeClassLoader, classLoaderDependencies, name, classLoaderConfiguration.isAttachmentScoped(), urls, null,
                namedModulesConfiguration != null, vestigeJars, verifierContext != null);
        vestigeClassLoader.setData(this, attachedVestigeClassLoader);

        JPMSInRepositoryModuleLayerAccessor moduleLayer = null;
        if (selfNeedModuleDefine) {
            moduleLayer = configuration.defineModules(new Function<String, VestigeClassLoader<AttachedVestigeClassLoader>, RuntimeException>() {

                @Override
                public VestigeClassLoader<AttachedVestigeClassLoader> apply(final String t) {
                    return finalClassLoader;
                }
            });

            if (namedModulesConfiguration != null) {
                Set<AddAccessibility> addExportsList = namedModulesConfiguration.getAddExports();
                if (addExportsList != null) {
                    for (AddAccessibility addAccessibility : addExportsList) {
                        moduleLayer.findModule(addAccessibility.getSource()).addExports(addAccessibility.getPn(), addAccessibility.getTarget());
                    }
                }
                Set<AddAccessibility> addOpensList = namedModulesConfiguration.getAddOpens();
                if (addOpensList != null) {
                    for (AddAccessibility addAccessibility : addOpensList) {
                        moduleLayer.findModule(addAccessibility.getSource()).addOpens(addAccessibility.getPn(), addAccessibility.getTarget());
                    }
                }
                Set<AddReads> addReadsList = namedModulesConfiguration.getAddReads();
                if (addReadsList != null) {
                    for (AddReads addReads : addReadsList) {
                        moduleLayer.findModule(addReads.getSource()).addReads(addReads.getTarget());
                    }
                }
            }
        }

        if (BOOT_LAYER != null) {
            JPMSClassLoaderConfiguration jpmsConfiguration = classLoaderConfiguration.getModuleConfiguration();
            for (ModuleConfiguration moduleConfiguration : jpmsConfiguration.getModuleConfigurations()) {
                String targetModuleName = moduleConfiguration.getTargetModuleName();
                String moduleName = moduleConfiguration.getModuleName();
                if (selfNeedModuleDefine) {
                    JPMSModuleAccessor moduleAccessor = moduleLayer.findModule(targetModuleName);
                    for (String packageName : moduleConfiguration.getAddExports()) {
                        moduleAccessor.requireBootAddExports(moduleName, packageName);
                    }
                    for (String packageName : moduleConfiguration.getAddOpens()) {
                        moduleAccessor.requireBootAddOpens(moduleName, packageName);
                    }
                } else {
                    JPMSModuleAccessor moduleAccessor = BOOT_LAYER.findModule(moduleName);
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
        }

        if (classLoaderConfiguration.isMdcIncluded()) {
            Thread currentThread = Thread.currentThread();
            ClassLoader contextClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(vestigeClassLoader);
            try {
                Object basicMDCAdapter = vestigeClassLoader.loadClass("org.slf4j.MDC").getMethod("getMDCAdapter").invoke(null);
                Class<? extends Object> class1 = basicMDCAdapter.getClass();
                if ("org.slf4j.helpers.BasicMDCAdapter".equals(class1.getName())) {
                    if (selfNeedModuleDefine) {
                        JPMSModuleAccessor slf4jModule = moduleLayer.findModule("org.slf4j");
                        if (slf4jModule != null) {
                            slf4jModule.addOpens("org.slf4j.helpers", DefaultVestigePlatform.class);
                        }
                    }
                    Field declaredField = class1.getDeclaredField("inheritableThreadLocal");
                    declaredField.setAccessible(true);
                    try {
                        declaredField.set(basicMDCAdapter, BASIC_MDC_HOOK);
                    } finally {
                        declaredField.setAccessible(false);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to redirect MDC", e);
            } catch (UnsupportedClassVersionError e) {
                LOGGER.warn("Unable to redirect MDC", e);
            } catch (NoClassDefFoundError e) {
                LOGGER.warn("Unable to redirect MDC", e);
            } finally {
                currentThread.setContextClassLoader(contextClassLoader);
            }

        }

        if (classLoaderConfiguration.isAttachmentScoped()) {
            attachmentMap.put(key, vestigeClassLoader);
        } else {
            map.put(key, new WeakReference<AttachedVestigeClassLoader>(attachedVestigeClassLoader));
        }
        return vestigeClassLoader.getData(this);
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

    @Override
    public void link(final JPMSInRepositoryModuleLayerAccessor moduleLayer, final VestigeClassLoader<AttachedVestigeClassLoader> classLoader) {
        classLoader.getData(this).setModuleLayer(moduleLayer);
    }

    public VestigeReaper getVestigeReaper() {
        return vestigeReaper;
    }

    public JPMSModuleLayerRepository getModuleLayerRepository() {
        return moduleLayerRepository;
    }

    public List<AttachedVestigeClassLoader> getAttached() {
        return attached;
    }

    public Map<Serializable, WeakReference<AttachedVestigeClassLoader>> getMap() {
        return map;
    }

    public void close() {
        for (AttachedVestigeClassLoader attachedVestigeClassLoader : attached) {
            VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
            vestigeClassLoader.setData(this, null);
            vestigeClassLoader.setDataProtector(this, null);

        }
        for (List<WeakReference<AttachedVestigeClassLoader>> attachedVestigeClassLoaderList : attachedClassLoaders) {
            if (attachedVestigeClassLoaderList != null) {
                for (WeakReference<AttachedVestigeClassLoader> reference : attachedVestigeClassLoaderList) {
                    AttachedVestigeClassLoader attachedVestigeClassLoader = reference.get();
                    if (attachedVestigeClassLoader != null) {
                        VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
                        vestigeClassLoader.setData(this, null);
                        vestigeClassLoader.setDataProtector(this, null);
                    }
                }
            }
        }
        for (WeakReference<AttachedVestigeClassLoader> reference : unattached) {
            AttachedVestigeClassLoader attachedVestigeClassLoader = reference.get();
            if (attachedVestigeClassLoader != null) {
                VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
                vestigeClassLoader.setData(this, null);
                vestigeClassLoader.setDataProtector(this, null);

            }
        }
        for (WeakReference<AttachedVestigeClassLoader> reference : map.values()) {
            AttachedVestigeClassLoader attachedVestigeClassLoader = reference.get();
            if (attachedVestigeClassLoader != null) {
                VestigeClassLoader<AttachedVestigeClassLoader> vestigeClassLoader = attachedVestigeClassLoader.getVestigeClassLoader();
                vestigeClassLoader.setData(this, null);
                vestigeClassLoader.setDataProtector(this, null);

            }
        }
        attached.clear();
        attachedClassLoaders.clear();
        unattached.clear();
        map.clear();
    }

    public static void add(final Set<AttachedVestigeClassLoader> set, final AttachedVestigeClassLoader attachedVestigeClassLoader) {
        set.add(attachedVestigeClassLoader);
        for (AttachedVestigeClassLoader depAttachedVestigeClassLoader : attachedVestigeClassLoader.getDependencies()) {
            add(set, depAttachedVestigeClassLoader);
        }
    }

    @Override
    public void discardUnattached() {
        Set<AttachedVestigeClassLoader> set = new HashSet<AttachedVestigeClassLoader>();
        for (AttachedVestigeClassLoader attach : attached) {
            if (attach != null) {
                add(set, attach);
            }
        }

        Iterator<WeakReference<AttachedVestigeClassLoader>> iterator = map.values().iterator();
        while (iterator.hasNext()) {
            WeakReference<AttachedVestigeClassLoader> next = iterator.next();
            AttachedVestigeClassLoader attachedVestigeClassLoader = next.get();
            if (attachedVestigeClassLoader != null && !set.contains(attachedVestigeClassLoader)) {
                iterator.remove();
            }
        }

        // also clear attachment scoped classloaders
        unattached.clear();
    }

}
