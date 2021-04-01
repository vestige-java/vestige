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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;

import fr.gaellalire.vestige.spi.trust.PGPPrivatePart;
import fr.gaellalire.vestige.spi.trust.PGPSignature;
import fr.gaellalire.vestige.spi.trust.PGPTrustSystem;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class BCPGPTrustSystem implements PGPTrustSystem {

    public static final int BUFFER_SIZE = 1024;

    public static final File GPG_DIRECTORY = new File(System.getProperty("user.home"), ".gnupg");

    public static final File USER_PGP_CONF = new File(GPG_DIRECTORY, "gpg.conf");

    private PGPFingerprintValidator fingerprintValidator;

    public BCPGPTrustSystem(final File trustFile) {
        fingerprintValidator = new FilePGPFingerprintValidator(trustFile);
    }

    @Override
    public PGPPrivatePart getDefaultPrivatePart() throws TrustException {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(USER_PGP_CONF)));
            try {
                String readLine = bufferedReader.readLine();
                while (readLine != null) {
                    if (readLine.startsWith("default-key")) {
                        return getPrivatePart(readLine.substring("default-key".length()).trim());
                    }
                    readLine = bufferedReader.readLine();
                }
            } finally {
                bufferedReader.close();
            }
        } catch (IOException e) {
            throw new TrustException(e);
        }
        return null;
    }

    @Override
    public PGPSignature loadSignature(final InputStream inputStream) throws TrustException {
        try {
            PGPObjectFactory pgpFactory = new PGPObjectFactory(inputStream, new BcKeyFingerprintCalculator());

            // the first object might be a PGP marker packet
            Object o;
            o = pgpFactory.nextObject();
            if (!(o instanceof PGPSignatureList)) {
                o = pgpFactory.nextObject(); // nullable
            }
            org.bouncycastle.openpgp.PGPSignature signature = ((PGPSignatureList) o).get(0);

            return new BCPGPSignature(signature, Long.toHexString(signature.getKeyID()), fingerprintValidator);
        } catch (IOException e) {
            throw new TrustException(e);
        }
    }

    @Override
    public PGPPrivatePart getPrivatePart(final String pgpKey) throws TrustException {
        BCPGPPublicPart publicPart = getPublicPart(pgpKey);
        if (publicPart == null) {
            return null;
        }
        return BCPGPPrivatePart.findBCPGPPrivatePart(publicPart);
    }

    @Override
    public BCPGPPublicPart getPublicPart(final String pgpKey) throws TrustException {
        return BCPGPPublicPart.findBCPGPPublicPart(pgpKey, fingerprintValidator);
    }

}
