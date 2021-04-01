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

package fr.gaellalire.vestige.trust.secure;

import java.io.InputStream;
import java.io.OutputStream;

import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.spi.trust.PGPPublicPart;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class SecurePGPPublicPart implements PGPPublicPart {

    private VestigeSystem secureVestigeSystem;

    private PGPPublicPart delegate;

    public SecurePGPPublicPart(final VestigeSystem secureVestigeSystem, final PGPPublicPart delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.delegate = delegate;
    }

    @Override
    public void encrypt(final InputStream is, final OutputStream os) throws TrustException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.encrypt(is, os);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public boolean verify(final InputStream dataInputStream, final InputStream signatureInputStream) throws TrustException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.verify(dataInputStream, signatureInputStream);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public boolean isTrusted() throws TrustException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.isTrusted();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public String getFingerprint() {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.getFingerprint();
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

}
