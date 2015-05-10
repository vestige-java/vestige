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

package com.googlecode.vestige.admin.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.util.Base64;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gael Lalire
 */
public class DefaultPublickeyAuthenticator implements PublickeyAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPublickeyAuthenticator.class);

    private Set<PublicKey> authorizedKeys;

    private File authorizedKeysFile;

    private long authorizedKeysFileLastModified;

    public DefaultPublickeyAuthenticator(final KeyPairProvider hostKeyProvider, final File authorizedKeysFile) {
        authorizedKeys = new HashSet<PublicKey>();
        this.authorizedKeysFile = authorizedKeysFile;
    }

    public boolean authenticate(final String username, final PublicKey suppliedKey, final ServerSession session) {
        if (username.equals("admin")) {
            if (getAuthorizedKeys().contains(suppliedKey)) {
                return true;
            }
        }
        return false;
    }

    public Set<PublicKey> getAuthorizedKeys() {
        synchronized (authorizedKeys) {
            if (authorizedKeysFileLastModified != authorizedKeysFile.lastModified()) {
                authorizedKeys.clear();
                if (authorizedKeysFile.isFile()) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(authorizedKeysFile));
                        try {
                            String line = br.readLine();
                            while (line != null) {
                                line = line.trim();
                                if (line.startsWith("#") || line.length() == 0) {
                                    line = br.readLine();
                                    continue;
                                }
                                String cleanLine;
                                if (line.startsWith("ssh-rsa ")) {
                                    cleanLine = line.substring("ssh-rsa ".length());
                                } else {
                                    cleanLine = line;
                                }
                                try {
                                    byte[] bytes = Base64.decodeBase64(cleanLine.getBytes());
                                    authorizedKeys.add(new Buffer(bytes).getRawPublicKey());
                                } catch (SshException e) {
                                    LOGGER.error("authorized_keys line is invalid", e);
                                }
                                line = br.readLine();
                            }
                        } finally {
                            br.close();
                        }
                        authorizedKeysFileLastModified = authorizedKeysFile.lastModified();
                    } catch (IOException e) {
                        LOGGER.error("authorized_keys file is unreadable", e);
                    }
                } else {
                    authorizedKeysFileLastModified = 0;
                }
            }
            return authorizedKeys;
        }
    }
}
