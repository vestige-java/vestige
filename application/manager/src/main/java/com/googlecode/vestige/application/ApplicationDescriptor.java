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

package com.googlecode.vestige.application;

import java.security.Permission;
import java.util.List;
import java.util.Set;

import com.googlecode.vestige.platform.ClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public interface ApplicationDescriptor {

    Set<List<Integer>> getSupportedMigrationVersions() throws ApplicationException;

    Set<List<Integer>> getUninterruptedMigrationVersions() throws ApplicationException;

    String getInstallerClassName() throws ApplicationException;

    ClassLoaderConfiguration getInstallerClassLoaderConfiguration() throws ApplicationException;

    String getLauncherClassName() throws ApplicationException;

    boolean isInstallerPrivateSystem() throws ApplicationException;

    boolean isLauncherPrivateSystem() throws ApplicationException;

    ClassLoaderConfiguration getLauncherClassLoaderConfiguration() throws ApplicationException;

    Set<Permission> getInstallerPermissions() throws ApplicationException;

    Set<Permission> getPermissions() throws ApplicationException;

}
