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

package fr.gaellalire.vestige.application.manager.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import fr.gaellalire.vestige.application.manager.PrivilegedExceptionActionExecutor;

/**
 * Proxy interfaces method parameters must be shared between source and target. <br>
 * Following elements can be proxyfied :
 * <ul>
 * <li>the return type</li>
 * <li>checked exception can be converted, but not its cause</li>
 * </ul>
 * @author Gael Lalire
 */
public class ProxyInvocationHandler implements InvocationHandler {

    private Map<String, Map<Integer, Method>> delegateMethods = new HashMap<String, Map<Integer, Method>>();

    private ClassLoader classLoader;

    private Object source;

    private PrivilegedExceptionActionExecutor privilegedExecutor;

    public static void init() {
        // preload ClassLoaderObjectInputStream
        ClassLoaderObjectInputStream.class.getName();
    }

    /**
     * @author Gael Lalire
     */
    private static class ClassLoaderObjectInputStream extends ObjectInputStream {

        private ClassLoader classLoader;

        ClassLoaderObjectInputStream(final InputStream in, final ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return Class.forName(desc.getName(), false, classLoader);
        }

    }

    public ProxyInvocationHandler(final ClassLoader classLoader, final Class<?> sourceItf, final Object source, final PrivilegedExceptionActionExecutor privilegedExecutor) {
        this.classLoader = classLoader;
        this.source = source;
        this.privilegedExecutor = privilegedExecutor;

        for (Method method : sourceItf.getMethods()) {
            String methodName = method.getName();
            Map<Integer, Method> map = delegateMethods.get(methodName);
            if (map == null) {
                map = new HashMap<Integer, Method>();
                delegateMethods.put(methodName, map);
            }
            Method oldMethod = map.put(method.getParameterTypes().length, method);
            if (oldMethod != null) {
                if (Arrays.asList(method.getParameterTypes()).equals(Arrays.asList(oldMethod.getParameterTypes()))) {
                    // check which one is the parent
                    Class<?> oldDeclaringClass = oldMethod.getDeclaringClass();
                    Class<?> declaringClass = method.getDeclaringClass();

                    boolean correctOverride = false;
                    if (Arrays.asList(oldDeclaringClass.getInterfaces()).contains(declaringClass)) {
                        correctOverride = true;
                    } else {
                        Class<?> superClass = oldDeclaringClass.getSuperclass();
                        while (superClass != null) {
                            if (superClass.equals(declaringClass)) {
                                correctOverride = true;
                                break;
                            }
                            superClass = superClass.getSuperclass();
                        }
                    }
                    if (correctOverride) {
                        // old is a child of of current, child is more precise
                        map.put(oldMethod.getParameterTypes().length, oldMethod);
                    } else {
                        if (Arrays.asList(declaringClass.getInterfaces()).contains(oldDeclaringClass)) {
                            correctOverride = true;
                        } else {
                            Class<?> superClass = declaringClass.getSuperclass();
                            while (superClass != null) {
                                if (superClass.equals(oldDeclaringClass)) {
                                    // current is a child of of old, child is more precise
                                    correctOverride = true;
                                    break;
                                }
                                superClass = superClass.getSuperclass();
                            }
                        }
                    }
                    if (!correctOverride) {
                        throw new UnsupportedOperationException("Cannot overload a method with same number of parameters");
                    }
                } else {
                    throw new UnsupportedOperationException("Cannot overload a method with same number of parameters");
                }
            }
        }
    }

    public static Object createProxy(final ClassLoader classLoader, final Class<?> sourceItf, final Class<?> itf, final Object source,
            final PrivilegedExceptionActionExecutor privilegedExecutor) {
        return Proxy.newProxyInstance(classLoader, new Class<?>[] {itf}, new ProxyInvocationHandler(classLoader, sourceItf, source, privilegedExecutor));
    }

    public Throwable convertException(final ClassLoader classLoader, final Throwable throwable) throws IOException, ClassNotFoundException {
        final Throwable savedCause = throwable.getCause();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        privilegedExecutor.setPrivileged();
        try {
            objectOutputStream = new ObjectOutputStream(out) {
                {
                    enableReplaceObject(true);
                }

                @Override
                protected Object replaceObject(final Object obj) throws IOException {
                    if (obj == savedCause) {
                        // fix the cause to self to be able to call initCause later
                        return throwable;
                    }
                    return obj;
                }
            };
        } finally {
            privilegedExecutor.unsetPrivileged();
        }
        try {
            objectOutputStream.writeObject(throwable);
        } finally {
            objectOutputStream.close();
        }

        Throwable o;
        ClassLoaderObjectInputStream in = new ClassLoaderObjectInputStream(new ByteArrayInputStream(out.toByteArray()), classLoader);
        try {
            o = (Throwable) in.readObject();
        } finally {
            in.close();
        }

        o.initCause(savedCause);

        return o;
    }

    public Class<?> getTypeClass(final Type parameterizedType) {
        Type type = ((ParameterizedType) parameterizedType).getActualTypeArguments()[0];
        final Class<?> result;
        if (type instanceof WildcardType) {
            result = (Class<?>) ((WildcardType) type).getUpperBounds()[0];
        } else {
            result = (Class<?>) type;
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        Map<Integer, Method> map = delegateMethods.get(method.getName());
        if (map == null) {
            if (Object.class.equals(method.getDeclaringClass())) {
                // hashCode, equals, or toString
                if ("equals".equals(method.getName())) {
                    return args[0] == proxy;
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                return method.invoke(source, args);
            }
            throw new UnsupportedOperationException("Method " + method.getName() + " not found");
        }
        Class<?>[] parameterTypeArray = method.getParameterTypes();
        Method delegateMethod = map.get(parameterTypeArray.length);
        if (delegateMethod == null) {
            throw new UnsupportedOperationException("Method " + method.getName() + " not found with matching parameters");
        }
        Object[] dargs = null;

        if (args != null) {
            dargs = new Object[args.length];
            Class<?>[] dParameterTypes = delegateMethod.getParameterTypes();
            for (int i = 0; i < dParameterTypes.length; i++) {
                Class<?> parameterType = dParameterTypes[i];
                if (parameterType.isEnum()) {
                    dargs[i] = Enum.valueOf((Class) parameterType, ((Enum<?>) args[i]).name());
                } else if (parameterType != parameterTypeArray[i]) {
                    dargs[i] = createProxy(parameterType.getClassLoader(), parameterTypeArray[i], parameterType, args[i], privilegedExecutor);
                } else {
                    dargs[i] = args[i];
                }
            }
        }
        try {
            Object invoke = delegateMethod.invoke(source, dargs);

            if (invoke != null) {
                if (Enumeration.class.equals(method.getReturnType())) {
                    final Class<?> genericReturnType = getTypeClass(method.getGenericReturnType());
                    final Class<?> delegateGenericReturnType = getTypeClass(delegateMethod.getGenericReturnType());
                    if (genericReturnType != delegateGenericReturnType) {
                        final Enumeration<?> enumeration = (Enumeration<?>) invoke;
                        return new Enumeration<Object>() {

                            @Override
                            public boolean hasMoreElements() {
                                return enumeration.hasMoreElements();
                            }

                            @Override
                            public Object nextElement() {
                                return createProxy(classLoader, delegateGenericReturnType, genericReturnType, enumeration.nextElement(), privilegedExecutor);
                            }
                        };
                    }
                } else if (delegateMethod.getReturnType() != method.getReturnType()) {
                    return createProxy(classLoader, delegateMethod.getReturnType(), method.getReturnType(), invoke, privilegedExecutor);
                }
            }
            return invoke;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            Class<?> causeClass = cause.getClass();
            if (!RuntimeException.class.isAssignableFrom(causeClass) && !Error.class.isAssignableFrom(causeClass)) {
                cause = convertException(classLoader, cause);
            }
            throw cause;
        }
    }

    @Override
    public String toString() {
        return source.toString();
    }

}
