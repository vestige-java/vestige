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

import java.io.File;
import java.io.IOException;

import fr.gaellalire.vestige.spi.trust.PGPTrustSystem;
import fr.gaellalire.vestige.spi.trust.TrustSystemAccessor;

/**
 * @author Gael Lalire
 */
public class DefaultTrustSystemAccessor implements TrustSystemAccessor {

    private BCPGPTrustSystem bcPGPTrustSystem;

    public DefaultTrustSystemAccessor(final File trustConfigFile) {
        File trustFile = new File(trustConfigFile, "pgp_trusted.txt");
        if (!trustFile.isFile()) {
            try {
                trustFile.createNewFile();
            } catch (IOException e) {
                // ignore
            }
        }
        bcPGPTrustSystem = new BCPGPTrustSystem(trustFile);
    }

    @Override
    public PGPTrustSystem getPGPTrustSystem() {
        return bcPGPTrustSystem;
    }

}
