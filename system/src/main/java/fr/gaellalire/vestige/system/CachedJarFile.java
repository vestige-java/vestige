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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class CachedJarFile extends JarFile implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedJarFile.class);

    private VestigeSystemJarURLStreamHandler vestigeSystemJarURLStreamHandler;

    private File file;

    private boolean temporary;

    private boolean closed;

    private int vestigeSystemUserCount;

    private Object mutex;

    public CachedJarFile(final File file, final boolean temporary, final VestigeSystemJarURLStreamHandler vestigeSystemJarURLStreamHandler) throws IOException {
        super(file);
        this.file = file;
        this.temporary = temporary;
        this.vestigeSystemJarURLStreamHandler = vestigeSystemJarURLStreamHandler;
        this.mutex = new Object();
    }

    public boolean isUsingCache() {
        return vestigeSystemJarURLStreamHandler != null;
    }

    public File getFile() {
        return file;
    }

    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (vestigeSystemJarURLStreamHandler != null) {
            vestigeSystemJarURLStreamHandler.removeFromCaches(file);
        }
        LOGGER.info("Closing {}", file);
        try {
            super.close();
        } catch (IOException e) {
            LOGGER.error("Unable to close jarFile", e);
        }
        if (temporary && !file.delete()) {
            LOGGER.error("Unable to delete temporary jarFile {}", file);
        }
        closed = true;
    }

    public void addVestigeSystemUser() {
        synchronized (mutex) {
            vestigeSystemUserCount++;
        }
    }

    public synchronized void removeVestigeSystemUser() {
        synchronized (mutex) {
            vestigeSystemUserCount--;
        }
    }

    public synchronized int getVestigeSystemUserCount() {
        synchronized (mutex) {
            return vestigeSystemUserCount;
        }
    }

}
