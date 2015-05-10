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

package com.googlecode.vestige.jvm_enhancer;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.search.ProxySearch.Strategy;
import com.btr.proxy.util.ProxyUtil;
import com.googlecode.vestige.core.StackedHandler;


/**
 * @author Gael Lalire
 */
public class SystemProxySelector extends ProxySelector implements StackedHandler<ProxySelector> {

    private ProxySearch proxySearch;

    public SystemProxySelector() {
        proxySearch = new ProxySearch();
        proxySearch.addStrategy(Strategy.OS_DEFAULT);
    }

    @Override
    public List<Proxy> select(final URI uri) {
        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(SystemProxySelector.class.getClassLoader());
        try {
            ProxySelector proxySelector = proxySearch.getProxySelector();
            if (proxySelector == null) {
                return ProxyUtil.noProxyList();
            }
            return proxySelector.select(uri);
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(SystemProxySelector.class.getClassLoader());
        try {
            ProxySelector proxySelector = proxySearch.getProxySelector();
            if (proxySelector != null) {
                proxySelector.connectFailed(uri, sa, ioe);
            }
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }
    }

    private ProxySelector nextHandler;

    public ProxySelector getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(final ProxySelector nextHandler) {
        this.nextHandler = nextHandler;
    }

}
