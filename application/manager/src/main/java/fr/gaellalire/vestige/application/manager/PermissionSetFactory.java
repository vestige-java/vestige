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

package fr.gaellalire.vestige.application.manager;

import java.io.Serializable;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gael Lalire
 */
public class PermissionSetFactory implements Serializable {

    private static final long serialVersionUID = 2023263475386818166L;

    private Set<Permission> staticPermissions;

    private List<PermissionFactory> dynamicPermissions;

    public PermissionSetFactory() {
        this.staticPermissions = new HashSet<Permission>();
        this.dynamicPermissions = new ArrayList<PermissionFactory>();
    }

    public void addPermissionFactory(final PermissionFactory permissionFactory) {
        if (permissionFactory.isDynamic()) {
            dynamicPermissions.add(permissionFactory);
        } else {
            staticPermissions.add(permissionFactory.createPermission());
        }
    }

    public Set<Permission> createPermissionSet() {
        if (dynamicPermissions.size() == 0) {
            return staticPermissions;
        }
        Set<Permission> permissions = new HashSet<Permission>(staticPermissions);
        for (PermissionFactory permissionFactory : dynamicPermissions) {
            permissions.add(permissionFactory.createPermission());
        }
        return permissions;
    }

}
