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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.gpg.SExprParser;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.spi.trust.PGPPrivatePart;
import fr.gaellalire.vestige.spi.trust.PublicPart;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class BCPGPPrivatePart implements PGPPrivatePart {

    private static final Logger LOGGER = LoggerFactory.getLogger(BCPGPPrivatePart.class);

    public static final File USER_PGP_LEGACY_SECRING_FILE = new File(BCPGPTrustSystem.GPG_DIRECTORY, "secring.gpg");

    public static final File USER_SECRET_KEY_DIR = new File(BCPGPTrustSystem.GPG_DIRECTORY, "private-keys-v1.d");

    private BCPGPPublicPart bcpgpPublicPart;

    private List<PGPPrivateKey> encryptPrivateKeys = new ArrayList<PGPPrivateKey>();

    private List<PGPPrivateKey> signPrivateKeys = new ArrayList<PGPPrivateKey>();

    public BCPGPPrivatePart(final BCPGPPublicPart bcpgpPublicPart) throws TrustException {
        this.bcpgpPublicPart = bcpgpPublicPart;

        if (USER_SECRET_KEY_DIR.isDirectory()) {

            try {
                for (PGPPublicKey pgpPublicKey : bcpgpPublicPart.getEncryptKeys()) {
                    PGPSecretKey parseSecretKey = new SExprParser(new BcPGPDigestCalculatorProvider()).parseSecretKey(
                            new BufferedInputStream(new FileInputStream(new File(USER_SECRET_KEY_DIR, TrustUtils.bytesToHex(KeyGrip.getKeyGrip(pgpPublicKey)).concat(".key")))),
                            null, pgpPublicKey);
                    encryptPrivateKeys.add(parseSecretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(null)));
                }
                for (PGPPublicKey pgpPublicKey : bcpgpPublicPart.getSignKeys()) {
                    PGPSecretKey parseSecretKey = new SExprParser(new BcPGPDigestCalculatorProvider()).parseSecretKey(
                            new BufferedInputStream(new FileInputStream(new File(USER_SECRET_KEY_DIR, TrustUtils.bytesToHex(KeyGrip.getKeyGrip(pgpPublicKey)).concat(".key")))),
                            null, pgpPublicKey);
                    signPrivateKeys.add(parseSecretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(null)));
                }
            } catch (Exception e) {
                throw new TrustException(e);
            }

        }
    }

    @Override
    public PublicPart getPublicPart() {
        return bcpgpPublicPart;
    }

    private static void decryptAndVerify(final InputStream encryptedInput, final OutputStream plainOutput, final PGPPrivateKey myKey) throws PGPException, IOException {
        // note: the signature is inside the encrypted data

        PGPObjectFactory pgpFactory = new PGPObjectFactory(encryptedInput, new BcKeyFingerprintCalculator());

        // the first object might be a PGP marker packet
        Object o = pgpFactory.nextObject(); // nullable
        if (!(o instanceof PGPEncryptedDataList)) {
            o = pgpFactory.nextObject(); // nullable
        }

        if (!(o instanceof PGPEncryptedDataList)) {
            LOGGER.warn("can't find encrypted data list in data");
            return;
        }
        PGPEncryptedDataList encDataList = (PGPEncryptedDataList) o;

        // check if secret key matches our encryption keyID
        Iterator<?> it = encDataList.getEncryptedDataObjects();
        PGPPrivateKey sKey = null;
        PGPPublicKeyEncryptedData pbe = null;
        long myKeyID = myKey.getKeyID();
        while (sKey == null && it.hasNext()) {
            Object i = it.next();
            if (!(i instanceof PGPPublicKeyEncryptedData)) {
                continue;
            }
            pbe = (PGPPublicKeyEncryptedData) i;
            if (pbe.getKeyID() == myKeyID) {
                sKey = myKey;
            }
        }
        if (sKey == null) {
            LOGGER.warn("private key for message not found");
            return;
        }

        InputStream clear = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(sKey));

        PGPObjectFactory plainFactory = new PGPObjectFactory(clear, new BcKeyFingerprintCalculator());

        Object object = plainFactory.nextObject(); // nullable
        if (object instanceof PGPCompressedData) {
            PGPCompressedData cData = (PGPCompressedData) object;
            plainFactory = new PGPObjectFactory(cData.getDataStream(), new BcKeyFingerprintCalculator());
            object = plainFactory.nextObject(); // nullable
        }

        // the first object could be the signature list
        // get signature from it

        if (!(object instanceof PGPLiteralData)) {
            LOGGER.warn("unknown packet type: " + object.getClass().getName());
            return;
        }

        PGPLiteralData ld = (PGPLiteralData) object;
        InputStream unc = ld.getInputStream();
        int ch;
        while ((ch = unc.read()) >= 0) {
            plainOutput.write(ch);
        }
    }

    @Override
    public void decrypt(final InputStream is, final OutputStream os) throws TrustException {
        try {
            decryptAndVerify(is, os, encryptPrivateKeys.get(0));
        } catch (PGPException e) {
            throw new TrustException(e);
        } catch (IOException e) {
            throw new TrustException(e);
        }
    }

    @Override
    public void sign(final InputStream is, final OutputStream os) throws TrustException {
        PGPPrivateKey pgpPrivateKey = signPrivateKeys.get(0);

        try {
            PGPSignatureGenerator sigGen = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(pgpPrivateKey.getPublicKeyPacket().getAlgorithm(), HashAlgorithmTags.SHA512));
            sigGen.init(PGPSignature.BINARY_DOCUMENT, pgpPrivateKey);

            // PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            // spGen.setSignerUserID(false, pgpPrivateKey.getPublicKeyPacket().getUserId());
            // sigGen.setUnhashedSubpackets(spGen.generate());

            byte[] buf = new byte[BCPGPTrustSystem.BUFFER_SIZE];
            int len;
            while ((len = is.read(buf)) > 0) {
                sigGen.update(buf, 0, len);
            }

            PGPSignature pgpSignature = sigGen.generate();
            pgpSignature.encode(os);

        } catch (PGPException e) {
            throw new TrustException(e);
        } catch (IOException e) {
            throw new TrustException(e);
        }

    }

}
