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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @author Gael Lalire
 */
public class SecureFile implements Serializable {

    private static final long serialVersionUID = 4455054515626455519L;

    private File file;

    private long lastModified;

    private byte[] checksum;

    private byte[] signature;

    public SecureFile(final File file) {
        this.file = file;
        try {
            this.checksum = createChecksum(file);
        } catch (Exception e) {
            // no md5
        }
    }

    public boolean verify() {
        if (!file.exists()) {
            return false;
        }
        if (signature == null) {
            // unsecure but faster check with checksum & lastModified
            long fileLastModified = file.lastModified();
            if (lastModified != fileLastModified && checksum != null) {
                // check md5
                try {
                    byte[] fileChecksum = createChecksum(file);
                    if (!Arrays.equals(checksum, fileChecksum)) {
                        file.delete();
                        return false;
                    }
                    lastModified = fileLastModified;
                } catch (Exception e) {
                    file.delete();
                    return false;
                }
            }
        }
        return true;
    }

    public static byte[] createChecksum(final File filename) throws IOException, NoSuchAlgorithmException {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("SHA1");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    public File getFile() {
        return file;
    }

}
