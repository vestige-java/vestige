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

import fr.gaellalire.vestige.spi.trust.PGPSignature;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class BCPGPSignature implements PGPSignature {

    private org.bouncycastle.openpgp.PGPSignature signature;

    private String publicFingerprint;

    private BCPGPPublicPart pgpPublicPart;

    private PGPFingerprintValidator fingerprintValidator;

    public BCPGPSignature(final org.bouncycastle.openpgp.PGPSignature signature, final BCPGPPublicPart pgpPublicPart, final PGPFingerprintValidator fingerprintValidator) {
        this.signature = signature;
        this.pgpPublicPart = pgpPublicPart;
        publicFingerprint = pgpPublicPart.getFingerprint();
        this.fingerprintValidator = fingerprintValidator;
    }

    public BCPGPSignature(final org.bouncycastle.openpgp.PGPSignature signature, final String publicFingerprint, final PGPFingerprintValidator fingerprintValidator) {
        this.signature = signature;
        this.publicFingerprint = publicFingerprint;
        this.fingerprintValidator = fingerprintValidator;
    }

    @Override
    public boolean verify(final InputStream inputStream) throws TrustException {
        try {
            BCPGPPublicPart pgpPublicPart = getPublicPart();
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
    public BCPGPPublicPart getPublicPart() throws TrustException {
        if (pgpPublicPart != null) {
            return pgpPublicPart;
        }
        pgpPublicPart = BCPGPPublicPart.findBCPGPPublicPart(publicFingerprint, fingerprintValidator);
        if (pgpPublicPart == null) {
            throw new TrustException("Public part not found for " + publicFingerprint);
        }
        return pgpPublicPart;
    }

}
