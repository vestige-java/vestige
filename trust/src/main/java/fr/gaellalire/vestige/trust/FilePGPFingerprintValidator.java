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

package fr.gaellalire.vestige.trust;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class FilePGPFingerprintValidator implements PGPFingerprintValidator {

    private File trustFile;

    public FilePGPFingerprintValidator(final File trustFile) {
        this.trustFile = trustFile;
    }

    @Override
    public boolean validate(final String fingerprint) throws TrustException {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(trustFile));
            try {
                String readLine = bufferedReader.readLine();
                while (readLine != null) {
                    readLine = readLine.replaceAll("\\p{javaSpaceChar}", "");
                    if (readLine.equalsIgnoreCase(fingerprint)) {
                        return true;
                    }
                    readLine = bufferedReader.readLine();
                }
            } finally {
                bufferedReader.close();
            }
        } catch (IOException e) {
            throw new TrustException(e);
        }
        return false;
    }

}
