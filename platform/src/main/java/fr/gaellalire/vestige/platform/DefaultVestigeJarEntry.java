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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import fr.gaellalire.vestige.spi.resolver.VestigeJar;
import fr.gaellalire.vestige.spi.resolver.VestigeJarEntry;

/**
 * @author Gael Lalire
 */
public class DefaultVestigeJarEntry implements VestigeJarEntry {

    private VestigeJar vestigeJar;

    private JarFile jarFile;

    private JarEntry je;

    public DefaultVestigeJarEntry(final VestigeJar vestigeJar, final JarFile jarFile, final JarEntry je) {
        this.vestigeJar = vestigeJar;
        this.jarFile = jarFile;
        this.je = je;
    }

    @Override
    public long getSize() {
        return je.getSize();
    }

    @Override
    public InputStream open() throws IOException {
        return jarFile.getInputStream(je);
    }

    @Override
    public boolean isDirectory() {
        return je.isDirectory();
    }

    @Override
    public long getModificationTime() {
        return je.getTime();
    }

    @Override
    public String getName() {
        return je.getName();
    }

    @Override
    public Certificate[] getCertificates() {
        return je.getCertificates();
    }

    @Override
    public VestigeJar getVestigeJar() {
        return vestigeJar;
    }

    @Override
    public String toString() {
        return "jar:" + vestigeJar.getCodeBase() + "!/" + getName();
    }

}
