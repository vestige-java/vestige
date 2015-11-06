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

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.search.ProxySearch.Strategy;

import fr.gaellalire.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public class SystemProxySelector extends ProxySelector implements StackedHandler<ProxySelector> {

    private ProxySelector proxySelector;

    public static final List<Proxy> NO_PROXY_LIST = Collections.singletonList(Proxy.NO_PROXY);

    public SystemProxySelector() {
        ProxySearch proxySearch = new ProxySearch();
        proxySearch.addStrategy(Strategy.JAVA);
        proxySearch.addStrategy(Strategy.OS_DEFAULT);
        proxySearch.addStrategy(Strategy.ENV_VAR);
        proxySelector = proxySearch.getProxySelector();
    }

    @Override
    public List<Proxy> select(final URI uri) {
        if (proxySelector == null) {
            return NO_PROXY_LIST;
        }
        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(SystemProxySelector.class.getClassLoader());
        try {
            return proxySelector.select(uri);
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
        if (proxySelector == null) {
            return;
        }
        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(SystemProxySelector.class.getClassLoader());
        try {
            proxySelector.connectFailed(uri, sa, ioe);
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
