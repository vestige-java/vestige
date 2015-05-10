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

import java.io.File;
import java.io.IOException;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;

/**
 * @author Gael Lalire
 */
public class RootedFileSystemFactory implements FileSystemFactory {

    private File root;

    private String userName;

    public RootedFileSystemFactory(final File root, final String userName) {
        this.root = root;
        this.userName = userName;
    }

    public FileSystemView createFileSystemView(final Session session) throws IOException {
        return new RootedFileSystemView(root, userName);
    }

}
