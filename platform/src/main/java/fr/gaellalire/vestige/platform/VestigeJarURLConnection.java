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

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.Map;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class VestigeJarURLConnection extends JarURLConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeJarURLConnection.class);

    private File file;

    private JarFile jarFile;

    private URLConnection jarFileURLConnection;

    private Map<File, JarFile> cache;

    protected VestigeJarURLConnection(final URL url, final Map<File, JarFile> cache) throws IOException {
        super(url);
        this.cache = cache;
        try {
            file = new File(getJarFileURL().toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Could not get URI", e);
        }
        jarFileURLConnection = getJarFileURL().openConnection();
    }

    @Override
    public void connect() throws IOException {
        synchronized (this) {
            if (jarFile == null) {
                synchronized (cache) {
                    jarFile = cache.get(file);
                    if (jarFile == null) {
                        jarFile = new JarFile(file);
                        cache.put(file, jarFile);
                    }
                }
            }
        }
    }

    /**
     * Retain a reference to this to prevent this to be GC while an input stream is open.
     * @author Gael Lalire
     */
    private class JarInputStream extends FilterInputStream {

        protected JarInputStream(final InputStream in) {
            super(in);
        }

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new JarInputStream(getJarFile().getInputStream(getJarEntry()));
    }

    @Override
    public Permission getPermission() throws IOException {
        return jarFileURLConnection.getPermission();
    }

    public long getContentLengthLong() {
        try {
            return getJarEntry().getSize();
        } catch (IOException e) {
            LOGGER.error("Unable to get content length", e);
            return -1;
        }
    }

    @Override
    public int getContentLength() {
        try {
            return (int) getJarEntry().getSize();
        } catch (IOException e) {
            LOGGER.error("Unable to get content length", e);
            return -1;
        }
    }

    @Override
    public JarFile getJarFile() throws IOException {
        if (jarFile == null) {
            connect();
        }
        return jarFile;
    }

}
