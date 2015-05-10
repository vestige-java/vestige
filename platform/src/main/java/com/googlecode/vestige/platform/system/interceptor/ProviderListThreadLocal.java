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

package com.googlecode.vestige.platform.system.interceptor;

import java.util.Arrays;
import java.util.List;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class ProviderListThreadLocal extends InheritableThreadLocal<Object> implements StackedHandler<ThreadLocal<Object>> {

    private static final List<String> THREAD_LOCAL_METHOD_NAMES = Arrays.asList("beginThreadProviderList", "endThreadProviderList");

    private ThreadLocal<Object> nextHandler;

    public abstract void setSecurityProviderList(Object object);

    public abstract Object getSecurityProviderList();

    public ProviderListThreadLocal(final ThreadLocal<Object> nextHandler) {
        this.nextHandler = nextHandler;
    }

    public boolean isThreadLocal() {
        StackTraceElement stackTraceElement = new Throwable().getStackTrace()[2];
        if ("sun.security.jca.Providers".equals(stackTraceElement.getClassName()) && THREAD_LOCAL_METHOD_NAMES.contains(stackTraceElement.getMethodName())) {
            return true;
        }
        return false;
    }

    @Override
    public Object get() {
        Object object = nextHandler.get();
        if (object != null) {
            return object;
        }
        if (isThreadLocal()) {
            return null;
        }
        return getSecurityProviderList();
    }

    @Override
    public void remove() {
        nextHandler.remove();
    }

    @Override
    public void set(final Object value) {
        if (isThreadLocal()) {
            nextHandler.set(value);
        } else {
            Object object = nextHandler.get();
            if (object != null) {
                nextHandler.set(value);
            } else {
                setSecurityProviderList(value);
            }
        }
    }

    @Override
    public ThreadLocal<Object> getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final ThreadLocal<Object> nextHandler) {
        this.nextHandler = nextHandler;
    }

}
