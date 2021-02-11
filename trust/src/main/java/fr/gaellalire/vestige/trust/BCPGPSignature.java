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

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;

import fr.gaellalire.vestige.spi.trust.PGPPublicPart;
import fr.gaellalire.vestige.spi.trust.PGPSignature;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class BCPGPSignature implements PGPSignature {

    private org.bouncycastle.openpgp.PGPSignature signature;

    private BCPGPPublicPart pgpPublicPart;

    public BCPGPSignature(final org.bouncycastle.openpgp.PGPSignature signature, final BCPGPPublicPart pgpPublicPart) {
        this.signature = signature;
        this.pgpPublicPart = pgpPublicPart;
    }

    @Override
    public boolean verify(final InputStream inputStream) throws TrustException {
        try {
            signature.init(new BcPGPContentVerifierBuilderProvider(), pgpPublicPart.getSignKeys().get(0));

            byte[] buf = new byte[BCPGPTrustSystem.BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                signature.update(buf, 0, len);
            }
            int hashAlgorithm = signature.getHashAlgorithm();
            if (hashAlgorithm != HashAlgorithmTags.SHA512) {
                return false;
            }
            return signature.verify();
        } catch (PGPException e) {
            throw new TrustException(e);
        } catch (IOException e) {
            throw new TrustException(e);
        }
    }

    @Override
    public PGPPublicPart getPublicKey() {
        return pgpPublicPart;
    }

}
