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

package fr.gaellalire.vestige.platform;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.VestigeCoreURLStreamHandler;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandler;

/**
 * @author Gael Lalire
 */
public class VestigeURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private Map<String, DelegateURLStreamHandler> map;

    public VestigeURLStreamHandlerFactory() {
        this(new HashMap<String, DelegateURLStreamHandler>());
    }

    public VestigeURLStreamHandlerFactory(final Map<String, DelegateURLStreamHandler> map) {
        this.map = map;
    }

    public DelegateURLStreamHandler get(final String protocol) {
        synchronized (map) {
            return map.get(protocol);
        }
    }

    public void put(final String protocol, final DelegateURLStreamHandler delegateURLStreamHandler) {
        synchronized (map) {
            map.put(protocol, delegateURLStreamHandler);
        }
    }

    public Map<String, DelegateURLStreamHandler> copyMap() {
        synchronized (map) {
            return new HashMap<String, DelegateURLStreamHandler>(map);
        }
    }

    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol) {
        if (VestigeCoreURLStreamHandler.PROTOCOL.equals(protocol)) {
            return VestigeClassLoader.URL_STREAM_HANDLER;
        }
        synchronized (map) {
            return map.get(protocol);
        }
    }

}
