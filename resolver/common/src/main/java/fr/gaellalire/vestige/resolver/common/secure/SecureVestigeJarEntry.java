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

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;

import fr.gaellalire.vestige.spi.resolver.VestigeJarEntry;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureVestigeJarEntry implements VestigeJarEntry {

    private VestigeSystem secureVestigeSystem;

    private VestigeJarEntry delegate;

    public SecureVestigeJarEntry(final VestigeSystem secureVestigeSystem, final VestigeJarEntry delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    public InputStream open() throws IOException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.open();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public boolean isDirectory() {
        return delegate.isDirectory();
    }

    @Override
    public long getModificationTime() {
        return delegate.getModificationTime();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Certificate[] getCertificates() {
        return delegate.getCertificates();
    }

    @Override
    public VestigeJarEntry nextEntry() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            VestigeJarEntry nextEntry = delegate.nextEntry();
            if (nextEntry == null) {
                return null;
            }
            return new SecureVestigeJarEntry(vestigeSystem, nextEntry);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

}
