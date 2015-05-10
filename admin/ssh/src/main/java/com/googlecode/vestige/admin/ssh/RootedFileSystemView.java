/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.googlecode.vestige.admin.ssh;

import java.io.File;

import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.common.file.nativefs.NativeSshFile;

/**
 * <strong>Internal class, do not use directly.</strong> File system view based
 * on native file system. Here the root directory will be user virtual root (/).
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class RootedFileSystemView extends NativeFileSystemView {


    // the first and the last character will always be '/'
    // It is always with respect to the root directory.
    private String currDir;

    private String rootDir;

    private String userName;

    /**
     * Constructor - internal do not use directly, use
     * {@link org.apache.sshd.common.file.nativefs.NativeFileSystemFactory} instead.
     */
    public RootedFileSystemView(final File root, final String userName) {
        super(userName);
        currDir = "/";
        rootDir = NativeSshFile.getPhysicalName(root.getAbsolutePath(), "/", "/", false);
        this.userName = userName;
    }

    /**
     * Get file object.
     */
    @Override
    public SshFile getFile(final String file) {
        return getFile(currDir, file);
    }

    @Override
    public SshFile getFile(final SshFile baseDir, final String file) {
        return getFile(baseDir.getAbsolutePath(), file);
    }

    @Override
    public SshFile getFile(final String dir, final String file) {
        // get actual file object
        String physicalName = NativeSshFile.getPhysicalName(rootDir, dir, file, false);
        File fileObj = new File(physicalName);

        // strip the root directory and return
        String userFileName = physicalName.substring(rootDir.length() - 1);
        return new RootedSshFile(this, userFileName, fileObj, userName);
    }

    @Override
    public NativeSshFile createNativeSshFile(final String name, final File file, final String userName) {
        return new RootedSshFile(this, name, file, userName);
    }

}
