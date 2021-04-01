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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.gpg.keybox.KeyBlob;
import org.bouncycastle.gpg.keybox.KeyInformation;
import org.bouncycastle.gpg.keybox.PublicKeyRingBlob;
import org.bouncycastle.gpg.keybox.bc.BcKeyBox;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyFlags;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.encoders.Hex;

import fr.gaellalire.vestige.spi.trust.PGPPublicPart;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class BCPGPPublicPart implements PGPPublicPart {

    public static final File USER_KEYBOX_PATH = new File(BCPGPTrustSystem.GPG_DIRECTORY, "pubring.kbx");

    public static final File USER_PGP_PUBRING_FILE = new File(BCPGPTrustSystem.GPG_DIRECTORY, "pubring.gpg");

    private List<PGPPublicKey> signKeys;

    private List<PGPPublicKey> encryptKeys;

    private String fingerprint;

    private PGPFingerprintValidator fingerprintValidator;

    public static int getKeyFlags(final PGPPublicKey key) {
        @SuppressWarnings("unchecked")
        Iterator<org.bouncycastle.openpgp.PGPSignature> sigs = key.getSignatures();
        while (sigs.hasNext()) {
            org.bouncycastle.openpgp.PGPSignature sig = sigs.next();
            PGPSignatureSubpacketVector subpackets = sig.getHashedSubPackets();
            if (subpackets != null) {
                return subpackets.getKeyFlags();
            }
        }
        return 0;
    }

    public static BCPGPPublicPart findBCPGPPublicPart(final String pgpKey, final PGPFingerprintValidator fingerprintValidator) throws TrustException {
        String pgpUpperCase = pgpKey.toUpperCase();
        BcKeyBox bcKeyBox;
        try {
            if (USER_KEYBOX_PATH.isFile()) {
                FileInputStream input = new FileInputStream(USER_KEYBOX_PATH);
                try {
                    bcKeyBox = new BcKeyBox(input);
                    List<KeyBlob> keyBlobs = bcKeyBox.getKeyBlobs();
                    for (KeyBlob keyBlob : keyBlobs) {
                        for (KeyInformation keyInfo : keyBlob.getKeyInformation()) {
                            String fingerprint = Hex.toHexString(keyInfo.getFingerprint()).toUpperCase();
                            if (fingerprint.endsWith(pgpUpperCase)) {
                                List<PGPPublicKey> signKeys = new ArrayList<PGPPublicKey>();
                                List<PGPPublicKey> encryptKeys = new ArrayList<PGPPublicKey>();

                                String masterFingerprint = null;

                                PGPPublicKeyRing pgpPublicKeyRing = ((PublicKeyRingBlob) keyBlob).getPGPPublicKeyRing();
                                Iterator<PGPPublicKey> publicKeys = pgpPublicKeyRing.getPublicKeys();
                                while (publicKeys.hasNext()) {
                                    PGPPublicKey publicKey = publicKeys.next();

                                    int keyFlags = getKeyFlags(publicKey);
                                    if ((keyFlags & PGPKeyFlags.CAN_SIGN) != 0) {
                                        signKeys.add(publicKey);
                                    }
                                    if ((keyFlags & PGPKeyFlags.CAN_ENCRYPT_COMMS) != 0 || (keyFlags & PGPKeyFlags.CAN_ENCRYPT_STORAGE) != 0) {
                                        encryptKeys.add(publicKey);
                                    }
                                    if (publicKey.isMasterKey()) {
                                        masterFingerprint = Hex.toHexString(publicKey.getFingerprint()).toUpperCase();
                                    }
                                }
                                return new BCPGPPublicPart(masterFingerprint, signKeys, encryptKeys, fingerprintValidator);
                            }
                        }
                    }
                } finally {
                    input.close();
                }
            }
            if (USER_PGP_PUBRING_FILE.isFile()) {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(USER_PGP_PUBRING_FILE));
                try {
                    PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, new BcKeyFingerprintCalculator());

                    Iterator<PGPPublicKeyRing> keyrings = pgpPub.getKeyRings();
                    while (keyrings.hasNext()) {
                        PGPPublicKeyRing keyRing = keyrings.next();
                        List<PGPPublicKey> signKeys = new ArrayList<PGPPublicKey>();
                        List<PGPPublicKey> encryptKeys = new ArrayList<PGPPublicKey>();

                        boolean fingerprintMatched = false;
                        String masterFingerprint = null;

                        Iterator<PGPPublicKey> keys = keyRing.getPublicKeys();
                        while (keys.hasNext()) {
                            PGPPublicKey key = keys.next();
                            String fingerprint = Hex.toHexString(key.getFingerprint()).toUpperCase();
                            if (fingerprint.endsWith(pgpUpperCase)) {
                                fingerprintMatched = true;
                            }
                            int keyFlags = getKeyFlags(key);
                            if ((keyFlags & PGPKeyFlags.CAN_SIGN) != 0) {
                                signKeys.add(key);
                            }
                            if ((keyFlags & PGPKeyFlags.CAN_ENCRYPT_COMMS) != 0 || (keyFlags & PGPKeyFlags.CAN_ENCRYPT_STORAGE) != 0) {
                                encryptKeys.add(key);
                            }
                            if (key.isMasterKey()) {
                                masterFingerprint = Hex.toHexString(key.getFingerprint()).toUpperCase();
                            }
                        }
                        if (fingerprintMatched) {
                            return new BCPGPPublicPart(masterFingerprint, signKeys, encryptKeys, fingerprintValidator);
                        }
                    }
                } finally {
                    in.close();
                }
            }
        } catch (PGPException e) {
            throw new TrustException(e);
        } catch (IOException e) {
            throw new TrustException(e);
        }

        return null;
    }

    public BCPGPPublicPart(final String fingerprint, final List<PGPPublicKey> signKeys, final List<PGPPublicKey> encryptKeys, final PGPFingerprintValidator fingerprintValidator) {
        this.fingerprint = fingerprint;
        this.signKeys = signKeys;
        this.encryptKeys = encryptKeys;
        this.fingerprintValidator = fingerprintValidator;
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

            return new BCPGPSignature(signature, this, fingerprintValidator).verify(dataInputStream);
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

    @Override
    public boolean isTrusted() throws TrustException {
        return fingerprintValidator.validate(fingerprint);
    }

}
