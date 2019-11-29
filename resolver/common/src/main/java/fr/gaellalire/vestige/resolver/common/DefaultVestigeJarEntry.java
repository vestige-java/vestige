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

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import fr.gaellalire.vestige.spi.resolver.VestigeJarEntry;

/**
 * @author Gael Lalire
 */
public class DefaultVestigeJarEntry implements VestigeJarEntry {

    private JarFile jarFile;

    private JarEntry je;

    public DefaultVestigeJarEntry(final JarFile jarFile, final JarEntry je) {
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

}
