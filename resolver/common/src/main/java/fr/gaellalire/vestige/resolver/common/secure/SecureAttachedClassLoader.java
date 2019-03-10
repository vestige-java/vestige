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

import fr.gaellalire.vestige.spi.resolver.AttachableClassLoader;
import fr.gaellalire.vestige.spi.resolver.AttachedClassLoader;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureAttachedClassLoader implements AttachedClassLoader {

    private VestigeSystem secureVestigeSystem;

    private AttachedClassLoader delegate;

    public SecureAttachedClassLoader(final VestigeSystem secureVestigeSystem, final AttachedClassLoader delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public AttachableClassLoader getAttachableClassLoader() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureAttachableClassLoader(secureVestigeSystem, delegate.getAttachableClassLoader());
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void detach() {
        delegate.detach();
    }

}
