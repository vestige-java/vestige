/*
 * Copyright 2019 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gaellalire.vestige.resolver.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import fr.gaellalire.vestige.platform.FileWithMetadata;
import fr.gaellalire.vestige.spi.resolver.VestigeJar;
import fr.gaellalire.vestige.spi.resolver.VestigeJarEntry;

/**
 * @author Gael Lalire
 */
public class DefaultVestigeJar implements VestigeJar {

    private URL codeBase;

    private File file;

    private JarFile jarFile;

    public DefaultVestigeJar(final FileWithMetadata secureFile) {
        this.file = secureFile.getFile();
        this.codeBase = secureFile.getCodeBase();
    }

    public JarFile getJarFile() throws IOException {
        if (jarFile == null) {
            this.jarFile = new JarFile(file);
        }
        return jarFile;
    }

    @Override
    public Enumeration<VestigeJarEntry> getEntries() throws IOException {
        final Enumeration<JarEntry> entries = getJarFile().entries();
        return new Enumeration<VestigeJarEntry>() {

            @Override
            public boolean hasMoreElements() {
                return entries.hasMoreElements();
            }

            @Override
            public VestigeJarEntry nextElement() {
                return new DefaultVestigeJarEntry(jarFile, entries.nextElement());
            }
        };
    }

    @Override
    public long getLastModified() {
        return file.lastModified();
    }

    @Override
    public Manifest getManifest() throws IOException {
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
        return file.length();
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    @Deprecated
    public File getFile() {
        return file;
    }

    @Override
    public VestigeJarEntry getEntry(final String name) throws IOException {
        JarEntry jarEntry = getJarFile().getJarEntry(name);
        if (jarEntry == null) {
            return null;
        }
        return new DefaultVestigeJarEntry(jarFile, jarEntry);
    }

}
