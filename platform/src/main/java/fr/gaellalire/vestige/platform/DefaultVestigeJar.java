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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import fr.gaellalire.vestige.core.resource.SecureFile;
import fr.gaellalire.vestige.core.resource.VestigeResource;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;
import fr.gaellalire.vestige.core.weak.CloseableReaperHelper;
import fr.gaellalire.vestige.core.weak.VestigeReaper;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;
import fr.gaellalire.vestige.spi.resolver.VestigeJarEntry;

/**
 * @author Gael Lalire
 */
public class DefaultVestigeJar implements VestigeJar {

    private URL codeBase;

    private File file;

    private VestigeJar patchVestigeJar;

    private JarFile jarFile;

    private SecureFile secureFile;

    private VestigeResourceLocator vestigeResourceLocator;

    private VestigeReaper vestigeReaper;

    public DefaultVestigeJar(final AbstractFileWithMetadata fileWithMetadata, final SecureFile secureFile, final VestigeJar patchVestigeJar,
            final VestigeResourceLocator vestigeResourceLocator, final VestigeReaper vestigeReaper) {
        this.file = fileWithMetadata.getFile();
        this.codeBase = fileWithMetadata.getCodeBase();
        this.secureFile = secureFile;
        this.patchVestigeJar = patchVestigeJar;
        this.vestigeResourceLocator = vestigeResourceLocator;
        this.vestigeReaper = vestigeReaper;
    }

    public JarFile getJarFile() throws IOException {
        if (jarFile == null) {
            this.jarFile = new JarFile(file);
            // why cast to Closeable is necessary ?
            vestigeReaper.addReapable(this, new CloseableReaperHelper(new Closeable() {

                @Override
                public void close() throws IOException {
                    jarFile.close();
                }
            }));
        }
        return jarFile;
    }

    @Override
    public Enumeration<VestigeJarEntry> getEntries() throws IOException {
        if (vestigeResourceLocator != null) {
            return new Enumeration<VestigeJarEntry>() {

                private Iterator<String> resourceNames = vestigeResourceLocator.getResourceNames().iterator();

                @Override
                public boolean hasMoreElements() {
                    return resourceNames.hasNext();
                }

                @Override
                public VestigeJarEntry nextElement() {
                    return new VestigeResourceVestigeJarEntry(DefaultVestigeJar.this, vestigeResourceLocator.findResource(resourceNames.next()));
                }
            };
        }

        final Enumeration<JarEntry> entries = getJarFile().entries();
        return new Enumeration<VestigeJarEntry>() {

            @Override
            public boolean hasMoreElements() {
                return entries.hasMoreElements();
            }

            @Override
            public VestigeJarEntry nextElement() {
                return new DefaultVestigeJarEntry(DefaultVestigeJar.this, jarFile, entries.nextElement());
            }
        };
    }

    @Override
    public long getLastModified() {
        if (vestigeResourceLocator != null) {
            return vestigeResourceLocator.getLastModified();
        }

        return file.lastModified();
    }

    @Override
    public Manifest getManifest() throws IOException {
        if (vestigeResourceLocator != null) {
            return vestigeResourceLocator.getManifest();
        }

        return getJarFile().getManifest();
    }

    @Override
    public URL getCodeBase() {
        return codeBase;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public long getSize() {
        if (secureFile != null) {
            secureFile.getSize();
        }
        return file.length();
    }

    @Override
    public InputStream open() throws IOException {
        if (secureFile != null) {
            return secureFile.getInputStream();
        }
        return new FileInputStream(file);
    }

    @Override
    @Deprecated
    public File getFile() {
        return file;
    }

    @Override
    public VestigeJarEntry getEntry(final String name) throws IOException {
        if (vestigeResourceLocator != null) {
            VestigeResource vestigeResource = vestigeResourceLocator.findResource(name);
            if (vestigeResource == null) {
                return null;
            }
            return new VestigeResourceVestigeJarEntry(DefaultVestigeJar.this, vestigeResource);
        }

        JarEntry jarEntry = getJarFile().getJarEntry(name);
        if (jarEntry == null) {
            return null;
        }
        return new DefaultVestigeJarEntry(this, jarFile, jarEntry);
    }

    @Override
    public VestigeJar getPatch() {
        return patchVestigeJar;
    }

    @Override
    public String toString() {
        if (codeBase != null) {
            return codeBase.toString();
        }
        return file.getName();
    }

}
