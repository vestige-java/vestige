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

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Policy;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.core.StackedHandler;
import fr.gaellalire.vestige.core.StackedHandlerUtils;
import fr.gaellalire.vestige.core.logger.VestigeLoggerFactory;
import fr.gaellalire.vestige.system.interceptor.ProviderListThreadLocal;
import fr.gaellalire.vestige.system.interceptor.RuntimeUtilScheduledThreadPoolExecutor;
import fr.gaellalire.vestige.system.interceptor.TCPTransportConnectionThreadPool;
import fr.gaellalire.vestige.system.interceptor.TCPTransportLocalEndpoints;
import fr.gaellalire.vestige.system.interceptor.VestigeArrayList;
import fr.gaellalire.vestige.system.interceptor.VestigeCopyOnWriteArrayList;
import fr.gaellalire.vestige.system.interceptor.VestigeDriverVector;
import fr.gaellalire.vestige.system.interceptor.VestigeHashMap;
import fr.gaellalire.vestige.system.interceptor.VestigeInputStream;
import fr.gaellalire.vestige.system.interceptor.VestigePolicy;
import fr.gaellalire.vestige.system.interceptor.VestigePrintStream;
import fr.gaellalire.vestige.system.interceptor.VestigeProperties;
import fr.gaellalire.vestige.system.interceptor.VestigeProxySelector;
import fr.gaellalire.vestige.system.interceptor.VestigeSecurityManager;
import fr.gaellalire.vestige.system.interceptor.VestigeURLConnectionContentHandlerFactory;
import fr.gaellalire.vestige.system.interceptor.VestigeURLConnectionHandlersHashTable;
import fr.gaellalire.vestige.system.interceptor.VestigeURLHandlersHashTable;
import fr.gaellalire.vestige.system.interceptor.VestigeURLStreamHandlerFactory;
import fr.gaellalire.vestige.system.logger.SLF4JLoggerFactoryAdapter;
import fr.gaellalire.vestige.system.logger.SLF4JPrintStream;
import fr.gaellalire.vestige.system.logger.SLF4JUncaughtExceptionHandler;
import fr.gaellalire.vestige.system.logger.SecureSLF4JLoggerFactoryAdapter;

/**
 * @author Gael Lalire
 */
public class JVMVestigeSystemActionExecutor implements VestigeSystemActionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JVMVestigeSystemActionExecutor.class);

    private boolean securityEnabled;

    public JVMVestigeSystemActionExecutor(final boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public void execute(final VestigeSystemAction vestigeSystemAction) {
        final VestigeSystemHolder vestigeSystemHolder = new VestigeSystemHolder();
        final DefaultVestigeSystem vestigeSystem = new DefaultVestigeSystem(vestigeSystemHolder, "rootVestigeSystem");
        vestigeSystemHolder.setFallbackVestigeSystem(vestigeSystem);
        vestigeSystemHolder.setFallbackVestigeSystemCache(new DefaultVestigeSystemCache(vestigeSystem, vestigeSystemHolder, null));
        vestigeSystemHolder.setHandlerVestigeSystem(vestigeSystem);

        VestigePolicy vestigePolicy = null;
        if (securityEnabled) {
            synchronized (Policy.class) {
                vestigePolicy = new VestigePolicy(Policy.getPolicy()) {
                    @Override
                    public Policy getCurrentPolicy() {
                        return vestigeSystemHolder.getVestigeSystem().getCurrentPolicy();
                    }
                };
                // ignore previous policy
                vestigeSystem.setPolicy(null);
                Policy.setPolicy(vestigePolicy);
            }
        }

        // VestigeCacheObjectReaper vestigeCacheObjectReaper = new VestigeCacheObjectReaper();

        VestigeSecurityManager vestigeSecurityManager = null;
        VestigeProperties vestigeProperties;
        SLF4JPrintStream pout;
        SLF4JPrintStream perr;
        VestigePrintStream out;
        VestigePrintStream err;
        VestigeInputStream in;
        // avoid direct log
        synchronized (System.class) {
            pout = new SLF4JPrintStream(vestigeSystem, true, System.out);
            out = new VestigePrintStream(pout) {

                @Override
                public PrintStream getPrintStream() {
                    return vestigeSystemHolder.getVestigeSystem().getOut();
                }
            };
            vestigeSystem.setOut(out.getNextHandler());
            System.setOut(out);

            perr = new SLF4JPrintStream(vestigeSystem, false, System.err);
            err = new VestigePrintStream(perr) {

                @Override
                public PrintStream getPrintStream() {
                    return vestigeSystemHolder.getVestigeSystem().getErr();
                }
            };
            vestigeSystem.setErr(err.getNextHandler());
            System.setErr(err);

            in = new VestigeInputStream(System.in) {

                @Override
                public InputStream getInputStream() {
                    return vestigeSystemHolder.getVestigeSystem().getIn();
                }
            };
            vestigeSystem.setIn(in.getNextHandler());
            System.setIn(in);

            vestigeProperties = new VestigeProperties(System.getProperties()) {
                private static final long serialVersionUID = 5951701845063821073L;

                @Override
                public Properties getProperties() {
                    return vestigeSystemHolder.getVestigeSystem().getProperties();
                }
            };
            vestigeSystem.setProperties(vestigeProperties.getNextHandler());
            System.setProperties(vestigeProperties);

            if (securityEnabled) {
                vestigeSecurityManager = new VestigeSecurityManager(System.getSecurityManager(), null) {

                    @Override
                    public SecurityManager getSecurityManager() {
                        return vestigeSystemHolder.getVestigeSystem().getCurrentSecurityManager();
                    }
                };
                vestigeSystem.setSecurityManager(vestigeSecurityManager.getNextHandler());
                System.setSecurityManager(vestigeSecurityManager);
            }
        }

        SLF4JUncaughtExceptionHandler slf4jUncaughtExceptionHandler;
        synchronized (Thread.class) {
            slf4jUncaughtExceptionHandler = new SLF4JUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
            Thread.setDefaultUncaughtExceptionHandler(slf4jUncaughtExceptionHandler);
        }

        VestigeProxySelector proxySelector;
        synchronized (ProxySelector.class) {
            proxySelector = new VestigeProxySelector(ProxySelector.getDefault()) {
                @Override
                public ProxySelector getProxySelector() {
                    return vestigeSystemHolder.getVestigeSystem().getDefaultProxySelector();
                }
            };
            vestigeSystem.setDefaultProxySelector(proxySelector.getNextHandler());
            ProxySelector.setDefault(proxySelector);
        }

        Class<?> levelClass = null;
        Map<String, StackedHandler<?>> levelFields = new HashMap<String, StackedHandler<?>>();
        VestigeArrayList<Object> vestigeArrayList = new VestigeArrayList<Object>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<Object> getArrayList() {
                return vestigeSystemHolder.getVestigeSystem().getKnownLevels();
            }
        };
        levelFields.put("known", vestigeArrayList);
        try {
            levelClass = Class.forName("java.util.logging.Level");
            try {
                installFields(levelClass, null, levelFields);
                vestigeSystem.setKnownLevels(vestigeArrayList.getNextHandler());
            } catch (NoSuchFieldException e) {
                LOGGER.trace("Missing field try another", e);
                // JDK 7
                levelClass = Class.forName("java.util.logging.Level$KnownLevel");
                levelFields.clear();
                VestigeHashMap<String, List<Object>> nameToLevels = new VestigeHashMap<String, List<Object>>(null) {
                    private static final long serialVersionUID = -5445337197591144585L;

                    @Override
                    public HashMap<String, List<Object>> getHashMap() {
                        return vestigeSystemHolder.getVestigeSystem().getNameToLevels();
                    }
                };
                VestigeHashMap<Integer, List<Object>> intToLevels = new VestigeHashMap<Integer, List<Object>>(null) {
                    private static final long serialVersionUID = 8660036849509335913L;

                    @Override
                    public HashMap<Integer, List<Object>> getHashMap() {
                        return vestigeSystemHolder.getVestigeSystem().getIntToLevels();
                    }
                };
                levelFields.put("nameToLevels", nameToLevels);
                levelFields.put("intToLevels", intToLevels);
                installFields(levelClass, null, levelFields);
                vestigeSystem.setNameToLevels(nameToLevels.getNextHandler());
                vestigeSystem.setIntToLevels(intToLevels.getNextHandler());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not intercept Level.known", e);
            levelFields = null;
        }

        // JDK log
        SLF4JLoggerFactoryAdapter slf4jLoggerFactoryAdapter;
        synchronized (VestigeLoggerFactory.class) {
            if (securityEnabled) {
                slf4jLoggerFactoryAdapter = new SecureSLF4JLoggerFactoryAdapter(vestigeSystem);
            } else {
                slf4jLoggerFactoryAdapter = new SLF4JLoggerFactoryAdapter();
            }
            slf4jLoggerFactoryAdapter.setNextHandler(VestigeLoggerFactory.getVestigeLoggerFactory());
            VestigeLoggerFactory.setVestigeLoggerFactory(slf4jLoggerFactoryAdapter);
        }

        Map<String, StackedHandler<?>> securityFields = new HashMap<String, StackedHandler<?>>();
        VestigeProperties vestigeSecurityProperties = new VestigeProperties(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public Properties getProperties() {
                return vestigeSystemHolder.getVestigeSystem().getSecurityProperties();
            }
        };
        securityFields.put("props", vestigeSecurityProperties);
        try {
            installFields(Security.class, null, securityFields);
            vestigeSystem.setSecurityProperties(vestigeSecurityProperties.getNextHandler());
        } catch (Exception e) {
            LOGGER.warn("Could not intercept Security.properties", e);
            securityFields = null;
        }

        // sun.security.jca.Providers
        Map<String, StackedHandler<?>> securityProvidersFields = new HashMap<String, StackedHandler<?>>();
        ProviderListThreadLocal providerListThreadLocal = new ProviderListThreadLocal(null) {

            @Override
            public Object getSecurityProviderList() {
                return vestigeSystemHolder.getVestigeSystem().getSecurityProviderList();
            }

            @Override
            public void setSecurityProviderList(final Object object) {
                vestigeSystemHolder.getVestigeSystem().setSecurityProviderList(object);
            }

        };
        securityProvidersFields.put("threadLists", providerListThreadLocal);
        Class<?> securityProviders = null;
        Field threadListsUsedField = null;
        try {
            securityProviders = Class.forName("sun.security.jca.Providers");
            vestigeSystem.setSecurityProviderList(getFieldValue(securityProviders.getDeclaredField("providerList"), null));
            threadListsUsedField = securityProviders.getDeclaredField("threadListsUsed");
            installFields(securityProviders, null, securityProvidersFields);
            synchronized (securityProviders) {
                // always use thread local providerList
                threadListsUsedField.setAccessible(true);
                threadListsUsedField.setInt(null, threadListsUsedField.getInt(null) + 1);
                threadListsUsedField.setAccessible(false);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not intercept sun.security.jca.Providers", e);
            securityProvidersFields = null;
        }

        Map<String, StackedHandler<?>> urlFields = new HashMap<String, StackedHandler<?>>();
        VestigeURLStreamHandlerFactory vestigeURLStreamHandlerFactory = new VestigeURLStreamHandlerFactory();
        urlFields.put("factory", vestigeURLStreamHandlerFactory);

        VestigeSystemJarURLStreamHandler jarUrlStreamHandler = new VestigeSystemJarURLStreamHandler(vestigeSystemHolder);
        vestigeSystemHolder.setVestigeApplicationJarURLStreamHandler(jarUrlStreamHandler);

        VestigeURLHandlersHashTable vestigeURLHandlersHashTable = new VestigeURLHandlersHashTable(vestigeSystemHolder, jarUrlStreamHandler);
        urlFields.put("handlers", vestigeURLHandlersHashTable);
        try {
            installFields(URL.class, null, urlFields);
            URLStreamHandlerFactory nextHandler = vestigeURLStreamHandlerFactory.getNextHandler();
            // default protocol differ (jar files) so we remove the cache
            vestigeSystem.setURLStreamHandlerByProtocol(new Hashtable<String, URLStreamHandler>());
            if (nextHandler != null) {
                vestigeSystem.setURLStreamHandlerFactory(nextHandler);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not intercept URL.setURLStreamHandlerFactory", e);
            urlFields = null;
        }

        Map<String, StackedHandler<?>> urlConnectionFields = new HashMap<String, StackedHandler<?>>();
        VestigeURLConnectionContentHandlerFactory vestigeURLConnectionContentHandlerFactory = new VestigeURLConnectionContentHandlerFactory();
        urlConnectionFields.put("factory", vestigeURLConnectionContentHandlerFactory);
        VestigeURLConnectionHandlersHashTable vestigeURLConnectionHandlersHashTable = new VestigeURLConnectionHandlersHashTable(vestigeSystemHolder);
        urlConnectionFields.put("handlers", vestigeURLConnectionHandlersHashTable);
        try {
            installFields(URLConnection.class, null, urlConnectionFields);
            vestigeSystem.setURLConnectionContentHandlerFactory(vestigeURLConnectionContentHandlerFactory.getNextHandler());
            vestigeSystem.setURLConnectionContentHandlerByMime(vestigeURLConnectionHandlersHashTable.getNextHandler());
        } catch (Exception e) {
            LOGGER.warn("Could not intercept URLConnection.setContentHandlerFactory", e);
            urlFields = null;
        }

        Class<?> driverManagerClass = null;
        Map<String, StackedHandler<?>> driverManagerFields = new HashMap<String, StackedHandler<?>>();
        VestigeDriverVector writeDrivers = new VestigeDriverVector(vestigeSystemHolder);
        VestigeDriverVector readDrivers = new VestigeDriverVector(vestigeSystemHolder, null, new WeakHashMap<DefaultVestigeSystem, Object>());
        writeDrivers.setReadDrivers(readDrivers);
        driverManagerFields.put("writeDrivers", writeDrivers);
        driverManagerFields.put("readDrivers", readDrivers);
        try {
            driverManagerClass = Class.forName("java.sql.DriverManager");
            try {
                installFields(driverManagerClass, null, driverManagerFields);
                vestigeSystem.setWriteDrivers(writeDrivers.getNextHandler());
                vestigeSystem.setReadDrivers(new WeakHashMap<Object, Vector<Object>>());
                vestigeSystem.getReadDrivers().put(vestigeSystem, readDrivers.getNextHandler());
            } catch (NoSuchFieldException e) {
                LOGGER.trace("Missing field try another", e);
                writeDrivers = null;
                readDrivers = null;
                // JDK 7
                driverManagerFields.clear();
                VestigeCopyOnWriteArrayList vestigeDriversCopyOnWriteArrayList = new VestigeCopyOnWriteArrayList(null) {
                    private static final long serialVersionUID = -8739537725123134572L;

                    @Override
                    public CopyOnWriteArrayList<Object> getCopyOnWriteArrayList() {
                        return vestigeSystemHolder.getVestigeSystem().getRegisteredDrivers();
                    }
                };
                driverManagerFields.put("registeredDrivers", vestigeDriversCopyOnWriteArrayList);
                installFields(driverManagerClass, null, driverManagerFields);
                vestigeSystem.setRegisteredDrivers(vestigeDriversCopyOnWriteArrayList.getNextHandler());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not intercept DriverManager.registerDriver", e);
            driverManagerFields = null;
        }

        Map<String, StackedHandler<?>> tcpTransportFields = new HashMap<String, StackedHandler<?>>();
        TCPTransportConnectionThreadPool vestigeExecutorService = new TCPTransportConnectionThreadPool(vestigeSystemHolder);
        tcpTransportFields.put("connectionThreadPool", vestigeExecutorService);

        Class<?> tcpTransportClass = null;
        try {
            tcpTransportClass = Class.forName("sun.rmi.transport.tcp.TCPTransport");

            installFields(tcpTransportClass, null, tcpTransportFields);
        } catch (Exception e) {
            LOGGER.warn("Could not intercept TCPTransport.connectionThreadPool", e);
            tcpTransportFields = null;
        }

        Map<String, StackedHandler<?>> tcpEndpointFields = new HashMap<String, StackedHandler<?>>();
        Class<?> tcpEndpointClass = null;
        try {
            tcpEndpointClass = Class.forName("sun.rmi.transport.tcp.TCPEndpoint");
            Class.forName("sun.rmi.transport.Transport");
            Class.forName("sun.rmi.transport.Endpoint");

            tcpEndpointFields.put("localEndpoints", new TCPTransportLocalEndpoints(vestigeSystemHolder));

            installFields(tcpEndpointClass, null, tcpEndpointFields);
        } catch (Exception e) {
            LOGGER.warn("Could not intercept TCPEndpoint.localEndpoints", e);
            tcpEndpointFields = null;
        }

        Map<String, StackedHandler<?>> runtimeUtilFields = new HashMap<String, StackedHandler<?>>();
        RuntimeUtilScheduledThreadPoolExecutor runtimeUtilScheduledThreadPoolExecutor = new RuntimeUtilScheduledThreadPoolExecutor(vestigeSystemHolder);
        runtimeUtilFields.put("scheduler", runtimeUtilScheduledThreadPoolExecutor);

        Class<?> runtimeUtilClass = null;
        Object runtimeUtilInstance = null;
        try {
            Class<?> newThreadActionClass = Class.forName("sun.rmi.runtime.NewThreadAction");
            ThreadGroup threadGroup = (ThreadGroup) getFieldValue(newThreadActionClass.getDeclaredField("systemThreadGroup"), null);
            RuntimeUtilScheduledThreadPoolExecutor.setSystemThreadGroup(threadGroup);

            runtimeUtilClass = Class.forName("sun.rmi.runtime.RuntimeUtil");
            runtimeUtilInstance = getFieldValue(runtimeUtilClass.getDeclaredField("instance"), null);

            installFields(runtimeUtilClass, runtimeUtilInstance, runtimeUtilFields);
        } catch (Exception e) {
            LOGGER.warn("Could not intercept RuntimeUtil.instance.scheduler", e);
            runtimeUtilFields = null;
        }

        // RenewCleanThread will stop after sun.rmi.dgc.cleanInterval (default to 3 minutes)
        // TODO interrupt ?, interrupt will kill only RenewCleanThread which has nothing to do
        // TODO interrupt all Thread which are childen of group NewThreadAction.systemThreadGroup and with name containing RenewClean

        // vestigeCacheObjectReaper.start();
        try {
            vestigeSystemAction.vestigeSystemRun(vestigeSystem);
        } finally {
            // vestigeCacheObjectReaper.interrupt();

            if (runtimeUtilFields != null) {
                try {
                    uninstallFields(runtimeUtilClass, runtimeUtilInstance, runtimeUtilFields);
                } catch (Exception e) {
                    LOGGER.error("Could not release RuntimeUtil.instance.scheduler interception", e);
                }
            }

            if (tcpEndpointFields != null) {
                try {
                    uninstallFields(tcpEndpointClass, null, tcpEndpointFields);
                } catch (Exception e) {
                    LOGGER.error("Could not release TCPEndpoint.localEndpoints interception", e);
                }
            }

            if (tcpTransportFields != null) {
                try {
                    uninstallFields(tcpTransportClass, null, tcpTransportFields);
                } catch (Exception e) {
                    LOGGER.error("Could not release TCPTransport.connectionThreadPool interception", e);
                }
            }

            if (driverManagerFields != null) {
                synchronized (driverManagerClass) {
                    if (writeDrivers != null) {
                        readDrivers = writeDrivers.getReadDrivers();
                        driverManagerFields.put("readDrivers", readDrivers);
                    }
                    try {
                        uninstallFields(driverManagerClass, null, driverManagerFields);
                    } catch (Exception e) {
                        LOGGER.error("Could not release DriverManager.registerDriver interception", e);
                    }
                }
            }

            if (urlConnectionFields != null) {
                try {
                    uninstallFields(URLConnection.class, null, urlConnectionFields);
                } catch (Exception e) {
                    LOGGER.error("Could not release URLConnection.setContentHandlerFactory interception", e);
                }
            }

            if (urlFields != null) {
                try {
                    uninstallFields(URL.class, null, urlFields);
                } catch (Exception e) {
                    LOGGER.error("Could not release URL.setURLStreamHandlerFactory interception", e);
                }
            }

            if (securityProvidersFields != null) {
                try {
                    synchronized (securityProviders) {
                        uninstallFields(securityProviders, null, securityProvidersFields);
                        // always use thread local providerList
                        threadListsUsedField.setAccessible(true);
                        threadListsUsedField.setInt(null, threadListsUsedField.getInt(null) - 1);
                        threadListsUsedField.setAccessible(false);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not release sun.security.jca.Providers interception", e);
                }
            }

            if (securityFields != null) {
                try {
                    uninstallFields(Security.class, null, securityFields);
                } catch (Exception e) {
                    LOGGER.warn("Could not release Security.properties interception", e);
                }
            }

            if (slf4jLoggerFactoryAdapter != null) {
                synchronized (VestigeLoggerFactory.class) {
                    VestigeLoggerFactory
                            .setVestigeLoggerFactory(StackedHandlerUtils.uninstallStackedHandler(slf4jLoggerFactoryAdapter, VestigeLoggerFactory.getVestigeLoggerFactory()));
                }
            }

            if (levelFields != null) {
                try {
                    uninstallFields(levelClass, null, levelFields);
                } catch (Exception e) {
                    LOGGER.warn("Could not release Level.known interception", e);
                }
            }

            synchronized (ProxySelector.class) {
                ProxySelector.setDefault(StackedHandlerUtils.uninstallStackedHandler(proxySelector, ProxySelector.getDefault()));
            }

            synchronized (Thread.class) {
                Thread.setDefaultUncaughtExceptionHandler(StackedHandlerUtils.uninstallStackedHandler(slf4jUncaughtExceptionHandler, Thread.getDefaultUncaughtExceptionHandler()));
            }

            synchronized (System.class) {
                if (securityEnabled) {
                    System.setSecurityManager(StackedHandlerUtils.uninstallStackedHandler(vestigeSecurityManager, System.getSecurityManager()));
                }
                System.setProperties(StackedHandlerUtils.uninstallStackedHandler(vestigeProperties, System.getProperties()));
                System.setOut(StackedHandlerUtils.uninstallStackedHandler(out, System.out));
                System.setErr(StackedHandlerUtils.uninstallStackedHandler(err, System.err));
                System.setIn(StackedHandlerUtils.uninstallStackedHandler(in, System.in));
                System.setOut(StackedHandlerUtils.uninstallStackedHandler(pout, System.out));
                System.setErr(StackedHandlerUtils.uninstallStackedHandler(perr, System.err));
            }

            if (securityEnabled) {
                synchronized (Policy.class) {
                    Policy.setPolicy(StackedHandlerUtils.uninstallStackedHandler(vestigePolicy, Policy.getPolicy()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void uninstallFields(final Class<?> clazz, final Object object, final Map<String, StackedHandler<?>> valueByFieldName) throws Exception {
        synchronized (clazz) {
            for (final Entry<String, StackedHandler<?>> entry : valueByFieldName.entrySet()) {
                final Field declaredField = clazz.getDeclaredField(entry.getKey());
                setFieldValue(declaredField, object, StackedHandlerUtils.uninstallStackedHandler((StackedHandler<Object>) entry.getValue(), getFieldValue(declaredField, object)));
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void installFields(final Class<?> clazz, final Object object, final Map<String, StackedHandler<?>> valueByFieldName) throws Exception {
        synchronized (clazz) {
            for (final Entry<String, StackedHandler<?>> entry : valueByFieldName.entrySet()) {
                Field declaredField = clazz.getDeclaredField(entry.getKey());
                StackedHandler value = entry.getValue();
                Object previousValue = getFieldValue(declaredField, object);
                setFieldValue(declaredField, object, value);
                value.setNextHandler(previousValue);
            }
        }
    }

    public static Object getFieldValue(final Field field, final Object object) throws Exception {
        Callable<Object> callable = new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                    try {
                        return field.get(object);
                    } finally {
                        field.setAccessible(false);
                    }
                } else {
                    return field.get(object);
                }
            }
        };
        if (Modifier.isFinal(field.getModifiers())) {
            // unset for get too (prevent caching)
            return FinalUnsetter.unsetFinalField(field, callable);
        } else {
            return callable.call();
        }
    }

    public static void setFieldValue(final Field field, final Object object, final Object value) throws Exception {
        Callable<Void> callable = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                    try {
                        field.set(object, value);
                    } finally {
                        field.setAccessible(false);
                    }
                } else {
                    field.set(object, value);
                }
                return null;
            }
        };
        if (Modifier.isFinal(field.getModifiers())) {
            FinalUnsetter.unsetFinalField(field, callable);
        } else {
            callable.call();
        }
    }

}
