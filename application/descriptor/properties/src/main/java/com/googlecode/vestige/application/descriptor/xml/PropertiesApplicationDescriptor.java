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

package com.googlecode.vestige.application.descriptor.xml;

import java.security.Permission;
import java.util.List;
import java.util.Set;

import com.googlecode.vestige.application.ApplicationDescriptor;
import com.googlecode.vestige.application.ApplicationException;
import com.googlecode.vestige.platform.ClassLoaderConfiguration;

/**
 * @author Gael Lalire
 */
public class PropertiesApplicationDescriptor implements ApplicationDescriptor {

    public Set<List<Integer>> getSupportedMigrationVersions() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<List<Integer>> getUninterruptedMigrationVersions() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getInstallerClassName() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    public ClassLoaderConfiguration getInstallerClassLoaderConfiguration() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLauncherClassName() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    public ClassLoaderConfiguration getLauncherClassLoaderConfiguration() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLauncherPrivateSystem() throws ApplicationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<Permission> getPermissions() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Permission> getInstallerPermissions() throws ApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isInstallerPrivateSystem() throws ApplicationException {
        // TODO Auto-generated method stub
        return false;
    }

}
