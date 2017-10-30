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

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class cannot be used with jar cache.
 * Indeed it is possible to cast a connection into a {@link java.net.JarURLConnection} and then get its {@link java.util.jar.JarFile}.
 * So when connection is GC reaper close JarFile however it is unexpected for the client which still retains the {@link java.util.jar.JarFile} instance.
 * We could have each public {@link java.util.jar.JarFile} method override, however the constructor is doing stuff we can't prevent unless using sun serialization API.
 * So we let {@link java.util.jar.JarFile} finalize method doing its jobs.
 *
 * @author Gael Lalire
 */
public class VestigeCacheObjectReaper extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeCacheObjectReaper.class);

    private ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();

    private Map<Reference<?>, Closeable> closeableByReference = new HashMap<Reference<?>, Closeable>();

    public VestigeCacheObjectReaper() {
        super("cache-object-reaper");
    }

    public ReferenceQueue<Object> getReferenceQueue() {
        return referenceQueue;
    }

    /**
     * The reference must have {@link #getReferenceQueue()} as reference queue.
     */
    public void closeCloseableWhenReferencedObjectIsReclaimed(final Reference<?> reference, final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        int size = 0;
        synchronized (closeableByReference) {
            closeableByReference.put(reference, closeable);
            if (LOGGER.isInfoEnabled()) {
                size = closeableByReference.size();
            }
        }
        LOGGER.info("Monitoring {} objects", size);
    }

    public PhantomReference<Object> closeCloseableWhenObjectIsReclaimed(final Object object, final Closeable closeable) {
        if (closeable == null) {
            return null;
        }
        PhantomReference<Object> phantomReference = new PhantomReference<Object>(object, referenceQueue);
        int size = 0;
        synchronized (closeableByReference) {
            closeableByReference.put(phantomReference, closeable);
            if (LOGGER.isInfoEnabled()) {
                size = closeableByReference.size();
            }
        }
        LOGGER.info("Monitoring {} objects", size);
        return phantomReference;
    }

    public void stopReaping(final WeakReference<?> weakReference) {
        if (weakReference == null) {
            return;
        }
        int size = 0;
        synchronized (closeableByReference) {
            closeableByReference.remove(weakReference);
            if (LOGGER.isInfoEnabled()) {
                size = closeableByReference.size();
            }
        }
        LOGGER.info("Monitoring {} objects", size);
    }

    @Override
    public void run() {
        try {
            LOGGER.info("VestigeCacheObjectReaper started");
            Reference<? extends Object> remove = referenceQueue.remove();
            while (true) {
                Closeable closeable;
                int size = 0;
                synchronized (closeableByReference) {
                    closeable = closeableByReference.remove(remove);
                    if (LOGGER.isInfoEnabled()) {
                        size = closeableByReference.size();
                    }
                }
                if (closeable != null) {
                    LOGGER.info("Monitoring {} objects", size);
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        LOGGER.error("Cannot close", e);
                    }
                }
                remove = referenceQueue.remove();
            }
        } catch (InterruptedException e) {
            LOGGER.trace("InterruptedException", e);
        }
        for (Closeable closeable : closeableByReference.values()) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    LOGGER.error("Cannot close", e);
                }
            }
        }
        LOGGER.info("VestigeCacheObjectReaper stopped");
    }

}
