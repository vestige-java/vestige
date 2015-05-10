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

package com.googlecode.vestige.edition.standard;

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

import com.googlecode.vestige.admin.command.VestigeCommandExecutor;
import com.googlecode.vestige.admin.ssh.DefaultPublickeyAuthenticator;
import com.googlecode.vestige.admin.ssh.RootedFileSystemFactory;
import com.googlecode.vestige.admin.ssh.SSHExecCommand;
import com.googlecode.vestige.admin.ssh.SSHShellCommandFactory;
import com.googlecode.vestige.application.ApplicationManager;
import com.googlecode.vestige.edition.standard.schema.Bind;
import com.googlecode.vestige.edition.standard.schema.SSH;
import com.googlecode.vestige.platform.VestigePlatform;

/**
 * @author Gael Lalire
 */
public class SSHServerFactory implements Callable<SshServer> {

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

    public SshServer call() throws Exception {
        Bind bind = ssh.getBind();

        File privateKey = new File(sshBase, "vestige_rsa");
        if (!privateKey.exists()) {
            sshBase.mkdirs();
            IOUtils.copy(StandardEditionVestige.class.getResourceAsStream("vestige_rsa"), new FileOutputStream(privateKey));
        }
        LOGGER.info("Use {} for private ssh key file", privateKey);
        KeyPairProvider keyPairProvider = new FileKeyPairProvider(new String[] {privateKey.getPath()});
        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setFileSystemFactory(new RootedFileSystemFactory(appHomeFile, "vestige"));
        sshServer.setIoServiceFactoryFactory(new MinaServiceFactoryFactory());
        String host = bind.getHost();
        sshServer.setHost(host);
        sshServer.setPort(bind.getPort());
        if (LOGGER.isInfoEnabled()) {
            if (host == null) {
                host = "*";
            }
            LOGGER.info("Listen on {}:{} for ssh interface", host, bind.getPort());
        }
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
        LOGGER.info("Use {} for authorized public ssh keys file", authorizedKeysFile);
        sshServer.setPublickeyAuthenticator(new DefaultPublickeyAuthenticator(keyPairProvider, authorizedKeysFile));
        // 1 hour time out
        sshServer.getProperties().put(ServerFactoryManager.IDLE_TIMEOUT, "3600000");
        return sshServer;
    }

}
