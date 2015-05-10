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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @param <E> item type
 * @author Gael Lalire
 */
public class WeakArrayList<E> extends ArrayList<E> {

    private ArrayList<WeakReference<E>> weakReferences = new ArrayList<WeakReference<E>>();

    private static final long serialVersionUID = 1L;

    private E gcValue;

    public WeakArrayList(final E gcValue) {
        this.gcValue = gcValue;
    }

    public List<E> createStrongList() {
        List<E> sList = new ArrayList<E>(weakReferences.size());
        Iterator<WeakReference<E>> iterator = weakReferences.iterator();
        while (iterator.hasNext()) {
            WeakReference<E> next = iterator.next();
            E e = next.get();
            if (e == null) {
                iterator.remove();
            } else {
                sList.add(e);
            }
        }
        return sList;
    }

    public void expunge() {
        Iterator<WeakReference<E>> iterator = weakReferences.iterator();
        while (iterator.hasNext()) {
            WeakReference<E> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
    }

    @Override
    public int size() {
        expunge();
        return weakReferences.size();
    }

    @Override
    public E get(final int index) {
        E e = weakReferences.get(index).get();
        if (e != null) {
            return e;
        }
        return gcValue;
    }

    @Override
    public boolean add(final E e) {
        weakReferences.add(new WeakReference<E>(e));
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        for (E e : c) {
            add(e);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        WeakArrayList<E> weakArrayList = (WeakArrayList<E>) super.clone();
        weakArrayList.weakReferences = (ArrayList<WeakReference<E>>) weakReferences.clone();
        return weakArrayList;
    }

}
