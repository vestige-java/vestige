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

package fr.gaellalire.vestige.system;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.spi.system.VestigeSystemCache;

/**
 * @author Gael Lalire
 */
public class VestigeSystemJarURLStreamHandler extends URLStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeSystemJarURLStreamHandler.class);

    private Map<URL, File> cachedJarFileKeyWeakReferenceByURL = new HashMap<URL, File>();

    private Map<File, WeakReference<CachedJarFile>> cachedJarFileKeyWeakReferenceByFile = new HashMap<File, WeakReference<CachedJarFile>>();

    private ThreadLocal<DefaultVestigeSystemCache> vestigeSystemCacheThreadLocal = new InheritableThreadLocal<DefaultVestigeSystemCache>();

    private File temp;

    private boolean clearingCache;

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new VestigeSystemJarURLConnection(this, url);
    }

    // sun.net.www.protocol.jar.URLJarFile
    private static boolean isLocalFileURL(final URL url) {
        if (url.getProtocol().equalsIgnoreCase("file")) {
            String str = url.getHost();
            if (str == null || str.equals("") || str.equals("~") || str.equalsIgnoreCase("localhost")) {
                return true;
            }
        }
        return false;
    }

    public CachedJarFile connect(final VestigeSystemJarURLConnection vestigeSystemJarURLConnection, final URL jarFileUrl, final boolean useCachesAsked) throws IOException {
        WeakReference<CachedJarFile> weakReference = null;
        CachedJarFile cachedJarFile = null;
        DefaultVestigeSystemCache vestigeSystemCache = null;
        boolean useCaches = false;
        File file;
        // we always try to use cache
        // because new URL("jar:file:/f.jar").connect() will prevent f.jar to be suppress until a GC finalize the CachedJarFile
        // we need that when the application exit GC or not, its data can be suppressed
        if (true  /* useCachesAsked */) {
            vestigeSystemCache = vestigeSystemCacheThreadLocal.get();
            if (vestigeSystemCache == null) {
                LOGGER.warn("Cannot use cache for {} when no system cache is set", jarFileUrl);
            } else {
                synchronized (cachedJarFileKeyWeakReferenceByURL) {
                    file = cachedJarFileKeyWeakReferenceByURL.get(jarFileUrl);
                    if (file != null) {
                        weakReference = cachedJarFileKeyWeakReferenceByFile.get(file);
                    }
                }
                if (weakReference != null) {
                    cachedJarFile = weakReference.get();
                }
                useCaches = true;
            }
        }
        if (cachedJarFile == null) {
            boolean temporary = false;
            if (isLocalFileURL(jarFileUrl)) {
                try {
                    file = new File(jarFileUrl.toURI());
                } catch (URISyntaxException e) {
                    throw new IOException("File URI syntax", e);
                }
            } else {
                // copy in temp
                InputStream inputStream = jarFileUrl.openConnection().getInputStream();
                file = File.createTempFile("jar_cache", ".jar", temp);
                FileOutputStream localFileOutputStream = new FileOutputStream(file);
                int i = 0;
                byte[] arrayOfByte = new byte[2048];
                while ((i = inputStream.read(arrayOfByte)) != -1) {
                    localFileOutputStream.write(arrayOfByte, 0, i);
                }
                localFileOutputStream.close();
                temporary = true;
            }

            if (useCaches) {
                synchronized (cachedJarFileKeyWeakReferenceByURL) {
                    weakReference = cachedJarFileKeyWeakReferenceByFile.get(file);
                    if (weakReference != null) {
                        cachedJarFile = weakReference.get();
                    }
                    if (cachedJarFile == null) {
                        // new cachedJarFile will be watch by reaper, and put in cache
                        cachedJarFile = new CachedJarFile(file, temporary, this);
                        weakReference = new WeakReference<CachedJarFile>(cachedJarFile);
                        cachedJarFileKeyWeakReferenceByFile.put(file, weakReference);
                        cachedJarFileKeyWeakReferenceByURL.put(jarFileUrl, file);
                    } else {
                        cachedJarFileKeyWeakReferenceByURL.put(jarFileUrl, file);
                    }
                }
            } else {
                cachedJarFile = new CachedJarFile(file, temporary, null);
            }
        }
        if (useCaches && vestigeSystemCache.getWeakReferences().add(weakReference)) {
            cachedJarFile.addVestigeSystemUser();
        }
        return cachedJarFile;
    }

    public void removeFromCaches(final File file) {
        synchronized (cachedJarFileKeyWeakReferenceByURL) {
            if (!clearingCache) {
                cachedJarFileKeyWeakReferenceByFile.remove(file);
            }
            Iterator<? extends Entry<?, File>> iterator = cachedJarFileKeyWeakReferenceByURL.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<?, File> next = iterator.next();
                if (next.getValue().equals(file)) {
                    iterator.remove();
                }
            }
        }
    }

    public void clearCache(final DefaultVestigeSystemCache popedVestigeSystemCache) {
        DefaultVestigeSystemCache vestigeSystemCache = vestigeSystemCacheThreadLocal.get();
        while (vestigeSystemCache != null && vestigeSystemCache != popedVestigeSystemCache) {
            vestigeSystemCache = vestigeSystemCache.getParent();
        }
        if (vestigeSystemCache == null) {
            // can not clear cache
            return;
        }
        vestigeSystemCache = vestigeSystemCacheThreadLocal.get();
        boolean last = false;
        while (!last) {
            if (vestigeSystemCache == popedVestigeSystemCache) {
                last = true;
            }
            for (WeakReference<CachedJarFile> weakReference : vestigeSystemCache.getWeakReferences()) {
                CachedJarFile cachedJarFile = weakReference.get();
                // if key is GC, then all VestigeSystemJarURLConnection which use this key are also GC.
                // the cachedJarFileByCachedJarFileKey loop will detect it, and close it
                if (cachedJarFile != null) {
                    // the key is not GC but it has one less user because vestigeSystemCache is popped
                    cachedJarFile.removeVestigeSystemUser();
                }
            }
            vestigeSystemCache = vestigeSystemCache.getParent();
        }
        vestigeSystemCacheThreadLocal.set(popedVestigeSystemCache.getParent());

        synchronized (cachedJarFileKeyWeakReferenceByURL) {
            clearingCache = true;
            try {
                Iterator<? extends Entry<?, WeakReference<CachedJarFile>>> iterator = cachedJarFileKeyWeakReferenceByFile.entrySet().iterator();
                while (iterator.hasNext()) {
                    CachedJarFile cachedJarFile = iterator.next().getValue().get();
                    if (cachedJarFile == null) {
                        iterator.remove();
                    } else if (cachedJarFile.getVestigeSystemUserCount() == 0) {
                        cachedJarFile.close();
                        iterator.remove();
                    }
                }
            } finally {
                clearingCache = false;
            }
        }
    }

    public VestigeSystemCache pushVestigeSystemCache() {
        DefaultVestigeSystemCache vestigeSystemCache = new DefaultVestigeSystemCache(this, vestigeSystemCacheThreadLocal.get());
        vestigeSystemCacheThreadLocal.set(vestigeSystemCache);
        return vestigeSystemCache;
    }

}
