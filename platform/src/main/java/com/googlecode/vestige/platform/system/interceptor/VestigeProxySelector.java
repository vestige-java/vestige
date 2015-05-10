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

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class VestigeProxySelector extends ProxySelector implements StackedHandler<ProxySelector> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeProxySelector.class);

    private ProxySelector nextHandler;

    public abstract ProxySelector getProxySelector();

    public VestigeProxySelector(final ProxySelector nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public List<Proxy> select(final URI uri) {
        ProxySelector proxySelector = getProxySelector();
        if (proxySelector == null) {
            return Collections.singletonList(Proxy.NO_PROXY);
        }
        return proxySelector.select(uri);
    }

    @Override
    public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
        ProxySelector proxySelector = getProxySelector();
        if (proxySelector == null) {
            if (System.getSecurityManager() == null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("connectFailed URI:{}, socketAddress:{}, IOException:{}", new Object[] {uri, sa, ioe});
                }
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("connectFailed URI:{}, socketAddress:{}, IOException:{}", new Object[] {uri, sa, ioe});
                        }
                        return null;
                    }
                });
            }
        } else {
            proxySelector.connectFailed(uri, sa, ioe);
        }
    }

    @Override
    public ProxySelector getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final ProxySelector nextHandler) {
        this.nextHandler = nextHandler;
    }

}
