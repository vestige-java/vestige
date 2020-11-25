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

package fr.gaellalire.vestige.edition.standard;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.forward.TcpipServerChannel;
import org.apache.sshd.common.io.mina.MinaServiceFactoryFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.admin.ssh.DefaultPublickeyAuthenticator;
import fr.gaellalire.vestige.admin.ssh.FixedSSHServer;
import fr.gaellalire.vestige.admin.ssh.RootedFileSystemFactory;
import fr.gaellalire.vestige.admin.ssh.SSHExecCommand;
import fr.gaellalire.vestige.admin.ssh.SSHShellCommandFactory;
import fr.gaellalire.vestige.admin.ssh.VestigeChannelSession;
import fr.gaellalire.vestige.edition.standard.schema.Bind;
import fr.gaellalire.vestige.edition.standard.schema.SSH;
import fr.gaellalire.vestige.utils.SimpleValueGetter;

/**
 * @author Gael Lalire
 */
public class SSHServerFactory implements Callable<VestigeServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSHServerFactory.class);

    private File sshConfig;

    private File sshData;

    private SSH ssh;

    private File appHomeFile;

    private VestigeCommandExecutor vestigeCommandExecutor;

    private File commandHistory;

    public SSHServerFactory(final File sshConfig, final File sshData, final File commandHistory, final SSH ssh, final File appHomeFile,
            final VestigeCommandExecutor vestigeCommandExecutor) {
        this.sshConfig = sshConfig;
        this.sshData = sshData;
        this.commandHistory = commandHistory;
        this.ssh = ssh;
        this.appHomeFile = appHomeFile;
        this.vestigeCommandExecutor = vestigeCommandExecutor;
    }

    private KeyPair generateKeyPair() throws Exception {
        RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();
        BigInteger publicExponent = new BigInteger("10001", 16);
        keyGen.init(new RSAKeyGenerationParameters(publicExponent, SecureRandom.getInstance("DEFAULT", BouncyCastleProvider.PROVIDER_NAME), 2048, 80));
        AsymmetricCipherKeyPair keys = keyGen.generateKeyPair();

        RSAPrivateCrtKeyParameters rsaKeyParameters = (RSAPrivateCrtKeyParameters) keys.getPrivate();
        RSAPrivateKeySpec privateSpec = new RSAPrivateCrtKeySpec(rsaKeyParameters.getModulus(), publicExponent, rsaKeyParameters.getExponent(), rsaKeyParameters.getP(),
                rsaKeyParameters.getQ(), rsaKeyParameters.getDP(), rsaKeyParameters.getDQ(), rsaKeyParameters.getQInv());
        KeyFactory factory = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);

        PrivateKey privateKey = factory.generatePrivate(privateSpec);
        PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keys.getPublic()).getEncoded()));

        return new KeyPair(publicKey, privateKey);
    }

    @SuppressWarnings("unchecked")
    public VestigeServer call() throws Exception {
        final Bind bind = ssh.getBind();
        sshConfig.mkdirs();
        sshData.mkdirs();

        File privateKey = new File(sshConfig, "server_rsa");
        if (!privateKey.exists()) {
            PemWriter pw = new PemWriter(new FileWriter(privateKey));
            privateKey.setReadable(false, false);
            privateKey.setWritable(false, false);
            privateKey.setReadable(true, true);
            privateKey.setWritable(true, true);
            privateKey.setExecutable(false, false);
            KeyPair keyPair = generateKeyPair();
            try {
                pw.writeObject(new JcaMiscPEMGenerator(keyPair.getPrivate()));
            } finally {
                pw.close();
            }

            File publicKey = new File(sshData, "known_hosts");

            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
            ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteOs);
            dos.writeInt("ssh-rsa".getBytes().length);
            dos.write("ssh-rsa".getBytes());
            dos.writeInt(rsaPublicKey.getPublicExponent().toByteArray().length);
            dos.write(rsaPublicKey.getPublicExponent().toByteArray());
            dos.writeInt(rsaPublicKey.getModulus().toByteArray().length);
            dos.write(rsaPublicKey.getModulus().toByteArray());
            String enc = Base64.toBase64String(byteOs.toByteArray());
            FileWriter fileWriter = new FileWriter(publicKey);
            publicKey.setReadable(false, false);
            publicKey.setWritable(false, false);
            publicKey.setReadable(true, true);
            publicKey.setWritable(true, true);
            publicKey.setExecutable(false, false);

            try {
                fileWriter.write("* ssh-rsa " + enc);
            } finally {
                fileWriter.close();
            }
        }
        LOGGER.info("Use {} for private SSH key file", privateKey);

        File authorizedKeysFile = new File(sshConfig, "authorized_keys");
        if (!authorizedKeysFile.exists()) {
            File clientPrivateKey = new File(sshData, "client_rsa");
            PemWriter pw = new PemWriter(new FileWriter(clientPrivateKey));
            clientPrivateKey.setReadable(false, false);
            clientPrivateKey.setWritable(false, false);
            clientPrivateKey.setReadable(true, true);
            clientPrivateKey.setWritable(true, true);
            clientPrivateKey.setExecutable(false, false);
            KeyPair keyPair = generateKeyPair();
            try {
                pw.writeObject(new JcaMiscPEMGenerator(keyPair.getPrivate()));
            } finally {
                pw.close();
            }

            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
            ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteOs);
            dos.writeInt("ssh-rsa".getBytes().length);
            dos.write("ssh-rsa".getBytes());
            dos.writeInt(rsaPublicKey.getPublicExponent().toByteArray().length);
            dos.write(rsaPublicKey.getPublicExponent().toByteArray());
            dos.writeInt(rsaPublicKey.getModulus().toByteArray().length);
            dos.write(rsaPublicKey.getModulus().toByteArray());
            String enc = Base64.toBase64String(byteOs.toByteArray());
            FileWriter fileWriter = new FileWriter(authorizedKeysFile);
            authorizedKeysFile.setReadable(false, false);
            authorizedKeysFile.setWritable(false, false);
            authorizedKeysFile.setReadable(true, true);
            authorizedKeysFile.setWritable(true, true);
            authorizedKeysFile.setExecutable(false, false);
            try {
                fileWriter.write("ssh-rsa " + enc + " vestige");
            } finally {
                fileWriter.close();
            }
        }
        LOGGER.info("Use {} for authorized public SSH keys file", authorizedKeysFile);

        KeyPairProvider keyPairProvider = new FileKeyPairProvider(new String[] {privateKey.getPath()});
        final SshServer sshServer = FixedSSHServer.setUpDefaultServer();
        sshServer.setChannelFactories(Arrays.<NamedFactory<Channel>> asList(new ChannelSession.Factory() {
            @Override
            public Channel create() {
                return new VestigeChannelSession();
            }
        }, new TcpipServerChannel.DirectTcpipFactory()));

        sshServer.setFileSystemFactory(new RootedFileSystemFactory(appHomeFile, "vestige"));
        sshServer.setIoServiceFactoryFactory(new MinaServiceFactoryFactory());
        final String host = SimpleValueGetter.INSTANCE.getValue(bind.getHost());
        sshServer.setHost(host);
        sshServer.setPort(SimpleValueGetter.INSTANCE.getValue(bind.getPort()));
        sshServer.setCommandFactory(new ScpCommandFactory(new CommandFactory() {

            public Command createCommand(final String command) {
                return new SSHExecCommand(vestigeCommandExecutor, command);
            }
        }));

        sshServer.setSubsystemFactories(Collections.<NamedFactory<Command>> singletonList(new SftpSubsystem.Factory()));

        sshServer.setShellFactory(new SSHShellCommandFactory(vestigeCommandExecutor, commandHistory));
        sshServer.setKeyPairProvider(keyPairProvider);
        sshServer.setPublickeyAuthenticator(new DefaultPublickeyAuthenticator(keyPairProvider, authorizedKeysFile));
        // 1 hour time out
        sshServer.getProperties().put(ServerFactoryManager.IDLE_TIMEOUT, "3600000");
        return new VestigeServer() {

            @Override
            public void stop() throws Exception {
                sshServer.stop();
                File portFile = new File(sshData, "port.txt");
                portFile.delete();
                LOGGER.info("SSH interface stopped");
            }

            @Override
            public void start() throws Exception {
                sshServer.start();
                File portFile = new File(sshData, "port.txt");
                FileWriter fileWriter = new FileWriter(portFile);
                int port = sshServer.getPort();
                try {
                    fileWriter.write(String.valueOf(port));
                } finally {
                    fileWriter.close();
                }
                if (LOGGER.isInfoEnabled()) {
                    if (host == null) {
                        LOGGER.info("Listen on *:{} for SSH interface", port);
                    } else {
                        LOGGER.info("Listen on {}:{} for SSH interface", host, port);
                    }
                    LOGGER.info("SSH interface started");
                }
            }

            @Override
            public String getLocalHost() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }
        };
    }

}
