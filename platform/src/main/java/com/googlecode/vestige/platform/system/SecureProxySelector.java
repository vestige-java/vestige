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

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

/**
 * @author Gael Lalire
 */
public class SecureProxySelector extends ProxySelector {

    private ProxySelector proxySelector;

    public SecureProxySelector(final ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
    }

    @Override
    public List<Proxy> select(final URI uri) {
        return AccessController.doPrivileged(new PrivilegedAction<List<Proxy>>() {

            @Override
            public List<Proxy> run() {
                return proxySelector.select(uri);
            }
        });
    }

    @Override
    public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                proxySelector.connectFailed(uri, sa, ioe);
                return null;
            }
        });
    }

}
