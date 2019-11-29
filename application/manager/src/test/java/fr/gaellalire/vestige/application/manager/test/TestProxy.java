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

package fr.gaellalire.vestige.application.manager.test;

import java.util.Collections;
import java.util.Enumeration;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import fr.gaellalire.vestige.application.manager.PrivilegedExceptionActionExecutor;
import fr.gaellalire.vestige.application.manager.proxy.ProxyInvocationHandler;

/**
 * @author Gael Lalire
 */
public class TestProxy {

    @Test
    public void testName() throws Exception {
        ClassLoader cl1 = new MyClassLoader("cl1");
        ClassLoader cl2 = new MyClassLoader("cl2");
        Class<?> loadClass1 = cl1.loadClass(MyClassLoader.ITF_NAME);
        Class<?> loadClass1a2 = cl1.loadClass(MyClassLoader.ITF2_NAME);
        Class<?> loadClass1e = cl1.loadClass(MyClassLoader.EXCEPTION_NAME);
        Class<?> loadClass2 = cl2.loadClass(MyClassLoader.ITF_NAME);
        Class<?> loadClass2a2 = cl2.loadClass(MyClassLoader.ITF2_NAME);
        Class<?> loadClass2e = cl2.loadClass(MyClassLoader.EXCEPTION_NAME);
        Object createMock = EasyMock.createMock(loadClass1);
        Object createMock2 = EasyMock.createMock(loadClass1a2);

        PrivilegedExceptionActionExecutor privilegedExceptionActionExecutor = EasyMock.createMock(PrivilegedExceptionActionExecutor.class);

        Assert.assertNotSame(loadClass1, loadClass2);
        Assert.assertNotSame(loadClass1a2, loadClass2a2);
        Assert.assertNotSame(loadClass1e, loadClass2e);

        Throwable exception1 = (Throwable) loadClass1e.getConstructor(String.class, Throwable.class).newInstance("mess", new RuntimeException("vlan"));

        loadClass1.getMethod("hello").invoke(createMock);
        EasyMock.expect(loadClass1.getMethod("withReturn").invoke(createMock)).andReturn(createMock2);
        loadClass1a2.getMethod("method").invoke(createMock2);
        EasyMock.expect(loadClass1.getMethod("withException").invoke(createMock)).andThrow(exception1);
        Throwable exception2 = new RuntimeException();
        EasyMock.expect(loadClass1.getMethod("withException").invoke(createMock)).andThrow(exception2);

        EasyMock.replay(createMock);
        EasyMock.replay(createMock2);

        Object createProxy = ProxyInvocationHandler.createProxy(cl2, loadClass1, loadClass2, createMock, privilegedExceptionActionExecutor);
        loadClass2.getMethod("hello").invoke(createProxy);
        Object invoke = loadClass2.getMethod("withReturn").invoke(createProxy);
        loadClass2a2.getMethod("method").invoke(invoke);
        try {
            loadClass2.getMethod("withException").invoke(createProxy);
        } catch (Throwable e) {
            Throwable cause = e.getCause();
            Assert.assertNotSame(cause, exception1);
            Assert.assertEquals(cause.getClass(), loadClass2e);
            Assert.assertSame(cause.getCause(), exception1.getCause());
        }
        try {
            loadClass2.getMethod("withException").invoke(createProxy);
        } catch (Throwable e) {
            Throwable cause = e.getCause();
            Assert.assertSame(cause, exception2);
        }

        EasyMock.verify(createMock);
        EasyMock.verify(createMock2);
    }

    @Test
    public void testEnumeration() throws Exception {
        ClassLoader cl1 = new MyClassLoader("cl1");
        ClassLoader cl2 = new MyClassLoader("cl2");
        Class<?> loadClass1 = cl1.loadClass(MyClassLoader.ITF_NAME);
        Class<?> loadClass1a2 = cl1.loadClass(MyClassLoader.ITF2_NAME);
        Class<?> loadClass1e = cl1.loadClass(MyClassLoader.EXCEPTION_NAME);
        Class<?> loadClass2 = cl2.loadClass(MyClassLoader.ITF_NAME);
        Class<?> loadClass2a2 = cl2.loadClass(MyClassLoader.ITF2_NAME);
        Class<?> loadClass2e = cl2.loadClass(MyClassLoader.EXCEPTION_NAME);
        Object createMock = EasyMock.createMock(loadClass1);
        Object createMock2 = EasyMock.createMock(loadClass1a2);

        PrivilegedExceptionActionExecutor privilegedExceptionActionExecutor = EasyMock.createMock(PrivilegedExceptionActionExecutor.class);

        Assert.assertNotSame(loadClass1, loadClass2);
        Assert.assertNotSame(loadClass1a2, loadClass2a2);
        Assert.assertNotSame(loadClass1e, loadClass2e);

        EasyMock.expect(loadClass1.getMethod("enumTest").invoke(createMock)).andReturn(Collections.enumeration(Collections.singletonList(createMock2)));
        loadClass1a2.getMethod("method").invoke(createMock2);

        EasyMock.replay(createMock);
        EasyMock.replay(createMock2);

        Object createProxy = ProxyInvocationHandler.createProxy(cl2, loadClass1, loadClass2, createMock, privilegedExceptionActionExecutor);
        Enumeration<?> invoke = (Enumeration<?>) loadClass2.getMethod("enumTest").invoke(createProxy);
        System.out.println(invoke);
        Assert.assertTrue(invoke.hasMoreElements());
        Object nextElement = invoke.nextElement();
        Assert.assertNotNull(nextElement.toString());
        nextElement.hashCode();
        Assert.assertTrue(nextElement.equals(nextElement));
        Assert.assertFalse(nextElement.equals(createMock2));
        loadClass2a2.getMethod("method").invoke(nextElement);

        Assert.assertFalse(invoke.hasMoreElements());

        EasyMock.verify(createMock);
        EasyMock.verify(createMock2);
    }

}
