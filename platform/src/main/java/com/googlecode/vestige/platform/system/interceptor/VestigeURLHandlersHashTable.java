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

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;
import java.util.Map;

import com.googlecode.vestige.core.StackedHandler;
import com.googlecode.vestige.platform.system.VestigeSystem;
import com.googlecode.vestige.platform.system.VestigeSystemHolder;

/**
 * @author Gael Lalire
 */
public class VestigeURLHandlersHashTable extends Hashtable<String, URLStreamHandler> implements StackedHandler<Hashtable<String, URLStreamHandler>> {

    private static final long serialVersionUID = 774582859985938991L;

    private Hashtable<String, URLStreamHandler> nextHandler;

    private VestigeSystemHolder vestigeSystemHolder;

    public VestigeURLHandlersHashTable(final VestigeSystemHolder vestigeSystemHolder) {
        this.vestigeSystemHolder = vestigeSystemHolder;
    }

    @Override
    public URLStreamHandler get(final Object protocol) {
        VestigeSystem system = vestigeSystemHolder.getVestigeSystem();
        Map<String, URLStreamHandler> urlStreamHandlerByProtocol = system.getURLStreamHandlerByProtocol();
        URLStreamHandler urlStreamHandler = urlStreamHandlerByProtocol.get(protocol);
        if (urlStreamHandler == null) {
            URLStreamHandlerFactory urlStreamHandlerFactory = system.getURLStreamHandlerFactory();
            if (urlStreamHandlerFactory != null) {
                urlStreamHandler = urlStreamHandlerFactory.createURLStreamHandler((String) protocol);
                if (urlStreamHandler == null) {
                    return null;
                }
                urlStreamHandlerByProtocol.put((String) protocol, urlStreamHandler);
            }
        }
        return urlStreamHandler;
    }

    @Override
    public URLStreamHandler put(final String protocol, final URLStreamHandler urlStreamHandler) {
        VestigeSystem system = vestigeSystemHolder.getVestigeSystem();
        return system.getURLStreamHandlerByProtocol().put(protocol, urlStreamHandler);
    }

    @Override
    public Hashtable<String, URLStreamHandler> getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final Hashtable<String, URLStreamHandler> nextHandler) {
        this.nextHandler = nextHandler;
    }

}
