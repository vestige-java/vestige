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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.googlecode.vestige.core.StackedHandler;

/**
 * @author Gael Lalire
 */
public abstract class VestigeCopyOnWriteArrayList extends CopyOnWriteArrayList<Object> implements StackedHandler<CopyOnWriteArrayList<Object>> {

    private static final long serialVersionUID = 998267829607402508L;

    private CopyOnWriteArrayList<Object> nextHandler;

    public VestigeCopyOnWriteArrayList(final CopyOnWriteArrayList<Object> nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public CopyOnWriteArrayList<Object> getNextHandler() {
        return nextHandler;
    }

    public abstract CopyOnWriteArrayList<Object> getCopyOnWriteArrayList();

    @Override
    public void setNextHandler(final CopyOnWriteArrayList<Object> nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public Iterator<Object> iterator() {
        return getCopyOnWriteArrayList().iterator();
    }

    @Override
    public boolean addIfAbsent(final Object e) {
        return getCopyOnWriteArrayList().addIfAbsent(e);
    }

    @Override
    public boolean contains(final Object o) {
        return getCopyOnWriteArrayList().contains(o);
    }

    @Override
    public boolean remove(final Object o) {
        return getCopyOnWriteArrayList().remove(o);
    }

    @Override
    public void add(final int index, final Object element) {
        getCopyOnWriteArrayList().add(index, element);
    }

    @Override
    public boolean add(final Object e) {
        return getCopyOnWriteArrayList().add(e);
    }

    @Override
    public boolean addAll(final Collection<? extends Object> c) {
        return getCopyOnWriteArrayList().addAll(c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends Object> c) {
        return getCopyOnWriteArrayList().addAll(index, c);
    }

    @Override
    public int addAllAbsent(final Collection<? extends Object> c) {
        return getCopyOnWriteArrayList().addAllAbsent(c);
    }

    @Override
    public void clear() {
        getCopyOnWriteArrayList().clear();
    }

    @Override
    public Object clone() {
        return getCopyOnWriteArrayList().clone();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return getCopyOnWriteArrayList().containsAll(c);
    }

    @Override
    public boolean equals(final Object o) {
        return getCopyOnWriteArrayList().equals(o);
    }

    @Override
    public Object get(final int index) {
        return getCopyOnWriteArrayList().get(index);
    }

    @Override
    public int hashCode() {
        return getCopyOnWriteArrayList().hashCode();
    }

    @Override
    public int indexOf(final Object e, final int index) {
        return getCopyOnWriteArrayList().indexOf(e, index);
    }

    @Override
    public int indexOf(final Object o) {
        return getCopyOnWriteArrayList().indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return getCopyOnWriteArrayList().isEmpty();
    }

    @Override
    public int lastIndexOf(final Object e, final int index) {
        return getCopyOnWriteArrayList().lastIndexOf(e, index);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return getCopyOnWriteArrayList().lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return getCopyOnWriteArrayList().listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(final int index) {
        return getCopyOnWriteArrayList().listIterator(index);
    }

    @Override
    public Object remove(final int index) {
        return getCopyOnWriteArrayList().remove(index);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return getCopyOnWriteArrayList().removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return getCopyOnWriteArrayList().retainAll(c);
    }

    @Override
    public Object set(final int index, final Object element) {
        return getCopyOnWriteArrayList().set(index, element);
    }

    @Override
    public int size() {
        return getCopyOnWriteArrayList().size();
    }

    @Override
    public List<Object> subList(final int fromIndex, final int toIndex) {
        return getCopyOnWriteArrayList().subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return getCopyOnWriteArrayList().toArray();
    }

    @Override
    public <T extends Object> T[] toArray(final T[] a) {
        return getCopyOnWriteArrayList().toArray(a);
    }

    @Override
    public String toString() {
        return getCopyOnWriteArrayList().toString();
    }

}
