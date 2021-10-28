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

import java.security.Permission;
import java.util.Collection;
import java.util.Set;

import fr.gaellalire.vestige.spi.resolver.AttachableClassLoader;
import fr.gaellalire.vestige.spi.system.VestigeSystem;

/**
 * @author Gael Lalire
 */
public class RuntimeApplicationInstallerContext {

    private AttachableClassLoader classLoader;

    private ApplicationInstallerInvoker applicationInstaller;

    private VestigeSystem vestigeSystem;

    private Set<Permission> installerAdditionnalPermissions;

    public RuntimeApplicationInstallerContext(final AttachableClassLoader classLoader, final Set<Permission> installerAdditionnalPermissions, final VestigeSystem vestigeSystem) {
        this.classLoader = classLoader;
        this.installerAdditionnalPermissions = installerAdditionnalPermissions;
        this.vestigeSystem = vestigeSystem;
    }

    public AttachableClassLoader getClassLoader() {
        return classLoader;
    }

    public void setApplicationInstaller(final ApplicationInstallerInvoker applicationInstaller) {
        this.applicationInstaller = applicationInstaller;
    }

    public ApplicationInstallerInvoker getApplicationInstaller() {
        return applicationInstaller;
    }

    public VestigeSystem getVestigeSystem() {
        return vestigeSystem;
    }

    public Set<Permission> getInstallerAdditionnalPermissions() {
        return installerAdditionnalPermissions;
    }

    public void addInstallerAdditionnalPermissions(final Collection<Permission> additionnalPermissions) {
        additionnalPermissions.addAll(installerAdditionnalPermissions);
    }

}
