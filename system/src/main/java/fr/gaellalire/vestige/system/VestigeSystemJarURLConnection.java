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

package fr.gaellalire.vestige.system;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class VestigeSystemJarURLConnection extends JarURLConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(VestigeSystemJarURLConnection.class);

    private URL jarFileUrl;

    private CachedJarFile cachedJarFile;

    private URLConnection jarFileURLConnection;

    private VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler;

    private Object mutex;

    public VestigeSystemJarURLConnection(final VestigeSystemJarURLStreamHandler vestigeApplicationJarURLStreamHandler, final URL url) throws IOException {
        super(url);
        jarFileUrl = getJarFileURL();
        jarFileURLConnection = jarFileUrl.openConnection();
        this.vestigeApplicationJarURLStreamHandler = vestigeApplicationJarURLStreamHandler;
        mutex = new Object();
    }

    @Override
    public void connect() throws IOException {
        synchronized (mutex) {
            if (!connected) {
                if (cachedJarFile == null) {
                    cachedJarFile = vestigeApplicationJarURLStreamHandler.connect(this, jarFileUrl, getUseCaches());
                }
                connected = true;
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

        @Override
        public void close() throws IOException {
            super.close();
            if (!cachedJarFile.isUsingCache()) {
                // this is JDK behavior, strange though why cannot we have multiple getInputStream
                // however it is described in URLConnection class javadoc that when we close, network resources may be freed
                // because we will force cache use, this doesn't matter.
                cachedJarFile.close();
            }
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
        if (cachedJarFile == null) {
            connect();
        }
        return cachedJarFile;
    }

    /**
     * After calling this method, all classes and resources should be loaded. So it can work with less permissions.
     */
    public static void init() {
        JarInputStream.class.getName();
    }

}
