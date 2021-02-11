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
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import fr.gaellalire.vestige.spi.trust.PGPPublicPart;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class BCPGPPublicPart implements PGPPublicPart {

    private List<PGPPublicKey> signKeys;

    private List<PGPPublicKey> encryptKeys;

    private String fingerprint;

    public BCPGPPublicPart(final String fingerprint, final List<PGPPublicKey> signKeys, final List<PGPPublicKey> encryptKeys) {
        this.fingerprint = fingerprint;
        this.signKeys = signKeys;
        this.encryptKeys = encryptKeys;
    }

    public static void encrypt(final InputStream plainInput, final OutputStream encryptedOutput, final PGPPublicKey encKey) throws IOException, PGPException {

        BcPGPDataEncryptorBuilder encryptor = new BcPGPDataEncryptorBuilder(PGPEncryptedData.AES_256);
        encryptor.setWithIntegrityPacket(true);
        encryptor.setSecureRandom(new SecureRandom());

        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(encryptor);
        encGen.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(encKey));

        OutputStream encryptedOut = encGen.open(encryptedOutput, new byte[BCPGPTrustSystem.BUFFER_SIZE]);

        PGPCompressedDataGenerator compGen = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
        OutputStream compressedOut = compGen.open(encryptedOut, new byte[BCPGPTrustSystem.BUFFER_SIZE]);

        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        OutputStream literalOut = literalGen.open(compressedOut, PGPLiteralData.BINARY, PGPLiteralData.CONSOLE, new Date(), new byte[BCPGPTrustSystem.BUFFER_SIZE]);

        byte[] buf = new byte[BCPGPTrustSystem.BUFFER_SIZE];
        int len;
        while ((len = plainInput.read(buf)) > 0) {
            literalOut.write(buf, 0, len);
        }

        literalGen.close();

        compGen.close();
        encGen.close();
    }

    @Override
    public void encrypt(final InputStream is, final OutputStream os) throws TrustException {
        try {
            encrypt(is, os, encryptKeys.get(0));
        } catch (IOException e) {
            throw new TrustException(e);
        } catch (PGPException e) {
            throw new TrustException(e);
        }
    }

    @Override
    public boolean verify(final InputStream dataInputStream, final InputStream signatureInputStream) throws TrustException {
        try {
            PGPObjectFactory pgpFactory = new PGPObjectFactory(signatureInputStream, new BcKeyFingerprintCalculator());

            Object o;
            o = pgpFactory.nextObject();
            if (!(o instanceof PGPSignatureList)) {
                o = pgpFactory.nextObject();
            }
            PGPSignature signature = ((PGPSignatureList) o).get(0);
            signature.init(new BcPGPContentVerifierBuilderProvider(), signKeys.get(0));

            byte[] buf = new byte[BCPGPTrustSystem.BUFFER_SIZE];
            int len;
            while ((len = dataInputStream.read(buf)) > 0) {
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

    public List<PGPPublicKey> getEncryptKeys() {
        return encryptKeys;
    }

    public List<PGPPublicKey> getSignKeys() {
        return signKeys;
    }

    @Override
    public String getFingerprint() {
        return fingerprint;
    }

}
