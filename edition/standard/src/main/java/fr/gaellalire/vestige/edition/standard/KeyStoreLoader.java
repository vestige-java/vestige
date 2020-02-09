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

package fr.gaellalire.vestige.edition.standard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public abstract class KeyStoreLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreLoader.class);

    private static final String DEFAULT_PASSWORD = "changeit";

    public KeyStore load(final File keyStoreFile) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);

        char[] password = DEFAULT_PASSWORD.toCharArray();
        boolean generateKS = false;
        if (keyStoreFile.exists()) {
            try {
                FileInputStream stream = new FileInputStream(keyStoreFile);
                try {
                    keyStore.load(stream, password);
                } finally {
                    stream.close();
                }
            } catch (Exception e) {
                LOGGER.error("Unable to load keyStore at " + keyStoreFile);
                LOGGER.debug("KeyStore load failed", e);
                generateKS = true;
            }
        } else {
            generateKS = true;
        }
        if (generateKS) {
            keyStore.load(null, password);
            init(keyStore);

            FileOutputStream fos = new FileOutputStream(keyStoreFile);
            keyStoreFile.setReadable(false, false);
            keyStoreFile.setWritable(false, false);
            keyStoreFile.setReadable(true, true);
            keyStoreFile.setWritable(true, true);
            keyStoreFile.setExecutable(false, false);
            try {
                keyStore.store(fos, password);
            } finally {
                fos.close();
            }
            keyStoreWritten(keyStoreFile);
        }
        return keyStore;
    }

    public void keyStoreWritten(final File keyStoreFile) {

    }

    public abstract void init(KeyStore keyStore) throws Exception;

}
