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

package com.googlecode.vestige.platform.system;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.security.Policy;
import java.security.Security;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.vestige.core.StackedHandler;
import com.googlecode.vestige.core.StackedHandlerUtils;
import com.googlecode.vestige.core.logger.VestigeLoggerFactory;
import com.googlecode.vestige.platform.logger.SLF4JLoggerFactoryAdapter;
import com.googlecode.vestige.platform.logger.SLF4JPrintStream;
import com.googlecode.vestige.platform.logger.SecureSLF4JLoggerFactoryAdapter;
import com.googlecode.vestige.platform.system.interceptor.ProviderListThreadLocal;
import com.googlecode.vestige.platform.system.interceptor.VestigeArrayList;
import com.googlecode.vestige.platform.system.interceptor.VestigeCopyOnWriteArrayList;
import com.googlecode.vestige.platform.system.interceptor.VestigeDriverVector;
import com.googlecode.vestige.platform.system.interceptor.VestigeHashMap;
import com.googlecode.vestige.platform.system.interceptor.VestigeInputStream;
import com.googlecode.vestige.platform.system.interceptor.VestigePolicy;
import com.googlecode.vestige.platform.system.interceptor.VestigePrintStream;
import com.googlecode.vestige.platform.system.interceptor.VestigeProperties;
import com.googlecode.vestige.platform.system.interceptor.VestigeProxySelector;
import com.googlecode.vestige.platform.system.interceptor.VestigeSecurityManager;
import com.googlecode.vestige.platform.system.interceptor.VestigeURLConnectionContentHandlerFactory;
import com.googlecode.vestige.platform.system.interceptor.VestigeURLConnectionHandlersHashTable;
import com.googlecode.vestige.platform.system.interceptor.VestigeURLHandlersHashTable;
import com.googlecode.vestige.platform.system.interceptor.VestigeURLStreamHandlerFactory;

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
        // JDK log
        synchronized (VestigeLoggerFactory.class) {
            SLF4JLoggerFactoryAdapter factory;
            if (securityEnabled) {
                factory = new SecureSLF4JLoggerFactoryAdapter();
            } else {
                factory = new SLF4JLoggerFactoryAdapter();
            }
            factory.setNextHandler(VestigeLoggerFactory.getVestigeLoggerFactory());
            VestigeLoggerFactory.setVestigeLoggerFactory(factory);
        }


        final VestigeSystemHolder vestigeSystemHolder = new VestigeSystemHolder();
        final VestigeSystem vestigeSystem = new VestigeSystem(vestigeSystemHolder);
        vestigeSystemHolder.setFallbackVestigeSystem(vestigeSystem);

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

        VestigeSecurityManager vestigeSecurityManager = null;
        VestigeProperties vestigeProperties;
        VestigePrintStream out;
        VestigePrintStream err;
        VestigeInputStream in;
        // avoid direct log
        synchronized (System.class) {
            out = new VestigePrintStream(new SLF4JPrintStream(true, System.out)) {

                @Override
                public PrintStream getPrintStream() {
                    return vestigeSystemHolder.getVestigeSystem().getOut();
                }
            };
            vestigeSystem.setOut(out.getNextHandler());
            System.setOut(out);

            err = new VestigePrintStream(new SLF4JPrintStream(false, System.err)) {

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

        Class<?> levelClass = Level.class;
        Map<String, StackedHandler<?>> levelFields = new HashMap<String, StackedHandler<?>>();
        VestigeArrayList<Level> vestigeArrayList = new VestigeArrayList<Level>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<Level> getArrayList() {
                return vestigeSystemHolder.getVestigeSystem().getKnownLevels();
            }
        };
        levelFields.put("known", vestigeArrayList);
        try {
            try {
                installFields(levelClass, levelFields);
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
                installFields(levelClass, levelFields);
                vestigeSystem.setNameToLevels(nameToLevels.getNextHandler());
                vestigeSystem.setIntToLevels(intToLevels.getNextHandler());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not intercept Level.known", e);
            levelFields = null;
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
            installFields(Security.class, securityFields);
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
            vestigeSystem.setSecurityProviderList(getStaticFieldValue(securityProviders.getDeclaredField("providerList")));
            threadListsUsedField = securityProviders.getDeclaredField("threadListsUsed");
            installFields(securityProviders, securityProvidersFields);
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
        VestigeURLHandlersHashTable vestigeURLHandlersHashTable = new VestigeURLHandlersHashTable(vestigeSystemHolder);
        urlFields.put("handlers", vestigeURLHandlersHashTable);
        try {
            installFields(URL.class, urlFields);
            vestigeSystem.setURLStreamHandlerFactory(vestigeURLStreamHandlerFactory.getNextHandler());
            vestigeSystem.setURLStreamHandlerByProtocol(vestigeURLHandlersHashTable.getNextHandler());
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
            installFields(URLConnection.class, urlConnectionFields);
            vestigeSystem.setURLConnectionContentHandlerFactory(vestigeURLConnectionContentHandlerFactory.getNextHandler());
            vestigeSystem.setURLConnectionContentHandlerByMime(vestigeURLConnectionHandlersHashTable.getNextHandler());
        } catch (Exception e) {
            LOGGER.warn("Could not intercept URLConnection.setContentHandlerFactory", e);
            urlFields = null;
        }

        Map<String, StackedHandler<?>> driverManagerFields = new HashMap<String, StackedHandler<?>>();
        VestigeDriverVector writeDrivers = new VestigeDriverVector(vestigeSystemHolder);
        VestigeDriverVector readDrivers = new VestigeDriverVector(vestigeSystemHolder, null, new WeakHashMap<VestigeSystem, Object>());
        writeDrivers.setReadDrivers(readDrivers);
        driverManagerFields.put("writeDrivers", writeDrivers);
        driverManagerFields.put("readDrivers", readDrivers);
        try {
            try {
                installFields(DriverManager.class, driverManagerFields);
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
                installFields(DriverManager.class, driverManagerFields);
                vestigeSystem.setRegisteredDrivers(vestigeDriversCopyOnWriteArrayList.getNextHandler());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not intercept DriverManager.registerDriver", e);
            driverManagerFields = null;
        }
        try {
            vestigeSystemAction.vestigeSystemRun(vestigeSystem);
        } finally {
            if (driverManagerFields != null) {
                synchronized (DriverManager.class) {
                    if (writeDrivers != null) {
                        readDrivers = writeDrivers.getReadDrivers();
                        driverManagerFields.put("readDrivers", readDrivers);
                    }
                    try {
                        uninstallFields(DriverManager.class, driverManagerFields);
                    } catch (Exception e) {
                        LOGGER.error("Could not release DriverManager.registerDriver interception", e);
                    }
                }
            }

            if (urlConnectionFields != null) {
                try {
                    uninstallFields(URLConnection.class, urlConnectionFields);
                } catch (Exception e) {
                    LOGGER.error("Could not release URLConnection.setContentHandlerFactory interception", e);
                }
            }

            if (urlFields != null) {
                try {
                    uninstallFields(URL.class, urlFields);
                } catch (Exception e) {
                    LOGGER.error("Could not release URL.setURLStreamHandlerFactory interception", e);
                }
            }

            if (securityProvidersFields != null) {
                try {
                    synchronized (securityProviders) {
                        uninstallFields(securityProviders, securityProvidersFields);
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
                    uninstallFields(Security.class, securityFields);
                } catch (Exception e) {
                    LOGGER.warn("Could not release Security.properties interception", e);
                }
            }

            if (levelFields != null) {
                try {
                    uninstallFields(levelClass, levelFields);
                } catch (Exception e) {
                    LOGGER.warn("Could not release Level.known interception", e);
                }
            }

            synchronized (ProxySelector.class) {
                ProxySelector.setDefault(StackedHandlerUtils.uninstallStackedHandler(proxySelector, ProxySelector.getDefault()));
            }

            synchronized (System.class) {
                if (securityEnabled) {
                    System.setSecurityManager(StackedHandlerUtils.uninstallStackedHandler(vestigeSecurityManager, System.getSecurityManager()));
                }
                System.setProperties(StackedHandlerUtils.uninstallStackedHandler(vestigeProperties, System.getProperties()));
                System.setOut(StackedHandlerUtils.uninstallStackedHandler(out, System.out));
                System.setErr(StackedHandlerUtils.uninstallStackedHandler(err, System.err));
                System.setIn(StackedHandlerUtils.uninstallStackedHandler(in, System.in));
            }

            if (securityEnabled) {
                synchronized (Policy.class) {
                    Policy.setPolicy(StackedHandlerUtils.uninstallStackedHandler(vestigePolicy, Policy.getPolicy()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void uninstallFields(final Class<?> clazz, final Map<String, StackedHandler<?>> valueByFieldName) throws Exception {
        synchronized (clazz) {
            for (final Entry<String, StackedHandler<?>> entry : valueByFieldName.entrySet()) {
                final Field declaredField = clazz.getDeclaredField(entry.getKey());
                setStaticFieldValue(declaredField, StackedHandlerUtils.uninstallStackedHandler((StackedHandler<Object>) entry.getValue(), getStaticFieldValue(declaredField)));
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void installFields(final Class<?> clazz, final Map<String, StackedHandler<?>> valueByFieldName) throws Exception {
        synchronized (clazz) {
            for (final Entry<String, StackedHandler<?>> entry : valueByFieldName.entrySet()) {
                Field declaredField = clazz.getDeclaredField(entry.getKey());
                StackedHandler value = entry.getValue();
                Object previousValue = getStaticFieldValue(declaredField);
                setStaticFieldValue(declaredField, value);
                value.setNextHandler(previousValue);
            }
        }
    }

    public static Object getStaticFieldValue(final Field field) throws Exception {
        Callable<Object> callable = new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                    try {
                        return field.get(null);
                    } finally {
                        field.setAccessible(false);
                    }
                } else {
                    return field.get(null);
                }
            }
        };
        if (Modifier.isFinal(field.getModifiers())) {
            // unset for get too (prevent caching)
            return unsetFinalField(field, callable);
        } else {
            return callable.call();
        }
    }

    public static void setStaticFieldValue(final Field field, final Object value) throws Exception {
        Callable<Void> callable = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                    try {
                        field.set(null, value);
                    } finally {
                        field.setAccessible(false);
                    }
                } else {
                    field.set(null, value);
                }
                return null;
            }
        };
        if (Modifier.isFinal(field.getModifiers())) {
            unsetFinalField(field, callable);
        } else {
            callable.call();
        }
    }

    public static <E> E unsetFinalField(final Field field, final Callable<E> callable) throws Exception {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        boolean accessible = modifiersField.isAccessible();
        if (!accessible) {
            modifiersField.setAccessible(true);
        }
        try {
            int modifiers = field.getModifiers();
            modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
            try {
                return callable.call();
            } finally {
                modifiersField.setInt(field, modifiers);
            }
        } finally {
            if (!accessible) {
                modifiersField.setAccessible(false);
            }
        }
    }

}
