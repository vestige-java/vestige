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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

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

    public ProxyInvocationHandler(final ClassLoader classLoader, final Class<?> sourceItf, final Object source) {
        this.classLoader = classLoader;
        this.source = source;

        for (Method method : sourceItf.getMethods()) {
            String methodName = method.getName();
            Map<Integer, Method> map = delegateMethods.get(methodName);
            if (map == null) {
                map = new HashMap<Integer, Method>();
                delegateMethods.put(methodName, map);
            }
            if (map.put(method.getParameterTypes().length, method) != null) {
                throw new UnsupportedOperationException("Cannot overload a method with same number of parameters");
            }
        }
    }

    public static Object createProxy(final ClassLoader classLoader, final Class<?> sourceItf, final Class<?> itf, final Object source) {
        return Proxy.newProxyInstance(classLoader, new Class<?>[] {itf}, new ProxyInvocationHandler(classLoader, sourceItf, source));
    }

    public static Throwable convertException(final ClassLoader classLoader, final Throwable throwable) throws IOException, ClassNotFoundException {
        Field causeField = null;
        Object savedCause = null;
        try {
            causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            savedCause = causeField.get(throwable);
            causeField.set(throwable, null);
        } catch (Exception e) {
            // serialize all stacktrace
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
        objectOutputStream.writeObject(throwable);

        Throwable o = (Throwable) new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())) {
            @Override
            protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, classLoader);
            }
        }.readObject();

        if (savedCause != null) {
            try {
                causeField.set(o, savedCause);
                causeField.set(throwable, savedCause);
                causeField.setAccessible(false);
            } catch (Exception e) {
                // should not happen
            }
        }

        return o;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        Map<Integer, Method> map = delegateMethods.get(method.getName());
        if (map == null) {
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
                    dargs[i] = createProxy(parameterType.getClassLoader(), parameterTypeArray[i], parameterType, args[i]);
                } else {
                    dargs[i] = args[i];
                }
            }
        }
        try {
            Object invoke = delegateMethod.invoke(source, dargs);

            if (invoke != null && delegateMethod.getReturnType() != method.getReturnType()) {
                return createProxy(classLoader, delegateMethod.getReturnType(), method.getReturnType(), invoke);
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
}
