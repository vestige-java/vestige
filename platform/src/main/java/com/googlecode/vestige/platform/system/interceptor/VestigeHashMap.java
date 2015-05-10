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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @param <K> item type
 * @param <V> value type
 * @author Gael Lalire
 */
public abstract class VestigeHashMap<K, V> extends HashMap<K, V> implements StackedHandler<HashMap<K, V>> {

    private static final long serialVersionUID = -5111459794019460272L;

    private HashMap<K, V> nextHandler;

    public VestigeHashMap(final HashMap<K, V> nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public HashMap<K, V> getNextHandler() {
        return nextHandler;
    }

    @Override
    public void setNextHandler(final HashMap<K, V> nextHandler) {
        this.nextHandler = nextHandler;
    }

    public abstract HashMap<K, V> getHashMap();

    @Override
    public void clear() {
        getHashMap().clear();
    }

    @Override
    public Object clone() {
        return getHashMap().clone();
    }

    @Override
    public boolean containsKey(final Object key) {
        return getHashMap().containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return getHashMap().containsValue(value);
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return getHashMap().entrySet();
    }

    @Override
    public boolean equals(final Object o) {
        return getHashMap().equals(o);
    }

    @Override
    public V get(final Object key) {
        return getHashMap().get(key);
    }

    @Override
    public int hashCode() {
        return getHashMap().hashCode();
    }

    @Override
    public boolean isEmpty() {
        return getHashMap().isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return getHashMap().keySet();
    }

    @Override
    public V put(final K key, final V value) {
        return getHashMap().put(key, value);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        getHashMap().putAll(m);
    }

    @Override
    public V remove(final Object key) {
        return getHashMap().remove(key);
    }

    @Override
    public int size() {
        return getHashMap().size();
    }

    @Override
    public String toString() {
        return getHashMap().toString();
    }

    @Override
    public Collection<V> values() {
        return getHashMap().values();
    }

}
