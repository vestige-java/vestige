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

package fr.gaellalire.vestige.resolver.common.secure;

import java.util.Enumeration;

import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 * @param <E> element type
 */
public class SecureEnumeration<E> implements Enumeration<E> {

    private VestigeSystem secureVestigeSystem;

    private ElementSecureMaker<E> elementSecureMaker;

    private Enumeration<? extends E> delegate;

    public SecureEnumeration(final VestigeSystem secureVestigeSystem, final ElementSecureMaker<E> elementSecureMaker, final Enumeration<? extends E> delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.elementSecureMaker = elementSecureMaker;
        this.delegate = delegate;
    }

    @Override
    public boolean hasMoreElements() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.hasMoreElements();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public E nextElement() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            E nextElement = delegate.nextElement();
            if (elementSecureMaker == null) {
                return nextElement;
            }
            return elementSecureMaker.makeSecure(nextElement);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}
