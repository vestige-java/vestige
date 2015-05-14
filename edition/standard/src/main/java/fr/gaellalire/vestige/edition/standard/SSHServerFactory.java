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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.io.mina.MinaServiceFactoryFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.admin.ssh.DefaultPublickeyAuthenticator;
import fr.gaellalire.vestige.admin.ssh.RootedFileSystemFactory;
import fr.gaellalire.vestige.admin.ssh.SSHExecCommand;
import fr.gaellalire.vestige.admin.ssh.SSHShellCommandFactory;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.edition.standard.schema.Bind;
import fr.gaellalire.vestige.edition.standard.schema.SSH;
import fr.gaellalire.vestige.platform.VestigePlatform;

/**
 * @author Gael Lalire
 */
public class SSHServerFactory implements Callable<VestigeServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSHServerFactory.class);

    private File sshBase;

    private SSH ssh;

    private File appHomeFile;

    private ApplicationManager applicationManager;

    private VestigePlatform vestigePlatform;

    public SSHServerFactory(final File sshBase, final SSH ssh, final File appHomeFile, final ApplicationManager applicationManager, final VestigePlatform vestigePlatform) {
        this.sshBase = sshBase;
        this.ssh = ssh;
        this.appHomeFile = appHomeFile;
        this.applicationManager = applicationManager;
        this.vestigePlatform = vestigePlatform;
    }

    public VestigeServer call() throws Exception {
        final Bind bind = ssh.getBind();

        File privateKey = new File(sshBase, "vestige_rsa");
        if (!privateKey.exists()) {
            sshBase.mkdirs();
            IOUtils.copy(StandardEditionVestige.class.getResourceAsStream("vestige_rsa"), new FileOutputStream(privateKey));
        }
        LOGGER.info("Use {} for private SSH key file", privateKey);
        KeyPairProvider keyPairProvider = new FileKeyPairProvider(new String[] {privateKey.getPath()});
        final SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setFileSystemFactory(new RootedFileSystemFactory(appHomeFile, "vestige"));
        sshServer.setIoServiceFactoryFactory(new MinaServiceFactoryFactory());
        final String host = bind.getHost();
        sshServer.setHost(host);
        sshServer.setPort(bind.getPort());
        final VestigeCommandExecutor vestigeCommandExecutor = new VestigeCommandExecutor(applicationManager, vestigePlatform);
        sshServer.setCommandFactory(new ScpCommandFactory(new CommandFactory() {

            public Command createCommand(final String command) {
                return new SSHExecCommand(vestigeCommandExecutor, command);
            }
        }));

        sshServer.setSubsystemFactories(Collections.<NamedFactory<Command>> singletonList(new SftpSubsystem.Factory()));

        sshServer.setShellFactory(new SSHShellCommandFactory(vestigeCommandExecutor));
        sshServer.setKeyPairProvider(keyPairProvider);
        File authorizedKeysFile = new File(sshBase, "authorized_keys");
        if (!authorizedKeysFile.exists()) {
            sshBase.mkdirs();
            authorizedKeysFile = new File(sshBase, "authorized_keys");
            authorizedKeysFile.createNewFile();
        }
        LOGGER.info("Use {} for authorized public SSH keys file", authorizedKeysFile);
        sshServer.setPublickeyAuthenticator(new DefaultPublickeyAuthenticator(keyPairProvider, authorizedKeysFile));
        // 1 hour time out
        sshServer.getProperties().put(ServerFactoryManager.IDLE_TIMEOUT, "3600000");
        return new VestigeServer() {

            @Override
            public void stop() throws Exception {
                sshServer.stop();
                LOGGER.info("SSH interface stopped");
            }

            @Override
            public void start() throws Exception {
                sshServer.start();
                if (LOGGER.isInfoEnabled()) {
                    if (host == null) {
                        LOGGER.info("Listen on *:{} for SSH interface", bind.getPort());
                    } else {
                        LOGGER.info("Listen on {}:{} for SSH interface", host, bind.getPort());
                    }
                    LOGGER.info("SSH interface started");
                }
            }
        };
    }

}
