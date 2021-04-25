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

package fr.gaellalire.vestige.system.interceptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import fr.gaellalire.vestige.core.StackedHandler;
import fr.gaellalire.vestige.system.VestigeSystemHolder;

/**
 * @author Gael Lalire
 */
public class TCPTransportLocalEndpoints extends HashMap<Object, List<Object>> implements StackedHandler<HashMap<Object, List<Object>>> {

    private static final long serialVersionUID = 4609225710625644657L;

    private HashMap<Object, List<Object>> nextHandler;

    private VestigeSystemHolder vestigeSystemHolder;

    public TCPTransportLocalEndpoints(final VestigeSystemHolder vestigeSystemHolder) {
        this.vestigeSystemHolder = vestigeSystemHolder;
    }

    @Override
    public List<Object> get(final Object key) {
        return vestigeSystemHolder.getVestigeSystemCache().getTcpEndpointLocalEndpoints().get(key);
    }

    @Override
    public List<Object> put(final Object key, final List<Object> value) {
        return vestigeSystemHolder.getVestigeSystemCache().getTcpEndpointLocalEndpoints().put(key, value);
    }

    @Override
    public int size() {
        return vestigeSystemHolder.getVestigeSystemCache().getTcpEndpointLocalEndpoints().size();
    }

    @Override
    public Collection<List<Object>> values() {
        return vestigeSystemHolder.getVestigeSystemCache().getTcpEndpointLocalEndpoints().values();
    }

    @Override
    public HashMap<Object, List<Object>> getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final HashMap<Object, List<Object>> nextHandler) {
        this.nextHandler = nextHandler;
    }

}
