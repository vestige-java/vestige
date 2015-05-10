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

package com.googlecode.vestige.platform.system;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

/**
 * @author Gael Lalire
 */
public class PrivateVestigePolicy extends Policy {

    private ThreadLocal<PermissionCollection> permissionCollectionThreadLocal = new InheritableThreadLocal<PermissionCollection>();

    public void setPermissionCollection(final PermissionCollection permissionCollection) {
        permissionCollectionThreadLocal.set(permissionCollection);
    }

    public void unsetPermissionCollection() {
        permissionCollectionThreadLocal.remove();
    }

    @Override
    public PermissionCollection getPermissions(final CodeSource codesource) {
        return new Permissions();
    }

    @Override
    public PermissionCollection getPermissions(final ProtectionDomain domain) {
        return new Permissions();
    }

    @Override
    public boolean implies(final ProtectionDomain domain, final Permission permission) {
        PermissionCollection permissionCollection = permissionCollectionThreadLocal.get();
        if (permissionCollection == null) {
            // not restricted
            return true;
        }
        if (permissionCollection.implies(permission)) {
            return true;
        }
        if (!AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                ClassLoader permClassLoader = permission.getClass().getClassLoader();
                if (permClassLoader == null) {
                    return true;
                }
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                while (systemClassLoader != null) {
                    if (systemClassLoader == permClassLoader) {
                        return true;
                    }
                    systemClassLoader = systemClassLoader.getParent();
                }
                return false;
            }
        })) {
            // the permission is not a system permission, so we ignore it
            return true;
        }
        return false;
    }

}
