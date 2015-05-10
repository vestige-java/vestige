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

import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

import com.googlecode.vestige.core.StackedHandler;
import com.googlecode.vestige.platform.system.VestigeSystem;
import com.googlecode.vestige.platform.system.VestigeSystemHolder;

/**
 * @author Gael Lalire
 */
public class VestigeDriverVector extends Vector<Object> implements StackedHandler<Vector<Object>> {

    private static final long serialVersionUID = -8974921954435686686L;

    private Vector<Object> nextHandler;

    // only for writeDrivers

    private VestigeDriverVector readDrivers;

    // only for readDriver

    // keep key to access vector<Object> instead of vector<Object> directly to
    // avoid keeping strong reference to object inside the vector.
    private Map<VestigeSystem, Object> vectorKeyBySystem;

    private VestigeSystemHolder vestigeSystemHolder;

    public VestigeDriverVector(final VestigeSystemHolder vestigeSystemHolder) {
        this.vestigeSystemHolder = vestigeSystemHolder;
    }

    public VestigeDriverVector(final VestigeSystemHolder vestigeSystemHolder, final Vector<Object> nextHandler, final Map<VestigeSystem, Object> vectorKeyBySystem) {
        this.vestigeSystemHolder = vestigeSystemHolder;
        this.vectorKeyBySystem = vectorKeyBySystem;
        this.nextHandler = nextHandler;
    }

    public void setReadDrivers(final VestigeDriverVector readDrivers) {
        this.readDrivers = readDrivers;
    }

    public VestigeDriverVector getReadDrivers() {
        return readDrivers;
    }

    @Override
    public Vector<Object> getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final Vector<Object> nextHandler) {
        this.nextHandler = nextHandler;
    }

    public Vector<Object> getDriverVector() {
        VestigeSystem system = vestigeSystemHolder.getVestigeSystem();
        if (readDrivers == null) {
            Vector<Object> vector;
            Object key = vectorKeyBySystem.get(system);
            if (key == null) {
                vector = system.getReadDrivers().get(system);
            } else {
                vector = system.getReadDrivers().get(key);
            }
            return vector;
        }
        return system.getWriteDrivers();
    }

    @Override
    public Object elementAt(final int index) {
        return getDriverVector().elementAt(index);
    }

    @Override
    public void removeElementAt(final int index) {
        getDriverVector().removeElementAt(index);
    }

    @Override
    public void addElement(final Object obj) {
        getDriverVector().addElement(obj);
    }

    @Override
    public int size() {
        return getDriverVector().size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        Map<VestigeSystem, Object> vectorKeyBySystem = new WeakHashMap<VestigeSystem, Object>(readDrivers.vectorKeyBySystem);
        readDrivers = new VestigeDriverVector(vestigeSystemHolder, readDrivers.nextHandler, vectorKeyBySystem);
        VestigeSystem system = vestigeSystemHolder.getVestigeSystem();
        Object key = new Object();
        vectorKeyBySystem.put(system, key);
        system.getReadDrivers().put(key, (Vector<Object>) system.getWriteDrivers().clone());
        return readDrivers;
    }

}
