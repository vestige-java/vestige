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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @param <E> item type
 * @author Gael Lalire
 */
public abstract class VestigeArrayList<E> extends ArrayList<E> implements StackedHandler<ArrayList<E>> {

    private static final long serialVersionUID = -3443987501289759122L;

    private ArrayList<E> nextHandler;

    public VestigeArrayList(final ArrayList<E> nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public ArrayList<E> getNextHandler() {
        return nextHandler;
    }

    public abstract ArrayList<E> getArrayList();

    @Override
    public void setNextHandler(final ArrayList<E> nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public boolean add(final E e) {
        return getArrayList().add(e);
    }

    @Override
    public void add(final int index, final E element) {
        getArrayList().add(index, element);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return getArrayList().addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        return getArrayList().addAll(index, c);
    }

    @Override
    public Object clone() {
        return getArrayList().clone();
    }

    @Override
    public boolean contains(final Object o) {
        return getArrayList().contains(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return getArrayList().containsAll(c);
    }

    @Override
    public void ensureCapacity(final int minCapacity) {
        getArrayList().ensureCapacity(minCapacity);
    }

    @Override
    public boolean equals(final Object o) {
        return getArrayList().equals(o);
    }

    @Override
    public E get(final int index) {
        return getArrayList().get(index);
    }

    @Override
    public int hashCode() {
        return getArrayList().hashCode();
    }

    @Override
    public int indexOf(final Object o) {
        return getArrayList().indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return getArrayList().isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return getArrayList().iterator();
    }

    @Override
    public int lastIndexOf(final Object o) {
        return getArrayList().lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return getArrayList().listIterator();
    }

    @Override
    public ListIterator<E> listIterator(final int index) {
        return getArrayList().listIterator(index);
    }

    @Override
    public E remove(final int index) {
        return getArrayList().remove(index);
    }

    @Override
    public boolean remove(final Object o) {
        return getArrayList().remove(o);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return getArrayList().removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return getArrayList().retainAll(c);
    }

    @Override
    public E set(final int index, final E element) {
        return getArrayList().set(index, element);
    }

    @Override
    public int size() {
        return getArrayList().size();
    }

    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        return getArrayList().subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return getArrayList().toArray();
    }

    @Override
    public <T extends Object> T[] toArray(final T[] a) {
        return getArrayList().toArray(a);
    }

    @Override
    public String toString() {
        return getArrayList().toString();
    }

    @Override
    public void trimToSize() {
        getArrayList().trimToSize();
    }

    @Override
    public void clear() {
        getArrayList().clear();
    }

}
