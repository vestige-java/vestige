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

package fr.gaellalire.vestige.platform;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;

import fr.gaellalire.vestige.core.resource.VestigeResource;
import fr.gaellalire.vestige.spi.resolver.VestigeJarEntry;

/**
 * @author Gael Lalire
 */
public class VestigeResourceVestigeJarEntry implements VestigeJarEntry {

    private VestigeResource vestigeResource;

    public VestigeResourceVestigeJarEntry(final VestigeResource vestigeResource) {
        this.vestigeResource = vestigeResource;
    }

    @Override
    public long getSize() {
        return vestigeResource.getSize();
    }

    @Override
    public InputStream open() throws IOException {
        return vestigeResource.getInputStream();
    }

    @Override
    public boolean isDirectory() {
        return vestigeResource.getName().endsWith("/");
    }

    @Override
    public long getModificationTime() {
        return vestigeResource.getModificationTime();
    }

    @Override
    public String getName() {
        return vestigeResource.getName();
    }

    @Override
    public Certificate[] getCertificates() {
        return vestigeResource.getCertificates();
    }

}
