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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.gpg.keybox.KeyBlob;
import org.bouncycastle.gpg.keybox.KeyInformation;
import org.bouncycastle.gpg.keybox.PublicKeyRingBlob;
import org.bouncycastle.gpg.keybox.bc.BcKeyBox;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPKeyFlags;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.util.encoders.Hex;

import fr.gaellalire.vestige.spi.trust.PGPPrivatePart;
import fr.gaellalire.vestige.spi.trust.PGPSignature;
import fr.gaellalire.vestige.spi.trust.PGPTrustSystem;
import fr.gaellalire.vestige.spi.trust.PrivatePart;
import fr.gaellalire.vestige.spi.trust.PublicPart;
import fr.gaellalire.vestige.spi.trust.TrustException;

/**
 * @author Gael Lalire
 */
public class BCPGPTrustSystem implements PGPTrustSystem {

    public static final int BUFFER_SIZE = 1024;

    public static final File GPG_DIRECTORY = new File(System.getProperty("user.home"), ".gnupg");

    public static final File USER_KEYBOX_PATH = new File(GPG_DIRECTORY, "pubring.kbx");

    public static final File USER_PGP_PUBRING_FILE = new File(GPG_DIRECTORY, "pubring.gpg");

    public static final File USER_PGP_CONF = new File(GPG_DIRECTORY, "gpg.conf");

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
            BCPGPPublicPart publicPart = getPublicPart(Long.toHexString(signature.getKeyID()));

            return new BCPGPSignature(signature, publicPart);
        } catch (IOException e) {
            throw new TrustException(e);
        }
    }

    @Override
    public PGPPrivatePart getPrivatePart(final String pgpKey) throws TrustException {
        BCPGPPublicPart publicPart = getPublicPart(pgpKey);
        return new BCPGPPrivatePart(publicPart);
    }

    private static int getKeyFlags(final PGPPublicKey key) {
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

    @Override
    public BCPGPPublicPart getPublicPart(final String pgpKey) throws TrustException {
        String pgpUpperCase = pgpKey.toUpperCase();
        BcKeyBox bcKeyBox;
        try {
            bcKeyBox = new BcKeyBox(new FileInputStream(USER_KEYBOX_PATH));
        } catch (IOException e) {
            throw new TrustException(e);
        }
        List<KeyBlob> keyBlobs = bcKeyBox.getKeyBlobs();
        for (KeyBlob keyBlob : keyBlobs) {
            for (KeyInformation keyInfo : keyBlob.getKeyInformation()) {
                String fingerprint = Hex.toHexString(keyInfo.getFingerprint()).toUpperCase();
                if (fingerprint.endsWith(pgpUpperCase)) {
                    try {
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
                        return new BCPGPPublicPart(masterFingerprint, signKeys, encryptKeys);
                    } catch (IOException e) {
                        throw new TrustException(e);
                    }
                }
            }
        }
        return null;
    }

    public static void main(final String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        BCPGPTrustSystem bcpgpTrustSystem = new BCPGPTrustSystem();
        // PublicPart publicKey = bcpgpTrustSystem.getPublicPart("1A33B3EA2279CD97B9C10C42C7906F4CB7CC789F");
        // System.out.println(publicKey);

        PrivatePart privatePart = bcpgpTrustSystem.getPrivatePart("1A33B3EA2279CD97B9C10C42C7906F4CB7CC789F");
        System.out.println(privatePart);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PublicPart publicPart = privatePart.getPublicPart();
        byte[] coucouBytes = new String("coucoAuB").getBytes();
        publicPart.encrypt(new ByteArrayInputStream(coucouBytes), os);
        byte[] byteArray = os.toByteArray();
        System.out.println(Arrays.toString(byteArray));

        privatePart.decrypt(new ByteArrayInputStream(byteArray), System.out);

        System.out.flush();

        System.out.println("SIGN");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        privatePart.sign(new ByteArrayInputStream(coucouBytes), byteArrayOutputStream);

        byteArray = byteArrayOutputStream.toByteArray();
        System.out.println(Arrays.toString(byteArray));

        System.out.println(publicPart.verify(new ByteArrayInputStream(coucouBytes), new ByteArrayInputStream(byteArray)));

        System.out.println(publicPart.verify(new FileInputStream("/Users/gaellalire/gpgtest/h.txt"), new FileInputStream("/Users/gaellalire/gpgtest/h.txt.sig")));

        privatePart.decrypt(new FileInputStream("/Users/gaellalire/gpgtest/h.txt.gpg"), System.out);
        System.out.flush();

        PGPSignature loadSignature = bcpgpTrustSystem.loadSignature(new FileInputStream("/Users/gaellalire/gpgtest/h.txt.sig"));
        System.out.println(loadSignature.verify(new FileInputStream("/Users/gaellalire/gpgtest/h.txt")));
        System.out.println(loadSignature.getPublicKey().getFingerprint());

        //
        // publicKey.decrypt();

        // readPublicKey(new FileInputStream("/Users/gaellalire/.gnupg/pubring.kbx"));

    }
}
