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

package fr.gaellalire.vestige.resolver.maven.secure;

import java.security.Permission;
import java.security.PermissionCollection;

import fr.gaellalire.vestige.resolver.common.secure.SecureResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolvedClassLoaderConfiguration;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.Scope;
import fr.gaellalire.vestige.spi.resolver.maven.CreateClassLoaderConfigurationRequest;
import fr.gaellalire.vestige.spi.resolver.maven.ModifyLoadedDependencyRequest;
import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.PrivateVestigePolicy;

/**
 * @author Gael Lalire
 */
public class SecureCreateClassLoaderConfigurationRequest implements CreateClassLoaderConfigurationRequest {

    private VestigeSystem secureVestigeSystem;

    private PrivateVestigePolicy vestigePolicy;

    private CreateClassLoaderConfigurationRequest delegate;

    public SecureCreateClassLoaderConfigurationRequest(final VestigeSystem secureVestigeSystem, final PrivateVestigePolicy vestigePolicy,
            final CreateClassLoaderConfigurationRequest delegate) {
        this.secureVestigeSystem = secureVestigeSystem;
        this.vestigePolicy = vestigePolicy;
        this.delegate = delegate;
    }

    @Override
    public void addModifyScope(final String groupId, final String artifactId, final Scope scope) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.addModifyScope(groupId, artifactId, scope);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void setNamedModuleActivated(final boolean namedModuleActivated) {
        delegate.setNamedModuleActivated(namedModuleActivated);
    }

    @Override
    public void addReads(final String source, final String target) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.addReads(source, target);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void addExports(final String source, final String pn, final String target) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.addExports(source, pn, target);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void addOpens(final String source, final String pn, final String target) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            delegate.addOpens(source, pn, target);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public ResolvedClassLoaderConfiguration execute() throws ResolverException {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            ResolvedClassLoaderConfiguration execute = delegate.execute();
            PermissionCollection permissionCollection = vestigePolicy.getPermissionCollection();
            if (permissionCollection != null) {
                for (Permission permission : execute.getPermissions()) {
                    permissionCollection.add(permission);
                }
            }
            return new SecureResolvedClassLoaderConfiguration(secureVestigeSystem, execute);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public ModifyLoadedDependencyRequest addModifyLoadedDependency(final String groupId, final String artifactId) {
        VestigeSystem vestigeSystem = secureVestigeSystem.setCurrentSystem();
        try {
            return delegate.addModifyLoadedDependency(groupId, artifactId);
        } finally {
            vestigeSystem.setCurrentSystem();
        }
    }

    @Override
    public void setSelfExcluded(final boolean selfExcluded) {
        delegate.setSelfExcluded(selfExcluded);
    }

    @Override
    public void setDependenciesExcluded(final boolean dependenciesExcluded) {
        delegate.setDependenciesExcluded(dependenciesExcluded);
    }

}
