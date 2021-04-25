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

/**
 * @param <T> reference type
 * @author Gael Lalire
 */
public abstract class JustInTimeReference<T> {

    private T reference;

    private Object mutex = new Object();

    // will only be set when the object is fully initialized
    // volatile will work correctly only after Java 1.4
    private volatile T volatileReference;

    protected abstract T create();

    public T getNoCreate() {
        if (reference == null) {
            if (volatileReference == null) {
                synchronized (mutex) {
                    if (volatileReference == null) {
                        return null;
                    }
                }
            }
        }
        return reference;
    }

    public T get() {
        if (reference == null) {
            if (volatileReference == null) {
                synchronized (mutex) {
                    if (volatileReference == null) {
                        volatileReference = create();
                    }
                }
            }
            reference = volatileReference;
        }
        return reference;
    }

}
