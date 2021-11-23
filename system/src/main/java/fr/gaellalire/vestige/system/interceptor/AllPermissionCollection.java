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

package fr.gaellalire.vestige.system.interceptor;

import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Gael Lalire
 */
public final class AllPermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = 4559120281103579044L;

    public static final AllPermissionCollection INSTANCE = new AllPermissionCollection();

    private static final List<Permission> ALL_PERMISSION_LIST = Collections.<Permission> singletonList(new AllPermission());

    private AllPermissionCollection() {
    }

    @Override
    public void add(final Permission permission) {
    }

    @Override
    public boolean implies(final Permission permission) {
        return true;
    }

    @Override
    public Enumeration<Permission> elements() {
        return Collections.enumeration(ALL_PERMISSION_LIST);
    }

}
