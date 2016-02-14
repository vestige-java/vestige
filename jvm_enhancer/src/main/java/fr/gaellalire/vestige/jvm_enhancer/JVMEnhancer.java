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

package fr.gaellalire.vestige.jvm_enhancer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ProxySelector;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import com.btr.proxy.util.Logger;

import fr.gaellalire.vestige.core.StackedHandlerUtils;
import fr.gaellalire.vestige.core.Vestige;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.jvm_enhancer.windows.WindowsShutdownHook;

/**
 * @author Gael Lalire
 */
public final class JVMEnhancer {

    private JVMEnhancer() {
    }

    private static SystemProxySelector systemProxySelector;

    public static void start() {
        // INSTALL
        synchronized (ProxySelector.class) {
            Logger.setBackend(new SLF4JBackend());
            ProxySelector proxySelector = ProxySelector.getDefault();
            systemProxySelector = new SystemProxySelector();
            systemProxySelector.setNextHandler(proxySelector);
            ProxySelector.setDefault(systemProxySelector);
        }
    }

    public static void stop() {
        // UNINSTALL
        synchronized (ProxySelector.class) {
            ProxySelector proxySelector = ProxySelector.getDefault();
            ProxySelector newProxySelector = StackedHandlerUtils.uninstallStackedHandler(systemProxySelector, proxySelector);
            if (proxySelector != newProxySelector) {
                ProxySelector.setDefault(newProxySelector);
            }
            systemProxySelector = null;
            Logger.setBackend(null);
        }
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
    public static void boot(final VestigeExecutor vestigeExecutor) throws InterruptedException {
        Thread thread = vestigeExecutor.createWorker("bootstrap-sun-worker", true, 0);

        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try {
            // keep the context classloader in static field
            Class<?> appContextClass = vestigeExecutor.classForName(systemClassLoader, "sun.awt.AppContext");
            vestigeExecutor.invoke(systemClassLoader, appContextClass.getMethod("getAppContext"), null);
        } catch (Exception e) {
            // ignore
        }
        try {
            // keep the context classloader in static field
            vestigeExecutor.classForName(systemClassLoader, "javax.security.auth.Policy");
        } catch (Exception e) {
            // ignore
        }
        try {
            // keep an exception in static field
            vestigeExecutor.classForName(systemClassLoader, "com.sun.org.apache.xerces.internal.dom.DOMNormalizer");
        } catch (Exception e) {
            // ignore
        }
        try {
            // keep an exception in static field
            vestigeExecutor.classForName(systemClassLoader, "com.sun.org.apache.xerces.internal.parsers.AbstractDOMParser");
        } catch (Exception e) {
            // ignore
        }
        try {
            // keep an exception in static field
            vestigeExecutor.classForName(systemClassLoader, "javax.management.remote.JMXServiceURL");
        } catch (Exception e) {
            // ignore
        }
        try {
            // sun.security.pkcs11.SunPKCS11 create thread (with the context
            // classloader of parent thread) and sun.security.jca.Providers keep
            // it in static field
            Class<?> providersClass = vestigeExecutor.classForName(systemClassLoader, "sun.security.jca.Providers");
            Object providerList = vestigeExecutor.invoke(systemClassLoader, providersClass.getMethod("getProviderList"), null);
            Class<?> providerListClass = vestigeExecutor.classForName(systemClassLoader, "sun.security.jca.ProviderList");
            vestigeExecutor.invoke(systemClassLoader, providerListClass.getMethod("getService", String.class, String.class), providerList, "MessageDigest", "SHA");
        } catch (Exception e) {
            // ignore
        }
        try {
            // sun.misc.GC create thread (with the context classloader of parent
            // thread)
            Class<?> gcClass = vestigeExecutor.classForName(systemClassLoader, "sun.misc.GC");
            vestigeExecutor.invoke(systemClassLoader, gcClass.getMethod("requestLatency", long.class), null, Long.valueOf(Long.MAX_VALUE - 1));
        } catch (Exception e) {
            // ignore
        }
        try {
            // com.sun.jndi.ldap.LdapPoolManager may create thread (with the
            // context
            // classloader of parent thread)
            vestigeExecutor.classForName(systemClassLoader, "com.sun.jndi.ldap.LdapPoolManager");
        } catch (Exception e) {
            // ignore
        }

        try {
            Class<?> weakSoftCacheClass = Class.forName("fr.gaellalire.vestige.jvm_enhancer.WeakSoftCache");
            setField(Thread.class.getDeclaredField("subclassAudits"), weakSoftCacheClass.newInstance());
        } catch (Exception e) {
            // ignore
        }

        try {
            // keep levels in static field
            try {
                Field declaredField = Level.class.getDeclaredField("known");
                List<Level> known = (List<Level>) getField(declaredField);
                WeakArrayList<Level> weakArrayList = new WeakArrayList<Level>(Level.OFF);
                weakArrayList.addAll(known);
                setField(declaredField, weakArrayList);
            } catch (NoSuchFieldException e) {
                Class<?> forName = Class.forName("java.util.logging.Level$KnownLevel");
                Field nameToLevelsField = forName.getDeclaredField("nameToLevels");
                Field intToLevelsField = forName.getDeclaredField("intToLevels");
                Field levelObjectField = forName.getDeclaredField("levelObject");
                Constructor<?> constructor = forName.getDeclaredConstructor(Level.class);
                levelObjectField.setAccessible(true);
                constructor.setAccessible(true);

                Map<String, List<Object>> initialNameToLevels = (Map<String, List<Object>>) getField(nameToLevelsField);
                Map<String, List<Object>> nameToLevels = new WeakLevelMap<String>(levelObjectField, constructor);
                for (Entry<String, List<Object>> entry : initialNameToLevels.entrySet()) {
                    List<Object> list = nameToLevels.get(entry.getKey());
                    list.addAll(entry.getValue());
                }
                Map<Integer, List<Object>> initialIntToLevels = (Map<Integer, List<Object>>) getField(intToLevelsField);
                Map<Integer, List<Object>> intToLevels = new WeakLevelMap<Integer>(levelObjectField, constructor);
                for (Entry<Integer, List<Object>> entry : initialIntToLevels.entrySet()) {
                    List<Object> list = intToLevels.get(entry.getKey());
                    list.addAll(entry.getValue());
                }
                setField(nameToLevelsField, nameToLevels);
                setField(intToLevelsField, intToLevels);
            }
        } catch (Exception e) {
            // ignore
        }

        thread.interrupt();
        thread.join();
    }

    public static void main(final String[] args) throws Exception {
        vestigeCoreMain(new VestigeExecutor(), args);
    }

    public static void vestigeCoreMain(final VestigeExecutor vestigeExecutor, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("expecting at least 1 arg : mainClass");
        }
        // preload some crappy classes to avoid classloader leak
        boot(vestigeExecutor);
        // install system proxy selector
        start();
        try {
            String[] dargs = new String[args.length - 1];
            System.arraycopy(args, 1, dargs, 0, dargs.length);
            runEnhancedMain(Thread.currentThread().getContextClassLoader(), args[0], vestigeExecutor, dargs);
        } finally {
            stop();
        }
    }

    public static void runEnhancedMain(final ClassLoader classLoader, final String mainclass, final VestigeExecutor vestigeExecutor, final String[] dargs) throws Exception {
        Class<?> loadClass = classLoader.loadClass(mainclass);
        try {
            Method method = loadClass.getMethod("vestigeEnhancedCoreMain", VestigeExecutor.class, Function.class, Function.class, String[].class);

            Function<Thread, Void, RuntimeException> addShutdownHook = null;
            Function<Thread, Void, RuntimeException> removeShutdownHook = null;

            String osName = System.getProperty("os.name");
            try {
                if (osName.toLowerCase().contains("windows")) {
                    Class.forName(WindowsShutdownHook.class.getName(), true, WindowsShutdownHook.class.getClassLoader());
                    addShutdownHook = new Function<Thread, Void, RuntimeException>() {
                        @Override
                        public Void apply(final Thread thread) throws RuntimeException {
                            WindowsShutdownHook.addShutdownHook(thread);
                            return null;
                        }
                    };
                    removeShutdownHook = new Function<Thread, Void, RuntimeException>() {
                        @Override
                        public Void apply(final Thread thread) throws RuntimeException {
                            WindowsShutdownHook.removeShutdownHook(thread);
                            return null;
                        }
                    };
                }
            } catch (UnsatisfiedLinkError e) {
                // continue with JVM shutdown hooks
            } catch (ExceptionInInitializerError e) {
                // continue with JVM shutdown hooks
            }
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                if (vestigeExecutor == null) {
                    method.invoke(null, new Object[] {new VestigeExecutor(), addShutdownHook, removeShutdownHook, dargs});
                } else {
                    method.invoke(null, new Object[] {vestigeExecutor, addShutdownHook, removeShutdownHook, dargs});
                }
            } finally {
                Thread.currentThread().setContextClassLoader(null);
            }
        } catch (NoSuchMethodException e) {
            Vestige.runMain(classLoader, mainclass, vestigeExecutor, dargs);
        }

    }

}
