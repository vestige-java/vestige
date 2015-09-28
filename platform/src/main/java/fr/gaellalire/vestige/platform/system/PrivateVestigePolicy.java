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

package fr.gaellalire.vestige.platform.system;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;

/**
 * @author Gael Lalire
 */
public class PrivateVestigePolicy extends Policy {

    private PermissionCollection impliesPermissionCollection;

    private ThreadLocal<PermissionCollection> permissionCollectionThreadLocal = new InheritableThreadLocal<PermissionCollection>();

    public PrivateVestigePolicy(final PermissionCollection impliesPermissionCollection) {
        this.impliesPermissionCollection = impliesPermissionCollection;
    }

    public void setPermissionCollection(final PermissionCollection permissionCollection) {
        permissionCollectionThreadLocal.set(permissionCollection);
    }

    public void unsetPermissionCollection() {
        permissionCollectionThreadLocal.remove();
    }

    public PermissionCollection getPermissionCollection() {
        return permissionCollectionThreadLocal.get();
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
        permissionCollectionThreadLocal.set(impliesPermissionCollection);
        try {
            if (permissionCollection != null && permissionCollection.implies(permission)) {
                return true;
            }
            // permission is refused, however if the permission is not a JVM permission we ignore it
            ClassLoader permClassLoader = permission.getClass().getClassLoader();
            if (permClassLoader == null) {
                return false;
            }
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            while (systemClassLoader != null) {
                if (systemClassLoader == permClassLoader) {
                    return false;
                }
                systemClassLoader = systemClassLoader.getParent();
            }
            // not a JVM permission
            return true;
        } finally {
            if (permissionCollection == null) {
                permissionCollectionThreadLocal.remove();
            } else {
                permissionCollectionThreadLocal.set(permissionCollection);
            }
        }
    }

}
