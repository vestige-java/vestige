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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.sshd.common.file.nativefs.NativeSshFile;

/**
 * @author Gael Lalire
 */
public class RootedSshFile extends NativeSshFile {

    private Map<Attribute, Object> attributes = new HashMap<Attribute, Object>();

    public RootedSshFile(final RootedFileSystemView rootedFileSystemView, final String fileName, final File file, final String userName) {
        super(rootedFileSystemView, fileName, file, userName);
    }

    @Override
    public void setAttribute(final Attribute attribute, final Object value) throws IOException {
        switch (attribute) {
        case LastModifiedTime:
            file.setLastModified((Long) value);
            break;
        default:
            break;
        }
        attributes.put(attribute, value);
    }

    @Override
    public void setAttributes(final Map<Attribute, Object> attributes) throws IOException {
        for (Entry<Attribute, Object> entry : attributes.entrySet()) {
            setAttribute(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object getAttribute(final Attribute attribute, final boolean followLinks) throws IOException {
        Object object = this.attributes.get(attribute);
        if (object != null) {
            return object;
        }
        return super.getAttribute(attribute, followLinks);
    }

    @Override
    public Map<Attribute, Object> getAttributes(final boolean followLinks) throws IOException {
        Map<Attribute, Object> attributes = super.getAttributes(followLinks);
        attributes.putAll(this.attributes);
        return attributes;
    }

}
