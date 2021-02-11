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
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * This file is not protected against changes. After creating a {@link fr.gaellalire.vestige.core.resource.SecureJarFile} with {@link #file},
 * {@link fr.gaellalire.vestige.core.resource.SecureJarFile#getInputStream()} can be used to check {@link #size}, {@link #sha1} or {@link #sha512}.
 * @author Gael Lalire
 */
public class SecureFile implements Serializable {

    private static final long serialVersionUID = 4455054515626455519L;

    private File file;

    private long size;

    private long lastModified;

    private String sha1;

    private URL codeBase;

    // private SecureFile patch;

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

    public SecureFile(final File file, final URL codeBase, final String sha1) {
        this.file = file;
        this.codeBase = codeBase;
        this.sha1 = sha1;
    }

    public long getSize() {
        return size;
    }

    public String getSha1() {
        return sha1;
    }

    public boolean verify() {
        if (!file.exists()) {
            return false;
        }
        // unsecure but faster check with checksum & lastModified
        long fileLastModified = file.lastModified();
        if (lastModified != fileLastModified && sha1 != null) {
            // check md5
            try {
                FileInputStream inputStream = new FileInputStream(file);
                try {
                    if (!sha1.equals(createChecksum(inputStream, Collections.singletonList("SHA-1"), null).get(0))) {
                        return false;
                    }
                } finally {
                    inputStream.close();
                }
                lastModified = fileLastModified;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public static List<String> createChecksum(final InputStream inputStream, final List<String> algos, final long[] sizeHolder)
            throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
        List<MessageDigest> messageDigests = new ArrayList<MessageDigest>(algos.size());
        long size = 0;
        try {
            for (String algo : algos) {
                messageDigests.add(MessageDigest.getInstance(algo, BouncyCastleProvider.PROVIDER_NAME));
            }
            byte[] buffer = new byte[1024];
            int numRead;
            do {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    size += numRead;
                    for (MessageDigest messageDigest : messageDigests) {
                        messageDigest.update(buffer, 0, numRead);
                    }
                }
            } while (numRead != -1);

        } finally {
            inputStream.close();
        }
        List<String> results = new ArrayList<String>(algos.size());
        for (MessageDigest messageDigest : messageDigests) {
            results.add(toHexString(messageDigest.digest()));
        }
        if (sizeHolder != null) {
            sizeHolder[0] = size;
        }
        return results;
    }

    public File getFile() {
        return file;
    }

    public URL getCodeBase() {
        return codeBase;
    }

    public String createSha512() throws IOException {
        InputStream inputStream = new FileInputStream(file);
        try {
            try {
                List<String> checksums = SecureFile.createChecksum(inputStream, Arrays.asList("SHA-512"), null);
                return checksums.get(0);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Cannot verify checkum", e);
            } catch (NoSuchProviderException e) {
                throw new IOException("Cannot verify checkum", e);
            }

        } finally {
            inputStream.close();
        }
    }

}
