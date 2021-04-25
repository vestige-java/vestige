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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.spi.system.VestigeSystemCache;
import fr.gaellalire.vestige.system.interceptor.JustInTimeReference;
import fr.gaellalire.vestige.system.interceptor.TCPTransportConnectionThreadPool;

/**
 * @author Gael Lalire
 */
public class DefaultVestigeSystemCache implements VestigeSystemCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVestigeSystemCache.class);

    private DefaultVestigeSystemCache parent;

    private Map<Object, List<Object>> tcpEndpointLocalEndpoints;

    private List<WeakReference<CachedJarFile>> weakReferences;

    private JustInTimeReference<ExecutorService> tcpTransportConnectionThreadPoolReference;

    private VestigeSystemHolder vestigeSystemHolder;

    public DefaultVestigeSystemCache(final VestigeSystemHolder vestigeSystemHolder, final DefaultVestigeSystemCache parent) {
        this.vestigeSystemHolder = vestigeSystemHolder;
        this.parent = parent;
        this.weakReferences = new ArrayList<WeakReference<CachedJarFile>>();
        tcpEndpointLocalEndpoints = new HashMap<Object, List<Object>>();
        tcpTransportConnectionThreadPoolReference = new JustInTimeReference<ExecutorService>() {

            @Override
            protected ExecutorService create() {
                return TCPTransportConnectionThreadPool.createConnectionThreadPool();
            }
        };
    }

    public Map<Object, List<Object>> getTcpEndpointLocalEndpoints() {
        return tcpEndpointLocalEndpoints;
    }

    public JustInTimeReference<ExecutorService> getTcpTransportConnectionThreadPoolReference() {
        return tcpTransportConnectionThreadPoolReference;
    }

    @Override
    public void clearCache() {
        vestigeSystemHolder.clearCache(this);
    }

    public DefaultVestigeSystemCache getParent() {
        return parent;
    }

    public List<WeakReference<CachedJarFile>> getWeakReferences() {
        return weakReferences;
    }

    public void doClearCache() {
        for (WeakReference<CachedJarFile> weakReference : weakReferences) {
            CachedJarFile cachedJarFile = weakReference.get();
            // if key is GC, then all VestigeSystemJarURLConnection which use this key are also GC.
            // the cachedJarFileByCachedJarFileKey loop will detect it, and close it
            if (cachedJarFile != null) {
                // the key is not GC but it has one less user because vestigeSystemCache is popped
                cachedJarFile.removeVestigeSystemUser();
            }
        }

        try {
            if (tcpEndpointLocalEndpoints.size() != 0) {
                Class<?> transportClass = Class.forName("sun.rmi.transport.Transport");
                Class<?> tcpTransportClass = Class.forName("sun.rmi.transport.tcp.TCPTransport");
                Class<?> endpointClass = Class.forName("sun.rmi.transport.Endpoint");

                for (List<Object> value : tcpEndpointLocalEndpoints.values()) {
                    for (Object object : value) {
                        Object transport = endpointClass.getMethod("getInboundTransport").invoke(object);
                        if (transport != null) {
                            if (tcpTransportClass.isInstance(transport)) {
                                Field declaredField = tcpTransportClass.getDeclaredField("server");
                                declaredField.setAccessible(true);
                                try {
                                    ServerSocket serverSocket = (ServerSocket) declaredField.get(transport);
                                    if (serverSocket != null) {
                                        serverSocket.close();
                                    }
                                } finally {
                                    declaredField.setAccessible(false);
                                }

                            }
                            transportClass.getMethod("free", endpointClass).invoke(transport, object);
                        }
                    }
                }
                // refQueue is not cleaned without a call to lookupLoader
                Class.forName("sun.rmi.server.LoaderHandler").getMethod("getClassLoader", String.class).invoke(null, new Object[] {null});
                // inheritableThreadLocals of java.lang.Thread [JNI Global, Thread, Stack Local ← t, this, thisThread, wt] "RMI Scheduler(0)" daemon tid=64 [TIMED_WAITING] 784 376

                // inheritableThreadLocals of java.lang.Thread [JNI Global, Thread, Stack Local ← this] "RMI RenewClean-[192.168.2.101:59238]" daemon tid=65 [TIMED_WAITING] 2704
                // 376
                // sun.rmi.transport.DGCClient

            }
        } catch (Exception e) {
            LOGGER.debug("Unable to clean RMI cache", e);
        }

        ExecutorService noCreate = tcpTransportConnectionThreadPoolReference.getNoCreate();
        if (noCreate != null) {
            noCreate.shutdownNow();
        }
    }

}
