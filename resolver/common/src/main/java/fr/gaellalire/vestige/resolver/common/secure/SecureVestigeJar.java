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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import fr.gaellalire.vestige.spi.resolver.VestigeJar;
import fr.gaellalire.vestige.spi.resolver.VestigeJarEntry;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class SecureVestigeJar implements VestigeJar {

    private VestigeSystem secureVestigeSystem;

    private VestigeJar delegate;

    public SecureVestigeJar(final VestigeSystem secureVestigeSystem, final VestigeJar delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public URL getCodeBase() {
        return delegate.getCodeBase();
    }

    @Override
    public long getLastModified() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.getLastModified();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public Manifest getManifest() throws IOException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.getManifest();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public long getSize() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.getSize();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
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
    @Deprecated
    public File getFile() {
        return delegate.getFile();
    }

    @Override
    public Enumeration<? extends VestigeJarEntry> getEntries() throws IOException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return new SecureEnumeration<VestigeJarEntry>(vestigeSystem, new ElementSecureMaker<VestigeJarEntry>() {

                @Override
                public VestigeJarEntry makeSecure(final VestigeJarEntry e) {
                    return new SecureVestigeJarEntry(secureVestigeSystem, e);
                }
            }, delegate.getEntries());
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public VestigeJarEntry getEntry(final String name) throws IOException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            VestigeJarEntry entry = delegate.getEntry(name);
            if (entry == null) {
                return null;
            }
            return new SecureVestigeJarEntry(secureVestigeSystem, entry);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public VestigeJar getPatch() {
        return delegate.getPatch();
    }

}
