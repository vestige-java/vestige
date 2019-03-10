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
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Gael Lalire
 */
public class SecureFile implements Serializable {

    private static final long serialVersionUID = 4455054515626455519L;

    private File file;

    private long lastModified;

    private String sha1sum;

    private URL codeBase;

    public static String toHexString(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder buffer = new StringBuilder(bytes.length * 2);

        for (byte aByte : bytes) {
            int b = aByte & 0xFF;
            if (b < 0x10) {
                buffer.append('0');
            }
            buffer.append(Integer.toHexString(b));
        }

        return buffer.toString();
    }

    public SecureFile(final File file, final URL codeBase, final String sha1sum) {
        this.file = file;
        this.codeBase = codeBase;
        this.sha1sum = sha1sum;
    }

    public boolean verify() {
        if (!file.exists()) {
            return false;
        }
        // unsecure but faster check with checksum & lastModified
        long fileLastModified = file.lastModified();
        if (lastModified != fileLastModified && sha1sum != null) {
            // check md5
            try {
                if (!sha1sum.equals(createChecksum(file))) {
                    return false;
                }
                lastModified = fileLastModified;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public static String createChecksum(final File filename) throws IOException, NoSuchAlgorithmException {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("SHA-1");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return toHexString(complete.digest());
    }

    public File getFile() {
        return file;
    }

    public URL getCodeBase() {
        return codeBase;
    }

}
