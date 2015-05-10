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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * @param <K> key of map
 * @author Gael Lalire
 */
public class WeakLevelMap<K> extends HashMap<K, List<Object>> {

    private static final long serialVersionUID = -175282012195131917L;

    private Field levelField;

    private Constructor<?> knownLevelConstructor;

    private HashMap<K, WeakArrayList<Level>> levelByKey;

    public WeakLevelMap(final Field levelField, final Constructor<?> knownLevelConstructor) {
        levelByKey = new HashMap<K, WeakArrayList<Level>>();
        this.levelField = levelField;
        this.knownLevelConstructor = knownLevelConstructor;
    }

    public Object createKnownLevel(final Level l) {
        try {
            return knownLevelConstructor.newInstance(l);
        } catch (Exception e) {
            return null;
        }
    }

    public Level getLevel(final Object knownLevel) {
        try {
            return (Level) levelField.get(knownLevel);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @author Gael Lalire
     */
    class KnowLevelArrayList extends ArrayList<Object> {

        private static final long serialVersionUID = -421803630073619102L;

        private K key;

        private List<Level> strongList;

        private WeakArrayList<Level> weakArrayList;

        public KnowLevelArrayList(final K key, final WeakArrayList<Level> weakArrayList, final List<Level> strongList) {
            this.key = key;
            this.weakArrayList = weakArrayList;
            this.strongList = strongList;
        }

        @Override
        public boolean addAll(final Collection<? extends Object> c) {
            for (Object o : c) {
                add(o);
            }
            return true;
        }

        @Override
        public boolean add(final Object e) {
            Level level = getLevel(e);
            if (strongList == null) {
                strongList = new ArrayList<Level>();
                weakArrayList = new WeakArrayList<Level>(Level.OFF);
                levelByKey.put(key, weakArrayList);
            }
            strongList.add(level);
            weakArrayList.add(level);
            return true;
        }

        @Override
        public Object get(final int index) {
            if (strongList == null) {
                return null;
            }
            return createKnownLevel(strongList.get(index));
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {

                private int maxSize = strongList.size();

                private int pos = 0;

                @Override
                public boolean hasNext() {
                    if (strongList == null) {
                        return false;
                    }
                    return pos != maxSize;
                }

                @Override
                public Object next() {
                    return get(pos++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

            };
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> get(final Object key) {
        List<Level> strongList = null;
        WeakArrayList<Level> list = levelByKey.get(key);
        if (list != null) {
            strongList = list.createStrongList();
            if (strongList.size() == 0) {
                strongList = null;
                levelByKey = null;
                levelByKey.remove(key);
            }
        }
        return new KnowLevelArrayList((K) key, list, strongList);
    }

    @Override
    public java.util.List<Object> put(final K key, final java.util.List<Object> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<List<Object>> values() {
        Collection<WeakArrayList<Level>> values = levelByKey.values();
        List<List<Object>> lists = new ArrayList<List<Object>>();
        Iterator<WeakArrayList<Level>> iterator = values.iterator();
        while (iterator.hasNext()) {
            WeakArrayList<Level> weakArrayList = iterator.next();
            List<Level> createStrongList = weakArrayList.createStrongList();
            int size = createStrongList.size();
            if (size == 0) {
                iterator.remove();
            } else {
                List<Object> list = new ArrayList<Object>(size);
                for (Level level : createStrongList) {
                    list.add(createKnownLevel(level));
                }
                lists.add(list);
            }
        }
        return lists;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        WeakLevelMap<K> clone = (WeakLevelMap<K>) super.clone();
        clone.levelByKey = (HashMap<K, WeakArrayList<Level>>) levelByKey.clone();
        Iterator<java.util.Map.Entry<K, WeakArrayList<Level>>> iterator = clone.levelByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<K, WeakArrayList<Level>> entry = iterator.next();
            entry.setValue((WeakArrayList<Level>) entry.getValue().clone());
        }
        return clone;
    }

}
