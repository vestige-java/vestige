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

package fr.gaellalire.vestige.jvm_enhancer.boot;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ProxySelector;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.btr.proxy.search.desktop.gnome.ProxySchemasGSettingsAccess;
import com.btr.proxy.search.desktop.win.Win32ProxyUtils;
import com.btr.proxy.util.Logger.LogBackEnd;

import fr.gaellalire.vestige.core.StackedHandler;
import fr.gaellalire.vestige.core.Vestige;
import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.core.parser.NoStateStringParser;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.jvm_enhancer.runtime.JULBackend;
import fr.gaellalire.vestige.jvm_enhancer.runtime.SystemProxySelector;
import fr.gaellalire.vestige.jvm_enhancer.runtime.WeakArrayList;
import fr.gaellalire.vestige.jvm_enhancer.runtime.WeakLevelMap;
import fr.gaellalire.vestige.jvm_enhancer.runtime.WeakSoftCache;
import fr.gaellalire.vestige.jvm_enhancer.runtime.windows.WindowsShutdownHook;

/**
 * @author Gael Lalire
 */
public final class JVMEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JVMEnhancer.class);

    private JVMEnhancer() {
    }

    private static Object getField(final Field field) throws Exception {
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
            return unsetFinalField(field, callable);
        } else {
            return callable.call();
        }
    }

    private static void setField(final Field field, final Object value) throws Exception {
        Callable<Object> callable = new Callable<Object>() {

            @Override
            public Object call() throws Exception {
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

    private static Object unsetFinalField(final Field field, final Callable<Object> callable) throws Exception {
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

    @SuppressWarnings("unchecked")
    public static void boot(final VestigeExecutor vestigeExecutor, final File directory, final Properties properties, final String mainClass, final String[] dargs)
            throws Exception {
        Thread thread = vestigeExecutor.createWorker("bootstrap-sun-worker", true, 0);

        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        LOGGER.debug("calling sun.awt.AppContext.getAppContext");
        try {
            // keep the context classloader in static field
            Class<?> appContextClass = vestigeExecutor.classForName(systemClassLoader, "sun.awt.AppContext");
            vestigeExecutor.invoke(systemClassLoader, appContextClass.getMethod("getAppContext"), null);
        } catch (Exception e) {
            LOGGER.trace("sun.awt.AppContext.getAppContext call failed", e);
        }
        LOGGER.debug("loading javax.security.auth.Policy");
        try {
            // keep the context classloader in static field
            vestigeExecutor.classForName(systemClassLoader, "javax.security.auth.Policy");
        } catch (Exception e) {
            LOGGER.trace("javax.security.auth.Policy load failed", e);
        }
        LOGGER.debug("loading com.sun.org.apache.xerces.internal.dom.DOMNormalizer");
        try {
            // keep an exception in static field
            vestigeExecutor.classForName(systemClassLoader, "com.sun.org.apache.xerces.internal.dom.DOMNormalizer");
        } catch (Exception e) {
            LOGGER.trace("com.sun.org.apache.xerces.internal.dom.DOMNormalizer load failed", e);
        }
        LOGGER.debug("loading com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl");
        try {
            // keep an exception in static field
            vestigeExecutor.classForName(systemClassLoader, "com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl");
        } catch (Exception e) {
            LOGGER.trace("com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl load failed", e);
        }
        LOGGER.debug("loading com.sun.org.apache.xerces.internal.parsers.AbstractDOMParser");
        try {
            // keep an exception in static field
            vestigeExecutor.classForName(systemClassLoader, "com.sun.org.apache.xerces.internal.parsers.AbstractDOMParser");
        } catch (Exception e) {
            LOGGER.trace("com.sun.org.apache.xerces.internal.parsers.AbstractDOMParser load failed", e);
        }
        LOGGER.debug("loading javax.management.remote.JMXServiceURL");
        try {
            // keep an exception in static field
            vestigeExecutor.classForName(systemClassLoader, "javax.management.remote.JMXServiceURL");
        } catch (Exception e) {
            LOGGER.trace("javax.management.remote.JMXServiceURL load failed", e);
        }
        LOGGER.debug("loading sun.java2d.Disposer");
        try {
            // create thread
            vestigeExecutor.classForName(systemClassLoader, "sun.java2d.Disposer");
        } catch (Exception e) {
            LOGGER.trace("sun.java2d.Disposer load failed", e);
        }
        LOGGER.debug("calling javax.security.auth.login.Configuration.getConfiguration");
        try {
            // keep the context classloader in static field
            Class<?> configurationClass = vestigeExecutor.classForName(systemClassLoader, "javax.security.auth.login.Configuration");
            vestigeExecutor.invoke(systemClassLoader, configurationClass.getMethod("getConfiguration"), null);
        } catch (Throwable e) {
            LOGGER.trace("javax.security.auth.login.Configuration.getConfiguration call failed", e);
        }
        LOGGER.debug("calling sun.security.jca.ProviderList.getService");
        try {
            // sun.security.pkcs11.SunPKCS11 create thread (with the context
            // classloader of parent thread) and sun.security.jca.Providers keep
            // it in static field
            Class<?> providersClass = vestigeExecutor.classForName(systemClassLoader, "sun.security.jca.Providers");
            Object providerList = vestigeExecutor.invoke(systemClassLoader, providersClass.getMethod("getProviderList"), null);
            Class<?> providerListClass = vestigeExecutor.classForName(systemClassLoader, "sun.security.jca.ProviderList");
            vestigeExecutor.invoke(systemClassLoader, providerListClass.getMethod("getService", String.class, String.class), providerList, "MessageDigest", "SHA");
        } catch (Exception e) {
            LOGGER.trace("sun.security.jca.ProviderList.getService call failed", e);
        }
        LOGGER.debug("calling sun.misc.GC.requestLatency");
        try {
            // sun.misc.GC create thread (with the context classloader of parent
            // thread)
            Class<?> gcClass = vestigeExecutor.classForName(systemClassLoader, "sun.misc.GC");
            vestigeExecutor.invoke(systemClassLoader, gcClass.getMethod("requestLatency", long.class), null, Long.valueOf(Long.MAX_VALUE - 1));
        } catch (Exception e) {
            LOGGER.trace("sun.misc.GC.requestLatency call failed", e);
        }
        LOGGER.debug("loading com.sun.jndi.ldap.LdapPoolManager");
        try {
            // com.sun.jndi.ldap.LdapPoolManager may create thread (with the
            // context
            // classloader of parent thread)
            vestigeExecutor.classForName(systemClassLoader, "com.sun.jndi.ldap.LdapPoolManager");
        } catch (Exception e) {
            LOGGER.trace("com.sun.jndi.ldap.LdapPoolManager load failed", e);
        }

        List<? extends ClassLoader> privilegedClassloaders;
        String runtimePaths = properties.getProperty("runtime.jar");
        List<URL> urlList = new ArrayList<URL>();
        Vestige.addClasspath(directory, urlList, runtimePaths);

        Function<Thread, Void, RuntimeException> addShutdownHook = null;
        Function<Thread, Void, RuntimeException> removeShutdownHook = null;

        if (runtimePaths == null) {
            privilegedClassloaders = Collections.emptyList();
        } else {
            URL[] urls = new URL[urlList.size()];
            urlList.toArray(urls);
            StringParser stringParser = new NoStateStringParser(0);
            // create classloader with executor to remove this protection domain
            // from access control
            VestigeClassLoader<Void> vestigeClassLoader = vestigeExecutor.createVestigeClassLoader(ClassLoader.getSystemClassLoader(),
                    Collections.singletonList(Collections.<VestigeClassLoader<Void>> singletonList(null)), stringParser, stringParser, urls);

            LOGGER.debug("replacing java.lang.Thread.subclassAudits");
            try {
                Class<?> weakSoftCacheClass = Class.forName(WeakSoftCache.class.getName());
                setField(Thread.class.getDeclaredField("subclassAudits"), weakSoftCacheClass.newInstance());
            } catch (Exception e) {
                LOGGER.trace("java.lang.Thread.subclassAudits replacement failed", e);
            }

            // keep levels in static field
            LOGGER.debug("replacing java.util.logging.Level.known");
            try {
                Field declaredField = Level.class.getDeclaredField("known");
                List<Level> known = (List<Level>) getField(declaredField);

                Class<?> weakArrayListClass = vestigeClassLoader.loadClass(WeakArrayList.class.getName());
                List<Level> weakArrayList = (List<Level>) weakArrayListClass.getConstructor(Level.class).newInstance(Level.OFF);
                weakArrayList.addAll(known);
                setField(declaredField, weakArrayList);
            } catch (NoSuchFieldException nsfe) {
                LOGGER.debug("replacing java.util.logging.Level$KnownLevel");
                try {
                    Class<?> forName = Class.forName("java.util.logging.Level$KnownLevel");
                    Field nameToLevelsField = forName.getDeclaredField("nameToLevels");
                    Field intToLevelsField = forName.getDeclaredField("intToLevels");
                    Field levelObjectField = forName.getDeclaredField("levelObject");
                    Constructor<?> constructor = forName.getDeclaredConstructor(Level.class);
                    levelObjectField.setAccessible(true);
                    constructor.setAccessible(true);

                    Map<String, List<Object>> initialNameToLevels = (Map<String, List<Object>>) getField(nameToLevelsField);

                    Class<?> weakLevelMapClass = vestigeClassLoader.loadClass(WeakLevelMap.class.getName());
                    Constructor<?> weakLevelMapConstructor = weakLevelMapClass.getConstructor(Field.class, Constructor.class);

                    Map<String, List<Object>> nameToLevels = (Map<String, List<Object>>) weakLevelMapConstructor.newInstance(levelObjectField, constructor);
                    for (Entry<String, List<Object>> entry : initialNameToLevels.entrySet()) {
                        List<Object> list = nameToLevels.get(entry.getKey());
                        list.addAll(entry.getValue());
                    }
                    Map<Integer, List<Object>> initialIntToLevels = (Map<Integer, List<Object>>) getField(intToLevelsField);
                    Map<Integer, List<Object>> intToLevels = (Map<Integer, List<Object>>) weakLevelMapConstructor.newInstance(levelObjectField, constructor);
                    for (Entry<Integer, List<Object>> entry : initialIntToLevels.entrySet()) {
                        List<Object> list = intToLevels.get(entry.getKey());
                        list.addAll(entry.getValue());
                    }
                    setField(nameToLevelsField, nameToLevels);
                    setField(intToLevelsField, intToLevels);
                } catch (Exception e) {
                    LOGGER.trace("java.util.logging.Level$KnownLevel replacement failed", e);
                }
            } catch (Exception e) {
                LOGGER.trace("java.util.logging.Level.known replacement failed", e);
            }

            String osName = System.getProperty("os.name").toLowerCase();
            boolean windows = osName.contains("windows");
            boolean mac = osName.contains("mac");
            String arch = "x86";
            if (!System.getProperty("os.arch").equals("w32")) {
                arch = System.getProperty("os.arch");
                if (arch.equals("x86_64")) {
                    arch = "amd64";
                }
            }

            // install proxy selector
            try {
                synchronized (ProxySelector.class) {
                    if (windows) {
                        String proxyUtilPath = properties.getProperty("proxy_vole.proxy_util." + arch);
                        if (proxyUtilPath != null) {
                            File proxyUtilFile = new File(directory, proxyUtilPath);
                            Class<?> win32ProxyUtilsClass = vestigeClassLoader.loadClass(Win32ProxyUtils.class.getName());
                            Method method = win32ProxyUtilsClass.getMethod("init", String.class);
                            try {
                                vestigeExecutor.invoke(vestigeClassLoader, method, null, proxyUtilFile.getAbsolutePath());
                            } catch (Exception e) {
                                LOGGER.warn("Unable to load windows proxy accessor", e);
                            }
                        }
                    } else if (!mac) {
                        String gsettingsPath = properties.getProperty("proxy_vole.gsettings." + arch);
                        if (gsettingsPath != null) {
                            File gsettingsFile = new File(directory, gsettingsPath);
                            Class<?> proxySchemasGSettingsAccessClass = vestigeClassLoader.loadClass(ProxySchemasGSettingsAccess.class.getName());
                            Method method = proxySchemasGSettingsAccessClass.getMethod("init", String.class);
                            try {
                                vestigeExecutor.invoke(vestigeClassLoader, method, null, gsettingsFile.getAbsolutePath());
                            } catch (Exception e) {
                                LOGGER.warn("Unable to load linux proxy accessor", e);
                            }
                        }
                    }

                    // redirect log to JUL
                    Class<?> julBackendClass = vestigeClassLoader.loadClass(JULBackend.class.getName());
                    Class<?> loggerClass = vestigeClassLoader.loadClass(com.btr.proxy.util.Logger.class.getName());
                    loggerClass.getMethod("setBackend", vestigeClassLoader.loadClass(LogBackEnd.class.getName())).invoke(null, julBackendClass.newInstance());

                    ProxySelector proxySelector = ProxySelector.getDefault();
                    Class<?> systemProxySelectorClass = vestigeClassLoader.loadClass(SystemProxySelector.class.getName());
                    Object systemProxySelector = systemProxySelectorClass.newInstance();
                    ((StackedHandler<ProxySelector>) systemProxySelector).setNextHandler(proxySelector);
                    ProxySelector.setDefault((ProxySelector) systemProxySelector);
                }
            } catch (Throwable e) {
                LOGGER.warn("Unable to install proxy vole", e);
            }

            // install exit handler
            if (windows) {
                try {
                    String shutdownHookPath = properties.getProperty("shutdownHook." + arch);
                    if (shutdownHookPath != null) {
                        File shutdownHookFile = new File(directory, shutdownHookPath);

                        Class<?> windowsShutdownHookClass = vestigeClassLoader.loadClass(WindowsShutdownHook.class.getName());
                        Method method = windowsShutdownHookClass.getMethod("init", String.class);
                        vestigeExecutor.invoke(vestigeClassLoader, method, null, shutdownHookFile.getAbsolutePath());
                        addShutdownHook = (Function<Thread, Void, RuntimeException>) windowsShutdownHookClass.getField("ADD_SHUTDOWN_HOOK_FUNCTION").get(null);
                        removeShutdownHook = (Function<Thread, Void, RuntimeException>) windowsShutdownHookClass.getField("REMOVE_SHUTDOWN_HOOK_FUNCTION").get(null);
                    }
                } catch (Throwable e) {
                    LOGGER.warn("Unable to install windows shutdown hook", e);
                }
            }
            privilegedClassloaders = Collections.singletonList(vestigeClassLoader);
        }

        thread.interrupt();
        thread.join();

        runEnhancedMain(mainClass, vestigeExecutor, privilegedClassloaders, addShutdownHook, removeShutdownHook, dargs);
    }

    public static void main(final String[] args) throws Exception {
        vestigeCoreMain(new VestigeExecutor(), args);
    }

    public static void vestigeCoreMain(final VestigeExecutor vestigeExecutor, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("expecting at least 3 arg : directory, properties, mainClass");
        }
        String[] dargs = new String[args.length - 3];
        System.arraycopy(args, 3, dargs, 0, dargs.length);
        Properties properties = new Properties();
        FileInputStream fileInputStream = new FileInputStream(args[1]);
        try {
            properties.load(fileInputStream);
        } finally {
            fileInputStream.close();
        }
        boot(vestigeExecutor, new File(args[0]), properties, args[2], dargs);
    }

    public static void runEnhancedMain(final String mainclass, final VestigeExecutor vestigeExecutor, final List<? extends ClassLoader> privilegedClassloaders,
            final Function<Thread, Void, RuntimeException> addShutdownHook, final Function<Thread, Void, RuntimeException> removeShutdownHook, final String[] dargs)
            throws Exception {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> loadClass = contextClassLoader.loadClass(mainclass);
        try {
            Method method = loadClass.getMethod("vestigeEnhancedCoreMain", VestigeExecutor.class, Function.class, Function.class, List.class, String[].class);
            method.invoke(null, new Object[] {vestigeExecutor, addShutdownHook, removeShutdownHook, privilegedClassloaders, dargs});
        } catch (NoSuchMethodException e) {
            Vestige.runMain(contextClassLoader, mainclass, vestigeExecutor, dargs);
        }

    }

}
