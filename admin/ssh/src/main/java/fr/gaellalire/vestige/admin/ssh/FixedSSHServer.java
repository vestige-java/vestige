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

package fr.gaellalire.vestige.admin.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.Cipher;
import org.apache.sshd.common.Compression;
import org.apache.sshd.common.KeyExchange;
import org.apache.sshd.common.Mac;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.RequestHandler;
import org.apache.sshd.common.Signature;
import org.apache.sshd.common.cipher.AES128CBC;
import org.apache.sshd.common.cipher.AES128CTR;
import org.apache.sshd.common.cipher.AES192CBC;
import org.apache.sshd.common.cipher.AES256CBC;
import org.apache.sshd.common.cipher.AES256CTR;
import org.apache.sshd.common.cipher.ARCFOUR128;
import org.apache.sshd.common.cipher.ARCFOUR256;
import org.apache.sshd.common.cipher.BlowfishCBC;
import org.apache.sshd.common.cipher.TripleDESCBC;
import org.apache.sshd.common.compression.CompressionNone;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.forward.DefaultTcpipForwarderFactory;
import org.apache.sshd.common.forward.TcpipServerChannel;
import org.apache.sshd.common.mac.HMACMD5;
import org.apache.sshd.common.mac.HMACMD596;
import org.apache.sshd.common.mac.HMACSHA1;
import org.apache.sshd.common.mac.HMACSHA196;
import org.apache.sshd.common.mac.HMACSHA256;
import org.apache.sshd.common.mac.HMACSHA512;
import org.apache.sshd.common.random.BouncyCastleRandom;
import org.apache.sshd.common.random.JceRandom;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.signature.SignatureDSA;
import org.apache.sshd.common.signature.SignatureECDSA;
import org.apache.sshd.common.signature.SignatureRSA;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.global.CancelTcpipForwardHandler;
import org.apache.sshd.server.global.KeepAliveHandler;
import org.apache.sshd.server.global.NoMoreSessionsHandler;
import org.apache.sshd.server.global.TcpipForwardHandler;
import org.apache.sshd.server.kex.DHG1;
import org.apache.sshd.server.kex.DHG14;
import org.apache.sshd.server.kex.DHGEX;
import org.apache.sshd.server.kex.DHGEX256;
import org.apache.sshd.server.kex.ECDHP256;
import org.apache.sshd.server.kex.ECDHP384;
import org.apache.sshd.server.kex.ECDHP521;

/**
 * @author Gael Lalire
 */
public class FixedSSHServer extends SshServer {

    @Override
    public void start() throws IOException {
        super.start();
        if (port == 0) {
            port = ((InetSocketAddress) acceptor.getBoundAddresses().iterator().next()).getPort();
        }
    }

    private static void setUpDefaultCiphers(final SshServer sshd) {
        List<NamedFactory<Cipher>> avail = new LinkedList<NamedFactory<Cipher>>();
        avail.add(new AES128CTR.Factory());
        avail.add(new AES256CTR.Factory());
        avail.add(new ARCFOUR128.Factory());
        avail.add(new ARCFOUR256.Factory());
        avail.add(new AES128CBC.Factory());
        avail.add(new TripleDESCBC.Factory());
        avail.add(new BlowfishCBC.Factory());
        avail.add(new AES192CBC.Factory());
        avail.add(new AES256CBC.Factory());

        for (Iterator<NamedFactory<Cipher>> i = avail.iterator(); i.hasNext();) {
            final NamedFactory<Cipher> f = i.next();
            try {
                final Cipher c = f.create();
                final byte[] key = new byte[c.getBlockSize()];
                final byte[] iv = new byte[c.getIVSize()];
                c.init(Cipher.Mode.Encrypt, key, iv);
            } catch (InvalidKeyException e) {
                i.remove();
            } catch (Exception e) {
                i.remove();
            }
        }
        sshd.setCipherFactories(avail);
    }

    public static SshServer setUpDefaultServer() {
        SshServer sshd = new FixedSSHServer();
        // DHG14 uses 2048 bits key which are not supported by the default JCE provider
        // EC keys are not supported until OpenJDK 8
        if (SecurityUtils.isBouncyCastleRegistered()) {
            sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>> asList(new DHGEX256.Factory(), new DHGEX.Factory(), new ECDHP256.Factory(), new ECDHP384.Factory(),
                    new ECDHP521.Factory(), new DHG14.Factory(), new DHG1.Factory()));
            sshd.setSignatureFactories(Arrays.<NamedFactory<Signature>> asList(new SignatureECDSA.NISTP256Factory(), new SignatureECDSA.NISTP384Factory(),
                    new SignatureECDSA.NISTP521Factory(), new SignatureDSA.Factory(), new SignatureRSA.Factory()));
            sshd.setRandomFactory(new SingletonRandomFactory(new BouncyCastleRandom.Factory()));
            // EC keys are not supported until OpenJDK 7
        } else if (SecurityUtils.hasEcc()) {
            sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>> asList(new DHGEX256.Factory(), new DHGEX.Factory(), new ECDHP256.Factory(), new ECDHP384.Factory(),
                    new ECDHP521.Factory(), new DHG1.Factory()));
            sshd.setSignatureFactories(Arrays.<NamedFactory<Signature>> asList(new SignatureECDSA.NISTP256Factory(), new SignatureECDSA.NISTP384Factory(),
                    new SignatureECDSA.NISTP521Factory(), new SignatureDSA.Factory(), new SignatureRSA.Factory()));
            sshd.setRandomFactory(new SingletonRandomFactory(new JceRandom.Factory()));
        } else {
            sshd.setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>> asList(new DHGEX256.Factory(), new DHGEX.Factory(), new DHG1.Factory()));
            sshd.setSignatureFactories(Arrays.<NamedFactory<Signature>> asList(new SignatureDSA.Factory(), new SignatureRSA.Factory()));
            sshd.setRandomFactory(new SingletonRandomFactory(new JceRandom.Factory()));
        }
        setUpDefaultCiphers(sshd);
        // Compression is not enabled by default
        // sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>>asList(
        // new CompressionNone.Factory(),
        // new CompressionZlib.Factory(),
        // new CompressionDelayedZlib.Factory()));
        sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>> asList(new CompressionNone.Factory()));
        sshd.setMacFactories(Arrays.<NamedFactory<Mac>> asList(new HMACSHA256.Factory(), new HMACSHA512.Factory(), new HMACSHA1.Factory(), new HMACMD5.Factory(),
                new HMACSHA196.Factory(), new HMACMD596.Factory()));
        sshd.setChannelFactories(Arrays.<NamedFactory<Channel>> asList(new ChannelSession.Factory(), new TcpipServerChannel.DirectTcpipFactory()));
        sshd.setFileSystemFactory(new NativeFileSystemFactory());
        sshd.setTcpipForwarderFactory(new DefaultTcpipForwarderFactory());
        sshd.setGlobalRequestHandlers(
                Arrays.<RequestHandler<ConnectionService>> asList(new KeepAliveHandler(), new NoMoreSessionsHandler(), new TcpipForwardHandler(), new CancelTcpipForwardHandler()));
        return sshd;
    }

}
