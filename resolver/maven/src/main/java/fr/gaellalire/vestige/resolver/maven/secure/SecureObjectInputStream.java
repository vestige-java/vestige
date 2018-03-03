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

package fr.gaellalire.vestige.resolver.maven.secure;

import java.io.IOException;
import java.io.ObjectInputStream;

import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureObjectInputStream extends ObjectInputStream {

    private VestigeSystem secureVestigeSystem;

    private ObjectInputStream delegate;

    public SecureObjectInputStream(final VestigeSystem secureVestigeSystem, final ObjectInputStream delegate) throws IOException, SecurityException {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public void readFully(final byte[] buf) throws IOException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.readFully(buf);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public int readInt() throws IOException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.readInt();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

}
